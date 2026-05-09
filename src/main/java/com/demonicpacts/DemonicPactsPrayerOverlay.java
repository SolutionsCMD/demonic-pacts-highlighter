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
import java.util.Locale;
import java.util.Set;

/**
 * Mirror of DemonicPactsSpellbookOverlay for the prayer book. Outlines
 * prayer icons that match outstanding "Activate X" / "Use the X Prayer"
 * tasks. Configurable via DemonicPactsConfig#highlightSpellbook (we
 * piggy-back on the same toggle since spells and prayers are the same
 * conceptual highlight surface).
 */
@Slf4j
public class DemonicPactsPrayerOverlay extends Overlay
{
    // Standard prayer book widget group (resizable).
    private static final int PRAYER_GROUP = 541;
    // Ruinous Powers (League / Necromancy spellbook variant) — included for
    // safety; harmless if not present in the player's account.
    private static final int RUINOUS_PRAYER_GROUP = 916;

    private static final Color OUTLINE_COLOR = new Color(255, 255, 0, 200);

    private final Client client;
    private final DemonicPactsConfig config;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsPrayerOverlay(Client client, DemonicPactsConfig config, DemonicPactsPlugin plugin)
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
        if (!plugin.isLeaguesWorld() || !config.highlightSpellbook())
        {
            return null;
        }

        Set<String> keywords = collectIncompletePrayerKeywords();
        if (keywords.isEmpty())
        {
            return null;
        }

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(OUTLINE_COLOR);

        for (int groupId : new int[]{PRAYER_GROUP, RUINOUS_PRAYER_GROUP})
        {
            Widget root = client.getWidget(groupId, 0);
            if (root == null || root.isHidden())
            {
                continue;
            }
            scanRecursive(graphics, root, keywords);
        }
        return null;
    }

    /**
     * Collect the names of prayers the player still has outstanding tasks
     * for. Tasks like "Activate Piety" / "Use the Piety Prayer" yield "piety"
     * after stripping the verb prefix and trailing " prayer".
     */
    private Set<String> collectIncompletePrayerKeywords()
    {
        Set<String> out = new HashSet<>();
        CompletedTaskManager mgr = plugin.getCompletedTaskManager();
        HiddenTaskManager hidden = plugin.getHiddenTaskManager();
        String[] prefixes = {"activate ", "use the ", "reactivate "};
        for (DemonicPactsTask task : TaskDatabase.getAllTasks())
        {
            if (mgr.isCompleted(task)) continue;
            if (hidden.isHidden(task)) continue;
            // Respect the per-region toggles so disabling a region also
            // hides the prayer circles for prayers scoped to it.
            if (!plugin.isTaskRegionEnabled(task)) continue;

            String n = task.getName();
            if (n == null) continue;
            String lower = n.toLowerCase(Locale.ROOT);
            for (String prefix : prefixes)
            {
                if (!lower.startsWith(prefix)) continue;
                String rest = lower.substring(prefix.length()).trim();
                if (rest.endsWith(" prayer"))
                {
                    rest = rest.substring(0, rest.length() - " prayer".length()).trim();
                }
                else if (!lower.contains("prayer"))
                {
                    // Skip non-prayer activate-tasks (e.g. "Activate the Statue
                    // of Ates"). Only keep verbs that look prayer-related.
                    break;
                }
                if (!rest.isEmpty())
                {
                    out.add(rest);
                }
                break;
            }
        }
        return out;
    }

    private void scanRecursive(Graphics2D graphics, Widget parent, Set<String> keywords)
    {
        scanArray(graphics, parent.getStaticChildren(), keywords);
        scanArray(graphics, parent.getDynamicChildren(), keywords);
        scanArray(graphics, parent.getNestedChildren(), keywords);
    }

    private void scanArray(Graphics2D graphics, Widget[] arr, Set<String> keywords)
    {
        if (arr == null) return;
        for (Widget w : arr)
        {
            if (w == null || w.isHidden()) continue;
            String name = w.getName();
            if (name != null && !name.isEmpty())
            {
                String cleaned = name.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
                if (keywords.contains(cleaned))
                {
                    drawOutline(graphics, w);
                }
            }
            scanRecursive(graphics, w, keywords);
        }
    }

    private void drawOutline(Graphics2D graphics, Widget w)
    {
        Rectangle b = w.getBounds();
        if (b == null || b.width <= 0 || b.height <= 0) return;
        graphics.drawOval(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
    }
}
