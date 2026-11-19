package net.runelite.client.plugins.microbot.wgs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;

import java.awt.Color;

/**
 * Special mechanics and counter-spells for Boss 1: Surok Magis.
 */
@Getter
@RequiredArgsConstructor
public enum WgsSurokMechanic
{
    NONE(
            "Standard Magic DPS",
            "STANDARD MAGIC (Keep Protect Magic)",
            "",
            new Color(0, 210, 255),
            0
    ),
    STRONG_WARRIOR(
            "Strong Warrior",
            "CAST WEAKEN / ENFEEBLE!",
            "WEAKEN",
            new Color(255, 165, 0),
            90
    ),
    AGILE_WARRIOR(
            "Agile Warrior",
            "CAST BIND / SNARE / ENTANGLE!",
            "BIND",
            new Color(50, 230, 110),
            85
    ),
    LIT_EXPLOSIVE_GROUND(
            "Lit Explosive (Ground)",
            "TELEGRAB LIT EXPLOSIVE!",
            "TELEGRAB",
            new Color(255, 60, 60),
            95
    ),
    LIT_EXPLOSIVE_INVENTORY(
            "Lit Explosive (Inventory)",
            "ALCH LIT EXPLOSIVE NOW!",
            "ALCH",
            new Color(255, 30, 30),
            100
    ),
    SURGE_EARTH(
            "Earth Surge Orb",
            "EARTH SURGE -> CAST AIR SPELL!",
            "AIR",
            new Color(220, 240, 255),
            92
    ),
    SURGE_FIRE(
            "Fire Surge Orb",
            "FIRE SURGE -> CAST WATER SPELL!",
            "WATER",
            new Color(0, 185, 255),
            92
    ),
    SURGE_WIND(
            "Wind Surge Orb",
            "WIND SURGE -> CAST EARTH SPELL!",
            "EARTH",
            new Color(165, 115, 60),
            92
    ),
    SURGE_WATER(
            "Water Surge Orb",
            "WATER SURGE -> CAST FIRE SPELL!",
            "FIRE",
            new Color(255, 95, 30),
            92
    ),
    POST_FIGHT_TELEORB(
            "Post-Fight Teleorb",
            "USE TELEORB ON SUROK!",
            "TELEORB",
            new Color(200, 120, 255),
            80
    );

    private final String displayName;
    private final String actionPrompt;
    private final String widgetLabel;
    private final Color color;
    private final int priority;

    /**
     * Resolves the elemental surge counter mechanic from a raised surge orb NPC ID.
     */
    public static WgsSurokMechanic fromSurgeNpcId(int npcId)
    {
        switch (npcId)
        {
            case NpcID.EARTH_SURGE_RAISED:
                return SURGE_EARTH;
            case NpcID.FIRE_SURGE_RAISED:
                return SURGE_FIRE;
            case NpcID.WIND_SURGE_RAISED:
                return SURGE_WIND;
            case NpcID.WATER_SURGE_RAISED:
                return SURGE_WATER;
            default:
                return null;
        }
    }

    /**
     * Resolves the highest priority active Surok mechanic across current encounter signals.
     */
    public static WgsSurokMechanic resolveHighestPriority(
            boolean bombInInventory,
            boolean bombOnGround,
            WgsSurokMechanic nearestSurgeOrb,
            boolean strongWarriorActive,
            boolean agileWarriorActive,
            boolean surokUnconscious)
    {
        if (bombInInventory)
        {
            return LIT_EXPLOSIVE_INVENTORY;
        }
        if (bombOnGround)
        {
            return LIT_EXPLOSIVE_GROUND;
        }
        if (nearestSurgeOrb != null && nearestSurgeOrb != NONE)
        {
            return nearestSurgeOrb;
        }
        if (strongWarriorActive)
        {
            return STRONG_WARRIOR;
        }
        if (agileWarriorActive)
        {
            return AGILE_WARRIOR;
        }
        if (surokUnconscious)
        {
            return POST_FIGHT_TELEORB;
        }
        return NONE;
    }

    /**
     * Returns the primary Standard Spellbook widget component ID for the mechanic and current Magic level.
     * For elemental surge counters, selects the highest unlocked combat spell of the opposite element.
     */
    public static int resolvePrimarySpellWidgetId(WgsSurokMechanic mechanic, int magicLevel)
    {
        if (mechanic == null)
        {
            return -1;
        }
        switch (mechanic)
        {
            case STRONG_WARRIOR:
                return magicLevel >= 73 ? InterfaceID.MagicSpellbook.ENFEEBLE : InterfaceID.MagicSpellbook.WEAKEN;
            case AGILE_WARRIOR:
                if (magicLevel >= 79)
                {
                    return InterfaceID.MagicSpellbook.ENTANGLE;
                }
                if (magicLevel >= 50)
                {
                    return InterfaceID.MagicSpellbook.SNARE;
                }
                return InterfaceID.MagicSpellbook.BIND;
            case LIT_EXPLOSIVE_GROUND:
                return InterfaceID.MagicSpellbook.TELEGRAB;
            case LIT_EXPLOSIVE_INVENTORY:
                return magicLevel >= 55 ? InterfaceID.MagicSpellbook.HIGH_ALCHEMY : InterfaceID.MagicSpellbook.LOW_ALCHEMY;
            case SURGE_EARTH:
                return resolveHighestAirSpell(magicLevel);
            case SURGE_FIRE:
                return resolveHighestWaterSpell(magicLevel);
            case SURGE_WIND:
                return resolveHighestEarthSpell(magicLevel);
            case SURGE_WATER:
                return resolveHighestFireSpell(magicLevel);
            default:
                return -1;
        }
    }

