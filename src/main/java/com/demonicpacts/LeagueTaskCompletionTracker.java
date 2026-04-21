package com.demonicpacts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads task completion status from the League Tasks panel (widget 657).
 * Child 18 = task name text column, Child 19 = status column (coloured per row).
 * Text color 0xF47113 (orange) on the status widget = complete, 0x9F9F9F (grey) = incomplete.
 *
 * Note: widget 657 only contains a small subset of tasks (typically the ones
 * you've completed in the current session or those shown in the summary
 * view), not your full lifetime completion history. For full coverage we
 * rely on the chat-message handler in DemonicPactsPlugin.onChatMessage,
 * which catches every "you've completed a task" announcement in real time.
 */
@Slf4j
@Singleton
public class LeagueTaskCompletionTracker
{
    private static final int TASK_LOG_GROUP_ID = 657;
    private static final int NAME_COLUMN_CHILD = 18;
    private static final int STATUS_COLUMN_CHILD = 19;
    private static final int COLOR_COMPLETE = 0xF47113;

    private final Set<String> completedTaskNames = new HashSet<>();

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() != TASK_LOG_GROUP_ID)
        {
            return;
        }
        // Give the client a tick to populate the dynamic children
        clientThread.invokeLater(this::syncFromTaskLog);
    }

    /**
     * Re-sync on every game tick while the task log is open. The widget's
     * dynamic children change when the player switches area tabs, scrolls, or
     * changes filters — each of those surfaces different tasks, and we want
     * to capture completion state from every view the player passes through.
     */
    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event)
    {
        Widget root = client.getWidget(TASK_LOG_GROUP_ID, 0);
        if (root == null || root.isHidden())
        {
            return;
        }
        syncFromTaskLog();
    }

    /**
     * Read task completion state from the Leagues task panel.
     *
     * The correct widget path (discovered empirically via the in-game widget
     * inspector on 2026-04-21) is:
     *   group 657 -> child 16 -> static children -> index 2 -> dynamic children
     *
     * Child 2 is the "name" column. Each dynamic child is one task row with
     * text like "Fletch 1000 arrow shafts" and a text color of 0xF47113
     * (orange) when completed, something else (grey-ish) when incomplete.
     *
     * The column inflates from a short preload (~300 rows) to the full 1592
     * a fraction of a second after the panel opens. The per-tick re-sync in
     * onGameTick makes sure we eventually catch the full list even if the
     * first read came in early.
     *
     * The legacy top-level children (NAME_COLUMN_CHILD, STATUS_COLUMN_CHILD)
     * remain constants but are only used as fallbacks if the nested path
     * unexpectedly fails.
     */
    private void syncFromTaskLog()
    {
        Widget container = client.getWidget(TASK_LOG_GROUP_ID, 16);
        if (container != null)
        {
            Widget[] pages = container.getStaticChildren();
            if (pages != null && pages.length > 2)
            {
                Widget namePage = pages[2];
                Widget[] rows = namePage.getDynamicChildren();
                if (rows != null)
                {
                    for (Widget row : rows)
                    {
                        if (row == null) continue;
                        String raw = row.getText();
                        if (raw == null || raw.isEmpty()) continue;
                        if (row.getTextColor() != COLOR_COMPLETE) continue;
                        String clean = cleanTaskName(raw);
                        if (clean.isEmpty()) continue;
                        completedTaskNames.add(clean);
                    }
                }
            }
        }

        // Legacy fallback path, kept in case nested structure goes missing
        Widget nameCol = client.getWidget(TASK_LOG_GROUP_ID, NAME_COLUMN_CHILD);
        Widget statusCol = client.getWidget(TASK_LOG_GROUP_ID, STATUS_COLUMN_CHILD);
        if (nameCol == null || statusCol == null)
        {
            return;
        }

        Widget[] names = nameCol.getDynamicChildren();
        Widget[] statuses = statusCol.getDynamicChildren();
        if (names == null || statuses == null)
        {
            return;
        }

        int count = Math.min(names.length, statuses.length);
        int newlyFound = 0;
        for (int i = 0; i < count; i++)
        {
            if (statuses[i].getTextColor() != COLOR_COMPLETE)
            {
                continue;
            }

            String cleaned = cleanTaskName(names[i].getText());
            if (cleaned.isEmpty())
            {
                cleaned = cleanTaskName(statuses[i].getText());
            }
            if (!cleaned.isEmpty() && completedTaskNames.add(cleaned))
            {
                newlyFound++;
            }
        }

        if (newlyFound > 0)
        {
            log.debug("Task log sync: +{} newly completed (total {})",
                newlyFound, completedTaskNames.size());
        }
    }

    /**
     * Strip HTML/colour tags, trim whitespace, and drop a trailing period so the
     * widget text matches what we have in TaskDatabase.
     */
    private static String cleanTaskName(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.replaceAll("<[^>]*>", "").trim();
        if (s.endsWith("."))
        {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Check if a task is completed. Tries exact match, case-insensitive match,
     * and finally a contains check both ways so minor wording drift still syncs.
     */
    public boolean isComplete(String taskName)
    {
        if (taskName == null)
        {
            return false;
        }
        String target = cleanTaskName(taskName);
        if (target.isEmpty())
        {
            return false;
        }
        if (completedTaskNames.contains(target))
        {
            return true;
        }
        String targetLower = target.toLowerCase();
        for (String completed : completedTaskNames)
        {
            String completedLower = completed.toLowerCase();
            if (completedLower.equals(targetLower))
            {
                return true;
            }
            if (completedLower.contains(targetLower) || targetLower.contains(completedLower))
            {
                return true;
            }
        }
        return false;
    }

    public Set<String> getCompleted()
    {
        return completedTaskNames;
    }

    public void addCompleted(String taskName)
    {
        if (taskName != null)
        {
            String cleaned = cleanTaskName(taskName);
            if (!cleaned.isEmpty())
            {
                completedTaskNames.add(cleaned);
            }
        }
    }

    public void clear()
    {
        completedTaskNames.clear();
    }

    public int getCompletedCount()
    {
        return completedTaskNames.size();
    }
}
