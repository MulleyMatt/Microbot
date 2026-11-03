package net.runelite.client.plugins.microbot.spindel.overlays;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.FontType;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.microbot.spindel.SpindelConfig;
import net.runelite.client.plugins.microbot.spindel.SpindelOverlayUtils;
import net.runelite.client.plugins.microbot.spindel.SpindelPhase;
import net.runelite.client.plugins.microbot.spindel.SpindelPlugin;
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
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders 3D scene overlays for Spindel Helper:
 * - Spindel 5x5 tile colored by Ranged/Magic phase + 8-attack rotation counter
 * - Expanding floor Web hazard tiles + safe escape tiles 2-3 tiles outside the web
 * - Spindel's Spiderling highlights
 * - Enemy PKer player highlights
 * - Cave Exit clickbox highlight
 * - Post-kill prayer conservation prompt
 */
public class SpindelSceneOverlay extends Overlay
{
    private static final Color CAVE_EXIT_NORMAL_COLOR = new Color(0, 210, 211);
    private static final Color CAVE_EXIT_URGENT_COLOR = new Color(255, 50, 50);

    private final Client client;
    private final SpindelPlugin plugin;
    private final SpindelConfig config;
    private final SkillIconManager skillIconManager;

    @Inject
    private SpindelSceneOverlay(
            Client client,
            SpindelPlugin plugin,
            SpindelConfig config,
            SkillIconManager skillIconManager)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.skillIconManager = skillIconManager;

        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInSpindelCave())
        {
            return null;
        }

        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());

        renderCaveExit(graphics);
        renderThreatPlayers(graphics);
        renderWebHazardsAndSafeTiles(graphics);
        renderSpiderlings(graphics);

        NPC spindel = plugin.getSpindelNpc();
        if (spindel != null && !spindel.isDead())
        {
            renderSpindelTileAndPhase(graphics, spindel);
            renderSpindelTicks(graphics, spindel);
        }
        else
        {
            renderPrayerConservation(graphics);
        }

        graphics.setFont(prevFont);
        return null;
    }

    private void renderSpindelTileAndPhase(Graphics2D graphics, NPC spindel)
    {
        SpindelPhase phase = plugin.getCurrentPhase();
        if (phase == null)
        {
            return;
        }

        LocalPoint localPoint = spindel.getLocalLocation();
        if (localPoint == null)
        {
            return;
        }

        int size = plugin.getSpindelSize();
        Color tileColor = phase.getColor();

        if (config.displaySpindelTile())
        {
            Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, localPoint, size);
            SpindelOverlayUtils.renderPolygon(
                    graphics,
                    tilePoly,
                    tileColor,
                    config.outlineWidth(),
                    config.fillAlpha()
            );
        }

        if (!config.attackTickCounter())
        {
            return;
        }

        Point centerCanvas = Perspective.localToCanvas(client, localPoint, client.getPlane(), 0);
        if (centerCanvas == null)
        {
            return;
        }

        int remaining = plugin.getRemainingAttacksInPhase();
        String tagText = phase.getDisplayName() + " (" + remaining + " left)";
        Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(tagText, graphics);
        Point textLoc = new Point(
                centerCanvas.getX() - (int) (bounds.getWidth() / 2),
                centerCanvas.getY()
        );

        SpindelOverlayUtils.renderTextLocation(
                graphics,
                textLoc,
                tagText,
                remaining <= 2 ? Color.YELLOW : tileColor,
                config::textOutline
        );

        boolean smallIcon = config.fontType() != FontType.BOLD;
        BufferedImage skillImage = skillIconManager.getSkillImage(phase.getSkillIcon(), smallIcon);
        if (skillImage != null)
        {
            int imgX = textLoc.getX() - skillImage.getWidth() - 4;
            int imgY = centerCanvas.getY() - skillImage.getHeight() + 2;
            graphics.drawImage(skillImage, imgX, imgY, null);
        }
    }

    private void renderSpindelTicks(Graphics2D graphics, NPC spindel)
    {
        if (!config.attackTickCounter())
        {
            return;
        }

        int attackTicks = plugin.getAttackTicks();
        if (attackTicks <= 0)
        {
            return;
        }

        String tickStr = String.valueOf(attackTicks);
        Point textLocation = spindel.getCanvasTextLocation(
                graphics,
                tickStr,
                spindel.getLogicalHeight() - 80
        );
        SpindelOverlayUtils.renderTextLocation(
                graphics,
                textLocation,
                tickStr,
                Color.WHITE,
                config::textOutline
        );
    }

    private void renderWebHazardsAndSafeTiles(Graphics2D graphics)
    {
        if (!config.highlightWebHazard())
        {
            return;
        }

        Set<WorldPoint> webHazardTiles = plugin.getAllWebHazardTiles();
        if (webHazardTiles.isEmpty())
        {
            return;
        }

        Color webColor = config.webHazardColor();
        for (WorldPoint wp : webHazardTiles)
        {
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null)
            {
                continue;
            }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            SpindelOverlayUtils.renderPolygon(
                    graphics,
                    poly,
                    webColor,
                    Math.max(1, config.outlineWidth() - 1),
                    config.fillAlpha()
            );
        }

        for (Map.Entry<WorldPoint, Integer> entry : plugin.getWebCenterTiles().entrySet())
        {
            LocalPoint centerLp = LocalPoint.fromWorld(client, entry.getKey());
            if (centerLp != null)
            {
                drawSceneTile(
                        graphics,
                        centerLp,
                        "WEB HAZARD - AVOID!",
                        webColor,
                        config.outlineWidth() + 1,
                        Math.min(255, config.fillAlpha() + 35)
                );
            }
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || localPlayer.getWorldLocation() == null)
        {
            return;
        }

        boolean inWebDanger = plugin.isPlayerInWebDanger();
        if (inWebDanger)
        {
            String moveMsg = "ON WEB - MOVE 2-3 TILES AWAY!";
            Point playerTextLoc = localPlayer.getCanvasTextLocation(
                    graphics,
                    moveMsg,
                    localPlayer.getLogicalHeight() + 20
            );
            SpindelOverlayUtils.renderTextLocation(
                    graphics,
                    playerTextLoc,
                    moveMsg,
                    Color.RED,
                    config::textOutline
            );
        }

        if (config.highlightWebSafeTiles() && inWebDanger)
        {
            WorldPoint bossSw = plugin.getSpindelNpc() != null ? plugin.getSpindelNpc().getWorldLocation() : null;
            List<WorldPoint> safeTiles = SpindelPlugin.calculateWebSafeEscapeTiles(
                    localPlayer.getWorldLocation(),
                    webHazardTiles,
                    bossSw,
                    plugin.getSpindelSize()
            );

            Color safeColor = config.webSafeTileColor();
            for (int i = 0; i < safeTiles.size(); i++)
            {
                WorldPoint safeWp = safeTiles.get(i);
                LocalPoint lp = LocalPoint.fromWorld(client, safeWp);
                if (lp == null)
                {
                    continue;
                }
                boolean isBest = (i == 0);
                drawSceneTile(
                        graphics,
                        lp,
                        isBest ? "SAFE (STEP HERE)" : "SAFE",
                        safeColor,
                        isBest ? config.outlineWidth() + 1 : config.outlineWidth(),
                        isBest ? Math.min(255, config.fillAlpha() + 45) : config.fillAlpha()
                );
            }
        }
    }

    private void renderSpiderlings(Graphics2D graphics)
    {
        if (!config.highlightSpiderlings() || plugin.getSpiderlings().isEmpty())
        {
            return;
        }

        Color spiderColor = config.spiderlingColor();
        for (NPC spiderling : plugin.getSpiderlings())
        {
            if (spiderling == null || spiderling.isDead())
            {
                continue;
            }
            LocalPoint lp = spiderling.getLocalLocation();
            if (lp != null)
            {
                drawSceneTile(
                        graphics,
                        lp,
                        "SPIDERLING",
                        spiderColor,
                        config.outlineWidth(),
                        Math.min(255, config.fillAlpha() + 25)
                );
            }
        }
    }

    private void renderThreatPlayers(Graphics2D graphics)
    {
        if (!config.highlightEnemyPlayers() || plugin.getDetectedThreatPlayers().isEmpty())
        {
            return;
        }

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
                SpindelOverlayUtils.renderPolygon(
                        graphics,
                        poly,
                        Color.RED,
                        config.outlineWidth() + 1,
                        Math.min(255, config.fillAlpha() + 50)
                );
            }

            Point txtLoc = pker.getCanvasTextLocation(graphics, label, pker.getLogicalHeight() + 30);
            SpindelOverlayUtils.renderTextLocation(graphics, txtLoc, label, Color.RED, config::textOutline);
        }
    }

    private void renderCaveExit(Graphics2D graphics)
    {
        if (!config.highlightCaveExit() || plugin.getCaveExitObjects().isEmpty())
        {
            return;
        }

        boolean urgent = !plugin.getDetectedThreatPlayers().isEmpty() || plugin.isTeleblocked();
        Color exitColor = urgent ? CAVE_EXIT_URGENT_COLOR : CAVE_EXIT_NORMAL_COLOR;
        String label = urgent ? "ESCAPE EXIT - CLICK HERE!" : "Cave Exit";

        for (GameObject exitObj : plugin.getCaveExitObjects().keySet())
        {
            if (exitObj == null)
            {
                continue;
            }
            Shape clickbox = exitObj.getClickbox();
            if (clickbox != null)
            {
                SpindelOverlayUtils.renderPolygon(
                        graphics,
                        clickbox,
                        exitColor,
                        urgent ? config.outlineWidth() + 1 : config.outlineWidth(),
                        urgent ? Math.min(255, config.fillAlpha() + 45) : config.fillAlpha()
                );
            }
            Point txtLoc = exitObj.getCanvasTextLocation(graphics, label, 120);
            SpindelOverlayUtils.renderTextLocation(graphics, txtLoc, label, exitColor, config::textOutline);
        }
    }

    private void renderPrayerConservation(Graphics2D graphics)
    {
        if (!config.prayerConservation() || !plugin.isPostKillPrayerReminder())
        {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null || player.getOverheadIcon() == null)
        {
            return;
        }

        String conserveStr = "Turn off overheads to conserve prayer!";
        Point txtLoc = player.getCanvasTextLocation(
                graphics,
                conserveStr,
                player.getLogicalHeight() - 22
        );
        SpindelOverlayUtils.renderTextLocation(
                graphics,
                txtLoc,
                conserveStr,
                Color.RED,
                config::textOutline
        );
    }

    private void drawSceneTile(
            Graphics2D graphics,
            LocalPoint localPoint,
            String label,
            Color color,
            int strokeWidth,
            int fillAlpha)
    {
        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
        SpindelOverlayUtils.renderPolygon(graphics, poly, color, strokeWidth, fillAlpha);

        if (label != null && !label.isEmpty())
        {
            Point textLoc = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
            SpindelOverlayUtils.renderTextLocation(graphics, textLoc, label, color, config::textOutline);
        }
    }
}
