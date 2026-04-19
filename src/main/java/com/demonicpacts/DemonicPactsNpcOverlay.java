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

            List<DemonicPactsTask> tasks = TaskDatabase.findNpcTasks(npc.getName());
            if (tasks.isEmpty())
            {
                continue;
            }

            // Filter out hidden tasks
            tasks = plugin.getHiddenTaskManager().filterVisible(tasks);
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

            // Use the highest-difficulty task's color
            DemonicPactsTask highestTask = getHighestDifficultyTask(tasks);
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
