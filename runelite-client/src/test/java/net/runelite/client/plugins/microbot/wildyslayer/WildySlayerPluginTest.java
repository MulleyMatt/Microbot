package net.runelite.client.plugins.microbot.wildyslayer;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WildySlayerPluginTest
{
    @Test
    public void testParseWildernessLevelText()
    {
        assertEquals(24, WildySlayerPlugin.parseWildernessLevelText("Level: 24"));
        assertEquals(30, WildySlayerPlugin.parseWildernessLevelText("Level: 30<br>96-126"));
        assertEquals(1, WildySlayerPlugin.parseWildernessLevelText("Level: 1"));
        assertEquals(0, WildySlayerPlugin.parseWildernessLevelText(""));
        assertEquals(0, WildySlayerPlugin.parseWildernessLevelText(null));
        assertEquals(0, WildySlayerPlugin.parseWildernessLevelText("Safe"));
    }

    @Test
    public void testResolveWildernessLevel()
    {
        // Widget level takes priority when > 0
        assertEquals(22, WildySlayerPlugin.resolveWildernessLevel(22, null));

        // Wilderness Slayer Cave underground tile (3385, 10085, 0) -> (10085 - 9920)/8 + 1 = 21
        WorldPoint slayerCaveTile = new WorldPoint(3385, 10085, 0);
        assertEquals(21, WildySlayerPlugin.resolveWildernessLevel(0, slayerCaveTile));

        // Ferox Enclave safe tile (3135, 3628, 0) -> 0
        WorldPoint feroxTile = new WorldPoint(3135, 3628, 0);
        assertEquals(0, WildySlayerPlugin.resolveWildernessLevel(0, feroxTile));
    }

    @Test
    public void testCanUseRoyalSeedPodAndTriggerConditions()
    {
        // Valid at levels 1 through 30 when not teleblocked
        assertTrue(WildySlayerPlugin.canUseRoyalSeedPod(1, 30, false));
        assertTrue(WildySlayerPlugin.canUseRoyalSeedPod(29, 30, false));
        assertTrue(WildySlayerPlugin.canUseRoyalSeedPod(30, 30, false));

        // Invalid above level 30, outside wilderness (0), or when teleblocked
        assertFalse(WildySlayerPlugin.canUseRoyalSeedPod(31, 30, false));
        assertFalse(WildySlayerPlugin.canUseRoyalSeedPod(0, 30, false));
        assertFalse(WildySlayerPlugin.canUseRoyalSeedPod(20, 30, true));

        // Full trigger check
        assertTrue(WildySlayerPlugin.shouldTriggerSeedPod(
                true, true, 25, 30, false, true, 10, 0
        ));
        // Disabled toggle -> false
        assertFalse(WildySlayerPlugin.shouldTriggerSeedPod(
                false, true, 25, 30, false, true, 10, 0
        ));
        // Above level 30 -> false
        assertFalse(WildySlayerPlugin.shouldTriggerSeedPod(
                true, true, 31, 30, false, true, 10, 0
        ));
        // Cooldown not elapsed (10 - 8 = 2 < 3) -> false
        assertFalse(WildySlayerPlugin.shouldTriggerSeedPod(
                true, true, 25, 30, false, true, 10, 8
        ));
        // Cooldown elapsed (10 - 7 = 3 >= 3) -> true
        assertTrue(WildySlayerPlugin.shouldTriggerSeedPod(
                true, true, 25, 30, false, true, 10, 7
        ));
    }

    @Test
    public void testAntiPkThreatDetection()
    {
        // Level 100 player in Level 25 Wilderness: bracket is 75..125
        assertTrue(WildySlayerPlugin.isThreatPlayer(
                100, 125, 25, false, false, true, false, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        assertTrue(WildySlayerPlugin.isThreatPlayer(
                100, 75, 25, false, false, true, false, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // Level 70 is outside bracket (100 - 25 = 75)
        assertFalse(WildySlayerPlugin.isThreatPlayer(
                100, 70, 25, false, false, true, false, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // Friend/Clan member is ignored when ignoreFriendsAndClan is true
        assertFalse(WildySlayerPlugin.isThreatPlayer(
                100, 100, 25, false, false, true, true, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
        // ANY_PLAYER mode triggers even outside combat bracket
        assertTrue(WildySlayerPlugin.isThreatPlayer(
                126, 3, 25, false, false, true, false, WildySlayerConfig.PkerDetectionMode.ANY_PLAYER
        ));
        // Cross-tile check: local at Wildy 28, PKer 2 tiles north at Wildy 29 with 29 level diff
        assertTrue(WildySlayerPlugin.isThreatPlayer(
                97, 126, 28, 29, false, false, true, false, WildySlayerConfig.PkerDetectionMode.COMBAT_BRACKET
        ));
    }
}
