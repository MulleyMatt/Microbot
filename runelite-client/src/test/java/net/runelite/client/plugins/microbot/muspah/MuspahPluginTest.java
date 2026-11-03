package net.runelite.client.plugins.microbot.muspah;

import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahPrayerMarkerOverlay;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.microbot.util.magic.thralls.Rs2Thrall;
import net.runelite.client.plugins.microbot.util.magic.thralls.ThrallType;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MuspahPluginTest
{
    @Test
    public void testMuspahFormNpcMappingsAndWeaponPrompts()
    {
        assertEquals(MuspahForm.RANGED, MuspahForm.fromNpcId(NpcID.PHANTOM_MUSPAH));
        assertEquals(MuspahForm.MELEE, MuspahForm.fromNpcId(NpcID.PHANTOM_MUSPAH_12078));
        assertEquals(MuspahForm.PRAYER_SHIELD, MuspahForm.fromNpcId(NpcID.PHANTOM_MUSPAH_12079));
        assertEquals(MuspahForm.POST_SHIELD, MuspahForm.fromNpcId(NpcID.PHANTOM_MUSPAH_12080));
        assertEquals(MuspahForm.TELEPORTING, MuspahForm.fromNpcId(NpcID.PHANTOM_MUSPAH_12082));
        assertNull(MuspahForm.fromNpcId(999999));

        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahForm.RANGED.getDefaultPrayer());
        assertEquals(Prayer.PROTECT_FROM_MELEE, MuspahForm.MELEE.getDefaultPrayer());
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahForm.PRAYER_SHIELD.getDefaultPrayer());
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahForm.POST_SHIELD.getDefaultPrayer());

        assertEquals("Bowfa", MuspahForm.RANGED.getWeaponPrompt());
        assertEquals("Bowfa + Kite", MuspahForm.MELEE.getWeaponPrompt());
        assertEquals("Sapphire Bolts + Smite", MuspahForm.PRAYER_SHIELD.getWeaponPrompt());
        assertEquals("Bowfa", MuspahForm.POST_SHIELD.getWeaponPrompt());

        assertEquals(MuspahForm.MELEE, MuspahForm.RANGED.getNextRotationForm());
        assertEquals(MuspahForm.RANGED, MuspahForm.MELEE.getNextRotationForm());
        assertEquals(MuspahForm.POST_SHIELD, MuspahForm.PRAYER_SHIELD.getNextRotationForm());
        assertNull(MuspahForm.POST_SHIELD.getNextRotationForm());
    }

    @Test
    public void testSpecialAttackAnimationMappings()
    {
        MuspahSpecialAttack magic = MuspahSpecialAttack.fromAnimationId(AnimationID.NPC_MUSPAH_ATTACK_MAGIC_01);
        assertNotNull(magic);
        assertEquals(MuspahSpecialAttack.CHARGED_MAGIC, magic);
        assertEquals(Prayer.PROTECT_FROM_MAGIC, magic.getOverridePrayer());
        assertEquals(4, magic.getDurationTicks());

        MuspahSpecialAttack shockwave = MuspahSpecialAttack.fromAnimationId(AnimationID.NPC_MUSPAH_ATTACK_EXPLOSION_01);
        assertNotNull(shockwave);
        assertEquals(MuspahSpecialAttack.CENTER_SHOCKWAVE, shockwave);
        assertEquals(6, shockwave.getDurationTicks());

        MuspahPlugin plugin = new MuspahPlugin();
        plugin.setCurrentFormForTest(MuspahForm.RANGED);
        plugin.handleMuspahAnimation(AnimationID.NPC_MUSPAH_ATTACK_MAGIC_01);
        assertEquals(4, plugin.getMagicAttackTicks());
        assertEquals(5, plugin.getAttackTicks());
        assertEquals(MuspahSpecialAttack.CHARGED_MAGIC, plugin.getActiveSpecialAttack());

        plugin.handleMuspahAnimation(AnimationID.NPC_MUSPAH_ATTACK_EXPLOSION_01);
        assertEquals(6, plugin.getShockwaveTicks());
        assertEquals(MuspahSpecialAttack.CENTER_SHOCKWAVE, plugin.getActiveSpecialAttack());
    }

    @Test
    public void testShockwaveSafeTileCalculation()
    {
        WorldPoint center = new WorldPoint(2848, 4256, 0);

        // Spike to the North at (2848, 4261, 0) -> safe tile behind it should be (2848, 4262, 0)
        WorldPoint spikeNorth = new WorldPoint(2848, 4261, 0);
        // Spike to the South-East at (2852, 4252, 0) -> safe tile behind it should be (2853, 4251, 0)
        WorldPoint spikeSouthEast = new WorldPoint(2852, 4252, 0);
        // Double-stacked spike to the West: (2843, 4256) and (2842, 4256) -> (2842, 4256) is blocked by spike, (2841, 4256) is safe
        WorldPoint spikeWest1 = new WorldPoint(2843, 4256, 0);
        WorldPoint spikeWest2 = new WorldPoint(2842, 4256, 0);

        Set<WorldPoint> safeTiles = MuspahPlugin.calculateShockwaveSafeTiles(
                center,
                MuspahPlugin.MUSPAH_SIZE,
                Arrays.asList(spikeNorth, spikeSouthEast, spikeWest1, spikeWest2)
        );

        assertTrue(safeTiles.contains(new WorldPoint(2848, 4262, 0)));
        assertTrue(safeTiles.contains(new WorldPoint(2853, 4251, 0)));
        assertFalse(safeTiles.contains(new WorldPoint(2842, 4256, 0)));
        assertTrue(safeTiles.contains(new WorldPoint(2841, 4256, 0)));
        assertEquals(3, safeTiles.size());

        assertTrue(MuspahPlugin.calculateShockwaveSafeTiles(center, 5, Collections.emptyList()).isEmpty());
    }

    @Test
    public void testMeleeDangerDistanceCalculation()
    {
        // Muspah 5x5 SW tile at (2846, 4254, 0) -> occupies X:[2846..2850], Y:[4254..4258]
        WorldPoint muspahSw = new WorldPoint(2846, 4254, 0);

        // Inside footprint -> distance 0 (in danger)
        WorldPoint inside = new WorldPoint(2848, 4256, 0);
        assertEquals(0, MuspahPlugin.getDistanceToMuspahFootprint(muspahSw, 5, inside));
        assertTrue(MuspahPlugin.isPlayerInMeleeDanger(muspahSw, 5, inside));

        // 1 tile East (adjacent melee range) -> distance 1 (in danger)
        WorldPoint adjacent = new WorldPoint(2851, 4256, 0);
        assertEquals(1, MuspahPlugin.getDistanceToMuspahFootprint(muspahSw, 5, adjacent));
        assertTrue(MuspahPlugin.isPlayerInMeleeDanger(muspahSw, 5, adjacent));

        // 2 tiles East (1 step from melee range) -> distance 2 (in danger)
        WorldPoint oneStepAway = new WorldPoint(2852, 4256, 0);
        assertEquals(2, MuspahPlugin.getDistanceToMuspahFootprint(muspahSw, 5, oneStepAway));
        assertTrue(MuspahPlugin.isPlayerInMeleeDanger(muspahSw, 5, oneStepAway));

        // 3 tiles East (safe kiting distance) -> distance 3 (safe)
        WorldPoint safeKiting = new WorldPoint(2853, 4256, 0);
        assertEquals(3, MuspahPlugin.getDistanceToMuspahFootprint(muspahSw, 5, safeKiting));
        assertFalse(MuspahPlugin.isPlayerInMeleeDanger(muspahSw, 5, safeKiting));
    }

    @Test
    public void testPrayerSpriteAndWidgetMappings()
    {
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MAGIC, MuspahPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MISSILES, MuspahPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MELEE, MuspahPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MELEE));
        assertEquals(SpriteID.PRAYER_SMITE, MuspahPrayerHelperOverlay.prayerToSpriteId(Prayer.SMITE));

        assertEquals(InterfaceID.Prayerbook.PRAYER13, MuspahPrayerMarkerOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(InterfaceID.Prayerbook.PRAYER14, MuspahPrayerMarkerOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(InterfaceID.Prayerbook.PRAYER15, MuspahPrayerMarkerOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MELEE));
        assertEquals(InterfaceID.Prayerbook.PRAYER18, MuspahPrayerMarkerOverlay.prayerToComponentId(Prayer.SMITE));
    }

    @Test
    public void testAutoPrayerResolutionAndMapping()
    {
        // Standard forms without charged magic
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.RANGED, -1, 3, false));
        assertEquals(Prayer.PROTECT_FROM_MELEE, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.MELEE, -1, 3, false));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.POST_SHIELD, -1, 3, false));

        // Charged magic overrides to Protect from Magic
        assertEquals(Prayer.PROTECT_FROM_MAGIC, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.RANGED, 4, 5, false));
        assertEquals(Prayer.PROTECT_FROM_MAGIC, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.PRAYER_SHIELD, 2, 3, true));

        // Prayer Shield Smite flicking when enabled
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.PRAYER_SHIELD, -1, 3, false));
        assertEquals(Prayer.SMITE, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.PRAYER_SHIELD, -1, 3, true));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.PRAYER_SHIELD, -1, 1, true));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, MuspahPlugin.resolveAutoOverheadPrayer(MuspahForm.PRAYER_SHIELD, -1, 5, true));

        // Mapping to Rs2PrayerEnum
        assertEquals(Rs2PrayerEnum.PROTECT_MAGIC, MuspahPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Rs2PrayerEnum.PROTECT_RANGE, MuspahPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Rs2PrayerEnum.PROTECT_MELEE, MuspahPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MELEE));
        assertEquals(Rs2PrayerEnum.SMITE, MuspahPlugin.toRs2PrayerEnum(Prayer.SMITE));
    }

    @Test
    public void testAutoThrallConditionsAndTierResolution()
    {
        int arceuusVarbit = Rs2Spellbook.ARCEUUS.getValue();

        // Targetable forms (Ranged, Melee, Prayer Shield, Post Shield) when off cooldown -> true
        assertTrue(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, false, false, arceuusVarbit, true, 10, 0));
        assertTrue(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.MELEE, false, false, arceuusVarbit, true, 10, 0));
        assertTrue(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.PRAYER_SHIELD, false, false, arceuusVarbit, true, 10, 0));
        assertTrue(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.POST_SHIELD, false, false, arceuusVarbit, true, 10, 0));

        // Skip during dark-screen TELEPORTING phase, dead boss, active/cooldown thrall, wrong spellbook, missing Book of the dead, or recast debounce
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.TELEPORTING, false, false, arceuusVarbit, true, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(null, false, false, arceuusVarbit, true, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, true, false, arceuusVarbit, true, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, false, true, arceuusVarbit, true, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, false, false, Rs2Spellbook.MODERN.getValue(), true, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, false, false, arceuusVarbit, false, 10, 0));
        assertFalse(MuspahPlugin.shouldAttemptThrallCast(MuspahForm.RANGED, false, false, arceuusVarbit, true, 5, 3));

        // Highest tier unlocked resolution (Greater -> Superior -> Lesser)
        assertEquals(Rs2Thrall.GREATER_SKELETON, MuspahPlugin.resolveBestThrall(ThrallType.RANGED, 99, 50, t -> true));
        assertEquals(Rs2Thrall.SUPERIOR_SKELETON, MuspahPlugin.resolveBestThrall(ThrallType.RANGED, 70, 50, t -> true));
        assertEquals(Rs2Thrall.LESSER_SKELETON, MuspahPlugin.resolveBestThrall(ThrallType.RANGED, 45, 50, t -> true));
        assertNull(MuspahPlugin.resolveBestThrall(ThrallType.RANGED, 30, 50, t -> true));

        // Falls back to lower tier if prayer points or runes are insufficient for Greater
        assertEquals(Rs2Thrall.SUPERIOR_GHOST, MuspahPlugin.resolveBestThrall(ThrallType.MAGIC, 99, 5, t -> true));
        assertEquals(Rs2Thrall.SUPERIOR_ZOMBIE, MuspahPlugin.resolveBestThrall(
                ThrallType.MELEE, 99, 50, t -> t != Rs2Thrall.GREATER_ZOMBIE));

        // Component ID mappings
        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON, MuspahPlugin.thrallToComponentId(Rs2Thrall.GREATER_SKELETON));
        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST, MuspahPlugin.thrallToComponentId(Rs2Thrall.GREATER_GHOST));
        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE, MuspahPlugin.thrallToComponentId(Rs2Thrall.GREATER_ZOMBIE));
        assertEquals(ThrallType.RANGED, MuspahConfig.ThrallStyle.RANGED.getThrallType());
        assertEquals(ThrallType.MAGIC, MuspahConfig.ThrallStyle.MAGIC.getThrallType());
        assertEquals(ThrallType.MELEE, MuspahConfig.ThrallStyle.MELEE.getThrallType());
    }

    @Test
    public void testLowPrayerAlertTriggerLogic()
    {
        // Fires immediately when prayer drops to 10 (or below) and alert is off cooldown
        assertTrue(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.RANGED, false, 10, 10, 20, -8));
        assertTrue(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.PRAYER_SHIELD, false, 7, 10, 20, 10));

        // Does not fire when prayer is above 10 (e.g. 11)
        assertFalse(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.RANGED, false, 11, 10, 20, -8));

        // Respects cooldown between repeated chimes while still <= 10
        assertFalse(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.RANGED, false, 10, 10, 25, 20));
        assertTrue(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.RANGED, false, 10, 10, 28, 20));

        // Does not fire when disabled, boss absent, or boss dead
        assertFalse(MuspahPlugin.shouldTriggerLowPrayerAlert(false, MuspahForm.RANGED, false, 10, 10, 20, -8));
        assertFalse(MuspahPlugin.shouldTriggerLowPrayerAlert(true, null, false, 10, 10, 20, -8));
        assertFalse(MuspahPlugin.shouldTriggerLowPrayerAlert(true, MuspahForm.RANGED, true, 10, 10, 20, -8));
    }
}
