package com.demonicpacts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Alpha;

import java.awt.Color;

@ConfigGroup("demonicpactstaskhighlighter")
public interface DemonicPactsConfig extends Config
{
    // =========================================================================
    // Sections
    // =========================================================================

    @ConfigSection(
        name = "Highlights",
        description = "What the plugin highlights in the world",
        position = 0
    )
    String highlightSection = "highlights";

    @ConfigSection(
        name = "Display",
        description = "Colors, border styling, and crowd control",
        position = 1
    )
    String displaySection = "display";

    @ConfigSection(
        name = "Regions",
        description = "Toggle which league regions you've unlocked. Tasks in disabled regions won't highlight or show tooltips.",
        position = 2
    )
    String regionsSection = "regions";

    @ConfigSection(
        name = "Tooltips",
        description = "What shows up in the hover tooltip",
        position = 3
    )
    String tooltipSection = "tooltips";

    @ConfigSection(
        name = "Task Tracking",
        description = "Completion detection and hidden-task management",
        position = 4
    )
    String trackingSection = "tracking";

    // =========================================================================
    // Highlights — master toggles
    // =========================================================================

    @ConfigItem(
        keyName = "highlightNpcs",
        name = "Highlight NPCs",
        description = "Highlight NPCs that are part of a league task",
        section = highlightSection,
        position = 0
    )
    default boolean highlightNpcs()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightItems",
        name = "Highlight Items",
        description = "Highlight items in inventory/bank that are part of a league task",
        section = highlightSection,
        position = 1
    )
    default boolean highlightItems()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightGroundItems",
        name = "Highlight Ground Items",
        description = "Highlight ground items that are part of a league task",
        section = highlightSection,
        position = 2
    )
    default boolean highlightGroundItems()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightObjects",
        name = "Highlight World Objects",
        description = "Master toggle for world-object highlights. Individual categories can be toggled below.",
        section = highlightSection,
        position = 3
    )
    default boolean highlightObjects()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Highlights — world object sub-categories
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "highlightTrees",
        name = "Highlight Trees",
        description = "Highlight trees that are part of a league task",
        section = highlightSection,
        position = 10
    )
    default boolean highlightTrees()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightRocks",
        name = "Highlight Rocks",
        description = "Highlight mining rocks that are part of a league task",
        section = highlightSection,
        position = 11
    )
    default boolean highlightRocks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightFishingSpots",
        name = "Highlight Fishing Spots",
        description = "Highlight fishing spots that are part of a league task",
        section = highlightSection,
        position = 12
    )
    default boolean highlightFishingSpots()
    {
        return true;
    }

    @ConfigItem(
        keyName = "highlightPatches",
        name = "Highlight Farming Patches",
        description = "Highlight farming patches, allotments, and flower beds that are part of a league task",
        section = highlightSection,
        position = 13
    )
    default boolean highlightPatches()
    {
        return true;
    }

    // =========================================================================
    // Display — appearance
    // =========================================================================

    @ConfigItem(
        keyName = "useDifficultyColors",
        name = "Use Difficulty Colors",
        description = "Color highlights based on task difficulty (Easy=Green, Medium=Orange, Hard=Red, Elite=Purple, Master=Cyan)",
        section = displaySection,
        position = 0
    )
    default boolean useDifficultyColors()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        keyName = "defaultHighlightColor",
        name = "Default Highlight Color",
        description = "Color used when difficulty colors are disabled",
        section = displaySection,
        position = 1
    )
    default Color defaultHighlightColor()
    {
        return new Color(255, 0, 255, 150);
    }

    @ConfigItem(
        keyName = "npcBorderWidth",
        name = "NPC Border Width",
        description = "Width of the highlight border on NPCs",
        section = displaySection,
        position = 2
    )
    default int npcBorderWidth()
    {
        return 2;
    }

    // -------------------------------------------------------------------------
    // Display — crowd control
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "onePerType",
        name = "One Per Type (NPCs & Objects)",
        description = "Only highlight the nearest instance of each NPC name and world-object name. E.g. one Guard + one iron rock + one clay rock, instead of every match on screen. (Does not apply to inventory/bank items.)",
        section = displaySection,
        position = 10
    )
    default boolean onePerType()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showAllObjects",
        name = "Show All Objects",
        description = "When One Per Type is off: highlight every matching object on screen. When off, only the closest N are highlighted.",
        section = displaySection,
        position = 11
    )
    default boolean showAllObjects()
    {
        return true;
    }

    @ConfigItem(
        keyName = "maxHighlightedObjects",
        name = "Max Highlighted Objects",
        description = "When both One Per Type and Show All Objects are off, only the nearest N world objects are highlighted.",
        section = displaySection,
        position = 12
    )
    default int maxHighlightedObjects()
    {
        return 100;
    }

    // =========================================================================
    // Tooltips
    // =========================================================================

    @ConfigItem(
        keyName = "showTooltips",
        name = "Show Tooltips",
        description = "Show task details when hovering over highlighted entities",
        section = tooltipSection,
        position = 0
    )
    default boolean showTooltips()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showRequirements",
        name = "Show Requirements",
        description = "Show task requirements in tooltips",
        section = tooltipSection,
        position = 1
    )
    default boolean showRequirements()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showPoints",
        name = "Show Points",
        description = "Show point values in tooltips",
        section = tooltipSection,
        position = 2
    )
    default boolean showPoints()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showArea",
        name = "Show Area",
        description = "Show task area in tooltips",
        section = tooltipSection,
        position = 3
    )
    default boolean showArea()
    {
        return true;
    }

    // =========================================================================
    // Task Tracking — completion
    // =========================================================================

    @ConfigItem(
        keyName = "hideCompleted",
        name = "Hide Completed Tasks",
        description = "Stop highlighting NPCs/items once their tasks are marked complete",
        section = trackingSection,
        position = 0
    )
    default boolean hideCompleted()
    {
        return true;
    }

    @ConfigItem(
        keyName = "autoDetectCompletion",
        name = "Auto-Detect Completion",
        description = "Automatically mark tasks complete when the league completion message appears in chat, or when you open the Leagues task menu",
        section = trackingSection,
        position = 1
    )
    default boolean autoDetectCompletion()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showCompletedInTooltip",
        name = "Show Completed In Tooltip",
        description = "Show completed tasks (with a checkmark) in tooltips instead of hiding them entirely",
        section = trackingSection,
        position = 2
    )
    default boolean showCompletedInTooltip()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showLoginHint",
        name = "Show Login Hint",
        description = "Show a one-time chat reminder on login to open the Leagues task menu for autocomplete",
        section = trackingSection,
        position = 3
    )
    default boolean showLoginHint()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Task Tracking — hidden tasks
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "enableHideTaskMenu",
        name = "Enable Hide Task Menu",
        description = "Adds a right-click 'Hide Task' option on highlighted NPCs, items, and objects so you can skip tasks you don't plan on doing",
        section = trackingSection,
        position = 10
    )
    default boolean enableHideTaskMenu()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showHiddenInTooltip",
        name = "Show Hidden In Tooltip",
        description = "Show hidden tasks faded out in tooltips instead of removing them entirely",
        section = trackingSection,
        position = 11
    )
    default boolean showHiddenInTooltip()
    {
        return false;
    }

    @ConfigItem(
        keyName = "unhideAllTasks",
        name = "Unhide All Tasks",
        description = "Tick this box to clear every hidden task for this account. The box auto-unticks after running so you can use it again later.",
        section = trackingSection,
        position = 12
    )
    default boolean unhideAllTasks()
    {
        return false;
    }

    // =========================================================================
    // Regions — toggle which league regions you've unlocked
    // =========================================================================

    @ConfigItem(
        keyName = "regionAsgarnia",
        name = "Asgarnia",
        description = "Highlight tasks located in Asgarnia",
        section = regionsSection,
        position = 0
    )
    default boolean regionAsgarnia()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionDesert",
        name = "Desert",
        description = "Highlight tasks located in the Desert",
        section = regionsSection,
        position = 1
    )
    default boolean regionDesert()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionFremennik",
        name = "Fremennik",
        description = "Highlight tasks located in Fremennik",
        section = regionsSection,
        position = 2
    )
    default boolean regionFremennik()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionKandarin",
        name = "Kandarin",
        description = "Highlight tasks located in Kandarin",
        section = regionsSection,
        position = 3
    )
    default boolean regionKandarin()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionKaramja",
        name = "Karamja",
        description = "Highlight tasks located in Karamja",
        section = regionsSection,
        position = 4
    )
    default boolean regionKaramja()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionKourend",
        name = "Kourend",
        description = "Highlight tasks located in Kourend",
        section = regionsSection,
        position = 5
    )
    default boolean regionKourend()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionMorytania",
        name = "Morytania",
        description = "Highlight tasks located in Morytania",
        section = regionsSection,
        position = 6
    )
    default boolean regionMorytania()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionTirannwn",
        name = "Tirannwn",
        description = "Highlight tasks located in Tirannwn",
        section = regionsSection,
        position = 7
    )
    default boolean regionTirannwn()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionVarlamore",
        name = "Varlamore",
        description = "Highlight tasks located in Varlamore (starter region)",
        section = regionsSection,
        position = 8
    )
    default boolean regionVarlamore()
    {
        return true;
    }

    @ConfigItem(
        keyName = "regionWilderness",
        name = "Wilderness",
        description = "Highlight tasks located in the Wilderness",
        section = regionsSection,
        position = 9
    )
    default boolean regionWilderness()
    {
        return true;
    }
}
