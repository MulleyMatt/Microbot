package net.runelite.client.plugins.microbot.MullyScripts.DebugRecorder;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

/**
 * Overlay that displays recording state, action count,
 * generation hint, and last generated script path.
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

        // Hint for generation
        panel.getChildren().add(TitleComponent.builder()
                .text("Press G to pick a recording and generate script")
                .color(Color.WHITE)
                .build());

        // Show last generated path
        String last = plugin.getLastGeneratedPath();
        if (last != null && !last.isEmpty()) {
            panel.getChildren().add(TitleComponent.builder()
                    .text("✅ Last generated: " + shortenPath(last, 40))
                    .color(Color.CYAN)
                    .build());
        }

        panel.setPreferredSize(new Dimension(500, 80));
        return panel.render(g);
    }

    private String shortenPath(String path, int maxLen) {
        if (path.length() <= maxLen) return path;
        int half = maxLen / 2;
        return path.substring(0, half) + "..." + path.substring(path.length() - half);
    }
}
