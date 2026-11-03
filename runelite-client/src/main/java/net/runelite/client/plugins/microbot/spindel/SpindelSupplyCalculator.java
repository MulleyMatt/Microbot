package net.runelite.client.plugins.microbot.spindel;

import com.google.common.collect.ImmutableSet;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates required, carried, bank-withdrawable, and GE-purchasable supplies for Spindel trips.
 */
@Slf4j
@Singleton
public class SpindelSupplyCalculator
{
    public static final int[] STAMINA_VARIANT_IDS = new int[]{
            ItemID._4DOSESTAMINA,
            ItemID._3DOSESTAMINA,
            ItemID._2DOSESTAMINA,
            ItemID._1DOSESTAMINA
    };

    public static final int[] LOOTING_BAG_VARIANT_IDS = new int[]{
            ItemID.LOOTING_BAG,
            ItemID.LOOTING_BAG_OPEN
    };

    public static final int[] HOLY_WRENCH_VARIANT_IDS = new int[]{
            ItemID.DEAL_WRENCH,
            ItemID.DEAL_WRENCH_BLESSED
    };

    public static final Set<Integer> NON_CONSUMABLE_SINGLE_ITEMS = ImmutableSet.of(
            ItemID.MM2_ROYAL_SEED_POD,
            ItemID.LOOTING_BAG,
            ItemID.LOOTING_BAG_OPEN,
            ItemID.DEAL_WRENCH,
            ItemID.DEAL_WRENCH_BLESSED
    );

    private final Client client;
    private final SpindelConfig config;

    @Inject
    public SpindelSupplyCalculator(Client client, SpindelConfig config)
    {
        this.client = client;
        this.config = config;
    }

    @Value
    public static class SupplyRequirement
    {
        String displayName;
        int primaryItemId;
        int[] acceptedItemIds;
        int singleTripQuantity;
        boolean tradeable;
        boolean scalesWithBatch;
    }

    @Value
    public static class SupplyStatus
    {
        SupplyRequirement requirement;
        int carriedQuantity;
        int totalOwnedWithBank;
        int requiredForBatch;

        public boolean isReadyInInventory()
        {
            return carriedQuantity >= requirement.getSingleTripQuantity();
        }

        public int getMissingFromInventory()
        {
            return Math.max(0, requirement.getSingleTripQuantity() - carriedQuantity);
        }

        public int getMissingFromTotalOwned()
        {
            return Math.max(0, requiredForBatch - totalOwnedWithBank);
        }
    }

    /**
     * Pure helper: builds the list of configured trip supply requirements from config settings.
     */
    public static List<SupplyRequirement> buildRequirements(SpindelConfig config)
    {
        if (config == null)
        {
            return Collections.emptyList();
        }

        List<SupplyRequirement> list = new ArrayList<>();

        if (config.requireSeedPod())
        {
            list.add(new SupplyRequirement(
                    "Royal seed pod",
                    ItemID.MM2_ROYAL_SEED_POD,
                    new int[]{ItemID.MM2_ROYAL_SEED_POD},
                    1,
                    false,
                    false
            ));
        }

        SpindelConfig.RestorePotionType restoreType = config.restorePotionType();
        int restoreCount = Math.max(0, config.restorePotionCount());
        if (restoreType != null && restoreCount > 0)
        {
            list.add(new SupplyRequirement(
                    restoreType.getDisplayName(),
                    restoreType.getPrimaryItemId(),
                    restoreType.getDoseVariantIds(),
                    restoreCount,
                    true,
                    true
            ));
        }

        SpindelConfig.CombatPotionType combatType = config.combatPotionType();
        int combatCount = Math.max(0, config.combatPotionCount());
        if (combatType != null && combatType != SpindelConfig.CombatPotionType.NONE && combatCount > 0)
        {
            list.add(new SupplyRequirement(
                    combatType.getDisplayName(),
                    combatType.getPrimaryItemId(),
                    combatType.getDoseVariantIds(),
                    combatCount,
                    true,
                    true
            ));
        }

        if (config.includeStaminaPotion())
        {
            list.add(new SupplyRequirement(
                    "Stamina potion(4)",
                    ItemID._4DOSESTAMINA,
                    STAMINA_VARIANT_IDS,
                    1,
                    true,
                    true
            ));
        }

        SpindelConfig.FoodType foodType = config.foodType();
        int foodCount = Math.max(0, config.foodCount());
        if (foodType != null && foodCount > 0)
        {
            list.add(new SupplyRequirement(
                    foodType.getDisplayName(),
                    foodType.getItemId(),
                    new int[]{foodType.getItemId()},
                    foodCount,
                    true,
                    true
            ));
        }

        SpindelConfig.KarambwanType karambwanType = config.karambwanType();
        int karambwanCount = Math.max(0, config.karambwanCount());
        if (karambwanType != null && karambwanType != SpindelConfig.KarambwanType.NONE && karambwanCount > 0)
        {
            list.add(new SupplyRequirement(
                    karambwanType.getDisplayName(),
                    karambwanType.getItemId(),
                    new int[]{karambwanType.getItemId()},
                    karambwanCount,
                    true,
                    true
            ));
        }

        if (config.includeLootingBag())
        {
            list.add(new SupplyRequirement(
                    "Looting bag",
                    ItemID.LOOTING_BAG,
                    LOOTING_BAG_VARIANT_IDS,
                    1,
                    false,
                    false
            ));
        }

        if (config.includeHolyWrench())
        {
            list.add(new SupplyRequirement(
                    "Holy wrench",
                    ItemID.DEAL_WRENCH,
                    HOLY_WRENCH_VARIANT_IDS,
                    1,
                    false,
                    false
            ));
        }

        return list;
    }

