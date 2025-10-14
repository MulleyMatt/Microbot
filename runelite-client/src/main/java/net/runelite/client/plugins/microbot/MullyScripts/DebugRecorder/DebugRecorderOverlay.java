package net.runelite.client.plugins.microbot.MullyScripts.DebugRecorder;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

/**
 * Simple overlay to show recording status (ON/OFF) and count of logged actions in-game.
 */
public class DebugRecorderOverlay extends Overlay {

    private final DebugRecorderPlugin plugin;
    private final PanelComponent panel = new PanelComponent();

    @Inject
    public DebugRecorderOverlay(DebugRecorderPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D g) {
        panel.getChildren().clear();

        if (plugin.isRecording()) {
            panel.getChildren().add(TitleComponent.builder()
                    .text("🎥 Recording: ON (" + plugin.getRecordedCount() + " actions)")
                    .color(Color.GREEN)
                    .build());
        } else {
            panel.getChildren().add(TitleComponent.builder()
                    .text("⏹ Recording: OFF")
                    .color(Color.RED)
                    .build());
        }

        panel.setPreferredSize(new Dimension(180, 30));
        return panel.render(g);
    }
}
