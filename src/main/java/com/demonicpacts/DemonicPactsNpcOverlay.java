package com.demonicpacts;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DemonicPactsNpcOverlay extends Overlay
{
    private final Client client;
    private final DemonicPactsConfig config;
    private final DemonicPactsPlugin plugin;

    @Inject
    DemonicPactsNpcOverlay(Client client, DemonicPactsConfig config, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_MED);
    }

    private static final class NpcCandidate
    {
        final NPC npc;
        final List<DemonicPactsTask> tasks;
        final int distSq;
        final String name;

        NpcCandidate(NPC npc, List<DemonicPactsTask> tasks, int distSq, String name)
        {
            this.npc = npc;
            this.tasks = tasks;
            this.distSq = distSq;
            this.name = name;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.highlightNpcs())
        {
            plugin.getRenderedHighlights().clearNpcs();
            return null;
        }

        Player local = client.getLocalPlayer();
        LocalPoint playerLp = local == null ? null : local.getLocalLocation();

        List<NpcCandidate> candidates = new ArrayList<>();

        for (NPC npc : client.getNpcs())
        {
            if (npc == null || npc.getName() == null)
            {
                continue;
            }

            // Skip pets and summoning-style followers — they aren't task NPCs
            // but can match item/task names (e.g. a Kalphite pet matching "Kalphite Queen").
            try
            {
                net.runelite.api.NPCComposition comp = npc.getTransformedComposition();
                if (comp != null && comp.isFollower())
                {
                    continue;
                }
            }
            catch (Exception ignored) {}

            List<DemonicPactsTask> tasks = TaskDatabase.findNpcTasks(npc.getName());
            if (tasks.isEmpty())
            {
                // NPCs named like "Fishing spot", "Rod Fishing spot", "Small Net Fishing spot"
                // are technically NPCs but behave like interactable objects. Try the
                // object lookup so fishing spots still get highlighted for Catch tasks.
                tasks = TaskDatabase.findObjectTasks(npc.getName());
                if (tasks.isEmpty())
                {
                    continue;
                }
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

            // Filter out completed tasks if enabled
            if (config.hideCompleted())
            {
                tasks = plugin.getCompletedTaskManager().filterIncomplete(tasks);
                if (tasks.isEmpty())
                {
                    continue;
                }
            }

            int distSq = 0;
            if (playerLp != null)
            {
                LocalPoint npcLp = npc.getLocalLocation();
                if (npcLp != null)
                {
                    int dx = npcLp.getX() - playerLp.getX();
                    int dy = npcLp.getY() - playerLp.getY();
                    distSq = dx * dx + dy * dy;
                }
            }

            candidates.add(new NpcCandidate(npc, tasks, distSq, npc.getName()));
        }

        // "One per object type" — extended to NPCs. Sort nearest-first, then keep
        // only the first NPC of each distinct name so crowds of e.g. Guards
        // collapse to one highlight.
        if (config.onePerType())
        {
            candidates.sort(Comparator.comparingInt(c -> c.distSq));
            Set<String> seen = new HashSet<>();
            List<NpcCandidate> dedup = new ArrayList<>();
            for (NpcCandidate c : candidates)
            {
                String key = c.name == null ? "" : c.name.toLowerCase();
                if (seen.add(key))
                {
                    dedup.add(c);
                }
            }
            candidates = dedup;
        }

        // Publish the set of NPCs we're actually drawing to the shared state
        // so the tooltip overlay can match hovered-vs-rendered.
        RenderedHighlights rh = plugin.getRenderedHighlights();
        rh.clearNpcs();
        for (NpcCandidate c : candidates)
        {
            rh.markNpc(c.npc.getIndex());
            rh.markNpcName(c.name);
        }

        for (NpcCandidate c : candidates)
        {
            NPC npc = c.npc;
            List<DemonicPactsTask> tasks = c.tasks;

            // Use the lowest-difficulty incomplete task — that's the "easiest
            // remaining thing to do" for this entity, which is what the player
            // would attempt first. Falls back to the lowest task overall if
            // everything has been completed (only relevant when hideCompleted
            // is off).
            DemonicPactsTask highestTask = pickDisplayTask(tasks);
            Color color = config.useDifficultyColors()
                ? highestTask.getDifficulty().getColor()
                : config.defaultHighlightColor();

            Shape hull = npc.getConvexHull();
            if (hull != null)
            {
                // Draw filled hull with transparency
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
                graphics.setColor(fillColor);
                graphics.fill(hull);

                // Draw border
                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(config.npcBorderWidth()));
                graphics.draw(hull);
            }

            // Draw task icon/text above NPC
            String label = "\u2694 " + highestTask.getDifficulty().name();
            if (tasks.size() > 1)
            {
                label += " (+" + (tasks.size() - 1) + ")";
            }

            Point textLoc = npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + 40);
            if (textLoc != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
            }
        }

        return null;
    }

    /**
     * Pick the task whose difficulty drives the highlight colour and label.
     * Prefers the lowest-difficulty task that's still incomplete (the "easiest
     * thing left to do"), falling back to the lowest difficulty overall when
     * every task on this entity has already been finished.
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
