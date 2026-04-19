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
 * Tracks tasks the player has explicitly hidden via the right-click menu.
 * Hidden tasks are filtered out of all highlights and tooltips, similar to
 * completed tasks but managed separately so the player can choose to skip a
 * task without pretending they finished it.
 *
 * Persisted per RuneScape account via ConfigManager's RSProfile scope.
 */
@Slf4j
@Singleton
public class HiddenTaskManager
{
    private static final String CONFIG_GROUP = "demonicpactstaskhighlighter";
    private static final String CONFIG_KEY = "hiddenTasks";

    private final ConfigManager configManager;
    private final Set<String> hiddenTasks = new HashSet<>();

    @Inject
    public HiddenTaskManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public void loadForCurrentProfile()
    {
        hiddenTasks.clear();
        String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (saved != null && !saved.isEmpty())
        {
            for (String taskName : saved.split("\\|"))
            {
                String trimmed = taskName.trim();
                if (!trimmed.isEmpty())
                {
                    hiddenTasks.add(trimmed);
                }
            }
        }
        log.debug("Loaded {} hidden tasks for current profile", hiddenTasks.size());
    }

    public void onLogout()
    {
        hiddenTasks.clear();
    }

    private void save()
    {
        String joined = String.join("|", hiddenTasks);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, joined);
    }

    public boolean isHidden(String taskName)
    {
        return hiddenTasks.contains(taskName);
    }

    public boolean isHidden(DemonicPactsTask task)
    {
        return hiddenTasks.contains(task.getName());
    }

    public void hide(String taskName)
    {
        if (hiddenTasks.add(taskName))
        {
            save();
            log.debug("Hid task: {}", taskName);
        }
    }

    public void unhide(String taskName)
    {
        if (hiddenTasks.remove(taskName))
        {
            save();
            log.debug("Unhid task: {}", taskName);
        }
    }

    public void clearAll()
    {
        hiddenTasks.clear();
        save();
        log.debug("Cleared all hidden tasks for current profile");
    }

    public Set<String> getHiddenTasks()
    {
        return Collections.unmodifiableSet(hiddenTasks);
    }

    public int getHiddenCount()
    {
        return hiddenTasks.size();
    }

    /**
     * Returns only tasks that are NOT hidden.
     */
    public List<DemonicPactsTask> filterVisible(List<DemonicPactsTask> tasks)
    {
        List<DemonicPactsTask> result = new ArrayList<>();
        for (DemonicPactsTask task : tasks)
        {
            if (!isHidden(task))
            {
                result.add(task);
            }
        }
        return result;
    }
}
