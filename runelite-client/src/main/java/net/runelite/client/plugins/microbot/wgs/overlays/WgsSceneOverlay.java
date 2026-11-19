package net.runelite.client.plugins.microbot.wgs.overlays;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.wgs.WgsBalanceElementalStyle;
import net.runelite.client.plugins.microbot.wgs.WgsBossStage;
import net.runelite.client.plugins.microbot.wgs.WgsConfig;
import net.runelite.client.plugins.microbot.wgs.WgsOverlayUtils;
import net.runelite.client.plugins.microbot.wgs.WgsPlugin;
import net.runelite.client.plugins.microbot.wgs.WgsSurokMechanic;
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
import java.util.List;
import java.util.Map;

/**
 * 3D Scene overlays (ABOVE_SCENE) for all three While Guthix Sleeps boss encounters:
 * - Boss 1 (Surok Magis): Surok tile, Strong/Agile Warriors, Lit Explosives, Elemental Surge orbs, Unconscious Surok Teleorb prompt.
 * - Boss 2 (Balance Elemental): Head-colour tile, 4-attack switch counter, unprayed style recommendation, HP phase banner.
 * - Boss 3 (Tormented Demons): Overhead prayer weakness & shield-drop burst window on each demon, Fire Bomb AoE danger tiles & safe escape tiles.
 */
public class WgsSceneOverlay extends Overlay
{
    private static final Color FIRE_BOMB_DANGER_COLOR = new Color(255, 55, 25);
    private static final Color FIRE_BOMB_SAFE_COLOR = Color.GREEN;

    private final Client client;
    private final WgsPlugin plugin;
    private final WgsConfig config;

