package net.runelite.client.plugins.microbot.createhelper.templates;

import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/**
 * Reusable full-screen flash overlay for idle AFK, full inventory, or spec attack alerts.
 * Uses smooth sinusoidal alpha pulsing so the alert is noticeable without blinding the user.
 * Pairs with Rs2SoundAlert for async sound notifications.
 */
public class ScreenFlashOverlayTemplate extends Overlay {

    private final Client client;

    // Conditions and colors (checked in order of priority)
    private BooleanSupplier fullInventoryCondition = () -> false;
    private Color fullInventoryColor = new Color(0, 80, 255, 120);

    private BooleanSupplier idleAfkCondition = () -> false;
    private Color idleAfkColor = new Color(255, 0, 0, 110);

    private BooleanSupplier specReadyCondition = () -> false;
    private Color specReadyColor = new Color(255, 220, 0, 90);

    // Audio rate-limiting state
    private Instant lastSoundAlertTime = Instant.EPOCH;
    private int soundRepeatIntervalSeconds = 3;
    private boolean soundEnabled = true;

    @Inject
    public ScreenFlashOverlayTemplate(Client client) {
        this.client = client;
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
    }

    public void configureConditions(
            BooleanSupplier fullInventoryCondition,
            BooleanSupplier idleAfkCondition,
            BooleanSupplier specReadyCondition,
            int soundRepeatIntervalSeconds
    ) {
        this.fullInventoryCondition = fullInventoryCondition;
        this.idleAfkCondition = idleAfkCondition;
        this.specReadyCondition = specReadyCondition;
        this.soundRepeatIntervalSeconds = soundRepeatIntervalSeconds;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getCanvas() == null) return null;

        // 1. Highest Priority: Full Inventory / Cannot Proceed
        if (fullInventoryCondition.getAsBoolean()) {
            triggerAudioIfDue();
            renderPulsingScreen(graphics, fullInventoryColor);
        }
        // 2. Second Priority: Idle / Inactive / AFK
        else if (idleAfkCondition.getAsBoolean()) {
            triggerAudioIfDue();
            renderPulsingScreen(graphics, idleAfkColor);
        }
        // 3. Third Priority: Special Attack 100% Ready
        else if (specReadyCondition.getAsBoolean()) {
            triggerAudioIfDue();
            renderPulsingScreen(graphics, specReadyColor);
        }

        return null;
    }

    private void renderPulsingScreen(Graphics2D graphics, Color baseColor) {
        if (baseColor == null) return;
        Dimension canvasSize = client.getCanvas().getSize();
        if (canvasSize == null || canvasSize.width <= 0 || canvasSize.height <= 0) return;

        long now = System.currentTimeMillis();
        // 1.0s smooth cycle
        double sine = Math.sin(now * (2.0 * Math.PI / 1000.0));
        double factor = 0.25 + 0.75 * ((sine + 1.0) / 2.0); // 0.25 to 1.0 scale
        int dynamicAlpha = (int) Math.round(baseColor.getAlpha() * factor);
        dynamicAlpha = Math.max(10, Math.min(255, dynamicAlpha));

        Color renderColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), dynamicAlpha);
        Color originalColor = graphics.getColor();
        graphics.setColor(renderColor);
        graphics.fillRect(0, 0, canvasSize.width, canvasSize.height);
        graphics.setColor(originalColor);
    }

    private void triggerAudioIfDue() {
        if (!soundEnabled) return;
        Instant now = Instant.now();
        if (soundRepeatIntervalSeconds <= 0) {
            if (lastSoundAlertTime.equals(Instant.EPOCH)) {
                Rs2SoundAlert.triggerAlert();
                lastSoundAlertTime = now;
            }
            return;
        }

        if (now.minusSeconds(soundRepeatIntervalSeconds).isAfter(lastSoundAlertTime)) {
            Rs2SoundAlert.triggerAlert();
            lastSoundAlertTime = now;
        }
    }

    /**
     * Call when action is clicked or state resolved to reset audio debounce.
     */
    public void resetAlert() {
        lastSoundAlertTime = Instant.EPOCH;
    }
}
