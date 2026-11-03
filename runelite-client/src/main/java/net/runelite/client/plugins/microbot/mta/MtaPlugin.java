package net.runelite.client.plugins.microbot.mta;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

@PluginDescriptor(
	name = PluginDescriptor.Default + "MTA Lumbridge Diary Helper",
	description = "Step-by-step zero-automation helper for Mage Training Arena (Bones to Peaches) and Lumbridge & Draynor Hard Diary",
	tags = {"user", "mta", "magic", "training", "arena", "bones", "peaches", "lumbridge", "diary", "helper", "microbot"},
	enabledByDefault = false
)
@Slf4j
public class MtaPlugin extends Plugin
{
	public static final int MTA_ROOMS_REGION = 13462;
	public static final int MTA_TELEKINETIC_REGION = 13463;
	public static final int MTA_LOBBY_REGION = 13363;
	public static final int MTA_LOBBY_MIN_X = 3353;
	public static final int MTA_LOBBY_MAX_X = 3374;
	public static final int MTA_LOBBY_MIN_Y = 3296;
	public static final int MTA_LOBBY_MAX_Y = 3325;
	public static final int AL_KHARID_PALACE_MIN_X = 3282;
	public static final int AL_KHARID_PALACE_MAX_X = 3303;
	public static final int AL_KHARID_PALACE_MIN_Y = 3159;
	public static final int AL_KHARID_PALACE_MAX_Y = 3166;
	public static final int DIARY_BONES_TO_PEACHES_BIT = 25;

	private static final int SEARCHBOX_LOADED = 750;
	private static final int NUM_CUPBOARDS = 6;
	private static final int ALCH_INFO_ITEM_START = 7;
	private static final int ALCH_INFO_POINT_START = 12;
	private static final int ALCH_INFO_ARROW_START = 22;
	private static final int ALCH_INFO_LENGTH = 5;
	private static final int ALCH_BEST_POINTS = 30;
	private static final int GRAVEYARD_CONVERT_THRESHOLD = 16;
	private static final int ALCHEMY_COIN_CAP_WARNING = 10000;

	public enum FlashAlertType
	{
		NONE,
		ROOM_GOAL_REACHED,
		LOW_HP_GRAVEYARD,
		WRONG_STAFF_OR_CAP
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	@Getter
	private MtaConfig config;

	@Inject
	@Getter
	private MtaSupplyCalculator supplyCalculator;

	@Inject
	private MtaBankInterface bankInterface;

	@Inject
	private MtaGrandExchangeInterface geInterface;

	@Inject
	private MtaOverlay hudOverlay;

	@Inject
	private MtaSceneOverlay sceneOverlay;

	@Inject
	private MtaInventoryOverlay inventoryOverlay;

	@Inject
	private MtaFlashOverlay flashOverlay;

	@Getter
	private MtaStep currentStep = MtaStep.PREPARE_SUPPLIES;

	@Getter
	private String actionInstruction = "Checking supplies and diary progress...";

	@Getter
	private String subInstruction = "";

	@Getter
	private int telekineticPoints = 0;

	@Getter
	private int alchemistPoints = 0;

	@Getter
	private int enchantmentPoints = 0;

	@Getter
	private int graveyardPoints = 0;

	@Getter
	private boolean bonesToPeachesUnlocked = false;

	@Getter
	private boolean diaryTaskComplete = false;

	// Enchanting Room State
	@Getter
	private MtaEnchantShape bonusShape = MtaEnchantShape.CUBE;

	@Getter
	private final List<WorldPoint> dragonstoneSpawns = new ArrayList<>();

	// Alchemy Room State
	@Getter
	private MtaAlchemyItem bestAlchemyItem = null;

	@Getter
	private MtaAlchemyItem freeAlchemyItem = null;

	private final MtaAlchemyItem[] cupboardItems = new MtaAlchemyItem[NUM_CUPBOARDS];
	private final GameObject[] cupboardObjects = new GameObject[NUM_CUPBOARDS];
	private final Map<MtaAlchemyItem, Integer> previousAlchInvCounts = new EnumMap<>(MtaAlchemyItem.class);
	private int lastClickedCupboardIndex = -1;

	// Graveyard Room State
	@Getter
	private int graveyardBoneScore = 0;

	@Getter
	private int graveyardBoneCount = 0;

	@Getter
	private int graveyardFruitCount = 0;

	// Telekinetic Room State
	private final List<WallObject> telekineticWalls = new ArrayList<>();
	private int cachedMazeWallCount = 0;
	private Rectangle mazeBounds = null;

	@Getter
	private WorldPoint telekineticFinishLocation = null;

	@Getter
	private NPC mazeGuardian = null;

	private WorldPoint lastGuardianWorldPoint = null;

	@Getter
	private LocalPoint guardianMovingDestination = null;

	@Getter
	private Stack<Direction> telekineticMoves = new Stack<>();

	// Alert / Sound State
	private FlashAlertType lastAlertType = FlashAlertType.NONE;
	private Instant lastSoundAlertTime = null;
	private boolean goalAlertDismissedInCurrentRoom = false;

	@Provides
	MtaConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(MtaConfig.class);
	}

