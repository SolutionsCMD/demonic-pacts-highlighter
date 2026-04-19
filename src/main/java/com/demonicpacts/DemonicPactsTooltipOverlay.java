package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

public class DemonicPactsTooltipOverlay extends Overlay
{
    private final Client client;
    private final DemonicPactsConfig config;
    private final TooltipManager tooltipManager;
    private final ItemManager itemManager;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsTooltipOverlay(Client client, DemonicPactsConfig config, TooltipManager tooltipManager, ItemManager itemManager, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.tooltipManager = tooltipManager;
        this.itemManager = itemManager;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOOLTIP);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showTooltips())
        {
            return null;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries.length == 0)
        {
            return null;
        }

        // Check the top (hovered) entry first
        MenuEntry topEntry = menuEntries[menuEntries.length - 1];
        String topTarget = topEntry.getTarget().replaceAll("<[^>]*>", "").trim();

        // 1. Check NPC tasks via entry.getNpc()
        NPC npc = topEntry.getNpc();
        if (npc != null && npc.getName() != null)
        {
            List<DemonicPactsTask> tasks = TaskDatabase.findNpcTasks(npc.getName());
            if (!tasks.isEmpty() && isNpcVisible(npc))
            {
                String text = buildTooltipText(tasks, npc.getName());
                if (text != null) tooltipManager.add(new Tooltip(text));
                return null;
            }
        }

        // 2. Check top entry target against both databases
        if (!topTarget.isEmpty())
        {
            String text = tryBuildTooltip(topTarget, topEntry);
            if (text != null)
            {
                tooltipManager.add(new Tooltip(text));
                return null;
            }
        }

        // 3. If top entry didn't match (e.g. "Walk here" on stacked ground items),
        //    scan ALL menu entries for task-related items/NPCs
        for (int i = menuEntries.length - 2; i >= 0; i--)
        {
            MenuEntry entry = menuEntries[i];
            String entryTarget = entry.getTarget().replaceAll("<[^>]*>", "").trim();

            if (entryTarget.isEmpty())
            {
                continue;
            }

            // Check NPC
            NPC entryNpc = entry.getNpc();
            if (entryNpc != null && entryNpc.getName() != null)
            {
                List<DemonicPactsTask> tasks = TaskDatabase.findNpcTasks(entryNpc.getName());
                if (!tasks.isEmpty() && isNpcVisible(entryNpc))
                {
                    String text = buildTooltipText(tasks, entryNpc.getName());
                    if (text != null)
                    {
                        tooltipManager.add(new Tooltip(text));
                        return null;
                    }
                }
            }

            // Check items
            String text = tryBuildTooltip(entryTarget, entry);
            if (text != null)
            {
                tooltipManager.add(new Tooltip(text));
                return null;
            }
        }

        return null;
    }

    /**
     * Try to build a tooltip for a given target name, checking item ID, item name,
     * NPC, and world object databases. Gated by RenderedHighlights so we only
     * show a tooltip for entities the overlays are actually drawing a highlight
     * on right now.
     */
    private String tryBuildTooltip(String cleanTarget, MenuEntry entry)
    {
        RenderedHighlights rh = plugin.getRenderedHighlights();
        MenuAction type = entry.getType();

        // Try item ID first (for inventory/bank items)
        if (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY)
        {
            int itemId = entry.getItemId();
            if (itemId >= 0)
            {
                try
                {
                    String itemName = itemManager.getItemComposition(itemId).getName();
                    List<DemonicPactsTask> tasks = TaskDatabase.findItemTasks(itemName);
                    if (!tasks.isEmpty() && isItemVisible(itemName))
                    {
                        return buildTooltipText(tasks, itemName);
                    }
                }
                catch (Exception ignored) {}
            }
        }

        // Try ground item actions — require the ground item to actually be highlighted
        if (isGroundItemAction(entry))
        {
            List<DemonicPactsTask> tasks = TaskDatabase.findItemTasks(cleanTarget);
            if (!tasks.isEmpty() && isGroundVisible(entry, cleanTarget))
            {
                return buildTooltipText(tasks, cleanTarget);
            }
        }

        // Fallback: try clean target against NPC database (with visibility)
        List<DemonicPactsTask> npcTasks = TaskDatabase.findNpcTasks(cleanTarget);
        if (!npcTasks.isEmpty())
        {
            // No precise NPC reference here — use name-level check
            if (rh.isNpcRenderedByName(cleanTarget) || !config.onePerType())
            {
                return buildTooltipText(npcTasks, cleanTarget);
            }
        }

        List<DemonicPactsTask> itemTasks = TaskDatabase.findItemTasks(cleanTarget);
        if (!itemTasks.isEmpty() && isItemVisible(cleanTarget))
        {
            return buildTooltipText(itemTasks, cleanTarget);
        }

        // World objects — must pass category toggle AND (if onePerType on) be the rendered one
        List<DemonicPactsTask> objectTasks = TaskDatabase.findObjectTasks(cleanTarget);
        if (!objectTasks.isEmpty() && isObjectVisible(entry, cleanTarget))
        {
            return buildTooltipText(objectTasks, cleanTarget);
        }

        return null;
    }

    /**
     * An NPC is "visible" for tooltip purposes if the NPC overlay is actually
     * drawing a highlight on it this frame.
     */
    private boolean isNpcVisible(NPC npc)
    {
        if (!config.highlightNpcs())
        {
            return false;
        }
        // When onePerType is off, every matching NPC is rendered so tooltip is fine.
        if (!config.onePerType())
        {
            return true;
        }
        return plugin.getRenderedHighlights().isNpcRendered(npc.getIndex());
    }

    /**
     * An item (inventory/bank/equipment) is "visible" if the item overlay drew
     * a border on it this frame. One Per Type doesn't apply to items (causes
     * flicker at high framerates) so any matching item is considered visible.
     */
    private boolean isItemVisible(String itemName)
    {
        return config.highlightItems();
    }

    /**
     * Ground-item visibility: when onePerType is on, tie the hovered tile to
     * the rendered set. Strict — no fallbacks, otherwise one rendered pile
     * would unlock tooltips on every pile of the same name.
     */
    private boolean isGroundVisible(MenuEntry entry, String itemName)
    {
        if (!config.highlightGroundItems())
        {
            return false;
        }
        if (!config.onePerType())
        {
            return true;
        }
        WorldPoint wp = entryToWorldPoint(entry);
        if (wp == null)
        {
            return false;
        }
        // 1-tile tolerance to handle any coord-rounding between menu entry and overlay mark
        return plugin.getRenderedHighlights().isGroundRenderedNear(wp.getX(), wp.getY(), itemName, 1);
    }

    /**
     * World-object visibility: first check the category toggle (so hovering a
     * tree with "Highlight Trees" off shows nothing), then check the per-tile
     * rendered set if onePerType is on. Strict — must match the exact rendered
     * tile.
     */
    private boolean isObjectVisible(MenuEntry entry, String objectName)
    {
        if (!config.highlightObjects())
        {
            return false;
        }
        if (!shouldShowCategory(objectName))
        {
            return false;
        }
        if (!config.onePerType())
        {
            return true;
        }
        WorldPoint wp = entryToWorldPoint(entry);
        if (wp == null)
        {
            return false;
        }
        // 1-tile tolerance to handle any coord-rounding between menu entry and overlay mark
        return plugin.getRenderedHighlights().isObjectRenderedNear(wp.getX(), wp.getY(), objectName, 1);
    }

    /**
     * Mirror of ObjectOverlay.shouldShowCategory so the tooltip respects the
     * same per-category toggles as the visual overlay.
     */
    private boolean shouldShowCategory(String objectName)
    {
        if (objectName == null)
        {
            return true;
        }
        String n = objectName.toLowerCase();
        if (n.contains("tree")
            || n.equals("oak") || n.equals("willow") || n.equals("yew")
            || n.equals("maple") || n.equals("magic") || n.equals("teak")
            || n.equals("mahogany") || n.equals("redwood") || n.equals("blisterwood")
            || n.equals("padri") || n.equals("juniper"))
        {
            return config.highlightTrees();
        }
        if (n.contains("rocks") || n.endsWith(" rock"))
        {
            return config.highlightRocks();
        }
        if (n.contains("fishing spot"))
        {
            return config.highlightFishingSpots();
        }
        if (n.contains("patch") || n.contains("allotment"))
        {
            return config.highlightPatches();
        }
        return true;
    }

    /**
     * Resolve a MenuEntry's param0/param1 (scene-local tile coords) into a
     * WorldPoint for matching against RenderedHighlights. Returns null if the
     * conversion isn't possible.
     */
    private WorldPoint entryToWorldPoint(MenuEntry entry)
    {
        try
        {
            int sceneX = entry.getParam0();
            int sceneY = entry.getParam1();
            if (sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
            {
                return null;
            }
            return WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    private boolean isGroundItemAction(MenuEntry entry)
    {
        MenuAction type = entry.getType();
        return type == MenuAction.GROUND_ITEM_FIRST_OPTION
            || type == MenuAction.GROUND_ITEM_SECOND_OPTION
            || type == MenuAction.GROUND_ITEM_THIRD_OPTION
            || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || type == MenuAction.GROUND_ITEM_FIFTH_OPTION
            || type == MenuAction.EXAMINE_ITEM_GROUND;
    }

    private String buildTooltipText(List<DemonicPactsTask> tasks, String entityName)
    {
        CompletedTaskManager ctm = plugin.getCompletedTaskManager();
        HiddenTaskManager htm = plugin.getHiddenTaskManager();
        StringBuilder sb = new StringBuilder();
        sb.append("<col=ffaa00>\u2694 Demonic Pacts League Task</col>");

        int shown = 0;
        for (int i = 0; i < tasks.size(); i++)
        {
            DemonicPactsTask task = tasks.get(i);
            boolean completed = ctm.isCompleted(task);
            boolean hidden = htm.isHidden(task);

            // Skip hidden tasks unless the player wants them faded
            if (hidden && !config.showHiddenInTooltip())
            {
                continue;
            }

            // Skip completed tasks unless configured to show them
            if (completed && config.hideCompleted() && !config.showCompletedInTooltip())
            {
                continue;
            }

            if (shown > 0)
            {
                sb.append("<br>---");
            }
            shown++;

            String hexColor = String.format("%06x", task.getDifficulty().getColor().getRGB() & 0xFFFFFF);

            // Show checkmark or X for completion status
            String statusIcon = completed ? "\u2714 " : (hidden ? "\u2300 " : "");
            String strikePrefix = (completed || hidden) ? "<col=666666>" : "";
            String strikeSuffix = (completed || hidden) ? "</col>" : "";

            sb.append("<br>").append(strikePrefix)
                .append(statusIcon)
                .append("<col=").append(hexColor).append(">")
                .append("[").append(task.getDifficulty().name()).append("]</col> ")
                .append(task.getName())
                .append(strikeSuffix);

            if (completed)
            {
                sb.append("<br><col=00ff00>COMPLETED</col>");
                continue;
            }
            if (hidden)
            {
                sb.append("<br><col=999999>HIDDEN</col>");
                continue;
            }

            sb.append("<br><col=b0b0b0>").append(task.getDescription()).append("</col>");

            if (config.showPoints())
            {
                sb.append("<br><col=ffffff>Points: </col><col=00ff00>")
                    .append(task.getDifficulty().getPoints()).append("</col>");
            }

            if (config.showArea() && !task.getArea().isEmpty())
            {
                sb.append("<br><col=ffffff>Area: </col><col=66ccff>")
                    .append(task.getArea()).append("</col>");
            }

            if (config.showRequirements() && task.getRequirements() != null && !task.getRequirements().isEmpty())
            {
                sb.append("<br><col=ffffff>Requires: </col><col=ff6666>")
                    .append(task.getRequirements()).append("</col>");
            }

            if (task.getQuantity() > 1)
            {
                sb.append("<br><col=ffffff>Quantity: </col><col=ffff00>")
                    .append(task.getQuantity()).append("</col>");
            }
        }

        if (shown == 0)
        {
            return null;
        }

        return sb.toString();
    }
}
