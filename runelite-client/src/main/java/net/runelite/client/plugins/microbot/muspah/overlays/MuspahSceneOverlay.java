package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.FontType;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.plugins.microbot.muspah.MuspahForm;
import net.runelite.client.plugins.microbot.muspah.MuspahOverlayUtils;
import net.runelite.client.plugins.microbot.muspah.MuspahPlugin;
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Renders 3D scene overlays for Phantom Muspah, modeled after Zulrah Overlay Helper's SceneOverlay:
 * - 5x5 boss tile colored by carapace form + Ranged "Phase Hat" icon and weapon tag
 * - Attack & special tick counters over Muspah
 * - Brown (Melee) form kiting danger-range border (Red in melee danger, Green at safe kiting distance)
 * - Center Shockwave safe-tile highlights behind floor spikes
 * - Incoming spike warnings, stationary spikes, homing spikes, and wandering shadow orbs
 * - Post-kill prayer conservation reminder
 */
public class MuspahSceneOverlay extends Overlay
{
    private static final Color MAGIC_CHARGE_COLOR = new Color(255, 60, 255);
    private static final Color STATIONARY_SPIKE_COLOR = new Color(255, 165, 0);

    private final Client client;
    private final MuspahPlugin plugin;
    private final MuspahConfig config;
    private final SkillIconManager skillIconManager;

    @Inject
    private MuspahSceneOverlay(
            Client client,
            MuspahPlugin plugin,
            MuspahConfig config,
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
        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());

        NPC muspah = plugin.getMuspahNpc();
        if (muspah != null && !muspah.isDead())
        {
            renderMeleeDangerBorder(graphics, muspah);
            renderMuspahPhaseTile(graphics, muspah);
            renderMuspahTicks(graphics, muspah);
            renderShockwaveSafeTiles(graphics);
            renderSpikesAndHazards(graphics);
            renderShadowOrbs(graphics);
        }
        else
        {
            renderPrayerConservation(graphics);
        }

