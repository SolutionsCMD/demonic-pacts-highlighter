package com.demonicpacts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks completed tasks per RuneScape account using RuneLite's
 * RSProfile config, which automatically scopes data to the
 * currently logged-in player.
 */
@Slf4j
@Singleton
public class CompletedTaskManager
{
    private static final String CONFIG_GROUP = "demonicpactstaskhighlighter";
    private static final String CONFIG_KEY = "completedTasks";

    private final ConfigManager configManager;
    private final Set<String> completedTasks = new HashSet<>();

    @Inject
    public CompletedTaskManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    /**
     * Load completed tasks for the current RS profile.
     * Called on login / plugin startup when logged in.
     */
    public void loadForCurrentProfile()
    {
        completedTasks.clear();
        String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (saved != null && !saved.isEmpty())
        {
            for (String taskName : saved.split("\\|"))
            {
                String trimmed = taskName.trim();
                if (!trimmed.isEmpty())
                {
                    completedTasks.add(trimmed);
                }
            }
        }
        log.debug("Loaded {} completed tasks for current profile", completedTasks.size());
    }

    /**
     * Clear in-memory state on logout.
     */
    public void onLogout()
    {
        completedTasks.clear();
        log.debug("Cleared in-memory completed tasks (logout)");
    }

    private void save()
    {
        String joined = String.join("|", completedTasks);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, joined);
    }

    public boolean isCompleted(String taskName)
    {
        return completedTasks.contains(taskName);
    }

    public boolean isCompleted(DemonicPactsTask task)
    {
        return completedTasks.contains(task.getName());
    }

    public void markCompleted(String taskName)
    {
        if (completedTasks.add(taskName))
        {
            save();
            log.debug("Marked task completed: {}", taskName);
        }
    }

    public void markIncomplete(String taskName)
    {
        if (completedTasks.remove(taskName))
        {
            save();
            log.debug("Marked task incomplete: {}", taskName);
        }
    }

    public void toggleCompleted(String taskName)
    {
        if (completedTasks.contains(taskName))
        {
            markIncomplete(taskName);
        }
        else
        {
            markCompleted(taskName);
        }
    }

    public void clearAll()
    {
        completedTasks.clear();
        save();
        log.debug("Cleared all completed tasks for current profile");
    }

    public Set<String> getCompletedTasks()
    {
        return Collections.unmodifiableSet(completedTasks);
    }

    public int getCompletedCount()
    {
        return completedTasks.size();
    }

    public boolean allCompleted(List<DemonicPactsTask> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return false;
        }
        for (DemonicPactsTask task : tasks)
        {
            if (!isCompleted(task))
            {
                return false;
            }
        }
        return true;
    }

    public List<DemonicPactsTask> filterIncomplete(List<DemonicPactsTask> tasks)
    {
        List<DemonicPactsTask> result = new ArrayList<>();
        for (DemonicPactsTask task : tasks)
        {
            if (!isCompleted(task))
            {
                result.add(task);
            }
        }
        return result;
    }
}
