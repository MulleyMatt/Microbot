package net.runelite.client.plugins.microbot.redwood;

import net.runelite.api.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RedwoodHelperTest {

    @Test
    public void testAxeAndGearCollections() {
        // Assert common axes are covered
        assertTrue("Dragon axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.DRAGON_AXE));
        assertTrue("Crystal axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.CRYSTAL_AXE));
        assertTrue("Infernal axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.INFERNAL_AXE));
        assertTrue("Rune axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.RUNE_AXE));
        assertTrue("Bronze axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.BRONZE_AXE));
        assertTrue("Dragon 2H axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.DRAGON_AXE_2H));
        assertTrue("Rune 2H axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID.RUNE_AXE_2H));
        assertTrue("3rd Age axe must be supported", RedwoodSupplyCalculator.AXE_IDS.contains(ItemID._3A_AXE));

        // Tradeable axes
        assertTrue(RedwoodSupplyCalculator.TRADEABLE_AXES.contains(ItemID.DRAGON_AXE));
        assertTrue(RedwoodSupplyCalculator.TRADEABLE_AXES.contains(ItemID.RUNE_AXE));
        assertTrue(RedwoodSupplyCalculator.TRADEABLE_AXES.contains(ItemID.DRAGON_AXE_2H));
        assertFalse("Crystal axe is untradeable", RedwoodSupplyCalculator.TRADEABLE_AXES.contains(ItemID.CRYSTAL_AXE));
        assertFalse("Infernal axe is untradeable", RedwoodSupplyCalculator.TRADEABLE_AXES.contains(ItemID.INFERNAL_AXE));

        // Lumberjack outfit pieces
        assertEquals("Should have 8 lumberjack pieces (4 standard + 4 forestry)", 8, RedwoodSupplyCalculator.LUMBERJACK_PIECES.size());
        assertTrue(RedwoodSupplyCalculator.LUMBERJACK_PIECES.contains(ItemID.RAMBLE_LUMBERJACK_HAT));
        assertTrue(RedwoodSupplyCalculator.LUMBERJACK_PIECES.contains(ItemID.FORESTRY_LUMBERJACK_HAT));
        assertTrue(RedwoodSupplyCalculator.LUMBERJACK_PIECES.contains(ItemID.RAMBLE_LUMBERJACK_TOP));
        assertTrue(RedwoodSupplyCalculator.LUMBERJACK_PIECES.contains(ItemID.RAMBLE_LUMBERJACK_LEGS));
        assertTrue(RedwoodSupplyCalculator.LUMBERJACK_PIECES.contains(ItemID.RAMBLE_LUMBERJACK_BOOTS));
    }

    @Test
    public void testRedwoodTreeIdClassification() {
        // Ready tree spots
        assertTrue(RedwoodSceneOverlay.isReadyRedwood(ObjectID.REDWOODTREE_L));
        assertTrue(RedwoodSceneOverlay.isReadyRedwood(ObjectID.REDWOODTREE_R));
        assertTrue(RedwoodSceneOverlay.isReadyRedwood(ObjectID.REDWOOD_TREE_FULLYGROWN_1_2));
        assertFalse(RedwoodSceneOverlay.isReadyRedwood(ObjectID.REDWOODTREE_L_CUT));
        assertFalse(RedwoodSceneOverlay.isReadyRedwood(ObjectID.REDWOODTREE_R_CUT));

        // Depleted tree spots
        assertTrue(RedwoodSceneOverlay.isDepletedRedwood(ObjectID.REDWOODTREE_L_CUT));
        assertTrue(RedwoodSceneOverlay.isDepletedRedwood(ObjectID.REDWOODTREE_R_CUT));
        assertTrue(RedwoodSceneOverlay.isDepletedRedwood(ObjectID.FARMING_REDWOOD_TREE_PATCH_1_2));
        assertFalse(RedwoodSceneOverlay.isDepletedRedwood(ObjectID.REDWOODTREE_L));
        assertFalse(RedwoodSceneOverlay.isDepletedRedwood(ObjectID.REDWOODTREE_R));
    }

    @Test
    public void testWoodcuttingAnimations() {
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_BRONZE));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_RUNE));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_DRAGON));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_CRYSTAL));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_INFERNAL));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_3A_AXE));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_2H_DRAGON));
        assertTrue(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.WOODCUTTING_2H_RUNE));

        assertFalse(RedwoodPlugin.WOODCUTTING_ANIMS.contains(-1));
        assertFalse(RedwoodPlugin.WOODCUTTING_ANIMS.contains(AnimationID.IDLE));
    }

    @Test
    public void testRedwoodStepsEnum() {
        for (RedwoodStep step : RedwoodStep.values()) {
            assertNotNull(step.getLabel());
            assertNotNull(step.getDescription());
            assertFalse(step.getLabel().isEmpty());
            assertFalse(step.getDescription().isEmpty());
        }

        assertEquals("Woodcutting", RedwoodStep.WOODCUTTING.getLabel());
        assertEquals("AFK / Inactive", RedwoodStep.IDLE_AFK.getLabel());
        assertEquals("Inventory Full", RedwoodStep.INVENTORY_FULL.getLabel());
        assertEquals("Bank Logs", RedwoodStep.BANKING.getLabel());
        assertEquals("Climb Ladder", RedwoodStep.CLIMBING_UP.getLabel());
        assertEquals("Climb Down", RedwoodStep.CLIMBING_DOWN.getLabel());
    }

    @Test
    public void testAfkInactivityThresholdLogic() {
        Instant now = Instant.now();
        Instant stoppedRecent = now.minusMillis(500); // 0.5s ago
        Instant stoppedOverThreshold = now.minusMillis(2500); // 2.5s ago

        int thresholdSeconds = 2;
        long thresholdMillis = thresholdSeconds * 1000L;

        boolean isAfkRecent = Duration.between(stoppedRecent, now).toMillis() >= thresholdMillis;
        boolean isAfkOverThreshold = Duration.between(stoppedOverThreshold, now).toMillis() >= thresholdMillis;

        assertFalse("0.5s since stopped chopping should not trigger AFK on a 2.0s threshold", isAfkRecent);
        assertTrue("2.5s since stopped chopping must trigger AFK on a 2.0s threshold", isAfkOverThreshold);
    }

    @Test
    public void testFlashStyleEnum() {
        assertEquals("Pulse", FlashStyle.PULSE.toString());
        assertEquals("Flash", FlashStyle.FLASH.toString());
        assertEquals("Solid", FlashStyle.SOLID.toString());
    }

    @Test
    public void testSpecialAttackAxeCollectionsAndThreshold() {
        assertTrue("Dragon axe must have special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.DRAGON_AXE));
        assertTrue("Dragon 2H axe must have special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.DRAGON_AXE_2H));
        assertTrue("Crystal axe must have special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.CRYSTAL_AXE));
        assertTrue("Infernal axe must have special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.INFERNAL_AXE));
        assertTrue("3rd Age axe must have special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID._3A_AXE));
        assertFalse("Rune axe has no special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.RUNE_AXE));
        assertFalse("Adamant axe has no special attack", RedwoodSupplyCalculator.SPEC_AXE_IDS.contains(ItemID.ADAMANT_AXE));

        // In OSRS, 1000 is 100% special attack energy
        int specEnergyFull = 1000;
        int specEnergyAlmostFull = 990;

        assertTrue("1000 spec energy is 100%", specEnergyFull >= 1000);
        assertFalse("990 spec energy is not 100%", specEnergyAlmostFull >= 1000);
    }

    @Test
    public void testFlashTypeEnum() {
        assertEquals(RedwoodPlugin.FlashType.NONE, RedwoodPlugin.FlashType.valueOf("NONE"));
        assertEquals(RedwoodPlugin.FlashType.INVENTORY_FULL, RedwoodPlugin.FlashType.valueOf("INVENTORY_FULL"));
        assertEquals(RedwoodPlugin.FlashType.AFK, RedwoodPlugin.FlashType.valueOf("AFK"));
        assertEquals(RedwoodPlugin.FlashType.SPECIAL_ATTACK, RedwoodPlugin.FlashType.valueOf("SPECIAL_ATTACK"));
    }

    @Test
    public void testSoundRepeatIntervalMath() {
        Instant t0 = Instant.now().minusMillis(3500);
        Instant now = Instant.now();
        int intervalSeconds = 3;
        long elapsedMillis = Duration.between(t0, now).toMillis();

        assertTrue("3500ms is >= 3000ms, repeat sound should fire", elapsedMillis >= (long) intervalSeconds * 1000L);

        Instant tRecent = Instant.now().minusMillis(1200);
        long elapsedRecent = Duration.between(tRecent, now).toMillis();
        assertFalse("1200ms is < 3000ms, sound should not repeat yet", elapsedRecent >= (long) intervalSeconds * 1000L);
    }

    @Test
    public void testBankingMovementOptionRecognition() {
        RedwoodPlugin plugin = new RedwoodPlugin();

        assertTrue(plugin.isBankingOrMovementOption("Walk here"));
        assertTrue(plugin.isBankingOrMovementOption("walk here"));
        assertTrue(plugin.isBankingOrMovementOption("Climb"));
        assertTrue(plugin.isBankingOrMovementOption("Climb-up"));
        assertTrue(plugin.isBankingOrMovementOption("Climb-down"));
        assertTrue(plugin.isBankingOrMovementOption("Bank"));
        assertTrue(plugin.isBankingOrMovementOption("Use"));
        assertTrue(plugin.isBankingOrMovementOption("Open"));
        assertTrue(plugin.isBankingOrMovementOption("Deposit"));
        assertTrue(plugin.isBankingOrMovementOption("Deposit-all"));

        assertFalse(plugin.isBankingOrMovementOption("Chop"));
        assertFalse(plugin.isBankingOrMovementOption("Drop"));
        assertFalse(plugin.isBankingOrMovementOption("Examine"));
        assertFalse(plugin.isBankingOrMovementOption(null));
    }

    @Test
    public void testBankingGracePeriodThreshold() {
        Instant now = Instant.now();
        Instant withinGrace = now.minusMillis(3000); // 3 seconds ago (e.g. descending ladder)
        Instant outsideGrace = now.minusMillis(11000); // 11 seconds ago (went AFK)

        int gracePeriod = RedwoodPlugin.BANKING_GRACE_PERIOD_SECONDS;
        assertEquals(10, gracePeriod);

        boolean isWithinGrace = Duration.between(withinGrace, now).getSeconds() < gracePeriod;
        boolean isOutsideGrace = Duration.between(outsideGrace, now).getSeconds() < gracePeriod;

        assertTrue("3s since last movement should remain within 10s banking grace period", isWithinGrace);
        assertFalse("11s since last movement should exceed 10s banking grace period", isOutsideGrace);
    }
}
