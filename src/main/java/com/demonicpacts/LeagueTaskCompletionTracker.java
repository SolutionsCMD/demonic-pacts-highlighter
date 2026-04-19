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
 * Text color 0xF47113 (orange) on the status widget = complete, grey = incomplete.
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

    private void syncFromTaskLog()
    {
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
            // The status widget carries the row colour; orange = complete.
            if (statuses[i].getTextColor() != COLOR_COMPLETE)
            {
                continue;
            }

            // Prefer the NAME column for the actual task name. Fall back to the
            // status column if the name column is blank for some reason.
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

        log.debug("Task log synced. Total complete: {} (new this pass: {})",
            completedTaskNames.size(), newlyFound);
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
