package net.runelite.client.plugins.microbot.wgs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;

import java.awt.Color;

/**
 * Head-colour stances, attack style mappings, and HP phase thresholds for Boss 2: Balance Elemental (750 HP).
 * Ignore attack animations visually (melee looks like a projectile) and read the head colour / transition instead:
 * - Blue head -> Protect from Magic
 * - Orange head -> Protect from Missiles
 * - White head -> Protect from Melee
 */
@Getter
@RequiredArgsConstructor
public enum WgsBalanceElementalStyle
{
    BLUE_MAGIC(
            "Blue Head (Magic)",
            "PRAY MAGIC",
            Prayer.PROTECT_FROM_MAGIC,
            new Color(0, 190, 255)
    ),
    ORANGE_RANGED(
            "Orange Head (Missiles)",
            "PRAY MISSILES",
            Prayer.PROTECT_FROM_MISSILES,
            new Color(255, 145, 0)
    ),
    WHITE_MELEE(
            "White Head (Melee)",
            "PRAY MELEE",
            Prayer.PROTECT_FROM_MELEE,
            Color.WHITE
    );

    public static final int MAX_HP = 750;
    public static final int ATTACKS_PER_ROTATION = 4;

    private final String displayName;
    private final String prayerPrompt;
    private final Prayer protectionPrayer;
    private final Color color;

    /**
     * HP phase brackets for Balance Elemental (750 HP).
     */
    @Getter
    @RequiredArgsConstructor
    public enum HpPhase
    {
        ABOVE_550_HP("> 550 HP", "Base speed, 0 overheads", new Color(180, 235, 255), false),
        BELOW_550_HP("376-550 HP", "Faster speed + 1 overhead", new Color(255, 220, 80), false),
        BELOW_50_PERCENT("201-375 HP (<50%)", "1-TICK WINDOW! Keep Pray > 50%", new Color(255, 130, 30), true),
        BELOW_200_HP("<= 200 HP", "Fastest + 2 OVERHEADS (Hit unprayed!)", new Color(255, 55, 55), true);

        private final String label;
        private final String mechanicNote;
        private final Color color;
        private final boolean critical;
    }

    /**
     * Determines the Balance Elemental HP bracket from estimated current HP.
     */
    public static HpPhase resolveHpPhase(int estimatedHp)
    {
        if (estimatedHp <= 200)
        {
            return HpPhase.BELOW_200_HP;
        }
        if (estimatedHp <= MAX_HP / 2)
        {
            return HpPhase.BELOW_50_PERCENT;
        }
        if (estimatedHp <= 550)
        {
            return HpPhase.BELOW_550_HP;
        }
        return HpPhase.ABOVE_550_HP;
    }

    /**
     * Resolves head colour / attack style from Balance Elemental animation IDs (transitions, idle, walk, attacks).
     */
    public static WgsBalanceElementalStyle fromAnimationId(int animationId)
    {
        switch (animationId)
        {
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_TRANSITION_WATER01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_TRANSITION_WATER01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_IDLE01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_WALK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK_SPECIAL01:
            case AnimationID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_SPOTANIM:
            case AnimationID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_PROJANIM:
                return BLUE_MAGIC;

            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_TRANSITION_FIRE01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_TRANSITION_FIRE01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_IDLE01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_WALK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK_SPECIAL01:
            case AnimationID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK01_PROJANIM:
                return ORANGE_RANGED;

            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_TRANSITION_AIR01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_TRANSITION_AIR01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_IDLE01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_WALK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01:
                return WHITE_MELEE;

            default:
                return null;
        }
    }

    /**
     * Returns true if the animation ID represents a Balance Elemental attack (standard or special).
     */
    public static boolean isAttackAnimation(int animationId)
    {
        switch (animationId)
        {
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK_SPECIAL01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK_SPECIAL01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK01:
            case AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01:
                return true;
            default:
                return false;
        }
    }

    /**
     * Resolves head colour / attack style from Balance Elemental projectile or spotanim IDs.
     */
    public static WgsBalanceElementalStyle fromSpotanimOrProjectileId(int id)
    {
        switch (id)
        {
            case SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_SPOTANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_PROJANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK_SPECIAL01_SPOTANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK_SPECIAL01_PROJANIM:
                return BLUE_MAGIC;

            case SpotanimID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK01_PROJANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK_SPECIAL01_SPOTANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK_SPECIAL01_PROJANIM:
                return ORANGE_RANGED;

            case SpotanimID.VFX_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01_SPOTANIM:
            case SpotanimID.VFX_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01_PROJANIM:
                return WHITE_MELEE;

            default:
                return null;
        }
    }
}
