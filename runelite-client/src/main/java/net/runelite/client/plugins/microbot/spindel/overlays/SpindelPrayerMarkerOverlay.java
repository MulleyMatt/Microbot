package net.runelite.client.plugins.microbot.spindel.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.spindel.SpindelConfig;
import net.runelite.client.plugins.microbot.spindel.SpindelOverlayUtils;
import net.runelite.client.plugins.microbot.spindel.SpindelPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Highlights the required overhead prayer icon directly inside the Prayer book widget tab.
 */
public class SpindelPrayerMarkerOverlay extends Overlay
{
    private static final int VARCLIENT_INVENTORY_TAB = 171;
    private static final int PRAYER_TAB_INDEX = 5;

    private final Client client;
    private final SpindelPlugin plugin;
    private final SpindelConfig config;

    @Inject
    private SpindelPrayerMarkerOverlay(Client client, SpindelPlugin plugin, SpindelConfig config)
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
        if (!config.prayerMarker() || plugin.getSpindelNpc() == null || plugin.getSpindelNpc().isDead())
        {
            return null;
        }

        if (client.getVarcIntValue(VARCLIENT_INVENTORY_TAB) != PRAYER_TAB_INDEX)
        {
            return null;
        }

        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return null;
        }

        int componentId = prayerToComponentId(requiredPrayer);
        if (componentId == -1)
        {
            return null;
        }

        Widget prayerWidget = client.getWidget(componentId);
        if (prayerWidget != null && !prayerWidget.isHidden())
        {
            boolean active = client.isPrayerActive(requiredPrayer);
            Color color = active ? Color.GREEN : Color.RED;
            SpindelOverlayUtils.renderWidgetPolygon(graphics, prayerWidget, color, true, !active);
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
