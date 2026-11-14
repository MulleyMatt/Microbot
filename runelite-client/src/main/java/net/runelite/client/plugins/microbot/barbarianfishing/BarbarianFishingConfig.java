package net.runelite.client.plugins.microbot.barbarianfishing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup(BarbarianFishingConfig.GROUP)
public interface BarbarianFishingConfig extends Config {
    String GROUP = "barbarianfishinghelper";

    @ConfigSection(
            name = "Fishing Spot Highlighting",
            description = "Settings for marking Barbarian fishing spots in the scene",
            position = 0
    )
    String spotSection = "spotSection";

    @ConfigSection(
            name = "Left-Click Drop & Inventory",
            description = "Settings for left-click dropping leaping fish and full inventory alerts",
            position = 1
    )
    String dropSection = "dropSection";

    @ConfigSection(
            name = "AFK Notifications & Screen Flash",
            description = "Settings for alerting when your character stops fishing and becomes inactive",
            position = 2
    )
    String afkSection = "afkSection";

    @ConfigSection(
            name = "Special Attack Alert",
            description = "Settings for alerting when your harpoon special attack is at 100% and ready to use",
            position = 3
    )
    String specSection = "specSection";

    @ConfigSection(
            name = "Sound Alerts",
            description = "Settings for audio alerts, volume, and custom sound effects",
            position = 4
    )
    String soundSection = "soundSection";

    @ConfigSection(
            name = "HUD & Session Timer",
            description = "On-screen information panel, XP/hr, fish/hr, and session timer",
            position = 5
    )
    String overlaySection = "overlaySection";

    @ConfigSection(
            name = "Grand Exchange Helper",
            description = "Grand Exchange search filter for Barbarian fishing bait & gear",
            position = 6
    )
    String geSection = "geSection";

    // --- Fishing Spot Highlighting Section ---

    @ConfigItem(
            keyName = "highlightFishingSpots",
            name = "Highlight Fishing Spots",
            description = "Highlights Barbarian fishing spots in the scene",
            section = spotSection,
            position = 0
    )
    default boolean highlightFishingSpots() {
        return true;
    }

    @ConfigItem(
            keyName = "spotHighlightColor",
            name = "Available Spot Color",
            description = "Color used to highlight available Barbarian fishing spots",
            section = spotSection,
            position = 1
    )
    default Color spotHighlightColor() {
        return new Color(0, 255, 128);
    }

    @ConfigItem(
            keyName = "activeSpotHighlightColor",
            name = "Active Spot Color",
            description = "Color used to highlight the fishing spot you are currently fishing",
            section = spotSection,
            position = 2
    )
    default Color activeSpotHighlightColor() {
        return new Color(255, 215, 0);
    }

    @ConfigItem(
            keyName = "drawClickbox",
            name = "Draw Spot Tile / Hull",
            description = "Render interactive click polygons around Barbarian fishing spots",
            section = spotSection,
            position = 3
    )
    default boolean drawClickbox() {
        return true;
    }

    @ConfigItem(
            keyName = "drawModelOutline",
            name = "Draw Model Outline",
            description = "Render glowing model outlines around Barbarian fishing spots",
            section = spotSection,
            position = 4
    )
    default boolean drawModelOutline() {
        return true;
    }

    @ConfigItem(
            keyName = "drawActionText",
            name = "Draw Action Prompts",
            description = "Render floating 'FISH' / 'FISHING' text prompts above spots",
            section = spotSection,
            position = 5
    )
    default boolean drawActionText() {
        return true;
    }

    // --- Left-Click Drop & Inventory Section ---