    @Inject
    public WgsSceneOverlay(Client client, WgsPlugin plugin, WgsConfig config)
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
        WgsBossStage stage = plugin.getActiveStage();
        if (stage == null && !plugin.isPostKillPrayerReminder())
        {
            return null;
        }

        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());

        if (stage == WgsBossStage.SUROK_MAGIS)
        {
            renderSurokScene(graphics);
        }
        else if (stage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            renderBalanceElementalScene(graphics);
        }
        else if (stage == WgsBossStage.TORMENTED_DEMONS)
        {
            renderTormentedDemonsScene(graphics);
        }

        if (plugin.isPostKillPrayerReminder() && stage == null)
        {
            renderPrayerConservation(graphics);
        }

        graphics.setFont(prevFont);
        return null;
    }

    private void renderSurokScene(Graphics2D graphics)
    {
        NPC surok = plugin.getSurokNpc();
        if (surok != null && !surok.isDead())
        {
            LocalPoint lp = surok.getLocalLocation();
            if (lp != null)
            {
                int size = plugin.getNpcSize(surok, 1);
                Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
                boolean protectMage = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
                Color color = protectMage ? new Color(0, 210, 255) : Color.RED;
                WgsOverlayUtils.renderPolygon(graphics, poly, color, config.outlineWidth(), config.fillAlpha());

                String label = protectMage
                        ? "Surok (" + plugin.getSurokAttacksSinceSpecial() + "/8)"
                        : "PRAY MAGIC NOW!";
                Point textLoc = surok.getCanvasTextLocation(graphics, label, surok.getLogicalHeight() + 40);
                WgsOverlayUtils.renderTextLocation(graphics, textLoc, label, color, config::textOutline);
            }
        }

        if (!config.highlightSurokSpecials())
        {
            return;
        }

        for (NPC strong : plugin.getStrongWarriors())
        {
            renderNpcPrompt(graphics, strong, "CAST WEAKEN!", WgsSurokMechanic.STRONG_WARRIOR.getColor());
        }

        for (NPC agile : plugin.getAgileWarriors())
        {
            renderNpcPrompt(graphics, agile, "CAST BIND / ENTANGLE!", WgsSurokMechanic.AGILE_WARRIOR.getColor());
        }

        for (NPC orb : plugin.getSurgeOrbs())
        {
            WgsSurokMechanic mech = WgsSurokMechanic.fromSurgeNpcId(orb.getId());
            if (mech != null)
            {
                renderNpcPrompt(graphics, orb, mech.getActionPrompt(), mech.getColor());
            }
        }

        for (WorldPoint bombWp : plugin.getGroundBombTiles())
        {
            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), bombWp);
            if (lp == null)
            {
                continue;
            }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            Color c = WgsSurokMechanic.LIT_EXPLOSIVE_GROUND.getColor();
            WgsOverlayUtils.renderPolygon(graphics, poly, c, Math.max(2, config.outlineWidth()), 55);
            Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, "TELEGRAB -> ALCH!", 20);
            WgsOverlayUtils.renderTextLocation(graphics, txt, "TELEGRAB -> ALCH!", c, config::textOutline);
        }

        NPC unconscious = plugin.getSurokUnconsciousNpc();
        if (unconscious != null)
        {
            renderNpcPrompt(
                    graphics,
                    unconscious,
                    WgsSurokMechanic.POST_FIGHT_TELEORB.getActionPrompt(),
                    WgsSurokMechanic.POST_FIGHT_TELEORB.getColor()
            );
        }
    }

    private void renderBalanceElementalScene(Graphics2D graphics)
    {
        if (!config.highlightBalanceElemental())
        {
            return;
        }

        NPC boss = plugin.getBalanceNpc();
        WgsBalanceElementalStyle style = plugin.getBalanceStyle();
        if (boss == null || boss.isDead() || style == null)
        {
            return;
        }

        LocalPoint lp = boss.getLocalLocation();
        if (lp == null)
        {
            return;
        }

        int size = plugin.getNpcSize(boss, 3);
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
        WgsOverlayUtils.renderPolygon(graphics, poly, style.getColor(), config.outlineWidth(), config.fillAlpha());

        String header = style.getPrayerPrompt() + " (" + plugin.getBalanceRemainingInRotation() + " left)";
        Point headerLoc = boss.getCanvasTextLocation(graphics, header, boss.getLogicalHeight() + 45);
        WgsOverlayUtils.renderTextLocation(graphics, headerLoc, header, style.getColor(), config::textOutline);

        String attackRec = WgsOverlayUtils.recommendPlayerAttackStyle(
                plugin.getBalanceProtectedStyles(),
                WgsBossStage.BALANCE_ELEMENTAL
        );
        Point recLoc = Perspective.localToCanvas(client, lp, client.getPlane(), 0);
        if (recLoc != null)
        {
            WgsOverlayUtils.renderTextLocation(graphics, recLoc, attackRec, Color.GREEN, config::textOutline);
        }
    }

    private void renderTormentedDemonsScene(Graphics2D graphics)
    {
        if (!config.highlightTormentedDemons())
        {
            return;
        }

        for (WgsPlugin.TormentedDemonState state : plugin.getTormentedDemons().values())
        {
            NPC demon = state.getNpc();
            if (demon == null || demon.isDead())
            {
                continue;
            }
            LocalPoint lp = demon.getLocalLocation();
            if (lp == null)
            {
                continue;
            }

            int size = plugin.getNpcSize(demon, 2);
            Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
            boolean shieldDown = state.getShieldDownTicks() > 0;
            Color tileColor = shieldDown ? Color.GREEN : new Color(255, 130, 50);
            WgsOverlayUtils.renderPolygon(graphics, poly, tileColor, config.outlineWidth(), config.fillAlpha());

            StringBuilder label = new StringBuilder();
            if (state.getRequiredPrayer() != null)
            {
                switch (state.getRequiredPrayer())
                {
                    case PROTECT_FROM_MAGIC:
                        label.append("MAGE | ");
                        break;
                    case PROTECT_FROM_MISSILES:
                        label.append("RANGE | ");
                        break;
                    case PROTECT_FROM_MELEE:
                        label.append("MELEE | ");
                        break;
                    default:
                        break;
                }
            }
            label.append(WgsOverlayUtils.recommendPlayerAttackStyle(state.getProtectedStyles(), WgsBossStage.TORMENTED_DEMONS));
            if (shieldDown)
            {
                label.append(" [SHIELD DOWN ").append(state.getShieldDownTicks()).append("t]");
            }

            String text = label.toString();
            Point textLoc = demon.getCanvasTextLocation(graphics, text, demon.getLogicalHeight() + 35);
            WgsOverlayUtils.renderTextLocation(graphics, textLoc, text, tileColor, config::textOutline);
        }

        // Fire bomb danger tiles
        for (Map.Entry<WorldPoint, Integer> entry : plugin.getFireBombDangerTiles().entrySet())
        {
            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), entry.getKey());
            if (lp == null)
            {
                continue;
            }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            WgsOverlayUtils.renderPolygon(graphics, poly, FIRE_BOMB_DANGER_COLOR, 2, 75);
            Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, "FIRE", 0);
            WgsOverlayUtils.renderTextLocation(graphics, txt, "FIRE", FIRE_BOMB_DANGER_COLOR, config::textOutline);
        }

        // Safe escape tiles when standing in fire bomb danger
        if (plugin.isPlayerInFireBombDanger())
        {
            List<WorldPoint> safeTiles = plugin.computeSafeFireBombEscapeTiles();
            for (WorldPoint safeWp : safeTiles)
            {
                LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), safeWp);
                if (lp == null)
                {
                    continue;
                }
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                WgsOverlayUtils.renderPolygon(graphics, poly, FIRE_BOMB_SAFE_COLOR, 2, 40);
                Point txt = Perspective.getCanvasTextLocation(client, graphics, lp, "SAFE", 0);
                WgsOverlayUtils.renderTextLocation(graphics, txt, "SAFE", FIRE_BOMB_SAFE_COLOR, config::textOutline);
            }
        }
    }

    private void renderNpcPrompt(Graphics2D graphics, NPC npc, String text, Color color)
    {
        if (npc == null || npc.isDead())
        {
            return;
        }
        LocalPoint lp = npc.getLocalLocation();
        if (lp == null)
        {
            return;
        }
        int size = plugin.getNpcSize(npc, 1);
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
        WgsOverlayUtils.renderPolygon(graphics, poly, color, Math.max(2, config.outlineWidth()), 45);
        Point textLoc = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 30);
        WgsOverlayUtils.renderTextLocation(graphics, textLoc, text, color, config::textOutline);
    }

    private void renderPrayerConservation(Graphics2D graphics)
    {
        Player local = client.getLocalPlayer();
        if (local == null || local.getOverheadIcon() == null)
        {
            return;
        }
        String text = "TURN OFF PRAYERS";
        Point textLoc = local.getCanvasTextLocation(graphics, text, local.getLogicalHeight() + 40);
        WgsOverlayUtils.renderTextLocation(graphics, textLoc, text, Color.RED, config::textOutline);
    }
}
