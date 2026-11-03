package net.runelite.client.plugins.microbot.muspah;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;

import java.awt.Color;
import java.util.Set;

/**
 * Represents the carapace forms/phases of the Phantom Muspah boss.
 * Modeled after ZulrahType in the Zulrah Overlay Helper.
 */
@Getter
@RequiredArgsConstructor
public enum MuspahForm
{
    RANGED(
            "Green (Ranged)",
            "Bowfa",
            Prayer.PROTECT_FROM_MISSILES,
            Skill.RANGED,
            new Color(46, 204, 113),
            5,
            33
    ),
    MELEE(
            "Brown (Melee)",
            "Bowfa + Kite",
            Prayer.PROTECT_FROM_MELEE,
            Skill.RANGED,
            new Color(211, 132, 65),
            5,
            24
    ),
    PRAYER_SHIELD(
            "Teal (Shield)",
            "Sapphire Bolts + Smite",
            Prayer.PROTECT_FROM_MISSILES,
            Skill.RANGED,
            new Color(0, 210, 211),
            6,
            38
    ),
    POST_SHIELD(
            "Green (Post-Shield)",
            "Bowfa",
            Prayer.PROTECT_FROM_MISSILES,
            Skill.RANGED,
            new Color(50, 255, 126),
            5,
            38
    ),
    TELEPORTING(
            "Dark (Orbs)",
            "Dodge Orbs + Bowfa",
            Prayer.PROTECT_FROM_MISSILES,
            Skill.RANGED,
            new Color(155, 89, 182),
            5,
            14
    );

    private static final Set<Integer> RANGED_NPC_IDS = ImmutableSet.of(
            NpcID.PHANTOM_MUSPAH,
            net.runelite.api.gameval.NpcID.MUSPAH_QUEST,
            NpcID.PHANTOM_MUSPAH_15549
    );

    private static final Set<Integer> MELEE_NPC_IDS = ImmutableSet.of(
            NpcID.PHANTOM_MUSPAH_12078,
            net.runelite.api.gameval.NpcID.MUSPAH_MELEE_QUEST
    );

    private static final Set<Integer> SHIELD_NPC_IDS = ImmutableSet.of(
            NpcID.PHANTOM_MUSPAH_12079,
            net.runelite.api.gameval.NpcID.MUSPAH_SOULSPLIT_QUEST
    );

    private static final Set<Integer> POST_SHIELD_NPC_IDS = ImmutableSet.of(
            NpcID.PHANTOM_MUSPAH_12080,
            net.runelite.api.gameval.NpcID.MUSPAH_FINAL_QUEST
    );

    private static final Set<Integer> TELEPORT_NPC_IDS = ImmutableSet.of(
            NpcID.PHANTOM_MUSPAH_12082,
            net.runelite.api.gameval.NpcID.MUSPAH_TELEPORT_QUEST
    );

    private final String displayName;
    private final String weaponPrompt;
    private final Prayer defaultPrayer;
    private final Skill attackSkill;
    private final Color color;
    private final int attackSpeedTicks;
    private final int maxHit;

    public Color getColorWithAlpha(int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns the expected next form during normal pre-shield rotation, or null if in end-of-fight phases.
     */
    public MuspahForm getNextRotationForm()
    {
        switch (this)
        {
            case RANGED:
                return MELEE;
            case MELEE:
                return RANGED;
            case PRAYER_SHIELD:
                return POST_SHIELD;
            default:
                return null;
        }
    }

    public static MuspahForm fromNpcId(int npcId)
    {
        if (RANGED_NPC_IDS.contains(npcId))
        {
            return RANGED;
        }
        if (MELEE_NPC_IDS.contains(npcId))
        {
            return MELEE;
        }
        if (SHIELD_NPC_IDS.contains(npcId))
        {
            return PRAYER_SHIELD;
        }
        if (POST_SHIELD_NPC_IDS.contains(npcId))
        {
            return POST_SHIELD;
        }
        if (TELEPORT_NPC_IDS.contains(npcId))
        {
            return TELEPORTING;
        }
        return null;
    }

    public static boolean isMuspahNpc(int npcId)
    {
        return fromNpcId(npcId) != null;
    }
}