	@Override
	protected void startUp()
	{
		telekineticPoints = Math.max(0, config.savedTelekineticPoints());
		alchemistPoints = Math.max(0, config.savedAlchemistPoints());
		enchantmentPoints = Math.max(0, config.savedEnchantmentPoints());
		graveyardPoints = Math.max(0, config.savedGraveyardPoints());

		resetAlchemyRoom();
		resetTelekineticRoom();
		dragonstoneSpawns.clear();
		goalAlertDismissedInCurrentRoom = false;
		lastAlertType = FlashAlertType.NONE;
		lastSoundAlertTime = null;

		bankInterface.setMissingBankItemSupplier(() ->
			supplyCalculator.getMissingBankItemIds(
				telekineticPoints,
				alchemistPoints,
				enchantmentPoints,
				graveyardPoints,
				bonesToPeachesUnlocked
			)
		);
		geInterface.setMissingTradeableSupplier(() ->
			supplyCalculator.getMissingTradeableItems(
				telekineticPoints,
				alchemistPoints,
				enchantmentPoints,
				graveyardPoints,
				bonesToPeachesUnlocked,
				config.runsToPurchase()
			)
		);

		overlayManager.add(hudOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(inventoryOverlay);
		overlayManager.add(flashOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(hudOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(flashOverlay);

		bankInterface.destroy();
		geInterface.destroy();
		if (client != null)
		{
			client.clearHintArrow();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			dragonstoneSpawns.clear();
			resetTelekineticRoom();
			resetAlchemyRoom();
			goalAlertDismissedInCurrentRoom = false;
			if (client != null)
			{
				client.clearHintArrow();
			}
		}
		else if (event.getGameState() == GameState.LOGGED_IN && getActiveRoom() == null)
		{
			resetAlchemyRoom();
			resetTelekineticRoom();
			if (client != null)
			{
				client.clearHintArrow();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		syncVarbitsAndPoints();
		updateRoomSpecificState();
		updateCurrentStepAndInstructions();
		evaluateAudioAlerts();
	}

	public void syncVarbitsAndPoints()
	{
		bonesToPeachesUnlocked = client.getVarbitValue(VarbitID.MAGICTRAINING_BONESPEACHES) == 1;
		int diaryVarp = client.getVarpValue(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY);
		diaryTaskComplete = (diaryVarp & (1 << DIARY_BONES_TO_PEACHES_BIT)) != 0;

		// Read live points from whichever MTA interface is currently active
		readWidgetPoints(InterfaceID.MagictrainingTele.MAGICTRAIN_TELE_PTS, MtaRoomType.TELEKINETIC);
		readWidgetPoints(InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_PTS, MtaRoomType.ALCHEMY);
		readWidgetPoints(InterfaceID.MagictrainingEncha.MAGICTRAIN_ENCHA_PTS, MtaRoomType.ENCHANTMENT);
		readWidgetPoints(InterfaceID.MagictrainingGrave.MAGICTRAIN_GRAVE_PTS, MtaRoomType.GRAVEYARD);

		// Also check Pizazz Progress Hat main overview interface (Group 553)
		readWidgetPoints(InterfaceID.MagictrainingMain.MAGICTRAIN_TELE_POINTS, MtaRoomType.TELEKINETIC);
		readWidgetPoints(InterfaceID.MagictrainingMain.MAGICTRAIN_ALCH_POINTS, MtaRoomType.ALCHEMY);
		readWidgetPoints(InterfaceID.MagictrainingMain.MAGICTRAIN_ENCH_POINTS, MtaRoomType.ENCHANTMENT);
		readWidgetPoints(InterfaceID.MagictrainingMain.MAGICTRAIN_GRAVE_POINTS, MtaRoomType.GRAVEYARD);
	}

	private void readWidgetPoints(int packedWidgetId, MtaRoomType room)
	{
		Widget widget = client.getWidget(packedWidgetId);
		if (widget == null || widget.isHidden())
		{
			return;
		}
		int parsed = parsePointsText(widget.getText());
		if (parsed < 0)
		{
			return;
		}
		setRoomPoints(room, parsed);
	}

	public static int parsePointsText(String raw)
	{
		if (raw == null || raw.isEmpty())
		{
			return -1;
		}
		String digits = raw.replaceAll("[^0-9]", "");
		if (digits.isEmpty())
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(digits);
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	public void setRoomPoints(MtaRoomType room, int value)
	{
		int sanitized = Math.max(0, value);
		switch (room)
		{
			case TELEKINETIC:
				if (telekineticPoints != sanitized)
				{
					telekineticPoints = sanitized;
					saveConfigPoints("savedTelekineticPoints", sanitized);
				}
				break;
			case ALCHEMY:
				if (alchemistPoints != sanitized)
				{
					alchemistPoints = sanitized;
					saveConfigPoints("savedAlchemistPoints", sanitized);
				}
				break;
			case ENCHANTMENT:
				if (enchantmentPoints != sanitized)
				{
					enchantmentPoints = sanitized;
					saveConfigPoints("savedEnchantmentPoints", sanitized);
				}
				break;
			case GRAVEYARD:
				if (graveyardPoints != sanitized)
				{
					graveyardPoints = sanitized;
					saveConfigPoints("savedGraveyardPoints", sanitized);
				}
				break;
		}
	}

	private void saveConfigPoints(String key, int value)
	{
		if (configManager != null)
		{
			configManager.setConfiguration(MtaConfig.CONFIG_GROUP, key, value);
		}
	}

	public int getPointsForRoom(MtaRoomType room)
	{
		switch (room)
		{
			case TELEKINETIC:
				return telekineticPoints;
			case ALCHEMY:
				return alchemistPoints;
			case ENCHANTMENT:
				return enchantmentPoints;
			case GRAVEYARD:
				return graveyardPoints;
			default:
				return 0;
		}
	}

	public int getTargetPointsForRoom(MtaRoomType room)
	{
		switch (room)
		{
			case TELEKINETIC:
				return config.telekineticTargetPoints();
			case ALCHEMY:
				return config.alchemistTargetPoints();
			case ENCHANTMENT:
				return config.enchantmentTargetPoints();
			case GRAVEYARD:
				return config.graveyardTargetPoints();
			default:
				return 0;
		}
	}

	public boolean isRoomComplete(MtaRoomType room)
	{
		return bonesToPeachesUnlocked || getPointsForRoom(room) >= getTargetPointsForRoom(room);
	}

	public boolean areAllRoomsComplete()
	{
		return bonesToPeachesUnlocked
			|| (isRoomComplete(MtaRoomType.ENCHANTMENT)
				&& isRoomComplete(MtaRoomType.ALCHEMY)
				&& isRoomComplete(MtaRoomType.GRAVEYARD)
				&& isRoomComplete(MtaRoomType.TELEKINETIC));
	}

	public MtaRoomType getNextRecommendedRoom()
	{
		for (MtaRoomType room : MtaRoomType.values())
		{
			if (!isRoomComplete(room))
			{
				return room;
			}
		}
		return null;
	}

	public MtaRoomType getActiveRoom()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		WorldPoint loc = player.getWorldLocation();
		if (loc != null)
		{
			// Surface MTA Lobby / Upstairs (3353..3374, 3296..3325) is never an active room
			if (loc.getX() >= MTA_LOBBY_MIN_X
				&& loc.getX() <= MTA_LOBBY_MAX_X
				&& loc.getY() >= MTA_LOBBY_MIN_Y
				&& loc.getY() <= MTA_LOBBY_MAX_Y)
			{
				return null;
			}

			// Enchanting (plane 0), Graveyard (plane 1), and Alchemy (plane 2) are strictly in region 13462
			if (loc.getRegionID() == MTA_ROOMS_REGION)
			{
				int plane = loc.getPlane();
				if (plane == 0)
				{
					return MtaRoomType.ENCHANTMENT;
				}
				if (plane == 1)
				{
					return MtaRoomType.GRAVEYARD;
				}
				if (plane == 2)
				{
					return MtaRoomType.ALCHEMY;
				}
			}

			if (loc.getRegionID() == MTA_TELEKINETIC_REGION)
			{
				return MtaRoomType.TELEKINETIC;
			}
		}

		// Telekinetic Theatre uses an instanced region where MAGICTRAINING_TELE widget & maze walls/guardian exist
		Widget teleWidget = client.getWidget(InterfaceID.MAGICTRAINING_TELE, 0);
		if (teleWidget != null && !teleWidget.isHidden() && (!telekineticWalls.isEmpty() || mazeGuardian != null))
		{
			return MtaRoomType.TELEKINETIC;
		}

		return null;
	}

	public boolean isInMtaLobbyOrUpstairs()
	{
		if (getActiveRoom() != null)
		{
			return false;
		}

		Player player = client.getLocalPlayer();
		if (player != null && player.getWorldLocation() != null)
		{
			WorldPoint wp = player.getWorldLocation();
			if (wp.getX() >= MTA_LOBBY_MIN_X
				&& wp.getX() <= MTA_LOBBY_MAX_X
				&& wp.getY() >= MTA_LOBBY_MIN_Y
				&& wp.getY() <= MTA_LOBBY_MAX_Y)
			{
				return true;
			}
		}

		Widget shopWidget = client.getWidget(InterfaceID.MAGICTRAINING_SHOP, 0);
		return shopWidget != null && !shopWidget.isHidden();
	}

	public boolean isInAlKharidPalace()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation() == null)
		{
			return false;
		}
		WorldPoint wp = player.getWorldLocation();
		return wp.getPlane() == 0
			&& wp.getX() >= AL_KHARID_PALACE_MIN_X
			&& wp.getX() <= AL_KHARID_PALACE_MAX_X
			&& wp.getY() >= AL_KHARID_PALACE_MIN_Y
			&& wp.getY() <= AL_KHARID_PALACE_MAX_Y;
	}

	private void updateRoomSpecificState()
	{
		MtaRoomType room = getActiveRoom();
		if (room == null)
		{
			goalAlertDismissedInCurrentRoom = false;
			return;
		}

		switch (room)
		{
			case ENCHANTMENT:
				updateEnchantmentRoomState();
				break;
			case ALCHEMY:
				updateAlchemyRoomState();
				break;
			case GRAVEYARD:
				updateGraveyardRoomState();
				break;
			case TELEKINETIC:
				updateTelekineticRoomState();
				break;
		}
	}

	private void updateEnchantmentRoomState()
	{
		for (MtaEnchantShape shape : MtaEnchantShape.values())
		{
			Widget w = client.getWidget(shape.getBonusWidgetId());
			if (w != null && !w.isHidden())
			{
				bonusShape = shape;
				break;
			}
		}
	}

	private void updateAlchemyRoomState()
	{
		MtaAlchemyItem currentBest = null;
		MtaAlchemyItem currentFree = null;

		for (int i = 0; i < ALCH_INFO_LENGTH; i++)
		{
			Widget textWidget = client.getWidget(InterfaceID.MAGICTRAINING_ALCHEM, ALCH_INFO_ITEM_START + i);
			Widget pointsWidget = client.getWidget(InterfaceID.MAGICTRAINING_ALCHEM, ALCH_INFO_POINT_START + i);
			Widget arrowWidget = client.getWidget(InterfaceID.MAGICTRAINING_ALCHEM, ALCH_INFO_ARROW_START + i);
			if (textWidget == null || pointsWidget == null)
			{
				continue;
			}
			MtaAlchemyItem item = MtaAlchemyItem.findByName(textWidget.getText());
			int pts = parsePointsText(pointsWidget.getText());
			if (pts == ALCH_BEST_POINTS)
			{
				currentBest = item;
			}
			if (arrowWidget != null && !arrowWidget.isHidden())
			{
				currentFree = item;
			}
		}

		if (currentBest != null && currentBest != bestAlchemyItem)
		{
			bestAlchemyItem = currentBest;
			Arrays.fill(cupboardItems, MtaAlchemyItem.UNKNOWN);
		}
		freeAlchemyItem = currentFree;
	}

	public void fillCupboards(int startCupboardIndex, MtaAlchemyItem foundItem)
	{
		if (startCupboardIndex < 0 || startCupboardIndex >= NUM_CUPBOARDS || foundItem == null || foundItem == MtaAlchemyItem.UNKNOWN)
		{
			return;
		}
		int itemIdx = foundItem.ordinal();
		for (int i = 0; i < NUM_CUPBOARDS; i++)
		{
			int cupIdx = (startCupboardIndex + i) % NUM_CUPBOARDS;
			int mappedItemOrdinal = (itemIdx + i) % NUM_CUPBOARDS;
			cupboardItems[cupIdx] = mappedItemOrdinal <= 4
				? MtaAlchemyItem.values()[mappedItemOrdinal]
				: MtaAlchemyItem.EMPTY;
		}
	}

	public MtaAlchemyItem getCupboardItem(int index)
	{
		if (index < 0 || index >= NUM_CUPBOARDS)
		{
			return MtaAlchemyItem.UNKNOWN;
		}
		return cupboardItems[index] != null ? cupboardItems[index] : MtaAlchemyItem.UNKNOWN;
	}

	public GameObject getCupboardObject(int index)
	{
		if (index < 0 || index >= NUM_CUPBOARDS)
		{
			return null;
		}
		return cupboardObjects[index];
	}

	public int getKnownBestCupboardIndex()
	{
		if (bestAlchemyItem == null)
		{
			return -1;
		}
		for (int i = 0; i < NUM_CUPBOARDS; i++)
		{
			if (cupboardItems[i] == bestAlchemyItem)
			{
				return i;
			}
		}
		return -1;
	}

	public int getNearestUnknownCupboardIndex()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getWorldLocation() == null)
		{
			return -1;
		}
		WorldPoint myPos = local.getWorldLocation();
		int nearestIdx = -1;
		int bestDist = Integer.MAX_VALUE;

		for (int i = 0; i < NUM_CUPBOARDS; i++)
		{
			if (cupboardItems[i] != MtaAlchemyItem.UNKNOWN || cupboardObjects[i] == null)
			{
				continue;
			}
			int dist = cupboardObjects[i].getWorldLocation().distanceTo(myPos);
			if (dist < bestDist)
			{
				bestDist = dist;
				nearestIdx = i;
			}
		}
		return nearestIdx;
	}

	public int getAlchemyCoinsInInventory()
	{
		return supplyCalculator.countItemInContainer(InventoryID.INV, ItemID.MAGICTRAINING_COINS);
	}

	public int getProjectedAlchemyPoints()
	{
		return alchemistPoints + (getAlchemyCoinsInInventory() / 100);
	}

	public int getRemaining30CoinAlchsNeeded()
	{
		int targetCoins = Math.max(0, (config.alchemistTargetPoints() - alchemistPoints) * 100 - getAlchemyCoinsInInventory());
		return (int) Math.ceil(targetCoins / 30.0);
	}

	public void updateGraveyardRoomState()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		Item[] items = inv != null ? inv.getItems() : null;
		graveyardBoneScore = calculateGraveyardBoneScore(items);
		graveyardBoneCount = countGraveyardBones(items);
		graveyardFruitCount = countGraveyardFruit(items);
	}

