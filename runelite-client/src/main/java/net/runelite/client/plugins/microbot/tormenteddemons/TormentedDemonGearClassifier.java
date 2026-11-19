package net.runelite.client.plugins.microbot.tormenteddemons;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.HeadIcon;
import net.runelite.api.gameval.ItemID;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

/**
 * Pure item, gear switch, punish weapon, special attack weapon, consumable,
 * and drop classifier for Tormented Demons.
 * Matches both canonical ItemIDs and normalized item names so recolored crystal
 * armour, corrupted Bowfa, degraded Barrows, and poisoned daggers always work.
 */
public final class TormentedDemonGearClassifier
{
    @Getter
    @RequiredArgsConstructor
    public enum CombatStyle
    {
        MELEE("MELEE", new Color(255, 145, 50)),
        RANGED("RANGED", new Color(75, 225, 95)),
        MAGIC("MAGIC", new Color(80, 195, 255));

        private final String label;
        private final Color color;
    }

    private static final Set<Integer> MELEE_WEAPON_IDS = ImmutableSet.of(
            ItemID.EMBERLIGHT,
            ItemID.ARCLIGHT,
            ItemID.DARKLIGHT,
            ItemID.ABYSSAL_WHIP,
            ItemID.ABYSSAL_TENTACLE,
            ItemID.ABYSSAL_BLUDGEON,
            ItemID.OSMUMTENS_FANG,
            ItemID.OSMUMTENS_FANG_ORNAMENT,
            ItemID.SCYTHE_OF_VITUR,
            ItemID.SOULREAPER,
            ItemID.INQUISITORS_MACE,
            ItemID.BONE_CLAWS,
            ItemID.DRAGON_CLAWS,
            ItemID.VOIDWAKER,
            ItemID.ARKAN_BLADE,
            ItemID.DRAGON_DAGGER,
            ItemID.DRAGON_DAGGER_P,
            ItemID.DRAGON_DAGGER_P_,
            ItemID.DRAGON_DAGGER_P__,
            ItemID.ABYSSAL_DAGGER,
            ItemID.ABYSSAL_DAGGER_P,
            ItemID.ABYSSAL_DAGGER_P_,
            ItemID.ABYSSAL_DAGGER_P__,
            ItemID.SGS,
            ItemID.SGSG,
            ItemID.ELDER_MAUL,
            ItemID.ELDER_MAUL_ORNAMENT,
            ItemID.BARROWS_DHAROK_WEAPON,
            ItemID.BARROWS_DHAROK_WEAPON_100,
            ItemID.BARROWS_DHAROK_WEAPON_75,
            ItemID.BARROWS_DHAROK_WEAPON_50,
            ItemID.BARROWS_DHAROK_WEAPON_25,
            ItemID.DRAGON_2H_SWORD,
            ItemID.TZHAAR_MAUL,
            ItemID.TZHAAR_MAUL_T,
            ItemID.GIANTS_FOUNDRY_COLOSSAL_BLADE
    );

    private static final Set<Integer> RANGED_WEAPON_IDS = ImmutableSet.of(
            ItemID.SCORCHING_BOW,
            ItemID.TWISTED_BOW,
            ItemID.BOW_OF_FAERDHINEN,
            ItemID.BOW_OF_FAERDHINEN_INFINITE,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_ITHELL,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_IORWERTH,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_TRAHAEARN,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_CADARN,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_CRWYS,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_MEILYR,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_AMLODD,
            ItemID.TOXIC_BLOWPIPE,
            ItemID.TOXIC_BLOWPIPE_LOADED,
            ItemID.TOXIC_BLOWPIPE_ORNAMENT,
            ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT,
            ItemID.ECLIPSE_ATLATL,
            ItemID.HEAVY_BALLISTA,
            ItemID.HEAVY_BALLISTA_ORNAMENT,
            ItemID.LIGHT_BALLISTA,
            ItemID.ZARYTE_XBOW,
            ItemID.ACB,
            ItemID.XBOWS_CROSSBOW_DRAGON,
            ItemID.XBOWS_CROSSBOW_RUNITE
    );

