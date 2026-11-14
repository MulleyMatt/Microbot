package net.runelite.client.plugins.microbot.barbarianfishing;

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
import java.util.Set;

@Slf4j
@Singleton
public class BarbarianFishingSupplyCalculator {

    public static final Set<Integer> BARBARIAN_ROD_IDS = ImmutableSet.of(
            ItemID.BRUT_FISHING_ROD,
            ItemID.FISHINGROD_PEARL_BRUT
    );

    public static final Set<Integer> BAIT_IDS = ImmutableSet.of(
            ItemID.FEATHER,
            ItemID.FISHING_BAIT,
            ItemID.HUNTING_STRIPY_BIRD_FEATHER,
            ItemID.BRUT_FISH_CUTS,
            ItemID.BRUT_ROE,
            ItemID.BRUT_CAVIAR
    );

    public static final Set<Integer> LEAPING_FISH_IDS = ImmutableSet.of(
            ItemID.BRUT_SPAWNING_TROUT,
            ItemID.BRUT_SPAWNING_SALMON,
            ItemID.BRUT_STURGEON
    );

    public static final Set<Integer> SPEC_HARPOON_IDS = ImmutableSet.of(
            ItemID.DRAGON_HARPOON,
            ItemID.INFERNAL_HARPOON,
            ItemID.CRYSTAL_HARPOON,
            ItemID.TRAILBLAZER_HARPOON,
            ItemID.TRAILBLAZER_HARPOON_NO_INFERNAL,
            ItemID.TRAILBLAZER_RELOADED_HARPOON,
            ItemID.TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL
    );

    public static final Set<Integer> ANGLER_PIECES = ImmutableSet.of(
            ItemID.TRAWLER_REWARD_HAT,
            ItemID.TRAWLER_REWARD_TOP,
            ItemID.TRAWLER_REWARD_LEGS,
            ItemID.TRAWLER_REWARD_BOOTS,
            ItemID.SPIRIT_ANGLER_HAT,
            ItemID.SPIRIT_ANGLER_TOP,
            ItemID.SPIRIT_ANGLER_LEGS,
            ItemID.SPIRIT_ANGLER_BOOTS
    );

    public static final Set<Integer> TRADEABLE_SUPPLIES = ImmutableSet.of(
            ItemID.FEATHER,
            ItemID.FISHING_BAIT,
            ItemID.HUNTING_STRIPY_BIRD_FEATHER,
            ItemID.DRAGON_HARPOON,
            ItemID.BRUT_ROE,
            ItemID.BRUT_CAVIAR,
            ItemID.BRUT_FISH_CUTS
    );

    private final Client client;
    private final ItemManager itemManager;

    @Inject
    public BarbarianFishingSupplyCalculator(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
    }

    public static boolean isLeapingFish(int itemId) {
        return LEAPING_FISH_IDS.contains(itemId);
    }

    public boolean hasBarbarianRodEquippedOrInInventory() {
        return getEquippedOrInventoryRodId() != -1;
    }

    public int getEquippedOrInventoryRodId() {
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && BARBARIAN_ROD_IDS.contains(item.getId())) {
                    return item.getId();
                }
            }
        }

        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) {
            for (Item item : inv.getItems()) {
                if (item != null && BARBARIAN_ROD_IDS.contains(item.getId())) {
                    return item.getId();
                }
            }
        }

        return -1;
    }

    public String getRodName() {
        int rodId = getEquippedOrInventoryRodId();
        if (rodId == -1) {
            return "None";
        }
        if (rodId == ItemID.FISHINGROD_PEARL_BRUT) {
            return "Pearl barbarian rod";
        }
        try {
            return itemManager.getItemComposition(rodId).getName();
        } catch (Exception e) {
            return "Barbarian rod";
        }
    }

    public boolean hasBaitInInventory() {
        return getTotalBaitCount() > 0;
    }

    public int getTotalBaitCount() {
        int total = 0;
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) {
            for (Item item : inv.getItems()) {
                if (item != null && BAIT_IDS.contains(item.getId())) {
                    total += item.getQuantity();
                }
            }
        }
        return total;
    }

    public int countLeapingFishInInventory() {
        int count = 0;
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) {
            for (Item item : inv.getItems()) {
                if (item != null && LEAPING_FISH_IDS.contains(item.getId())) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean hasSpecHarpoonEquipped() {
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && SPEC_HARPOON_IDS.contains(item.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int countAnglerPieces() {
        int count = 0;
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null) {
            for (Item item : equip.getItems()) {
                if (item != null && ANGLER_PIECES.contains(item.getId())) {
                    count++;
                }
            }
        }
        return count;
    }

    @Value
    public static class SupplyCheckResult {
        boolean hasRod;
        String rodName;
        int baitCount;
        int leapingFishCount;
        int anglerPieceCount;
    }

    public SupplyCheckResult checkSupplies() {
        return new SupplyCheckResult(
                hasBarbarianRodEquippedOrInInventory(),
                getRodName(),
                getTotalBaitCount(),
                countLeapingFishInInventory(),
                countAnglerPieces()
        );
    }
}
