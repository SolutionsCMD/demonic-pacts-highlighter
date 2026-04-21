package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Highlights standard-spellbook spells that are referenced by incomplete tasks.
 *
 * The standard spellbook is widget group 218. Each spell is a child widget with
 * a `name` attribute containing markup like "&lt;col=...&gt;Home Teleport&lt;/col&gt;".
 * We strip tags and match against a small set of known Demonic Pacts spell tasks.
 *
 * Lunar, Ancient, and Arceuus spellbooks are not covered by this overlay —
 * adding them would require verified widget group IDs and spell-name lookups
 * for each book, which are out of scope for this release.
 */
public class DemonicPactsSpellbookOverlay extends Overlay
{
    private static final int STANDARD_SPELLBOOK_GROUP = 149;

    /**
     * Maps a canonical spell name (lowercase) to the Demonic Pacts task names
     * that require casting it. Only standard-spellbook spells are listed here.
     * If Jagex renames a spell the task will silently stop highlighting until
     * the mapping is updated, which is an acceptable failure mode.
     */
    private static final Map<String, String[]> SPELL_TO_TASKS = new HashMap<>();
    static
    {
        SPELL_TO_TASKS.put("home teleport",
            new String[]{"Cast Home Teleport"});
        SPELL_TO_TASKS.put("low level alchemy",
            new String[]{"Cast Low Level Alchemy"});
        SPELL_TO_TASKS.put("high level alchemy",
            new String[]{"Convert an item into at least 500 coins"});
        SPELL_TO_TASKS.put("earth blast",
            new String[]{"Cast an Earth Blast Spell"});
        SPELL_TO_TASKS.put("water surge",
            new String[]{"Cast a water surge spell at a Black dragon in Kandarin"});
        SPELL_TO_TASKS.put("water wave",
            new String[]{"Cast a Wave Spell"});
        SPELL_TO_TASKS.put("earth wave",
            new String[]{"Cast a Wave Spell"});
        SPELL_TO_TASKS.put("fire wave",
            new String[]{"Cast a Wave Spell"});
        SPELL_TO_TASKS.put("kourend castle teleport",
            new String[]{"Cast Kourend Castle Teleport"});
    }

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

    private boolean diagPrinted = false;
    private int renderCount = 0;

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.highlightSpells())
        {
            return null;
        }

        renderCount++;

        // Dump each child of both candidate groups (149 and 163) to find the spellbook
        if (!diagPrinted)
        {
            int[] groups = {149, 163};
            for (int groupId : groups)
            {
                Widget root = client.getWidget(groupId, 0);
                if (root == null) continue;
                Widget[] kids = root.getDynamicChildren();
                if (kids == null || kids.length == 0) continue;

                try
                {
                    client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "",
                        "<col=ff00ff>[DP-Spells]</col> ======== group " + groupId + " ========",
                        null);
                    for (int i = 0; i < Math.min(kids.length, 10); i++)
                    {
                        Widget w = kids[i];
                        if (w == null) continue;
                        String rawName = w.getName() == null ? "null" : w.getName();
                        String cleaned = rawName.replaceAll("<[^>]*>", "").trim();
                        int spriteId = w.getSpriteId();
                        boolean hidden = w.isHidden();
                        client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "",
                            "<col=ff00ff>[" + groupId + "." + i + "]</col> hidden=" + hidden
                                + " sprite=" + spriteId + " name='" + cleaned + "'",
                            null);
                    }
                }
                catch (Exception ignored) {}
            }
            diagPrinted = true;
        }

        Widget spellbook = client.getWidget(STANDARD_SPELLBOOK_GROUP, 0);
        if (spellbook == null || spellbook.isHidden())
        {
            return null;
        }

        Widget[] children = spellbook.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            return null;
        }

        CompletedTaskManager ctm = plugin.getCompletedTaskManager();
        HiddenTaskManager htm = plugin.getHiddenTaskManager();

        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setStroke(new BasicStroke(1f));

            for (Widget spell : children)
            {
                if (spell == null || spell.isHidden())
                {
                    continue;
                }

                String rawName = spell.getName();
                if (rawName == null || rawName.isEmpty())
                {
                    continue;
                }

                String name = rawName.replaceAll("<[^>]*>", "").trim().toLowerCase();
                String[] taskNames = SPELL_TO_TASKS.get(name);
                if (taskNames == null)
                {
                    continue;
                }

                // Find the first matching task that isn't completed, hidden, or
                // from a disabled region. If all are filtered out, skip.
                DemonicPactsTask visibleTask = null;
                for (String taskName : taskNames)
                {
                    for (DemonicPactsTask task : TaskDatabase.getAllTasks())
                    {
                        if (!task.getName().equalsIgnoreCase(taskName))
                        {
                            continue;
                        }
                        if (ctm.isCompleted(task)) continue;
                        if (htm.isHidden(task)) continue;
                        if (!plugin.isTaskRegionEnabled(task)) continue;
                        visibleTask = task;
                        break;
                    }
                    if (visibleTask != null) break;
                }

                if (visibleTask == null)
                {
                    continue;
                }

                Rectangle bounds = spell.getBounds();
                if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
                {
                    continue;
                }

                Color color = config.useDifficultyColors()
                    ? visibleTask.getDifficulty().getColor()
                    : config.defaultHighlightColor();
                g.setColor(color);
                g.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1);
            }
        }
        finally
        {
            g.dispose();
        }

        return null;
    }

    /**
     * Exposed for the tooltip overlay so it can show task info when hovering
     * a highlighted spell.
     */
    public static List<DemonicPactsTask> findSpellTasks(String spellName)
    {
        if (spellName == null) return java.util.Collections.emptyList();
        String[] names = SPELL_TO_TASKS.get(spellName.toLowerCase());
        if (names == null) return java.util.Collections.emptyList();
        java.util.List<DemonicPactsTask> out = new java.util.ArrayList<>();
        for (String taskName : names)
        {
            for (DemonicPactsTask task : TaskDatabase.getAllTasks())
            {
                if (task.getName().equalsIgnoreCase(taskName))
                {
                    out.add(task);
                }
            }
        }
        return out;
    }
}
