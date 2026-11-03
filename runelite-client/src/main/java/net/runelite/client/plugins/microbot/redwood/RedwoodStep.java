package net.runelite.client.plugins.microbot.redwood;

import lombok.Getter;

public enum RedwoodStep {
    CHECKING_SUPPLIES("Check Gear", "Equip or carry a woodcutting axe"),
    CLIMBING_UP("Climb Ladder", "Climb rope ladder up to redwood canopy platform"),
    FINDING_TREE("Find Tree", "Click an available redwood tree spot to start chopping"),
    WOODCUTTING("Woodcutting", "Chopping redwood tree..."),
    IDLE_AFK("AFK / Inactive", "Character stopped woodcutting! Click an available redwood spot to resume"),
    INVENTORY_FULL("Inventory Full", "Inventory is full! Climb down ladder to bank logs"),
    CLIMBING_DOWN("Climb Down", "Climb rope ladder down to ground level"),
    BANKING("Bank Logs", "Deposit logs in the Woodcutting Guild bank chest");

    @Getter
    private final String label;

    @Getter
    private final String description;

    RedwoodStep(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String toString() {
        return label;
    }
}
