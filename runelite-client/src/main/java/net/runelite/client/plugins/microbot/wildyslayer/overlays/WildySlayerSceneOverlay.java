package net.runelite.client.plugins.microbot.wildyslayer.overlays;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerConfig;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerOverlayUtils;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * Renders 3D scene highlights for nearby enemy PKer players in the Wilderness.
 */
public class WildySlayerSceneOverlay extends Overlay
{
    private final Client client;
    private final WildySlayerPlugin plugin;
    private final WildySlayerConfig config;

    @Inject
    private WildySlayerSceneOverlay(
            Client client,
            WildySlayerPlugin plugin,
            WildySlayerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInWilderness() || !config.highlightEnemyPlayers() || plugin.getDetectedThreatPlayers().isEmpty())
        {
            return null;
        }

        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());

        for (Player pker : plugin.getDetectedThreatPlayers())
        {
            if (pker == null)
            {
                continue;
            }
            LocalPoint lp = pker.getLocalLocation();
            String name = pker.getName() != null ? pker.getName() : "PKer";
            String label = "PKER: " + name + " (Lvl " + pker.getCombatLevel() + ")";

            if (lp != null)
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                WildySlayerOverlayUtils.renderPolygon(
                        graphics,
                        poly,
                        Color.RED,
                        config.outlineWidth() + 1,
                        Math.min(255, config.fillAlpha() + 50)
                );
            }

            Point txtLoc = pker.getCanvasTextLocation(graphics, label, pker.getLogicalHeight() + 30);
            WildySlayerOverlayUtils.renderTextLocation(graphics, txtLoc, label, Color.RED, config::textOutline);
        }

        graphics.setFont(prevFont);
        return null;
    }
}
