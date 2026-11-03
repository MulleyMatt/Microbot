package net.runelite.client.plugins.microbot.herbrun;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(HerbRunConfig.GROUP)
public interface HerbRunConfig extends Config {
    String GROUP = "microbotherbrun";

    @ConfigSection(
            name = "Strategy & Supplies",
            description = "Farming strategy and supply requirements",
            position = 0
    )
    String strategySection = "strategySection";

    @ConfigSection(
            name = "Grand Exchange Helper",
            description = "Quick-click helper for purchasing missing items at the Grand Exchange",
            position = 1
    )
    String geSection = "geSection";

    @ConfigSection(
            name = "Bank Helper",
            description = "Bank filtering and withdrawal assistance for herb runs",
            position = 2
    )
    String bankSection = "bankSection";

    @ConfigSection(
            name = "Patches",
            description = "Enable or disable individual herb patches",
            position = 3
    )
    String patchesSection = "patchesSection";

    @ConfigSection(
            name = "Overlays & Visuals",
            description = "Customize on-screen highlights and guidance",
            position = 4
    )
    String visualsSection = "visualsSection";

    // --- Strategy Section ---

    @ConfigItem(
            keyName = "strategy",
            name = "Herb Run Strategy",
            description = "Choose between Strategy A (Pure Ranarr) or Strategy B (Pro Split Method)",
            section = strategySection,
            position = 0
    )
    default HerbRunStrategy strategy() {
        return HerbRunStrategy.PURE_RANARR;
    }

    @ConfigItem(
            keyName = "autoDetectUnlocks",
            name = "Auto-detect Patch Unlocks",
            description = "Automatically skip patches where quest requirements or farming levels are not met",
            section = strategySection,
            position = 1
    )
    default boolean autoDetectUnlocks() {
        return true;
    }

    // --- Grand Exchange Section ---

    @Range(min = 1, max = 50)
    @ConfigItem(
            keyName = "runsToPurchase",
            name = "Runs to Purchase",
            description = "Number of runs worth of missing seeds and teleports to show in GE quick-buy options",
            section = geSection,
            position = 0
    )
    default int runsToPurchase() {
        return 1;
    }

    @ConfigItem(
            keyName = "highlightMissingInGe",
            name = "GE Quick-Click Filter",
            description = "Add a quick-click button on the Grand Exchange search bar to view and buy missing herb run supplies",
            section = geSection,
            position = 1
    )
    default boolean highlightMissingInGe() {
        return true;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
            keyName = "gePriceMargin",
            name = "Suggested Price Margin (%)",
            description = "Recommended percentage above guide price displayed in HUD for quick purchasing",
            section = geSection,
            position = 2
    )
    default int gePriceMargin() {
        return 5;
    }

    // --- Bank Helper Section ---

    @ConfigItem(
            keyName = "filterBankForMissingSupplies",
            name = "Auto-Filter Missing Supplies",
            description = "Automatically hide all bank items except missing herb run supplies when opening the bank",
            section = bankSection,
            position = 0
    )
    default boolean filterBankForMissingSupplies() {
        return true;
    }

    // --- Patches Section ---

    @ConfigItem(
            keyName = "trollStronghold",
            name = "Troll Stronghold",
            description = "Disease-free patch (requires My Arm's Big Adventure)",
            section = patchesSection,
            position = 0
    )
    default boolean trollStronghold() {
        return true;
    }

    @ConfigItem(
            keyName = "weiss",
            name = "Weiss",
            description = "Disease-free patch (requires Making Friends with My Arm)",
            section = patchesSection,
            position = 1
    )
    default boolean weiss() {
        return true;
    }

    @ConfigItem(
            keyName = "hosidius",
            name = "Hosidius",
            description = "Disease-free patch (POH House Teleport)",
            section = patchesSection,
            position = 2
    )
    default boolean hosidius() {
        return true;
    }

    @ConfigItem(
            keyName = "farmingGuild",
            name = "Farming Guild",
            description = "Disease-free patch (requires 85+ Farming)",
            section = patchesSection,
            position = 3
    )
    default boolean farmingGuild() {
        return true;
    }

    @ConfigItem(
            keyName = "catherby",
            name = "Catherby",
            description = "Standard patch near Catherby bank",
            section = patchesSection,
            position = 4
    )
    default boolean catherby() {
        return true;
    }

    @ConfigItem(
            keyName = "ardougne",
            name = "Ardougne",
            description = "Standard patch north of Ardougne (Ardy Cloak)",
            section = patchesSection,
            position = 5
    )
    default boolean ardougne() {
        return true;
    }

    @ConfigItem(
            keyName = "falador",
            name = "Falador",
            description = "Standard patch south of Falador (Explorer's Ring)",
            section = patchesSection,
            position = 6
    )
    default boolean falador() {
        return true;
    }

    @ConfigItem(
            keyName = "portPhasmatys",
            name = "Port Phasmatys",
            description = "Standard patch west of Port Phasmatys (Ectophial)",
            section = patchesSection,
            position = 7
    )
    default boolean portPhasmatys() {
        return true;
    }

    @ConfigItem(
            keyName = "varlamore",
            name = "Varlamore",
            description = "Standard patch in Varlamore (POH House Teleport)",
            section = patchesSection,
            position = 8
    )
    default boolean varlamore() {
        return true;
    }

    @ConfigItem(
            keyName = "harmony",
            name = "Harmony Island",
            description = "Standard patch on Harmony Island (requires Morytania Elite)",
            section = patchesSection,
            position = 9
    )
    default boolean harmony() {
        return false;
    }

    // --- Overlays & Visuals Section ---

    @ConfigItem(
            keyName = "showOverlayPanel",
            name = "Show HUD Panel",
            description = "Display the floating herb run HUD panel",
            section = visualsSection,
            position = 0
    )
    default boolean showOverlayPanel() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightPatches",
            name = "Highlight Patches",
            description = "Draw high-visibility outlines and text over target herb patches",
            section = visualsSection,
            position = 1
    )
    default boolean highlightPatches() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightLeprechaun",
            name = "Highlight Tool Leprechaun",
            description = "Highlight the Tool Leprechaun when you have unnoted herbs to exchange",
            section = visualsSection,
            position = 2
    )
    default boolean highlightLeprechaun() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightInventoryItems",
            name = "Highlight Inventory Items",
            description = "Highlight active teleports, seeds, compost, or herbs in your inventory",
            section = visualsSection,
            position = 3
    )
    default boolean highlightInventoryItems() {
        return true;
    }

    @ConfigItem(
            keyName = "drawPatchClickbox",
            name = "Draw Patch Clickbox",
            description = "Draw exact polygon clickbox of the herb patch on screen",
            section = visualsSection,
            position = 4
    )
    default boolean drawPatchClickbox() {
        return true;
    }
}