    /**
     * Evaluates current statuses for all configured supply requirements.
     */
    public List<SupplyStatus> evaluateSupplies()
    {
        List<SupplyRequirement> requirements = buildRequirements(config);
        if (requirements.isEmpty())
        {
            return Collections.emptyList();
        }

        int batch = Math.max(1, config.batchCount());
        ItemContainer inv = client != null ? client.getItemContainer(InventoryID.INVENTORY) : null;
        ItemContainer equip = client != null ? client.getItemContainer(InventoryID.EQUIPMENT) : null;
        ItemContainer bank = client != null ? client.getItemContainer(InventoryID.BANK) : null;
        boolean bankMirrorAvailable = Rs2Bank.hasBankMirrorSnapshot();

        List<SupplyStatus> statuses = new ArrayList<>(requirements.size());
        for (SupplyRequirement req : requirements)
        {
            int carried = 0;
            int bankCount = 0;
            for (int id : req.getAcceptedItemIds())
            {
                if (inv != null)
                {
                    carried += countItemInContainer(inv, id);
                }
                if (equip != null)
                {
                    carried += countItemInContainer(equip, id);
                }
                if (bank != null)
                {
                    bankCount += countItemInContainer(bank, id);
                }
                else if (bankMirrorAvailable)
                {
                    bankCount += Rs2Bank.count(id);
                }
            }

            int requiredTotal = req.isScalesWithBatch()
                    ? req.getSingleTripQuantity() * batch
                    : req.getSingleTripQuantity();

            statuses.add(new SupplyStatus(req, carried, carried + bankCount, requiredTotal));
        }
        return statuses;
    }

    /**
     * Returns all item IDs (including dose variants) for supplies that the player still needs
     * to withdraw from the Bank into their Inventory for 1 trip.
     */
    public Set<Integer> getMissingBankItemIds()
    {
        Set<Integer> missingIds = new LinkedHashSet<>();
        for (SupplyStatus status : evaluateSupplies())
        {
            if (!status.isReadyInInventory())
            {
                for (int acceptedId : status.getRequirement().getAcceptedItemIds())
                {
                    missingIds.add(acceptedId);
                }
            }
        }
        return missingIds;
    }

    /**
     * Returns primary tradeable item IDs for supplies where the player's total owned quantity
     * (Inventory + Equipment + Bank) is less than the required batch quantity.
     */
    public Set<Integer> getMissingTradeableItemIds()
    {
        Set<Integer> missingTradeable = new LinkedHashSet<>();
        for (SupplyStatus status : evaluateSupplies())
        {
            SupplyRequirement req = status.getRequirement();
            if (req.isTradeable() && status.getMissingFromTotalOwned() > 0)
            {
                missingTradeable.add(req.getPrimaryItemId());
            }
        }
        return missingTradeable;
    }

    /**
     * Pure helper: calculates missing quantities given an explicit map of owned item counts.
     */
    public static Map<Integer, Integer> calculateMissingQuantities(
            Map<Integer, Integer> requiredSupplies,
            Map<Integer, Integer> ownedCounts,
            int batchCount)
    {
        if (requiredSupplies == null || requiredSupplies.isEmpty())
        {
            return Collections.emptyMap();
        }
        int effectiveBatch = Math.max(1, batchCount);
        Map<Integer, Integer> missing = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : requiredSupplies.entrySet())
        {
            int itemId = entry.getKey();
            int baseQty = Math.max(0, entry.getValue());
            int requiredQty = NON_CONSUMABLE_SINGLE_ITEMS.contains(itemId)
                    ? baseQty
                    : baseQty * effectiveBatch;
            int owned = ownedCounts != null ? ownedCounts.getOrDefault(itemId, 0) : 0;
            if (owned < requiredQty)
            {
                missing.put(itemId, requiredQty - owned);
            }
        }
        return missing;
    }

    public boolean hasRoyalSeedPodInInventory()
    {
        if (client == null)
        {
            return false;
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        return inv != null && countItemInContainer(inv, ItemID.MM2_ROYAL_SEED_POD) > 0;
    }

    public static int countItemInContainer(ItemContainer container, int itemId)
    {
        if (container == null || container.getItems() == null)
        {
            return 0;
        }
        int count = 0;
        for (Item item : container.getItems())
        {
            if (item != null && item.getId() == itemId)
            {
                count += Math.max(1, item.getQuantity());
            }
        }
        return count;
    }
}
