package net.runelite.client.plugins.microbot.bosshelper;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.bosshelper.model.TargetBoss;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Color;

@ConfigGroup("bosshelper")
public interface BossHelperConfig extends Config {

    @ConfigSection(
            name = "Server Settings",
            description = "Agent Server connection configuration",
            position = 0
    )
    String serverSection = "serverSection";

    @ConfigSection(
            name = "Encounter Settings",
            description = "Boss encounter and fallback behavior",
            position = 10
    )
    String encounterSection = "encounterSection";

    @ConfigSection(
            name = "Visuals & Overlays",
            description = "Visuals for prayer icons and canvas tile markers",
            position = 20
    )
    String visualsSection = "visualsSection";

    @ConfigItem(
            keyName = "agentServerUrl",
            name = "Agent Server URL",
            description = "Endpoint of the local Agent Server (port 8081)",
            section = serverSection,
            position = 1
    )
    default String agentServerUrl() {
        return "http://localhost:8081";
    }

    @ConfigItem(
            keyName = "targetBoss",
            name = "Target Boss",
            description = "Boss encounter mode: auto-detect or specific boss logic",
            section = encounterSection,
            position = 11
    )
    default TargetBoss targetBoss() {
        return TargetBoss.AUTO_DETECT;
    }

    @ConfigItem(
            keyName = "enableLocalFallback",
            name = "Enable Local Fallback",
            description = "Use local heuristic and tick-tracking if the Agent Server is unreachable",
            section = encounterSection,
            position = 12
    )
    default boolean enableLocalFallback() {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "safeTileColor",
            name = "Safe Tile Color",
            description = "Color of safe ground tile outlines and fills",
            section = visualsSection,
            position = 21
    )
    default Color safeTileColor() {
        return new Color(0, 255, 0, 90);
    }

    @Alpha
    @ConfigItem(
            keyName = "hazardTileColor",
            name = "Hazard Tile Color",
            description = "Color of dangerous hazard ground tiles",
            section = visualsSection,
            position = 22
    )
    default Color hazardTileColor() {
        return new Color(255, 0, 0, 90);
    }

    @Range(min = 24, max = 128)
    @ConfigItem(
            keyName = "prayerIconScale",
            name = "Prayer Icon Scale",
            description = "Scale size in pixels for the recommended prayer overlay icon",
            section = visualsSection,
            position = 23
    )
    default int prayerIconScale() {
        return 48;
    }

    @ConfigItem(
            keyName = "prayerOverlayPosition",
            name = "Prayer Overlay Position",
            description = "Screen position for the prayer overlay",
            section = visualsSection,
            position = 24
    )
    default OverlayPosition prayerOverlayPosition() {
        return OverlayPosition.DETACHED;
    }
}
