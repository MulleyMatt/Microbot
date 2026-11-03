package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

/**
 * Displays the fight duration timer, modeled after Zulrah Overlay Helper's InstanceTimerOverlay.
 */
public class MuspahInstanceTimerOverlay extends OverlayPanel
{
    private final MuspahConfig config;
    private Instant fightStartTimer;

    @Inject
    private MuspahInstanceTimerOverlay(MuspahConfig config)
    {
        this.config = config;
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.instanceTimer() || fightStartTimer == null)
        {
            return null;
        }

        Duration elapsed = Duration.between(fightStartTimer, Instant.now());
        long totalSeconds = Math.max(0L, elapsed.getSeconds());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Muspah Timer:")
                .leftColor(Color.WHITE)
                .right(String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60))
                .rightColor(Color.GREEN)
                .build());

        return super.render(graphics);
    }

    public void setTimerIfNotRunning()
    {
        if (fightStartTimer == null)
        {
            fightStartTimer = Instant.now();
        }
    }

    public void resetTimer()
    {
        fightStartTimer = null;
    }
}
