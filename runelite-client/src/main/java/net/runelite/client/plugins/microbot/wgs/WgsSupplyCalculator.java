package net.runelite.client.plugins.microbot.wgs;

import com.google.common.collect.ImmutableSet;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.RunePouchType;
import net.runelite.client.plugins.microbot.util.magic.Runes;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates required, carried (Inventory + Equipment + Rune Pouch), Bank-withdrawable,
 * and GE-purchasable supplies for each of the 3 While Guthix Sleeps boss encounters.
 */
@Slf4j
@Singleton
public class WgsSupplyCalculator
{
    public static final int[] STAMINA_VARIANT_IDS = new int[]{
            ItemID._4DOSESTAMINA,
            ItemID._3DOSESTAMINA,
            ItemID._2DOSESTAMINA,
            ItemID._1DOSESTAMINA
    };

    public static final int[] TELEORB_VARIANT_IDS = new int[]{
            ItemID.WGS_LUCIEN_TELEORB,
            ItemID.WGS_COM_TELE_ORB,
            ItemID.WGS_COM_TELE_ORB_2
    };

    public static final Set<Integer> DARK_SQUALL_ROBE_IDS = ImmutableSet.of(
            ItemID.WGS_DARK_SQUALL_HOOD,
            ItemID.WGS_DARK_SQUALL_ROBE_TOP,
            ItemID.WGS_DARK_SQUALL_ROBE_BOTTOM
    );

    private static final int[] RUNE_POUCH_RUNE_VARBITS = {
            Varbits.RUNE_POUCH_RUNE1,
            Varbits.RUNE_POUCH_RUNE2,
            Varbits.RUNE_POUCH_RUNE3,
            Varbits.RUNE_POUCH_RUNE4
    };

    private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {
            Varbits.RUNE_POUCH_AMOUNT1,
            Varbits.RUNE_POUCH_AMOUNT2,
            Varbits.RUNE_POUCH_AMOUNT3,
            Varbits.RUNE_POUCH_AMOUNT4
    };

    private final Client client;
    private final WgsConfig config;

    @Inject
    public WgsSupplyCalculator(Client client, WgsConfig config)
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
        Runes runeType;
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
     * Pure helper: builds the supply requirement list for the configured WGS boss stage.
     */
    public static List<SupplyRequirement> buildRequirements(WgsConfig config)
    {
        if (config == null)
        {
            return Collections.emptyList();
        }

        WgsBossStage stage = config.prepBossStage() != null
                ? config.prepBossStage()
                : WgsBossStage.SUROK_MAGIS;
        return buildRequirementsForStage(config, stage);
    }

    /**
     * Pure helper: builds supply requirements for an explicit WGS boss stage.
     */
    public static List<SupplyRequirement> buildRequirementsForStage(WgsConfig config, WgsBossStage stage)
    {
        if (config == null || stage == null)
        {
            return Collections.emptyList();
        }

        List<SupplyRequirement> list = new ArrayList<>();

        switch (stage)
        {
            case SUROK_MAGIS:
                addSurokRequirements(list, config);
                break;
            case BALANCE_ELEMENTAL:
                addBalanceElementalRequirements(list, config);
                break;
            case TORMENTED_DEMONS:
                addTormentedDemonsRequirements(list, config);
                break;
        }

        return list;
    }

