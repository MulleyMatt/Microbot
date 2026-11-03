package net.runelite.client.plugins.microbot.cachewarningfilter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class CacheWarningFilterOverlay extends OverlayPanel {

    private final CacheWarningFilterPlugin plugin;
    private final CacheWarningFilterConfig config;

    @Inject
    public CacheWarningFilterOverlay(CacheWarningFilterPlugin plugin, CacheWarningFilterConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Cache Warning Filter")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Blocked warnings:")
                .right(String.valueOf(plugin.getBlockedCount().get()))
                .build());

        return super.render(graphics);
    }
}
