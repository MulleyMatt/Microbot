package net.runelite.client.plugins.microbot.mta;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MtaPluginTest
{
	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private ClientThread clientThread;

	@Mock
	@Bind
	private ConfigManager configManager;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Mock
	@Bind
	private BankSearch bankSearch;

	@Mock
	@Bind
	private ItemManager itemManager;

	@Mock
	@Bind
	private ModelOutlineRenderer modelOutlineRenderer;

	@Bind
	private MtaConfig config = new MtaConfig() {};

	@Inject
	private MtaPlugin plugin;

	@Inject
	private MtaSupplyCalculator supplyCalculator;

	private ItemContainer invContainer;
	private ItemContainer equipContainer;
	private ItemContainer bankContainer;

	@Before
	public void setUp()
	{
		invContainer = mock(ItemContainer.class);
		equipContainer = mock(ItemContainer.class);
		bankContainer = mock(ItemContainer.class);

		when(invContainer.getItems()).thenReturn(new Item[0]);
		when(equipContainer.getItems()).thenReturn(new Item[0]);
		when(bankContainer.getItems()).thenReturn(new Item[0]);

		when(client.getItemContainer(InventoryID.INV)).thenReturn(invContainer);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipContainer);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bankContainer);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);

		when(itemManager.canonicalize(anyInt())).thenAnswer(inv -> inv.getArgument(0));
		ItemComposition tradeableComp = mock(ItemComposition.class);
		when(tradeableComp.isGeTradeable()).thenReturn(true);
		when(itemManager.getItemComposition(anyInt())).thenReturn(tradeableComp);

		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	@Test
	public void testRunePouchAndInventoryRuneSummation()
	{
		// Player has Divine Rune Pouch in inventory + 100 Cosmic runes in inventory
		when(invContainer.getItems()).thenReturn(new Item[]{
			new Item(ItemID.DIVINE_RUNE_POUCH, 1),
			new Item(ItemID.COSMICRUNE, 100)
		});
		// Inside the Rune Pouch: Slot 1 = Cosmic (150), Slot 2 = Soul (5000), Slot 3 = Law (500), Slot 4 = Nature (1300)
		when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE1)).thenReturn(Runes.COSMIC.getId());
		when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT1)).thenReturn(150);
		when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE2)).thenReturn(Runes.SOUL.getId());
		when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT2)).thenReturn(5000);
		when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE3)).thenReturn(Runes.LAW.getId());
		when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT3)).thenReturn(500);
		when(client.getVarbitValue(Varbits.RUNE_POUCH_RUNE4)).thenReturn(Runes.NATURE.getId());
		when(client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT4)).thenReturn(1300);

		assertEquals(250, supplyCalculator.getRuneCountInPossession(Runes.COSMIC));
		assertEquals(5000, supplyCalculator.getRuneCountInPossession(Runes.SOUL));
		assertEquals(500, supplyCalculator.getRuneCountInPossession(Runes.LAW));
		assertEquals(1300, supplyCalculator.getRuneCountInPossession(Runes.NATURE));
	}

	@Test
	public void testMudBattlestaffSatisfiesEarthAndSkipsWaterRunes()
	{
		// Equip Mud Battlestaff -> covers both Earth and Water
		when(equipContainer.getItems()).thenReturn(new Item[]{
			new Item(ItemID.MUD_BATTLESTAFF, 1)
		});

		assertTrue(supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.GRAVEYARD));

		List<MtaSupplyCalculator.SupplyItem> checklist = supplyCalculator.calculateSupplies(0, 0, 0, 0, false);
		boolean hasWaterRuneRequirement = checklist.stream()
			.anyMatch(s -> s.getItemId() == ItemID.WATERRUNE);
		assertFalse("Water runes should not be required when Mud Battlestaff is owned", hasWaterRuneRequirement);
	}

	@Test
	public void testSupplyBatchMultiplierForGePurchase()
	{
		List<MtaSupplyCalculator.SupplyItem> missing1x = supplyCalculator.getMissingTradeableItems(0, 0, 0, 0, false, 1);
		List<MtaSupplyCalculator.SupplyItem> missing3x = supplyCalculator.getMissingTradeableItems(0, 0, 0, 0, false, 3);

		MtaSupplyCalculator.SupplyItem shark1 = missing1x.stream()
			.filter(i -> i.getItemId() == ItemID.SHARK)
			.findFirst()
			.orElse(null);
		MtaSupplyCalculator.SupplyItem shark3 = missing3x.stream()
			.filter(i -> i.getItemId() == ItemID.SHARK)
			.findFirst()
			.orElse(null);

		assertNotNull(shark1);
		assertNotNull(shark3);
		assertEquals(2, shark1.getGeBuyQuantity(1));
		assertEquals(6, shark3.getGeBuyQuantity(3));

		// Reusable staves should NOT multiply by runsToPurchase
		MtaSupplyCalculator.SupplyItem fireStaff3 = missing3x.stream()
			.filter(i -> i.getItemId() == ItemID.STAFF_OF_FIRE)
			.findFirst()
			.orElse(null);
		assertNotNull(fireStaff3);
		assertEquals(1, fireStaff3.getGeBuyQuantity(3));
	}

	@Test
	public void testGraveyardBoneScoringAndReadyToConvert()
	{
		Item[] items = new Item[]{
			new Item(ItemID.MAGICTRAINING_BONES1, 1), // 1
			new Item(ItemID.MAGICTRAINING_BONES2, 1), // 2
			new Item(ItemID.MAGICTRAINING_BONES3, 2), // 3 + 3 (unstackable in OSRS, test array entries)
			new Item(ItemID.MAGICTRAINING_BONES3, 1),
			new Item(ItemID.MAGICTRAINING_BONES4, 2), // 4
			new Item(ItemID.MAGICTRAINING_BONES4, 1)  // 4 -> total = 1+2+3+3+4+4 = 17
		};
		when(invContainer.getItems()).thenReturn(items);

		plugin.updateGraveyardRoomState();
		assertEquals(17, plugin.getGraveyardBoneScore());
		assertEquals(6, plugin.getGraveyardBoneCount());
		assertTrue(plugin.isGraveyardReadyToConvert());
	}

	@Test
	public void testAlchemyCupboardClockwiseFillAndProjection()
	{
		// Finding Emerald (ordinal 3) in cupboard #1 should populate all 6 cupboards clockwise
		plugin.fillCupboards(1, MtaAlchemyItem.EMERALD);

		assertEquals(MtaAlchemyItem.EMERALD, plugin.getCupboardItem(1));
		assertEquals(MtaAlchemyItem.RUNE_LONGSWORD, plugin.getCupboardItem(2));
		assertEquals(MtaAlchemyItem.EMPTY, plugin.getCupboardItem(3));
		assertEquals(MtaAlchemyItem.LEATHER_BOOTS, plugin.getCupboardItem(4));
		assertEquals(MtaAlchemyItem.ADAMANT_KITESHIELD, plugin.getCupboardItem(5));
		assertEquals(MtaAlchemyItem.ADAMANT_MED_HELM, plugin.getCupboardItem(0));

		// Test Alchemy point projection with held coins
		plugin.setRoomPoints(MtaRoomType.ALCHEMY, 285);
		when(invContainer.getItems()).thenReturn(new Item[]{
			new Item(ItemID.MAGICTRAINING_COINS, 1500) // +15 points
		});
		assertEquals(300, plugin.getProjectedAlchemyPoints());
		assertEquals(0, plugin.getRemaining30CoinAlchsNeeded());
	}

	@Test
	public void testDiaryStepProgression()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3164, 3489, 0)); // At GE

		// 1. Outside MTA Rooms -> Go to MTA & Choose a Room (MTA_LOBBY)
		plugin.syncVarbitsAndPoints();
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.MTA_LOBBY, plugin.getCurrentStep());

		// 1b. Standing inside MTA Lobby (Region 13363 / (3363, 3318, 0)) -> MTA_LOBBY
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3363, 3318, 0));
		when(client.getVarbitValue(VarbitID.MAGICTRAINING_ENTRA_NOOB)).thenReturn(1);
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.MTA_LOBBY, plugin.getCurrentStep());

		// 2. All 4 rooms complete -> BUY_BONES_TO_PEACHES
		plugin.setRoomPoints(MtaRoomType.TELEKINETIC, 200);
		plugin.setRoomPoints(MtaRoomType.ALCHEMY, 300);
		plugin.setRoomPoints(MtaRoomType.ENCHANTMENT, 2000);
		plugin.setRoomPoints(MtaRoomType.GRAVEYARD, 200);
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.BUY_BONES_TO_PEACHES, plugin.getCurrentStep());

		// 3. Bones to Peaches unlocked (Varbit 1505 = 1), outside Al Kharid Palace -> TRAVEL_TO_PALACE
		when(client.getVarbitValue(VarbitID.MAGICTRAINING_BONESPEACHES)).thenReturn(1);
		plugin.syncVarbitsAndPoints();
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.TRAVEL_TO_PALACE, plugin.getCurrentStep());

		// 4. Inside Al Kharid Palace with Bones -> CAST_BONES_TO_PEACHES
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3293, 3163, 0));
		when(invContainer.getItems()).thenReturn(new Item[]{new Item(ItemID.BONES, 1)});
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.CAST_BONES_TO_PEACHES, plugin.getCurrentStep());

		// 5. Diary bit 25 set -> DIARY_COMPLETE
		when(client.getVarpValue(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY)).thenReturn(1 << MtaPlugin.DIARY_BONES_TO_PEACHES_BIT);
		plugin.syncVarbitsAndPoints();
		plugin.updateCurrentStepAndInstructions();
		assertEquals(MtaStep.DIARY_COMPLETE, plugin.getCurrentStep());
	}

	@Test
	public void testMissingBankFilterIds()
	{
		when(bankContainer.count(ItemID.STAFF_OF_FIRE)).thenReturn(1);
		when(bankContainer.count(ItemID.MUD_BATTLESTAFF)).thenReturn(1);
		when(bankContainer.count(ItemID.SHARK)).thenReturn(20);

		Set<Integer> bankIds = supplyCalculator.getMissingBankItemIds(0, 0, 0, 0, false);
		assertTrue(bankIds.contains(ItemID.STAFF_OF_FIRE));
		assertTrue(bankIds.contains(ItemID.MUD_BATTLESTAFF));
		assertTrue(bankIds.contains(ItemID.SHARK));
	}
}
