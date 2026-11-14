package net.runelite.client.plugins.microbot.barbarianfishing;

import lombok.Getter;

public enum BarbarianFishingStep {
    CHECKING_SUPPLIES("Check Gear", "Carry a Barbarian rod and feathers or bait"),
    FINDING_SPOT("Find Spot", "Click an available barbarian fishing spot"),
    FISHING("Fishing", "Barbarian fishing..."),
    IDLE_AFK("AFK / Inactive", "Character stopped fishing! Click a fishing spot to resume"),
    INVENTORY_FULL("Inventory Full", "Inventory is full! Left-click leaping fish to drop");

    @Getter
    private final String label;

    @Getter
    private final String description;

    BarbarianFishingStep(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String toString() {
        return label;
    }
}
