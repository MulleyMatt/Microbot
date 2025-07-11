package net.runelite.client.plugins.microbot.barrows;

import net.runelite.api.ItemID;
import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.revKiller.revKillerConfig;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

@ConfigGroup("barrows")
@ConfigInformation("1. Have an inventory setup named Barrows <br><br> 2. Required items: prayer potions or moonlight moth mixes(2), barrows teleports tablets, or teleport to house tablets, food, Catalyic runes (if using wind spells), and a spade.<br /><br /> 3. Spells: Wind: Blast, Wave, and Surge. Or Powered staffs: supports any trident, any sceptre, any crystal staff, Tumeken's, and Sanguinesti. <br /><br /> Special thanks to george for adding the barrows dungeon to the walker; and Crannyy for script testing!<br /><br /> Config by Crannyy")
public interface BarrowsConfig extends Config {
    @ConfigItem(
            keyName = "Food",
            name = "Food",
            description = "type of food",
            position = 0
    )
    default Rs2Food food()
    {
        return Rs2Food.POTATO_WITH_CHEESE;
    }
    @ConfigItem(
            keyName = "targetFoodAmount",
            name = "Max Food Amount",
            description = "Max amount of food to withdraw from the bank.",
            position = 1
    )
    @Range(min = 1, max = 28)
    default int targetFoodAmount() {
        return 10;
    }

    @ConfigItem(
            keyName = "minFood",
            name = "Min Food",
            description = "Minimum amount of food to withdraw from the bank.",
            position = 2
    )
    @Range(min = 1, max = 28)
    default int minFood() {
        return 5;
    }

    @ConfigItem(
            keyName = "selectedPrayerRestoreType",
            name = "Prayer Restore Type:",
            description = "Between prayer potions, or moonlight moth mixes.",
            position = 3
    )
    default prayerRestoreType prayerRestoreType() {
        return prayerRestoreType.Prayer_Potion; // Default selection
    }

    enum prayerRestoreType {
        Prayer_Potion(ItemID.PRAYER_POTION4, "Prayer potion(4)"),
        MoonlightMothMix(ItemID.MOONLIGHT_MOTH_MIX_2, "Moonlight moth mix (2)"),
        MoonlightMoth(ItemID.MOONLIGHT_MOTH_28893, "Moonlight moth");

        private final int id;
        private final String name;

        prayerRestoreType(int id, String name) {
            this.id = id;
            this.name = name;
        }


        public int getPrayerRestoreTypeID() {
            return id;
        }

        public String getPrayerRestoreTypeName() {
            return name;
        }

    }

    @ConfigItem(
            keyName = "targetPrayerPots",
            name = "Max Prayer Restore",
            description = "Max amount of prayer potions, or moonlight moth mixes to withdraw from the bank.",
            position = 4
    )
    @Range(min = 1, max = 20)
    default int targetPrayerPots() {
        return 8;
    }

    @ConfigItem(
            keyName = "minPrayerPots",
            name = "Min Prayer Restore",
            description = "Minimum amount of prayer potions, or moonlight moth mixes to withdraw from the bank.",
            position = 5
    )
    @Range(min = 1, max = 10)
    default int minPrayerPots() {
        return 4;
    }

    @ConfigItem(
            keyName = "targetForgottenBrew",
            name = "Max Forgotten Brews",
            description = "Max amount of forgotten brews to withdraw from the bank.",
            position = 6
    )
    @Range(min = 1, max = 5)
    default int targetForgottenBrew() {
        return 3;
    }

    @ConfigItem(
            keyName = "minForgottenBrew",
            name = "Min Forgotten Brews",
            description = "Minimum amount of forgotten brews to withdraw from the bank.",
            position = 7
    )
    @Range(min = 0, max = 5)
    default int minForgottenBrew() {
        return 1;
    }

    @ConfigItem(
            keyName = "selectedToBarrowsTPMethod",
            name = "Barrows TP Method",
            description = "Between using a barrows teleport tablet, or your POH portal.",
            position = 8
    )
    default selectedToBarrowsTPMethod selectedToBarrowsTPMethod() {
        return selectedToBarrowsTPMethod.Tablet; // Default selection
    }

    enum selectedToBarrowsTPMethod {
        Tablet(ItemID.BARROWS_TELEPORT, "Barrows teleport"),
        POH(ItemID.TELEPORT_TO_HOUSE, "Teleport to house");

        private final int id;
        private final String name;

        selectedToBarrowsTPMethod(int id, String name) {
            this.id = id;
            this.name = name;
        }


        public int getToBarrowsTPMethodItemID() {
            return id;
        }

        public String getToBarrowsTPMethodItemName() {
            return name;
        }

    }

    @ConfigItem(
            keyName = "targetBarrowsTeleports",
            name = "Max Barrows Teleports",
            description = "Max amount of Barrows teleports to withdraw from the bank.",
            position = 9
    )
    @Range(min = 1, max = 10)
    default int targetBarrowsTeleports() {
        return 8;
    }

    @ConfigItem(
            keyName = "minBarrowsTeleports",
            name = "Min Barrows Teleports",
            description = "Minimum amount of Barrows teleports to withdraw from the bank.",
            position = 10
    )
    @Range(min = 1, max = 10)
    default int minBarrowsTeleports() {
        return 1;
    }

    @ConfigItem(
            keyName = "minRuneAmount",
            name = "Min Runes",
            description = "Minimum amount of runes before banking",
            position = 11
    )
    @Range(min = 50, max = 1000)
    default int minRuneAmount() {
        return 180;
    }

    @ConfigItem(
            keyName = "shouldGainRP",
            name = "Aim for 86+% rewards potential",
            description = "Should we gain additional RP other than the barrows brothers?",
            position = 12
    )
    default boolean shouldGainRP() {
        return false;
    }

    @ConfigItem(
            keyName = "shouldPrayAgainstWeakerBrothers",
            name = "Pray against Torag, Verac, and Guthans?",
            description = "Should we Pray against Torag, Verac, and Guthans?",
            position = 13
    )
    default boolean shouldPrayAgainstWeakerBrothers() {
        return true;
    }

}
