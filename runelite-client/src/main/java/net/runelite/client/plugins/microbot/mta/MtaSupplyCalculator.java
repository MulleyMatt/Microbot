package net.runelite.client.plugins.microbot.mta;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.inventory.RunePouchType;
import net.runelite.client.plugins.microbot.util.magic.Runes;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MtaSupplyCalculator
{
	public static final Set<Integer> AIR_STAVES = ImmutableSet.of(
		ItemID.STAFF_OF_AIR,
		ItemID.AIR_BATTLESTAFF,
		ItemID.MYSTIC_AIR_STAFF,
		ItemID.SMOKE_BATTLESTAFF,
		ItemID.MYSTIC_SMOKE_BATTLESTAFF,
		ItemID.DUST_BATTLESTAFF,
		ItemID.MYSTIC_DUST_BATTLESTAFF,
		ItemID.MIST_BATTLESTAFF,
		ItemID.MYSTIC_MIST_BATTLESTAFF
	);

	public static final Set<Integer> FIRE_STAVES = ImmutableSet.of(
		ItemID.STAFF_OF_FIRE,
		ItemID.FIRE_BATTLESTAFF,
		ItemID.MYSTIC_FIRE_STAFF,
		ItemID.LAVA_BATTLESTAFF,
		ItemID.MYSTIC_LAVA_STAFF,
		ItemID.LAVA_BATTLESTAFF_PRETTY,
		ItemID.STEAM_BATTLESTAFF,
		ItemID.MYSTIC_STEAM_BATTLESTAFF,
		ItemID.STEAM_BATTLESTAFF_PRETTY,
		ItemID.MYSTIC_STEAM_BATTLESTAFF_PRETTY,
		ItemID.SMOKE_BATTLESTAFF,
		ItemID.MYSTIC_SMOKE_BATTLESTAFF,
		ItemID.TOME_OF_FIRE
	);

	public static final Set<Integer> EARTH_OR_MUD_STAVES = ImmutableSet.of(
		ItemID.MUD_BATTLESTAFF,
		ItemID.MYSTIC_MUD_STAFF,
		ItemID.STAFF_OF_EARTH,
		ItemID.EARTH_BATTLESTAFF,
		ItemID.MYSTIC_EARTH_STAFF,
		ItemID.LAVA_BATTLESTAFF,
		ItemID.MYSTIC_LAVA_STAFF,
		ItemID.LAVA_BATTLESTAFF_PRETTY,
		ItemID.DUST_BATTLESTAFF,
		ItemID.MYSTIC_DUST_BATTLESTAFF
	);

	public static final Set<Integer> WATER_WEAPONS = ImmutableSet.of(
		ItemID.MUD_BATTLESTAFF,
		ItemID.MYSTIC_MUD_STAFF,
		ItemID.KODAI_WAND,
		ItemID.BR_KODAI_WAND,
		ItemID.STAFF_OF_WATER,
		ItemID.WATER_BATTLESTAFF,
		ItemID.MYSTIC_WATER_STAFF,
		ItemID.MIST_BATTLESTAFF,
		ItemID.MYSTIC_MIST_BATTLESTAFF,
		ItemID.STEAM_BATTLESTAFF,
		ItemID.MYSTIC_STEAM_BATTLESTAFF,
		ItemID.STEAM_BATTLESTAFF_PRETTY,
		ItemID.MYSTIC_STEAM_BATTLESTAFF_PRETTY,
		ItemID.TOME_OF_WATER
	);

	public static final Set<Integer> ENCHANT_WANDS_AND_STAVES = ImmutableSet.of(
		ItemID.KODAI_WAND,
		ItemID.BR_KODAI_WAND,
		ItemID.MAGICTRAINING_WAND_MASTER,
		ItemID.MUD_BATTLESTAFF,
		ItemID.MYSTIC_MUD_STAFF,
		ItemID.STAFF_OF_WATER,
		ItemID.WATER_BATTLESTAFF,
		ItemID.MYSTIC_WATER_STAFF
	);

	public static final Set<Integer> FOOD_IDS = ImmutableSet.of(
		ItemID.SHARK,
		ItemID.ANGLERFISH,
		ItemID.MANTARAY,
		ItemID.MONKFISH,
		ItemID.DARK_CRAB,
		ItemID.TBWT_COOKED_KARAMBWAN
	);

	public static final Set<Integer> IN_ROOM_HEALING_IDS = ImmutableSet.of(
		ItemID.SHARK,
		ItemID.ANGLERFISH,
		ItemID.MANTARAY,
		ItemID.MONKFISH,
		ItemID.DARK_CRAB,
		ItemID.TBWT_COOKED_KARAMBWAN,
		ItemID.PEACH,
		ItemID.BANANA
	);

	public static final Set<Integer> STAMINA_IDS = ImmutableSet.of(
		ItemID._4DOSESTAMINA,
		ItemID._3DOSESTAMINA,
		ItemID._2DOSESTAMINA,
		ItemID._1DOSESTAMINA
	);

	public static final Set<Integer> RING_OF_DUELING_IDS = ImmutableSet.of(
		ItemID.RING_OF_DUELING_8,
		ItemID.RING_OF_DUELING_7,
		ItemID.RING_OF_DUELING_6,
		ItemID.RING_OF_DUELING_5,
		ItemID.RING_OF_DUELING_4,
		ItemID.RING_OF_DUELING_3,
		ItemID.RING_OF_DUELING_2,
		ItemID.RING_OF_DUELING_1
	);

	public static final Set<Integer> PIZAZZ_HAT_IDS = ImmutableSet.of(
		ItemID.MAGICTRAINING_PROGHAT_DULL,
		ItemID.MAGICTRAINING_PROGHAT_ENERGISED,
		ItemID.MAGICTRAINING_PROGHAT_FULL
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

	@Getter
	@RequiredArgsConstructor
	public static class SupplyItem
	{
		private final String name;
		private final int itemId;
		private final int requiredQuantity;
		private final int currentPossession; // inventory + equipped + rune pouch
		private final int bankQuantity;
		private final boolean tradeable;
		private final String category; // "Pouch", "Rune", "Staff", "Food", "Potion", "Diary"

		public int getMissingForRun()
		{
			return Math.max(0, requiredQuantity - currentPossession);
		}

		public boolean isSatisfied()
		{
			return currentPossession >= requiredQuantity;
		}

		public boolean isMissingFromInventory()
		{
			return getMissingForRun() > 0;
		}

		public boolean canWithdrawFromBank()
		{
			return isMissingFromInventory() && bankQuantity >= getMissingForRun();
		}

		public boolean canPartiallyWithdrawFromBank()
		{
			return isMissingFromInventory() && bankQuantity > 0 && bankQuantity < getMissingForRun();
		}

		public boolean needsToBuyFromGe()
		{
			return tradeable && (currentPossession + bankQuantity < requiredQuantity);
		}

		public int getGeBuyQuantity(int runsToPurchase)
		{
			boolean reusable = "Pouch".equals(category) || "Staff".equals(category);
			int multiplier = Math.max(1, runsToPurchase);
			int totalNeeded = reusable ? requiredQuantity : requiredQuantity * multiplier;
			int totalOwned = currentPossession + bankQuantity;
			return Math.max(0, totalNeeded - totalOwned);
		}
	}

	private final Client client;
	private final MtaConfig config;
	private final ItemManager itemManager;

	@Inject
	public MtaSupplyCalculator(Client client, MtaConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
	}

	public List<SupplyItem> calculateSupplies(
		int telePoints,
		int alchPoints,
		int enchPoints,
		int gravePoints,
		boolean bonesToPeachesUnlocked
	)
	{
		List<SupplyItem> checklist = new ArrayList<>();

		int teleRem = bonesToPeachesUnlocked ? 0 : Math.max(0, config.telekineticTargetPoints() - telePoints);
		int alchRem = bonesToPeachesUnlocked ? 0 : Math.max(0, config.alchemistTargetPoints() - alchPoints);
		int enchRem = bonesToPeachesUnlocked ? 0 : Math.max(0, config.enchantmentTargetPoints() - enchPoints);
		int graveRem = bonesToPeachesUnlocked ? 0 : Math.max(0, config.graveyardTargetPoints() - gravePoints);
		boolean anyMtaRoomRemaining = (teleRem + alchRem + enchRem + graveRem) > 0;

		// 1. Rune Pouch check
		if (anyMtaRoomRemaining && config.requireRunePouch())
		{
			int pouchInv = hasRunePouchInInventory() ? 1 : 0;
			int pouchBank = getBankCountForSet(getPouchIdSet());
			checklist.add(new SupplyItem(
				"Rune pouch / Divine pouch",
				ItemID.BH_RUNE_POUCH,
				1,
				pouchInv,
				pouchBank,
				false,
				"Pouch"
			));
		}

		// 2. Elemental Staves check
		if (config.requireElementalStaves())
		{
			if (graveRem > 0 || (bonesToPeachesUnlocked && config.trackAlKharidPalaceCast()))
			{
				addStaffGroupCheck(
					checklist,
					"Earth / Mud staff (Graveyard)",
					ItemID.MUD_BATTLESTAFF,
					EARTH_OR_MUD_STAVES
				);
			}
			if (alchRem > 0)
			{
				addStaffGroupCheck(
					checklist,
					"Fire staff (Alchemy)",
					ItemID.STAFF_OF_FIRE,
					FIRE_STAVES
				);
			}
			if (teleRem > 0)
			{
				addStaffGroupCheck(
					checklist,
					"Air staff (Telekinetic)",
					ItemID.STAFF_OF_AIR,
					AIR_STAVES
				);
			}
		}

		// 3. Runes (Inventory + Rune Pouch + Bank)
		if (enchRem > 0)
		{
			int enchCasts = Math.max(15, (int) Math.ceil(enchRem / 8.0));
			addRuneCheck(checklist, "Cosmic runes (Enchant)", Runes.COSMIC, enchCasts);
			addRuneCheck(checklist, "Soul runes (Lvl-7 Enchant)", Runes.SOUL, enchCasts * 20);
			if (config.includeBloodRunesForEnchant7())
			{
				addRuneCheck(checklist, "Blood runes (Lvl-7 Enchant)", Runes.BLOOD, enchCasts * 20);
			}
		}

		int alchNatures = alchRem > 0 ? Math.max(30, (int) Math.ceil(alchRem * 3.5)) : 0;
		int graveNatures = graveRem > 0 ? Math.max(20, graveRem) : 0;
		int palaceNatures = config.trackAlKharidPalaceCast() ? 2 : 0;
		int totalNatures = alchNatures + graveNatures + palaceNatures;
		if (totalNatures > 0)
		{
			addRuneCheck(checklist, "Nature runes (Alch & Grave)", Runes.NATURE, totalNatures);
		}

		if (teleRem > 0)
		{
			int lawNeeded = Math.max(25, (int) Math.ceil(teleRem * 2.5));
			addRuneCheck(checklist, "Law runes (Telekinetic)", Runes.LAW, lawNeeded);
		}

		// Water runes for Bones to Bananas / Bones to Peaches if player does not have Mud Battlestaff / Water weapon
		boolean hasWaterStaffOwned = countInInventoryOrEquipment(WATER_WEAPONS) > 0
			|| getBankCountForSet(WATER_WEAPONS) > 0;
		if (config.includeWaterRunesIfNoMudStaff() && !hasWaterStaffOwned)
		{
			int waterNeeded = (graveRem > 0 ? Math.max(40, graveRem * 2) : 0)
				+ (config.trackAlKharidPalaceCast() ? 4 : 0);
			if (waterNeeded > 0)
			{
				addRuneCheck(checklist, "Water runes (Graveyard)", Runes.WATER, waterNeeded);
			}
		}

		// 4. Food & Stamina Potions
		if (graveRem > 0 && config.foodCount() > 0)
		{
			int foodInv = countInInventoryOrEquipment(FOOD_IDS);
			int foodBank = getBankCountForSet(FOOD_IDS);
			int primaryFoodId = getBankCount(ItemID.ANGLERFISH) > 0 || countInInventoryOrEquipment(ImmutableSet.of(ItemID.ANGLERFISH)) > 0
				? ItemID.ANGLERFISH
				: ItemID.SHARK;
			checklist.add(new SupplyItem(
				"Sharks / Anglerfish (Graveyard)",
				primaryFoodId,
				config.foodCount(),
				foodInv,
				foodBank,
				true,
				"Food"
			));
		}

		if (anyMtaRoomRemaining && config.staminaCount() > 0)
		{
			int stamInv = countInInventoryOrEquipment(STAMINA_IDS);
			int stamBank = getBankCountForSet(STAMINA_IDS);
			checklist.add(new SupplyItem(
				"Stamina potion(4)",
				ItemID._4DOSESTAMINA,
				config.staminaCount(),
				stamInv,
				stamBank,
				true,
				"Potion"
			));
		}

		// 5. Bones for Final Al Kharid Palace Cast
		if (bonesToPeachesUnlocked && config.trackAlKharidPalaceCast())
		{
			int bonesInv = countInInventoryOrEquipment(ImmutableSet.of(ItemID.BONES, ItemID.BIG_BONES));
			int bonesBank = getBankCount(ItemID.BONES) + getBankCount(ItemID.BIG_BONES);
			checklist.add(new SupplyItem(
				"Bones (For Al Kharid Palace cast)",
				ItemID.BONES,
				1,
				bonesInv,
				bonesBank,
				true,
				"Diary"
			));
		}

		return checklist;
	}

	private void addStaffGroupCheck(
		List<SupplyItem> list,
		String label,
		int defaultGeItemId,
		Set<Integer> validStaffIds
	)
	{
		int possessed = countInInventoryOrEquipment(validStaffIds) > 0 ? 1 : 0;
		int bankCount = getBankCountForSet(validStaffIds);
		list.add(new SupplyItem(
			label,
			defaultGeItemId,
			1,
			possessed,
			bankCount,
			true,
			"Staff"
		));
	}

	private void addRuneCheck(List<SupplyItem> list, String label, Runes rune, int requiredQty)
	{
		int possessed = getRuneCountInPossession(rune);
		int bank = getBankCount(rune.getItemId());
		for (Runes combo : Runes.getComboRunes(rune))
		{
			bank += getBankCount(combo.getItemId());
		}
		list.add(new SupplyItem(
			label,
			rune.getItemId(),
			requiredQty,
			possessed,
			bank,
			true,
			"Rune"
		));
	}

	public int getRuneCountInPossession(Runes targetRune)
	{
		if (targetRune == null)
		{
			return 0;
		}

		int total = countItemInContainer(InventoryID.INV, targetRune.getItemId());
		for (Runes combo : Runes.getComboRunes(targetRune))
		{
			total += countItemInContainer(InventoryID.INV, combo.getItemId());
		}

		if (hasRunePouchInInventory() && client != null)
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

	public boolean hasRunePouchInInventory()
	{
		return countInInventoryOrEquipment(getPouchIdSet()) > 0;
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

	public boolean hasPizazzHatInPossession()
	{
		return countInInventoryOrEquipment(PIZAZZ_HAT_IDS) > 0;
	}

	public boolean hasRequiredStaffEquippedForRoom(MtaRoomType room)
	{
		if (room == null)
		{
			return true;
		}
		switch (room)
		{
			case TELEKINETIC:
				return isAnyEquipped(AIR_STAVES);
			case ALCHEMY:
				return isAnyEquipped(FIRE_STAVES);
			case GRAVEYARD:
				return isAnyEquipped(EARTH_OR_MUD_STAVES);
			case ENCHANTMENT:
			default:
				return true;
		}
	}

	public Set<Integer> getInventoryStaffIdsForRoom(MtaRoomType room)
	{
		if (room == null)
		{
			return ImmutableSet.of();
		}
		Set<Integer> candidates;
		switch (room)
		{
			case TELEKINETIC:
				candidates = AIR_STAVES;
				break;
			case ALCHEMY:
				candidates = FIRE_STAVES;
				break;
			case GRAVEYARD:
				candidates = EARTH_OR_MUD_STAVES;
				break;
			case ENCHANTMENT:
				candidates = ENCHANT_WANDS_AND_STAVES;
				break;
			default:
				return ImmutableSet.of();
		}
		return getMatchingItemsInInventory(candidates);
	}

	public Set<Integer> getMatchingItemsInInventory(Set<Integer> validIds)
	{
		Set<Integer> found = new HashSet<>();
		if (client == null || validIds == null)
		{
			return found;
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return found;
		}
		for (Item item : inv.getItems())
		{
			if (item != null && validIds.contains(item.getId()))
			{
				found.add(item.getId());
			}
		}
		return found;
	}

	public boolean isAnyEquipped(Set<Integer> validIds)
	{
		if (client == null || validIds == null)
		{
			return false;
		}
		ItemContainer equip = client.getItemContainer(InventoryID.WORN);
		if (equip == null)
		{
			return false;
		}
		for (Item item : equip.getItems())
		{
			if (item != null && validIds.contains(item.getId()))
			{
				return true;
			}
		}
		return false;
	}

	public int countInInventoryOrEquipment(Set<Integer> itemIds)
	{
		int total = 0;
		for (int id : itemIds)
		{
			total += countItemInContainer(InventoryID.INV, id);
			total += countItemInContainer(InventoryID.WORN, id);
		}
		return total;
	}

	public int countItemInContainer(int containerId, int itemId)
	{
		if (client == null)
		{
			return 0;
		}
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null)
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

	public int getFreeInventorySlots()
	{
		if (client == null)
		{
			return 28;
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return 28;
		}
		int occupied = 0;
		for (Item item : inv.getItems())
		{
			if (item != null && item.getId() > 0 && item.getQuantity() > 0)
			{
				occupied++;
			}
		}
		return Math.max(0, 28 - occupied);
	}

	public int getBankCountForSet(Set<Integer> itemIds)
	{
		int total = 0;
		for (int id : itemIds)
		{
			total += getBankCount(id);
		}
		return total;
	}

	public int getBankCount(int itemId)
	{
		int canonicalId = itemManager != null ? itemManager.canonicalize(itemId) : itemId;
		if (client != null)
		{
			ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank != null)
			{
				int count = bank.count(canonicalId);
				if (count > 0)
				{
					return count;
				}
				if (canonicalId != itemId)
				{
					count = bank.count(itemId);
					if (count > 0)
					{
						return count;
					}
				}
			}
		}
		if (!Rs2Bank.hasBankMirrorSnapshot())
		{
			return 0;
		}
		try
		{
			return Rs2Bank.bankItems().stream()
				.filter(item -> item.getId() == canonicalId || item.getId() == itemId)
				.mapToInt(Rs2ItemModel::getQuantity)
				.sum();
		}
		catch (Exception ignored)
		{
			return 0;
		}
	}

	public Set<Integer> getMissingBankItemIds(
		int telePoints,
		int alchPoints,
		int enchPoints,
		int gravePoints,
		boolean bonesToPeachesUnlocked
	)
	{
		Set<Integer> filterIds = new HashSet<>();
		List<SupplyItem> supplies = calculateSupplies(telePoints, alchPoints, enchPoints, gravePoints, bonesToPeachesUnlocked);

		for (SupplyItem item : supplies)
		{
			if (!item.isMissingFromInventory())
			{
				continue;
			}
			if ("Pouch".equals(item.getCategory()))
			{
				addOwnedBankIdsFromSet(filterIds, getPouchIdSet());
			}
			else if ("Staff".equals(item.getCategory()))
			{
				if (item.getItemId() == ItemID.MUD_BATTLESTAFF)
				{
					addOwnedBankIdsFromSet(filterIds, EARTH_OR_MUD_STAVES);
				}
				else if (item.getItemId() == ItemID.STAFF_OF_FIRE)
				{
					addOwnedBankIdsFromSet(filterIds, FIRE_STAVES);
				}
				else if (item.getItemId() == ItemID.STAFF_OF_AIR)
				{
					addOwnedBankIdsFromSet(filterIds, AIR_STAVES);
				}
			}
			else if ("Food".equals(item.getCategory()))
			{
				addOwnedBankIdsFromSet(filterIds, FOOD_IDS);
			}
			else if ("Potion".equals(item.getCategory()))
			{
				addOwnedBankIdsFromSet(filterIds, STAMINA_IDS);
			}
			else if ("Rune".equals(item.getCategory()))
			{
				if (getBankCount(item.getItemId()) > 0)
				{
					filterIds.add(item.getItemId());
				}
				Runes rune = Runes.byItemId(item.getItemId());
				if (rune != null)
				{
					for (Runes combo : Runes.getComboRunes(rune))
					{
						if (getBankCount(combo.getItemId()) > 0)
						{
							filterIds.add(combo.getItemId());
						}
					}
				}
			}
			else if (getBankCount(item.getItemId()) > 0)
			{
				filterIds.add(item.getItemId());
			}
		}

		// Also include Ring of Dueling if player does not have one in inventory/equipment
		if (countInInventoryOrEquipment(RING_OF_DUELING_IDS) == 0)
		{
			addOwnedBankIdsFromSet(filterIds, RING_OF_DUELING_IDS);
		}

		// Also include Pizazz Progress Hat if stored in bank
		if (!hasPizazzHatInPossession())
		{
			addOwnedBankIdsFromSet(filterIds, PIZAZZ_HAT_IDS);
		}

		return filterIds;
	}

	private void addOwnedBankIdsFromSet(Set<Integer> target, Set<Integer> candidates)
	{
		for (int id : candidates)
		{
			if (getBankCount(id) > 0)
			{
				target.add(id);
			}
		}
	}

	public List<SupplyItem> getMissingTradeableItems(
		int telePoints,
		int alchPoints,
		int enchPoints,
		int gravePoints,
		boolean bonesToPeachesUnlocked,
		int runsToPurchase
	)
	{
		return calculateSupplies(telePoints, alchPoints, enchPoints, gravePoints, bonesToPeachesUnlocked).stream()
			.filter(SupplyItem::isTradeable)
			.filter(item -> item.getGeBuyQuantity(runsToPurchase) > 0)
			.collect(Collectors.toList());
	}

	public boolean hasAllMissingSuppliesSatisfied(
		int telePoints,
		int alchPoints,
		int enchPoints,
		int gravePoints,
		boolean bonesToPeachesUnlocked
	)
	{
		return calculateSupplies(telePoints, alchPoints, enchPoints, gravePoints, bonesToPeachesUnlocked).stream()
			.filter(item -> !"Food".equals(item.getCategory()) && !"Potion".equals(item.getCategory()))
			.allMatch(SupplyItem::isSatisfied);
	}
}
