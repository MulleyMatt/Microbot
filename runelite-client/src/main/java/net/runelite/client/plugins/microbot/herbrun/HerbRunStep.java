package net.runelite.client.plugins.microbot.herbrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stages in an interactive herb run.
 */
@Getter
@RequiredArgsConstructor
public enum HerbRunStep {
    CHECKING_SUPPLIES("Checking Supplies"),
    MISSING_SUPPLIES("Missing Supplies"),
    TELEPORTING("Teleporting"),
    HARVESTING("Harvest Herbs"),
    CLEARING("Clear Dead Patch"),
    WEEDING("Rake Weeds"),
    COMPOSTING("Apply Compost"),
    PLANTING("Plant Seed"),
    NOTING_HERBS("Note Herbs on Leprechaun"),
    FINISHED("Herb Run Complete");

    private final String description;

    @Override
    public String toString() {
        return description;
    }
}
