package net.runelite.client.plugins.microbot.wgs.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.microbot.wgs.WgsBossStage;
import net.runelite.client.plugins.microbot.wgs.WgsConfig;
import net.runelite.client.plugins.microbot.wgs.WgsPlugin;
import net.runelite.client.plugins.microbot.wgs.WgsSurokMechanic;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Floating Prayer Helper icon box above the chatbox right showing the required
 * protection prayer and attack rotation / mechanic tag for the active WGS boss.
 */
public class WgsPrayerHelperOverlay extends OverlayPanel
{
    private static final Color INACTIVE_RED = new Color(255, 0, 0, 60);
    private static final Color ACTIVE_GREEN = new Color(0, 255, 0, 45);

    private final Client client;
    private final WgsPlugin plugin;
    private final WgsConfig config;
    private final SpriteManager spriteManager;

    @Inject
    public WgsPrayerHelperOverlay(
            Client client,
            WgsPlugin plugin,
            WgsConfig config,
            SpriteManager spriteManager)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.spriteManager = spriteManager;

        setResizable(false);
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        panelComponent.setOrientation(ComponentOrientation.HORIZONTAL);
        panelComponent.setBorder(new Rectangle(0, 0, 0, 0));
        panelComponent.setGap(new Point(2, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.prayerHelper() || !plugin.isInAnyBossFight())
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return null;
        }

        boolean active = client.isPrayerActive(requiredPrayer);
        String boxText = buildBoxText();
        Color textColor = Color.WHITE;

        if (plugin.getActiveStage() == WgsBossStage.BALANCE_ELEMENTAL
                && plugin.getBalanceRemainingInRotation() <= 1)
        {
            textColor = Color.YELLOW;
        }
        else if (plugin.getActiveStage() == WgsBossStage.SUROK_MAGIS
                && plugin.getActiveSurokMechanic() != WgsSurokMechanic.NONE)
        {
            textColor = plugin.getActiveSurokMechanic().getColor();
        }

        InfoBoxComponent overheadBox = new InfoBoxComponent();
        overheadBox.setImage(spriteManager.getSprite(prayerToSpriteId(requiredPrayer), 0));
        overheadBox.setBackgroundColor(active ? ACTIVE_GREEN : INACTIVE_RED);
        overheadBox.setPreferredSize(new Dimension(44, 44));
        overheadBox.setText(boxText);
        overheadBox.setColor(textColor);
        overheadBox.setOutline(true);

        panelComponent.getChildren().add(overheadBox);
        panelComponent.setPreferredSize(new Dimension(44, 0));
        return super.render(graphics);
    }

    private String buildBoxText()
    {
        WgsBossStage stage = plugin.getActiveStage();
        if (stage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            return plugin.getBalanceRemainingInRotation() + " left";
        }
        if (stage == WgsBossStage.SUROK_MAGIS)
        {
            WgsSurokMechanic mech = plugin.getActiveSurokMechanic();
            if (mech != WgsSurokMechanic.NONE && !mech.getWidgetLabel().isEmpty())
            {
                return mech.getWidgetLabel();
            }
            return plugin.getSurokAttacksSinceSpecial() + "/8";
        }
        if (stage == WgsBossStage.TORMENTED_DEMONS)
        {
            WgsPlugin.TormentedDemonState primary = plugin.getPrimaryTormentedDemonState();
            if (primary != null && primary.getShieldDownTicks() > 0)
            {
                return "SHIELD!";
            }
            return "TD";
        }
        return "";
    }

    public static int prayerToSpriteId(Prayer prayer)
    {
        if (prayer == null)
        {
            return -1;
        }
        switch (prayer)
        {
            case PROTECT_FROM_MELEE:
                return SpriteID.PRAYER_PROTECT_FROM_MELEE;
            case PROTECT_FROM_MISSILES:
                return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
            case PROTECT_FROM_MAGIC:
                return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
            default:
                return -1;
        }
    }
}
