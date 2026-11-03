package net.runelite.client.plugins.microbot.spindel.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.microbot.spindel.SpindelConfig;
import net.runelite.client.plugins.microbot.spindel.SpindelPlugin;
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
 * Displays the floating Prayer Helper icon box above the chatbox right,
 * showing the required overhead prayer (Protect from Missiles / Magic) and
 * the remaining attacks (1..8) before Spindel switches style.
 */
public class SpindelPrayerHelperOverlay extends OverlayPanel
{
    private static final Color INACTIVE_RED = new Color(255, 0, 0, 55);
    private static final Color ACTIVE_GREEN = new Color(0, 255, 0, 45);

    private final Client client;
    private final SpindelPlugin plugin;
    private final SpindelConfig config;
    private final SpriteManager spriteManager;

    @Inject
    private SpindelPrayerHelperOverlay(
            Client client,
            SpindelPlugin plugin,
            SpindelConfig config,
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
        if (!config.prayerHelper() || plugin.getSpindelNpc() == null || plugin.getSpindelNpc().isDead())
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return null;
        }

        int remaining = plugin.getRemainingAttacksInPhase();
        boolean prayerActive = client.isPrayerActive(requiredPrayer);

        InfoBoxComponent overheadBox = new InfoBoxComponent();
        overheadBox.setImage(spriteManager.getSprite(prayerToSpriteId(requiredPrayer), 0));
        overheadBox.setBackgroundColor(prayerActive ? ACTIVE_GREEN : INACTIVE_RED);
        overheadBox.setPreferredSize(new Dimension(42, 42));
        overheadBox.setText( remaining + " left");
        overheadBox.setColor(remaining <= 2 ? Color.YELLOW : Color.WHITE);
        overheadBox.setOutline(true);

        panelComponent.getChildren().add(overheadBox);
        panelComponent.setPreferredSize(new Dimension(42, 0));
        return super.render(graphics);
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
            case SMITE:
                return SpriteID.PRAYER_SMITE;
            default:
                return -1;
        }
    }
}