	public static int calculateGraveyardBoneScore(Item[] items)
	{
		if (items == null)
		{
			return 0;
		}
		int total = 0;
		for (Item item : items)
		{
			if (item != null)
			{
				total += getBoneBananaValue(item.getId());
			}
		}
		return total;
	}

	public static int countGraveyardBones(Item[] items)
	{
		if (items == null)
		{
			return 0;
		}
		int count = 0;
		for (Item item : items)
		{
			if (item != null && getBoneBananaValue(item.getId()) > 0)
			{
				count++;
			}
		}
		return count;
	}

	public static int countGraveyardFruit(Item[] items)
	{
		if (items == null)
		{
			return 0;
		}
		int count = 0;
		for (Item item : items)
		{
			if (item != null && (item.getId() == ItemID.BANANA || item.getId() == ItemID.PEACH))
			{
				count += Math.max(1, item.getQuantity());
			}
		}
		return count;
	}

	public static int getBoneBananaValue(int itemId)
	{
		switch (itemId)
		{
			case ItemID.MAGICTRAINING_BONES1:
				return 1;
			case ItemID.MAGICTRAINING_BONES2:
				return 2;
			case ItemID.MAGICTRAINING_BONES3:
				return 3;
			case ItemID.MAGICTRAINING_BONES4:
				return 4;
			default:
				return 0;
		}
	}

