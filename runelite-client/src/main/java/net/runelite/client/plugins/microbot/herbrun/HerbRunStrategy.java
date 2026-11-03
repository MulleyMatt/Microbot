package net.runelite.client.plugins.microbot.herbrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Strategy definitions based on player farming preferences.
 */
@Getter
@RequiredArgsConstructor
public enum HerbRunStrategy {
    PURE_RANARR("Strategy A: Pure Ranarr", "Plant Ranarr seeds across all 9 patches for maximum daily GP profit."),
    SPLIT_METHOD("Strategy B: Pro Split Method", "Plant Ranarr in 4 disease-free patches & Toadflax in 5 standard patches to eliminate dead-patch losses.");

    private final String name;
    private final String description;

    @Override
    public String toString() {
        return name;
    }
}
