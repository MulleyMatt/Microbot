package net.runelite.client.plugins.microbot.tormenteddemons;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.plugins.microbot.util.magic.thralls.ThrallType;

@ConfigGroup(TormentedDemonConfig.GROUP)
public interface TormentedDemonConfig extends Config
{
    String GROUP = "tormenteddemonshelper";

    @ConfigSection(
            name = "Fight & Micro-Assists",
            description = "Opt-in auto-prayer switching, thrall summoning, shift-click walk-through, and combat style pair",
            position = 0
    )
    String fightAssistSection = "fightAssistSection";

    @ConfigSection(
            name = "Mechanics & Inventory Highlights",
            description = "Scene overlays, fire bomb dodge tiles, gear switch/punish/spec highlights, Arceuus reminders, and room warnings",
            position = 1
    )
    String mechanicsSection = "mechanicsSection";

    @ConfigSection(
            name = "Loot & Profit Tracker",
            description = "Kills/hr & GP/hr tracker, ground drop highlights, Smouldering consumable prompts, and jackpot alerts",
            position = 2
    )
    String lootSection = "lootSection";

    @ConfigSection(
            name = "Supply Prep (Bank & GE)",
            description = "1-click Bank & Grand Exchange filters and configurable trip supply checklist",
            position = 3
    )
    String supplyPrepSection = "supplyPrepSection";

    @ConfigSection(
            name = "Display & Audio",
            description = "HUD overlay, screen flash alerts, sound volume, and scene rendering options",
            position = 4,
            closedByDefault = true
    )
    String displayAudioSection = "displayAudioSection";

    // =========================================================================
    // Section 0: Fight & Micro-Assists
    // =========================================================================

    @ConfigItem(
            name = "Auto-Switch Overhead Prayers",
            keyName = "autoSwitchPrayers",
            description = "Automatically switches protection prayers using attack animations/projectiles and post-fire-bomb movement prediction",
            position = 0,
            section = fightAssistSection
    )
    default boolean autoSwitchPrayers()
    {
        return true;
    }

    @ConfigItem(
            name = "Auto-Disable Prayers On Kill",
            keyName = "autoDisablePrayersOnKill",
            description = "Automatically turns off active prayers when a Tormented Demon dies and no other demon is attacking you",
            position = 1,
            section = fightAssistSection
    )
    default boolean autoDisablePrayersOnKill()
    {
        return true;
    }

    @ConfigItem(
            name = "Auto-Cast Thralls",
            keyName = "autoCastThrall",
            description = "Automatically summons your Thrall on cooldown during combat (client-thread-safe, requires Arceuus spellbook, Book of the dead, and runes)",
            position = 2,
            section = fightAssistSection
    )
    default boolean autoCastThrall()
    {
        return true;
    }

    @ConfigItem(
            name = "Thrall Style",
            keyName = "thrallStyle",
            description = "Thrall style to summon (Magic Ghost is ideal as a 3rd style when using Melee + Ranged)",
            position = 3,
            section = fightAssistSection
    )
    default ThrallStyle thrallStyle()
    {
        return ThrallStyle.MAGIC;
    }

    @ConfigItem(
            name = "Shift-Click Walk Through Demon",
            keyName = "shiftClickWalkThrough",
            description = "Swaps Shift-click (and Ctrl-click during Fire Bombs) over Tormented Demons to 'Walk here' so shifting clickboxes don't trap you",
            position = 4,
            section = fightAssistSection
    )
    default boolean shiftClickWalkThrough()
    {
        return true;
    }

    @ConfigItem(
            name = "Combat Style Pair",
            keyName = "combatStylePair",
            description = "Your primary two combat styles so the plugin highlights the correct gear switch when the demon prays against you",
            position = 5,
            section = fightAssistSection
    )
    default CombatStylePair combatStylePair()
    {
        return CombatStylePair.MELEE_RANGED;
    }

    @ConfigItem(
            name = "Prayer Helper Box",
            keyName = "prayerHelper",
            description = "Displays a floating box above the chatbox showing the required protection prayer and status tag",
            position = 6,
            section = fightAssistSection
    )
    default boolean prayerHelper()
    {
        return true;
    }

