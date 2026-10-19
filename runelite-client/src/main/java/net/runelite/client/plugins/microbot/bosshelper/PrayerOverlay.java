package net.runelite.client.plugins.microbot.bosshelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.mouse.BotEventGuard;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

@Slf4j
public class PrayerOverlay extends Overlay {

    private final Client client;
    private final BossHelperPlugin plugin;
    private final BossHelperConfig config;
    private final SpriteManager spriteManager;

    private final Rectangle bounds = new Rectangle();
    private volatile boolean hovered = false;
    private boolean mouseListenerHooked = false;

    private final MouseListener mouseListener = new MouseAdapter() {
        @Override
        public MouseEvent mouseMoved(MouseEvent e) {
            if (BotEventGuard.isSynthetic()) {
                return e;
            }
            hovered = bounds.contains(e.getPoint());
            return e;
        }

        @Override
        public MouseEvent mousePressed(MouseEvent e) {
            if (BotEventGuard.isSynthetic()) {
                return e;
            }
            if (bounds.contains(e.getPoint()) && SwingUtilities.isLeftMouseButton(e)) {
                activateRecommendedPrayer();
                e.consume();
            }
            return e;
        }
    };

    @Inject
    public PrayerOverlay(Client client, BossHelperPlugin plugin, BossHelperConfig config, SpriteManager spriteManager) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.spriteManager = spriteManager;

        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        setPosition(config.prayerOverlayPosition() != null ? config.prayerOverlayPosition() : OverlayPosition.DETACHED);

        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Activate", "Protection Prayer", entry -> activateRecommendedPrayer());
    }

    public void registerMouseListener() {
        if (!mouseListenerHooked && Microbot.getMouseManager() != null) {
            Microbot.getMouseManager().registerMouseListener(mouseListener);
            mouseListenerHooked = true;
        }
    }

    public void unregisterMouseListener() {
        if (mouseListenerHooked && Microbot.getMouseManager() != null) {
            Microbot.getMouseManager().unregisterMouseListener(mouseListener);
            mouseListenerHooked = false;
        }
    }

    public void activateRecommendedPrayer() {
        Prayer prayer = plugin.getRecommendedPrayer();
        if (prayer != null) {
            Microbot.getClientThread().runOnClientThread(() -> Rs2Prayer.fastPrayer(prayer));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Prayer prayer = plugin.getRecommendedPrayer();
        if (prayer == null) {
            bounds.setBounds(0, 0, 0, 0);
            return null;
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int iconScale = Math.max(24, Math.min(128, config.prayerIconScale()));
        int padding = 6;
        int textHeight = 14;
        int totalWidth = iconScale + (padding * 2);
        int totalHeight = iconScale + (padding * 2) + textHeight;

        boolean isActive = client.isPrayerActive(prayer);

        // Background
        graphics.setColor(new Color(18, 20, 26, 215));
        graphics.fillRoundRect(0, 0, totalWidth, totalHeight, 8, 8);

        // Border: Green if active, Pulsing red/orange if switch needed, Yellow if hovered
        Color borderColor;
        if (hovered) {
            borderColor = new Color(255, 235, 59, 240);
        } else if (isActive) {
            borderColor = new Color(76, 175, 80, 240);
        } else {
            borderColor = new Color(244, 67, 54, 240);
        }
        graphics.setColor(borderColor);
        graphics.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
        graphics.drawRoundRect(0, 0, totalWidth, totalHeight, 8, 8);

        // Sprite
        int spriteId = getSpriteIdForPrayer(prayer, isActive);
        BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
        if (sprite != null) {
            graphics.drawImage(sprite, padding, padding, iconScale, iconScale, null);
        }

        // Status text
        graphics.setFont(new Font("SansSerif", Font.BOLD, 10));
        String statusText;
        if (isActive) {
            statusText = "ACTIVE";
            graphics.setColor(new Color(129, 199, 132));
        } else {
            statusText = "SWITCH";
            graphics.setColor(new Color(255, 138, 128));
        }

        FontMetrics fm = graphics.getFontMetrics();
        int textX = (totalWidth - fm.stringWidth(statusText)) / 2;
        int textY = iconScale + padding + textHeight;
        graphics.drawString(statusText, textX, textY);

        Rectangle overlayBounds = getBounds();
        if (overlayBounds != null) {
            bounds.setBounds(overlayBounds);
        } else {
            bounds.setBounds(0, 0, totalWidth, totalHeight);
        }

        return new Dimension(totalWidth, totalHeight);
    }

    private int getSpriteIdForPrayer(Prayer prayer, boolean active) {
        switch (prayer) {
            case PROTECT_FROM_MAGIC:
                return active ? SpriteID.PRAYER_PROTECT_FROM_MAGIC : SpriteID.PRAYER_PROTECT_FROM_MAGIC_DISABLED;
            case PROTECT_FROM_MISSILES:
                return active ? SpriteID.PRAYER_PROTECT_FROM_MISSILES : SpriteID.PRAYER_PROTECT_FROM_MISSILES_DISABLED;
            case PROTECT_FROM_MELEE:
                return active ? SpriteID.PRAYER_PROTECT_FROM_MELEE : SpriteID.PRAYER_PROTECT_FROM_MELEE_DISABLED;
            default:
                return active ? SpriteID.PRAYER_PROTECT_FROM_MAGIC : SpriteID.PRAYER_PROTECT_FROM_MAGIC_DISABLED;
        }
    }
}
