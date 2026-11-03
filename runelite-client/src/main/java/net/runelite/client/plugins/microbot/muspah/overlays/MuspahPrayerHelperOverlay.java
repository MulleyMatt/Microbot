package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.plugins.microbot.muspah.MuspahPlugin;
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
 * modeled after Zulrah Overlay Helper's PrayerHelperOverlay.
 */
public class MuspahPrayerHelperOverlay extends OverlayPanel
{
    private static final Color INACTIVE_RED = new Color(255, 0, 0, 45);
    private static final Color ACTIVE_GREEN = new Color(0, 255, 0, 45);
    private static final Color SMITE_PROMPT_CYAN = new Color(0, 210, 211, 55);

    private final Client client;
    private final MuspahPlugin plugin;
    private final MuspahConfig config;
    private final SpriteManager spriteManager;

    @Inject
    private MuspahPrayerHelperOverlay(
            Client client,
            MuspahPlugin plugin,
            MuspahConfig config,
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
        if (!config.prayerHelper() || plugin.getMuspahNpc() == null || plugin.getMuspahNpc().isDead())
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return null;
        }

        InfoBoxComponent overheadBox = new InfoBoxComponent();
        overheadBox.setImage(spriteManager.getSprite(prayerToSpriteId(requiredPrayer), 0));
        boolean prayerActive = client.isPrayerActive(requiredPrayer);
        overheadBox.setBackgroundColor(prayerActive ? ACTIVE_GREEN : INACTIVE_RED);
        overheadBox.setPreferredSize(new Dimension(40, 40));

        if (plugin.getMagicAttackTicks() > 0 && !prayerActive)
        {
            overheadBox.setText(String.valueOf(plugin.getMagicAttackTicks()));
            overheadBox.setColor(Color.YELLOW);
            overheadBox.setOutline(true);
        }

        panelComponent.getChildren().add(overheadBox);

        if (plugin.shouldPromptSmite())
        {
            InfoBoxComponent smiteBox = new InfoBoxComponent();
            smiteBox.setImage(spriteManager.getSprite(SpriteID.PRAYER_SMITE, 0));
            boolean smiteActive = client.isPrayerActive(Prayer.SMITE);
            smiteBox.setBackgroundColor(smiteActive ? ACTIVE_GREEN : SMITE_PROMPT_CYAN);
            smiteBox.setPreferredSize(new Dimension(40, 40));
            smiteBox.setText("Smite");
            smiteBox.setColor(smiteActive ? Color.GREEN : Color.CYAN);
            smiteBox.setOutline(true);
            panelComponent.getChildren().add(smiteBox);
        }

        int width = plugin.shouldPromptSmite() ? 84 : 40;
        panelComponent.setPreferredSize(new Dimension(width, 0));
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
