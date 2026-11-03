package net.runelite.client.plugins.microbot.herbrun;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbRunTest {

    @Test
    public void testOptimalRouteAndDiseaseFreePatches() {
        List<HerbPatch> route = HerbPatch.getOptimalRoute();
        assertEquals("Optimal route should have 9 patches", 9, route.size());

        assertEquals(HerbPatch.TROLL_STRONGHOLD, route.get(0));
        assertEquals(HerbPatch.WEISS, route.get(1));
        assertEquals(HerbPatch.HOSIDIUS, route.get(2));
        assertEquals(HerbPatch.FARMING_GUILD, route.get(3));
        assertEquals(HerbPatch.CATHERBY, route.get(4));
        assertEquals(HerbPatch.ARDOUGNE, route.get(5));
        assertEquals(HerbPatch.FALADOR, route.get(6));
        assertEquals(HerbPatch.PORT_PHASMATYS, route.get(7));
        assertEquals(HerbPatch.VARLAMORE, route.get(8));

        // 4 disease-free patches from strategy guide
        List<HerbPatch> diseaseFree = route.stream()
                .filter(HerbPatch::isDiseaseFree)
                .collect(Collectors.toList());

        assertEquals(4, diseaseFree.size());
        assertTrue(diseaseFree.contains(HerbPatch.TROLL_STRONGHOLD));
        assertTrue(diseaseFree.contains(HerbPatch.WEISS));
        assertTrue(diseaseFree.contains(HerbPatch.HOSIDIUS));
        assertTrue(diseaseFree.contains(HerbPatch.FARMING_GUILD));

        // 5 standard patches
        List<HerbPatch> standard = route.stream()
                .filter(p -> !p.isDiseaseFree())
                .collect(Collectors.toList());

        assertEquals(5, standard.size());
        assertTrue(standard.contains(HerbPatch.ARDOUGNE));
        assertTrue(standard.contains(HerbPatch.FALADOR));
        assertTrue(standard.contains(HerbPatch.CATHERBY));
        assertTrue(standard.contains(HerbPatch.PORT_PHASMATYS));
        assertTrue(standard.contains(HerbPatch.VARLAMORE));
    }

    @Test
    public void testFarmingRegionNames() {
        assertEquals("Kourend", HerbPatch.HOSIDIUS.getFarmingRegionName());
        assertEquals("Morytania", HerbPatch.PORT_PHASMATYS.getFarmingRegionName());
        assertEquals("Civitas illa Fortis", HerbPatch.VARLAMORE.getFarmingRegionName());
        assertEquals("Harmony", HerbPatch.HARMONY.getFarmingRegionName());
        assertEquals("Troll Stronghold", HerbPatch.TROLL_STRONGHOLD.getFarmingRegionName());
        assertEquals("Weiss", HerbPatch.WEISS.getFarmingRegionName());
        assertEquals("Farming Guild", HerbPatch.FARMING_GUILD.getFarmingRegionName());
        assertEquals("Catherby", HerbPatch.CATHERBY.getFarmingRegionName());
        assertEquals("Ardougne", HerbPatch.ARDOUGNE.getFarmingRegionName());
        assertEquals("Falador", HerbPatch.FALADOR.getFarmingRegionName());
    }

    @Test
    public void testPatchTeleportItemMatching() {
        // Troll Stronghold matches stony basalt & trollheim tab
        assertTrue(HerbPatch.TROLL_STRONGHOLD.matchesTeleportId(ItemID.STRONGHOLD_TELEPORT_BASALT));
        assertTrue(HerbPatch.TROLL_STRONGHOLD.matchesTeleportId(ItemID.NZONE_TELETAB_TROLLHEIM));
        assertFalse(HerbPatch.TROLL_STRONGHOLD.matchesTeleportId(ItemID.WEISS_TELEPORT_BASALT));

        // Weiss matches icy basalt
        assertTrue(HerbPatch.WEISS.matchesTeleportId(ItemID.WEISS_TELEPORT_BASALT));

        // Ardougne matches ardy cloaks
        assertTrue(HerbPatch.ARDOUGNE.matchesTeleportId(ItemID.ARDY_CAPE_MEDIUM));
        assertTrue(HerbPatch.ARDOUGNE.matchesTeleportId(ItemID.ARDY_CAPE_HARD));
        assertTrue(HerbPatch.ARDOUGNE.matchesTeleportId(ItemID.ARDY_CAPE_ELITE));

        // Port Phasmatys matches Ectophial
        assertTrue(HerbPatch.PORT_PHASMATYS.matchesTeleportId(ItemID.ECTOPHIAL));
        assertTrue(HerbPatch.PORT_PHASMATYS.matchesTeleportId(ItemID.ECTOPHIAL_EMPTY));

        // Hosidius matches POH house teleports (house tablet, construction cape, max cape)
        assertTrue(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertTrue(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.SKILLCAPE_CONSTRUCTION));
        assertTrue(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED));
        assertTrue(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.SKILLCAPE_MAX));
        assertFalse(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.NZONE_TELETAB_KOUREND));
        assertFalse(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.POH_TABLET_KOURENDTELEPORT));
        assertFalse(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.VEOS_KHAREDSTS_MEMOIRS));
        assertFalse(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.BOOK_OF_THE_DEAD));
        assertFalse(HerbPatch.HOSIDIUS.matchesTeleportId(ItemID.XERIC_TALISMAN));

        // Varlamore matches POH house teleports (house tablet, construction cape, max cape)
        assertTrue(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertTrue(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.SKILLCAPE_CONSTRUCTION));
        assertTrue(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED));
        assertTrue(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.SKILLCAPE_MAX));
        assertFalse(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.HG_QUETZALWHISTLE_BASIC));
        assertFalse(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.HG_QUETZALWHISTLE_ENHANCED));
        assertFalse(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.HG_QUETZALWHISTLE_PERFECTED));
        assertFalse(HerbPatch.VARLAMORE.matchesTeleportId(ItemID.POH_TABLET_FORTISTELEPORT));
    }

    @Test
    public void testSupplyItemCalculations() {
        // Case 1: Item in bank, missing from inventory -> withdraw from bank
        HerbRunSupplyCalculator.SupplyItem itemInBank = new HerbRunSupplyCalculator.SupplyItem(
                "Ranarr seed",
                ItemID.RANARR_SEED,
                4, // needed
                0, // in inventory
                10, // in bank
                true,
                false,
                "Seed"
        );

        assertTrue(itemInBank.isMissingFromInventory());
        assertTrue(itemInBank.canWithdrawFromBank());
        assertFalse(itemInBank.needsToBuyFromGe());
        assertEquals(0, itemInBank.getGeBuyQuantity(1));

        // Case 2: Missing completely -> buy from GE
        HerbRunSupplyCalculator.SupplyItem itemMissing = new HerbRunSupplyCalculator.SupplyItem(
                "Toadflax seed",
                ItemID.TOADFLAX_SEED,
                5, // needed per run
                0, // in inventory
                0, // in bank
                true,
                false,
                "Seed"
        );

        assertTrue(itemMissing.isMissingFromInventory());
        assertFalse(itemMissing.canWithdrawFromBank());
        assertTrue(itemMissing.needsToBuyFromGe());
        // For 5 runs, should buy 25 seeds
        assertEquals(25, itemMissing.getGeBuyQuantity(5));

        // Case 3: Partial ownership across 5 runs
        HerbRunSupplyCalculator.SupplyItem partialItem = new HerbRunSupplyCalculator.SupplyItem(
                "Stony basalt",
                ItemID.STRONGHOLD_TELEPORT_BASALT,
                1, // 1 per run
                1, // in inventory
                2, // in bank (total 3 owned)
                true,
                false,
                "Teleport"
        );

        assertFalse(partialItem.isMissingFromInventory()); // 1 in inv is enough for active run
        // For 5 runs, needs 5 * 1 = 5, has 3 total -> needs 2 from GE
        assertEquals(2, partialItem.getGeBuyQuantity(5));

        // Case 4: Partial in bank for 1 run (needs 9, has 5 in bank, 0 in inv)
        HerbRunSupplyCalculator.SupplyItem partialBankItem = new HerbRunSupplyCalculator.SupplyItem(
                "Ultracompost",
                ItemID.BUCKET_ULTRACOMPOST,
                9, // needed for run
                0, // in inventory
                5, // in bank
                true,
                false,
                "Compost"
        );

        assertTrue(partialBankItem.isMissingFromInventory());
        assertFalse(partialBankItem.canWithdrawFromBank()); // Needs 9, only 5 in bank -> cannot fully withdraw
        assertTrue(partialBankItem.canPartiallyWithdrawFromBank()); // Has 5 > 0 -> can partially withdraw
        // Case 5: 1-run requirement sufficiency (having enough for 1 run means 0 needed from GE for 1 run)
        HerbRunSupplyCalculator.SupplyItem singleRunItem = new HerbRunSupplyCalculator.SupplyItem(
                "Ranarr seed",
                ItemID.RANARR_SEED,
                9, // needed for 1 run of 9 patches
                10, // player has 10 in inventory
                0, // 0 in bank
                true,
                false,
                "Seed"
        );
        assertFalse(singleRunItem.isMissingFromInventory());
        assertEquals("Should need 0 to buy for 1 run", 0, singleRunItem.getGeBuyQuantity(1));
        // But for 5 runs, total needed is 45, player has 10 -> needs 35
        assertEquals(35, singleRunItem.getGeBuyQuantity(5));
    }

    @Test
    public void testBankAlternateTeleportCoverage() {
        // All acceptable Ardougne cloaks must match Ardougne patch
        int[] ardyCloaks = {
                ItemID.ARDY_CAPE_EASY,
                ItemID.ARDY_CAPE_MEDIUM,
                ItemID.ARDY_CAPE_HARD,
                ItemID.ARDY_CAPE_ELITE,
                ItemID.POH_TABLET_ARDOUGNETELEPORT
        };
        for (int cloakId : ardyCloaks) {
            assertTrue("Ardougne patch must match teleport id: " + cloakId,
                    HerbPatch.ARDOUGNE.matchesTeleportId(cloakId));
        }

        // Skills necklaces match Farming Guild
        int[] skillsNecklaces = {
                ItemID.JEWL_NECKLACE_OF_SKILLS_1,
                ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                ItemID.JEWL_NECKLACE_OF_SKILLS_4,
                ItemID.JEWL_NECKLACE_OF_SKILLS_5,
                ItemID.JEWL_NECKLACE_OF_SKILLS_6
        };
        for (int neckId : skillsNecklaces) {
            assertTrue("Farming guild must match skills necklace: " + neckId,
                    HerbPatch.FARMING_GUILD.matchesTeleportId(neckId));
        }

        // Lumbridge explorer's rings match Falador patch
        int[] rings = {
                ItemID.LUMBRIDGE_RING_EASY,
                ItemID.LUMBRIDGE_RING_MEDIUM,
                ItemID.LUMBRIDGE_RING_HARD,
                ItemID.LUMBRIDGE_RING_ELITE,
                ItemID.TELETAB_DRAYNOR,
                ItemID.POH_TABLET_FALADORTELEPORT
        };
        for (int ringId : rings) {
            assertTrue("Falador patch must match ring/teleport id: " + ringId,
                    HerbPatch.FALADOR.matchesTeleportId(ringId));
        }
    }

    @Test
    public void testUnnotedHerbDetection() {
        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel unnotedGrimy = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(unnotedGrimy.getName()).thenReturn("Grimy ranarr weed");
        org.mockito.Mockito.when(unnotedGrimy.isNoted()).thenReturn(false);
        assertTrue("Unnoted grimy ranarr should be detected as unnoted herb", HerbRunPlugin.isUnnotedHerb(unnotedGrimy));

        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel notedGrimy = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(notedGrimy.getName()).thenReturn("Grimy ranarr weed");
        org.mockito.Mockito.when(notedGrimy.isNoted()).thenReturn(true);
        assertFalse("Noted grimy ranarr should NOT be detected as unnoted herb", HerbRunPlugin.isUnnotedHerb(notedGrimy));

        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel notedIrit = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(notedIrit.getName()).thenReturn("Grimy irit leaf");
        org.mockito.Mockito.when(notedIrit.isNoted()).thenReturn(true);
        assertFalse("Noted grimy irit leaf must NOT prompt noting on leprechaun", HerbRunPlugin.isUnnotedHerb(notedIrit));

        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel unnotedIrit = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(unnotedIrit.getName()).thenReturn("Grimy irit leaf");
        org.mockito.Mockito.when(unnotedIrit.isNoted()).thenReturn(false);
        assertTrue("Unnoted grimy irit leaf MUST prompt noting on leprechaun", HerbRunPlugin.isUnnotedHerb(unnotedIrit));

        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel cleanRanarr = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(cleanRanarr.getName()).thenReturn("Ranarr weed");
        org.mockito.Mockito.when(cleanRanarr.isNoted()).thenReturn(false);
        assertTrue("Clean unnoted ranarr weed should be detected as unnoted herb", HerbRunPlugin.isUnnotedHerb(cleanRanarr));

        net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel nonHerb = org.mockito.Mockito.mock(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel.class);
        org.mockito.Mockito.when(nonHerb.getName()).thenReturn("Ranarr seed");
        org.mockito.Mockito.when(nonHerb.isNoted()).thenReturn(false);
        assertFalse("Seed should not be detected as unnoted herb", HerbRunPlugin.isUnnotedHerb(nonHerb));
    }

    @Test
    public void testHosidiusAndVarlamoreHouseTeleports() {
        assertEquals("Teleport to house", HerbPatch.HOSIDIUS.getPrimaryTeleportName());
        assertEquals(ItemID.POH_TABLET_TELEPORTTOHOUSE, HerbPatch.HOSIDIUS.getPrimaryTeleportItemId());

        assertEquals("Teleport to house", HerbPatch.VARLAMORE.getPrimaryTeleportName());
        assertEquals(ItemID.POH_TABLET_TELEPORTTOHOUSE, HerbPatch.VARLAMORE.getPrimaryTeleportItemId());

        // Verify previous items are strictly excluded from both patches
        int[] previousHosidiusIds = {
                ItemID.POH_TABLET_KOURENDTELEPORT,
                ItemID.NZONE_TELETAB_KOUREND,
                ItemID.XERIC_TALISMAN,
                ItemID.VEOS_KHAREDSTS_MEMOIRS,
                ItemID.BOOK_OF_THE_DEAD
        };
        for (int oldId : previousHosidiusIds) {
            assertFalse("Hosidius patch must not match old item: " + oldId, HerbPatch.HOSIDIUS.matchesTeleportId(oldId));
        }

        int[] previousVarlamoreIds = {
                ItemID.HG_QUETZALWHISTLE_BASIC,
                ItemID.HG_QUETZALWHISTLE_ENHANCED,
                ItemID.HG_QUETZALWHISTLE_PERFECTED,
                ItemID.POH_TABLET_FORTISTELEPORT
        };
        for (int oldId : previousVarlamoreIds) {
            assertFalse("Varlamore patch must not match old item: " + oldId, HerbPatch.VARLAMORE.matchesTeleportId(oldId));
        }

        // Verify house tabs and construction capes are accepted by both
        int[] validHouseTeleportIds = {
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                ItemID.SKILLCAPE_CONSTRUCTION,
                ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED,
                ItemID.SKILLCAPE_MAX
        };
        for (int validId : validHouseTeleportIds) {
            assertTrue("Hosidius must accept POH teleport id: " + validId, HerbPatch.HOSIDIUS.matchesTeleportId(validId));
            assertTrue("Varlamore must accept POH teleport id: " + validId, HerbPatch.VARLAMORE.matchesTeleportId(validId));
        }
    }
}
