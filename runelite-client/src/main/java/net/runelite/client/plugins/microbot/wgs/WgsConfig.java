package net.runelite.client.plugins.microbot.wgs;

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
import net.runelite.client.plugins.microbot.util.magic.Runes;

@ConfigGroup(WgsConfig.GROUP)
public interface WgsConfig extends Config
{
    String GROUP = "wgsbosshelper";

    @ConfigSection(
            name = "Fight & Auto-Prayer",
            description = "Auto-prayer switching, offensive prayers, prayer tab marker, and Surok spellbook marker",
            position = 0
    )
    String fightPrayerSection = "fightPrayerSection";

    @ConfigSection(
            name = "Boss Mechanics & Hazards",
            description = "3D highlights, counters, and flash alerts for Surok Magis, Balance Elemental, and Tormented Demons",
            position = 1
    )
    String bossMechanicsSection = "bossMechanicsSection";

    @ConfigSection(
            name = "Supply Prep (Bank & GE)",
            description = "Boss-specific supply checklists and 1-click Bank & Grand Exchange filter buttons",
            position = 2
    )
    String supplyPrepSection = "supplyPrepSection";

    @ConfigSection(
            name = "Display & Audio",
            description = "HUD panel, scene rendering styles, and emergency audio alerts",
            position = 3,
            closedByDefault = true
    )
    String displayAudioSection = "displayAudioSection";

    // =========================================================================
    // Section 0: Fight & Auto-Prayer
    // =========================================================================

    @ConfigItem(
            name = "Auto-Switch Overhead Prayers",
            keyName = "autoSwitchPrayers",
            description = "Automatically switches protection prayers (Magic vs Surok, Head Colour vs Balance Elemental, and Attack Style vs Tormented Demons)",
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
            description = "Automatically activates your best unlocked offensive prayer (Augury / Rigour / Piety) during active WGS boss fights",
            position = 1,
            section = fightPrayerSection
    )
    default boolean autoOffensivePrayer()
    {
        return false;
    }

    @ConfigItem(
            name = "Offensive Prayer Style",
            keyName = "offensivePrayerStyle",
            description = "Style for Auto Offensive Prayer (Auto chooses Magic for Surok or matches boss unprayed style / equipped weapon)",
            position = 2,
            section = fightPrayerSection
    )
    default OffensivePrayerStyle offensivePrayerStyle()
    {
        return OffensivePrayerStyle.AUTO;
    }

    @ConfigItem(
            name = "Auto-Disable Prayers After Kill",
            keyName = "autoDisablePrayersOnKill",
            description = "Automatically turns off active prayers when a WGS boss dies or becomes unconscious",
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
            description = "Displays a floating box above the chatbox showing the required overhead prayer and attack counter",
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
            name = "Spellbook & Item Marker (Surok)",
            keyName = "spellbookMarker",
            description = "Highlights Weaken, Bind/Entangle, Telegrab, Alchemy, opposite Elemental Surge spells, and Teleorb in your Magic/Inventory tabs",
            position = 6,
            section = fightPrayerSection
    )
    default boolean spellbookMarker()
    {
        return true;
    }

    // =========================================================================
    // Section 1: Boss Mechanics & Hazards
    // =========================================================================