	public int getProjectedGraveyardPoints()
	{
		int remainder = client != null ? client.getVarbitValue(VarbitID.MAGICTRAINING_ENCH_CONVCOUNT) : 0;
		int totalFruitIfConverted = remainder + graveyardFruitCount + graveyardBoneScore;
		return graveyardPoints + (totalFruitIfConverted / GRAVEYARD_CONVERT_THRESHOLD);
	}

	public boolean isGraveyardReadyToConvert()
	{
		if (graveyardBoneScore <= 0)
		{
			return false;
		}
		int convertibleSlots = supplyCalculator.getFreeInventorySlots() + graveyardBoneCount;
		return graveyardBoneScore >= GRAVEYARD_CONVERT_THRESHOLD
			|| graveyardBoneScore >= convertibleSlots
			|| getProjectedGraveyardPoints() >= config.graveyardTargetPoints();
	}

	public boolean isLowHpInGraveyard()
	{
		if (getActiveRoom() != MtaRoomType.GRAVEYARD)
		{
			return false;
		}
		int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		return currentHp > 0 && currentHp <= config.graveyardLowHpThreshold();
	}

	private void updateTelekineticRoomState()
	{
		if (telekineticWalls.size() != cachedMazeWallCount && !telekineticWalls.isEmpty())
		{
			mazeBounds = computeMazeBounds(telekineticWalls);
			cachedMazeWallCount = telekineticWalls.size();
			lastGuardianWorldPoint = null;
		}

		if (mazeGuardian == null || telekineticFinishLocation == null || mazeBounds == null)
		{
			telekineticMoves.clear();
			guardianMovingDestination = null;
			return;
		}

		if (mazeGuardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE)
		{
			telekineticMoves.clear();
			guardianMovingDestination = null;
			return;
		}

		WorldPoint currentGuardianPos;
		if (mazeGuardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_MOVING)
		{
			guardianMovingDestination = computeGuardianDestination(mazeGuardian);
			currentGuardianPos = guardianMovingDestination != null
				? WorldPoint.fromLocal(client, guardianMovingDestination)
				: mazeGuardian.getWorldLocation();
		}
		else
		{
			guardianMovingDestination = null;
			currentGuardianPos = mazeGuardian.getWorldLocation();
		}

		if (currentGuardianPos != null && !currentGuardianPos.equals(lastGuardianWorldPoint))
		{
			lastGuardianWorldPoint = currentGuardianPos;
			if (currentGuardianPos.equals(telekineticFinishLocation))
			{
				telekineticMoves.clear();
			}
			else
			{
				telekineticMoves = solveTelekineticMaze(currentGuardianPos);
			}
		}
	}

	public Direction getRequiredTelekineticDirection()
	{
		if (telekineticMoves == null || telekineticMoves.isEmpty())
		{
			return null;
		}
		return telekineticMoves.peek();
	}

