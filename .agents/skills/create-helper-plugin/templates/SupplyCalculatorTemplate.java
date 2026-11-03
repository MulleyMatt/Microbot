package net.runelite.client.plugins.microbot.createhelper.templates;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable template for calculating owned vs missing items across Inventory, Equipment, and Bank.
 */
public class SupplyCalculatorTemplate {

    public static Map<Integer, Integer> calculateMissing(
            Client client,
            Map<Integer, Integer> requiredSupplies,
            int batchCount
    ) {
        Map<Integer, Integer> missing = new LinkedHashMap<>();
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);

        for (Map.Entry<Integer, Integer> entry : requiredSupplies.entrySet()) {
            int itemId = entry.getKey();
            int requiredQty = entry.getValue() * batchCount;

            int ownedQty = 0;
            if (inv != null) {
                ownedQty += countItem(inv, itemId);
            }
            if (equip != null) {
                ownedQty += countItem(equip, itemId);
            }
            if (Rs2Bank.isOpen() || Rs2Bank.getBankItemCache() != null) {
                ownedQty += Rs2Bank.count(itemId);
            }

            if (ownedQty < requiredQty) {
                missing.put(itemId, requiredQty - ownedQty);
            }
        }

        return missing;
    }

    private static int countItem(ItemContainer container, int itemId) {
        int count = 0;
        for (Item item : container.getItems()) {
            if (item != null && item.getId() == itemId) {
                count += Math.max(1, item.getQuantity());
            }
        }
        return count;
    }
}
