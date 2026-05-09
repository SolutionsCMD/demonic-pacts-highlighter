package com.demonicpacts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Outlines skill orbs in the resizable stats tab when the player's XP for that
 * skill crosses any of the league task XP milestones (99 / 25M / 35M / 50M /
 * 100M / 200M). Implements GitHub issue #18.
 *
 * Skill widget detection is best-effort: we walk the dynamic children of the
 * stats container (widget group 320) and match each child's name against
 * known skill names. If Jagex changes the widget structure later we may need
 * to revisit. Configurable via DemonicPactsConfig#xpTrimEnabled.
 */
@Slf4j
public class DemonicPactsXpTrimOverlay extends Overlay
{
    // Resizable-layout stats tab group ID. 320:0 is the root container.
    private static final int STATS_GROUP_ID = 320;

    // Milestone XP thresholds and matching outline colours, in ascending
    // order. The highest milestone the player has crossed wins.
    private static final int[] MILESTONE_XP = {
        13_034_431,   // 99
        25_000_000,   // 25M
        35_000_000,   // 35M
        50_000_000,   // 50M
        100_000_000,  // 100M
        200_000_000,  // 200M
    };

    private static final Color[] MILESTONE_COLORS = {
        new Color(102, 204, 102),   // 99   — green
        new Color(102, 204, 255),   // 25M  — light blue
        new Color(180, 102, 255),   // 35M  — purple
        new Color(255, 153, 51),    // 50M  — orange
        new Color(255, 215, 0),     // 100M — gold
        new Color(255, 51, 51),     // 200M — red
    };

    private final Client client;
    private final DemonicPactsConfig config;
    private final DemonicPactsPlugin plugin;

    // Maps lowercased Skill.getName() to the enum value. Built once.
    private final Map<String, Skill> skillByName;

    @Inject
    DemonicPactsXpTrimOverlay(Client client, DemonicPactsConfig config, DemonicPactsPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        skillByName = new HashMap<>();
        for (Skill s : Skill.values())
        {
            if (s.getName() != null)
            {
                skillByName.put(s.getName().toLowerCase(Locale.ROOT), s);
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isLeaguesWorld() || !config.xpTrimEnabled())
        {
            return null;
        }

        Widget root = client.getWidget(STATS_GROUP_ID, 0);
        if (root == null || root.isHidden())
        {
            return null;
        }

        Widget[] children = root.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            children = root.getStaticChildren();
        }
        if (children == null)
        {
            return null;
        }

        graphics.setStroke(new BasicStroke(2f));
        for (Widget child : children)
        {
            if (child == null)
            {
                continue;
            }
            String name = child.getName();
            if (name == null || name.isEmpty())
            {
                continue;
            }
            // Widget names sometimes come back like "<col=...>Attack</col>" —
            // strip tags before matching.
            String cleaned = name.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
            Skill skill = skillByName.get(cleaned);
            if (skill == null)
            {
                continue;
            }
            int xp = client.getSkillExperience(skill);
            int tier = -1;
            for (int i = 0; i < MILESTONE_XP.length; i++)
            {
                if (xp >= MILESTONE_XP[i])
                {
                    tier = i;
                }
            }
            if (tier < 0)
            {
                continue;
            }
            Rectangle bounds = child.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
            {
                continue;
            }
            graphics.setColor(MILESTONE_COLORS[tier]);
            graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        }
        return null;
    }
}
