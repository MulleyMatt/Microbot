package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.plugins.microbot.muspah.MuspahPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 * Renders a subtle sinusoidal screen flash alert when:
 * 1. Muspah charges its purple Magic corruption attack (9918) and Protect from Magic is NOT active.
 * 2. Muspah teleports to the center for the Shockwave (9925) and the player is NOT on a safe tile behind a spike.
 */
public class MuspahFlashOverlay extends Overlay
{
    private final Client client;
    private final MuspahPlugin plugin;
    private final MuspahConfig config;

    @Inject
    private MuspahFlashOverlay(Client client, MuspahPlugin plugin, MuspahConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getMuspahNpc() == null || plugin.getMuspahNpc().isDead())
        {
            return null;
        }

        boolean flashMagic = config.magicAttackFlash()
                && plugin.getMagicAttackTicks() > 0
                && !client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);

        boolean flashShockwave = config.shockwaveFlash()
                && plugin.getShockwaveTicks() > 0
                && !plugin.isPlayerOnShockwaveSafeTile();

        if (!flashMagic && !flashShockwave)
        {
            return null;
        }

        double pulse = (Math.sin(System.currentTimeMillis() / 130.0) + 1.0) / 2.0;
        int fillAlpha = (int) (15 + pulse * 45);
        int borderAlpha = (int) (110 + pulse * 140);

        Color baseColor = flashMagic ? new Color(210, 40, 255) : new Color(255, 110, 0);
        String bannerText = flashMagic
                ? "PRAY MAGIC! (" + plugin.getMagicAttackTicks() + "t)"
                : "SHOCKWAVE - HIDE BEHIND SPIKE! (" + plugin.getShockwaveTicks() + "t)";

        Rectangle bounds = graphics.getClipBounds();
        if (bounds == null)
        {
            bounds = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        }

        graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), fillAlpha));
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        Stroke prevStroke = graphics.getStroke();
        graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), borderAlpha));
        graphics.setStroke(new BasicStroke(5.0f));
        graphics.drawRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
        graphics.setStroke(prevStroke);

        Font prevFont = graphics.getFont();
        Font alertFont = prevFont.deriveFont(Font.BOLD, 22.0f);
        graphics.setFont(alertFont);
        FontMetrics fm = graphics.getFontMetrics();
        int textX = bounds.x + (bounds.width - fm.stringWidth(bannerText)) / 2;
        int textY = bounds.y + 52;

        graphics.setColor(Color.BLACK);
        graphics.drawString(bannerText, textX + 2, textY + 2);
        graphics.setColor(Color.WHITE);
        graphics.drawString(bannerText, textX, textY);
        graphics.setFont(prevFont);

        return null;
    }
}
