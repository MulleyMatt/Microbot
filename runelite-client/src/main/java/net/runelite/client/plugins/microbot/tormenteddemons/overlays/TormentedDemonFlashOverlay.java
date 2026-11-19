package net.runelite.client.plugins.microbot.tormenteddemons.overlays;

import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonConfig;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonPlugin;
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
 * Sinusoidal screen flash alerts (ALWAYS_ON_TOP) for urgent Tormented Demon events:
 * - Jackpot Drop (Tormented Synapse or Burning Claw)
 * - Active Fire Bomb AoE danger (Ctrl+click 2+ tiles away)
 * - Low Hitpoints during combat
 * - Entering a Double / Triple multi-combat room
 */
public class TormentedDemonFlashOverlay extends Overlay
{
    private final Client client;
    private final TormentedDemonPlugin plugin;
    private final TormentedDemonConfig config;

    @Inject
    public TormentedDemonFlashOverlay(Client client, TormentedDemonPlugin plugin, TormentedDemonConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.screenFlashAlerts() || !plugin.isInGuthixianTemple())
        {
            return null;
        }

        Color baseColor = null;
        String bannerText = null;

        if (config.jackpotDropAlert() && plugin.isJackpotBannerActive())
        {
            baseColor = new Color(255, 215, 0);
            bannerText = "JACKPOT DROP: " + plugin.getJackpotDropName();
        }
        else if (plugin.isPlayerInFireBombDanger())
        {
            baseColor = new Color(255, 55, 25);
            bannerText = "FIRE BOMB! CTRL+CLICK 2+ TILES AWAY!";
        }
        else if (plugin.isLowHpInCombat())
        {
            baseColor = new Color(255, 45, 45);
            bannerText = "LOW HP! EAT FOOD NOW!";
        }
        else if (plugin.isInMultiCombatWarningRoom() && plugin.isInActiveFight())
        {
            baseColor = new Color(255, 165, 0);
            bannerText = "MULTI-COMBAT ROOM (" + plugin.getTdMultiwayLevel() + "x) - WATCH AGGRO!";
        }

        if (baseColor == null || bannerText == null)
        {
            return null;
        }

        double pulse = (Math.sin(System.currentTimeMillis() / 130.0) + 1.0) / 2.0;
        int fillAlpha = (int) (16 + pulse * 42);
        int borderAlpha = (int) (125 + pulse * 125);

        Rectangle bounds = graphics.getClipBounds();
        if (bounds == null)
        {
            bounds = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        }

        graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), fillAlpha));
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        Stroke oldStroke = graphics.getStroke();
        graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), borderAlpha));
        graphics.setStroke(new BasicStroke(5.0f));
        graphics.drawRect(bounds.x + 2, bounds.y + 2, Math.max(0, bounds.width - 4), Math.max(0, bounds.height - 4));
        graphics.setStroke(oldStroke);

        Font oldFont = graphics.getFont();
        Font bannerFont = new Font("Arial", Font.BOLD, 17);
        graphics.setFont(bannerFont);
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(bannerText);
        int boxWidth = textWidth + 28;
        int boxHeight = 32;
        int boxX = bounds.x + (bounds.width - boxWidth) / 2;
        int boxY = bounds.y + 38;

        graphics.setColor(new Color(15, 15, 15, 200));
        graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
        graphics.setColor(baseColor);
        graphics.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

        int textX = boxX + 14;
        int textY = boxY + 22;
        graphics.setColor(Color.BLACK);
        graphics.drawString(bannerText, textX + 1, textY + 1);
        graphics.setColor(baseColor);
        graphics.drawString(bannerText, textX, textY);
        graphics.setFont(oldFont);

        return null;
    }
}
