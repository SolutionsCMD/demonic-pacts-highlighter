package com.demonicpacts;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Local test runner for the Demonic Pacts plugin.
 *
 * Right-click this class in IntelliJ and choose Run to launch a RuneLite
 * client with the plugin loaded directly from your local source — no
 * Plugin Hub publish, fork update, or commit hash required. Edit code,
 * stop the run, and run again to see the change.
 */
public class DemonicPactsPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(DemonicPactsPlugin.class);
        RuneLite.main(args);
    }
}