        graphics.setFont(prevFont);
        return null;
    }

    private void renderMuspahPhaseTile(Graphics2D graphics, NPC muspah)
    {
        MuspahForm form = plugin.getCurrentForm();
        if (form == null)
        {
            return;
        }

        LocalPoint localPoint = muspah.getLocalLocation();
        if (localPoint == null)
        {
            return;
        }

        int size = plugin.getMuspahSize();
        Color tileColor = plugin.getMagicAttackTicks() > 0 ? MAGIC_CHARGE_COLOR : form.getColor();

        if (config.displayMuspahTile()
                || config.phaseDisplayType() == MuspahConfig.DisplayType.TILE
                || config.phaseDisplayType() == MuspahConfig.DisplayType.BOTH)
        {
            Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, localPoint, size);
            MuspahOverlayUtils.renderPolygon(
                    graphics,
                    tilePoly,
                    tileColor,
                    config.outlineWidth(),
                    config.fillAlpha()
            );
        }

        if (config.phaseDisplayType() == MuspahConfig.DisplayType.OFF
                || config.phaseDisplayType() == MuspahConfig.DisplayType.OVERLAY)
        {
            return;
        }

        Point centerCanvas = Perspective.localToCanvas(client, localPoint, client.getPlane(), 0);
        if (centerCanvas == null)
        {
            return;
        }

        String tagText = getMuspahPhaseString(form);
        Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(tagText, graphics);
        Point textLoc = new Point(
                centerCanvas.getX() - (int) (bounds.getWidth() / 2),
                centerCanvas.getY()
        );

        MuspahOverlayUtils.renderTextLocation(
                graphics,
                textLoc,
                tagText,
                tileColor,
                config::textOutline
        );

        if (config.phaseHats())
        {
            boolean smallIcon = config.fontType() != FontType.BOLD;
            BufferedImage skillImage = skillIconManager.getSkillImage(Skill.RANGED, smallIcon);
            if (skillImage != null)
            {
                int imgX = textLoc.getX() - skillImage.getWidth() - 4;
                int imgY = centerCanvas.getY() - skillImage.getHeight() + 2;
                graphics.drawImage(skillImage, imgX, imgY, null);
            }
        }
    }

    private String getMuspahPhaseString(MuspahForm form)
    {
        if (plugin.getMagicAttackTicks() > 0)
        {
            return "PRAY MAGIC! (" + plugin.getMagicAttackTicks() + "t)";
        }
        if (plugin.getShockwaveTicks() > 0)
        {
            return "SHOCKWAVE - HIDE BEHIND SPIKE!";
        }
        if (!config.phaseTags())
        {
            return form.getDisplayName();
        }
        return form.getDisplayName() + " - " + form.getWeaponPrompt();
    }

    private void renderMeleeDangerBorder(Graphics2D graphics, NPC muspah)
    {
        if (!config.meleeDangerRange() || plugin.getCurrentForm() != MuspahForm.MELEE)
        {
            return;
        }

        LocalPoint muspahLocal = muspah.getLocalLocation();
        WorldPoint muspahSw = muspah.getWorldLocation();
        Player localPlayer = client.getLocalPlayer();
        if (muspahLocal == null || muspahSw == null || localPlayer == null || localPlayer.getWorldLocation() == null)
        {
            return;
        }

        int size = plugin.getMuspahSize();
        boolean inDanger = MuspahPlugin.isPlayerInMeleeDanger(muspahSw, size, localPlayer.getWorldLocation());
        Color rangeColor = inDanger ? Color.RED : Color.GREEN;

        // Draw 1-tile expanded melee reach border (7x7 around 5x5 Muspah)
        Polygon meleeReachPoly = Perspective.getCanvasTileAreaPoly(client, muspahLocal, size + 2);
        MuspahOverlayUtils.renderPolygon(
                graphics,
                meleeReachPoly,
                rangeColor,
                Math.max(1, config.outlineWidth()),
                inDanger ? 25 : 8
        );

        if (inDanger)
        {
            String kiteMsg = "MELEE RANGE - KITE!";
            Point playerTextLoc = localPlayer.getCanvasTextLocation(
                    graphics,
                    kiteMsg,
                    localPlayer.getLogicalHeight() + 20
            );
            MuspahOverlayUtils.renderTextLocation(
                    graphics,
                    playerTextLoc,
                    kiteMsg,
                    Color.RED,
                    config::textOutline
            );
        }
    }

    private void renderMuspahTicks(Graphics2D graphics, NPC muspah)
    {
        if (!config.attackTickCounter())
        {
            return;
        }

        int attackTicks = plugin.getAttackTicks();
        int shockwaveTicks = plugin.getShockwaveTicks();
        int magicTicks = plugin.getMagicAttackTicks();

        StringBuilder sb = new StringBuilder();
        if (shockwaveTicks > 0)
        {
            sb.append("Shockwave: ").append(shockwaveTicks);
        }
        else if (magicTicks > 0)
        {
            sb.append("Magic: ").append(magicTicks);
        }
        else if (attackTicks > 0)
        {
            sb.append(attackTicks);
        }

        if (sb.length() == 0)
        {
            return;
        }

        String cntString = sb.toString();
        Point textLocation = muspah.getCanvasTextLocation(
                graphics,
                cntString,
                muspah.getLogicalHeight() - 120
        );
        Color color = magicTicks > 0
                ? MAGIC_CHARGE_COLOR
                : (shockwaveTicks > 0 ? Color.ORANGE : config.tickCounterColors());

        MuspahOverlayUtils.renderTextLocation(
                graphics,
                textLocation,
                cntString,
                color,
                config::textOutline
        );
    }

    private void renderShockwaveSafeTiles(Graphics2D graphics)
    {
        if (!config.shockwaveSafespots() || plugin.getShockwaveTicks() <= 0)
        {
            return;
        }

        Set<WorldPoint> safeTiles = plugin.getCurrentShockwaveSafeTiles();
        if (safeTiles.isEmpty())
        {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        WorldPoint playerLoc = localPlayer != null ? localPlayer.getWorldLocation() : null;

        WorldPoint closestSafeTile = null;
        if (playerLoc != null)
        {
            closestSafeTile = safeTiles.stream()
                    .min(Comparator.comparingInt(wp -> wp.distanceTo2D(playerLoc)))
                    .orElse(null);
        }

        Color baseSafeColor = config.safeTileColor();
        for (WorldPoint safeWp : safeTiles)
        {
            LocalPoint lp = LocalPoint.fromWorld(client, safeWp);
            if (lp == null)
            {
                continue;
            }

            boolean isClosest = safeWp.equals(closestSafeTile);
            String label = isClosest ? "SAFE (" + plugin.getShockwaveTicks() + "t)" : "SAFE";
            Color color = isClosest ? Color.GREEN : baseSafeColor;

            drawSceneTile(
                    graphics,
                    lp,
                    label,
                    color,
                    isClosest ? config.outlineWidth() + 1 : config.outlineWidth(),
                    isClosest ? Math.min(255, config.fillAlpha() + 45) : config.fillAlpha()
            );
        }
    }

    private void renderSpikesAndHazards(Graphics2D graphics)
    {
        if (!config.highlightSpikes())
        {
            return;
        }

        Color warningColor = config.spikeWarningColor();

        // 1. Incoming spike warning tiles (including form-switch player-tile warning)
        for (Map.Entry<WorldPoint, Integer> entry : plugin.getSpikeWarningTiles().entrySet())
        {
            LocalPoint lp = LocalPoint.fromWorld(client, entry.getKey());
            if (lp != null)
            {
                drawSceneTile(
                        graphics,
                        lp,
                        "SPIKE (" + entry.getValue() + ")",
                        warningColor,
                        config.outlineWidth(),
                        Math.min(255, config.fillAlpha() + 35)
                );
            }
        }

        // 2. Moving / homing spikes
        for (WorldPoint homingWp : plugin.getHomingSpikes().values())
        {
            LocalPoint lp = LocalPoint.fromWorld(client, homingWp);
            if (lp != null)
            {
                drawSceneTile(
                        graphics,
                        lp,
                        "HOMING",
                        Color.RED,
                        config.outlineWidth() + 1,
                        Math.min(255, config.fillAlpha() + 40)
                );
            }
        }

        // 3. Stationary floor spikes
        for (WorldPoint spikeWp : plugin.getStationarySpikes().values())
        {
            LocalPoint lp = LocalPoint.fromWorld(client, spikeWp);
            if (lp != null)
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                MuspahOverlayUtils.renderPolygon(
                        graphics,
                        poly,
                        STATIONARY_SPIKE_COLOR,
                        1,
                        Math.max(5, config.fillAlpha() / 2)
                );
            }
        }
    }

    private void renderShadowOrbs(Graphics2D graphics)
    {
        if (!config.highlightShadowOrbs() || plugin.getShadowOrbs().isEmpty())
        {
            return;
        }

        Color orbColor = config.shadowOrbColor();
        for (NPC orb : plugin.getShadowOrbs())
        {
            if (orb == null || orb.isDead())
            {
                continue;
            }
            LocalPoint lp = orb.getLocalLocation();
            if (lp != null)
            {
                drawSceneTile(
                        graphics,
                        lp,
                        "ORB",
                        orbColor,
                        config.outlineWidth(),
                        Math.min(255, config.fillAlpha() + 30)
                );
            }
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
        MuspahOverlayUtils.renderTextLocation(
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
        MuspahOverlayUtils.renderPolygon(graphics, poly, color, strokeWidth, fillAlpha);

        if (label != null && !label.isEmpty())
        {
            Point textLoc = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
            MuspahOverlayUtils.renderTextLocation(graphics, textLoc, label, color, config::textOutline);
        }
    }
}
