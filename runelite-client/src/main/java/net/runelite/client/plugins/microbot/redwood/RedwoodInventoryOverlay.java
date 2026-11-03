package net.runelite.client.plugins.microbot.redwood;

import net.runelite.api.Point;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

public class RedwoodInventoryOverlay extends Overlay {

    private final RedwoodPlugin plugin;
    private final RedwoodConfig config;

    @Inject
    public RedwoodInventoryOverlay(RedwoodPlugin plugin, RedwoodConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.highlightInventoryLogs()) {
            return null;
        }

        boolean invFull = Rs2Inventory.isFull();
        if (invFull) {
            highlightLogs(graphics);
        }

        return null;
    }

    private void highlightLogs(Graphics2D graphics) {
        List<Rs2ItemModel> logItems = Rs2Inventory.items()
                .filter(item -> item.getId() == ItemID.REDWOOD_LOGS)
                .collect(Collectors.toList());

        for (Rs2ItemModel item : logItems) {
            renderItemSlotHighlight(graphics, item, "BANK", new Color(0, 140, 255));
        }
    }

    private void renderItemSlotHighlight(Graphics2D graphics, Rs2ItemModel item, String label, Color color) {
        Rectangle bounds = Rs2Inventory.itemBounds(item);
        if (bounds == null) {
            return;
        }

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
        graphics.setColor(fillColor);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        graphics.setColor(color);
        graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        if (label != null) {
            Point textPoint = new Point(bounds.x + 2, bounds.y + 12);
            graphics.setFont(new Font("Arial", Font.BOLD, 10));
            OverlayUtil.renderTextLocation(graphics, textPoint, label, Color.WHITE);
        }
    }
}
