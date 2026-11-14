package net.runelite.client.plugins.microbot.barbarianfishing;

public enum BarbarianFishingFlashStyle {
    PULSE("Pulse"),
    FLASH("Flash"),
    SOLID("Solid");

    private final String name;

    BarbarianFishingFlashStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
