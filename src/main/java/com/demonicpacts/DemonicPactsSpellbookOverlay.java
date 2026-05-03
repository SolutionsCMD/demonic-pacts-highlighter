package com.demonicpacts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Draws a coloured circle around spell icons in the magic spellbook when the
 * player has outstanding "Cast X" tasks for those spells.
 *
 * Strategy: walk all known spellbook widget groups (standard / ancient /
 * lunar / arceuus), check each child widget's getName() against a set of
 * task keywords compiled from incomplete TaskType.SPELL tasks, and outline
 * matches. Best-effort first attempt — if Jagex names a spell widget
 * differently in any spellbook we may need to add aliases.
 */
@Slf4j
public class DemonicPactsSpellbookOverlay extends Overlay
{
    // Standard spellbook widget IDs (resizable client). The spellbook has
    // separate group IDs depending on which spellbook is active.
    private static final int[] SPELLBOOK_GROUPS = {
        218,  // Standard / modern
        217,  // Ancient
        430,  // Lunar
        220,  // Arceuus
    };

    private static final Color OUTLINE_COLOR = new Color(255, 0, 255, 200);

    private final Client client;
    private final DemonicPactsConfig config;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsSpellbookOverlay(Client client, DemonicPactsConfig config, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.highlightSpellbook())
        {
            return null;
        }

        // Build a set of spell keywords from outstanding SPELL-type tasks.
        // Cheap to recompute every frame at this scale — there aren't many
        // SPELL tasks in the database.
        Set<String> spellKeywords = collectIncompleteSpellKeywords();
        if (spellKeywords.isEmpty())
        {
            return null;
        }

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(OUTLINE_COLOR);

        for (int groupId : SPELLBOOK_GROUPS)
        {
            Widget root = client.getWidget(groupId, 0);
            if (root == null || root.isHidden())
            {
                continue;
            }
            drawMatchingChildren(graphics, root, spellKeywords);
        }
        return null;
    }

    private Set<String> collectIncompleteSpellKeywords()
    {
        Set<String> out = new HashSet<>();
        CompletedTaskManager mgr = plugin.getCompletedTaskManager();
        HiddenTaskManager hidden = plugin.getHiddenTaskManager();
        for (DemonicPactsTask task : TaskDatabase.getAllTasks())
        {
            String name = task.getName();
            // Accept any task named "Cast X" regardless of TaskType so newer
            // tasks I added with TaskType.ACTIVITY (Claws of Guthix, Saradomin
            // Strike, etc.) still light up.
            boolean nameMatch = name != null && name.toLowerCase(Locale.ROOT).startsWith("cast ");
            if (!nameMatch && task.getType() != TaskType.SPELL)
            {
                continue;
            }
            if (mgr.isCompleted(task)) continue;
            if (hidden.isHidden(task)) continue;
            // Respect the per-region toggles so disabling a region also turns
            // off the spell circles for tasks scoped to it.
            if (!plugin.isTaskRegionEnabled(task)) continue;

            if (task.getMatchKeywords() != null)
            {
                for (String kw : task.getMatchKeywords())
                {
                    if (kw != null && !kw.isEmpty())
                    {
                        out.add(kw.toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (nameMatch)
            {
                out.add(name.substring(5).toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /**
     * Recursively scan widget children. The spellbook tree typically has
     * spell icons as static or nested children; we match by widget name.
     */
    private void drawMatchingChildren(Graphics2D graphics, Widget parent, Set<String> keywords)
    {
        scanArray(graphics, parent.getStaticChildren(), keywords);
        scanArray(graphics, parent.getDynamicChildren(), keywords);
        scanArray(graphics, parent.getNestedChildren(), keywords);
    }

    private void scanArray(Graphics2D graphics, Widget[] arr, Set<String> keywords)
    {
        if (arr == null)
        {
            return;
        }
        for (Widget w : arr)
        {
            if (w == null || w.isHidden())
            {
                continue;
            }
            String name = w.getName();
            if (name != null && !name.isEmpty())
            {
                String cleaned = name.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
                if (keywords.contains(cleaned))
                {
                    drawOutline(graphics, w);
                }
            }
            // Recurse — some spellbook variants nest icons in sub-containers.
            drawMatchingChildren(graphics, w, keywords);
        }
    }

    private void drawOutline(Graphics2D graphics, Widget w)
    {
        Rectangle b = w.getBounds();
        if (b == null || b.width <= 0 || b.height <= 0)
        {
            return;
        }
        // Draw an oval rather than a rect for spells, since spell icons are
        // round in the OSRS UI.
        graphics.drawOval(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
    }
}
