package net.runelite.client.plugins.microbot.wgs;

import net.runelite.api.HeadIcon;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsWidgetOverlay;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WgsPluginTest
{
    @Test
    public void testSurokMechanicPriorityResolution()
    {
        // Bomb in inventory always takes highest priority (needs immediate Alch before detonation)
        assertEquals(
                WgsSurokMechanic.LIT_EXPLOSIVE_INVENTORY,
                WgsSurokMechanic.resolveHighestPriority(true, true, WgsSurokMechanic.SURGE_FIRE, true, true, false)
        );

        // Bomb on ground takes priority over surge orbs and warriors
        assertEquals(
                WgsSurokMechanic.LIT_EXPLOSIVE_GROUND,
                WgsSurokMechanic.resolveHighestPriority(false, true, WgsSurokMechanic.SURGE_EARTH, true, true, false)
        );

        // Elemental Surge orb takes priority over warriors
        assertEquals(
                WgsSurokMechanic.SURGE_EARTH,
                WgsSurokMechanic.resolveHighestPriority(false, false, WgsSurokMechanic.SURGE_EARTH, true, true, false)
        );

        // Strong Warrior (Weaken) > Agile Warrior (Bind)
        assertEquals(
                WgsSurokMechanic.STRONG_WARRIOR,
                WgsSurokMechanic.resolveHighestPriority(false, false, null, true, true, false)
        );

        assertEquals(
                WgsSurokMechanic.AGILE_WARRIOR,
                WgsSurokMechanic.resolveHighestPriority(false, false, null, false, true, false)
        );

        // Unconscious Surok -> Post-fight Teleorb prompt
        assertEquals(
                WgsSurokMechanic.POST_FIGHT_TELEORB,
                WgsSurokMechanic.resolveHighestPriority(false, false, null, false, false, true)
        );

        // Normal combat
        assertEquals(
                WgsSurokMechanic.NONE,
                WgsSurokMechanic.resolveHighestPriority(false, false, null, false, false, false)
        );
    }

    @Test
    public void testSurokSurgeOppositeElementAndSpellWidgets()
    {
        // Opposite element NPC ID mapping: Earth -> Air, Fire -> Water, Wind -> Earth, Water -> Fire
        assertEquals(WgsSurokMechanic.SURGE_EARTH, WgsSurokMechanic.fromSurgeNpcId(NpcID.EARTH_SURGE_RAISED));
        assertEquals(WgsSurokMechanic.SURGE_FIRE, WgsSurokMechanic.fromSurgeNpcId(NpcID.FIRE_SURGE_RAISED));
        assertEquals(WgsSurokMechanic.SURGE_WIND, WgsSurokMechanic.fromSurgeNpcId(NpcID.WIND_SURGE_RAISED));
        assertEquals(WgsSurokMechanic.SURGE_WATER, WgsSurokMechanic.fromSurgeNpcId(NpcID.WATER_SURGE_RAISED));
        assertNull(WgsSurokMechanic.fromSurgeNpcId(NpcID.DARK_SQUALL_COMBAT));

        // Level 99 Magic selects Surge spells + Enfeeble + Entangle + High Alch
        assertEquals(InterfaceID.MagicSpellbook.WIND_SURGE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_EARTH, 99));
        assertEquals(InterfaceID.MagicSpellbook.WATER_SURGE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_FIRE, 99));
        assertEquals(InterfaceID.MagicSpellbook.EARTH_SURGE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_WIND, 99));
        assertEquals(InterfaceID.MagicSpellbook.FIRE_SURGE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_WATER, 99));
        assertEquals(InterfaceID.MagicSpellbook.ENFEEBLE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.STRONG_WARRIOR, 99));
        assertEquals(InterfaceID.MagicSpellbook.ENTANGLE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.AGILE_WARRIOR, 99));
        assertEquals(InterfaceID.MagicSpellbook.TELEGRAB, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.LIT_EXPLOSIVE_GROUND, 99));
        assertEquals(InterfaceID.MagicSpellbook.HIGH_ALCHEMY, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.LIT_EXPLOSIVE_INVENTORY, 99));

        // Level 75 Magic selects Wave spells, Snare, Enfeeble, High Alch
        assertEquals(InterfaceID.MagicSpellbook.WIND_WAVE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_EARTH, 75));
        assertEquals(InterfaceID.MagicSpellbook.WATER_WAVE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_FIRE, 75));
        assertEquals(InterfaceID.MagicSpellbook.EARTH_WAVE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_WIND, 75));
        assertEquals(InterfaceID.MagicSpellbook.FIRE_WAVE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_WATER, 75));
        assertEquals(InterfaceID.MagicSpellbook.SNARE, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.AGILE_WARRIOR, 75));

        // Level 45 Magic selects Blast/Bolt, Bind, Weaken, Low Alch
        assertEquals(InterfaceID.MagicSpellbook.WIND_BLAST, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_EARTH, 45));
        assertEquals(InterfaceID.MagicSpellbook.FIRE_BOLT, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.SURGE_WATER, 45));
        assertEquals(InterfaceID.MagicSpellbook.WEAKEN, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.STRONG_WARRIOR, 45));
        assertEquals(InterfaceID.MagicSpellbook.BIND, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.AGILE_WARRIOR, 45));
        assertEquals(InterfaceID.MagicSpellbook.LOW_ALCHEMY, WgsSurokMechanic.resolvePrimarySpellWidgetId(WgsSurokMechanic.LIT_EXPLOSIVE_INVENTORY, 45));
    }

    @Test
    public void testBalanceElementalHeadColourAndPrayerMappings()
    {
        // Blue head -> Protect from Magic
        assertEquals(WgsBalanceElementalStyle.BLUE_MAGIC,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_TRANSITION_WATER01));
        assertEquals(WgsBalanceElementalStyle.BLUE_MAGIC,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_WATER_IDLE01));
        assertEquals(WgsBalanceElementalStyle.BLUE_MAGIC,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK01));
        assertEquals(WgsBalanceElementalStyle.BLUE_MAGIC,
                WgsBalanceElementalStyle.fromSpotanimOrProjectileId(SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_PROJANIM));
        assertEquals(Prayer.PROTECT_FROM_MAGIC, WgsBalanceElementalStyle.BLUE_MAGIC.getProtectionPrayer());

        // Orange head -> Protect from Missiles
        assertEquals(WgsBalanceElementalStyle.ORANGE_RANGED,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_WATER_TRANSITION_FIRE01));
        assertEquals(WgsBalanceElementalStyle.ORANGE_RANGED,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_WALK01));
        assertEquals(WgsBalanceElementalStyle.ORANGE_RANGED,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK_SPECIAL01));
        assertEquals(WgsBalanceElementalStyle.ORANGE_RANGED,
                WgsBalanceElementalStyle.fromSpotanimOrProjectileId(SpotanimID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK01_PROJANIM));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, WgsBalanceElementalStyle.ORANGE_RANGED.getProtectionPrayer());

        // White head -> Protect from Melee
        assertEquals(WgsBalanceElementalStyle.WHITE_MELEE,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_WATER_TRANSITION_AIR01));
        assertEquals(WgsBalanceElementalStyle.WHITE_MELEE,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_AIR_IDLE01));
        assertEquals(WgsBalanceElementalStyle.WHITE_MELEE,
                WgsBalanceElementalStyle.fromAnimationId(AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK01));
        assertEquals(WgsBalanceElementalStyle.WHITE_MELEE,
                WgsBalanceElementalStyle.fromSpotanimOrProjectileId(SpotanimID.VFX_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01_PROJANIM));
        assertEquals(Prayer.PROTECT_FROM_MELEE, WgsBalanceElementalStyle.WHITE_MELEE.getProtectionPrayer());

        assertTrue(WgsBalanceElementalStyle.isAttackAnimation(AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK01));
        assertFalse(WgsBalanceElementalStyle.isAttackAnimation(AnimationID.NPC_BALANCE_ELEMENTAL_AIR_IDLE01));
    }

    @Test
    public void testBalanceElementalHpPhasesAndOverheadExtraction()
    {
        assertEquals(WgsBalanceElementalStyle.HpPhase.ABOVE_550_HP, WgsBalanceElementalStyle.resolveHpPhase(750));
        assertEquals(WgsBalanceElementalStyle.HpPhase.ABOVE_550_HP, WgsBalanceElementalStyle.resolveHpPhase(551));
        assertEquals(WgsBalanceElementalStyle.HpPhase.BELOW_550_HP, WgsBalanceElementalStyle.resolveHpPhase(550));
        assertEquals(WgsBalanceElementalStyle.HpPhase.BELOW_50_PERCENT, WgsBalanceElementalStyle.resolveHpPhase(375));
        assertEquals(WgsBalanceElementalStyle.HpPhase.BELOW_50_PERCENT, WgsBalanceElementalStyle.resolveHpPhase(201));
        assertEquals(WgsBalanceElementalStyle.HpPhase.BELOW_200_HP, WgsBalanceElementalStyle.resolveHpPhase(200));

        // Combined HeadIcon decoding (e.g. RANGE_MAGE -> Crush left open)
        Set<HeadIcon> rangeMage = WgsOverlayUtils.extractProtectedStyles(
                new short[]{(short) HeadIcon.RANGE_MAGE.ordinal(), -1}
        );
        assertEquals(EnumSet.of(HeadIcon.RANGED, HeadIcon.MAGIC), rangeMage);
        assertEquals("HIT WITH: CRUSH", WgsOverlayUtils.recommendPlayerAttackStyle(rangeMage, WgsBossStage.BALANCE_ELEMENTAL));

        // Multi-element array decoding (MELEE + MAGIC -> Heavy Ranged left open)
        Set<HeadIcon> meleeMagic = WgsOverlayUtils.extractProtectedStyles(
                new short[]{(short) HeadIcon.MELEE.ordinal(), (short) HeadIcon.MAGIC.ordinal()}
        );
        assertEquals(EnumSet.of(HeadIcon.MELEE, HeadIcon.MAGIC), meleeMagic);
        assertEquals("HIT WITH: HEAVY RANGED", WgsOverlayUtils.recommendPlayerAttackStyle(meleeMagic, WgsBossStage.BALANCE_ELEMENTAL));

        // Single protection prayer (MELEE -> Heavy Ranged / Magic left open)
        Set<HeadIcon> singleMelee = WgsOverlayUtils.extractProtectedStyles(
                new short[]{(short) HeadIcon.MELEE.ordinal()}
        );
        assertEquals("HIT WITH: HEAVY RANGED / MAGIC", WgsOverlayUtils.recommendPlayerAttackStyle(singleMelee, WgsBossStage.BALANCE_ELEMENTAL));
    }

    @Test
    public void testTormentedDemonMappingsAndPrayerHelpers()
    {
        assertEquals(Prayer.PROTECT_FROM_MAGIC,
                WgsPlugin.resolveTormentedDemonPrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_FIREY_BALLS));
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                WgsPlugin.resolveTormentedDemonPrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_SPARE_RIBS));
        assertEquals(Prayer.PROTECT_FROM_MELEE,
                WgsPlugin.resolveTormentedDemonPrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_MELEE));
        assertNull(WgsPlugin.resolveTormentedDemonPrayerFromAnimation(AnimationID.LUC2_UNDEAD_DEMON_EXPLOSION_FIRE));

        assertEquals(Prayer.PROTECT_FROM_MAGIC,
                WgsPlugin.resolveTormentedDemonPrayerFromProjectile(SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_PROJ));
        assertEquals(Prayer.PROTECT_FROM_MISSILES,
                WgsPlugin.resolveTormentedDemonPrayerFromProjectile(SpotanimID.LUC2_RIB_BONE_SHARD_PROJECTILE));

        assertEquals(Rs2PrayerEnum.PROTECT_MAGIC, WgsPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Rs2PrayerEnum.PROTECT_RANGE, WgsPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Rs2PrayerEnum.PROTECT_MELEE, WgsPlugin.toRs2PrayerEnum(Prayer.PROTECT_FROM_MELEE));

        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MAGIC, WgsPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MISSILES, WgsPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(SpriteID.PRAYER_PROTECT_FROM_MELEE, WgsPrayerHelperOverlay.prayerToSpriteId(Prayer.PROTECT_FROM_MELEE));

        assertEquals(InterfaceID.Prayerbook.PRAYER13, WgsWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(InterfaceID.Prayerbook.PRAYER14, WgsWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(InterfaceID.Prayerbook.PRAYER15, WgsWidgetOverlay.prayerToComponentId(Prayer.PROTECT_FROM_MELEE));

        assertEquals(WgsConfig.OffensivePrayerStyle.MAGIC,
                WgsPlugin.resolveAutoOffensiveStyle(WgsConfig.OffensivePrayerStyle.AUTO, WgsBossStage.SUROK_MAGIS, "Dragon crossbow", EnumSet.noneOf(HeadIcon.class)));
        assertEquals(WgsConfig.OffensivePrayerStyle.RANGED,
                WgsPlugin.resolveAutoOffensiveStyle(WgsConfig.OffensivePrayerStyle.AUTO, WgsBossStage.BALANCE_ELEMENTAL, "Scorching bow", EnumSet.of(HeadIcon.MELEE)));
        assertEquals(WgsConfig.OffensivePrayerStyle.MELEE,
                WgsPlugin.resolveAutoOffensiveStyle(WgsConfig.OffensivePrayerStyle.AUTO, WgsBossStage.TORMENTED_DEMONS, "Emberlight", EnumSet.of(HeadIcon.RANGED)));
    }

    @Test
    public void testSupplyRequirementsForAllThreeBossStages()
    {
        WgsConfig defaultConfig = new WgsConfig() {};

        // 1. Surok Magis: Dark Squall robes (non-tradeable), Teleorb, all 4 elemental runes + Blood + Body + Nature + Law
        List<WgsSupplyCalculator.SupplyRequirement> surokReqs = WgsSupplyCalculator.buildRequirementsForStage(
                defaultConfig,
                WgsBossStage.SUROK_MAGIS
        );
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.WGS_DARK_SQUALL_HOOD && !r.isTradeable()));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.WGS_DARK_SQUALL_ROBE_TOP && !r.isTradeable()));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.WGS_DARK_SQUALL_ROBE_BOTTOM && !r.isTradeable()));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.WGS_LUCIEN_TELEORB && !r.isTradeable()));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.AIRRUNE && r.getSingleTripQuantity() == 300));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.WATERRUNE && r.getSingleTripQuantity() == 300));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.EARTHRUNE && r.getSingleTripQuantity() == 300));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.FIRERUNE && r.getSingleTripQuantity() == 300));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.BLOODRUNE && r.getSingleTripQuantity() == 150));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.BODYRUNE));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.NATURERUNE));
        assertTrue(surokReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.LAWRUNE));

        // 2. Balance Elemental: enforces at least 4 Super Restores even if user configured Prayer Potions / count < 4
        WgsConfig lowRestoreConfig = new WgsConfig()
        {
            @Override
            public RestorePotionType restorePotionType()
            {
                return RestorePotionType.PRAYER_POTION;
            }

            @Override
            public int restorePotionCount()
            {
                return 2;
            }
        };
        List<WgsSupplyCalculator.SupplyRequirement> balanceReqs = WgsSupplyCalculator.buildRequirementsForStage(
                lowRestoreConfig,
                WgsBossStage.BALANCE_ELEMENTAL
        );
        assertTrue(balanceReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID._4DOSE2RESTORE && r.getSingleTripQuantity() == 4));
        assertTrue(balanceReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertTrue(balanceReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.TBWT_COOKED_KARAMBWAN));

        // 3. Tormented Demons: includes food, karambwans, restores, teleport
        List<WgsSupplyCalculator.SupplyRequirement> tdReqs = WgsSupplyCalculator.buildRequirementsForStage(
                defaultConfig,
                WgsBossStage.TORMENTED_DEMONS
        );
        assertTrue(tdReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.MANTARAY && r.getSingleTripQuantity() == 12));
        assertTrue(tdReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID.TBWT_COOKED_KARAMBWAN && r.getSingleTripQuantity() == 4));
        assertTrue(tdReqs.stream().anyMatch(r -> r.getPrimaryItemId() == ItemID._4DOSE2RESTORE && r.getSingleTripQuantity() == 4));
    }
}
