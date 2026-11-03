package net.runelite.client.plugins.microbot.herbrun;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class HerbRunSupplyCalculator {

    @Getter
    @RequiredArgsConstructor
    public static class SupplyItem {
        private final String name;
        private final int itemId;
        private final int requiredQuantity;
        private final int currentPossession; // inventory + equipped
        private final int bankQuantity;
        private final boolean tradeable;
        private final boolean equipmentBoost;
        private final String category; // "Gear", "Tool", "Seed", "Teleport", "Compost"

        public int getMissingForRun() {
            return Math.max(0, requiredQuantity - currentPossession);
        }

        public boolean isMissingFromInventory() {
            return getMissingForRun() > 0;
        }

        public boolean canWithdrawFromBank() {
            return isMissingFromInventory() && bankQuantity >= getMissingForRun();
        }

        public boolean canPartiallyWithdrawFromBank() {
            return isMissingFromInventory() && bankQuantity > 0 && bankQuantity < getMissingForRun();
        }

        public boolean hasInBank() {
            return bankQuantity > 0;
        }

        public boolean needsToBuyFromGe() {
            return tradeable && (currentPossession + bankQuantity < requiredQuantity);
        }

        public int getGeBuyQuantity(int runsToPurchase) {
            // Reusable items (tools, gear, bottomless bucket) are needed once, not once per run
            boolean reusable = "Tool".equals(category) || "Gear".equals(category)
                    || itemId == ItemID.BOTTOMLESS_COMPOST_BUCKET;
            int totalNeeded = reusable ? requiredQuantity : requiredQuantity * runsToPurchase;
            int totalOwned = currentPossession + bankQuantity;
            return Math.max(0, totalNeeded - totalOwned);
        }
    }

    private final Client client;
    private final HerbRunConfig config;
    private final ItemManager itemManager;

    @Inject
    public HerbRunSupplyCalculator(Client client, HerbRunConfig config, ItemManager itemManager) {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
    }

    /**
     * Resolves the list of active enabled patches based on user configuration and unlock status.
     */
    public List<HerbPatch> getEnabledPatches() {
        return HerbPatch.getOptimalRoute().stream()
                .filter(this::isPatchConfigEnabled)
                .filter(patch -> !config.autoDetectUnlocks() || patch.isUnlocked(client))
                .collect(Collectors.toList());
    }

    private boolean isPatchConfigEnabled(HerbPatch patch) {
        switch (patch) {
            case TROLL_STRONGHOLD:
                return config.trollStronghold();
            case WEISS:
                return config.weiss();
            case HOSIDIUS:
                return config.hosidius();
            case FARMING_GUILD:
                return config.farmingGuild();
            case CATHERBY:
                return config.catherby();
            case ARDOUGNE:
                return config.ardougne();
            case FALADOR:
                return config.falador();
            case PORT_PHASMATYS:
                return config.portPhasmatys();
            case VARLAMORE:
                return config.varlamore();
            case HARMONY:
                return config.harmony();
            default:
                return true;
        }
    }

    /**
     * Calculates the complete supply checklist for 1 run and identifies any missing items.
     */
    public List<SupplyItem> calculateSupplies() {
        List<SupplyItem> checklist = new ArrayList<>();
        List<HerbPatch> enabledPatches = getEnabledPatches();

        if (enabledPatches.isEmpty()) {
            return checklist;
        }

        // 1. Crucial Yield Boosts (Farming Cape & Magic Secateurs)
        addFarmingCapeCheck(checklist);
        addMagicSecateursCheck(checklist);

        // 2. Core Farming Tools
        addToolCheck(checklist, "Spade", ItemID.SPADE);
        addToolCheck(checklist, "Seed dibber", ItemID.DIBBER);

        // 3. Compost Boost (Bottomless Compost Bucket or Ultracompost)
        addCompostCheck(checklist, enabledPatches.size());

        // 4. Seeds based on Selected Strategy
        addSeedsCheck(checklist, enabledPatches);

        // 5. Teleport Items for Enabled Patches
        addTeleportsCheck(checklist, enabledPatches);

        return checklist;
    }

    private void addFarmingCapeCheck(List<SupplyItem> list) {
        boolean wearing = Rs2Equipment.isWearing(ItemID.SKILLCAPE_FARMING) || Rs2Equipment.isWearing(ItemID.SKILLCAPE_FARMING_TRIMMED);
        int inv = Rs2Inventory.itemQuantity(ItemID.SKILLCAPE_FARMING) + Rs2Inventory.itemQuantity(ItemID.SKILLCAPE_FARMING_TRIMMED);
        int bank = getBankItemCount(ItemID.SKILLCAPE_FARMING) + getBankItemCount(ItemID.SKILLCAPE_FARMING_TRIMMED);
        int owned = (wearing ? 1 : 0) + inv;

        list.add(new SupplyItem(
                "Farming cape",
                ItemID.SKILLCAPE_FARMING,
                1,
                owned,
                bank,
                false,
                true,
                "Gear"
        ));
    }

    private void addMagicSecateursCheck(List<SupplyItem> list) {
        boolean wearing = Rs2Equipment.isWearing(ItemID.FAIRY_ENCHANTED_SECATEURS);
        int inv = Rs2Inventory.itemQuantity(ItemID.FAIRY_ENCHANTED_SECATEURS);
        int bank = getBankItemCount(ItemID.FAIRY_ENCHANTED_SECATEURS);
        int owned = (wearing ? 1 : 0) + inv;

        list.add(new SupplyItem(
                "Magic secateurs",
                ItemID.FAIRY_ENCHANTED_SECATEURS,
                1,
                owned,
                bank,
                false,
                true,
                "Gear"
        ));
    }

    private void addToolCheck(List<SupplyItem> list, String toolName, int toolId) {
        int inv = Rs2Inventory.itemQuantity(toolId);
        int bank = getBankItemCount(toolId);
        list.add(new SupplyItem(
                toolName,
                toolId,
                1,
                inv,
                bank,
                true,
                false,
                "Tool"
        ));
    }

    private void addCompostCheck(List<SupplyItem> list, int patchCount) {
        boolean hasBucket = Rs2Inventory.hasItem(ItemID.BOTTOMLESS_COMPOST_BUCKET) ||
                Rs2Inventory.hasItem(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
        if (hasBucket) {
            list.add(new SupplyItem(
                    "Bottomless compost bucket",
                    ItemID.BOTTOMLESS_COMPOST_BUCKET,
                    1,
                    1,
                    0,
                    true,
                    false,
                    "Compost"
            ));
            return;
        }

        int bucketInBank = getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET) + getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
        if (bucketInBank > 0) {
            list.add(new SupplyItem(
                    "Bottomless compost bucket",
                    ItemID.BOTTOMLESS_COMPOST_BUCKET,
                    1,
                    0,
                    bucketInBank,
                    true,
                    false,
                    "Compost"
            ));
            return;
        }

        // Standard Ultracompost (with Supercompost fallback if player owns supercompost instead)
        int invCompost = Rs2Inventory.items()
                .filter(item -> item.getId() == ItemID.BUCKET_ULTRACOMPOST ||
                        (item.getName() != null && item.getName().equalsIgnoreCase("Ultracompost")))
                .mapToInt(Rs2ItemModel::getQuantity)
                .sum();
        int bankCompost = getBankItemCount(ItemID.BUCKET_ULTRACOMPOST);
        if (invCompost == 0 && bankCompost == 0) {
            int invSuper = Rs2Inventory.items()
                    .filter(item -> item.getId() == ItemID.BUCKET_SUPERCOMPOST ||
                            (item.getName() != null && item.getName().equalsIgnoreCase("Supercompost")))
                    .mapToInt(Rs2ItemModel::getQuantity)
                    .sum();
            int bankSuper = getBankItemCount(ItemID.BUCKET_SUPERCOMPOST);
            if (invSuper > 0 || bankSuper > 0) {
                list.add(new SupplyItem(
                        "Supercompost",
                        ItemID.BUCKET_SUPERCOMPOST,
                        patchCount,
                        invSuper,
                        bankSuper,
                        true,
                        false,
                        "Compost"
                ));
                return;
            }
        }
        list.add(new SupplyItem(
                "Ultracompost",
                ItemID.BUCKET_ULTRACOMPOST,
                patchCount,
                invCompost,
                bankCompost,
                true,
                false,
                "Compost"
        ));
    }

    private void addSeedsCheck(List<SupplyItem> list, List<HerbPatch> enabledPatches) {
        if (config.strategy() == HerbRunStrategy.PURE_RANARR) {
            // Strategy A: Ranarr across all patches
            int needed = enabledPatches.size();
            int inv = Rs2Inventory.itemQuantity(ItemID.RANARR_SEED);
            int bank = getBankItemCount(ItemID.RANARR_SEED);
            list.add(new SupplyItem(
                    "Ranarr seed",
                    ItemID.RANARR_SEED,
                    needed,
                    inv,
                    bank,
                    true,
                    false,
                    "Seed"
            ));
        } else {
            // Strategy B: Split Method (Ranarr for disease-free, Toadflax for standard)
            long diseaseFreeCount = enabledPatches.stream().filter(HerbPatch::isDiseaseFree).count();
            long standardCount = enabledPatches.size() - diseaseFreeCount;

            if (diseaseFreeCount > 0) {
                int invRanarr = Rs2Inventory.itemQuantity(ItemID.RANARR_SEED);
                int bankRanarr = getBankItemCount(ItemID.RANARR_SEED);
                list.add(new SupplyItem(
                        "Ranarr seed (Disease-Free)",
                        ItemID.RANARR_SEED,
                        (int) diseaseFreeCount,
                        invRanarr,
                        bankRanarr,
                        true,
                        false,
                        "Seed"
                ));
            }

            if (standardCount > 0) {
                int invToadflax = Rs2Inventory.itemQuantity(ItemID.TOADFLAX_SEED);
                int bankToadflax = getBankItemCount(ItemID.TOADFLAX_SEED);
                list.add(new SupplyItem(
                        "Toadflax seed (Standard)",
                        ItemID.TOADFLAX_SEED,
                        (int) standardCount,
                        invToadflax,
                        bankToadflax,
                        true,
                        false,
                        "Seed"
                ));
            }
        }
    }

    public boolean hasInfiniteHouseTeleport() {
        return Rs2Equipment.isWearing(ItemID.SKILLCAPE_CONSTRUCTION) ||
                Rs2Equipment.isWearing(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED) ||
                Rs2Equipment.isWearing(ItemID.SKILLCAPE_MAX) ||
                Rs2Equipment.isWearing(ItemID.SKILLCAPE_MAX_WORN) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_CONSTRUCTION) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_MAX) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_MAX_WORN);
    }

    private void addTeleportsCheck(List<SupplyItem> list, List<HerbPatch> enabledPatches) {
        boolean hasInfiniteHouse = hasInfiniteHouseTeleport();
        int invHouseTabs = Rs2Inventory.itemQuantity(ItemID.POH_TABLET_TELEPORTTOHOUSE);
        int remainingBankHouseTabs = getBankItemCount(ItemID.POH_TABLET_TELEPORTTOHOUSE);
        int runsToPurchase = config != null ? config.runsToPurchase() : 1;

        for (HerbPatch patch : enabledPatches) {
            boolean usesHouseTeleport = patch.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE);
            if (usesHouseTeleport) {
                int currentPossession;
                if (hasInfiniteHouse) {
                    currentPossession = 1;
                } else if (invHouseTabs > 0) {
                    currentPossession = 1;
                    invHouseTabs--;
                } else {
                    currentPossession = 0;
                }

                int allocatedBank = Math.min(remainingBankHouseTabs, runsToPurchase);
                remainingBankHouseTabs = Math.max(0, remainingBankHouseTabs - allocatedBank);

                list.add(new SupplyItem(
                        patch.getPrimaryTeleportName() + " (" + patch.getPatchName() + ")",
                        patch.getPrimaryTeleportItemId(),
                        1,
                        currentPossession,
                        allocatedBank,
                        true,
                        false,
                        "Teleport"
                ));
                continue;
            }

            boolean hasInInvOrEquipped = false;
            int countInInv = 0;
            int countInBank = 0;

            for (int id : patch.getAlternateTeleportItemIds()) {
                if (Rs2Equipment.isWearing(id) || Rs2Inventory.hasItem(id)) {
                    hasInInvOrEquipped = true;
                    countInInv += Math.max(1, Rs2Inventory.itemQuantity(id));
                }
                countInBank += getBankItemCount(id);
            }

            list.add(new SupplyItem(
                    patch.getPrimaryTeleportName() + " (" + patch.getPatchName() + ")",
                    patch.getPrimaryTeleportItemId(),
                    1,
                    hasInInvOrEquipped ? Math.max(1, countInInv) : 0,
                    countInBank,
                    isItemTradeable(patch.getPrimaryTeleportItemId()),
                    false,
                    "Teleport"
            ));
        }
    }

    private boolean isItemTradeable(int itemId) {
        switch (itemId) {
            case ItemID.STRONGHOLD_TELEPORT_BASALT:
            case ItemID.WEISS_TELEPORT_BASALT:
            case ItemID.POH_TABLET_TELEPORTTOHOUSE:
            case ItemID.POH_TABLET_KOURENDTELEPORT:
            case ItemID.NZONE_TELETAB_KOUREND:
            case ItemID.LUNAR_TABLET_CATHERBY_TELEPORT:
            case ItemID.POH_TABLET_CAMELOTTELEPORT:
            case ItemID.POH_TABLET_ARDOUGNETELEPORT:
            case ItemID.POH_TABLET_FALADORTELEPORT:
            case ItemID.POH_TABLET_FORTISTELEPORT:
            case ItemID.SPADE:
            case ItemID.DIBBER:
            case ItemID.BUCKET_ULTRACOMPOST:
            case ItemID.RANARR_SEED:
            case ItemID.TOADFLAX_SEED:
                return true;
            default:
                return false;
        }
    }

    public int getBankItemCount(int itemId) {
        int canonicalId = itemManager != null ? itemManager.canonicalize(itemId) : itemId;
        if (client != null) {
            ItemContainer bank = client.getItemContainer(InventoryID.BANK);
            if (bank != null) {
                int count = bank.count(canonicalId);
                if (count > 0) {
                    return count;
                }
                if (canonicalId != itemId) {
                    count = bank.count(itemId);
                    if (count > 0) {
                        return count;
                    }
                }
            }
        }
        if (!Rs2Bank.hasBankMirrorSnapshot()) {
            return 0;
        }
        try {
            return Rs2Bank.bankItems().stream()
                    .filter(item -> item.getId() == canonicalId || item.getId() == itemId)
                    .mapToInt(Rs2ItemModel::getQuantity)
                    .sum();
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Resolves the set of acceptable item IDs for missing supplies that are currently in the bank.
     * When the bank filter is active, only these items will remain visible.
     */
    public Set<Integer> getMissingBankItemIds() {
        Set<Integer> itemIds = new HashSet<>();
        List<HerbPatch> enabledPatches = getEnabledPatches();
        if (enabledPatches.isEmpty()) {
            return itemIds;
        }

        // 1. Farming Cape
        boolean hasFarmingCape = Rs2Equipment.isWearing(ItemID.SKILLCAPE_FARMING) ||
                Rs2Equipment.isWearing(ItemID.SKILLCAPE_FARMING_TRIMMED) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_FARMING) ||
                Rs2Inventory.hasItem(ItemID.SKILLCAPE_FARMING_TRIMMED);
        if (!hasFarmingCape) {
            if (getBankItemCount(ItemID.SKILLCAPE_FARMING) > 0) itemIds.add(ItemID.SKILLCAPE_FARMING);
            if (getBankItemCount(ItemID.SKILLCAPE_FARMING_TRIMMED) > 0) itemIds.add(ItemID.SKILLCAPE_FARMING_TRIMMED);
        }

        // 2. Magic Secateurs
        boolean hasSecateurs = Rs2Equipment.isWearing(ItemID.FAIRY_ENCHANTED_SECATEURS) ||
                Rs2Inventory.hasItem(ItemID.FAIRY_ENCHANTED_SECATEURS);
        if (!hasSecateurs) {
            if (getBankItemCount(ItemID.FAIRY_ENCHANTED_SECATEURS) > 0) itemIds.add(ItemID.FAIRY_ENCHANTED_SECATEURS);
        }

        // 3. Tools
        if (!Rs2Inventory.hasItem(ItemID.SPADE)) {
            if (getBankItemCount(ItemID.SPADE) > 0) itemIds.add(ItemID.SPADE);
        }
        if (!Rs2Inventory.hasItem(ItemID.DIBBER)) {
            if (getBankItemCount(ItemID.DIBBER) > 0) itemIds.add(ItemID.DIBBER);
        }

        // 4. Compost
        boolean hasBucket = Rs2Inventory.hasItem(ItemID.BOTTOMLESS_COMPOST_BUCKET) ||
                Rs2Inventory.hasItem(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
        if (!hasBucket) {
            boolean bucketInBank = getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET) > 0 ||
                    getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) > 0;
            if (bucketInBank) {
                if (getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET) > 0) itemIds.add(ItemID.BOTTOMLESS_COMPOST_BUCKET);
                if (getBankItemCount(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) > 0) itemIds.add(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
            } else {
                int invCompost = Rs2Inventory.items()
                        .filter(item -> item.getId() == ItemID.BUCKET_ULTRACOMPOST ||
                                item.getId() == ItemID.BUCKET_SUPERCOMPOST ||
                                (item.getName() != null && (item.getName().equalsIgnoreCase("Ultracompost") || item.getName().equalsIgnoreCase("Supercompost"))))
                        .mapToInt(Rs2ItemModel::getQuantity)
                        .sum();
                if (invCompost < enabledPatches.size()) {
                    if (getBankItemCount(ItemID.BUCKET_ULTRACOMPOST) > 0) {
                        itemIds.add(ItemID.BUCKET_ULTRACOMPOST);
                    } else if (getBankItemCount(ItemID.BUCKET_SUPERCOMPOST) > 0) {
                        itemIds.add(ItemID.BUCKET_SUPERCOMPOST);
                    }
                }
            }
        }

        // 5. Seeds
        if (config.strategy() == HerbRunStrategy.PURE_RANARR) {
            int needed = enabledPatches.size();
            int inv = Rs2Inventory.itemQuantity(ItemID.RANARR_SEED);
            if (inv < needed && getBankItemCount(ItemID.RANARR_SEED) > 0) {
                itemIds.add(ItemID.RANARR_SEED);
            }
        } else {
            long diseaseFreeCount = enabledPatches.stream().filter(HerbPatch::isDiseaseFree).count();
            long standardCount = enabledPatches.size() - diseaseFreeCount;
            if (diseaseFreeCount > 0 && Rs2Inventory.itemQuantity(ItemID.RANARR_SEED) < diseaseFreeCount && getBankItemCount(ItemID.RANARR_SEED) > 0) {
                itemIds.add(ItemID.RANARR_SEED);
            }
            if (standardCount > 0 && Rs2Inventory.itemQuantity(ItemID.TOADFLAX_SEED) < standardCount && getBankItemCount(ItemID.TOADFLAX_SEED) > 0) {
                itemIds.add(ItemID.TOADFLAX_SEED);
            }
        }

        // 6. Teleports
        boolean hasInfiniteHouse = hasInfiniteHouseTeleport();
        int houseTabsInInv = Rs2Inventory.itemQuantity(ItemID.POH_TABLET_TELEPORTTOHOUSE);
        long housePatchesNeeded = enabledPatches.stream()
                .filter(p -> p.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE))
                .count();

        if (!hasInfiniteHouse && houseTabsInInv < housePatchesNeeded) {
            if (getBankItemCount(ItemID.POH_TABLET_TELEPORTTOHOUSE) > 0) {
                itemIds.add(ItemID.POH_TABLET_TELEPORTTOHOUSE);
            }
            if (getBankItemCount(ItemID.SKILLCAPE_CONSTRUCTION) > 0) {
                itemIds.add(ItemID.SKILLCAPE_CONSTRUCTION);
            }
            if (getBankItemCount(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED) > 0) {
                itemIds.add(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED);
            }
            if (getBankItemCount(ItemID.SKILLCAPE_MAX) > 0) {
                itemIds.add(ItemID.SKILLCAPE_MAX);
            }
            if (getBankItemCount(ItemID.SKILLCAPE_MAX_WORN) > 0) {
                itemIds.add(ItemID.SKILLCAPE_MAX_WORN);
            }
        }

        for (HerbPatch patch : enabledPatches) {
            if (patch.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE)) {
                continue;
            }
            boolean hasTeleport = false;
            for (int altId : patch.getAlternateTeleportItemIds()) {
                if (Rs2Equipment.isWearing(altId) || Rs2Inventory.hasItem(altId)) {
                    hasTeleport = true;
                    break;
                }
            }
            if (!hasTeleport) {
                for (int altId : patch.getAlternateTeleportItemIds()) {
                    if (getBankItemCount(altId) > 0) {
                        itemIds.add(altId);
                    }
                }
            }
        }

        return itemIds;
    }

    /**
     * Checks if all items required for 1 run are currently in the player's possession (equipped or in inventory).
     * Essential requirements: Core tools (Spade, Seed dibber), herb seeds, and at least 1 teleport.
     * Compost and yield gear (Farming cape, Magic secateurs) are optional enhancements.
     */
    public boolean hasAllSuppliesForRun() {
        boolean hasSpade = Rs2Inventory.hasItem(ItemID.SPADE);
        boolean hasDibber = Rs2Inventory.hasItem(ItemID.DIBBER);
        boolean hasSeeds = Rs2Inventory.itemQuantity(ItemID.RANARR_SEED) > 0 ||
                Rs2Inventory.itemQuantity(ItemID.TOADFLAX_SEED) > 0;

        if (!hasSpade || !hasDibber || !hasSeeds) {
            return false;
        }

        List<HerbPatch> enabled = getEnabledPatches();
        if (enabled.isEmpty()) {
            return false;
        }

        return enabled.stream().anyMatch(this::hasTeleportForPatch);
    }

    public boolean hasTeleportForPatch(HerbPatch patch) {
        if (patch == null) {
            return false;
        }
        for (int id : patch.getAlternateTeleportItemIds()) {
            if (Rs2Equipment.isWearing(id) || Rs2Inventory.hasItem(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of tradeable items that need to be purchased from the Grand Exchange.
     */
    public List<SupplyItem> getMissingTradeableItems(int runsToPurchase) {
        return calculateSupplies().stream()
                .filter(SupplyItem::isTradeable)
                .filter(item -> item.getGeBuyQuantity(runsToPurchase) > 0)
                .collect(Collectors.toList());
    }
}
