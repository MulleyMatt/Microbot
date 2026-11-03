package net.runelite.client.plugins.microbot.redwood;

public enum FlashStyle {
    PULSE("Pulse"),
    FLASH("Flash"),
    SOLID("Solid");

    private final String name;

    FlashStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
