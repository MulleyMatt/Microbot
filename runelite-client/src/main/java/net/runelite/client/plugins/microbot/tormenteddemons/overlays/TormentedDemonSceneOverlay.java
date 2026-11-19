package net.runelite.client.plugins.microbot.tormenteddemons.overlays;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonConfig;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonGearClassifier;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonOverlayUtils;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonPlugin;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;

/**
 * 3D Scene overlays (ABOVE_SCENE) for Tormented Demons:
 * - Hull & tile outlines colored by target style, Shield-Down (Gold), or 100% Accuracy (Cyan).
 * - Fire Bomb red AoE tiles + optimal 2+ tile green Ctrl-click escape tiles.
 * - Categorized ground loot highlights inside the temple.
 * - Wall/corner box-in warning around the player.
 */
public class TormentedDemonSceneOverlay extends Overlay
{
    private static final Color SHIELD_DOWN_GOLD = new Color(255, 215, 0);
    private static final Color DEFENCELESS_CYAN = Color.CYAN;
    private static final Color FIRE_BOMB_DANGER_COLOR = new Color(255, 50, 25);
    private static final Color FIRE_BOMB_SAFE_COLOR = Color.GREEN;

    private final Client client;
    private final TormentedDemonPlugin plugin;
    private final TormentedDemonConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public TormentedDemonSceneOverlay(
            Client client,
            TormentedDemonPlugin plugin,
            TormentedDemonConfig config,
            ModelOutlineRenderer modelOutlineRenderer)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;

        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInGuthixianTemple())
        {
            return null;
        }

        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());

        if (config.highlightDemonOverlays())
        {
            renderDemons(graphics);
        }

        if (config.highlightFireBombs())
        {
            renderFireBombsAndEscapeTiles(graphics);
        }

        if (config.highlightValuableDrops())
        {
            renderGroundDrops(graphics);
        }

        if (plugin.isBoxedInNearWallOrCorner())
        {
            renderCornerWarning(graphics);
        }

        if (plugin.isPostKillPrayerReminder() && !plugin.isInActiveFight())
        {
            renderTurnOffPrayersReminder(graphics);
        }

        graphics.setFont(prevFont);
        return null;
    }

    private void renderDemons(Graphics2D graphics)
    {
        Player local = client.getLocalPlayer();
        NPC primary = plugin.getPrimaryDemon();

        for (TormentedDemonState state : plugin.getTormentedDemons().values())
        {
            NPC demon = state.getNpc();
            if (demon == null || demon.isDead())
            {
                continue;
            }
            boolean engaged = state.isInCombatWithPlayer()
                    || demon == primary
                    || (local != null && (demon.getInteracting() == local || local.getInteracting() == demon));
            if (!engaged)
            {
                continue;
            }

            LocalPoint lp = demon.getLocalLocation();
            if (lp == null)
            {
                continue;
            }

            TormentedDemonGearClassifier.CombatStyle targetStyle =
                    TormentedDemonGearClassifier.resolveTargetStyleForDemon(state.getProtectedStyles(), config.combatStylePair());

            Color color;
            if (state.isShieldDown())
            {
                color = SHIELD_DOWN_GOLD;
            }
            else if (state.isDefenceless())
            {
                color = DEFENCELESS_CYAN;
            }
            else
            {
                color = targetStyle.getColor();
            }

            int outlineWidth = Math.max(1, config.outlineWidth());
            modelOutlineRenderer.drawOutline(demon, outlineWidth, color, 2);

            int size = plugin.getNpcSize(demon, 3);
            Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
            TormentedDemonOverlayUtils.renderPolygon(graphics, poly, color, outlineWidth, config.fillAlpha());

            StringBuilder label = new StringBuilder("HIT: ").append(targetStyle.getLabel());
            if (state.isShieldDown())
            {
                label.append(" [SHIELD DOWN!]");
            }
            else if (state.isDefenceless())
            {
                label.append(" [100% ACC!]");
            }
            else
            {
                label.append(" | ").append(state.getDamageRemainingToPrayerSwap()).append(" to swap");
            }

            String text = label.toString();
            Point textLoc = demon.getCanvasTextLocation(graphics, text, demon.getLogicalHeight() + 35);
            TormentedDemonOverlayUtils.renderTextLocation(graphics, textLoc, text, color, config::textOutline);
        }
    }

    private void renderFireBombsAndEscapeTiles(Graphics2D graphics)
    {
        if (client.getTopLevelWorldView() == null)
        {
            return;
        }
        for (WorldPoint wp : plugin.getFireBombDangerTiles().keySet())
        {
            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
            if (lp == null)
            {
                continue;
            }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            TormentedDemonOverlayUtils.renderPolygon(graphics, poly, FIRE_BOMB_DANGER_COLOR, 2, 85);
            Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, "FIRE!", 0);
            TormentedDemonOverlayUtils.renderTextLocation(graphics, txt, "FIRE!", FIRE_BOMB_DANGER_COLOR, config::textOutline);
        }

        if (plugin.isPlayerInFireBombDanger())
        {
            List<WorldPoint> safeTiles = plugin.computeSafeFireBombEscapeTiles();
            for (int i = 0; i < safeTiles.size(); i++)
            {
                WorldPoint safeWp = safeTiles.get(i);
                LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), safeWp);
                if (lp == null)
                {
                    continue;
                }
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                int alpha = i == 0 ? 75 : 35;
                String tag = i == 0 ? "RUN (CTRL)" : "SAFE";
                TormentedDemonOverlayUtils.renderPolygon(graphics, poly, FIRE_BOMB_SAFE_COLOR, 2, alpha);
                Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, tag, 0);
                TormentedDemonOverlayUtils.renderTextLocation(graphics, txt, tag, FIRE_BOMB_SAFE_COLOR, config::textOutline);
            }
        }
    }

    private void renderGroundDrops(Graphics2D graphics)
    {
        if (client.getTopLevelWorldView() == null || plugin.getGroundDrops().isEmpty())
        {
            return;
        }
        for (Map.Entry<WorldPoint, List<TormentedDemonPlugin.GroundDropInfo>> entry : plugin.getGroundDrops().entrySet())
        {
            List<TormentedDemonPlugin.GroundDropInfo> drops = entry.getValue();
            if (drops == null || drops.isEmpty())
            {
                continue;
            }
            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), entry.getKey());
            if (lp == null)
            {
                continue;
            }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);

            // Pick highest-priority drop on this tile for color
            TormentedDemonPlugin.GroundDropInfo topDrop = drops.get(0);
            Color tileColor = resolveDropColor(topDrop.getItemId());
            for (TormentedDemonPlugin.GroundDropInfo info : drops)
            {
                if (TormentedDemonGearClassifier.isJackpotDrop(info.getItemId()))
                {
                    topDrop = info;
                    tileColor = SHIELD_DOWN_GOLD;
                    break;
                }
            }

            TormentedDemonOverlayUtils.renderPolygon(graphics, poly, tileColor, 2, 40);
            String label = topDrop.getQuantity() > 1
                    ? topDrop.getName() + " x" + topDrop.getQuantity()
                    : topDrop.getName();
            Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, label, 15);
            TormentedDemonOverlayUtils.renderTextLocation(graphics, txt, label, tileColor, config::textOutline);
        }
    }

    private Color resolveDropColor(int itemId)
    {
        if (TormentedDemonGearClassifier.isJackpotDrop(itemId))
        {
            return SHIELD_DOWN_GOLD;
        }
        if (TormentedDemonGearClassifier.isSmoulderingConsumable(itemId))
        {
            return Color.ORANGE;
        }
        if (TormentedDemonGearClassifier.isAshesForOffering(itemId))
        {
            return new Color(195, 130, 255);
        }
        if (itemId == ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE
                || itemId == ItemID._4DOSEPRAYERRESTORE
                || itemId == ItemID._2DOSEPRAYERRESTORE)
        {
            return Color.CYAN;
        }
        return new Color(120, 255, 120);
    }

    private void renderCornerWarning(Graphics2D graphics)
    {
        Player local = client.getLocalPlayer();
        if (local == null || local.getLocalLocation() == null)
        {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(client, local.getLocalLocation());
        TormentedDemonOverlayUtils.renderPolygon(graphics, poly, Color.ORANGE, 2, 35);
        String msg = "MOVE TO OPEN FLOOR";
        Point txt = local.getCanvasTextLocation(graphics, msg, local.getLogicalHeight() + 20);
        TormentedDemonOverlayUtils.renderTextLocation(graphics, txt, msg, Color.ORANGE, config::textOutline);
    }

    private void renderTurnOffPrayersReminder(Graphics2D graphics)
    {
        Player local = client.getLocalPlayer();
        if (local == null || local.getOverheadIcon() == null)
        {
            return;
        }
        String text = "TURN OFF PRAYERS";
        Point textLoc = local.getCanvasTextLocation(graphics, text, local.getLogicalHeight() + 40);
        TormentedDemonOverlayUtils.renderTextLocation(graphics, textLoc, text, Color.RED, config::textOutline);
    }
}
