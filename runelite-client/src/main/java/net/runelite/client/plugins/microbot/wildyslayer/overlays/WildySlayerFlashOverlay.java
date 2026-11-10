package net.runelite.client.plugins.microbot.wildyslayer.overlays;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerConfig;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerPlugin;
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
 * Renders a smooth sinusoidal red screen flash alert when an enemy PKer is detected in the Wilderness.
 */
public class WildySlayerFlashOverlay extends Overlay
{
    private final Client client;
    private final WildySlayerPlugin plugin;
    private final WildySlayerConfig config;

    @Inject
    private WildySlayerFlashOverlay(Client client, WildySlayerPlugin plugin, WildySlayerConfig config)
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
        if (!plugin.isInWilderness() || !config.pkerScreenFlash() || plugin.getDetectedThreatPlayers().isEmpty())
        {
            return null;
        }

        double pulse = (Math.sin(System.currentTimeMillis() / 130.0) + 1.0) / 2.0;
        int fillAlpha = (int) (18 + pulse * 48);
        int borderAlpha = (int) (120 + pulse * 130);

        Color baseColor = new Color(255, 25, 25);
        Player pker = plugin.getDetectedThreatPlayers().get(0);
        String pkerName = pker.getName() != null ? pker.getName() : "Enemy";
        int wildyLevel = plugin.getCurrentWildernessLevel();

        String bannerText;
        if (plugin.isTeleblocked())
        {
            bannerText = "PKER: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - TELEBLOCKED, ESCAPE!";
        }
        else if (plugin.isSeedPodTriggeredThisEncounter())
        {
            bannerText = "PKER: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - SEED POD ACTIVATED!";
        }
        else if (wildyLevel > config.maxSeedPodWildernessLevel())
        {
            bannerText = "PKER: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - ABOVE LVL "
                    + config.maxSeedPodWildernessLevel() + " WILDY, RUN SOUTH!";
        }
        else
        {
            bannerText = "PKER DETECTED: " + pkerName + " (Lvl " + pker.getCombatLevel() + ") - TELEPORT OUT!";
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
