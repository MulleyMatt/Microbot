package net.runelite.client.plugins.microbot.muspah;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.AnimationID;

/**
 * Special and standard attack animations performed by Phantom Muspah.
 */
@Getter
@RequiredArgsConstructor
public enum MuspahSpecialAttack
{
    CHARGED_MAGIC(
            AnimationID.NPC_MUSPAH_ATTACK_MAGIC_01,
            "PRAY MAGIC! (Corruption)",
            Prayer.PROTECT_FROM_MAGIC,
            4
    ),
    SPIKE_SLAM(
            AnimationID.NPC_MUSPAH_ATTACK_SUMMON_01,
            "SPIKES SUMMONED - MOVE!",
            null,
            4
    ),
    CENTER_SHOCKWAVE(
            AnimationID.NPC_MUSPAH_ATTACK_EXPLOSION_01,
            "SHOCKWAVE! HIDE BEHIND SPIKE!",
            null,
            6
    ),
    TELEPORT_ORBS(
            AnimationID.NPC_MUSPAH_TELEPORT_ATTACK_01,
            "SHADOW ORBS - DODGE!",
            null,
            6
    );

    public static final int ANIM_MELEE_ATTACK = AnimationID.NPC_MUSPAH_ATTACK_MELEE_01;
    public static final int ANIM_RANGED_ATTACK = AnimationID.NPC_MUSPAH_ATTACK_RANGED_01;
    public static final int ANIM_MAGIC_ATTACK = AnimationID.NPC_MUSPAH_ATTACK_MAGIC_01;
    public static final int ANIM_SPIKE_SLAM = AnimationID.NPC_MUSPAH_ATTACK_SUMMON_01;
    public static final int ANIM_SHOCKWAVE = AnimationID.NPC_MUSPAH_ATTACK_EXPLOSION_01;
    public static final int ANIM_TELEPORT_DISAPPEAR = AnimationID.NPC_MUSPAH_TELEPORT_DISAPPEAR_01;
    public static final int ANIM_TELEPORT_ATTACK = AnimationID.NPC_MUSPAH_TELEPORT_ATTACK_01;
    public static final int ANIM_TRANSFORM_DISAPPEAR_1 = AnimationID.NPC_MUSPAH_TRANSFORM_DISAPPEAR_01;
    public static final int ANIM_TRANSFORM_DISAPPEAR_2 = AnimationID.NPC_MUSPAH_TRANSFORM_DISAPPEAR_02;
    public static final int ANIM_DEATH_1 = AnimationID.NPC_MUSPAH_DEATH_01;
    public static final int ANIM_DEATH_2 = AnimationID.NPC_MUSPAH_DEATH_02;

    private final int animationId;
    private final String alertText;
    private final Prayer overridePrayer;
    private final int durationTicks;

    public static MuspahSpecialAttack fromAnimationId(int animationId)
    {
        for (MuspahSpecialAttack special : values())
        {
            if (special.animationId == animationId)
            {
                return special;
            }
        }
        return null;
    }

    public static boolean isAttackAnimation(int animationId)
    {
        return animationId == ANIM_MELEE_ATTACK
                || animationId == ANIM_RANGED_ATTACK
                || animationId == ANIM_MAGIC_ATTACK
                || animationId == ANIM_SPIKE_SLAM
                || animationId == ANIM_SHOCKWAVE
                || animationId == ANIM_TELEPORT_ATTACK;
    }
}
