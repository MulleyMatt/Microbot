package net.runelite.client.plugins.microbot.redwood;

import com.google.common.collect.ImmutableSet;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class RedwoodSupplyCalculator {

    public static final Set<Integer> AXE_IDS = ImmutableSet.of(
            ItemID.CRYSTAL_AXE,
            ItemID.CRYSTAL_AXE_INACTIVE,
            ItemID.CRYSTAL_AXE_2H,
            ItemID.CRYSTAL_AXE_2H_INACTIVE,
            ItemID.INFERNAL_AXE,
            ItemID.INFERNAL_AXE_EMPTY,
            ItemID.DRAGON_AXE,
            ItemID.DRAGON_AXE_2H,
            ItemID._3A_AXE,
            ItemID.TRAIL_GILDED_AXE,
            ItemID.RUNE_AXE,
            ItemID.RUNE_AXE_2H,
            ItemID.ADAMANT_AXE,
            ItemID.ADAMANT_AXE_2H,
            ItemID.MITHRIL_AXE,
            ItemID.MITHRIL_AXE_2H,
            ItemID.BLACK_AXE,
            ItemID.BLACK_AXE_2H,
            ItemID.STEEL_AXE,
            ItemID.STEEL_AXE_2H,
            ItemID.IRON_AXE,
            ItemID.IRON_AXE_2H,
            ItemID.BRONZE_AXE,
            ItemID.BRONZE_AXE_2H
    );

    public static final Set<Integer> TRADEABLE_AXES = ImmutableSet.of(
            ItemID.DRAGON_AXE,
            ItemID.DRAGON_AXE_2H,
            ItemID.TRAIL_GILDED_AXE,
            ItemID._3A_AXE,
            ItemID.RUNE_AXE,
            ItemID.RUNE_AXE_2H,
            ItemID.ADAMANT_AXE,
            ItemID.ADAMANT_AXE_2H,
            ItemID.MITHRIL_AXE,
            ItemID.MITHRIL_AXE_2H,
            ItemID.BLACK_AXE,
            ItemID.BLACK_AXE_2H,
            ItemID.STEEL_AXE,
            ItemID.STEEL_AXE_2H,
            ItemID.IRON_AXE,
            ItemID.IRON_AXE_2H,
            ItemID.BRONZE_AXE,
            ItemID.BRONZE_AXE_2H
    );

    public static final Set<Integer> SPEC_AXE_IDS = ImmutableSet.of(
            ItemID.DRAGON_AXE,
            ItemID.DRAGON_AXE_2H,
            ItemID.CRYSTAL_AXE,
            ItemID.CRYSTAL_AXE_2H,
            ItemID.INFERNAL_AXE,
            ItemID._3A_AXE,
            ItemID.TRAIL_GILDED_AXE
    );

    public static final Set<Integer> LUMBERJACK_PIECES = ImmutableSet.of(
            ItemID.RAMBLE_LUMBERJACK_HAT,
            ItemID.FORESTRY_LUMBERJACK_HAT,
            ItemID.RAMBLE_LUMBERJACK_TOP,
            ItemID.FORESTRY_LUMBERJACK_TOP,
            ItemID.RAMBLE_LUMBERJACK_LEGS,
            ItemID.FORESTRY_LUMBERJACK_LEGS,
            ItemID.RAMBLE_LUMBERJACK_BOOTS,
            ItemID.FORESTRY_LUMBERJACK_BOOTS
    );

    private final Client client;
    private final ItemManager itemManager;

    @Inject
    public RedwoodSupplyCalculator(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
    }

    public boolean hasAxeEquippedOrInInventory() {
        return getEquippedOrInventoryAxeId() != -1;
    }

    public boolean hasSpecAxeEquipped() {
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && SPEC_AXE_IDS.contains(item.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getEquippedOrInventoryAxeId() {
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && AXE_IDS.contains(item.getId())) {
                    return item.getId();
                }
            }
        }

        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) {
            for (Item item : inv.getItems()) {
                if (item != null && AXE_IDS.contains(item.getId())) {
                    return item.getId();
                }
            }
        }

        return -1;
    }

    public String getAxeName() {
        int axeId = getEquippedOrInventoryAxeId();
        if (axeId == -1) {
            return "None";
        }
        try {
            return itemManager.getItemComposition(axeId).getName();
        } catch (Exception e) {
            return "Axe (" + axeId + ")";
        }
    }

    public int countLumberjackPieces() {
        int count = 0;
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && LUMBERJACK_PIECES.contains(item.getId())) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<Integer> getMissingGearIds() {
        List<Integer> missing = new ArrayList<>();
        if (!hasAxeEquippedOrInInventory()) {
            missing.add(ItemID.DRAGON_AXE);
        }
        return missing;
    }

    @Value
    public static class SupplyCheckResult {
        boolean hasAxe;
        String axeName;
        int lumberjackPieceCount;
    }

    public SupplyCheckResult checkSupplies() {
        return new SupplyCheckResult(
                hasAxeEquippedOrInInventory(),
                getAxeName(),
                countLumberjackPieces()
        );
    }
}