	public Direction getPlayerMazeSideDirection()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getWorldLocation() == null || mazeBounds == null)
		{
			return null;
		}
		WorldPoint mine = local.getWorldLocation();
		if (mine.getY() >= mazeBounds.getMaxY() && mine.getX() < mazeBounds.getMaxX() && mine.getX() > mazeBounds.getX())
		{
			return Direction.NORTH;
		}
		if (mine.getY() <= mazeBounds.getY() && mine.getX() < mazeBounds.getMaxX() && mine.getX() > mazeBounds.getX())
		{
			return Direction.SOUTH;
		}
		if (mine.getX() >= mazeBounds.getMaxX() && mine.getY() < mazeBounds.getMaxY() && mine.getY() > mazeBounds.getY())
		{
			return Direction.EAST;
		}
		if (mine.getX() <= mazeBounds.getX() && mine.getY() < mazeBounds.getMaxY() && mine.getY() > mazeBounds.getY())
		{
			return Direction.WEST;
		}
		return null;
	}

	public WorldPoint getOptimalTelekineticStandTile(int moveIndex)
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getWorldLocation() == null || mazeBounds == null)
		{
			return null;
		}
		if (telekineticMoves == null || telekineticMoves.size() <= moveIndex)
		{
			return null;
		}

		WorldPoint currentPos = local.getWorldLocation();
		Direction next = telekineticMoves.get(telekineticMoves.size() - 1 - moveIndex);
		WorldArea areaNext = getIndicatorLine(next);
		if (areaNext == null)
		{
			return null;
		}
		WorldPoint nearestNext = nearestPointInArea(areaNext, currentPos);

		if (telekineticMoves.size() <= 1 + moveIndex)
		{
			return nearestNext;
		}

		Direction after = telekineticMoves.get(telekineticMoves.size() - 2 - moveIndex);
		WorldArea areaAfter = getIndicatorLine(after);
		if (areaAfter == null)
		{
			return nearestNext;
		}
		WorldPoint nearestAfter = nearestPointInArea(areaAfter, nearestNext);
		return nearestPointInArea(areaNext, nearestAfter);
	}

	private WorldArea getIndicatorLine(Direction direction)
	{
		if (mazeBounds == null || direction == null)
		{
			return null;
		}
		switch (direction)
		{
			case NORTH:
				return new WorldArea(mazeBounds.x + 1, (int) mazeBounds.getMaxY(), mazeBounds.width - 1, 1, 0);
			case SOUTH:
				return new WorldArea(mazeBounds.x + 1, mazeBounds.y, mazeBounds.width - 1, 1, 0);
			case WEST:
				return new WorldArea(mazeBounds.x, mazeBounds.y + 1, 1, mazeBounds.height - 1, 0);
			case EAST:
				return new WorldArea((int) mazeBounds.getMaxX(), mazeBounds.y + 1, 1, mazeBounds.height - 1, 0);
			default:
				return null;
		}
	}

	private static WorldPoint nearestPointInArea(WorldArea area, WorldPoint target)
	{
		int bestDist = Integer.MAX_VALUE;
		WorldPoint nearest = null;
		for (WorldPoint p : area.toWorldPointList())
		{
			int d = Math.abs(p.getX() - target.getX()) + Math.abs(p.getY() - target.getY());
			if (nearest == null || d < bestDist)
			{
				bestDist = d;
				nearest = p;
			}
		}
		return nearest;
	}

	private LocalPoint computeGuardianDestination(NPC guardianNpc)
	{
		if (guardianNpc == null || guardianNpc.getLocalLocation() == null)
		{
			return null;
		}
		Angle angle = new Angle(guardianNpc.getOrientation());
		Direction facing = angle.getNearestDirection();
		return slideNeighbour(guardianNpc.getLocalLocation(), facing);
	}

	private Stack<Direction> solveTelekineticMaze(WorldPoint start)
	{
		Queue<WorldPoint> visit = new LinkedList<>();
		Set<WorldPoint> closed = new HashSet<>();
		Map<WorldPoint, Integer> scores = new HashMap<>();
		Map<WorldPoint, WorldPoint> edges = new HashMap<>();
		scores.put(start, 0);
		visit.add(start);

		while (!visit.isEmpty())
		{
			WorldPoint next = visit.poll();
			closed.add(next);

			LocalPoint localNext = LocalPoint.fromWorld(client, next);
			if (localNext == null)
			{
				continue;
			}

			for (Direction dir : Direction.values())
			{
				LocalPoint neighbour = slideNeighbour(localNext, dir);
				if (neighbour == null)
				{
					continue;
				}
				WorldPoint nghbWorld = WorldPoint.fromLocal(client, neighbour);
				if (nghbWorld != null && !nghbWorld.equals(next) && !closed.contains(nghbWorld))
				{
					int score = scores.get(next) + 1;
					if (!scores.containsKey(nghbWorld) || scores.get(nghbWorld) > score)
					{
						scores.put(nghbWorld, score);
						edges.put(nghbWorld, next);
						visit.add(nghbWorld);
					}
				}
			}
		}

		Stack<Direction> path = new Stack<>();
		WorldPoint current = telekineticFinishLocation;
		while (current != null && edges.containsKey(current))
		{
			WorldPoint prev = edges.get(current);
			if (prev.getX() > current.getX())
			{
				path.add(Direction.WEST);
			}
			else if (prev.getX() < current.getX())
			{
				path.add(Direction.EAST);
			}
			else if (prev.getY() > current.getY())
			{
				path.add(Direction.SOUTH);
			}
			else
			{
				path.add(Direction.NORTH);
			}
			current = prev;
		}
		return path;
	}

	private LocalPoint slideNeighbour(LocalPoint point, Direction direction)
	{
		if (point == null || client.getTopLevelWorldView() == null)
		{
			return null;
		}
		WorldPoint worldPoint = WorldPoint.fromLocal(client, point);
		if (worldPoint == null)
		{
			return null;
		}
		WorldArea area = worldPoint.toWorldArea();
		int dx;
		int dy;
		switch (direction)
		{
			case NORTH:
				dx = 0;
				dy = 1;
				break;
			case SOUTH:
				dx = 0;
				dy = -1;
				break;
			case EAST:
				dx = 1;
				dy = 0;
				break;
			case WEST:
				dx = -1;
				dy = 0;
				break;
			default:
				return null;
		}

		int safety = 0;
		while (safety++ < 30 && area.canTravelInDirection(client.getTopLevelWorldView(), dx, dy))
		{
			worldPoint = area.toWorldPoint().dx(dx).dy(dy);
			area = worldPoint.toWorldArea();
		}
		return LocalPoint.fromWorld(client, worldPoint);
	}

	private static Rectangle computeMazeBounds(List<WallObject> walls)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (WallObject wall : walls)
		{
			WorldPoint point = wall.getWorldLocation();
			minX = Math.min(minX, point.getX());
			minY = Math.min(minY, point.getY());
			maxX = Math.max(maxX, point.getX());
			maxY = Math.max(maxY, point.getY());
		}
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public void updateCurrentStepAndInstructions()
	{
		subInstruction = "";

		// 1. Check if the entire Lumbridge & Draynor Hard Diary task is completed!
		if (diaryTaskComplete && config.trackAlKharidPalaceCast())
		{
			currentStep = MtaStep.DIARY_COMPLETE;
			actionInstruction = "Done! Bones to Peaches cast in Al Kharid Palace!";
			subInstruction = "Lumbridge & Draynor Hard Diary task is complete.";
			return;
		}

		// 2. Check if Bones to Peaches is unlocked!
		if (bonesToPeachesUnlocked)
		{
			if (!config.trackAlKharidPalaceCast())
			{
				currentStep = MtaStep.DIARY_COMPLETE;
				actionInstruction = "Done! Bones to Peaches is unlocked!";
				return;
			}

			boolean hasBones = supplyCalculator.countInInventoryOrEquipment(Set.of(ItemID.BONES, ItemID.BIG_BONES)) > 0;
			if (isInAlKharidPalace())
			{
				currentStep = MtaStep.CAST_BONES_TO_PEACHES;
				if (!hasBones)
				{
					actionInstruction = "Get 1 Bones (kill a Guard/Man or withdraw from Al Kharid Bank)";
					subInstruction = "You must hold at least 1 Bones to cast Bones to Peaches.";
				}
				else if (!supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.GRAVEYARD)
					&& !supplyCalculator.getInventoryStaffIdsForRoom(MtaRoomType.GRAVEYARD).isEmpty())
				{
					actionInstruction = "Equip your Earth / Mud Staff in inventory";
					subInstruction = "Then click Bones to Peaches in your Magic Spellbook!";
				}
				else
				{
					actionInstruction = "Click 'Bones to Peaches' in your Magic Spellbook!";
					subInstruction = "You are inside Al Kharid Palace with Bones ready.";
				}
			}
			else
			{
				currentStep = MtaStep.TRAVEL_TO_PALACE;
				actionInstruction = hasBones
					? "Run south to Al Kharid Palace and step inside"
					: "Withdraw 1 Bones from bank, then enter Al Kharid Palace";
				subInstruction = "Need 1 Bones + 2 Nature + 4 Water + 4 Earth runes (or Mud Staff).";
			}
			return;
		}

		// 3. Check if player is currently inside one of the 4 MTA rooms
		MtaRoomType activeRoom = getActiveRoom();
		if (activeRoom != null)
		{
			switch (activeRoom)
			{
				case ENCHANTMENT:
					currentStep = MtaStep.IN_ENCHANTMENT_ROOM;
					updateEnchantmentInstructions();
					return;
				case ALCHEMY:
					currentStep = MtaStep.IN_ALCHEMY_ROOM;
					updateAlchemyInstructions();
					return;
				case GRAVEYARD:
					currentStep = MtaStep.IN_GRAVEYARD_ROOM;
					updateGraveyardInstructions();
					return;
				case TELEKINETIC:
					currentStep = MtaStep.IN_TELEKINETIC_ROOM;
					updateTelekineticInstructions();
					return;
			}
		}

		// 4. Check if all 4 rooms have reached their target points -> Buy Bones to Peaches!
		if (areAllRoomsComplete())
		{
			currentStep = MtaStep.BUY_BONES_TO_PEACHES;
			Widget shopWidget = client.getWidget(InterfaceID.MAGICTRAINING_SHOP, 0);
			if (shopWidget != null && !shopWidget.isHidden())
			{
				actionInstruction = "Click 'Bones to Peaches' icon -> Click Confirm button!";
				subInstruction = "Unlocks the spell permanently (200T / 300A / 2000E / 200G).";
			}
			else if (client.getLocalPlayer() != null && client.getLocalPlayer().getWorldLocation().getPlane() == 1)
			{
				actionInstruction = "Click 'Trade-with' on the Rewards Guardian (Green)";
				subInstruction = "Buy the Bones to Peaches spell from his shop.";
			}
			else
			{
				actionInstruction = "All 4 rooms complete! Climb the Stairs to Upstairs";
				subInstruction = "The Rewards Guardian is on the upper floor of the MTA lobby.";
			}
			return;
		}

		// 5. Not inside any of the 4 rooms: Guide player to go to MTA and choose a room
		currentStep = MtaStep.MTA_LOBBY;
		MtaRoomType nextRoom = getNextRecommendedRoom();
		if (isInMtaLobbyOrUpstairs())
		{
			if (nextRoom != null)
			{
				actionInstruction = "Choose a room: Click the " + nextRoom.getRoomName() + " Teleport (Green)";
				subInstruction = "Equip: " + nextRoom.getRecommendedStaffName() + " | Goal: "
					+ getPointsForRoom(nextRoom) + " / " + getTargetPointsForRoom(nextRoom) + " pts";
			}
			else
			{
				actionInstruction = "Choose a room portal inside the Magic Training Arena";
			}
		}
		else
		{
			actionInstruction = "Go to the Magic Training Arena and choose a room";
			if (nextRoom != null)
			{
				subInstruction = "Next Recommended: " + nextRoom.getRoomName() + " ("
					+ getPointsForRoom(nextRoom) + " / " + getTargetPointsForRoom(nextRoom)
					+ " pts) — Equip " + nextRoom.getRecommendedStaffName();
			}
			else
			{
				subInstruction = "Enter the Mage Training Arena north of Emir's Arena.";
			}
		}
	}

	private void updateEnchantmentInstructions()
	{
		if (isRoomComplete(MtaRoomType.ENCHANTMENT))
		{
			actionInstruction = "✓ 2,000 PTS REACHED! Click the Exit Teleport portal!";
			subInstruction = "Enchanting Chamber complete — return to lobby.";
			return;
		}

		int dstoneInv = supplyCalculator.countItemInContainer(InventoryID.INV, ItemID.MAGICTRAINING_DRAGONSTONE);
		int bonusInv = bonusShape != null
			? supplyCalculator.countItemInContainer(InventoryID.INV, bonusShape.getItemId())
			: 0;
		int totalShapesInv = countAllEnchantShapesInInventory();
		int orbsInv = supplyCalculator.countItemInContainer(InventoryID.INV, ItemID.MAGICTRAINING_ENCHAN_SHAPEORB);
		int freeSlots = supplyCalculator.getFreeInventorySlots();

		if (dstoneInv > 0)
		{
			actionInstruction = "Cast Lvl-7 Enchant on Dragonstones in inventory (+14 pts!)";
			subInstruction = "Dragonstones give double points! Click Lvl-7 Enchant -> Dragonstone.";
		}
		else if (!dragonstoneSpawns.isEmpty() && freeSlots > 0)
		{
			actionInstruction = "Click the highlighted Dragonstone on the floor (14 pts!)";
			subInstruction = dragonstoneSpawns.size() + " Dragonstone(s) available on the ground.";
		}
		else if (bonusInv > 0)
		{
			actionInstruction = "Cast Lvl-7 Enchant on " + bonusShape.getDisplayName() + " in inventory (+9 pts)";
			subInstruction = "Bonus shape: " + bonusShape.getDisplayName() + " (" + bonusInv + " in inventory).";
		}
		else if (freeSlots == 0 && orbsInv > 0 && totalShapesInv == 0)
		{
			actionInstruction = "Click the center Hole (Green) to deposit Orbs (+3 pts/20)";
			subInstruction = "Inventory is full of Orbs (" + orbsInv + "). Deposit to free space!";
		}
		else if (totalShapesInv > 0 && freeSlots == 0)
		{
			actionInstruction = "Cast Lvl-7 Enchant on shapes in inventory (or click new Bonus Pile)";
			subInstruction = "Current Bonus Shape is " + (bonusShape != null ? bonusShape.getDisplayName() : "Unknown") + ".";
		}
		else if (bonusShape != null)
		{
			actionInstruction = "Click the " + bonusShape.getDisplayName() + " Pile (Green) to take shapes";
			subInstruction = "Bonus shape gives +2 extra points per cast (9 pts with Lvl-7 Enchant).";
		}
	}

	public int countAllEnchantShapesInInventory()
	{
		int count = 0;
		for (MtaEnchantShape shape : MtaEnchantShape.values())
		{
			count += supplyCalculator.countItemInContainer(InventoryID.INV, shape.getItemId());
		}
		return count;
	}

	private void updateAlchemyInstructions()
	{
		if (isRoomComplete(MtaRoomType.ALCHEMY))
		{
			actionInstruction = "✓ 300 PTS REACHED! Click the Exit Teleport portal!";
			subInstruction = "Alchemist's Playground complete — return to lobby.";
			return;
		}

		int coins = getAlchemyCoinsInInventory();
		int projected = getProjectedAlchemyPoints();
		if (projected >= config.alchemistTargetPoints())
		{
			actionInstruction = "✓ 300 PTS IN COINS HELD! Click the Coin Collector (Green)!";
			subInstruction = "You hold " + coins + " coins (+" + (coins / 100) + " pts) — deposit now to finish!";
			return;
		}

		if (!supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.ALCHEMY))
		{
			actionInstruction = "Step 0: Equip your Fire Staff in inventory (Highlighted)!";
			subInstruction = "High Level Alchemy requires a Fire Staff equipped.";
			return;
		}

		if (coins >= ALCHEMY_COIN_CAP_WARNING)
		{
			actionInstruction = "WARNING: " + coins + " Coins! Click Coin Collector NOW!";
			subInstruction = "Holding 12,000+ coins kicks you out and confiscates your coins!";
			return;
		}

		String bestName = bestAlchemyItem != null ? bestAlchemyItem.getName() : "30-coin item";
		int bestCountInInv = bestAlchemyItem != null
			? supplyCalculator.countItemInContainer(InventoryID.INV, bestAlchemyItem.getItemId())
			: 0;

		if (bestCountInInv > 0)
		{
			actionInstruction = "Cast High Level Alchemy on " + bestName + " in inventory (30c)";
			subInstruction = "Need " + getRemaining30CoinAlchsNeeded() + " more 30c alchs for " + config.alchemistTargetPoints() + " pts.";
		}
		else if (getKnownBestCupboardIndex() >= 0)
		{
			int cupNum = getKnownBestCupboardIndex() + 1;
			actionInstruction = "Click Cupboard #" + cupNum + " (Green) to take " + bestName + " (30c)";
			subInstruction = "Grab several " + bestName + "s and High Alch them for 30 coins each.";
		}
		else
		{
			actionInstruction = "Click the highlighted Cupboard (Cyan) to locate " + bestName;
			subInstruction = "Searching 1 cupboard automatically reveals all 6 cupboards!";
		}
	}

	private void updateGraveyardInstructions()
	{
		if (isRoomComplete(MtaRoomType.GRAVEYARD))
		{
			actionInstruction = "✓ 200 PTS REACHED! Click the Exit Teleport portal!";
			subInstruction = "Creature Graveyard complete — return to lobby.";
			return;
		}

		if (isLowHpInGraveyard())
		{
			int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			actionInstruction = "LOW HP (" + hp + ")! Click Food / Bananas in inventory to heal!";
			subInstruction = "Falling bones deal 2 damage periodically — stay above " + config.graveyardLowHpThreshold() + " HP.";
			return;
		}

		if (!supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.GRAVEYARD))
		{
			actionInstruction = "Step 0: Equip your Earth / Mud Staff in inventory!";
			subInstruction = "Bones to Bananas requires Earth + Water + Nature runes.";
			return;
		}

		if (graveyardFruitCount > 0 && graveyardBoneScore == 0)
		{
			actionInstruction = "Step 3: Click the Food Chute on the wall (Green) to deposit!";
			subInstruction = "Depositing " + graveyardFruitCount + " fruit (16 fruit = 1 Graveyard point).";
			return;
		}

		if (isGraveyardReadyToConvert())
		{
			String spellName = bonesToPeachesUnlocked ? "Bones to Peaches" : "Bones to Bananas";
			actionInstruction = "Step 2: STOP grabbing bones! Cast " + spellName + " NOW!";
			subInstruction = "Your " + graveyardBoneCount + " bones will convert into " + graveyardBoneScore + " fruit!";
			return;
		}

		actionInstruction = "Step 1: Click the highlighted Bone Pile (Green) to grab bones";
		subInstruction = "Current Bone Value: " + graveyardBoneScore + " / 16 Bananas (Convert at 16+).";
	}

	private void updateTelekineticInstructions()
	{
		if (isRoomComplete(MtaRoomType.TELEKINETIC))
		{
			actionInstruction = "✓ 200 PTS REACHED! Click the Exit Teleport portal!";
			subInstruction = "Telekinetic Theatre complete — return to lobby.";
			return;
		}

		if (!supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.TELEKINETIC))
		{
			actionInstruction = "Step 0: Equip your Air Staff in inventory (Highlighted)!";
			subInstruction = "Telekinetic Grab requires 1 Air + 1 Law rune.";
			return;
		}

		if (mazeGuardian != null && mazeGuardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE)
		{
			actionInstruction = "Step 3: Maze Solved! Click 'New-maze' on Maze Guardian (Green)!";
			subInstruction = "Every 5th maze awards a +8 point bonus (18 pts per 5 mazes).";
			return;
		}

		Direction reqDir = getRequiredTelekineticDirection();
		Direction myDir = getPlayerMazeSideDirection();

		if (mazeGuardian != null && mazeGuardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_MOVING)
		{
			actionInstruction = "Statue sliding... Run to the Green/Cyan tile for the next pull!";
			subInstruction = reqDir != null ? "Next pull side: " + reqDir.name() : "Statue reaching goal...";
			return;
		}

		if (reqDir != null && myDir == reqDir)
		{
			actionInstruction = "Step 2: IN POSITION! Cast Telekinetic Grab on Maze Guardian!";
			subInstruction = "Click Telekinetic Grab in spellbook -> Click the Maze Guardian statue.";
		}
		else if (reqDir != null)
		{
			actionInstruction = "Step 1: Run to the GREEN tile on the " + reqDir.name() + " side!";
			subInstruction = "Moves remaining to solve maze: " + telekineticMoves.size();
		}
		else
		{
			actionInstruction = "Observe or talk to the Maze Guardian to begin";
		}
	}

	public boolean isCurrentRoomGoalReachedIncludingHeldItems()
	{
		MtaRoomType room = getActiveRoom();
		if (room == null)
		{
			return false;
		}
		switch (room)
		{
			case ENCHANTMENT:
				return isRoomComplete(MtaRoomType.ENCHANTMENT);
			case ALCHEMY:
				return isRoomComplete(MtaRoomType.ALCHEMY)
					|| getProjectedAlchemyPoints() >= config.alchemistTargetPoints();
			case GRAVEYARD:
				return isRoomComplete(MtaRoomType.GRAVEYARD)
					|| getProjectedGraveyardPoints() >= config.graveyardTargetPoints();
			case TELEKINETIC:
				return isRoomComplete(MtaRoomType.TELEKINETIC);
			default:
				return false;
		}
	}

	public FlashAlertType getActiveFlashAlert()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return FlashAlertType.NONE;
		}

		// 1. Low HP in Graveyard takes top safety priority
		if (config.flashOnLowHp() && isLowHpInGraveyard())
		{
			return FlashAlertType.LOW_HP_GRAVEYARD;
		}

		// 2. Room Goal Reached (including held coins/bones)
		if (config.flashOnRoomGoalReached()
			&& !goalAlertDismissedInCurrentRoom
			&& isCurrentRoomGoalReachedIncludingHeldItems())
		{
			return FlashAlertType.ROOM_GOAL_REACHED;
		}

		// 3. Wrong Elemental Staff Equipped or 10k+ Alchemy Coin Cap Warning
		MtaRoomType room = getActiveRoom();
		if (room != null)
		{
			if (config.flashOnWrongStaff() && !supplyCalculator.hasRequiredStaffEquippedForRoom(room))
			{
				return FlashAlertType.WRONG_STAFF_OR_CAP;
			}
			if (room == MtaRoomType.ALCHEMY && getAlchemyCoinsInInventory() >= ALCHEMY_COIN_CAP_WARNING)
			{
				return FlashAlertType.WRONG_STAFF_OR_CAP;
			}
		}

		return FlashAlertType.NONE;
	}

	private void evaluateAudioAlerts()
	{
		FlashAlertType activeAlert = getActiveFlashAlert();
		if (activeAlert == FlashAlertType.NONE)
		{
			lastAlertType = FlashAlertType.NONE;
			lastSoundAlertTime = null;
			return;
		}

		if (!config.playAlertSounds())
		{
			lastAlertType = activeAlert;
			return;
		}

		Instant now = Instant.now();
		if (activeAlert != lastAlertType || lastSoundAlertTime == null)
		{
			lastAlertType = activeAlert;
			lastSoundAlertTime = now;
			Rs2SoundAlert.triggerAlert(config.customSoundPath(), config.soundVolume());
			return;
		}

		int repeatSec = config.soundIntervalSeconds();
		if (repeatSec > 0 && Duration.between(lastSoundAlertTime, now).getSeconds() >= repeatSec)
		{
			lastSoundAlertTime = now;
			Rs2SoundAlert.triggerAlert(config.customSoundPath(), config.soundVolume());
		}
	}

	// --- Event Subscriptions ---

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		String target = event.getMenuTarget();

		if (option != null)
		{
			if (option.contains("Exit") || option.contains("Teleport") || option.contains("Deposit"))
			{
				goalAlertDismissedInCurrentRoom = true;
			}
		}

		if (getActiveRoom() == MtaRoomType.ALCHEMY && target != null && target.toLowerCase().contains("cupboard"))
		{
			int nearestIdx = findNearestCupboardIndexToPlayer();
			if (nearestIdx >= 0)
			{
				lastClickedCupboardIndex = nearestIdx;
			}
		}

		bankInterface.handleClick(event);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String msg = event.getMessage();
		if (msg == null)
		{
			return;
		}

		if (getActiveRoom() == MtaRoomType.ALCHEMY)
		{
			if (msg.contains("You found:"))
			{
				String itemText = msg.replace("You found:", "").trim();
				MtaAlchemyItem found = MtaAlchemyItem.findByName(itemText);
				int idx = lastClickedCupboardIndex >= 0 ? lastClickedCupboardIndex : findNearestCupboardIndexToPlayer();
				if (found != null && idx >= 0)
				{
					fillCupboards(idx, found);
				}
			}
			else if (msg.contains("The cupboard is empty."))
			{
				int idx = lastClickedCupboardIndex >= 0 ? lastClickedCupboardIndex : findNearestCupboardIndexToPlayer();
				if (idx >= 0)
				{
					fillCupboards(idx, MtaAlchemyItem.EMPTY);
				}
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		bankInterface.onItemContainerChanged(event);

		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		ItemContainer inv = event.getItemContainer();
		if (inv == null)
		{
			return;
		}

		if (getActiveRoom() == MtaRoomType.ALCHEMY)
		{
			for (MtaAlchemyItem alchItem : MtaAlchemyItem.values())
			{
				if (alchItem == MtaAlchemyItem.EMPTY || alchItem == MtaAlchemyItem.UNKNOWN)
				{
					continue;
				}
				int newCount = inv.count(alchItem.getItemId());
				int oldCount = previousAlchInvCounts.getOrDefault(alchItem, 0);
				if (newCount > oldCount)
				{
					int idx = lastClickedCupboardIndex >= 0 ? lastClickedCupboardIndex : findNearestCupboardIndexToPlayer();
					if (idx >= 0)
					{
						fillCupboards(idx, alchItem);
					}
				}
				previousAlchInvCounts.put(alchItem, newCount);
			}
		}
		else if (getActiveRoom() == MtaRoomType.GRAVEYARD)
		{
			updateGraveyardRoomState();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		if (obj == null)
		{
			return;
		}
		int cupboardIdx = getCupboardIndexFromObjectId(obj.getId());
		if (cupboardIdx >= 0)
		{
			cupboardObjects[cupboardIdx] = obj;
			if (cupboardItems[cupboardIdx] == null)
			{
				cupboardItems[cupboardIdx] = MtaAlchemyItem.UNKNOWN;
			}
		}
	}

	public static int getCupboardIndexFromObjectId(int objectId)
	{
		switch (objectId)
		{
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD0:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD0_OPEN:
				return 0;
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD1:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD1_OPEN:
				return 1;
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD2:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD2_OPEN:
				return 2;
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD3:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD3_OPEN:
				return 3;
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD4:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD4_OPEN:
				return 4;
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD5:
			case ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD5_OPEN:
				return 5;
			default:
				return -1;
		}
	}

	private int findNearestCupboardIndexToPlayer()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getWorldLocation() == null)
		{
			return -1;
		}
		WorldPoint mine = local.getWorldLocation();
		int bestIdx = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < NUM_CUPBOARDS; i++)
		{
			if (cupboardObjects[i] == null)
			{
				continue;
			}
			int dist = cupboardObjects[i].getWorldLocation().distanceTo(mine);
			if (dist < bestDist)
			{
				bestDist = dist;
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		TileItem item = event.getItem();
		Tile tile = event.getTile();
		if (item != null && tile != null && item.getId() == ItemID.MAGICTRAINING_DRAGONSTONE)
		{
			WorldPoint wp = tile.getWorldLocation();
			if (wp != null && !dragonstoneSpawns.contains(wp))
			{
				dragonstoneSpawns.add(wp);
			}
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		TileItem item = event.getItem();
		Tile tile = event.getTile();
		if (item != null && tile != null && item.getId() == ItemID.MAGICTRAINING_DRAGONSTONE)
		{
			dragonstoneSpawns.remove(tile.getWorldLocation());
		}
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		WallObject wall = event.getWallObject();
		if (wall != null && wall.getId() == ObjectID.MAGICTRAINING_MINIWALL)
		{
			telekineticWalls.add(wall);
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		GroundObject ground = event.getGroundObject();
		if (ground != null && ground.getId() == ObjectID.MAGICTRAINING_MINI_STATUE_PITFALL)
		{
			telekineticFinishLocation = ground.getWorldLocation();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc == null)
		{
			return;
		}
		int id = npc.getId();
		if (id == NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE
			|| id == NpcID.MAGICTRAINING_GUARD_MAZE_MOVING
			|| id == NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE)
		{
			mazeGuardian = npc;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == mazeGuardian)
		{
			mazeGuardian = null;
		}
	}

	@Subscribe(priority = -1)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		bankInterface.onScriptCallbackEvent(event);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		bankInterface.onScriptPreFired(event);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		bankInterface.onScriptPostFired(event);
		if (event.getScriptId() == SEARCHBOX_LOADED && geInterface.isAtGrandExchange())
		{
			geInterface.init();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		bankInterface.onWidgetLoaded(event);
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		bankInterface.onWidgetClosed(event);
	}

	@Subscribe
	public void onGrandExchangeSearched(GrandExchangeSearched event)
	{
		geInterface.onGrandExchangeSearched(event);
	}

	private void resetAlchemyRoom()
	{
		Arrays.fill(cupboardItems, MtaAlchemyItem.UNKNOWN);
		Arrays.fill(cupboardObjects, null);
		previousAlchInvCounts.clear();
		lastClickedCupboardIndex = -1;
	}

	private void resetTelekineticRoom()
	{
		telekineticWalls.clear();
		cachedMazeWallCount = 0;
		mazeBounds = null;
		telekineticFinishLocation = null;
		mazeGuardian = null;
		lastGuardianWorldPoint = null;
		guardianMovingDestination = null;
		telekineticMoves.clear();
	}
}
