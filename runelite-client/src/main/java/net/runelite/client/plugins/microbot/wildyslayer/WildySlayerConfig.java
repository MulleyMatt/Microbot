package net.runelite.client.plugins.microbot.wildyslayer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(WildySlayerConfig.GROUP)
public interface WildySlayerConfig extends Config
{
    String GROUP = "wildyslayerhelper";

    @ConfigSection(
            name = "Anti-PK & Royal Seed Pod",
            description = "Automatic Royal Seed Pod teleport when an attackable PKer is nearby at or below level 30 Wilderness",
            position = 0
    )
    String antiPkSection = "antiPkSection";

    @ConfigSection(
            name = "Display & Audio",
            description = "HUD panel, scene rendering styles, and sound alert settings",
            position = 1
    )
    String displayAudioSection = "displayAudioSection";

    // =========================================================================
    // Section 0: Anti-PK & Royal Seed Pod
    // =========================================================================

    @ConfigItem(
            name = "Auto Royal Seed Pod on PKer",
            keyName = "autoSeedPodOnPker",
            description = "Automatically activates your Royal seed pod the instant an attackable player is nearby at or below the max Wilderness level",
            position = 0,
            section = antiPkSection
    )
    default boolean autoSeedPodOnPker()
    {
        return true;
    }

    @Range(min = 1, max = 30)
    @ConfigItem(
            name = "Max Seed Pod Wilderness Level",
            keyName = "maxSeedPodWildernessLevel",
            description = "Maximum Wilderness level at which Auto Royal Seed Pod will trigger (Royal seed pod works up to level 30 Wilderness)",
            position = 1,
            section = antiPkSection
    )
    default int maxSeedPodWildernessLevel()
    {
        return 30;
    }

    @ConfigItem(
            name = "PKer Detection Mode",
            keyName = "pkerDetectionMode",
            description = "Trigger on players within your Wilderness combat level bracket (recommended) or any nearby player",
            position = 2,
            section = antiPkSection
    )
    default PkerDetectionMode pkerDetectionMode()
    {
        return PkerDetectionMode.COMBAT_BRACKET;
    }

    @ConfigItem(
            name = "Ignore Friends & Clan",
            keyName = "ignoreFriendsAndClan",
            description = "Do not trigger PKer alerts or auto-teleport for players on your Friends list, Clan, or Friends Chat",
            position = 3,
            section = antiPkSection
    )
    default boolean ignoreFriendsAndClan()
    {
        return true;
    }

    @ConfigItem(
            name = "PKer Screen Flash",
            keyName = "pkerScreenFlash",
            description = "Flashes the screen red when a threat player is detected nearby in the Wilderness",
            position = 4,
            section = antiPkSection
    )
    default boolean pkerScreenFlash()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Enemy Players",
            keyName = "highlightEnemyPlayers",
            description = "Highlights threat players in the Wilderness with their name and combat level",
            position = 5,
            section = antiPkSection
    )
    default boolean highlightEnemyPlayers()
    {
        return true;
    }

    // =========================================================================
    // Section 1: Display & Audio
    // =========================================================================

    @ConfigItem(
            name = "Show HUD Panel",
            keyName = "showHudOverlay",
            description = "Displays the Wilderness Slayer status HUD panel while in the Wilderness",
            position = 0,
            section = displayAudioSection
    )
    default boolean showHudOverlay()
    {
        return true;
    }

    @ConfigItem(
            name = "Sound Alerts on PKer",
            keyName = "soundAlerts",
            description = "Plays an audio alert when a threat player is detected in the Wilderness",
            position = 1,
            section = displayAudioSection
    )
    default boolean soundAlerts()
    {
        return true;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            name = "Sound Volume",
            keyName = "soundVolume",
            description = "Volume for audio alert notifications (0-100%)",
            position = 2,
            section = displayAudioSection
    )
    default int soundVolume()
    {
        return 75;
    }

    @ConfigItem(
            name = "Font Type",
            keyName = "fontType",
            description = "Font used for scene text labels",
            position = 3,
            section = displayAudioSection
    )
    default FontType fontType()
    {
        return FontType.SMALL;
    }

    @ConfigItem(
            name = "Text Outline",
            keyName = "textOutline",
            description = "Use an outline around scene text instead of a drop shadow",
            position = 4,
            section = displayAudioSection
    )
    default boolean textOutline()
    {
        return true;
    }

    @Range(min = 1, max = 10)
    @Units(Units.PIXELS)
    @ConfigItem(
            name = "Outline Width",
            keyName = "outlineWidth",
            description = "Stroke width of scene tile polygons",
            position = 5,
            section = displayAudioSection
    )
    default int outlineWidth()
    {
        return 2;
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            name = "Fill Alpha",
            keyName = "fillAlpha",
            description = "Fill opacity of scene tile polygons",
            position = 6,
            section = displayAudioSection
    )
    default int fillAlpha()
    {
        return 25;
    }

    // =========================================================================
    // Enums
    // =========================================================================

    @Getter
    @RequiredArgsConstructor
    enum PkerDetectionMode
    {
        COMBAT_BRACKET("Attackable Combat Bracket"),
        ANY_PLAYER("Any Nearby Player");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }
}
