package net.runelite.client.plugins.microbot.cachewarningfilter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("cachewarningfilter")
public interface CacheWarningFilterConfig extends Config {

    @ConfigSection(
            name = "Filter Settings",
            description = "Options for filtering cache mismatch and overlay warnings",
            position = 0
    )
    String filterSection = "filterSection";

    @ConfigItem(
            keyName = "hideHashMismatch",
            name = "Hide Cache Hash Mismatches",
            description = "Suppresses 'Mismatch in overlaid cache archive hash' warnings in chat",
            position = 1,
            section = filterSection
    )
    default boolean hideHashMismatch() {
        return true;
    }

    @ConfigItem(
            keyName = "hideAllOverlaidCacheWarnings",
            name = "Hide All Overlaid Cache Warnings",
            description = "Suppresses all warnings containing 'overlaid cache' in chat",
            position = 2,
            section = filterSection
    )
    default boolean hideAllOverlaidCacheWarnings() {
        return true;
    }

    @ConfigItem(
            keyName = "customFilterWords",
            name = "Additional Words/Phrases",
            description = "Comma-separated list of additional words or phrases to hide from the chat",
            position = 3,
            section = filterSection
    )
    default String customFilterWords() {
        return "";
    }

    @ConfigSection(
            name = "Overlay Settings",
            description = "Status overlay configuration",
            position = 10
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Counter Overlay",
            description = "Display a small on-screen counter of blocked warnings",
            position = 11,
            section = overlaySection
    )
    default boolean showOverlay() {
        return false;
    }
}
