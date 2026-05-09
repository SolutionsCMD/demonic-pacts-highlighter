package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DemonicPactsObjectOverlay extends Overlay
{
    private final Client client;
    private final DemonicPactsConfig config;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsObjectOverlay(Client client, DemonicPactsConfig config, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_MED);
    }

    /** A candidate object to highlight, with its distance to the player. */
    private static final class Candidate
    {
        final GameObject gameObject;
        final List<DemonicPactsTask> tasks;
        final int distSq;
        final String objectName;

        Candidate(GameObject gameObject, List<DemonicPactsTask> tasks, int distSq, String objectName)
        {
            this.gameObject = gameObject;
            this.tasks = tasks;
            this.distSq = distSq;
            this.objectName = objectName;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        RenderedHighlights rh = plugin.getRenderedHighlights();
        rh.clearObjects();

        if (!plugin.isLeaguesWorld() || !config.highlightObjects())
        {
            return null;
        }

        Player me = client.getLocalPlayer();
        if (me == null)
        {
            return null;
        }
        LocalPoint myLp = me.getLocalLocation();
        if (myLp == null)
        {
            return null;
        }

        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = client.getPlane();

        List<Candidate> candidates = new ArrayList<>();

        for (int x = 0; x < tiles[z].length; x++)
        {
            for (int y = 0; y < tiles[z][x].length; y++)
            {
                Tile tile = tiles[z][x][y];
                if (tile == null)
                {
                    continue;
                }

                for (GameObject gameObject : tile.getGameObjects())
                {
                    if (gameObject == null)
                    {
                        continue;
                    }

                    String objectName = getObjectName(gameObject);
                    if (objectName == null)
                    {
                        continue;
                    }

                    List<DemonicPactsTask> tasks = TaskDatabase.findObjectTasks(objectName);
                    if (tasks.isEmpty())
                    {
                        continue;
                    }

                    // Per-category toggle: skip objects whose category is disabled
                    if (!shouldShowCategory(objectName))
                    {
                        continue;
                    }

                    // Filter out hidden tasks
                    tasks = plugin.getHiddenTaskManager().filterVisible(tasks);
                    if (tasks.isEmpty())
                    {
                        continue;
                    }

                    // Filter by enabled regions
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

                    LocalPoint lp = gameObject.getLocalLocation();
                    if (lp == null)
                    {
                        continue;
                    }
                    int dx = lp.getX() - myLp.getX();
                    int dy = lp.getY() - myLp.getY();
                    int distSq = dx * dx + dy * dy;

                    candidates.add(new Candidate(gameObject, tasks, distSq, objectName));
                }
            }
        }

        // Sort nearest-first. If "Show All Objects" is enabled, render everything;
        // otherwise cap to the configured max to reduce flicker in crowded scenes.
        candidates.sort(Comparator.comparingInt(c -> c.distSq));

        // "One per object type" — after sorting nearest-first, keep only the first
        // candidate for each distinct object name. One iron rock + one clay rock
        // survive, not 20 iron rocks + 5 clay rocks.
        if (config.onePerType())
        {
            java.util.Set<String> seenNames = new java.util.HashSet<>();
            java.util.List<Candidate> dedup = new java.util.ArrayList<>();
            for (Candidate c : candidates)
            {
                String key = c.objectName == null ? "" : c.objectName.toLowerCase();
                if (seenNames.add(key))
                {
                    dedup.add(c);
                }
            }
            candidates = dedup;
        }

        int limit;
        if (config.showAllObjects() || config.onePerType())
        {
            limit = candidates.size();
        }
        else
        {
            int max = config.maxHighlightedObjects();
            limit = (max > 0 && candidates.size() > max) ? max : candidates.size();
        }

        for (int i = 0; i < limit; i++)
        {
            Candidate c = candidates.get(i);
            DemonicPactsTask highestTask = pickDisplayTask(c.tasks);

            // Publish that this object is being drawn so the tooltip knows.
            // Use scene coords converted to world coords so the tooltip's menu-entry
            // based lookup (which goes scene -> world) produces identical keys.
            net.runelite.api.coords.LocalPoint lp = c.gameObject.getLocalLocation();
            if (lp != null)
            {
                int sceneX = lp.getSceneX();
                int sceneY = lp.getSceneY();
                net.runelite.api.coords.WorldPoint wp =
                    net.runelite.api.coords.WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
                if (wp != null)
                {
                    rh.markObject(wp.getX(), wp.getY(), c.objectName);
                }
            }

            Color color = config.useDifficultyColors()
                ? highestTask.getDifficulty().getColor()
                : config.defaultHighlightColor();

            Shape hull = c.gameObject.getConvexHull();
            if (hull != null)
            {
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 30);
                graphics.setColor(fillColor);
                graphics.fill(hull);

                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(config.npcBorderWidth()));
                graphics.draw(hull);
            }

            String icon = iconForTask(highestTask);
            String label = icon + " " + highestTask.getDifficulty().name();
            if (c.tasks.size() > 1)
            {
                label += " (+" + (c.tasks.size() - 1) + ")";
            }

            Point textLoc = c.gameObject.getCanvasTextLocation(graphics, label, 80);
            if (textLoc != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
            }
        }

        return null;
    }

    /**
     * Picks a label glyph based on the task type so trees show an axe,
     * rocks show a pickaxe, fishing spots show a rod, etc.
     */
    private String iconForTask(DemonicPactsTask task)
    {
        if (task == null || task.getType() == null)
        {
            return "\u2605"; // star fallback
        }
        switch (task.getType())
        {
            case CHOP_ITEM:
                return "\uD83E\uDE93"; // axe
            case MINE_ITEM:
                return "\u26CF";       // pickaxe
            case BURN_ITEM:
                return "\uD83D\uDD25"; // fire
            case CATCH_ITEM:
                return "\uD83C\uDFA3"; // fishing rod
            case COOK_ITEM:
                return "\uD83C\uDF73"; // cooking pan
            case DEFEAT_NPC:
                return "\u2694";       // crossed swords
            case CLEAN_ITEM:
                return "\uD83C\uDF3F"; // herb
            case CRAFT_ITEM:
                return "\u2692";       // hammer and pick
            case EQUIP_ITEM:
                return "\uD83D\uDEE1"; // shield
            case SPELL:
                return "\u2728";       // sparkles
            case PRAYER:
                return "\uD83D\uDD4A"; // dove
            case SKILL_LEVEL:
                return "\u2B50";       // star
            case ACTIVITY:
                return "\uD83C\uDFC6"; // trophy
            case MISC:
            default:
                return "\u2605";       // star
        }
    }

    private String getObjectName(GameObject gameObject)
    {
        try
        {
            ObjectComposition comp = client.getObjectDefinition(gameObject.getId());
            if (comp == null)
            {
                return null;
            }

            if (comp.getImpostorIds() != null)
            {
                ObjectComposition transformed = comp.getImpostor();
                if (transformed != null)
                {
                    String name = transformed.getName();
                    if (name != null && !name.equals("null") && !name.isEmpty())
                    {
                        return name;
                    }
                }
            }

            String name = comp.getName();
            if (name != null && !name.equals("null") && !name.isEmpty())
            {
                return name;
            }
        }
        catch (Exception ignored) {}

        return null;
    }

    /**
     * Pick the lowest-difficulty incomplete task as the colour/label driver.
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

    /**
     * Classify a world object by its in-game name and check the corresponding
     * config toggle. Objects that don't fit any category (e.g. sulliusceps,
     * miscellaneous scenery) are always allowed.
     */
    private boolean shouldShowCategory(String objectName)
    {
        if (objectName == null)
        {
            return true;
        }
        String n = objectName.toLowerCase();

        // Trees: anything containing "tree" or the common standalone log-producing names
        if (n.contains("tree")
            || n.equals("oak") || n.equals("willow") || n.equals("yew")
            || n.equals("maple") || n.equals("magic") || n.equals("teak")
            || n.equals("mahogany") || n.equals("redwood") || n.equals("blisterwood")
            || n.equals("padri") || n.equals("juniper"))
        {
            return config.highlightTrees();
        }

        // Mining rocks
        if (n.contains("rocks") || n.endsWith(" rock"))
        {
            return config.highlightRocks();
        }

        // Fishing spots
        if (n.contains("fishing spot"))
        {
            return config.highlightFishingSpots();
        }

        // Farming patches
        if (n.contains("patch") || n.contains("allotment"))
        {
            return config.highlightPatches();
        }

        // Unknown category — allow by default
        return true;
    }
}
