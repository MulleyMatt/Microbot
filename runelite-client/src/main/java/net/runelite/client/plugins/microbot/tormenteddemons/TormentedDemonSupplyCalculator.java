package net.runelite.client.plugins.microbot.tormenteddemons;

import lombok.Value;
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
 * and GE-purchasable supplies for Tormented Demons farming trips.
 */
@Singleton
public class TormentedDemonSupplyCalculator
{
    public static final int[] GAMES_NECKLACE_IDS = new int[]{
            ItemID.NECKLACE_OF_MINIGAMES_8,
            ItemID.NECKLACE_OF_MINIGAMES_7,
            ItemID.NECKLACE_OF_MINIGAMES_6,
            ItemID.NECKLACE_OF_MINIGAMES_5,
            ItemID.NECKLACE_OF_MINIGAMES_4,
            ItemID.NECKLACE_OF_MINIGAMES_3,
            ItemID.NECKLACE_OF_MINIGAMES_2,
            ItemID.NECKLACE_OF_MINIGAMES_1
    };

    public static final int[] RUNE_POUCH_IDS = new int[]{
            ItemID.DIVINE_RUNE_POUCH,
            ItemID.DIVINE_RUNE_POUCH_TROUVER,
            ItemID.BH_RUNE_POUCH,
            ItemID.BH_RUNE_POUCH_TROUVER
    };

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
    private final TormentedDemonConfig config;

    @Inject
    public TormentedDemonSupplyCalculator(Client client, TormentedDemonConfig config)
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
     * Pure helper: builds the supply checklist for a Tormented Demons trip.
     */
    public static List<SupplyRequirement> buildRequirements(TormentedDemonConfig config)
    {
        if (config == null)
        {
            return Collections.emptyList();
        }

        List<SupplyRequirement> list = new ArrayList<>();

        // Travel to Ancient Guthixian Temple
        if (config.travelMethod() == TormentedDemonConfig.TravelMethod.GAMES_NECKLACE_SAPPHIRE_LANTERN)
        {
            list.add(itemReq("Games necklace", ItemID.NECKLACE_OF_MINIGAMES_8, GAMES_NECKLACE_IDS, 1, true, false));
            list.add(itemReq(
                    "Sapphire lantern (Lit)",
                    ItemID.TOG_SAPPHIRE_LANTERN_LIT,
                    new int[]{ItemID.TOG_SAPPHIRE_LANTERN_LIT, ItemID.TOG_SAPPHIRE_LANTERN_UNLIT},
                    1,
                    true,
                    false
            ));
        }
        else
        {
            list.add(itemReq(
                    "Guthixian temple teleport",
                    ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE,
                    new int[]{ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE},
                    1,
                    true,
                    true
            ));
        }

        // Bank escape teleport
        TormentedDemonConfig.BankTeleport bankTp = config.bankTeleport();
        if (bankTp != null)
        {
            boolean isTradeable = bankTp != TormentedDemonConfig.BankTeleport.ROYAL_SEED_POD;
            boolean scales = bankTp == TormentedDemonConfig.BankTeleport.HOUSE_TAB;
            list.add(itemReq(bankTp.getDisplayName(), bankTp.getPrimaryItemId(), bankTp.getAcceptedIds(), 1, isTradeable, scales));
        }

        // Combat boost potions
        TormentedDemonConfig.MeleePotionType meleePot = config.meleePotion();
        if (meleePot != null && meleePot != TormentedDemonConfig.MeleePotionType.NONE)
        {
            list.add(itemReq(meleePot.getDisplayName(), meleePot.getPrimaryItemId(), meleePot.getAcceptedIds(), 1, true, true));
        }

        TormentedDemonConfig.RangedPotionType rangedPot = config.rangedPotion();
        if (rangedPot != null && rangedPot != TormentedDemonConfig.RangedPotionType.NONE)
        {
            list.add(itemReq(rangedPot.getDisplayName(), rangedPot.getPrimaryItemId(), rangedPot.getAcceptedIds(), 1, true, true));
        }

        // Prayer restores
        TormentedDemonConfig.PrayerRestoreType prayerType = config.prayerRestoreType();
        int prayerQty = Math.max(0, config.prayerRestoreQty());
        if (prayerType != null && prayerQty > 0)
        {
            list.add(itemReq(prayerType.getDisplayName(), prayerType.getPrimaryItemId(), prayerType.getAcceptedIds(), prayerQty, true, true));
        }

        // Food
        TormentedDemonConfig.FoodType foodType = config.foodType();
        int foodQty = Math.max(0, config.foodQty());
        if (foodType != null && foodQty > 0)
        {
            list.add(itemReq(foodType.getDisplayName(), foodType.getItemId(), new int[]{foodType.getItemId()}, foodQty, true, true));
        }

        // Arceuus spellbook tools & runes (Thralls, Death Charge, Demonic Offering)
        if (config.prepArceuusRunes())
        {
            list.add(itemReq("Book of the dead", ItemID.BOOK_OF_THE_DEAD, new int[]{ItemID.BOOK_OF_THE_DEAD}, 1, false, false));
            list.add(itemReq("Rune pouch", ItemID.BH_RUNE_POUCH, RUNE_POUCH_IDS, 1, false, false));

            list.add(runeReq("Fire runes", Runes.FIRE, 100));
            list.add(runeReq("Blood runes", Runes.BLOOD, 60));
            list.add(runeReq("Cosmic runes", Runes.COSMIC, 25));
            list.add(runeReq("Death runes", Runes.DEATH, 50));
            list.add(runeReq("Soul runes", Runes.SOUL, 100));
            list.add(runeReq("Wrath runes", Runes.WRATH, 50));
        }

        return list;
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
        for (int pouchId : getPouchIdSet())
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
