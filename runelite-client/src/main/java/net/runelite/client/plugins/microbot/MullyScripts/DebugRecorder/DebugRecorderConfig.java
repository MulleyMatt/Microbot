package net.runelite.client.plugins.microbot.MullyScripts.DebugRecorder;

import net.runelite.client.config.*;

@ConfigGroup(DebugRecorderConfig.configGroup)
@ConfigInformation(
        "• This plugin records user data. <br />"
)
public interface DebugRecorderConfig extends Config {

    String configGroup = "DebugRecorderConfig";
}