    private static final Set<Integer> MAGIC_WEAPON_IDS = ImmutableSet.of(
            ItemID.PURGING_STAFF,
            ItemID.TOXIC_SOTD,
            ItemID.TOXIC_SOTD_CHARGED,
            ItemID.SOTD,
            ItemID.MAGICTRAINING_WAND_MASTER,
            ItemID.BARROWS_AHRIM_WEAPON,
            ItemID.BARROWS_AHRIM_WEAPON_100,
            ItemID.BARROWS_AHRIM_WEAPON_75,
            ItemID.BARROWS_AHRIM_WEAPON_50,
            ItemID.BARROWS_AHRIM_WEAPON_25,
            ItemID.FROSTMOON_SPEAR,
            ItemID.NIGHTMARE_STAFF_ELDRITCH,
            ItemID.NIGHTMARE_STAFF_VOLATILE
    );

    private static final Set<Integer> PUNISH_WEAPON_IDS = ImmutableSet.of(
            ItemID.BARROWS_DHAROK_WEAPON,
            ItemID.BARROWS_DHAROK_WEAPON_100,
            ItemID.BARROWS_DHAROK_WEAPON_75,
            ItemID.BARROWS_DHAROK_WEAPON_50,
            ItemID.BARROWS_DHAROK_WEAPON_25,
            ItemID.HEAVY_BALLISTA,
            ItemID.HEAVY_BALLISTA_ORNAMENT,
            ItemID.LIGHT_BALLISTA,
            ItemID.DRAGON_2H_SWORD,
            ItemID.TZHAAR_MAUL,
            ItemID.TZHAAR_MAUL_T,
            ItemID.ELDER_MAUL,
            ItemID.ELDER_MAUL_ORNAMENT,
            ItemID.GIANTS_FOUNDRY_COLOSSAL_BLADE,
            ItemID.ZARYTE_XBOW,
            ItemID.ACB,
            ItemID.XBOWS_CROSSBOW_DRAGON
    );

    private static final Set<Integer> SMOULDERING_CONSUMABLE_IDS = ImmutableSet.of(
            ItemID.SMOULDERING_HEART,
            ItemID.SMOULDERING_GLAND,
            ItemID.SMOULDERING_PILE_OF_FLESH
    );

    private static final Set<Integer> ASHES_FOR_OFFERING_IDS = ImmutableSet.of(
            ItemID.INFERNAL_ASHES,
            ItemID.MALICIOUS_ASHES
    );

    private static final Set<Integer> JACKPOT_DROP_IDS = ImmutableSet.of(
            ItemID.TORMENTED_SYNAPSE,
            ItemID.BONE_CLAW
    );

    private static final Set<Integer> VALUABLE_DROP_IDS = ImmutableSet.of(
            ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE,
            ItemID._4DOSEPRAYERRESTORE,
            ItemID._2DOSEPRAYERRESTORE,
            ItemID.MANTARAY,
            ItemID.DRAGON_ARROWHEADS,
            ItemID.RUNE_PLATEBODY,
            ItemID.RUNE_KITESHIELD,
            ItemID.DRAGON_DAGGER,
            ItemID.BATTLESTAFF,
            ItemID.FIRE_ORB,
            ItemID.UNSTRUNG_MAGIC_SHORTBOW,
            ItemID.UNSTRUNG_MAGIC_LONGBOW,
            ItemID.SOULRUNE,
            ItemID.CHAOSRUNE,
            ItemID.RUNE_ARROW
    );

    private TormentedDemonGearClassifier()
    {
    }