    @ConfigItem(
            name = "Highlight Surok Magis Specials",
            keyName = "highlightSurokSpecials",
            description = "Highlights Strong/Agile Warriors, Lit Explosives, Elemental Surge orbs (with opposite element), and post-fight Teleorb",
            position = 0,
            section = bossMechanicsSection
    )
    default boolean highlightSurokSpecials()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Balance Elemental",
            keyName = "highlightBalanceElemental",
            description = "Highlights Balance Elemental by head colour, 4-attack counter, <50% HP 1-tick warnings, and unprayed attack style",
            position = 1,
            section = bossMechanicsSection
    )
    default boolean highlightBalanceElemental()
    {
        return true;
    }

    @ConfigItem(
            name = "Highlight Tormented Demons",
            keyName = "highlightTormentedDemons",
            description = "Highlights Tormented Demon prayer weaknesses, shield-down burst windows, Fire Bomb AoE tiles, and safe adjacent tiles",
            position = 2,
            section = bossMechanicsSection
    )
    default boolean highlightTormentedDemons()
    {
        return true;
    }

    @ConfigItem(
            name = "Emergency Screen Flash Alerts",
            keyName = "screenFlashAlerts",
            description = "Pulses the screen for Lit Explosives, missing Protect Magic on Surok, critical Balance Elemental Prayer/HP, and TD Fire Bombs",
            position = 3,
            section = bossMechanicsSection
    )
    default boolean screenFlashAlerts()
    {
        return true;
    }

    // =========================================================================
    // Section 2: Supply Prep (Bank & GE)
    // =========================================================================

    @ConfigItem(
            name = "Supply Prep Boss",
            keyName = "prepBossStage",
            description = "Select which WGS boss encounter to check and filter supplies for at the Bank and Grand Exchange",
            position = 0,
            section = supplyPrepSection
    )
    default WgsBossStage prepBossStage()
    {
        return WgsBossStage.SUROK_MAGIS;
    }

    @ConfigItem(
            name = "Enable Bank Filter Button",
            keyName = "enableBankFilter",
            description = "Adds a 1-click button at the top of the Bank to filter missing supplies for the selected WGS boss",
            position = 1,
            section = supplyPrepSection
    )
    default boolean enableBankFilter()
    {
        return true;
    }

    @ConfigItem(
            name = "Enable GE Quick-Search Button",
            keyName = "enableGeFilter",
            description = "Adds a 1-click button in the Grand Exchange search bar to list missing tradeable WGS boss supplies",
            position = 2,
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
            description = "Number of trips to multiply consumable supply quantities by when checking missing items for GE",
            position = 3,
            section = supplyPrepSection
    )
    default int batchCount()
    {
        return 1;
    }

    @ConfigItem(
            name = "Surok Combat Rune Tier",
            keyName = "surokCatalystRune",
            description = "Catalyst rune tier for Surok Standard Spellbook elemental spells (Surge = Wrath, Wave = Blood, Blast = Death)",
            position = 4,
            section = supplyPrepSection
    )
    default CatalystRuneTier surokCatalystRune()
    {
        return CatalystRuneTier.BLOOD_WAVE;
    }

    @Range(min = 50, max = 2000)
    @ConfigItem(
            name = "Elemental Runes (Each)",
            keyName = "elementalRuneCount",
            description = "Quantity of Air, Water, Earth, and Fire runes to bring for Surok Magis",
            position = 5,
            section = supplyPrepSection
    )
    default int elementalRuneCount()
    {
        return 300;
    }

    @Range(min = 0, max = 1000)
    @ConfigItem(
            name = "Combat Catalyst Rune Count",
            keyName = "catalystRuneCount",
            description = "Quantity of Wrath, Blood, or Death runes to bring for Surok Magis",
            position = 6,
            section = supplyPrepSection
    )
    default int catalystRuneCount()
    {
        return 150;
    }

    @ConfigItem(
            name = "Restore / Prayer Potion",
            keyName = "restorePotionType",
            description = "Prayer restoration potion (Boss 2: Balance Elemental always enforces Super Restores due to stat drain)",
            position = 7,
            section = supplyPrepSection
    )
    default RestorePotionType restorePotionType()
    {
        return RestorePotionType.SUPER_RESTORE;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
            name = "Restore Potion Count",
            keyName = "restorePotionCount",
            description = "Number of 4-dose restore/prayer potions (Balance Elemental requires at least 4 Super Restores)",
            position = 8,
            section = supplyPrepSection
    )
    default int restorePotionCount()
    {
        return 4;
    }

    @ConfigItem(
            name = "Primary Food Type",
            keyName = "foodType",
            description = "Primary high-healing food to bring",
            position = 9,
            section = supplyPrepSection
    )
    default FoodType foodType()
    {
        return FoodType.MANTA_RAY;
    }

    @Range(min = 0, max = 28)
    @ConfigItem(
            name = "Primary Food Count",
            keyName = "foodCount",
            description = "Number of primary food items per trip",
            position = 10,
            section = supplyPrepSection
    )
    default int foodCount()
    {
        return 12;
    }

    @Range(min = 0, max = 14)
    @ConfigItem(
            name = "Cooked Karambwan Count",
            keyName = "karambwanCount",
            description = "Number of combo-eat Cooked Karambwans for Balance Elemental and Tormented Demons",
            position = 11,
            section = supplyPrepSection
    )
    default int karambwanCount()
    {
        return 4;
    }

    @ConfigItem(
            name = "One-Click Teleport Item",
            keyName = "teleportItemType",
            description = "Non-rune one-click emergency teleport (Balance Elemental can drain Magic to 1)",
            position = 12,
            section = supplyPrepSection
    )
    default TeleportItemType teleportItemType()
    {
        return TeleportItemType.HOUSE_TABLET;
    }

    @ConfigItem(
            name = "Include Stamina Potion(4)",
            keyName = "includeStaminaPotion",
            description = "Include 1 Stamina potion(4) in supply prep checks",
            position = 13,
            section = supplyPrepSection
    )
    default boolean includeStaminaPotion()
    {
        return true;
    }

    // =========================================================================
    // Section 3: Display & Audio
    // =========================================================================

    @ConfigItem(
            name = "Show HUD Panel",
            keyName = "showHudOverlay",
            description = "Displays the status HUD panel during WGS boss fights and Bank/GE supply prep",
            position = 0,
            section = displayAudioSection
    )
    default boolean showHudOverlay()
    {
        return true;
    }

    @ConfigItem(
            name = "Emergency Sound Alerts",
            keyName = "soundAlerts",
            description = "Plays an audio alert for Surok special spawns (Bombs/Surges/Warriors) and TD Fire Bombs",
            position = 1,
            section = displayAudioSection
    )
    default boolean soundAlerts()
    {
        return true;
    }

    @ConfigItem(
            name = "Low Prayer Sound Alert",
            keyName = "lowPrayerSoundAlert",
            description = "Plays an audio alert when Prayer drops to or below threshold (higher urgency <50% HP on Balance Elemental)",
            position = 2,
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
            description = "Prayer points threshold that triggers the low prayer emergency sound alert",
            position = 3,
            section = displayAudioSection
    )
    default int lowPrayerThreshold()
    {
        return 25;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            name = "Sound Volume",
            keyName = "soundVolume",
            description = "Volume for audio alert notifications (0-100%)",
            position = 4,
            section = displayAudioSection
    )
    default int soundVolume()
    {
        return 80;
    }

    @ConfigItem(
            name = "Font Type",
            keyName = "fontType",
            description = "Font used for 3D scene text labels",
            position = 5,
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
            position = 6,
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
            position = 7,
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
            position = 8,
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
        AUTO("Auto (Match Boss / Weapon)"),
        MAGIC("Magic (Augury / Mystic Might)"),
        RANGED("Ranged (Rigour / Eagle Eye)"),
        MELEE("Melee (Piety / Chivalry)");

        private final String displayName;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum CatalystRuneTier
    {
        WRATH_SURGE("Wrath runes (Surge spells)", Runes.WRATH),
        BLOOD_WAVE("Blood runes (Wave spells)", Runes.BLOOD),
        DEATH_BLAST("Death runes (Blast spells)", Runes.DEATH);

        private final String displayName;
        private final Runes rune;

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum RestorePotionType
    {
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
    enum TeleportItemType
    {
        HOUSE_TABLET("Teleport to house (tablet)", ItemID.POH_TABLET_TELEPORTTOHOUSE, new int[]{ItemID.POH_TABLET_TELEPORTTOHOUSE}, true),
        ROYAL_SEED_POD("Royal seed pod", ItemID.MM2_ROYAL_SEED_POD, new int[]{ItemID.MM2_ROYAL_SEED_POD}, false),
        ECTOPHIAL("Ectophial", ItemID.ECTOPHIAL, new int[]{ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY}, false);

        private final String displayName;
        private final int primaryItemId;
        private final int[] acceptedIds;
        private final boolean tradeable;

        @Override
        public String toString()
        {
            return displayName;
        }
    }
}
