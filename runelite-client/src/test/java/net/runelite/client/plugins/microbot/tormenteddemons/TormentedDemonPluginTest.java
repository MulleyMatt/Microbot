package net.runelite.client.plugins.microbot.tormenteddemons;

import net.runelite.api.HeadIcon;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonWidgetOverlay;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.microbot.util.magic.thralls.Rs2Thrall;
import net.runelite.client.plugins.microbot.util.magic.thralls.ThrallType;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TormentedDemonPluginTest
{
    @Test
    public void testPostFireBombStylePredictionMatrix()
    {
        // Moving toward player always predicts Protect from Melee regardless of previous style
        assertEquals(Prayer.PROTECT_FROM_MELEE,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MAGIC, true, false));
        assertEquals(Prayer.PROTECT_FROM_MELEE,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MISSILES, true, true));
        assertEquals(Prayer.PROTECT_FROM_MELEE,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MELEE, true, false));

        // Before 2 ticks have passed without movement, do not guess off Magic/Ranged yet
        assertNull(TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MAGIC, false, false));
        assertNull(TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MISSILES, false, false));

        // Stationary confirmed after 2+ ticks: Magic <-> Ranged swap, Melee -> Ranged fallback
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MAGIC, false, true));
        assertEquals(Prayer.PROTECT_FROM_MAGIC,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MISSILES, false, true));
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                TormentedDemonPlugin.predictPostFireBombPrayer(Prayer.PROTECT_FROM_MELEE, false, true));
        assertNull(TormentedDemonPlugin.predictPostFireBombPrayer(null, false, true));
    }

    @Test
    public void testAnimationAndProjectilePrayerMappings()
    {
        assertEquals(Prayer.PROTECT_FROM_MAGIC,
                TormentedDemonPlugin.resolvePrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_FIREY_BALLS));
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                TormentedDemonPlugin.resolvePrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_SPARE_RIBS));
        assertEquals(Prayer.PROTECT_FROM_MELEE,
                TormentedDemonPlugin.resolvePrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_MELEE));
        assertNull(TormentedDemonPlugin.resolvePrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_EXPLOSION_FIRE));

        assertEquals(Prayer.PROTECT_FROM_MAGIC,
                TormentedDemonPlugin.resolvePrayerFromProjectile(SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_PROJ));
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                TormentedDemonPlugin.resolvePrayerFromProjectile(SpotanimID.LUC2_RIB_BONE_SHARD_PROJECTILE));
        assertNull(TormentedDemonPlugin.resolvePrayerFromProjectile(SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_LARGE_PROJ));

        assertEquals(Rs2PrayerEnum.PROTECT_MAGIC, TormentedDemonPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Rs2PrayerEnum.PROTECT_RANGE, TormentedDemonPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Rs2PrayerEnum.PROTECT_MELEE, TormentedDemonPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MELEE));

        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MAGIC, TormentedDemonPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MISSILES, TormentedDemonPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MELEE, TormentedDemonPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MELEE));

        assertEquals(InterfaceID.Prayerbook.PRAYER13, TormentedDemonWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(InterfaceID.Prayerbook.PRAYER14, TormentedDemonWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(InterfaceID.Prayerbook.PRAYER15, TormentedDemonWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MELEE));
    }

    @Test
    public void testStateDamageSwapShieldAndDefencelessLifecycle()
    {
        NPC demon = mock(NPC.class);
        TormentedDemonState state = new TormentedDemonState(demon);

        // Engage on tick 10 -> Shield Down active for 1 punish hit
        state.onFirstHitEngaged(10);
        assertTrue(state.isInCombatWithPlayer());
        assertTrue(state.isShieldDown());
        assertFalse(state.isDefenceless());
        assertEquals(150, state.getDamageRemainingToPrayerSwap());

        // Same-tick hitsplat does not prematurely clear Shield Down
        state.onPlayerHitConsumedShield(10);
        assertTrue(state.isShieldDown());

        // Punish hit on tick 11 consumes Shield Down
        state.recordDamage(65);
        state.onPlayerHitConsumedShield(11);
        assertFalse(state.isShieldDown());
        assertEquals(65, state.getDamageSincePrayerSwap());
        assertEquals(85, state.getDamageRemainingToPrayerSwap());

        // Cross 150 damage threshold and observe demon overhead swap -> resets damage counter
        state.recordDamage(90);
        assertEquals(0, state.getDamageRemainingToPrayerSwap());
        Set<HeadIcon> newOverheads = EnumSet.of(HeadIcon.MELEE);
        assertTrue(state.onProtectedStylesChanged(newOverheads));
        assertEquals(0, state.getDamageSincePrayerSwap());
        assertEquals(TormentedDemonState.PRAYER_SWAP_PAUSE_TICKS, state.getPostPrayerSwapPauseTicks());

        // 30 ticks into combat -> flames go out (100% Accuracy)
        for (int i = 0; i < TormentedDemonState.DEFENCELESS_TICKS; i++)
        {
            state.onTick();
        }
        assertTrue(state.isDefenceless());
        assertEquals(0, state.getTicksUntilDefenceless());

        // Fire bomb drops shield again while keeping 100% accuracy until first post-bomb player hit
        state.onConfirmedAttack(Prayer.PROTECT_FROM_MAGIC, 40);
        state.onFireBombStarted(42, new WorldPoint(4136, 4372, 0));
        assertTrue(state.isShieldDown());
        assertTrue(state.isAwaitingPostFireBombStyle());
        assertTrue(state.isDefenceless());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, state.getStyleBeforeFireBomb());

        // Post-fire-bomb player hit on tick 44 consumes both Shield Down & carryover 100% accuracy
        state.onPlayerHitConsumedShield(44);
        assertFalse(state.isShieldDown());
        assertFalse(state.isDefenceless());
        assertEquals(TormentedDemonState.DEFENCELESS_TICKS, state.getTicksUntilDefenceless());
    }

    @Test
    public void testAutoThrallEligibilityAndResolution()
    {
        int arceuus = Rs2Spellbook.ARCEUUS.getValue();

        assertTrue(TormentedDemonPlugin.shouldAttemptThrallCast(true, true, false, arceuus, true, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(false, true, false, arceuus, true, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(true, false, false, arceuus, true, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(true, true, true, arceuus, true, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(true, true, false, Rs2Spellbook.MODERN.getValue(), true, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(true, true, false, arceuus, false, 10, 5));
        assertFalse(TormentedDemonPlugin.shouldAttemptThrallCast(true, true, false, arceuus, true, 10, 8));

        // Magic Ghost (3rd style for Melee+Ranged) selects highest unlocked tier
        assertEquals(Rs2Thrall.GREATER_GHOST, TormentedDemonPlugin.resolveBestThrall(ThrallType.MAGIC, 76, 10, t -> true));
        assertEquals(Rs2Thrall.SUPERIOR_GHOST, TormentedDemonPlugin.resolveBestThrall(ThrallType.MAGIC, 60, 10, t -> true));
        assertEquals(Rs2Thrall.LESSER_GHOST, TormentedDemonPlugin.resolveBestThrall(ThrallType.MAGIC, 40, 10, t -> true));
        assertNull(TormentedDemonPlugin.resolveBestThrall(ThrallType.MAGIC, 76, 1, t -> true));

        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST, TormentedDemonPlugin.thrallToComponentId(Rs2Thrall.GREATER_GHOST));
        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON, TormentedDemonPlugin.thrallToComponentId(Rs2Thrall.GREATER_SKELETON));
        assertEquals(InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE, TormentedDemonPlugin.thrallToComponentId(Rs2Thrall.GREATER_ZOMBIE));
    }

    @Test
    public void testShiftClickWalkHereSwapOverTormentedDemon()
    {
        NPC tdNpc = mock(NPC.class);
        when(tdNpc.getId()).thenReturn(NpcID.TORMENTED_DEMON_1);

        MenuEntry cancelEntry = mock(MenuEntry.class);
        when(cancelEntry.getType()).thenReturn(MenuAction.CANCEL);

        MenuEntry walkEntry = mock(MenuEntry.class);
        when(walkEntry.getType()).thenReturn(MenuAction.WALK);
        when(walkEntry.getOption()).thenReturn("Walk here");

        MenuEntry attackTdEntry = mock(MenuEntry.class);
        when(attackTdEntry.getType()).thenReturn(MenuAction.NPC_SECOND_OPTION);
        when(attackTdEntry.getNpc()).thenReturn(tdNpc);
        when(attackTdEntry.getTarget()).thenReturn("<col=ffff00>Tormented Demon</col>  (level-450)");

        Menu menu = mock(Menu.class);
        MenuEntry[] entries = new MenuEntry[]{cancelEntry, walkEntry, attackTdEntry};
        when(menu.getMenuEntries()).thenReturn(entries);

        assertTrue(TormentedDemonPlugin.swapWalkHereOverTormentedDemon(menu));
        assertSame(attackTdEntry, entries[1]);
        assertSame(walkEntry, entries[2]);
        verify(menu).setMenuEntries(entries);

        // Non-TD NPC should never be swapped
        NPC otherNpc = mock(NPC.class);
        when(otherNpc.getId()).thenReturn(NpcID.GREATER_DEMON);
        MenuEntry attackOtherEntry = mock(MenuEntry.class);
        when(attackOtherEntry.getType()).thenReturn(MenuAction.NPC_SECOND_OPTION);
        when(attackOtherEntry.getNpc()).thenReturn(otherNpc);
        when(attackOtherEntry.getTarget()).thenReturn("<col=ffff00>Greater demon</col>");

        Menu otherMenu = mock(Menu.class);
        MenuEntry[] otherEntries = new MenuEntry[]{cancelEntry, walkEntry, attackOtherEntry};
        when(otherMenu.getMenuEntries()).thenReturn(otherEntries);

        assertFalse(TormentedDemonPlugin.swapWalkHereOverTormentedDemon(otherMenu));
        verify(otherMenu, never()).setMenuEntries(any());
    }

    @Test
    public void testOverheadDecodingGpFormattingAndHelperTags()
    {
        Set<HeadIcon> decoded = TormentedDemonOverlayUtils.extractProtectedStyles(
                new short[]{(short) HeadIcon.RANGE_MAGE.ordinal(), -1}
        );
        assertEquals(EnumSet.of(HeadIcon.RANGED, HeadIcon.MAGIC), decoded);

        assertEquals("850 gp", TormentedDemonOverlayUtils.formatGp(850));
        assertEquals("450K", TormentedDemonOverlayUtils.formatGp(450_000));
        assertEquals("45.2M", TormentedDemonOverlayUtils.formatGp(45_200_000));
        assertEquals(1_000_000L, TormentedDemonOverlayUtils.computeRatePerHour(500_000L, 1_800_000L));

        NPC demon = mock(NPC.class);
        TormentedDemonState state = new TormentedDemonState(demon);
        assertEquals("FIRE!", TormentedDemonPrayerHelperOverlay.resolveBoxTag(true, state));

        state.onFirstHitEngaged(1);
        assertEquals("SHIELD", TormentedDemonPrayerHelperOverlay.resolveBoxTag(false, state));

        state.onPlayerHitConsumedShield(2);
        state.onDefencelessDetected();
        assertEquals("100%", TormentedDemonPrayerHelperOverlay.resolveBoxTag(false, state));
    }
}
