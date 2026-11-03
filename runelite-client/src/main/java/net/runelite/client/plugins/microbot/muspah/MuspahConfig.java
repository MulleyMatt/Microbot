package net.runelite.client.plugins.microbot.muspah;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.plugins.microbot.util.magic.thralls.ThrallType;

import java.awt.Color;

@ConfigGroup("muspahhelper")
public interface MuspahConfig extends Config
{
    @ConfigSection(
            name = "Phantom Muspah",
            description = "General rendering and font settings for Phantom Muspah overlays",
            position = 0,
            closedByDefault = true
    )
    String muspahSection = "muspahSection";

    @ConfigSection(
            name = "Fight & Prayer Helper",
            description = "Prayer helper panel, prayer tab widget markers, Smite prompts, and kiting distance border",
            position = 1
    )
    String fightHelperSection = "fightHelperSection";

    @ConfigSection(
            name = "Phase & Boss Helper",
            description = "Carapace form HUD, phase hats, weapon reminders (Bowfa / Sapphire Bolts), and tick counters",
            position = 2
    )
    String phaseHelperSection = "phaseHelperSection";

    @ConfigSection(
            name = "Shockwave & Hazards",
            description = "Shockwave safe tiles behind spikes, floor spikes, homing spikes, and shadow orbs",
            position = 3
    )
    String hazardsSection = "hazardsSection";

    @ConfigSection(
            name = "Alerts & Audio",
            description = "Optional screen flash and sound alerts for Charged Magic and Center Shockwave",
            position = 4
    )
    String alertsSection = "alertsSection";

    // =========================================================================
    // Section 0: General Display Settings (matching ZulrahConfig)
    // =========================================================================

    @ConfigItem(
            name = "Font Type",
            keyName = "fontType",
            description = "Configure the font for the plugin's scene overlays",
            position = 0,
            section = muspahSection
    )
    default FontType fontType()
    {
        return FontType.SMALL;
    }

    @ConfigItem(
            name = "Text Outline",
            keyName = "textOutline",
            description = "Use an outline around text instead of a drop shadow",
            position = 1,
            section = muspahSection
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
            description = "Configures the stroke width of scene tile polygons",
            position = 2,
            section = muspahSection
    )
    default int outlineWidth()
    {
        return 2;
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            name = "Fill Alpha",
            keyName = "fillAlpha",
            description = "Configures the fill opacity of scene tile polygons",
            position = 3,
            section = muspahSection
    )
    default int fillAlpha()
    {
        return 25;
    }

    @ConfigItem(
            name = "Kill Timer",
            keyName = "instanceTimer",
            description = "Displays an overlay timer tracking the duration of the current Muspah fight",
            position = 4,
            section = muspahSection
    )
    default boolean instanceTimer()
    {
        return true;
    }

    // =========================================================================
    // Section 1: Fight & Prayer Helper
    // =========================================================================

    @ConfigItem(
            name = "Auto-Switch Overhead Prayers",
            keyName = "autoSwitchPrayers",
            description = "Automatically switches protection prayers (Melee on Brown, Missiles on Green/Teal, and Magic when Muspah charges purple energy)",
            position = 0,
            section = fightHelperSection
    )
    default boolean autoSwitchPrayers()
    {
        return true;
    }

    @ConfigItem(
            name = "Auto-Flick Smite (Shield Phase)",
            keyName = "autoSmiteShieldPhase",
            description = "When Auto-Switch Prayers is enabled, automatically flicks Smite between Muspah's attacks during the Teal Prayer Shield phase",
            position = 1,
            section = fightHelperSection
    )
    default boolean autoSmiteShieldPhase()
    {
        return false;
    }

    @ConfigItem(
            name = "Auto Offensive Ranged Prayer",
            keyName = "autoOffensivePrayer",
            description = "Automatically activates your best unlocked Ranged offensive prayer (Rigour / Deadeye / Eagle Eye) during the fight",
            position = 2,
            section = fightHelperSection
    )
    default boolean autoOffensivePrayer()
    {
        return false;
    }

    @ConfigItem(
            name = "Auto-Disable Prayers After Kill",
            keyName = "autoDisablePrayersOnKill",
            description = "Automatically turns off active prayers when Phantom Muspah dies",
            position = 3,
            section = fightHelperSection
    )
    default boolean autoDisablePrayersOnKill()
    {
        return true;
    }

