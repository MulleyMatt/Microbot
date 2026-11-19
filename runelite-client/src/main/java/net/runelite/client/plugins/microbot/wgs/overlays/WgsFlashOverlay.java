package net.runelite.client.plugins.microbot.wgs.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.wgs.WgsBalanceElementalStyle;
import net.runelite.client.plugins.microbot.wgs.WgsBossStage;
import net.runelite.client.plugins.microbot.wgs.WgsConfig;
import net.runelite.client.plugins.microbot.wgs.WgsPlugin;
import net.runelite.client.plugins.microbot.wgs.WgsSurokMechanic;
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
 * Sinusoidal screen flash alert (ALWAYS_ON_TOP) for critical WGS boss mechanics:
 * - Surok Magis: Lit Explosive in inventory/ground, missing Protect from Magic, or Elemental Surge / Warrior spawns.
 * - Balance Elemental: Wrong overhead prayer or Prayer <= 50% in the <50% HP 1-tick phase.
 * - Tormented Demons: Standing on a darkened Fire Bomb AoE tile.
 */
public class WgsFlashOverlay extends Overlay
{
    private final Client client;
    private final WgsPlugin plugin;
    private final WgsConfig config;

    @Inject
    public WgsFlashOverlay(Client client, WgsPlugin plugin, WgsConfig config)
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
        if (!config.screenFlashAlerts() || !plugin.isInAnyBossFight())
        {
            return null;
        }

        Color baseColor = null;
        String bannerText = null;

        WgsBossStage stage = plugin.getActiveStage();
        if (stage == WgsBossStage.SUROK_MAGIS)
        {
            WgsSurokMechanic mechanic = plugin.getActiveSurokMechanic();
            if (mechanic == WgsSurokMechanic.LIT_EXPLOSIVE_INVENTORY
                    || mechanic == WgsSurokMechanic.LIT_EXPLOSIVE_GROUND)
            {
                baseColor = new Color(255, 45, 45);
                bannerText = mechanic.getActionPrompt();
            }
            else if (plugin.getSurokNpc() != null && !client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
            {
                baseColor = new Color(255, 45, 45);
                bannerText = "TURN ON PROTECT FROM MAGIC! (SUROK HITS 40+)";
            }
            else if (mechanic != WgsSurokMechanic.NONE && mechanic != WgsSurokMechanic.POST_FIGHT_TELEORB)
            {
                baseColor = mechanic.getColor();
                bannerText = mechanic.getActionPrompt();
            }
        }
        else if (stage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            WgsBalanceElementalStyle style = plugin.getBalanceStyle();
            WgsBalanceElementalStyle.HpPhase hpPhase = plugin.getBalanceHpPhase();
            int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
            int maxPrayer = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));

            if (style != null && !client.isPrayerActive(style.getProtectionPrayer()))
            {
                baseColor = new Color(255, 45, 45);
                bannerText = "SWITCH TO " + style.getPrayerPrompt() + "! (" + style.getDisplayName() + ")";
            }
            else if (hpPhase.isCritical() && currentPrayer <= maxPrayer / 2)
            {
                baseColor = new Color(255, 165, 0);
                bannerText = "DRINK SUPER RESTORE! KEEP PRAYER > 50% (<50% HP PHASE)";
            }
        }
        else if (stage == WgsBossStage.TORMENTED_DEMONS)
        {
            if (plugin.isPlayerInFireBombDanger())
            {
                baseColor = new Color(255, 70, 20);
                bannerText = "DARKENED FIRE TILE - STEP OFF IMMEDIATELY!";
            }
        }

        if (baseColor == null || bannerText == null)
        {
            return null;
        }

        double pulse = (Math.sin(System.currentTimeMillis() / 130.0) + 1.0) / 2.0;
        int fillAlpha = (int) (18 + pulse * 44);
        int borderAlpha = (int) (125 + pulse * 125);

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
        Font alertFont = prevFont.deriveFont(Font.BOLD, 20.0f);
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
