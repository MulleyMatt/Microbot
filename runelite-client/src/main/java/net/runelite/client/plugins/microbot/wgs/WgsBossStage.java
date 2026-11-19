package net.runelite.client.plugins.microbot.wgs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the three boss encounters in While Guthix Sleeps.
 * Used both for live encounter detection and Bank / Grand Exchange supply filtering.
 */
@Getter
@RequiredArgsConstructor
public enum WgsBossStage
{
    SUROK_MAGIS("Boss 1: Surok Magis", "Surok Magis (Lvl 265, 450 HP)"),
    BALANCE_ELEMENTAL("Boss 2: Balance Elemental", "Balance Elemental (Lvl 524, 750 HP)"),
    TORMENTED_DEMONS("Boss 3: Tormented Demons", "Two Tormented Demons (Lvl 450)");

    private final String shortName;
    private final String description;

    @Override
    public String toString()
    {
        return shortName;
    }
}