    @ConfigItem(
            name = "Prayer Tab Marker",
            keyName = "prayerMarker",
            description = "Highlights the required overhead prayer directly inside the Prayer book tab (Green = active, Red = off)",
            position = 7,
            section = fightAssistSection
    )
    default boolean prayerMarker()
    {
        return true;
    }

    // =========================================================================
    // Section 1: Mechanics & Inventory Highlights
    // =========================================================================

    @ConfigItem(
            name = "Demon Scene Overlays",
            keyName = "highlightDemonOverlays",
            description = "Draws hull/tile outlines, target attack style, 150-dmg swap counter, Shield Down & 100% Accuracy banners",
            position = 0,
            section = mechanicsSection
    )
    default boolean highlightDemonOverlays()
    {
        return true;
    }

    @ConfigItem(
            name = "Fire Bomb & Escape Tiles",
            keyName = "highlightFireBombs",
            description = "Highlights lethal Fire Bomb impact tiles in red and optimal 2+ tile Ctrl-click escape tiles in green",
            position = 1,
            section = mechanicsSection
    )
    default boolean highlightFireBombs()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Main Style Switches",
            keyName = "highlightInventoryGearSwitches",
            description = "Highlights your opposite combat style weapons & armour in inventory whenever the demon prays against your equipped weapon",
            position = 2,
            section = mechanicsSection
    )
    default boolean highlightInventoryGearSwitches()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Shield-Down Punish Weapon",
            keyName = "highlightPunishWeapon",
            description = "Highlights slow heavy Crush / Heavy Ranged punish weapons (Dharok's greataxe, Heavy ballista, Dragon 2h) during Shield Down",
            position = 3,
            section = mechanicsSection
    )
    default boolean highlightPunishWeapon()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight 100% Acc Spec Weapon",
            keyName = "highlightSpecWeapon",
            description = "Highlights special attack weapons (Burning claws, DDS, Abyssal dagger, SGS) when the demon's flames go out (100% accuracy)",
            position = 4,
            section = mechanicsSection
    )
    default boolean highlightSpecWeapon()
    {
        return true;
    }

    @ConfigItem(
            name = "Arceuus Spellbook Prompts",
            keyName = "highlightArceuusSpells",
            description = "Highlights Demonic Offering (at 3+ ashes) and Death Charge (at <=160 HP) in your Magic tab",
            position = 5,
            section = mechanicsSection
    )
    default boolean highlightArceuusSpells()
    {
        return true;
    }

    @ConfigItem(
            name = "Warn Multi-Combat Rooms (2x/3x)",
            keyName = "warnMultiCombatRooms",
            description = "Alerts you when entering a Double or Triple Tormented Demon combat room via the Temple indicator varbit",
            position = 6,
            section = mechanicsSection
    )
    default boolean warnMultiCombatRooms()
    {
        return true;
    }

    @ConfigItem(
            name = "Warn Wall / Corner Box-In",
            keyName = "warnWallCornerBoxIn",
            description = "Warns when fighting near walls/corners with few walkable escape tiles (prevents Fire Bomb trap)",
            position = 7,
            section = mechanicsSection
    )
    default boolean warnWallCornerBoxIn()
    {
        return true;
    }

    // =========================================================================
    // Section 2: Loot & Profit Tracker
    // =========================================================================

    @ConfigItem(
            name = "Show Profit Tracker",
            keyName = "showProfitTracker",
            description = "Tracks session kills, kills/hr, total loot GP, GP/hr, and unique drops in the HUD overlay",
            position = 0,
            section = lootSection
    )
    default boolean showProfitTracker()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Ground Drops",
            keyName = "highlightValuableDrops",
            description = "Highlights Tormented Synapse, Burning Claw, Temple Teleports, Prayer Potions, Smouldering drops, Ashes, and Alchables",
            position = 1,
            section = lootSection
    )
    default boolean highlightValuableDrops()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Smouldering Consumables",
            keyName = "highlightSmoulderingInInventory",
            description = "Highlights Smouldering heart/gland/pile of flesh in inventory to remind you to consume them for trip sustain",
            position = 2,
            section = lootSection
    )
    default boolean highlightSmoulderingInInventory()
    {
        return true;
    }

    @ConfigItem(
            name = "Jackpot Drop Alert",
            keyName = "jackpotDropAlert",
            description = "Triggers screen flash and sound alert whenever a Tormented Synapse or Burning Claw drops",
            position = 3,
            section = lootSection
    )
    default boolean jackpotDropAlert()
    {
        return true;
    }

    // =========================================================================
    // Section 3: Supply Prep (Bank & GE)
    // =========================================================================

    @ConfigItem(
            name = "Enable Bank Filter Button",
            keyName = "enableBankFilter",
            description = "Adds a 1-click button in the Bank header to filter missing Tormented Demon trip supplies",
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
            description = "Adds a 1-click button in the Grand Exchange search bar to list missing tradeable Tormented Demon supplies",
            position = 1,
            section = supplyPrepSection
    )
    default boolean enableGeFilter()
    {
        return true;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(
            name = "Trips to Prep (Batch Count)",
            keyName = "batchCount",
            description = "Number of trips to multiply consumables by when checking missing supplies for the Grand Exchange",
            position = 2,
            section = supplyPrepSection
    )
    default int batchCount()
    {
        return 1;
    }

    @ConfigItem(
            name = "Temple Travel Method",
            keyName = "travelMethod",
            description = "How you travel to the Ancient Guthixian Temple (Guthixian temple teleport scrolls or Games necklace + Lit sapphire lantern)",
            position = 3,
            section = supplyPrepSection
    )
    default TravelMethod travelMethod()
    {
        return TravelMethod.GUTHIXIAN_TEMPLE_TELEPORT;
    }

    @ConfigItem(
            name = "Bank Escape Teleport",
            keyName = "bankTeleport",
            description = "Emergency/banking teleport item to check in supplies",
            position = 4,
            section = supplyPrepSection
    )
    default BankTeleport bankTeleport()
    {
        return BankTeleport.HOUSE_TAB;
    }

    @ConfigItem(
            name = "Melee Boost Potion",
            keyName = "meleePotion",
            description = "Melee combat boost potion for Bank & GE supply prep",
            position = 5,
            section = supplyPrepSection
    )
    default MeleePotionType meleePotion()
    {
        return MeleePotionType.DIVINE_SUPER_COMBAT;
    }

    @ConfigItem(
            name = "Ranged Boost Potion",
            keyName = "rangedPotion",
            description = "Ranged boost potion for Bank & GE supply prep",
            position = 6,
            section = supplyPrepSection
    )
    default RangedPotionType rangedPotion()
    {
        return RangedPotionType.DIVINE_RANGING;
    }

    @ConfigItem(
            name = "Prayer Restore Type",
            keyName = "prayerRestoreType",
            description = "Prayer restoration potion type for Bank & GE supply prep",
            position = 7,
            section = supplyPrepSection
    )
    default PrayerRestoreType prayerRestoreType()
    {
        return PrayerRestoreType.PRAYER_POTION;
    }

    @Range(min = 1, max = 14)
    @ConfigItem(
            name = "Prayer Potions Per Trip",
            keyName = "prayerRestoreQty",
            description = "Number of prayer/restore potions to prep per trip",
            position = 8,
            section = supplyPrepSection
    )
    default int prayerRestoreQty()
    {
        return 6;
    }

    @ConfigItem(
            name = "Food Type",
            keyName = "foodType",
            description = "Food type for Bank & GE supply prep",
            position = 9,
            section = supplyPrepSection
    )
    default FoodType foodType()
    {
        return FoodType.SHARK;
    }

    @Range(min = 1, max = 16)
    @ConfigItem(
            name = "Food Per Trip",
            keyName = "foodQty",
            description = "Number of food items to prep per trip",
            position = 10,
            section = supplyPrepSection
    )
    default int foodQty()
    {
        return 9;
    }

    @ConfigItem(
            name = "Prep Arceuus Thrall/Offering Runes",
            keyName = "prepArceuusRunes",
            description = "Include Book of the dead, Rune pouch, and Thrall / Death Charge / Demonic Offering runes in supply prep",
            position = 11,
            section = supplyPrepSection
    )
    default boolean prepArceuusRunes()
    {
        return true;
    }

    // =========================================================================
    // Section 4: Display & Audio
    // =========================================================================

    @ConfigItem(
            name = "Show HUD Status Panel",
            keyName = "showHudOverlay",
            description = "Displays the top-left Tormented Demons fight tracker & Bank/GE supply checklist",
            position = 0,
            section = displayAudioSection
    )
    default boolean showHudOverlay()
    {
        return true;
    }

    @ConfigItem(
            name = "Screen Flash Alerts",
            keyName = "screenFlashAlerts",
            description = "Pulses screen borders for Fire Bombs, Jackpot drops, Low HP, and Multi-Combat rooms",
            position = 1,
            section = displayAudioSection
    )
    default boolean screenFlashAlerts()
    {
        return true;
    }

    @ConfigItem(
            name = "Sound Alerts",
            keyName = "soundAlerts",
            description = "Plays Rs2SoundAlert cues for Fire Bombs, Jackpot drops, and Low Prayer/HP emergencies",
            position = 2,
            section = displayAudioSection
    )
    default boolean soundAlerts()
    {
        return true;
    }

    @Range(min = 5, max = 70)
    @ConfigItem(
            name = "Low Prayer Alert Threshold",
            keyName = "lowPrayerThreshold",
            description = "Highlight prayer restores & alert when Prayer points drop to or below this value during combat",
            position = 3,
            section = displayAudioSection
    )
    default int lowPrayerThreshold()
    {
        return 20;
    }

    @Range(min = 15, max = 90)
    @ConfigItem(
            name = "Low HP Alert Threshold",
            keyName = "lowHpThreshold",
            description = "Highlight food & alert when Hitpoints drop to or below this value during combat",
            position = 4,
            section = displayAudioSection
    )
    default int lowHpThreshold()
    {
        return 45;
    }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(
            name = "Alert Sound Volume",
            keyName = "soundVolume",
            description = "Volume percentage for Rs2SoundAlert cues",
            position = 5,
            section = displayAudioSection
    )
    default int soundVolume()
    {
        return 80;
    }

    @ConfigItem(
            name = "Font Type",
            keyName = "fontType",
            description = "Font used for 3D scene text overlays",
            position = 6,
            section = displayAudioSection
    )
    default FontType fontType()
    {
        return FontType.SMALL;
    }

    @ConfigItem(
            name = "Text Outline",
            keyName = "textOutline",
            description = "Draw a crisp 4-way outline around 3D scene text",
            position = 7,
            section = displayAudioSection
    )
    default boolean textOutline()
    {
        return true;
    }

    @Range(min = 1, max = 6)
    @Units(Units.PIXELS)
    @ConfigItem(
            name = "Outline Width",
            keyName = "outlineWidth",
            description = "Stroke width for demon hulls and scene tile polygons",
            position = 8,
            section = displayAudioSection
    )
    default int outlineWidth()
    {
        return 2;
    }

    @Range(min = 0, max = 150)
    @ConfigItem(
            name = "Tile Fill Alpha",
            keyName = "fillAlpha",
            description = "Opacity of demon scene tile fills",
            position = 9,
            section = displayAudioSection
    )
    default int fillAlpha()
    {
        return 25;
    }

    // =========================================================================
    // Config Enums
    // =========================================================================

    @Getter
    @RequiredArgsConstructor
    enum ThrallStyle
    {
        MAGIC("Magic (Ghost)", ThrallType.MAGIC),
        RANGED("Ranged (Skeleton)", ThrallType.RANGED),
        MELEE("Melee (Zombie)", ThrallType.MELEE);

        private final String name;
        private final ThrallType thrallType;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum CombatStylePair
    {
        MELEE_RANGED("Melee + Ranged", TormentedDemonGearClassifier.CombatStyle.MELEE, TormentedDemonGearClassifier.CombatStyle.RANGED),
        MELEE_MAGIC("Melee + Magic", TormentedDemonGearClassifier.CombatStyle.MELEE, TormentedDemonGearClassifier.CombatStyle.MAGIC),
        RANGED_MAGIC("Ranged + Magic", TormentedDemonGearClassifier.CombatStyle.RANGED, TormentedDemonGearClassifier.CombatStyle.MAGIC);

        private final String name;
        private final TormentedDemonGearClassifier.CombatStyle primaryStyle;
        private final TormentedDemonGearClassifier.CombatStyle secondaryStyle;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum TravelMethod
    {
        GUTHIXIAN_TEMPLE_TELEPORT("Guthixian temple teleport"),
        GAMES_NECKLACE_SAPPHIRE_LANTERN("Games necklace + Lit sapphire lantern");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum BankTeleport
    {
        HOUSE_TAB("Teleport to house", ItemID.POH_TABLET_TELEPORTTOHOUSE, new int[]{ItemID.POH_TABLET_TELEPORTTOHOUSE}),
        ROYAL_SEED_POD("Royal seed pod", ItemID.MM2_ROYAL_SEED_POD, new int[]{ItemID.MM2_ROYAL_SEED_POD}),
        RING_OF_DUELING("Ring of dueling(8)", ItemID.RING_OF_DUELING_8, new int[]{
                ItemID.RING_OF_DUELING_8, ItemID.RING_OF_DUELING_7, ItemID.RING_OF_DUELING_6,
                ItemID.RING_OF_DUELING_5, ItemID.RING_OF_DUELING_4, ItemID.RING_OF_DUELING_3,
                ItemID.RING_OF_DUELING_2, ItemID.RING_OF_DUELING_1
        });

        private final String displayName;
        private final int primaryItemId;
        private final int[] acceptedIds;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum MeleePotionType
    {
        DIVINE_SUPER_COMBAT("Divine super combat potion(4)", ItemID._4DOSEDIVINECOMBAT, new int[]{
                ItemID._4DOSEDIVINECOMBAT, ItemID._3DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._1DOSEDIVINECOMBAT
        }),
        SUPER_COMBAT("Super combat potion(4)", ItemID._4DOSE2COMBAT, new int[]{
                ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT
        }),
        NONE("None", -1, new int[0]);

        private final String displayName;
        private final int primaryItemId;
        private final int[] acceptedIds;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum RangedPotionType
    {
        DIVINE_RANGING("Divine ranging potion(4)", ItemID._4DOSEDIVINERANGE, new int[]{
                ItemID._4DOSEDIVINERANGE, ItemID._3DOSEDIVINERANGE, ItemID._2DOSEDIVINERANGE, ItemID._1DOSEDIVINERANGE
        }),
        DIVINE_BASTION("Divine bastion potion(4)", ItemID._4DOSEDIVINEBASTION, new int[]{
                ItemID._4DOSEDIVINEBASTION, ItemID._3DOSEDIVINEBASTION, ItemID._2DOSEDIVINEBASTION, ItemID._1DOSEDIVINEBASTION
        }),
        RANGING_POTION("Ranging potion(4)", ItemID._4DOSERANGERSPOTION, new int[]{
                ItemID._4DOSERANGERSPOTION, ItemID._3DOSERANGERSPOTION, ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION
        }),
        NONE("None", -1, new int[0]);

        private final String displayName;
        private final int primaryItemId;
        private final int[] acceptedIds;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum PrayerRestoreType
    {
        PRAYER_POTION("Prayer potion(4)", ItemID._4DOSEPRAYERRESTORE, new int[]{
                ItemID._4DOSEPRAYERRESTORE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSEPRAYERRESTORE
        }),
        SUPER_RESTORE("Super restore(4)", ItemID._4DOSE2RESTORE, new int[]{
                ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._2DOSE2RESTORE, ItemID._1DOSE2RESTORE
        });

        private final String displayName;
        private final int primaryItemId;
        private final int[] acceptedIds;

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
        SHARK("Shark", ItemID.SHARK),
        MANTA_RAY("Manta ray", ItemID.MANTARAY),
        ANGLERFISH("Anglerfish", ItemID.ANGLERFISH),
        KARAMBWAN("Cooked karambwan", ItemID.TBWT_COOKED_KARAMBWAN),
        MOONLIGHT_ANTELOPE("Cooked moonlight antelope", ItemID.ANTELOPEMOON_COOKED);

        private final String displayName;
        private final int itemId;

        @Override
        public String toString()
        {
            return displayName;
        }
    }
}