    private static void addSurokRequirements(List<SupplyRequirement> list, WgsConfig config)
    {
        // Required Dark Squall Robes (non-tradeable quest items needed to enter)
        list.add(itemReq("Dark squall hood", ItemID.WGS_DARK_SQUALL_HOOD, new int[]{ItemID.WGS_DARK_SQUALL_HOOD}, 1, false, false));
        list.add(itemReq("Dark squall robe top", ItemID.WGS_DARK_SQUALL_ROBE_TOP, new int[]{ItemID.WGS_DARK_SQUALL_ROBE_TOP}, 1, false, false));
        list.add(itemReq("Dark squall robe bottom", ItemID.WGS_DARK_SQUALL_ROBE_BOTTOM, new int[]{ItemID.WGS_DARK_SQUALL_ROBE_BOTTOM}, 1, false, false));

        // Teleorb required post-fight on Surok's unconscious body
        list.add(itemReq("Teleorb (post-fight)", ItemID.WGS_LUCIEN_TELEORB, TELEORB_VARIANT_IDS, 1, false, false));

        // Runes for all 4 elemental combat spells + Weaken + Bind/Snare/Entangle + Telegrab + Alchemy
        int elemQty = Math.max(50, config.elementalRuneCount());
        list.add(runeReq("Air runes (Elemental + Telegrab)", Runes.AIR, elemQty));
        list.add(runeReq("Water runes (Elemental + Weaken/Bind)", Runes.WATER, elemQty));
        list.add(runeReq("Earth runes (Elemental + Weaken/Bind)", Runes.EARTH, elemQty));
        list.add(runeReq("Fire runes (Elemental + Alchemy)", Runes.FIRE, elemQty));

        WgsConfig.CatalystRuneTier catalystTier = config.surokCatalystRune();
        int catalystQty = Math.max(0, config.catalystRuneCount());
        if (catalystTier != null && catalystQty > 0)
        {
            list.add(runeReq(catalystTier.getDisplayName(), catalystTier.getRune(), catalystQty));
        }

        list.add(runeReq("Body runes (Weaken)", Runes.BODY, 30));
        list.add(runeReq("Nature runes (Bind & Alchemy)", Runes.NATURE, 60));
        list.add(runeReq("Law runes (Telekinetic Grab)", Runes.LAW, 30));

        // Potions & Food
        if (config.includeStaminaPotion())
        {
            list.add(itemReq("Stamina potion(4)", ItemID._4DOSESTAMINA, STAMINA_VARIANT_IDS, 1, true, true));
        }

        WgsConfig.RestorePotionType restoreType = config.restorePotionType();
        int restoreCount = Math.max(0, config.restorePotionCount());
        if (restoreType != null && restoreCount > 0)
        {
            list.add(itemReq(restoreType.getDisplayName(), restoreType.getPrimaryItemId(), restoreType.getDoseVariantIds(), restoreCount, true, true));
        }

        addFoodRequirement(list, config, false);
    }

    private static void addBalanceElementalRequirements(List<SupplyRequirement> list, WgsConfig config)
    {
        // Balance Elemental drains stats and Magic to 1 -> enforce at least 4 Super Restores
        int restoreCount = Math.max(4, config.restorePotionCount());
        WgsConfig.RestorePotionType superRestore = WgsConfig.RestorePotionType.SUPER_RESTORE;
        list.add(itemReq(
                "Super restore(4) (Min 4)",
                superRestore.getPrimaryItemId(),
                superRestore.getDoseVariantIds(),
                restoreCount,
                true,
                true
        ));

        // Mandatory one-click non-rune teleport (Magic can be drained to 1)
        WgsConfig.TeleportItemType tp = config.teleportItemType();
        if (tp != null)
        {
            list.add(itemReq(tp.getDisplayName(), tp.getPrimaryItemId(), tp.getAcceptedIds(), 1, tp.isTradeable(), false));
        }

        if (config.includeStaminaPotion())
        {
            list.add(itemReq("Stamina potion(4)", ItemID._4DOSESTAMINA, STAMINA_VARIANT_IDS, 1, true, true));
        }

        addFoodRequirement(list, config, true);
    }

    private static void addTormentedDemonsRequirements(List<SupplyRequirement> list, WgsConfig config)
    {
        WgsConfig.RestorePotionType restoreType = config.restorePotionType();
        int restoreCount = Math.max(0, config.restorePotionCount());
        if (restoreType != null && restoreCount > 0)
        {
            list.add(itemReq(restoreType.getDisplayName(), restoreType.getPrimaryItemId(), restoreType.getDoseVariantIds(), restoreCount, true, true));
        }

        WgsConfig.TeleportItemType tp = config.teleportItemType();
        if (tp != null)
        {
            list.add(itemReq(tp.getDisplayName(), tp.getPrimaryItemId(), tp.getAcceptedIds(), 1, tp.isTradeable(), false));
        }

        if (config.includeStaminaPotion())
        {
            list.add(itemReq("Stamina potion(4)", ItemID._4DOSESTAMINA, STAMINA_VARIANT_IDS, 1, true, true));
        }

        addFoodRequirement(list, config, true);
    }