    /**
     * Returns all relevant Standard Spellbook widget component IDs to highlight for the mechanic.
     */
    public static int[] resolveAllSpellWidgetIds(WgsSurokMechanic mechanic, int magicLevel)
    {
        if (mechanic == null)
        {
            return new int[0];
        }
        switch (mechanic)
        {
            case STRONG_WARRIOR:
                return magicLevel >= 73
                        ? new int[]{InterfaceID.MagicSpellbook.WEAKEN, InterfaceID.MagicSpellbook.ENFEEBLE}
                        : new int[]{InterfaceID.MagicSpellbook.WEAKEN};
            case AGILE_WARRIOR:
                if (magicLevel >= 79)
                {
                    return new int[]{
                            InterfaceID.MagicSpellbook.ENTANGLE,
                            InterfaceID.MagicSpellbook.SNARE,
                            InterfaceID.MagicSpellbook.BIND
                    };
                }
                if (magicLevel >= 50)
                {
                    return new int[]{InterfaceID.MagicSpellbook.SNARE, InterfaceID.MagicSpellbook.BIND};
                }
                return new int[]{InterfaceID.MagicSpellbook.BIND};
            case LIT_EXPLOSIVE_GROUND:
                return new int[]{InterfaceID.MagicSpellbook.TELEGRAB};
            case LIT_EXPLOSIVE_INVENTORY:
                return magicLevel >= 55
                        ? new int[]{InterfaceID.MagicSpellbook.HIGH_ALCHEMY, InterfaceID.MagicSpellbook.LOW_ALCHEMY}
                        : new int[]{InterfaceID.MagicSpellbook.LOW_ALCHEMY};
            case SURGE_EARTH:
            case SURGE_FIRE:
            case SURGE_WIND:
            case SURGE_WATER:
                int id = resolvePrimarySpellWidgetId(mechanic, magicLevel);
                return id != -1 ? new int[]{id} : new int[0];
            default:
                return new int[0];
        }
    }

    private static int resolveHighestAirSpell(int magicLevel)
    {
        if (magicLevel >= 81)
        {
            return InterfaceID.MagicSpellbook.WIND_SURGE;
        }
        if (magicLevel >= 62)
        {
            return InterfaceID.MagicSpellbook.WIND_WAVE;
        }
        if (magicLevel >= 41)
        {
            return InterfaceID.MagicSpellbook.WIND_BLAST;
        }
        if (magicLevel >= 17)
        {
            return InterfaceID.MagicSpellbook.WIND_BOLT;
        }
        return InterfaceID.MagicSpellbook.WIND_STRIKE;
    }

    private static int resolveHighestWaterSpell(int magicLevel)
    {
        if (magicLevel >= 85)
        {
            return InterfaceID.MagicSpellbook.WATER_SURGE;
        }
        if (magicLevel >= 65)
        {
            return InterfaceID.MagicSpellbook.WATER_WAVE;
        }
        if (magicLevel >= 47)
        {
            return InterfaceID.MagicSpellbook.WATER_BLAST;
        }
        if (magicLevel >= 23)
        {
            return InterfaceID.MagicSpellbook.WATER_BOLT;
        }
        return InterfaceID.MagicSpellbook.WATER_STRIKE;
    }

    private static int resolveHighestEarthSpell(int magicLevel)
    {
        if (magicLevel >= 90)
        {
            return InterfaceID.MagicSpellbook.EARTH_SURGE;
        }
        if (magicLevel >= 70)
        {
            return InterfaceID.MagicSpellbook.EARTH_WAVE;
        }
        if (magicLevel >= 53)
        {
            return InterfaceID.MagicSpellbook.EARTH_BLAST;
        }
        if (magicLevel >= 29)
        {
            return InterfaceID.MagicSpellbook.EARTH_BOLT;
        }
        return InterfaceID.MagicSpellbook.EARTH_STRIKE;
    }

    private static int resolveHighestFireSpell(int magicLevel)
    {
        if (magicLevel >= 95)
        {
            return InterfaceID.MagicSpellbook.FIRE_SURGE;
        }
        if (magicLevel >= 75)
        {
            return InterfaceID.MagicSpellbook.FIRE_WAVE;
        }
        if (magicLevel >= 59)
        {
            return InterfaceID.MagicSpellbook.FIRE_BLAST;
        }
        if (magicLevel >= 35)
        {
            return InterfaceID.MagicSpellbook.FIRE_BOLT;
        }
        return InterfaceID.MagicSpellbook.FIRE_STRIKE;
    }
}
