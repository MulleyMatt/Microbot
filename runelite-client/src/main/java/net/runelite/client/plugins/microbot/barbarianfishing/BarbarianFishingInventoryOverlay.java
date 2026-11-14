package net.runelite.client.plugins.microbot.barbarianfishing;

import net.runelite.api.Point;
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

public class BarbarianFishingInventoryOverlay extends Overlay {

    private final BarbarianFishingPlugin plugin;
    private final BarbarianFishingConfig config;

    @Inject
    public BarbarianFishingInventoryOverlay(BarbarianFishingPlugin plugin, BarbarianFishingConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.highlightInventoryFish()) {
            return null;
        }

        if (Rs2Inventory.isFull()) {
            highlightFish(graphics);
        }

        return null;
    }

    private void highlightFish(Graphics2D graphics) {
        List<Rs2ItemModel> fishItems = Rs2Inventory.items()
                .filter(item -> BarbarianFishingSupplyCalculator.isLeapingFish(item.getId()))
                .collect(Collectors.toList());

        for (Rs2ItemModel item : fishItems) {
            renderItemSlotHighlight(graphics, item, "DROP", new Color(0, 140, 255));
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
