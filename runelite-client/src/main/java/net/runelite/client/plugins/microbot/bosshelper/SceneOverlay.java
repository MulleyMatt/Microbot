package net.runelite.client.plugins.microbot.bosshelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import java.util.List;

@Slf4j
public class SceneOverlay extends Overlay {

    private final Client client;
    private final BossHelperPlugin plugin;
    private final BossHelperConfig config;

    private static final Stroke TILE_STROKE = new BasicStroke(2.0f);

    @Inject
    public SceneOverlay(Client client, BossHelperPlugin plugin, BossHelperConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> safeTiles = plugin.getSafeTiles();
        List<WorldPoint> hazardTiles = plugin.getHazardTiles();

        if (safeTiles.isEmpty() && hazardTiles.isEmpty()) {
            return null;
        }

        Color safeColor = config.safeTileColor();
        Color safeOutline = new Color(safeColor.getRed(), safeColor.getGreen(), safeColor.getBlue(), 230);
        Color safeFill = new Color(safeColor.getRed(), safeColor.getGreen(), safeColor.getBlue(), Math.min(safeColor.getAlpha(), 90));

        Color hazardColor = config.hazardTileColor();
        Color hazardOutline = new Color(hazardColor.getRed(), hazardColor.getGreen(), hazardColor.getBlue(), 230);
        Color hazardFill = new Color(hazardColor.getRed(), hazardColor.getGreen(), hazardColor.getBlue(), Math.min(hazardColor.getAlpha(), 110));

        for (WorldPoint wp : safeTiles) {
            if (wp != null) {
                renderWorldPoint(graphics, wp, safeOutline, safeFill, TILE_STROKE);
            }
        }

        for (WorldPoint wp : hazardTiles) {
            if (wp != null) {
                renderWorldPoint(graphics, wp, hazardOutline, hazardFill, TILE_STROKE);
            }
        }

        return null;
    }

    private void renderWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color outlineColor, Color fillColor, Stroke stroke) {
        if (worldPoint == null) {
            return;
        }

        // Handle instanced areas (e.g. Zulrah, Vorkath, Hunllef)
        Collection<WorldPoint> instancePoints = WorldPoint.toLocalInstance(client, worldPoint);
        if (instancePoints.isEmpty()) {
            renderTile(graphics, worldPoint, outlineColor, fillColor, stroke);
        } else {
            for (WorldPoint instancedWp : instancePoints) {
                renderTile(graphics, instancedWp, outlineColor, fillColor, stroke);
            }
        }
    }

    private void renderTile(Graphics2D graphics, WorldPoint worldPoint, Color outlineColor, Color fillColor, Stroke stroke) {
        // Entity guide Gotcha 26: Null-check LocalPoint.fromWorld
        LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
        if (localPoint == null) {
            return;
        }

        // Entity guide Gotcha 27: Bounds-check LocalPoint against its own worldview before projecting
        WorldView wv = client.getWorldView(localPoint.getWorldView());
        if (wv == null || localPoint.getSceneX() < 0 || localPoint.getSceneY() < 0
                || localPoint.getSceneX() >= wv.getSizeX() || localPoint.getSceneY() >= wv.getSizeY()) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
        if (poly != null) {
            OverlayUtil.renderPolygon(graphics, poly, outlineColor, fillColor, stroke);
        }
    }
}