    @ConfigItem(
            keyName = "leftClickDropFish",
            name = "Left-Click Drop Leaping Fish",
            description = "Always sets the left-click option to Drop on Leaping trout, Leaping salmon, and Leaping sturgeon while this plugin is active",
            section = dropSection,
            position = 0
    )
    default boolean leftClickDropFish() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightInventoryFish",
            name = "Highlight Fish When Full",
            description = "Highlights leaping fish in your inventory with 'DROP' when your inventory is full",
            section = dropSection,
            position = 1
    )
    default boolean highlightInventoryFish() {
        return true;
    }

    @ConfigItem(
            keyName = "flashOnInventoryFull",
            name = "Flash on Full Inventory",
            description = "Flashes the screen blue when your inventory is completely full",
            section = dropSection,
            position = 2
    )
    default boolean flashOnInventoryFull() {
        return true;
    }

    @ConfigItem(
            keyName = "inventoryFullFlashColor",
            name = "Full Inventory Flash Color",
            description = "Color to flash the screen when inventory is full",
            section = dropSection,
            position = 3
    )
    default Color inventoryFullFlashColor() {
        return new Color(0, 120, 255, 75);
    }

    @ConfigItem(
            keyName = "playInventoryFullSound",
            name = "Play Full Inventory Sound Alert",
            description = "Plays the alert sound effect when your inventory is full",
            section = dropSection,
            position = 4
    )
    default boolean playInventoryFullSound() {
        return true;
    }

    // --- AFK Notifications & Screen Flash Section ---

    @ConfigItem(
            keyName = "flashOnAfk",
            name = "Flash Screen on AFK",
            description = "Flashes the screen red when your character stops fishing and is inactive",
            section = afkSection,
            position = 0
    )
    default boolean flashOnAfk() {
        return true;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(
            keyName = "afkDelaySeconds",
            name = "AFK Inactivity Delay (s)",
            description = "Number of seconds after fishing stops before the AFK alert triggers",
            section = afkSection,
            position = 1
    )
    default int afkDelaySeconds() {
        return 2;
    }

    @ConfigItem(
            keyName = "afkFlashColor",
            name = "AFK Flash Color",
            description = "Color to flash the screen when your character goes AFK",
            section = afkSection,
            position = 2
    )
    default Color afkFlashColor() {
        return new Color(255, 0, 0, 75);
    }

    @ConfigItem(
            keyName = "afkFlashUntilInteraction",
            name = "Flash Until Interaction",
            description = "When enabled, flashes continuously until you click a fishing spot or drop fish. If disabled, flashes for 2.5 seconds.",
            section = afkSection,
            position = 3
    )
    default boolean afkFlashUntilInteraction() {
        return true;
    }

    @ConfigItem(
            keyName = "flashStyle",
            name = "Flash Visual Style",
            description = "Choose visual flash animation: smooth Pulse, alternating Flash, or Solid",
            section = afkSection,
            position = 4
    )
    default BarbarianFishingFlashStyle flashStyle() {
        return BarbarianFishingFlashStyle.PULSE;
    }

    @ConfigItem(
            keyName = "playAfkSound",
            name = "Play AFK Sound Alert",
            description = "Plays the alert sound effect when your character stops fishing and goes inactive",
            section = afkSection,
            position = 5
    )
    default boolean playAfkSound() {
        return true;
    }

    // --- Special Attack Section ---

    @ConfigItem(
            keyName = "flashOnSpecialAttack",
            name = "Flash on 100% Spec",
            description = "Flashes the screen yellow when your Dragon/Crystal/Infernal harpoon special attack is at 100%",
            section = specSection,
            position = 0
    )
    default boolean flashOnSpecialAttack() {
        return true;
    }

    @ConfigItem(
            keyName = "specialAttackFlashColor",
            name = "Special Attack Flash Color",
            description = "Color to flash the screen when harpoon special attack is 100%",
            section = specSection,
            position = 1
    )
    default Color specialAttackFlashColor() {
        return new Color(255, 220, 0, 75);
    }

    @ConfigItem(
            keyName = "specFlashUntilUsed",
            name = "Flash Until Spec Used",
            description = "Flashes continuously until special attack is used. When disabled, flashes for 3 seconds.",
            section = specSection,
            position = 2
    )
    default boolean specFlashUntilUsed() {
        return true;
    }

    @ConfigItem(
            keyName = "playSpecialAttackSound",
            name = "Play Special Attack Sound Alert",
            description = "Plays the alert sound effect when the harpoon special attack reaches 100%",
            section = specSection,
            position = 3
    )
    default boolean playSpecialAttackSound() {
        return true;
    }

    // --- Sound Alerts Section ---

    @Range(min = 1, max = 100)
    @ConfigItem(
            keyName = "soundVolume",
            name = "Sound Volume (%)",
            description = "Volume level for alert sound effects (1 - 100%)",
            section = soundSection,
            position = 0
    )
    default int soundVolume() {
        return 80;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "soundIntervalSeconds",
            name = "Sound Repeat Interval (s)",
            description = "Seconds between alert sounds while an alert is active. Set to 0 to play only once when the alert triggers.",
            section = soundSection,
            position = 1
    )
    default int soundIntervalSeconds() {
        return 3;
    }

    @ConfigItem(
            keyName = "customSoundPath",
            name = "Custom Sound File",
            description = "Optional path to a custom .mp3 or .wav audio file. Leave blank to use the built-in alert sound effect.",
            section = soundSection,
            position = 2
    )
    default String customSoundPath() {
        return "";
    }

    // --- HUD & Session Timer Section ---

    @ConfigItem(
            keyName = "showOverlayPanel",
            name = "Show HUD Info Panel",
            description = "Displays current fishing state, supplies, AFK timer, XP/hr, and fish/hr",
            section = overlaySection,
            position = 0
    )
    default boolean showOverlayPanel() {
        return true;
    }

    @ConfigItem(
            keyName = "showSessionTime",
            name = "Show Session Time",
            description = "Displays the total session timer in the HUD info panel",
            section = overlaySection,
            position = 1
    )
    default boolean showSessionTime() {
        return true;
    }

    @ConfigItem(
            keyName = "showXpAndRates",
            name = "Show XP & Catch Rates",
            description = "Displays Fishing XP gained, XP/hr, and fish caught/hr in the HUD panel",
            section = overlaySection,
            position = 2
    )
    default boolean showXpAndRates() {
        return true;
    }

    // --- Grand Exchange Section ---

    @ConfigItem(
            keyName = "enableGeQuickFilter",
            name = "GE Quick-Click Filter",
            description = "Adds a quick-click button to the Grand Exchange search bar to view and buy Barbarian fishing supplies",
            section = geSection,
            position = 0
    )
    default boolean enableGeQuickFilter() {
        return true;
    }
}
