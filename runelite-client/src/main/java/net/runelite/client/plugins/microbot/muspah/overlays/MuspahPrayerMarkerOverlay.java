package net.runelite.client.plugins.microbot.muspah.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.muspah.MuspahConfig;
import net.runelite.client.plugins.microbot.muspah.MuspahOverlayUtils;
import net.runelite.client.plugins.microbot.muspah.MuspahPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Highlights the required prayer icon directly inside the Prayer book widget tab,
 * modeled after Zulrah Overlay Helper's PrayerMarkerOverlay.
 */
public class MuspahPrayerMarkerOverlay extends Overlay
{
    private static final int VARCLIENT_INVENTORY_TAB = 171;
    private static final int PRAYER_TAB_INDEX = 5;

    private final Client client;
    private final MuspahPlugin plugin;
    private final MuspahConfig config;

    @Inject
    private MuspahPrayerMarkerOverlay(Client client, MuspahPlugin plugin, MuspahConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.prayerMarker() || plugin.getMuspahNpc() == null || plugin.getMuspahNpc().isDead())
        {
            return null;
        }

        if (client.getVarcIntValue(VARCLIENT_INVENTORY_TAB) != PRAYER_TAB_INDEX)
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer != null)
        {
            int componentId = prayerToComponentId(requiredPrayer);
            if (componentId != -1)
            {
                Widget prayerWidget = client.getWidget(componentId);
                if (prayerWidget != null && !prayerWidget.isHidden())
                {
                    boolean active = client.isPrayerActive(requiredPrayer);
                    Color color = active ? Color.GREEN : Color.RED;
                    boolean urgentFlash = !active && plugin.getMagicAttackTicks() > 0;
                    MuspahOverlayUtils.renderWidgetPolygon(graphics, prayerWidget, color, true, urgentFlash);
                }
            }
        }

        if (plugin.shouldPromptSmite())
        {
            Widget smiteWidget = client.getWidget(InterfaceID.Prayerbook.PRAYER18);
            if (smiteWidget != null && !smiteWidget.isHidden())
            {
                boolean smiteActive = client.isPrayerActive(Prayer.SMITE);
                Color smiteColor = smiteActive ? Color.GREEN : Color.CYAN;
                MuspahOverlayUtils.renderWidgetPolygon(graphics, smiteWidget, smiteColor, true, false);
            }
        }

        return null;
    }

    public static int prayerToComponentId(Prayer prayer)
    {
        if (prayer == null)
        {
            return -1;
        }
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                return InterfaceID.Prayerbook.PRAYER13;
            case PROTECT_FROM_MISSILES:
                return InterfaceID.Prayerbook.PRAYER14;
            case PROTECT_FROM_MELEE:
                return InterfaceID.Prayerbook.PRAYER15;
            case SMITE:
                return InterfaceID.Prayerbook.PRAYER18;
            default:
                return -1;
        }
    }
}
