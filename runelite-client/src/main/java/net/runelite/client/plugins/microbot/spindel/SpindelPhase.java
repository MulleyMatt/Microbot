package net.runelite.client.plugins.microbot.spindel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;

import java.awt.Color;

/**
 * Represents Spindel's 16-attack rotation phases:
 * 8 Ranged attacks in a row followed by 8 Magic attacks in a row.
 */
@Getter
@RequiredArgsConstructor
public enum SpindelPhase
{
    RANGED(
            "Ranged Phase",
            Prayer.PROTECT_FROM_MISSILES,
            Skill.RANGED,
            new Color(67, 196, 87),
            "Pray Missiles (Ranged)"
    ),
    MAGIC(
            "Magic Phase",
            Prayer.PROTECT_FROM_MAGIC,
            Skill.MAGIC,
            new Color(45, 152, 240),
            "Pray Magic"
    );

    public static final int ATTACKS_PER_PHASE = 8;
    public static final int TOTAL_CYCLE_ATTACKS = 16;

    private final String displayName;
    private final Prayer defaultPrayer;
    private final Skill skillIcon;
    private final Color color;
    private final String prayerPrompt;

    public SpindelPhase getNextPhase()
    {
        return this == RANGED ? MAGIC : RANGED;
    }

    public Color getColorWithAlpha(int alpha)
    {
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }
}
