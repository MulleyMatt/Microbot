package net.runelite.client.plugins.microbot.spindel;

import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelPrayerMarkerOverlay;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpindelPluginTest
{
    @Test
    public void testSpindel16AttackRotationCycle()
    {
        SpindelPhase phase = SpindelPhase.RANGED;
        int attacksInPhase = 0;

        // First 7 Ranged attacks keep Spindel in RANGED phase
        for (int i = 1; i <= 7; i++)
        {
            SpindelPlugin.RotationState state = SpindelPlugin.computeNextRotationState(
                    phase,
                    attacksInPhase,
                    SpindelPlugin.ANIM_RANGED
            );
            assertEquals(SpindelPhase.RANGED, state.getPhase());
            assertEquals(i, state.getAttacksInPhase());
            assertFalse(state.isPhaseSwitched());
            phase = state.getPhase();
            attacksInPhase = state.getAttacksInPhase();
        }

        // 8th Ranged attack immediately transitions to MAGIC phase (0 attacks done in Magic)
        SpindelPlugin.RotationState eighthRanged = SpindelPlugin.computeNextRotationState(
                phase,
                attacksInPhase,
                SpindelPlugin.ANIM_RANGED
        );
        assertEquals(SpindelPhase.MAGIC, eighthRanged.getPhase());
        assertEquals(0, eighthRanged.getAttacksInPhase());
        assertTrue(eighthRanged.isPhaseSwitched());
        phase = eighthRanged.getPhase();
        attacksInPhase = eighthRanged.getAttacksInPhase();

        // Next 7 Magic attacks keep Spindel in MAGIC phase
        for (int i = 1; i <= 7; i++)
        {
            SpindelPlugin.RotationState state = SpindelPlugin.computeNextRotationState(
                    phase,
                    attacksInPhase,
                    SpindelPlugin.ANIM_MAGIC
            );
            assertEquals(SpindelPhase.MAGIC, state.getPhase());
            assertEquals(i, state.getAttacksInPhase());
            assertFalse(state.isPhaseSwitched());
            phase = state.getPhase();
            attacksInPhase = state.getAttacksInPhase();
        }

        // 8th Magic attack transitions back to RANGED phase
        SpindelPlugin.RotationState eighthMagic = SpindelPlugin.computeNextRotationState(
                phase,
                attacksInPhase,
                SpindelPlugin.ANIM_MAGIC
        );
        assertEquals(SpindelPhase.RANGED, eighthMagic.getPhase());
        assertEquals(0, eighthMagic.getAttacksInPhase());
        assertTrue(eighthMagic.isPhaseSwitched());
    }

    @Test
    public void testRotationSelfSyncMidKill()
    {
        // If player enters cave mid-kill while tracker defaults to RANGED (0),
        // but Spindel fires a MAGIC attack, tracker immediately syncs to MAGIC (1/8)
        SpindelPlugin.RotationState synced = SpindelPlugin.computeNextRotationState(
                SpindelPhase.RANGED,
                0,
                SpindelPlugin.ANIM_MAGIC
        );
        assertEquals(SpindelPhase.MAGIC, synced.getPhase());
        assertEquals(1, synced.getAttacksInPhase());
        assertTrue(synced.isPhaseSwitched());
    }

    @Test
    public void testNpcAndPrayerMappings()
    {
        assertTrue(SpindelPlugin.isSpindelBossId(NpcID.VENENATIS_SINGLES));
        assertTrue(SpindelPlugin.isSpindelBossId(NpcID.VENENATIS));
        assertFalse(SpindelPlugin.isSpindelBossId(NpcID.SPINDEL_SPIDERLING));

        assertTrue(SpindelPlugin.isSpiderlingId(NpcID.SPINDEL_SPIDERLING));
        assertTrue(SpindelPlugin.isSpiderlingId(NpcID.VENENATIS_SPIDERLING));

        assertEquals(Prayer.PROTECT_FROM_MISSILES, SpindelPhase.RANGED.getDefaultPrayer());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, SpindelPhase.MAGIC.getDefaultPrayer());

        assertEquals(Rs2PrayerEnum.PROTECT_RANGE, SpindelPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Rs2PrayerEnum.PROTECT_MAGIC, SpindelPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Rs2PrayerEnum.PROTECT_MELEE, SpindelPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MELEE));

        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MISSILES, SpindelPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MAGIC, SpindelPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MAGIC));

        assertEquals(InterfaceID.Prayerbook.PRAYER14, SpindelPrayerMarkerOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(InterfaceID.Prayerbook.PRAYER13, SpindelPrayerMarkerOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    public void testAntiPkThreatDetection()
    {
        // Level 100 player in Level 29 Wilderness (Spindel's cave): bracket is 71..126
        assertTrue(SpindelPlugin.isThreatPlayer(
                100, 126, 29, false, false, true, false, SpindelConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        assertTrue(SpindelPlugin.isThreatPlayer(
                100, 71, 29, false, false, true, false, SpindelConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // Level 65 is outside bracket (100 - 29 = 71)
        assertFalse(SpindelPlugin.isThreatPlayer(
                100, 65, 29, false, false, true, false, SpindelConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // Friend/Clan member is ignored when ignoreFriendsAndClan is true
        assertFalse(SpindelPlugin.isThreatPlayer(
                100, 105, 29, false, false, true, true, SpindelConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // ANY_PLAYER mode triggers even outside combat bracket
        assertTrue(SpindelPlugin.isThreatPlayer(
                126, 3, 29, false, false, true, false, SpindelConfig.PkerDetectionMode.ANY_PLAYER
        ));
    }

    @Test
    public void testWebHazardAndSafeEscapeTiles()
    {
        WorldPoint webCenter = new WorldPoint(1630, 11550, 2);
        Set<WorldPoint> hazardTiles = SpindelPlugin.calculateExpandedWebTiles(
                Collections.emptyList(),
                Collections.singletonList(webCenter)
        );

        // 7x7 expansion around center = 49 tiles
        assertEquals(49, hazardTiles.size());
        assertTrue(hazardTiles.contains(webCenter));
        assertTrue(hazardTiles.contains(new WorldPoint(1633, 11553, 2)));
        assertFalse(hazardTiles.contains(new WorldPoint(1634, 11550, 2)));

        // Player standing on edge of web at (1633, 11550, 2), boss at (1620, 11550, 2)
        WorldPoint playerTile = new WorldPoint(1633, 11550, 2);
        WorldPoint bossSw = new WorldPoint(1620, 11550, 2);
        List<WorldPoint> safeTiles = SpindelPlugin.calculateWebSafeEscapeTiles(
                playerTile,
                hazardTiles,
                bossSw,
                SpindelPlugin.SPINDEL_SIZE
        );

        assertFalse(safeTiles.isEmpty());
        for (WorldPoint safe : safeTiles)
        {
            assertFalse(hazardTiles.contains(safe));
        }
        // Closest tile outside the East edge of the web is (1634, 11550, 2) (1 tile East)
        assertEquals(new WorldPoint(1634, 11550, 2), safeTiles.get(0));
    }

    @Test
    public void testSupplyCalculatorRequirementsAndBatchScaling()
    {
        SpindelConfig defaultConfig = new SpindelConfig()
        {
        };
        List<SpindelSupplyCalculator.SupplyRequirement> reqs = SpindelSupplyCalculator.buildRequirements(defaultConfig);
        assertNotNull(reqs);
        assertFalse(reqs.isEmpty());

        Map<Integer, Integer> requiredMap = new LinkedHashMap<>();
        requiredMap.put(ItemID.MM2_ROYAL_SEED_POD, 1);
        requiredMap.put(ItemID.BLIGHTED_4DOSE2RESTORE, 4);
        requiredMap.put(ItemID.BLIGHTED_MANTARAY, 16);
        requiredMap.put(ItemID.LOOTING_BAG, 1);

        Map<Integer, Integer> ownedMap = new LinkedHashMap<>();
        ownedMap.put(ItemID.MM2_ROYAL_SEED_POD, 0);
        ownedMap.put(ItemID.BLIGHTED_4DOSE2RESTORE, 2);
        ownedMap.put(ItemID.BLIGHTED_MANTARAY, 10);
        ownedMap.put(ItemID.LOOTING_BAG, 1);

        // With batchCount = 3:
        // - Seed pod (non-consumable) needs 1 -> missing 1
        // - Blighted super restore (4 * 3 = 12) - 2 owned = 10 missing
        // - Blighted manta ray (16 * 3 = 48) - 10 owned = 38 missing
        // - Looting bag (1 owned) -> not missing
        Map<Integer, Integer> missing = SpindelSupplyCalculator.calculateMissingQuantities(requiredMap, ownedMap, 3);
        assertEquals(Integer.valueOf(1), missing.get(ItemID.MM2_ROYAL_SEED_POD));
        assertEquals(Integer.valueOf(10), missing.get(ItemID.BLIGHTED_4DOSE2RESTORE));
        assertEquals(Integer.valueOf(38), missing.get(ItemID.BLIGHTED_MANTARAY));
        assertFalse(missing.containsKey(ItemID.LOOTING_BAG));
    }
}
