package com.demonicpacts;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
    name = "Demonic Pacts Task Highlighter",
    description = "Highlights NPCs, items, and objects related to Demonic Pacts League tasks with tooltips showing task details",
    tags = {"league", "demonic", "pacts", "tasks", "highlight", "overlay"}
)
public class DemonicPactsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private DemonicPactsConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DemonicPactsNpcOverlay npcOverlay;

    @Inject
    private DemonicPactsItemOverlay itemOverlay;

    @Inject
    private DemonicPactsGroundOverlay groundOverlay;

    @Inject
    private DemonicPactsTooltipOverlay tooltipOverlay;

    @Inject
    private DemonicPactsObjectOverlay objectOverlay;

    @Inject
    private DemonicPactsXpTrimOverlay xpTrimOverlay;

    @Inject
    private DemonicPactsSpellbookOverlay spellbookOverlay;

    @Inject
    private DemonicPactsPrayerOverlay prayerOverlay;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private ConfigManager configManager;

    @Getter
    @Inject
    private CompletedTaskManager completedTaskManager;

    @Getter
    @Inject
    private HiddenTaskManager hiddenTaskManager;

    @Getter
    @Inject
    private RenderedHighlights renderedHighlights;

    @Inject
    private LeagueTaskCompletionTracker taskTracker;

    // Tracks whether we've shown the autocomplete hint this session
    private boolean shownLoginHint = false;

    // Tracks whether we've shown the loaded-task-count banner this session
    private boolean shownVersionBanner = false;

    @Override
    protected void startUp() throws Exception
    {
        log.debug("Demonic Pacts Task Highlighter started - loaded {} tasks", TaskDatabase.getAllTasks().size());
        overlayManager.add(npcOverlay);
        overlayManager.add(itemOverlay);
        overlayManager.add(groundOverlay);
        overlayManager.add(tooltipOverlay);
        overlayManager.add(objectOverlay);
        overlayManager.add(xpTrimOverlay);
        overlayManager.add(spellbookOverlay);
        overlayManager.add(prayerOverlay);

        // Register the widget-based tracker to receive WidgetLoaded events
        eventBus.register(taskTracker);

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            completedTaskManager.loadForCurrentProfile();
            hiddenTaskManager.loadForCurrentProfile();
            // If the plugin was enabled while already logged in, hint now (next tick)
            clientThread.invokeLater(this::showLoginHintIfNeeded);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(npcOverlay);
        overlayManager.remove(itemOverlay);
        overlayManager.remove(groundOverlay);
        overlayManager.remove(tooltipOverlay);
        overlayManager.remove(objectOverlay);
        overlayManager.remove(xpTrimOverlay);
        overlayManager.remove(spellbookOverlay);
        overlayManager.remove(prayerOverlay);
        eventBus.unregister(taskTracker);
        taskTracker.clear();
        completedTaskManager.onLogout();
        hiddenTaskManager.onLogout();
        shownLoginHint = false;
        log.debug("Demonic Pacts Task Highlighter stopped");
    }

    @Provides
    DemonicPactsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DemonicPactsConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            completedTaskManager.loadForCurrentProfile();
            hiddenTaskManager.loadForCurrentProfile();
            clientThread.invokeLater(this::showVersionBannerIfNeeded);
            clientThread.invokeLater(this::showLoginHintIfNeeded);
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            taskTracker.clear();
            completedTaskManager.onLogout();
            hiddenTaskManager.onLogout();
            shownLoginHint = false;
            shownVersionBanner = false;
        }
    }

    /**
     * Shows a chat message once per login confirming which build is running.
     * The task count changes whenever the database is updated, so it works
     * as a quick "did my new build actually load?" signal in dev sessions.
     */
    private void showVersionBannerIfNeeded()
    {
        if (shownVersionBanner || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        shownVersionBanner = true;

        String message = new ChatMessageBuilder()
            .append(Color.MAGENTA, "[Demonic Pacts] ")
            .append(Color.WHITE, "Loaded " + TaskDatabase.getAllTasks().size() + " tasks.")
            .build();

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
    }

    /**
     * Shows a chat message once per session reminding the player to open the
     * Leagues task log so the plugin can sync completed tasks via widget 657.
     */
    private void showLoginHintIfNeeded()
    {
        if (shownLoginHint || !config.showLoginHint() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        shownLoginHint = true;

        String message = new ChatMessageBuilder()
            .append(Color.MAGENTA, "[Demonic Pacts] ")
            .append(Color.WHITE, "Open the Leagues task menu so completed tasks sync automatically.")
            .build();

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
    }

    /**
     * Every game tick, sync widget tracker completions into CompletedTaskManager.
     * When the tracker reports newly-synced tasks (i.e. tasks completed before
     * the plugin was installed that just showed up from the task log widget),
     * emit a one-shot chat message so the player knows the sync worked.
     */
    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event)
    {
        if (!config.autoDetectCompletion() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // Drain any tasks the widget tracker discovered since the last tick.
        // The tracker stores canonical TaskDatabase names already, so we can
        // mark them all completed in a single batch (one config-write) instead
        // of iterating ALL_TASKS and saving per task — that O(N) loop was
        // visible as client lag while the league task panel was open.
        Set<String> newlySynced = taskTracker.drainNewlySynced();
        if (newlySynced.isEmpty())
        {
            return;
        }
        completedTaskManager.markCompletedBatch(newlySynced);

        int count = newlySynced.size();
        String msg = new ChatMessageBuilder()
            .append(Color.MAGENTA, "[Demonic Pacts] ")
            .append(Color.WHITE, "Synced " + count + " completed task" + (count == 1 ? "" : "s") + " from your task log.")
            .build();
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
    }

    /**
     * Auto-detect task completion from league chat messages.
     * Game format: "Congratulations, you've completed an easy task: TASK NAME"
     */
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!config.autoDetectCompletion())
        {
            return;
        }

        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String message = Text.removeTags(event.getMessage());

        if ((message.contains("you've completed") || message.contains("you have completed")) && message.contains("task"))
        {
            int colonIdx = message.lastIndexOf(':');
            if (colonIdx >= 0 && colonIdx < message.length() - 1)
            {
                String taskName = message.substring(colonIdx + 1).trim();
                if (taskName.endsWith("."))
                {
                    taskName = taskName.substring(0, taskName.length() - 1).trim();
                }

                // Try exact match
                for (DemonicPactsTask task : TaskDatabase.getAllTasks())
                {
                    if (task.getName().equalsIgnoreCase(taskName))
                    {
                        completedTaskManager.markCompleted(task.getName());
                        taskTracker.addCompleted(task.getName());
                        log.debug("Chat-detected task completion: {}", task.getName());
                        return;
                    }
                }

                // Fuzzy match
                for (DemonicPactsTask task : TaskDatabase.getAllTasks())
                {
                    if (message.toLowerCase().contains(task.getName().toLowerCase()))
                    {
                        completedTaskManager.markCompleted(task.getName());
                        taskTracker.addCompleted(task.getName());
                        log.debug("Chat-detected task completion (fuzzy): {}", task.getName());
                        return;
                    }
                }
            }
        }
    }

    // =========================================================================
    // Hide Task menu
    // =========================================================================

    private static final String HIDE_MENU_TEXT = "Hide task";
    private static final String COMPLETE_MENU_TEXT = "Mark complete";

    /**
     * Inject a red "Hide task: <Name>" menu entry for each task matching the
     * hovered NPC, item (inventory/bank/equipment/ground), or game object.
     * Hidden tasks still appear in this menu with prefix "Unhide task" so the
     * player can reverse the action from the same context.
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!config.enableHideTaskMenu())
        {
            return;
        }

        MenuEntry entry = event.getMenuEntry();
        MenuAction type = entry.getType();
        String cleanTarget = Text.removeTags(entry.getTarget()).trim();

        // Inject on Examine entries for NPCs, world objects, and ground items.
        // For inventory/bank/equipment, the Examine action type is different
        // (CC_OP_LOW_PRIORITY with "Examine" as the option text) so we match
        // by option text as well. Also inject on widget-action entries
        // ("Cast", "Activate", etc.) so spells and prayers get the same
        // hide-task option as the rest of the highlight surfaces.
        String optionLower = Text.removeTags(entry.getOption()).toLowerCase();
        boolean isExamine = type == MenuAction.EXAMINE_NPC
            || type == MenuAction.EXAMINE_OBJECT
            || type == MenuAction.EXAMINE_ITEM_GROUND
            || type == MenuAction.EXAMINE_ITEM
            || (type == MenuAction.CC_OP_LOW_PRIORITY
                && "examine".equals(optionLower));
        boolean isWidgetAction = (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY)
            && (optionLower.startsWith("cast")
                || optionLower.startsWith("activate")
                || optionLower.startsWith("reactivate")
                || optionLower.startsWith("deactivate")
                || optionLower.startsWith("toggle"));
        if (!isExamine && !isWidgetAction)
        {
            return;
        }

        Set<DemonicPactsTask> matches = new HashSet<>();

        // NPC tasks
        NPC npc = entry.getNpc();
        if (npc != null && npc.getName() != null)
        {
            matches.addAll(TaskDatabase.findNpcTasks(npc.getName()));
            // Fishing spots are NPCs but tasks are mapped via object keywords —
            // gated to fishing-spot-like names since findObjectTasks now falls
            // back to ITEM_TASKS (which would over-match generic NPC names).
            if (npc.getName().toLowerCase().contains("fishing spot"))
            {
                matches.addAll(TaskDatabase.findObjectTasks(npc.getName()));
            }
        }

        // Inventory / bank / equipment: resolve by item ID when available
        int itemId = entry.getItemId();
        if (itemId >= 0)
        {
            try
            {
                String resolvedName = itemManager.getItemComposition(itemId).getName();
                if (resolvedName != null && !resolvedName.isEmpty() && !"null".equals(resolvedName))
                {
                    matches.addAll(TaskDatabase.findItemTasks(resolvedName));
                }
            }
            catch (Exception ignored) {}
        }

        // Item tasks (ground examine gives item name via target)
        if (!cleanTarget.isEmpty())
        {
            matches.addAll(TaskDatabase.findItemTasks(cleanTarget));
            matches.addAll(TaskDatabase.findObjectTasks(cleanTarget));
            // Also try NPC lookup by target text as a fallback
            if (npc == null)
            {
                matches.addAll(TaskDatabase.findNpcTasks(cleanTarget));
            }
        }

        if (matches.isEmpty())
        {
            return;
        }

        // Check existing menu entries to avoid injecting duplicate "Hide task" lines
        // when the game fires onMenuEntryAdded multiple times (e.g. stacks of ground items).
        MenuEntry[] existing = client.getMenuEntries();

        for (DemonicPactsTask task : matches)
        {
            // Skip already-hidden tasks so players can't unhide from the right-click menu.
            // They must use the "Unhide All Tasks" config toggle instead.
            if (hiddenTaskManager.isHidden(task))
            {
                continue;
            }

            // Skip completed tasks — no point offering to hide something already done.
            if (completedTaskManager.isCompleted(task))
            {
                continue;
            }

            // Skip tasks whose region the user has disabled — they wouldn't see
            // the highlight anyway, so offering to hide is just noise.
            if (!isTaskRegionEnabled(task))
            {
                continue;
            }

            // Dedup: don't add the same Hide task entry twice in the same menu cycle
            boolean alreadyThere = false;
            String wantTarget = "<col=ffaa00>" + task.getName() + "</col>";
            for (MenuEntry me : existing)
            {
                if (me.getType() == MenuAction.RUNELITE
                    && wantTarget.equals(me.getTarget())
                    && me.getOption() != null
                    && me.getOption().contains(HIDE_MENU_TEXT))
                {
                    alreadyThere = true;
                    break;
                }
            }
            if (alreadyThere)
            {
                continue;
            }

            client.createMenuEntry(-1)
                .setOption("<col=ff0000>" + HIDE_MENU_TEXT + "</col>")
                .setTarget(wantTarget)
                .setType(MenuAction.RUNELITE)
                .onClick(e -> hiddenTaskManager.hide(task.getName()));
        }
    }

    /**
     * When the player ticks the "Unhide All Tasks" config box, clear the
     * hidden-task set for this account and reset the toggle so it can be used
     * again later.
     */
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"demonicpactstaskhighlighter".equals(event.getGroup()))
        {
            return;
        }
        if ("unhideAllTasks".equals(event.getKey()) && "true".equals(event.getNewValue()))
        {
            int count = hiddenTaskManager.getHiddenCount();
            hiddenTaskManager.clearAll();
            configManager.setConfiguration("demonicpactstaskhighlighter", "unhideAllTasks", false);

            String msg = new ChatMessageBuilder()
                .append(Color.MAGENTA, "[Demonic Pacts] ")
                .append(Color.WHITE, "Unhid " + count + " task" + (count == 1 ? "" : "s") + ".")
                .build();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
        }
    }

    // =========================================================================
    // Region filtering
    // =========================================================================

    /**
     * Returns true if the given task's area is currently enabled in the
     * Regions config section. "General" tasks are always shown since they
     * aren't tied to any unlock.
     */
    public boolean isTaskRegionEnabled(DemonicPactsTask task)
    {
        if (task == null)
        {
            return true;
        }
        String area = task.getArea();
        if (area == null || area.isEmpty() || "General".equalsIgnoreCase(area))
        {
            return true;
        }
        switch (area.toLowerCase())
        {
            case "asgarnia":   return config.regionAsgarnia();
            case "desert":     return config.regionDesert();
            case "fremennik":  return config.regionFremennik();
            case "kandarin":   return config.regionKandarin();
            case "karamja":    return config.regionKaramja();
            case "kourend":    return config.regionKourend();
            case "morytania":  return config.regionMorytania();
            case "tirannwn":   return config.regionTirannwn();
            case "varlamore":  return config.regionVarlamore();
            case "wilderness": return config.regionWilderness();
            default:           return true; // Unknown area — show by default
        }
    }

    /**
     * Convenience: filter a list of tasks down to those in enabled regions.
     * Returns a new list — the input is not modified.
     */
    public java.util.List<DemonicPactsTask> filterByEnabledRegions(java.util.List<DemonicPactsTask> tasks)
    {
        java.util.List<DemonicPactsTask> result = new java.util.ArrayList<>();
        for (DemonicPactsTask task : tasks)
        {
            if (isTaskRegionEnabled(task))
            {
                result.add(task);
            }
        }
        return result;
    }
}
