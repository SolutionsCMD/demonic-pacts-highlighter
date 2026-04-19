package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

public class DemonicPactsItemOverlay extends WidgetItemOverlay
{
    private final Client client;
    private final DemonicPactsConfig config;
    private final ItemManager itemManager;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsItemOverlay(Client client, DemonicPactsConfig config, ItemManager itemManager, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.plugin = plugin;
        showOnInventory();
        showOnBank();
        showOnEquipment();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!config.highlightItems())
        {
            return;
        }

        String itemName = itemManager.getItemComposition(itemId).getName();
        List<DemonicPactsTask> tasks = TaskDatabase.findItemTasks(itemName);

        if (tasks.isEmpty())
        {
            return;
        }

        tasks = plugin.getHiddenTaskManager().filterVisible(tasks);
        if (tasks.isEmpty())
        {
            return;
        }

        if (config.hideCompleted())
        {
            tasks = plugin.getCompletedTaskManager().filterIncomplete(tasks);
            if (tasks.isEmpty())
            {
                return;
            }
        }

        // NOTE: "One Per Type" isn't applied to items. WidgetItemOverlay gets
        // called multiple times per frame at unpredictable intervals, so any
        // frame-level dedup causes flickering at higher framerates. Every
        // matching inventory / bank / equipment slot gets its border.
        // Publish every matching slot so the tooltip can match.
        plugin.getRenderedHighlights().markItem(0, itemName);

        DemonicPactsTask highestTask = getHighestDifficultyTask(tasks);
        Color color = config.useDifficultyColors()
            ? highestTask.getDifficulty().getColor()
            : config.defaultHighlightColor();

        Rectangle bounds = widgetItem.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        // Thin, crisp pixel-aligned border around the item icon. No AA (blurs
        // 1px strokes across 2 pixels). Inset by 1px on each side because the
        // slot bounds are slightly larger than the item icon footprint.
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setStroke(new BasicStroke(1f));
            g.setColor(color);
            g.drawRect(bounds.x - 1, bounds.y, bounds.width - 3, bounds.height - 1);
        }
        finally
        {
            g.dispose();
        }
    }

    private DemonicPactsTask getHighestDifficultyTask(List<DemonicPactsTask> tasks)
    {
        DemonicPactsTask highest = tasks.get(0);
        for (DemonicPactsTask task : tasks)
        {
            if (task.getDifficulty().ordinal() > highest.getDifficulty().ordinal())
            {
                highest = task;
            }
        }
        return highest;
    }
}
