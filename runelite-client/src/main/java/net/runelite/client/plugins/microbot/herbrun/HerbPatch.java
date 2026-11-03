package net.runelite.client.plugins.microbot.herbrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

import java.util.Arrays;
import java.util.List;

/**
 * Representation of all 9 primary herb patches (plus optional Harmony Island),
 * ordered in the optimal routing sequence.
 */
@Getter
@RequiredArgsConstructor
public enum HerbPatch {
    TROLL_STRONGHOLD(
            "Troll Stronghold",
            true,
            new WorldPoint(2827, 3693, 0),
            ObjectID.MYARM_HERBPATCH,
            "Stony basalt",
            ItemID.STRONGHOLD_TELEPORT_BASALT,
            new int[]{ItemID.STRONGHOLD_TELEPORT_BASALT, ItemID.NZONE_TELETAB_TROLLHEIM},
            Quest.MY_ARMS_BIG_ADVENTURE,
            1,
            VarbitID.FARMING_TRANSMIT_A
    ),
    WEISS(
            "Weiss",
            true,
            new WorldPoint(2849, 3933, 0),
            ObjectID.MY2ARM_HERBPATCH,
            "Icy basalt",
            ItemID.WEISS_TELEPORT_BASALT,
            new int[]{ItemID.WEISS_TELEPORT_BASALT},
            Quest.MAKING_FRIENDS_WITH_MY_ARM,
            1,
            VarbitID.FARMING_TRANSMIT_A
    ),
    HOSIDIUS(
            "Hosidius",
            true,
            new WorldPoint(1739, 3552, 0),
            ObjectID.FARMING_HERB_PATCH_6,
            "Teleport to house",
            ItemID.POH_TABLET_TELEPORTTOHOUSE,
            new int[]{
                    ItemID.POH_TABLET_TELEPORTTOHOUSE,
                    ItemID.SKILLCAPE_CONSTRUCTION,
                    ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED,
                    ItemID.SKILLCAPE_MAX
            },
            null,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    FARMING_GUILD(
            "Farming Guild",
            true,
            new WorldPoint(1238, 3728, 0),
            ObjectID.FARMING_HERB_PATCH_7,
            "Farming cape",
            ItemID.SKILLCAPE_FARMING,
            new int[]{
                    ItemID.SKILLCAPE_FARMING,
                    ItemID.SKILLCAPE_FARMING_TRIMMED,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_1,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_4,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_5,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_6
            },
            null,
            85,
            VarbitID.FARMING_TRANSMIT_E
    ),
    CATHERBY(
            "Catherby",
            false,
            new WorldPoint(2815, 3464, 0),
            ObjectID.FARMING_HERB_PATCH_2,
            "Catherby tablet",
            ItemID.LUNAR_TABLET_CATHERBY_TELEPORT,
            new int[]{ItemID.LUNAR_TABLET_CATHERBY_TELEPORT, ItemID.POH_TABLET_CAMELOTTELEPORT},
            null,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    ARDOUGNE(
            "Ardougne",
            false,
            new WorldPoint(2672, 3375, 0),
            ObjectID.FARMING_HERB_PATCH_3,
            "Ardougne cloak",
            ItemID.ARDY_CAPE_MEDIUM,
            new int[]{
                    ItemID.ARDY_CAPE_MEDIUM,
                    ItemID.ARDY_CAPE_HARD,
                    ItemID.ARDY_CAPE_ELITE,
                    ItemID.ARDY_CAPE_EASY,
                    ItemID.POH_TABLET_ARDOUGNETELEPORT
            },
            null,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    FALADOR(
            "Falador",
            false,
            new WorldPoint(3057, 3311, 0),
            ObjectID.FARMING_HERB_PATCH_1,
            "Explorer's ring",
            ItemID.LUMBRIDGE_RING_MEDIUM,
            new int[]{
                    ItemID.LUMBRIDGE_RING_MEDIUM,
                    ItemID.LUMBRIDGE_RING_HARD,
                    ItemID.LUMBRIDGE_RING_ELITE,
                    ItemID.LUMBRIDGE_RING_EASY,
                    ItemID.TELETAB_DRAYNOR,
                    ItemID.POH_TABLET_FALADORTELEPORT
            },
            null,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    PORT_PHASMATYS(
            "Port Phasmatys",
            false,
            new WorldPoint(3605, 3528, 0),
            ObjectID.FARMING_HERB_PATCH_4,
            "Ectophial",
            ItemID.ECTOPHIAL,
            new int[]{ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY},
            Quest.GHOSTS_AHOY,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    VARLAMORE(
            "Varlamore",
            false,
            new WorldPoint(1581, 3094, 0),
            ObjectID.FARMING_HERB_PATCH_8,
            "Teleport to house",
            ItemID.POH_TABLET_TELEPORTTOHOUSE,
            new int[]{
                    ItemID.POH_TABLET_TELEPORTTOHOUSE,
                    ItemID.SKILLCAPE_CONSTRUCTION,
                    ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED,
                    ItemID.SKILLCAPE_MAX
            },
            Quest.CHILDREN_OF_THE_SUN,
            1,
            VarbitID.FARMING_TRANSMIT_D
    ),
    HARMONY(
            "Harmony Island",
            false,
            new WorldPoint(3790, 3839, 0),
            ObjectID.FARMING_HERB_PATCH_5,
            "Harmony tablet",
            ItemID.TELETAB_HARMONY,
            new int[]{ItemID.TELETAB_HARMONY},
            null,
            1,
            VarbitID.FARMING_TRANSMIT_B
    );

    private final String patchName;
    private final boolean diseaseFree;
    private final WorldPoint location;
    private final int objectId;
    private final String primaryTeleportName;
    private final int primaryTeleportItemId;
    private final int[] alternateTeleportItemIds;
    private final Quest requiredQuest;
    private final int requiredFarmingLevel;
    private final int varbit;
    private boolean cachedUnlocked = true;

    /**
     * Updates the cached unlock status for this patch.
     * Should only be called safely outside of CS2 script callbacks (e.g. on GameTick, startup, config change).
     */
    public void updateUnlockStatus(Client client) {
        if (client == null) {
            cachedUnlocked = true;
            return;
        }
        if (requiredFarmingLevel > 1) {
            if (client.getRealSkillLevel(Skill.FARMING) < requiredFarmingLevel) {
                cachedUnlocked = false;
                return;
            }
        }
        if (requiredQuest != null) {
            try {
                if (requiredQuest.getState(client) != QuestState.FINISHED) {
                    cachedUnlocked = false;
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        cachedUnlocked = true;
    }

    public static void updateAllUnlocks(Client client) {
        if (client == null) {
            return;
        }
        for (HerbPatch patch : values()) {
            patch.updateUnlockStatus(client);
        }
    }

    /**
     * Checks if the player has unlocked access to this patch.
     * Uses cached status so no reentrant CS2 scripts are executed during script callbacks.
     */
    public boolean isUnlocked(Client client) {
        return cachedUnlocked;
    }

    /**
     * Checks if an item ID matches any of the patch's valid teleports.
     */
    public boolean matchesTeleportId(int itemId) {
        if (itemId == primaryTeleportItemId) {
            return true;
        }
        for (int alt : alternateTeleportItemIds) {
            if (alt == itemId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the FarmingRegion in RuneLite's FarmingWorld corresponding to this patch.
     */
    public String getFarmingRegionName() {
        switch (this) {
            case HOSIDIUS:
                return "Kourend";
            case PORT_PHASMATYS:
                return "Morytania";
            case VARLAMORE:
                return "Civitas illa Fortis";
            case HARMONY:
                return "Harmony";
            default:
                return patchName;
        }
    }

    /**
     * Returns the 9 standard patches in optimal routing order.
     */
    public static List<HerbPatch> getOptimalRoute() {
        return Arrays.asList(
                TROLL_STRONGHOLD,
                WEISS,
                HOSIDIUS,
                FARMING_GUILD,
                CATHERBY,
                ARDOUGNE,
                FALADOR,
                PORT_PHASMATYS,
                VARLAMORE
        );
    }
}