    private static String normalizeName(String itemName)
    {
        return itemName == null ? "" : itemName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Classifies an equipped or inventory weapon into MELEE, RANGED, or MAGIC.
     */
    public static CombatStyle classifyWeaponStyle(int itemId, String itemName)
    {
        if (itemId == ItemID.BARROWS_DHAROK_WEAPON_BROKEN)
        {
            return null;
        }
        if (MELEE_WEAPON_IDS.contains(itemId))
        {
            return CombatStyle.MELEE;
        }
        if (RANGED_WEAPON_IDS.contains(itemId))
        {
            return CombatStyle.RANGED;
        }
        if (MAGIC_WEAPON_IDS.contains(itemId))
        {
            return CombatStyle.MAGIC;
        }

        String lower = normalizeName(itemName);
        if (lower.isEmpty())
        {
            return null;
        }

        if (lower.contains("bow") || lower.contains("crossbow") || lower.contains("blowpipe")
                || lower.contains("ballista") || lower.contains("atlatl") || lower.contains("chinchompa"))
        {
            return CombatStyle.RANGED;
        }
        if (lower.contains("staff") || lower.contains("wand") || lower.contains("trident")
                || lower.contains("shadow") || lower.contains("sceptre") || lower.contains("nightmare")
                || lower.contains("blue moon spear"))
        {
            return CombatStyle.MAGIC;
        }
        if (lower.contains("emberlight") || lower.contains("arclight") || lower.contains("darklight")
                || lower.contains("whip") || lower.contains("tentacle") || lower.contains("bludgeon")
                || lower.contains("fang") || lower.contains("claws") || lower.contains("dagger")
                || lower.contains("godsword") || lower.contains("greataxe") || lower.contains("maul")
                || lower.contains("sword") || lower.contains("scythe") || lower.contains("axe")
                || lower.contains("mace") || lower.contains("halberd") || lower.contains("blade")
                || lower.contains("tzhaar-ket-om") || lower.contains("voidwaker"))
        {
            return CombatStyle.MELEE;
        }
        return null;
    }

    public static boolean isMeleeSwitchGear(int itemId, String itemName)
    {
        if (itemId == ItemID.INFERNAL_DEFENDER
                || itemId == ItemID.DRAGON_PARRYINGDAGGER
                || itemId == ItemID.DRAGON_PARRYINGDAGGER_T
                || itemId == ItemID.INFERNAL_CAPE
                || itemId == ItemID.TZHAAR_CAPE_FIRE)
        {
            return true;
        }
        if (classifyWeaponStyle(itemId, itemName) == CombatStyle.MELEE && !isPunishWeapon(itemId, itemName))
        {
            return true;
        }
        String lower = normalizeName(itemName);
        if (lower.isEmpty())
        {
            return false;
        }
        return lower.contains("emberlight") || lower.contains("arclight") || lower.contains("defender")
                || lower.contains("infernal cape") || lower.contains("fire cape")
                || lower.contains("torva") || lower.contains("bandos") || lower.contains("blood moon")
                || lower.contains("oathplate") || lower.contains("fighter torso") || lower.contains("faceguard")
                || lower.contains("ferocious gloves") || lower.contains("primordial") || lower.contains("ultor")
                || lower.contains("berserker ring") || lower.contains("rancour") || lower.contains("torture")
                || lower.contains("blood fury") || lower.contains("void melee");
    }

    public static boolean isRangedSwitchGear(int itemId, String itemName)
    {
        if (itemId == ItemID.AVAS_ASSEMBLER
                || itemId == ItemID.ANMA_50_REWARD
                || itemId == ItemID.DIZANAS_QUIVER_CHARGED
                || itemId == ItemID.DIZANAS_QUIVER_CHARGED_TROUVER)
        {
            return true;
        }
        if (classifyWeaponStyle(itemId, itemName) == CombatStyle.RANGED && !isPunishWeapon(itemId, itemName))
        {
            return true;
        }
        String lower = normalizeName(itemName);
        if (lower.isEmpty())
        {
            return false;
        }
        return lower.contains("bow of faerdhinen") || lower.contains("scorching bow") || lower.contains("twisted bow")
                || lower.contains("blowpipe") || lower.contains("atlatl")
                || lower.contains("crystal helm") || lower.contains("crystal body") || lower.contains("crystal legs")
                || lower.contains("masori") || lower.contains("karil") || lower.contains("armadyl")
                || lower.contains("d'hide") || lower.contains("eclipse moon")
                || lower.contains("assembler") || lower.contains("quiver")
                || lower.contains("anguish") || lower.contains("rupture")
                || lower.contains("zaryte vambraces") || lower.contains("pegasian") || lower.contains("venator ring")
                || lower.contains("void ranger") || lower.contains("elite void");
    }

    public static boolean isMagicSwitchGear(int itemId, String itemName)
    {
        if (classifyWeaponStyle(itemId, itemName) == CombatStyle.MAGIC)
        {
            return true;
        }
        String lower = normalizeName(itemName);
        if (lower.isEmpty())
        {
            return false;
        }
        return lower.contains("purging staff") || lower.contains("ahrim") || lower.contains("ancestral")
                || lower.contains("virtus") || lower.contains("blue moon")
                || lower.contains("occult necklace") || lower.contains("tormented bracelet")
                || lower.contains("confliction gauntlets") || lower.contains("elidinis' ward")
                || (lower.contains("imbued") && lower.contains("cape"))
                || lower.contains("magus ring") || lower.contains("eternal boots")
                || lower.contains("void mage");
    }

    public static boolean isGearForStyle(CombatStyle style, int itemId, String itemName)
    {
        if (style == null)
        {
            return false;
        }
        switch (style)
        {
            case MELEE:
                return isMeleeSwitchGear(itemId, itemName);
            case RANGED:
                return isRangedSwitchGear(itemId, itemName);
            case MAGIC:
                return isMagicSwitchGear(itemId, itemName);
            default:
                return false;
        }
    }

    /**
     * Identifies slow heavy Crush / Heavy Ranged punish weapons for the Shield-Down bonus hit.
     * Explicitly rejects broken Dharok's greataxe (0%).
     */
    public static boolean isPunishWeapon(int itemId, String itemName)
    {
        if (itemId == ItemID.BARROWS_DHAROK_WEAPON_BROKEN)
        {
            return false;
        }
        if (PUNISH_WEAPON_IDS.contains(itemId))
        {
            return true;
        }
        String lower = normalizeName(itemName);
        if (lower.isEmpty() || lower.endsWith(" 0"))
        {
            return false;
        }
        return lower.contains("dharok's greataxe") || lower.contains("heavy ballista")
                || lower.contains("light ballista") || lower.contains("dragon 2h sword")
                || lower.contains("tzhaar-ket-om") || lower.contains("elder maul")
                || lower.contains("colossal blade") || lower.contains("zaryte crossbow")
                || lower.contains("armadyl crossbow") || lower.contains("dragon crossbow");
    }

    /**
     * Returns special attack energy percentage required (25/30/50/75), or -1 if not a TD spec weapon.
     */
    public static int getSpecWeaponCostPercent(int itemId, String itemName)
    {
        if (itemId == ItemID.BONE_CLAWS || itemId == ItemID.ARKAN_BLADE)
        {
            return 30;
        }
        if (itemId == ItemID.DRAGON_DAGGER || itemId == ItemID.DRAGON_DAGGER_P
                || itemId == ItemID.DRAGON_DAGGER_P_ || itemId == ItemID.DRAGON_DAGGER_P__
                || itemId == ItemID.ABYSSAL_DAGGER || itemId == ItemID.ABYSSAL_DAGGER_P
                || itemId == ItemID.ABYSSAL_DAGGER_P_ || itemId == ItemID.ABYSSAL_DAGGER_P__)
        {
            return 25;
        }
        if (itemId == ItemID.SGS || itemId == ItemID.SGSG
                || itemId == ItemID.DRAGON_CLAWS || itemId == ItemID.VOIDWAKER
                || itemId == ItemID.TOXIC_BLOWPIPE || itemId == ItemID.TOXIC_BLOWPIPE_LOADED
                || itemId == ItemID.TOXIC_BLOWPIPE_ORNAMENT || itemId == ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT)
        {
            return 50;
        }
        if (itemId == ItemID.ZARYTE_XBOW)
        {
            return 75;
        }

        String lower = normalizeName(itemName);
        if (lower.isEmpty())
        {
            return -1;
        }
        if (lower.contains("burning claws") || lower.contains("arkan blade"))
        {
            return 30;
        }
        if (lower.contains("dragon dagger") || lower.contains("abyssal dagger"))
        {
            return 25;
        }
        if (lower.contains("saradomin godsword") || lower.contains("dragon claws")
                || lower.contains("voidwaker") || lower.contains("blowpipe"))
        {
            return 50;
        }
        if (lower.contains("zaryte crossbow"))
        {
            return 75;
        }
        return -1;
    }

    /**
     * Picks the best unprayed combat style from the user's configured style pair.
     */
    public static CombatStyle resolveTargetStyleForDemon(
            Set<HeadIcon> protectedStyles,
            TormentedDemonConfig.CombatStylePair pair)
    {
        CombatStyle primary = pair != null ? pair.getPrimaryStyle() : CombatStyle.MELEE;
        CombatStyle secondary = pair != null ? pair.getSecondaryStyle() : CombatStyle.RANGED;

        if (protectedStyles == null || protectedStyles.isEmpty())
        {
            return primary;
        }
        if (!isStyleProtected(primary, protectedStyles))
        {
            return primary;
        }
        if (!isStyleProtected(secondary, protectedStyles))
        {
            return secondary;
        }
        for (CombatStyle fallback : CombatStyle.values())
        {
            if (!isStyleProtected(fallback, protectedStyles))
            {
                return fallback;
            }
        }
        return primary;
    }

    public static boolean isStyleProtected(CombatStyle style, Set<HeadIcon> protectedStyles)
    {
        if (style == null || protectedStyles == null || protectedStyles.isEmpty())
        {
            return false;
        }
        switch (style)
        {
            case MELEE:
                return protectedStyles.contains(HeadIcon.MELEE);
            case RANGED:
                return protectedStyles.contains(HeadIcon.RANGED);
            case MAGIC:
                return protectedStyles.contains(HeadIcon.MAGIC);
            default:
                return false;
        }
    }

    public static boolean isWeaponPrayedAgainst(CombatStyle equippedStyle, Set<HeadIcon> protectedStyles)
    {
        return equippedStyle != null && isStyleProtected(equippedStyle, protectedStyles);
    }

    public static boolean isSmoulderingConsumable(int itemId)
    {
        return SMOULDERING_CONSUMABLE_IDS.contains(itemId);
    }

    public static boolean isAshesForOffering(int itemId)
    {
        return ASHES_FOR_OFFERING_IDS.contains(itemId);
    }

    public static boolean isJackpotDrop(int itemId)
    {
        return JACKPOT_DROP_IDS.contains(itemId);
    }

    public static boolean isValuableTempleDrop(int itemId)
    {
        return JACKPOT_DROP_IDS.contains(itemId)
                || SMOULDERING_CONSUMABLE_IDS.contains(itemId)
                || ASHES_FOR_OFFERING_IDS.contains(itemId)
                || VALUABLE_DROP_IDS.contains(itemId);
    }

    public static boolean isFoodItem(int itemId, String itemName)
    {
        if (itemId == ItemID.SHARK || itemId == ItemID.MANTARAY || itemId == ItemID.ANGLERFISH
                || itemId == ItemID.TBWT_COOKED_KARAMBWAN || itemId == ItemID.ANTELOPEMOON_COOKED)
        {
            return true;
        }
        String lower = normalizeName(itemName);
        return lower.contains("shark") || lower.contains("manta ray") || lower.contains("anglerfish")
                || lower.contains("karambwan") || lower.contains("moonlight antelope")
                || lower.contains("sea turtle") || lower.contains("monkfish");
    }

    public static boolean isPrayerRestoreItem(int itemId, String itemName)
    {
        if (itemId == ItemID._4DOSEPRAYERRESTORE || itemId == ItemID._3DOSEPRAYERRESTORE
                || itemId == ItemID._2DOSEPRAYERRESTORE || itemId == ItemID._1DOSEPRAYERRESTORE
                || itemId == ItemID._4DOSE2RESTORE || itemId == ItemID._3DOSE2RESTORE
                || itemId == ItemID._2DOSE2RESTORE || itemId == ItemID._1DOSE2RESTORE)
        {
            return true;
        }
        String lower = normalizeName(itemName);
        return lower.startsWith("prayer potion(") || lower.startsWith("super restore(")
                || lower.startsWith("sanfew serum(");
    }
}
