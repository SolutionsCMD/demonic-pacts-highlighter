package com.demonicpacts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads task completion status from the League Tasks panel (widget group 657).
 *
 * Strategy: walk the entire widget tree under group 657 recursively and collect
 * every widget whose text color matches the "completed" orange (0xF47113). This
 * is resilient to Jagex reshuffling child indexes between client updates — we
 * don't care where in the tree the row lives as long as the color is right and
 * the cleaned text matches a known task name in TaskDatabase.
 *
 * The panel inflates its rows asynchronously after opening, so we also re-sync
 * on every GameTick while the panel is visible to catch rows that weren't
 * present when the first WidgetLoaded fired.
 */
@Slf4j
@Singleton
public class LeagueTaskCompletionTracker
{
    private static final int TASK_LOG_GROUP_ID = 657;
    private static final int COLOR_COMPLETE = 0xF47113;

    private final Set<String> completedTaskNames = new HashSet<>();
    private final Set<String> newlySyncedSinceLastDrain = new HashSet<>();

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
     * Walk the entire widget 657 tree and collect every widget whose text color
     * is the completed orange (0xF47113). Match the cleaned text against known
     * tasks; the widget text may be either the task name or its description
     * depending on which column we hit, so we try both lookup directions.
     */
    private void syncFromTaskLog()
    {
        Widget root = client.getWidget(TASK_LOG_GROUP_ID, 0);
        if (root == null)
        {
            return;
        }

        int before = completedTaskNames.size();

        Deque<Widget> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty())
        {
            Widget w = stack.pop();
            if (w == null)
            {
                continue;
            }

            if (w.getTextColor() == COLOR_COMPLETE)
            {
                String cleaned = cleanTaskName(w.getText());
                if (!cleaned.isEmpty())
                {
                    String matched = matchKnownTask(cleaned);
                    if (matched != null && completedTaskNames.add(matched))
                    {
                        newlySyncedSinceLastDrain.add(matched);
                    }
                }
            }

            pushChildren(stack, w.getStaticChildren());
            pushChildren(stack, w.getDynamicChildren());
            pushChildren(stack, w.getNestedChildren());
        }

        int added = completedTaskNames.size() - before;
        if (added > 0)
        {
            log.debug("Task log sync: +{} newly completed (total {})", added, completedTaskNames.size());
        }
    }

    private static void pushChildren(Deque<Widget> stack, Widget[] children)
    {
        if (children == null)
        {
            return;
        }
        for (Widget child : children)
        {
            if (child != null)
            {
                stack.push(child);
            }
        }
    }

    /**
     * Resolve widget text (which may be either a task name or description) to
     * the canonical task name from TaskDatabase. Returns null if no match.
     */
    private static String matchKnownTask(String widgetText)
    {
        String target = widgetText.trim();
        if (target.isEmpty())
        {
            return null;
        }
        String targetLower = target.toLowerCase();

        // Exact (case-insensitive) name match first
        for (DemonicPactsTask task : TaskDatabase.getAllTasks())
        {
            if (task.getName().equalsIgnoreCase(target))
            {
                return task.getName();
            }
        }

        // Description match — some widget columns render the task description
        for (DemonicPactsTask task : TaskDatabase.getAllTasks())
        {
            String desc = task.getDescription();
            if (desc != null && desc.equalsIgnoreCase(target))
            {
                return task.getName();
            }
        }

        // Fuzzy contains — lets minor wording drift (trailing period already
        // stripped in cleanTaskName, "the"/"a" omissions, etc.) still register
        for (DemonicPactsTask task : TaskDatabase.getAllTasks())
        {
            String nameLower = task.getName().toLowerCase();
            if (nameLower.length() < 4)
            {
                continue; // too short — high risk of false positives
            }
            if (targetLower.equals(nameLower)
                || targetLower.contains(nameLower)
                || nameLower.contains(targetLower))
            {
                return task.getName();
            }
        }

        return null;
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
        newlySyncedSinceLastDrain.clear();
    }

    public int getCompletedCount()
    {
        return completedTaskNames.size();
    }

    /**
     * Returns the set of tasks discovered via widget sync since the last call
     * and atomically clears the buffer. Used by the plugin to emit a single
     * "synced N tasks" chat message per batch without double-counting.
     */
    public Set<String> drainNewlySynced()
    {
        if (newlySyncedSinceLastDrain.isEmpty())
        {
            return java.util.Collections.emptySet();
        }
        Set<String> copy = new HashSet<>(newlySyncedSinceLastDrain);
        newlySyncedSinceLastDrain.clear();
        return copy;
    }
}