    private static void addFoodRequirement(List<SupplyRequirement> list, WgsConfig config, boolean includeKarambwan)
    {
        WgsConfig.FoodType foodType = config.foodType();
        int foodCount = Math.max(0, config.foodCount());
        if (foodType != null && foodCount > 0)
        {
            list.add(itemReq(foodType.getDisplayName(), foodType.getItemId(), new int[]{foodType.getItemId()}, foodCount, true, true));
        }

        if (includeKarambwan && config.karambwanCount() > 0)
        {
            list.add(itemReq("Cooked karambwan", ItemID.TBWT_COOKED_KARAMBWAN, new int[]{ItemID.TBWT_COOKED_KARAMBWAN}, config.karambwanCount(), true, true));
        }
    }

    private static SupplyRequirement itemReq(
            String displayName,
            int primaryItemId,
            int[] acceptedItemIds,
            int qty,
            boolean tradeable,
            boolean scalesWithBatch)
    {
        return new SupplyRequirement(displayName, primaryItemId, acceptedItemIds, qty, tradeable, scalesWithBatch, null);
    }

    private static SupplyRequirement runeReq(String displayName, Runes rune, int qty)
    {
        Runes[] combos = Runes.getComboRunes(rune);
        int[] accepted = new int[1 + combos.length];
        accepted[0] = rune.getItemId();
        for (int i = 0; i < combos.length; i++)
        {
            accepted[i + 1] = combos[i].getItemId();
        }
        return new SupplyRequirement(displayName, rune.getItemId(), accepted, qty, true, true, rune);
    }

    public List<SupplyStatus> evaluateSupplies()
    {
        return evaluateSupplies(buildRequirements(config));
    }

    public List<SupplyStatus> evaluateSupplies(List<SupplyRequirement> requirements)
    {
        if (requirements == null || requirements.isEmpty())
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

            if (req.getRuneType() != null)
            {
                carried = getRuneCountInPossession(inv, equip, req.getRuneType());
                for (int id : req.getAcceptedItemIds())
                {
                    if (bank != null)
                    {
                        bankCount += countItemInContainer(bank, id);
                    }
                    else if (bankMirrorAvailable)
                    {
                        bankCount += Rs2Bank.count(id);
                    }
                }
            }
            else
            {
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
            }

            int requiredTotal = req.isScalesWithBatch()
                    ? req.getSingleTripQuantity() * batch
                    : req.getSingleTripQuantity();

            statuses.add(new SupplyStatus(req, carried, carried + bankCount, requiredTotal));
        }

        return statuses;
    }

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

    public int getRuneCountInPossession(ItemContainer inv, ItemContainer equip, Runes targetRune)
    {
        if (targetRune == null)
        {
            return 0;
        }

        int total = 0;
        if (inv != null)
        {
            total += countItemInContainer(inv, targetRune.getItemId());
            for (Runes combo : Runes.getComboRunes(targetRune))
            {
                total += countItemInContainer(inv, combo.getItemId());
            }
        }

        if (hasRunePouch(inv, equip) && client != null)
        {
            for (int i = 0; i < RUNE_POUCH_RUNE_VARBITS.length; i++)
            {
                int runeVarbitVal = client.getVarbitValue(RUNE_POUCH_RUNE_VARBITS[i]);
                int amountVal = client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[i]);
                if (runeVarbitVal <= 0 || amountVal <= 0)
                {
                    continue;
                }
                Runes pouchRune = Runes.byVarbitId(runeVarbitVal);
                if (pouchRune != null && pouchRune.providesRune(targetRune))
                {
                    total += amountVal;
                }
            }
        }

        return total;
    }

    private static boolean hasRunePouch(ItemContainer inv, ItemContainer equip)
    {
        Set<Integer> pouchIds = getPouchIdSet();
        for (int pouchId : pouchIds)
        {
            if ((inv != null && countItemInContainer(inv, pouchId) > 0)
                    || (equip != null && countItemInContainer(equip, pouchId) > 0))
            {
                return true;
            }
        }
        return false;
    }

    public static Set<Integer> getPouchIdSet()
    {
        Set<Integer> ids = new HashSet<>();
        for (int id : RunePouchType.getPouchIds())
        {
            ids.add(id);
        }
        return ids;
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
