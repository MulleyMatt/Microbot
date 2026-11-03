package net.runelite.client.plugins.microbot.herbrun;

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

public class HerbRunInventoryOverlay extends Overlay {

    private final HerbRunPlugin plugin;
    private final HerbRunConfig config;

    @Inject
    public HerbRunInventoryOverlay(HerbRunPlugin plugin, HerbRunConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.highlightInventoryItems()) {
            return null;
        }

        HerbRunStep step = plugin.getCurrentStep();
        HerbPatch currentPatch = plugin.getCurrentPatch();

        switch (step) {
            case TELEPORTING:
                if (currentPatch != null) {
                    highlightTeleportItems(graphics, currentPatch);
                }
                break;
            case COMPOSTING:
                highlightCompostItems(graphics);
                break;
            case PLANTING:
                highlightSeedItems(graphics, currentPatch);
                break;
            case NOTING_HERBS:
                highlightGrimyHerbs(graphics);
                break;
            case CLEARING:
                highlightItemById(graphics, ItemID.SPADE, "CLEAR", new Color(240, 60, 60));
                break;
            case WEEDING:
                highlightItemById(graphics, ItemID.RAKE, "RAKE", new Color(240, 200, 30));
                break;
            default:
                break;
        }

        return null;
    }

    private void highlightTeleportItems(Graphics2D graphics, HerbPatch patch) {
        List<Rs2ItemModel> matches = Rs2Inventory.items()
                .filter(item -> patch.matchesTeleportId(item.getId()))
                .collect(Collectors.toList());

        for (Rs2ItemModel item : matches) {
            renderItemSlotHighlight(graphics, item, "TELE", new Color(255, 170, 0));
        }
    }

    private void highlightCompostItems(Graphics2D graphics) {
        List<Rs2ItemModel> matches = Rs2Inventory.items()
                .filter(item -> item.getId() == ItemID.BOTTOMLESS_COMPOST_BUCKET ||
                        item.getId() == ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED ||
                        item.getId() == ItemID.BUCKET_ULTRACOMPOST ||
                        item.getId() == ItemID.BUCKET_SUPERCOMPOST ||
                        (item.getName() != null && (item.getName().equalsIgnoreCase("Ultracompost") || item.getName().equalsIgnoreCase("Supercompost"))))
                .collect(Collectors.toList());

        for (Rs2ItemModel item : matches) {
            renderItemSlotHighlight(graphics, item, "COMPOST", new Color(50, 200, 240));
        }
    }

    private void highlightSeedItems(Graphics2D graphics, HerbPatch patch) {
        int targetSeedId = (plugin.isStrategyPureRanarr() || (patch != null && patch.isDiseaseFree()))
                ? ItemID.RANARR_SEED
                : ItemID.TOADFLAX_SEED;

        List<Rs2ItemModel> matches = Rs2Inventory.items()
                .filter(item -> item.getId() == targetSeedId)
                .collect(Collectors.toList());

        for (Rs2ItemModel item : matches) {
            renderItemSlotHighlight(graphics, item, "PLANT", new Color(60, 240, 80));
        }
    }

    private void highlightGrimyHerbs(Graphics2D graphics) {
        List<Rs2ItemModel> matches = Rs2Inventory.items()
                .filter(HerbRunPlugin::isUnnotedHerb)
                .collect(Collectors.toList());

        for (Rs2ItemModel item : matches) {
            renderItemSlotHighlight(graphics, item, "NOTE", new Color(190, 90, 250));
        }
    }

    private void highlightItemById(Graphics2D graphics, int itemId, String label, Color color) {
        List<Rs2ItemModel> matches = Rs2Inventory.items()
                .filter(item -> item.getId() == itemId)
                .collect(Collectors.toList());

        for (Rs2ItemModel item : matches) {
            renderItemSlotHighlight(graphics, item, label, color);
        }
    }

    private void renderItemSlotHighlight(Graphics2D graphics, Rs2ItemModel item, String label, Color color) {
        Rectangle bounds = Rs2Inventory.itemBounds(item);
        if (bounds == null) {
            return;
        }

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 65);
        graphics.setColor(fillColor);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        graphics.setColor(color);
        graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        Point textPoint = new Point(bounds.x + 2, bounds.y + 12);
        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        OverlayUtil.renderTextLocation(graphics, textPoint, label, Color.WHITE);
    }
}
