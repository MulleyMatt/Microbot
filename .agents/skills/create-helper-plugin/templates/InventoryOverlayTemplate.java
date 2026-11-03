package net.runelite.client.plugins.microbot.createhelper.templates;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;

/**
 * Reusable template for highlighting specific items, teleports, potions, or gear in the inventory.
 */
public class InventoryOverlayTemplate extends Overlay {
    private final Client client;

    public InventoryOverlayTemplate(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    public void highlightInventoryItems(Graphics2D graphics, Set<Integer> targetItemIds, Color highlightColor) {
        Widget inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);
        if (inventoryWidget == null || inventoryWidget.isHidden()) {
            return;
        }

        Widget[] items = inventoryWidget.getChildren();
        if (items == null) {
            return;
        }

        for (Widget item : items) {
            if (item.getItemId() > 0 && targetItemIds.contains(item.getItemId())) {
                Rectangle bounds = item.getBounds();
                if (bounds != null) {
                    graphics.setColor(highlightColor);
                    graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                    graphics.setColor(new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 40));
                    graphics.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                }
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        return null;
    }
}