    @ConfigItem(
            name = "Auto-Cast Thralls",
            keyName = "autoCastThrall",
            description = "Automatically summons your Thrall on cooldown when fighting Phantom Muspah (requires Arceuus spellbook, Book of the dead, and runes)",
            position = 4,
            section = fightHelperSection
    )
    default boolean autoCastThrall()
    {
        return false;
    }

    @ConfigItem(
            name = "Thrall Style",
            keyName = "thrallStyle",
            description = "Which Thrall style to summon when Auto-Cast Thralls is enabled (automatically uses the highest tier unlocked)",
            position = 5,
            section = fightHelperSection
    )
    default ThrallStyle thrallStyle()
    {
        return ThrallStyle.RANGED;
    }

    @ConfigItem(
            name = "Prayer Helper",
            keyName = "prayerHelper",
            description = "Displays an overlay panel showing the correct overhead prayer (auto-switches to Protect from Magic on charged purple attack)",
            position = 6,
            section = fightHelperSection
    )
    default boolean prayerHelper()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Marker",
            keyName = "prayerMarker",
            description = "Marks the correct prayer to use directly inside the Prayer book tab (Red = off, Green = active)",
            position = 7,
            section = fightHelperSection
    )
    default boolean prayerMarker()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Shield Smite Prompt",
            keyName = "smiteHelper",
            description = "During Teal (Prayer Shield) phase, shows Smite icon in the Prayer Helper and marks Smite in the Prayer tab",
            position = 8,
            section = fightHelperSection
    )
    default boolean smiteHelper()
    {
        return true;
    }

    @ConfigItem(
            name = "Melee Danger-Range Border",
            keyName = "meleeDangerRange",
            description = "During Brown (Melee) form, draws a melee danger border around Muspah (Red when in melee range, Green when at safe kiting distance)",
            position = 9,
            section = fightHelperSection
    )
    default boolean meleeDangerRange()
    {
        return true;
    }

    @ConfigItem(
            name = "Attack Tick Counter",
            keyName = "attackTickCounter",
            description = "Displays a tick counter over Muspah showing ticks until its next attack",
            position = 10,
            section = fightHelperSection
    )
    default boolean attackTickCounter()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "Tick Counter Color",
            keyName = "tickCounterColors",
            description = "Color of the attack/special tick counter text over Muspah",
            position = 11,
            section = fightHelperSection
    )
    default Color tickCounterColors()
    {
        return Color.WHITE;
    }

    @ConfigItem(
            name = "Prayer Conservation",
            keyName = "prayerConservation",
            description = "Displays text over your head reminding you to turn off overheads when Muspah dies or is absent",
            position = 12,
            section = fightHelperSection
    )
    default boolean prayerConservation()
    {
        return true;
    }

    // =========================================================================
    // Section 2: Phase & Boss Helper
    // =========================================================================

    @ConfigItem(
            name = "Phase Display Type",
            keyName = "phaseDisplayType",
            description = "Overlay: Shows form/weapon info in HUD panel; Tile: Shows on Muspah's tile; Both: Shows on both",
            position = 0,
            section = phaseHelperSection
    )
    default DisplayType phaseDisplayType()
    {
        return DisplayType.BOTH;
    }

    @ConfigItem(
            name = "Phase Display Mode",
            keyName = "phaseDisplayMode",
            description = "Show Current form only, Next form only, or Both Current and Next forms",
            position = 1,
            section = phaseHelperSection
    )
    default DisplayMode phaseDisplayMode()
    {
        return DisplayMode.BOTH;
    }

    @ConfigItem(
            name = "Phase Hats",
            keyName = "phaseHats",
            description = "Displays the Ranged attack style skill icon over Muspah on the scene tile",
            position = 2,
            section = phaseHelperSection
    )
    default boolean phaseHats()
    {
        return true;
    }

    @ConfigItem(
            name = "Phase & Weapon Tags",
            keyName = "phaseTags",
            description = "Tags Muspah with form & Ranged weapon reminder (Bowfa vs Sapphire Bolts + Smite)",
            position = 3,
            section = phaseHelperSection
    )
    default boolean phaseTags()
    {
        return true;
    }

    @ConfigItem(
            name = "Display Muspah Tile",
            keyName = "displayMuspahTile",
            description = "Highlights Phantom Muspah's 5x5 tile footprint colored by its current carapace form",
            position = 4,
            section = phaseHelperSection
    )
    default boolean displayMuspahTile()
    {
        return true;
    }

    // =========================================================================
    // Section 3: Shockwave & Hazards
    // =========================================================================

    @ConfigItem(
            name = "Shockwave Safe Tiles",
            keyName = "shockwaveSafespots",
            description = "When Muspah teleports to the center for the shockwave, highlights the safe tiles shielded behind floor spikes",
            position = 0,
            section = hazardsSection
    )
    default boolean shockwaveSafespots()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "Safe Tile Color",
            keyName = "safeTileColor",
            description = "Highlight color for safe tiles behind spikes during the center shockwave",
            position = 1,
            section = hazardsSection
    )
    default Color safeTileColor()
    {
        return Color.GREEN;
    }

    @ConfigItem(
            name = "Highlight Floor & Homing Spikes",
            keyName = "highlightSpikes",
            description = "Highlights incoming spike ground warnings, form-change player-tile spike, stationary spikes, and homing spikes",
            position = 2,
            section = hazardsSection
    )
    default boolean highlightSpikes()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "Spike Warning Color",
            keyName = "spikeWarningColor",
            description = "Highlight color for incoming/homing spike hazards",
            position = 3,
            section = hazardsSection
    )
    default Color spikeWarningColor()
    {
        return new Color(255, 69, 0);
    }

    @ConfigItem(
            name = "Highlight Shadow Orbs",
            keyName = "highlightShadowOrbs",
            description = "Highlights the wandering shadow orbs during the dark-screen phase",
            position = 4,
            section = hazardsSection
    )
    default boolean highlightShadowOrbs()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "Shadow Orb Color",
            keyName = "shadowOrbColor",
            description = "Highlight color for wandering shadow orbs",
            position = 5,
            section = hazardsSection
    )
    default Color shadowOrbColor()
    {
        return new Color(186, 85, 211);
    }

    // =========================================================================
    // Section 4: Alerts & Audio
    // =========================================================================

    @ConfigItem(
            name = "Charged Magic Screen Flash",
            keyName = "magicAttackFlash",
            description = "Pulses the screen purple/red when Muspah charges its Magic corruption attack and Protect from Magic is not active",
            position = 0,
            section = alertsSection
    )
    default boolean magicAttackFlash()
    {
        return true;
    }

    @ConfigItem(
            name = "Shockwave Screen Flash",
            keyName = "shockwaveFlash",
            description = "Pulses the screen orange/red when Muspah charges its center Shockwave until you stand on a safe tile behind a spike",
            position = 1,
            section = alertsSection
    )
    default boolean shockwaveFlash()
    {
        return true;
    }

    @ConfigItem(
            name = "Sound Alerts",
            keyName = "soundAlerts",
            description = "Plays an audio alert when Muspah charges its purple Magic attack or teleports to center for the Shockwave",
            position = 2,
            section = alertsSection
    )
    default boolean soundAlerts()
    {
        return true;
    }

    @ConfigItem(
            name = "Low Prayer Sound Alert",
            keyName = "lowPrayerSoundAlert",
            description = "Plays an audio alert during the fight when your Prayer points drop to or below the Low Prayer Threshold",
            position = 3,
            section = alertsSection
    )
    default boolean lowPrayerSoundAlert()
    {
        return true;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(
            name = "Low Prayer Threshold",
            keyName = "lowPrayerThreshold",
            description = "Prayer points threshold at which the low prayer sound alert triggers (default: 10)",
            position = 4,
            section = alertsSection
    )
    default int lowPrayerThreshold()
    {
        return 10;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            name = "Sound Volume",
            keyName = "soundVolume",
            description = "Volume for the audio alert notifications (0-100%)",
            position = 5,
            section = alertsSection
    )
    default int soundVolume()
    {
        return 75;
    }

    @Getter
    @RequiredArgsConstructor
    enum DisplayType
    {
        OFF("Off"),
        OVERLAY("Overlay"),
        TILE("Tile"),
        BOTH("Both");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum DisplayMode
    {
        CURRENT("Current"),
        NEXT("Next"),
        BOTH("Both");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum ThrallStyle
    {
        RANGED("Ranged (Skeleton)", ThrallType.RANGED),
        MAGIC("Magic (Ghost)", ThrallType.MAGIC),
        MELEE("Melee (Zombie)", ThrallType.MELEE);

        private final String name;
        private final ThrallType thrallType;

        @Override
        public String toString()
        {
            return name;
        }
    }
}
