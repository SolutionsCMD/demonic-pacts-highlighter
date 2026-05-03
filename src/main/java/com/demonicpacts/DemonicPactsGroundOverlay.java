package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DemonicPactsGroundOverlay extends Overlay
{
    private final Client client;
    private final DemonicPactsConfig config;
    private final ItemManager itemManager;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsGroundOverlay(Client client, DemonicPactsConfig config, ItemManager itemManager, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_MED);
    }

    private static final class GroundCandidate
    {
        final Tile tile;
        final String itemName;
        final List<DemonicPactsTask> tasks;
        final int distSq;

        GroundCandidate(Tile tile, String itemName, List<DemonicPactsTask> tasks, int distSq)
        {
            this.tile = tile;
            this.itemName = itemName;
            this.tasks = tasks;
            this.distSq = distSq;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        RenderedHighlights rh = plugin.getRenderedHighlights();
        rh.clearGround();

        if (!config.highlightGroundItems())
        {
            return null;
        }

        Player local = client.getLocalPlayer();
        LocalPoint playerLp = local == null ? null : local.getLocalLocation();

        // First pass: collect one candidate per (tile, item name) combination.
        // Deduping by name on each tile keeps stacks (50 iron ore) from piling up.
        List<GroundCandidate> candidates = new ArrayList<>();
        Tile[][][] tiles = client.getScene().getTiles();
        int z = client.getPlane();

        for (int x = 0; x < tiles[z].length; x++)
        {
            for (int y = 0; y < tiles[z][x].length; y++)
            {
                Tile tile = tiles[z][x][y];
                if (tile == null)
                {
                    continue;
                }

                List<TileItem> groundItems = tile.getGroundItems();
                if (groundItems == null)
                {
                    continue;
                }

                Set<String> drawnOnTile = new HashSet<>();
                for (TileItem item : groundItems)
                {
                    if (item == null)
                    {
                        continue;
                    }

                    String itemName = itemManager.getItemComposition(item.getId()).getName();
                    if (!drawnOnTile.add(itemName.toLowerCase()))
                    {
                        continue;
                    }

                    List<DemonicPactsTask> tasks = TaskDatabase.findItemTasks(itemName);
                    if (tasks.isEmpty())
                    {
                        continue;
                    }

                    tasks = plugin.getHiddenTaskManager().filterVisible(tasks);
                    if (tasks.isEmpty())
                    {
                        continue;
                    }

                    tasks = plugin.filterByEnabledRegions(tasks);
                    if (tasks.isEmpty())
                    {
                        continue;
                    }

                    if (config.hideCompleted())
                    {
                        tasks = plugin.getCompletedTaskManager().filterIncomplete(tasks);
                        if (tasks.isEmpty())
                        {
                            continue;
                        }
                    }

                    int distSq = 0;
                    LocalPoint tileLp = tile.getLocalLocation();
                    if (playerLp != null && tileLp != null)
                    {
                        int dx = tileLp.getX() - playerLp.getX();
                        int dy = tileLp.getY() - playerLp.getY();
                        distSq = dx * dx + dy * dy;
                    }

                    candidates.add(new GroundCandidate(tile, itemName, tasks, distSq));
                }
            }
        }

        // "One per type" extended to ground items: keep only the nearest tile per
        // unique item name so a scattered drop of iron ore highlights just the
        // closest pile.
        if (config.onePerType())
        {
            Map<String, GroundCandidate> nearestByName = new HashMap<>();
            for (GroundCandidate c : candidates)
            {
                String key = c.itemName.toLowerCase();
                GroundCandidate existing = nearestByName.get(key);
                if (existing == null || c.distSq < existing.distSq)
                {
                    nearestByName.put(key, c);
                }
            }
            candidates = new ArrayList<>(nearestByName.values());
        }

        // Sort nearest-first so overlapping text labels favor the closest item
        candidates.sort(Comparator.comparingInt(c -> c.distSq));

        for (GroundCandidate c : candidates)
        {
            DemonicPactsTask highestTask = pickDisplayTask(c.tasks);
            Color color = config.useDifficultyColors()
                ? highestTask.getDifficulty().getColor()
                : config.defaultHighlightColor();

            LocalPoint lp = c.tile.getLocalLocation();
            if (lp == null)
            {
                continue;
            }

            // Publish this (tile, item name) so the tooltip only fires over
            // rendered piles. Use scene coords converted to world coords for
            // consistency with the tooltip's menu-entry lookup.
            int sceneX = lp.getSceneX();
            int sceneY = lp.getSceneY();
            net.runelite.api.coords.WorldPoint wp =
                net.runelite.api.coords.WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
            if (wp != null)
            {
                rh.markGround(wp.getX(), wp.getY(), c.itemName);
            }

            Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
            if (tilePoly != null)
            {
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 30);
                graphics.setColor(fillColor);
                graphics.fill(tilePoly);

                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(1));
                graphics.draw(tilePoly);
            }

            Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp,
                "\u2694 " + c.itemName, 0);
            if (textLoc != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLoc, "\u2694 " + c.itemName, color);
            }
        }

        return null;
    }

    /**
     * Pick the lowest-difficulty incomplete task as the colour driver.
     * See DemonicPactsNpcOverlay#pickDisplayTask for the rationale.
     */
    private DemonicPactsTask pickDisplayTask(List<DemonicPactsTask> tasks)
    {
        CompletedTaskManager mgr = plugin.getCompletedTaskManager();
        DemonicPactsTask lowestIncomplete = null;
        DemonicPactsTask lowestAny = tasks.get(0);
        for (DemonicPactsTask task : tasks)
        {
            if (task.getDifficulty().ordinal() < lowestAny.getDifficulty().ordinal())
            {
                lowestAny = task;
            }
            if (mgr.isCompleted(task))
            {
                continue;
            }
            if (lowestIncomplete == null
                || task.getDifficulty().ordinal() < lowestIncomplete.getDifficulty().ordinal())
            {
                lowestIncomplete = task;
            }
        }
        return lowestIncomplete != null ? lowestIncomplete : lowestAny;
    }
}
