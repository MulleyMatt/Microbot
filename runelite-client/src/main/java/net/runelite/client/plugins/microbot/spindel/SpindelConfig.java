package net.runelite.client.plugins.microbot.spindel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import java.awt.Color;

@ConfigGroup(SpindelConfig.GROUP)
public interface SpindelConfig extends Config
{
    String GROUP = "spindelhelper";

    @ConfigSection(
            name = "Fight & Auto-Prayer",
            description = "Auto-prayer rotation (8 Ranged / 8 Magic), offensive prayers, and prayer helper overlays",
            position = 0
    )
    String fightPrayerSection = "fightPrayerSection";

    @ConfigSection(
            name = "Anti-PK & Royal Seed Pod",
            description = "Automatic Royal Seed Pod teleport when an attackable PKer enters Spindel's cave",
            position = 1
    )
    String antiPkSection = "antiPkSection";

    @ConfigSection(
            name = "Web Hazard & Spiderlings",
            description = "Expanding Web floor hazard highlights, safe escape tiles, and Spiderling highlights",
            position = 2
    )
    String hazardsSection = "hazardsSection";

    @ConfigSection(
            name = "Supply Prep (Bank & GE)",
            description = "1-click Bank & Grand Exchange filters and configurable trip supply quantities",
            position = 3
    )
    String supplyPrepSection = "supplyPrepSection";

    @ConfigSection(
            name = "Display & Audio",
            description = "HUD panel, scene rendering styles, and sound alert settings",
            position = 4,
            closedByDefault = true
    )
    String displayAudioSection = "displayAudioSection";

    // =========================================================================
    // Section 0: Fight & Auto-Prayer
    // =========================================================================

    @ConfigItem(
            name = "Auto-Switch Overhead Prayers",
            keyName = "autoSwitchPrayers",
            description = "Automatically switches between Protect from Missiles (8 Ranged attacks) and Protect from Magic (8 Magic attacks)",
            position = 0,
            section = fightPrayerSection
    )
    default boolean autoSwitchPrayers()
    {
        return true;
    }

    @ConfigItem(
            name = "Auto Offensive Prayer",
            keyName = "autoOffensivePrayer",
            description = "Automatically activates your best unlocked offensive prayer (Piety / Rigour / Augury) during the fight",
            position = 1,
            section = fightPrayerSection
    )
    default boolean autoOffensivePrayer()
    {
        return true;
    }

    @ConfigItem(
            name = "Offensive Prayer Style",
            keyName = "offensivePrayerStyle",
            description = "Combat style for Auto Offensive Prayer (Melee/Crush is standard for Spindel)",
            position = 2,
            section = fightPrayerSection
    )
    default OffensivePrayerStyle offensivePrayerStyle()
    {
        return OffensivePrayerStyle.MELEE;
    }

