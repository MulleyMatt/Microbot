package net.runelite.client.plugins.microbot.tormenteddemons.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonConfig;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonPlugin;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonState;
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
 * protection prayer and highest-priority fight status tag.
 */
public class TormentedDemonPrayerHelperOverlay extends OverlayPanel
{
    private static final Color INACTIVE_RED = new Color(255, 0, 0, 60);
    private static final Color ACTIVE_GREEN = new Color(0, 255, 0, 45);

    private final Client client;
    private final TormentedDemonPlugin plugin;
    private final TormentedDemonConfig config;
    private final SpriteManager spriteManager;

    @Inject
    public TormentedDemonPrayerHelperOverlay(
            Client client,
            TormentedDemonPlugin plugin,
            TormentedDemonConfig config,
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
        if (!config.prayerHelper() || !plugin.isInActiveFight())
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return null;
        }

        boolean active = client.isPrayerActive(requiredPrayer);
        TormentedDemonState primary = plugin.getPrimaryDemonState();
        String tag = resolveBoxTag(plugin.isPlayerInFireBombDanger(), primary);
        Color textColor = Color.WHITE;
        if (plugin.isPlayerInFireBombDanger())
        {
            textColor = Color.RED;
        }
        else if (primary != null && primary.isShieldDown())
        {
            textColor = new Color(255, 215, 0);
        }
        else if (primary != null && primary.isDefenceless())
        {
            textColor = Color.CYAN;
        }
        else if (primary != null && primary.isPrayerPredicted())
        {
            textColor = Color.YELLOW;
        }

        InfoBoxComponent box = new InfoBoxComponent();
        box.setImage(spriteManager.getSprite(prayerToSpriteId(requiredPrayer), 0));
        box.setBackgroundColor(active ? ACTIVE_GREEN : INACTIVE_RED);
        box.setPreferredSize(new Dimension(44, 44));
        box.setText(tag);
        box.setColor(textColor);
        box.setOutline(true);

        panelComponent.getChildren().add(box);
        panelComponent.setPreferredSize(new Dimension(44, 0));
        return super.render(graphics);
    }

    public static String resolveBoxTag(boolean fireBombDanger, TormentedDemonState primary)
    {
        if (fireBombDanger)
        {
            return "FIRE!";
        }
        if (primary == null)
        {
            return "TD";
        }
        if (primary.isShieldDown())
        {
            return "SHIELD";
        }
        if (primary.isDefenceless())
        {
            return "100%";
        }
        if (primary.isPrayerPredicted())
        {
            return "PRED";
        }
        return primary.getDamageSincePrayerSwap() + "/150";
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
