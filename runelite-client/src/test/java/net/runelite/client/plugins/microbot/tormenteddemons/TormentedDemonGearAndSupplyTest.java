package net.runelite.client.plugins.microbot.tormenteddemons;

import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TormentedDemonGearAndSupplyTest
{
    @Test
    public void testGearAndRecoloredCrystalClassification()
    {
        // Melee & Ranged primary weapons
        assertEquals(TormentedDemonGearClassifier.CombatStyle.MELEE,
                TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.ARCLIGHT, "Arclight"));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.MELEE,
                TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.EMBERLIGHT, "Emberlight"));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.RANGED,
                TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.BOW_OF_FAERDHINEN_INFINITE_CADARN, "Bow of faerdhinen (c)"));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.RANGED,
                TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.SCORCHING_BOW, "Scorching bow"));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.MAGIC,
                TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.PURGING_STAFF, "Purging staff"));

        // Recolored green Crystal armour matches via normalized name fallback even with arbitrary recolor IDs
        assertTrue(TormentedDemonGearClassifier.isRangedSwitchGear(999001, "Crystal helm (Iorwerth)"));
        assertTrue(TormentedDemonGearClassifier.isRangedSwitchGear(999002, "Crystal body (Cadarn)"));
        assertTrue(TormentedDemonGearClassifier.isRangedSwitchGear(999003, "Crystal legs (Crwys)"));
        assertTrue(TormentedDemonGearClassifier.isRangedSwitchGear(ItemID.BOW_OF_FAERDHINEN_INFINITE, "Bow of faerdhinen (c)"));

        // Melee switch gear includes Avernic defender, Fire/Infernal cape, Arclight/Emberlight
        assertTrue(TormentedDemonGearClassifier.isMeleeSwitchGear(ItemID.INFERNAL_DEFENDER, "Avernic defender"));
        assertTrue(TormentedDemonGearClassifier.isMeleeSwitchGear(ItemID.TZHAAR_CAPE_FIRE, "Fire cape"));
        assertTrue(TormentedDemonGearClassifier.isMeleeSwitchGear(ItemID.ARCLIGHT, "Arclight"));

        // Punish weapons (degraded Dharok's 100/75/50/25 allowed, broken 0 rejected)
        assertTrue(TormentedDemonGearClassifier.isPunishWeapon(ItemID.BARROWS_DHAROK_WEAPON_75, "Dharok's greataxe 75"));
        assertFalse(TormentedDemonGearClassifier.isPunishWeapon(ItemID.BARROWS_DHAROK_WEAPON_BROKEN, "Dharok's greataxe 0"));
        assertTrue(TormentedDemonGearClassifier.isPunishWeapon(ItemID.HEAVY_BALLISTA, "Heavy ballista"));
        assertTrue(TormentedDemonGearClassifier.isPunishWeapon(ItemID.DRAGON_2H_SWORD, "Dragon 2h sword"));

        // Special attack weapons and thresholds
        assertEquals(30, TormentedDemonGearClassifier.getSpecWeaponCostPercent(ItemID.BONE_CLAWS, "Burning claws"));
        assertEquals(25, TormentedDemonGearClassifier.getSpecWeaponCostPercent(ItemID.DRAGON_DAGGER_P__, "Dragon dagger(p++)"));
        assertEquals(25, TormentedDemonGearClassifier.getSpecWeaponCostPercent(ItemID.ABYSSAL_DAGGER_P__, "Abyssal dagger(p++)"));
        assertEquals(50, TormentedDemonGearClassifier.getSpecWeaponCostPercent(ItemID.SGS, "Saradomin godsword"));
        assertEquals(-1, TormentedDemonGearClassifier.getSpecWeaponCostPercent(ItemID.ARCLIGHT, "Arclight"));
    }

    @Test
    public void testTargetStyleResolutionAndPrayedAgainstChecks()
    {
        TormentedDemonConfig.CombatStylePair pair = TormentedDemonConfig.CombatStylePair.MELEE_RANGED;

        assertEquals(TormentedDemonGearClassifier.CombatStyle.MELEE,
                TormentedDemonGearClassifier.resolveTargetStyleForDemon(EnumSet.noneOf(HeadIcon.class), pair));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.RANGED,
                TormentedDemonGearClassifier.resolveTargetStyleForDemon(EnumSet.of(HeadIcon.MELEE), pair));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.MELEE,
                TormentedDemonGearClassifier.resolveTargetStyleForDemon(EnumSet.of(HeadIcon.RANGED), pair));
        assertEquals(TormentedDemonGearClassifier.CombatStyle.MAGIC,
                TormentedDemonGearClassifier.resolveTargetStyleForDemon(EnumSet.of(HeadIcon.MELEE, HeadIcon.RANGED), pair));

        assertTrue(TormentedDemonGearClassifier.isWeaponPrayedAgainst(
                TormentedDemonGearClassifier.CombatStyle.MELEE, EnumSet.of(HeadIcon.MELEE)));
        assertFalse(TormentedDemonGearClassifier.isWeaponPrayedAgainst(
                TormentedDemonGearClassifier.CombatStyle.RANGED, EnumSet.of(HeadIcon.MELEE)));
        assertFalse(TormentedDemonGearClassifier.isWeaponPrayedAgainst(
                null, EnumSet.of(HeadIcon.MELEE)));
    }

    @Test
    public void testDefaultAndAlternateSupplyRequirements()
    {
        TormentedDemonConfig defaultConfig = new TormentedDemonConfig() {};
        List<TormentedDemonSupplyCalculator.SupplyRequirement> reqs =
                TormentedDemonSupplyCalculator.buildRequirements(defaultConfig);

        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE && r.getSingleTripQuantity() == 1));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.POH_TABLET_TELEPORTTOHOUSE && r.getSingleTripQuantity() == 1));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID._4DOSEDIVINECOMBAT && r.getSingleTripQuantity() == 1));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID._4DOSEDIVINERANGE && r.getSingleTripQuantity() == 1));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID._4DOSEPRAYERRESTORE && r.getSingleTripQuantity() == 6));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.SHARK && r.getSingleTripQuantity() == 9));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.BOOK_OF_THE_DEAD && !r.isTradeable()));
        assertTrue(reqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.BH_RUNE_POUCH && !r.isTradeable()));
        assertTrue(reqs.stream().anyMatch(r -> r.getRuneType() == Runes.DEATH));
        assertTrue(reqs.stream().anyMatch(r -> r.getRuneType() == Runes.SOUL));
        assertTrue(reqs.stream().anyMatch(r -> r.getRuneType() == Runes.WRATH));

        TormentedDemonConfig starterConfig = new TormentedDemonConfig()
        {
            @Override
            public TravelMethod travelMethod()
            {
                return TravelMethod.GAMES_NECKLACE_SAPPHIRE_LANTERN;
            }

            @Override
            public boolean prepArceuusRunes()
            {
                return false;
            }
        };
        List<TormentedDemonSupplyCalculator.SupplyRequirement> starterReqs =
                TormentedDemonSupplyCalculator.buildRequirements(starterConfig);
        assertTrue(starterReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.NECKLACE_OF_MINIGAMES_8));
        assertTrue(starterReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.TOG_SAPPHIRE_LANTERN_LIT));
        assertFalse(starterReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.BOOK_OF_THE_DEAD));
    }

    @Test
    public void testSupplyEvaluationWithBatchPotionDosesAndRunePouch()
    {
        Client client = mock(Client.class);
        ItemContainer inv = mock(ItemContainer.class);
        ItemContainer equip = mock(ItemContainer.class);
        ItemContainer bank = mock(ItemContainer.class);

        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equip);
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);

        // Rune pouch holds Fire (3000), Blood (3000), Cosmic (1000)
        when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE1)).thenReturn(Runes.FIRE.getId());
        when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT1)).thenReturn(3000);
        when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE2)).thenReturn(Runes.BLOOD.getId());
        when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT2)).thenReturn(3000);
        when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE3)).thenReturn(Runes.COSMIC.getId());
        when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT3)).thenReturn(1000);

        when(equip.getItems()).thenReturn(new Item[0]);
        when(inv.getItems()).thenReturn(new Item[]{
                new Item(ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE, 5),
                new Item(ItemID.POH_TABLET_TELEPORTTOHOUSE, 8),
                new Item(ItemID._3DOSEDIVINECOMBAT, 1), // 3-dose counts toward melee pot
                new Item(ItemID._4DOSEDIVINERANGE, 1),
                new Item(ItemID._4DOSEPRAYERRESTORE, 6),
                new Item(ItemID.SHARK, 9),
                new Item(ItemID.BOOK_OF_THE_DEAD, 1),
                new Item(ItemID.DIVINE_RUNE_POUCH, 1),
                new Item(ItemID.DEATHRUNE, 500),
                new Item(ItemID.SOULRUNE, 500),
                new Item(ItemID.WRATHRUNE, 500)
        });
        when(bank.getItems()).thenReturn(new Item[]{
                new Item(ItemID.SHARK, 5) // Total Sharks = 14 (less than batch 3 * 9 = 27)
        });

        TormentedDemonConfig batchConfig = new TormentedDemonConfig()
        {
            @Override
            public int batchCount()
            {
                return 3;
            }
        };

        TormentedDemonSupplyCalculator calc = new TormentedDemonSupplyCalculator(client, batchConfig);
        List<TormentedDemonSupplyCalculator.SupplyStatus> statuses = calc.evaluateSupplies();

        // Inventory single trip is 100% ready (missing bank IDs empty)
        assertTrue(calc.getMissingBankItemIds().isEmpty());

        // GE missing tradeables with batch=3: Divine super combat (1/3), Divine ranging (1/3), Prayer pot (6/18), Sharks (14/27)
        Set<Integer> geMissing = calc.getMissingTradeableItemIds();
        assertTrue(geMissing.contains(ItemID._4DOSEDIVINECOMBAT));
        assertTrue(geMissing.contains(ItemID._4DOSEDIVINERANGE));
        assertTrue(geMissing.contains(ItemID._4DOSEPRAYERRESTORE));
        assertTrue(geMissing.contains(ItemID.SHARK));
        assertFalse(geMissing.contains(ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE));
        assertFalse(geMissing.contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertFalse(geMissing.contains(ItemID.FIRERUNE));
        assertFalse(geMissing.contains(ItemID.BOOK_OF_THE_DEAD));

        TormentedDemonSupplyCalculator.SupplyStatus fireStatus = statuses.stream()
                .filter(s -> s.getRequirement().getRuneType() == Runes.FIRE)
                .findFirst()
                .orElse(null);
        assertEquals(3000, fireStatus.getCarriedQuantity());
        assertNull(TormentedDemonGearClassifier.classifyWeaponStyle(ItemID.BARROWS_DHAROK_WEAPON_BROKEN, "Dharok's greataxe 0"));
    }
}
