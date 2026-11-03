package net.runelite.client.plugins.microbot.spindel.overlays;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.plugins.microbot.spindel.SpindelConfig;
import net.runelite.client.plugins.microbot.spindel.SpindelPlugin;
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
 * Renders a smooth sinusoidal screen flash alert when:
 * 1. An enemy PKer enters Spindel's cave.
 * 2. The player is standing on or inside Spindel's expanding Web hazard.
 */
public class SpindelFlashOverlay extends Overlay
{
    private final Client client;
    private final SpindelPlugin plugin;
    private final SpindelConfig config;

    @Inject
    private SpindelFlashOverlay(Client client, SpindelPlugin plugin, SpindelConfig config)
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
        if (!plugin.isInSpindelCave())
        {
            return null;
        }

        boolean flashPker = config.pkerScreenFlash() && !plugin.getDetectedThreatPlayers().isEmpty();
        boolean flashWeb = config.webDangerFlash() && plugin.isPlayerInWebDanger();

        if (!flashPker && !flashWeb)
        {
            return null;
        }

        double pulse = (Math.sin(System.currentTimeMillis() / 130.0) + 1.0) / 2.0;
        int fillAlpha = (int) (18 + pulse * 48);
        int borderAlpha = (int) (120 + pulse * 130);

        Color baseColor;
        String bannerText;

        if (flashPker)
        {
            baseColor = new Color(255, 25, 25);
            Player pker = plugin.getDetectedThreatPlayers().get(0);
            String pkerName = pker.getName() != null ? pker.getName() : "Enemy";
            if (plugin.isTeleblocked())
            {
                bannerText = "PKER: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - TELEBLOCKED, RUN TO EXIT!";
            }
            else if (plugin.isSeedPodTriggeredThisEncounter())
            {
                bannerText = "PKER: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - SEED POD ACTIVATED!";
            }
            else
            {
                bannerText = "PKER DETECTED: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - TELEPORT OUT!";
            }
        }
        else
        {
            baseColor = new Color(255, 110, 0);
            bannerText = "WEB HAZARD - MOVE 2-3 TILES AWAY IMMEDIATELY!";
        }

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
        Font alertFont = prevFont.deriveFont(Font.BOLD, 21.0f);
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
