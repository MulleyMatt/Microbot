package net.runelite.client.plugins.microbot.barbarianfishing;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class BarbarianFishingFlashOverlay extends Overlay {

    private final Client client;
    private final BarbarianFishingPlugin plugin;
    private final BarbarianFishingConfig config;

    @Inject
    public BarbarianFishingFlashOverlay(Client client, BarbarianFishingPlugin plugin, BarbarianFishingConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getCanvas() == null) {
            return null;
        }

        // Trigger / sustain system sound alert while alert condition is active
        plugin.checkAlertSound();

        // 1. Inventory full blue flash takes highest priority
        if (plugin.shouldFlashBlue()) {
            renderScreenFlash(graphics, config.inventoryFullFlashColor());
        }
        // 2. AFK red flash takes second priority
        else if (plugin.shouldFlashRed()) {
            renderScreenFlash(graphics, config.afkFlashColor());
        }
        // 3. Special attack yellow flash when 100% and ready to use
        else if (plugin.shouldFlashYellow()) {
            renderScreenFlash(graphics, config.specialAttackFlashColor());
        }

        return null;
    }

    private void renderScreenFlash(Graphics2D graphics, Color baseColor) {
        if (baseColor == null) {
            return;
        }

        Dimension canvasSize = client.getCanvas().getSize();
        if (canvasSize == null || canvasSize.width <= 0 || canvasSize.height <= 0) {
            return;
        }

        BarbarianFishingFlashStyle style = config.flashStyle();
        Color renderColor;

        long now = System.currentTimeMillis();

        switch (style) {
            case FLASH:
                if (now % 1000 >= 500) {
                    return;
                }
                renderColor = baseColor;
                break;

            case SOLID:
                renderColor = baseColor;
                break;

            case PULSE:
            default:
                double sine = Math.sin(now * (2.0 * Math.PI / 1000.0));
                double factor = 0.25 + 0.75 * ((sine + 1.0) / 2.0);
                int dynamicAlpha = (int) Math.round(baseColor.getAlpha() * factor);
                dynamicAlpha = Math.max(10, Math.min(255, dynamicAlpha));
                renderColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), dynamicAlpha);
                break;
        }

        Color originalColor = graphics.getColor();
        graphics.setColor(renderColor);
        graphics.fillRect(0, 0, canvasSize.width, canvasSize.height);
        graphics.setColor(originalColor);
    }
}
