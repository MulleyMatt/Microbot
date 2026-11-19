package net.runelite.client.plugins.microbot.tormenteddemons;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-Tormented-Demon combat state tracking overhead swaps (every 150 damage),
 * shield-down punish windows (1 hit on fight start or after fire bombs),
 * 100% accuracy defenceless windows (~30 ticks after engage/post-fire-bomb hit),
 * and post-fire-bomb movement style prediction.
 */
@Getter
public class TormentedDemonState
{
    public static final int MAX_HP = 600;
    public static final int PRAYER_SWAP_DAMAGE_THRESHOLD = 150;
    public static final int DEFENCELESS_TICKS = 30;
    public static final int FIRE_BOMB_INTERVAL_TICKS = 60;
    public static final int SHIELD_DOWN_SAFETY_TIMEOUT_TICKS = 15;
    public static final int PRAYER_SWAP_PAUSE_TICKS = 6;
    public static final int POST_FIRE_BOMB_PREDICTION_WINDOW_TICKS = 8;

    private final NPC npc;

    private Prayer requiredPrayer;
    private boolean prayerPredicted;
    private Prayer lastConfirmedAttackStyle;
    private Prayer styleBeforeFireBomb;
    private boolean awaitingPostFireBombStyle;
    private WorldPoint postFireBombAnchorLocation;
    private int postFireBombTicks;

    private int ticksSinceFireBomb;
    private int attacksSinceFireBomb;

    private Set<HeadIcon> protectedStyles = EnumSet.noneOf(HeadIcon.class);
    private int damageSincePrayerSwap;
    private int postPrayerSwapPauseTicks;

    private boolean shieldDown;
    private int shieldDownStartTick = -1;
    private int shieldDownTimeoutTicks;

    private boolean defenceless;
    private boolean awaitingFirstHitAfterFireBomb;
    private int combatTicksSinceReset;

    private boolean inCombatWithPlayer;
    private int lastAttackTick = -10;
    @Setter
    private int lastHealthRatio = -1;
    @Setter
    private int estimatedHp = MAX_HP;

    public TormentedDemonState(NPC npc)
    {
        this.npc = npc;
    }

    public void recordDamage(int amount)
    {
        if (amount <= 0)
        {
            return;
        }
        damageSincePrayerSwap = Math.min(MAX_HP, damageSincePrayerSwap + amount);
    }

    public boolean onProtectedStylesChanged(Set<HeadIcon> nextStyles)
    {
        Set<HeadIcon> safeNext = nextStyles == null || nextStyles.isEmpty()
                ? EnumSet.noneOf(HeadIcon.class)
                : EnumSet.copyOf(nextStyles);

        if (safeNext.equals(protectedStyles))
        {
            return false;
        }

        boolean hadDamageOrPrevious = damageSincePrayerSwap > 0 || !protectedStyles.isEmpty();
        protectedStyles = safeNext;
        damageSincePrayerSwap = 0;
        if (hadDamageOrPrevious && !safeNext.isEmpty())
        {
            postPrayerSwapPauseTicks = PRAYER_SWAP_PAUSE_TICKS;
        }
        return true;
    }

    public void onFireBombStarted(int currentTick, WorldPoint anchorLocation)
    {
        inCombatWithPlayer = true;
        styleBeforeFireBomb = lastConfirmedAttackStyle != null ? lastConfirmedAttackStyle : requiredPrayer;
        awaitingPostFireBombStyle = true;
        postFireBombAnchorLocation = anchorLocation;
        postFireBombTicks = 0;
        ticksSinceFireBomb = 0;
        attacksSinceFireBomb = 0;

        shieldDown = true;
        shieldDownStartTick = currentTick;
        shieldDownTimeoutTicks = SHIELD_DOWN_SAFETY_TIMEOUT_TICKS;
        awaitingFirstHitAfterFireBomb = true;
    }

    public void onFirstHitEngaged(int currentTick)
    {
        if (inCombatWithPlayer)
        {
            return;
        }
        inCombatWithPlayer = true;
        combatTicksSinceReset = 0;
        ticksSinceFireBomb = 0;
        attacksSinceFireBomb = 0;

        shieldDown = true;
        shieldDownStartTick = currentTick;
        shieldDownTimeoutTicks = SHIELD_DOWN_SAFETY_TIMEOUT_TICKS;
    }

    public void onPlayerHitConsumedShield(int currentTick)
    {
        if (currentTick <= shieldDownStartTick)
        {
            return;
        }
        if (shieldDown)
        {
            shieldDown = false;
            shieldDownTimeoutTicks = 0;
        }
        if (awaitingFirstHitAfterFireBomb)
        {
            awaitingFirstHitAfterFireBomb = false;
            defenceless = false;
            combatTicksSinceReset = 0;
        }
    }

    public void onShieldRestored()
    {
        shieldDown = false;
        shieldDownTimeoutTicks = 0;
    }

    public void onShieldDownSpotanim(int currentTick)
    {
        if (!shieldDown)
        {
            shieldDown = true;
            shieldDownStartTick = currentTick;
            shieldDownTimeoutTicks = SHIELD_DOWN_SAFETY_TIMEOUT_TICKS;
        }
    }

    public void onDefencelessDetected()
    {
        defenceless = true;
        if (combatTicksSinceReset < DEFENCELESS_TICKS)
        {
            combatTicksSinceReset = DEFENCELESS_TICKS;
        }
    }

    public void onConfirmedAttack(Prayer style, int currentTick)
    {
        if (style == null)
        {
            return;
        }
        inCombatWithPlayer = true;
        requiredPrayer = style;
        prayerPredicted = false;
        lastConfirmedAttackStyle = style;
        awaitingPostFireBombStyle = false;
        postFireBombTicks = 0;
        postPrayerSwapPauseTicks = 0;

        if (currentTick != lastAttackTick)
        {
            lastAttackTick = currentTick;
            attacksSinceFireBomb++;
        }
    }

    public void setPredictedPrayer(Prayer predicted)
    {
        if (predicted == null)
        {
            return;
        }
        requiredPrayer = predicted;
        prayerPredicted = true;
    }

    public void onTick()
    {
        if (inCombatWithPlayer)
        {
            ticksSinceFireBomb++;
            combatTicksSinceReset++;
            if (!defenceless && combatTicksSinceReset >= DEFENCELESS_TICKS)
            {
                defenceless = true;
            }
        }

        if (awaitingPostFireBombStyle)
        {
            postFireBombTicks++;
            if (postFireBombTicks > POST_FIRE_BOMB_PREDICTION_WINDOW_TICKS)
            {
                awaitingPostFireBombStyle = false;
                postFireBombTicks = 0;
            }
        }

        if (postPrayerSwapPauseTicks > 0)
        {
            postPrayerSwapPauseTicks--;
        }

        if (shieldDown && shieldDownTimeoutTicks > 0)
        {
            shieldDownTimeoutTicks--;
            if (shieldDownTimeoutTicks == 0)
            {
                shieldDown = false;
            }
        }
    }

    public int getTicksUntilFireBomb()
    {
        return Math.max(0, FIRE_BOMB_INTERVAL_TICKS - ticksSinceFireBomb);
    }

    public int getTicksUntilDefenceless()
    {
        return defenceless ? 0 : Math.max(0, DEFENCELESS_TICKS - combatTicksSinceReset);
    }

    public int getDamageRemainingToPrayerSwap()
    {
        return Math.max(0, PRAYER_SWAP_DAMAGE_THRESHOLD - damageSincePrayerSwap);
    }
}
