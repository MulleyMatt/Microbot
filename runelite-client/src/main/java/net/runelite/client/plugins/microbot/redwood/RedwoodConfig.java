package net.runelite.client.plugins.microbot.redwood;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup(RedwoodConfig.GROUP)
public interface RedwoodConfig extends Config {
    String GROUP = "redwoodhelper";

    @ConfigSection(
            name = "Tree Spot Highlighting",
            description = "Settings for marking redwood tree click spots in the scene",
            position = 0
    )
    String treeSection = "treeSection";

    @ConfigSection(
            name = "AFK Notifications & Screen Flash",
            description = "Settings for alerting when your character stops woodcutting and becomes inactive",
            position = 1
    )
    String afkSection = "afkSection";

    @ConfigSection(
            name = "Inventory Full & Banking Alert",
            description = "Settings for alerting when inventory fills up and banking is required",
            position = 2
    )
    String inventorySection = "inventorySection";

    @ConfigSection(
            name = "Special Attack Alert",
            description = "Settings for alerting when your dragon axe special attack is at 100% and ready to use",
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
            description = "On-screen information panel and woodcutting timer",
            position = 5
    )
    String overlaySection = "overlaySection";

    @ConfigSection(
            name = "Grand Exchange Helper",
            description = "Grand Exchange search filter for woodcutting axes & gear",
            position = 6
    )
    String geSection = "geSection";

    // --- Tree Spot Highlighting Section ---

    @ConfigItem(
            keyName = "highlightTreeSpots",
            name = "Highlight Ready Spots",
            description = "Highlights redwood tree spots that are ready to be chopped",
            section = treeSection,
            position = 0
    )
    default boolean highlightTreeSpots() {
        return true;
    }

    @ConfigItem(
            keyName = "treeHighlightColor",
            name = "Ready Spot Color",
            description = "Color used to highlight available redwood spots",
            section = treeSection,
            position = 1
    )
    default Color treeHighlightColor() {
        return new Color(0, 255, 128);
    }

    @ConfigItem(
            keyName = "activeTreeHighlightColor",
            name = "Active Chop Spot Color",
            description = "Color used to highlight the tree spot currently being cut",
            section = treeSection,
            position = 2
    )
    default Color activeTreeHighlightColor() {
        return new Color(255, 215, 0);
    }

    @ConfigItem(
            keyName = "highlightDepletedTrees",
            name = "Highlight Depleted Spots",
            description = "Highlights depleted redwood spots so you know which spots are on cooldown",
            section = treeSection,
            position = 3
    )
    default boolean highlightDepletedTrees() {
        return true;
    }

    @ConfigItem(
            keyName = "depletedTreeColor",
            name = "Depleted Spot Color",
            description = "Color used to mark depleted redwood spots",
            section = treeSection,
            position = 4
    )
    default Color depletedTreeColor() {
        return new Color(220, 50, 50, 160);
    }

    @ConfigItem(
            keyName = "drawClickbox",
            name = "Draw 3D Clickbox",
            description = "Render interactive 3D clickboxes around tree spots",
            section = treeSection,
            position = 5
    )
    default boolean drawClickbox() {
        return true;
    }

    @ConfigItem(
            keyName = "drawModelOutline",
            name = "Draw Model Outline",
            description = "Render glowing model outlines around tree spots",
            section = treeSection,
            position = 6
    )
    default boolean drawModelOutline() {
        return true;
    }

    @ConfigItem(
            keyName = "drawActionText",
            name = "Draw Action Prompts",
            description = "Render floating 'CHOP' text prompts above ready spots",
            section = treeSection,
            position = 7
    )
    default boolean drawActionText() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightLaddersAndBank",
            name = "Highlight Ladders & Bank",
            description = "Highlight rope ladders and the Woodcutting Guild bank chest when transitioning levels or banking",
            section = treeSection,
            position = 8
    )
    default boolean highlightLaddersAndBank() {
        return true;
    }

    // --- AFK Notifications & Screen Flash Section ---

    @ConfigItem(
            keyName = "flashOnAfk",
            name = "Flash Screen on AFK",
            description = "Flashes the screen red when your character stops chopping and is inactive",
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
            description = "Number of seconds after woodcutting stops before the red AFK flash triggers (prevents flicker on chop rolls)",
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
            description = "When enabled, flashes continuously until mouse/keyboard interaction or a new chop action begins. If disabled, flashes for 2.5 seconds.",
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
    default FlashStyle flashStyle() {
        return FlashStyle.PULSE;
    }

    @ConfigItem(
            keyName = "playAfkSound",
            name = "Play AFK Sound Alert",
            description = "Plays the alert sound effect when the AFK screen flash is happening",
            section = afkSection,
            position = 5
    )
    default boolean playAfkSound() {
        return true;
    }

    // --- Inventory Full Section ---

    @ConfigItem(
            keyName = "flashOnInventoryFull",
            name = "Flash on Full Inventory",
            description = "Flashes the screen blue when your inventory is completely full and logs need to be banked",
            section = inventorySection,
            position = 0
    )
    default boolean flashOnInventoryFull() {
        return true;
    }

    @ConfigItem(
            keyName = "inventoryFullFlashColor",
            name = "Full Inventory Flash Color",
            description = "Color to flash the screen when inventory is full",
            section = inventorySection,
            position = 1
    )
    default Color inventoryFullFlashColor() {
        return new Color(0, 120, 255, 75);
    }

    @ConfigItem(
            keyName = "highlightInventoryLogs",
            name = "Highlight Logs in Inventory",
            description = "Highlights redwood logs in inventory when inventory is full",
            section = inventorySection,
            position = 2
    )
    default boolean highlightInventoryLogs() {
        return true;
    }

    @ConfigItem(
            keyName = "playInventoryFullSound",
            name = "Play Full Inventory Sound Alert",
            description = "Plays the alert sound effect when the inventory full screen flash is happening",
            section = inventorySection,
            position = 3
    )
    default boolean playInventoryFullSound() {
        return true;
    }

    @ConfigItem(
            keyName = "dismissInventoryFlashOnMovement",
            name = "Stop Alert When Moving to Bank",
            description = "Stops the blue screen flash and sound alert once you start moving or heading to the bank",
            section = inventorySection,
            position = 4
    )
    default boolean dismissInventoryFlashOnMovement() {
        return true;
    }

    // --- Special Attack Section ---

    @ConfigItem(
            keyName = "flashOnSpecialAttack",
            name = "Flash on 100% Spec",
            description = "Flashes the screen yellow when your dragon axe special attack is at 100% and ready to use",
            section = specSection,
            position = 0
    )
    default boolean flashOnSpecialAttack() {
        return true;
    }

    @ConfigItem(
            keyName = "specialAttackFlashColor",
            name = "Special Attack Flash Color",
            description = "Color to flash the screen when dragon axe special attack is 100%",
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
            description = "Plays the alert sound effect when the special attack screen flash is happening",
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
            description = "Optional path to a custom .mp3 or .wav audio file. Leave blank to use the built-in Redwood alert sound effect.",
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
            description = "Displays current woodcutting state, AFK timer, and step instructions",
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

    @Deprecated
    default boolean showSessionStats() {
        return showSessionTime();
    }

    // --- Grand Exchange Section ---

    @ConfigItem(
            keyName = "enableGeQuickFilter",
            name = "GE Quick-Click Filter",
            description = "Adds a quick-click button to the Grand Exchange search bar to view and buy woodcutting axes & gear",
            section = geSection,
            position = 0
    )
    default boolean enableGeQuickFilter() {
        return true;
    }
}