    @ConfigItem(
            name = "Auto-Disable Prayers After Kill",
            keyName = "autoDisablePrayersOnKill",
            description = "Automatically turns off active prayers when Spindel dies",
            position = 3,
            section = fightPrayerSection
    )
    default boolean autoDisablePrayersOnKill()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Helper Box",
            keyName = "prayerHelper",
            description = "Displays a floating box showing the required overhead prayer and remaining attacks before Spindel switches style",
            position = 4,
            section = fightPrayerSection
    )
    default boolean prayerHelper()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Tab Marker",
            keyName = "prayerMarker",
            description = "Highlights the required overhead prayer inside the Prayer book tab (Green = active, Red = off)",
            position = 5,
            section = fightPrayerSection
    )
    default boolean prayerMarker()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Conservation Reminder",
            keyName = "prayerConservation",
            description = "Displays a reminder over your character to turn off prayers after Spindel dies",
            position = 6,
            section = fightPrayerSection
    )
    default boolean prayerConservation()
    {
        return true;
    }

    // =========================================================================
    // Section 1: Anti-PK & Royal Seed Pod
    // =========================================================================

    @ConfigItem(
            name = "Auto Royal Seed Pod on PKer",
            keyName = "autoSeedPodOnPker",
            description = "Automatically activates your Royal seed pod the instant an attackable player enters Spindel's cave",
            position = 0,
            section = antiPkSection
    )
    default boolean autoSeedPodOnPker()
    {
        return true;
    }

    @ConfigItem(
            name = "PKer Detection Mode",
            keyName = "pkerDetectionMode",
            description = "Trigger on players within your Wilderness combat level bracket (recommended) or any player entering the cave",
            position = 1,
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
            position = 2,
            section = antiPkSection
    )
    default boolean ignoreFriendsAndClan()
    {
        return true;
    }

    @ConfigItem(
            name = "PKer Screen Flash",
            keyName = "pkerScreenFlash",
            description = "Flashes the screen red when a threat player enters the cave",
            position = 3,
            section = antiPkSection
    )
    default boolean pkerScreenFlash()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Enemy Players",
            keyName = "highlightEnemyPlayers",
            description = "Highlights threat players in the cave with their name and combat level",
            position = 4,
            section = antiPkSection
    )
    default boolean highlightEnemyPlayers()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Cave Exit",
            keyName = "highlightCaveExit",
            description = "Highlights the cave exit (urgent prompt if you are teleblocked or lack a Seed Pod)",
            position = 5,
            section = antiPkSection
    )
    default boolean highlightCaveExit()
    {
        return true;
    }

    // =========================================================================
    // Section 2: Web Hazard & Spiderlings
    // =========================================================================

    @ConfigItem(
            name = "Highlight Expanding Web Hazard",
            keyName = "highlightWebHazard",
            description = "Highlights Spindel's expanding floor webs and incoming web projectile landing zone",
            position = 0,
            section = hazardsSection
    )
    default boolean highlightWebHazard()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Safe Tiles Outside Web",
            keyName = "highlightWebSafeTiles",
            description = "When standing in or near the expanding web, highlights the closest safe tiles 2-3 tiles outside the web",
            position = 1,
            section = hazardsSection
    )
    default boolean highlightWebSafeTiles()
    {
        return true;
    }

    @ConfigItem(
            name = "Web Danger Screen Flash",
            keyName = "webDangerFlash",
            description = "Pulses the screen orange/red if you are standing on a web tile or inside the incoming web impact zone",
            position = 2,
            section = hazardsSection
    )
    default boolean webDangerFlash()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Spiderlings",
            keyName = "highlightSpiderlings",
            description = "Highlights Spindel's Spiderlings on the floor (prayer-draining minions)",
            position = 3,
            section = hazardsSection
    )
    default boolean highlightSpiderlings()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            name = "Web Hazard Color",
            keyName = "webHazardColor",
            description = "Color for expanding web hazard tiles",
            position = 4,
            section = hazardsSection
    )
    default Color webHazardColor()
    {
        return new Color(255, 50, 50);
    }

    @Alpha
    @ConfigItem(
            name = "Web Safe Tile Color",
            keyName = "webSafeTileColor",
            description = "Color for safe escape tiles outside the expanding web",
            position = 5,
            section = hazardsSection
    )
    default Color webSafeTileColor()
    {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(
            name = "Spiderling Highlight Color",
            keyName = "spiderlingColor",
            description = "Color for highlighting Spindel's Spiderlings",
            position = 6,
            section = hazardsSection
    )
    default Color spiderlingColor()
    {
        return new Color(255, 165, 0);
    }

    // =========================================================================
    // Section 3: Supply Prep (Bank & GE)
    // =========================================================================

    @ConfigItem(
            name = "Enable Bank Filter Button",
            keyName = "enableBankFilter",
            description = "Adds a 1-click button at the top of the Bank to filter for missing Spindel trip supplies",
            position = 0,
            section = supplyPrepSection
    )
    default boolean enableBankFilter()
    {
        return true;
    }

    @ConfigItem(
            name = "Enable GE Quick-Search Button",
            keyName = "enableGeFilter",
            description = "Adds a 1-click button in the Grand Exchange search bar to list missing tradeable Spindel supplies",
            position = 1,
            section = supplyPrepSection
    )
    default boolean enableGeFilter()
    {
        return true;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
            name = "Trips to Prep (Batch Count)",
            keyName = "batchCount",
            description = "Number of trips to multiply consumable supply quantities by when checking missing items for GE",
            position = 2,
            section = supplyPrepSection
    )
    default int batchCount()
    {
        return 1;
    }

    @ConfigItem(
            name = "Require Royal Seed Pod",
            keyName = "requireSeedPod",
            description = "Include Royal seed pod (1) in trip supply checks and Bank filter",
            position = 3,
            section = supplyPrepSection
    )
    default boolean requireSeedPod()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer / Restore Potion",
            keyName = "restorePotionType",
            description = "Type of prayer restoration potion to bring",
            position = 4,
            section = supplyPrepSection
    )
    default RestorePotionType restorePotionType()
    {
        return RestorePotionType.BLIGHTED_SUPER_RESTORE;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
            name = "Prayer / Restore Count",
            keyName = "restorePotionCount",
            description = "Number of 4-dose prayer/restore potions per trip",
            position = 5,
            section = supplyPrepSection
    )
    default int restorePotionCount()
    {
        return 4;
    }

    @ConfigItem(
            name = "Primary Food Type",
            keyName = "foodType",
            description = "Primary healing food to bring",
            position = 6,
            section = supplyPrepSection
    )
    default FoodType foodType()
    {
        return FoodType.BLIGHTED_MANTA_RAY;
    }

    @Range(min = 0, max = 28)
    @ConfigItem(
            name = "Primary Food Count",
            keyName = "foodCount",
            description = "Number of primary food items per trip",
            position = 7,
            section = supplyPrepSection
    )
    default int foodCount()
    {
        return 16;
    }

    @ConfigItem(
            name = "Combo Food (Karambwan)",
            keyName = "karambwanType",
            description = "Combo food to bring alongside primary food",
            position = 8,
            section = supplyPrepSection
    )
    default KarambwanType karambwanType()
    {
        return KarambwanType.BLIGHTED_KARAMBWAN;
    }

    @Range(min = 0, max = 28)
    @ConfigItem(
            name = "Combo Food Count",
            keyName = "karambwanCount",
            description = "Number of combo food items per trip",
            position = 9,
            section = supplyPrepSection
    )
    default int karambwanCount()
    {
        return 4;
    }

    @ConfigItem(
            name = "Combat Boost Potion",
            keyName = "combatPotionType",
            description = "Combat boost potion to bring",
            position = 10,
            section = supplyPrepSection
    )
    default CombatPotionType combatPotionType()
    {
        return CombatPotionType.DIVINE_SUPER_COMBAT;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            name = "Combat Potion Count",
            keyName = "combatPotionCount",
            description = "Number of combat boost potions per trip",
            position = 11,
            section = supplyPrepSection
    )
    default int combatPotionCount()
    {
        return 1;
    }

    @ConfigItem(
            name = "Include Stamina Potion(4)",
            keyName = "includeStaminaPotion",
            description = "Include 1 Stamina potion(4) per trip",
            position = 12,
            section = supplyPrepSection
    )
    default boolean includeStaminaPotion()
    {
        return true;
    }

    @ConfigItem(
            name = "Include Looting Bag",
            keyName = "includeLootingBag",
            description = "Check for a Looting bag in inventory/bank",
            position = 13,
            section = supplyPrepSection
    )
    default boolean includeLootingBag()
    {
        return true;
    }

    @ConfigItem(
            name = "Include Holy Wrench",
            keyName = "includeHolyWrench",
            description = "Include a Holy wrench in trip supply checks to mitigate spiderling prayer drain",
            position = 14,
            section = supplyPrepSection
    )
    default boolean includeHolyWrench()
    {
        return false;
    }

    // =========================================================================
    // Section 4: Display & Audio
    // =========================================================================

    @ConfigItem(
            name = "Show HUD Panel",
            keyName = "showHudOverlay",
            description = "Displays the status HUD panel (rotation counter, PKer status, and supply prep)",
            position = 0,
            section = displayAudioSection
    )
    default boolean showHudOverlay()
    {
        return true;
    }

    @ConfigItem(
            name = "Display Spindel 5x5 Tile",
            keyName = "displaySpindelTile",
            description = "Highlights Spindel's 5x5 footprint colored by current attack style (Green = Ranged, Blue = Magic)",
            position = 1,
            section = displayAudioSection
    )
    default boolean displaySpindelTile()
    {
        return true;
    }

    @ConfigItem(
            name = "Attack & Rotation Counter on Boss",
            keyName = "attackTickCounter",
            description = "Displays the 8-attack style counter and next attack tick timer on Spindel",
            position = 2,
            section = displayAudioSection
    )
    default boolean attackTickCounter()
    {
        return true;
    }

    @ConfigItem(
            name = "Sound Alerts (PKer / Web / Style Switch)",
            keyName = "soundAlerts",
            description = "Plays an audio alert when a PKer enters, when standing on a web, or when Spindel switches attack styles",
            position = 3,
            section = displayAudioSection
    )
    default boolean soundAlerts()
    {
        return true;
    }

    @ConfigItem(
            name = "Low Prayer Sound Alert",
            keyName = "lowPrayerSoundAlert",
            description = "Plays an audio alert when your Prayer points drop to or below the threshold (especially from Spiderlings)",
            position = 4,
            section = displayAudioSection
    )
    default boolean lowPrayerSoundAlert()
    {
        return true;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(
            name = "Low Prayer Threshold",
            keyName = "lowPrayerThreshold",
            description = "Prayer points threshold at which the low prayer sound alert triggers (default: 15)",
            position = 5,
            section = displayAudioSection
    )
    default int lowPrayerThreshold()
    {
        return 15;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            name = "Sound Volume",
            keyName = "soundVolume",
            description = "Volume for audio alert notifications (0-100%)",
            position = 6,
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
            position = 7,
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
            position = 8,
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
            position = 9,
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
            position = 10,
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
    enum OffensivePrayerStyle
    {
        MELEE("Melee (Piety / Chivalry)"),
        RANGED("Ranged (Rigour / Deadeye)"),
        MAGIC("Magic (Augury / Vigour)");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum PkerDetectionMode
    {
        COMBAT_BRACKET("Attackable Combat Bracket"),
        ANY_PLAYER("Any Player in Cave");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum RestorePotionType
    {
        BLIGHTED_SUPER_RESTORE("Blighted super restore(4)", ItemID.BLIGHTED_4DOSE2RESTORE, new int[]{
                ItemID.BLIGHTED_4DOSE2RESTORE, ItemID.BLIGHTED_3DOSE2RESTORE, ItemID.BLIGHTED_2DOSE2RESTORE, ItemID.BLIGHTED_1DOSE2RESTORE
        }),
        SUPER_RESTORE("Super restore(4)", ItemID._4DOSE2RESTORE, new int[]{
                ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._2DOSE2RESTORE, ItemID._1DOSE2RESTORE
        }),
        PRAYER_POTION("Prayer potion(4)", ItemID._4DOSEPRAYERRESTORE, new int[]{
                ItemID._4DOSEPRAYERRESTORE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSEPRAYERRESTORE
        });

        private final String displayName;
        private final int primaryItemId;
        private final int[] doseVariantIds;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum FoodType
    {
        BLIGHTED_MANTA_RAY("Blighted manta ray", ItemID.BLIGHTED_MANTARAY),
        BLIGHTED_ANGLERFISH("Blighted anglerfish", ItemID.BLIGHTED_ANGLERFISH),
        MANTA_RAY("Manta ray", ItemID.MANTARAY),
        ANGLERFISH("Anglerfish", ItemID.ANGLERFISH),
        SHARK("Shark", ItemID.SHARK);

        private final String displayName;
        private final int itemId;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum KarambwanType
    {
        BLIGHTED_KARAMBWAN("Blighted karambwan", ItemID.BLIGHTED_KARAMBWAN),
        COOKED_KARAMBWAN("Cooked karambwan", ItemID.TBWT_COOKED_KARAMBWAN),
        NONE("None", -1);

        private final String displayName;
        private final int itemId;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum CombatPotionType
    {
        DIVINE_SUPER_COMBAT("Divine super combat(4)", ItemID._4DOSEDIVINECOMBAT, new int[]{
                ItemID._4DOSEDIVINECOMBAT, ItemID._3DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._1DOSEDIVINECOMBAT
        }),
        SUPER_COMBAT("Super combat potion(4)", ItemID._4DOSE2COMBAT, new int[]{
                ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT
        }),
        NONE("None", -1, new int[0]);

        private final String displayName;
        private final int primaryItemId;
        private final int[] doseVariantIds;

        @Override
        public String toString()
        {
            return displayName;
        }
    }
}
