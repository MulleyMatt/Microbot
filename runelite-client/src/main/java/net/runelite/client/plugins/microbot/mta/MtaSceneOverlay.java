package net.runelite.client.plugins.microbot.mta;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

public class MtaSceneOverlay extends Overlay
{
	private static final Color ACTION_GREEN = new Color(0, 255, 100);
	private static final Color NEXT_CYAN = new Color(0, 210, 255);
	private static final Color WARN_ORANGE = new Color(255, 165, 0);
	private static final Color DANGER_RED = new Color(255, 60, 60);
	private static final Color DIM_GRAY = new Color(150, 150, 150, 140);

	private static final Set<Integer> BONE_PILE_IDS = Set.of(
		ObjectID.MAGICTRAINING_BONES_PILE1,
		ObjectID.MAGICTRAINING_BONES_PILE2,
		ObjectID.MAGICTRAINING_BONES_PILE3,
		ObjectID.MAGICTRAINING_BONES_PILE4
	);

	private static final Set<Integer> LOBBY_STAIRS_IDS = Set.of(
		ObjectID.MAGICTRAINING_STAIRS_BASE,
		ObjectID.MAGICTRAINING_STAIRS_BASE_MIRROR
	);

	private final Client client;
	private final MtaPlugin plugin;
	private final MtaConfig config;
	private final MtaSupplyCalculator supplyCalculator;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final ItemManager itemManager;

	@Inject
	public MtaSceneOverlay(
		Client client,
		MtaPlugin plugin,
		MtaConfig config,
		MtaSupplyCalculator supplyCalculator,
		ModelOutlineRenderer modelOutlineRenderer,
		ItemManager itemManager
	)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.supplyCalculator = supplyCalculator;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showSceneOverlay() || client.getLocalPlayer() == null)
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeBoldFont());

		MtaRoomType activeRoom = plugin.getActiveRoom();
		if (activeRoom != null)
		{
			switch (activeRoom)
			{
				case ENCHANTMENT:
					renderEnchantmentRoom(graphics);
					break;
				case ALCHEMY:
					renderAlchemyRoom(graphics);
					break;
				case GRAVEYARD:
					renderGraveyardRoom(graphics);
					break;
				case TELEKINETIC:
					renderTelekineticRoom(graphics);
					break;
			}
			return null;
		}

		MtaStep step = plugin.getCurrentStep();
		if (step == MtaStep.GET_PIZAZZ_HAT || step == MtaStep.MTA_LOBBY || step == MtaStep.BUY_BONES_TO_PEACHES)
		{
			if (!plugin.isInMtaLobbyOrUpstairs())
			{
				renderMtaEntranceDoors(graphics);
			}
			renderMtaLobbyAndShop(graphics, step);
		}
		else if (step == MtaStep.TRAVEL_TO_MTA)
		{
			renderMtaEntranceDoors(graphics);
		}
		else if (step == MtaStep.TRAVEL_TO_PALACE || step == MtaStep.CAST_BONES_TO_PEACHES)
		{
			renderAlKharidPalaceTarget(graphics);
		}

		return null;
	}

	private void renderMtaEntranceDoors(Graphics2D graphics)
	{
		Rs2TileObjectModel door = Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.MAGICTRAINING_TEMPLE_DOOR)
			.nearestOnClientThread();
		if (door != null)
		{
			renderTileObjectHighlight(graphics, door, ACTION_GREEN, ">> ENTER MAGE TRAINING ARENA <<");
		}
	}

	private void renderMtaLobbyAndShop(Graphics2D graphics, MtaStep step)
	{
		if (step == MtaStep.GET_PIZAZZ_HAT)
		{
			Rs2NpcModel entranceGuard = Microbot.getRs2NpcCache().query()
				.withId(NpcID.MAGICTRAINING_GUARD_ENTRANCE)
				.nearestOnClientThread();
			if (entranceGuard != null && entranceGuard.getNpc() != null)
			{
				renderNpcHighlight(graphics, entranceGuard.getNpc(), ACTION_GREEN, ">> 1. TALK TO ENTRANCE GUARDIAN (Get Hat) <<");
			}
			return;
		}

		if (step == MtaStep.BUY_BONES_TO_PEACHES)
		{
			int plane = client.getLocalPlayer().getWorldLocation().getPlane();
			if (plane == 0)
			{
				Rs2TileObjectModel stairs = Microbot.getRs2TileObjectCache().query()
					.where(o -> LOBBY_STAIRS_IDS.contains(o.getId()))
					.nearestOnClientThread();
				if (stairs != null)
				{
					renderTileObjectHighlight(graphics, stairs, ACTION_GREEN, ">> CLIMB STAIRS TO REWARDS GUARDIAN <<");
				}
			}
			else if (plane == 1)
			{
				Rs2NpcModel rewardsGuard = Microbot.getRs2NpcCache().query()
					.withId(NpcID.MAGICTRAINING_GUARD_REWARDS)
					.nearestOnClientThread();
				if (rewardsGuard != null && rewardsGuard.getNpc() != null)
				{
					renderNpcHighlight(graphics, rewardsGuard.getNpc(), ACTION_GREEN, ">> TRADE REWARDS GUARDIAN (Buy Bones to Peaches) <<");
				}
			}
			return;
		}

		// Step == MTA_LOBBY: Highlight the 4 Room Teleport Portals
		MtaRoomType recommended = plugin.getNextRecommendedRoom();
		for (MtaRoomType room : MtaRoomType.values())
		{
			Rs2TileObjectModel portal = Microbot.getRs2TileObjectCache().query()
				.withId(room.getPortalObjectId())
				.nearestOnClientThread();
			if (portal == null)
			{
				continue;
			}

			int pts = plugin.getPointsForRoom(room);
			int target = plugin.getTargetPointsForRoom(room);
			if (plugin.isRoomComplete(room))
			{
				renderTileObjectHighlight(graphics, portal, DIM_GRAY, room.getRoomName() + " (DONE ✓)");
			}
			else if (room == recommended)
			{
				renderTileObjectHighlight(
					graphics,
					portal,
					ACTION_GREEN,
					">> ENTER " + room.getRoomName().toUpperCase() + " (" + pts + "/" + target + ") <<"
				);
			}
			else
			{
				renderTileObjectHighlight(
					graphics,
					portal,
					NEXT_CYAN,
					room.getRoomName() + " (" + pts + "/" + target + ")"
				);
			}
		}
	}

	private void renderEnchantmentRoom(Graphics2D graphics)
	{
		if (plugin.isRoomComplete(MtaRoomType.ENCHANTMENT))
		{
			renderExitPortal(graphics, ">> 2,000 PTS COMPLETE! CLICK EXIT PORTAL <<");
			return;
		}

		// 1. Highlight Dragonstone ground spawns if any exist
		for (WorldPoint dstoneWp : plugin.getDragonstoneSpawns())
		{
			renderWorldPointTile(graphics, dstoneWp, ACTION_GREEN, ">> PICK UP DRAGONSTONE (14 PTS!) <<");
		}

		// 2. Highlight Shape Piles (Bonus pile in bright green, others dimmed)
		MtaEnchantShape bonus = plugin.getBonusShape();
		for (MtaEnchantShape shape : MtaEnchantShape.values())
		{
			Rs2TileObjectModel pile = Microbot.getRs2TileObjectCache().query()
				.withId(shape.getPileObjectId())
				.nearestOnClientThread();
			if (pile == null)
			{
				continue;
			}

			if (shape == bonus)
			{
				renderTileObjectHighlight(
					graphics,
					pile,
					ACTION_GREEN,
					">> CLICK BONUS: " + shape.getDisplayName().toUpperCase() + " (+9 PTS) <<"
				);
			}
			else
			{
				renderTileObjectHighlight(graphics, pile, DIM_GRAY, shape.getDisplayName() + " (No Bonus)");
			}
		}

		// 3. Highlight center Hole if player has Orbs and no free space
		int orbsInv = supplyCalculator.countItemInContainer(InventoryID.INV, ItemID.MAGICTRAINING_ENCHAN_SHAPEORB);
		if (orbsInv >= 20 || (orbsInv > 0 && supplyCalculator.getFreeInventorySlots() == 0 && plugin.countAllEnchantShapesInInventory() == 0))
		{
			Rs2TileObjectModel hole = Microbot.getRs2TileObjectCache().query()
				.withId(ObjectID.MAGICTRAINING_ENCHA_HOLE)
				.nearestOnClientThread();
			if (hole != null)
			{
				renderTileObjectHighlight(graphics, hole, NEXT_CYAN, ">> DEPOSIT ORBS IN HOLE (+3 PTS/20) <<");
			}
		}
	}

	private void renderAlchemyRoom(Graphics2D graphics)
	{
		if (plugin.isRoomComplete(MtaRoomType.ALCHEMY))
		{
			renderExitPortal(graphics, ">> 300 PTS COMPLETE! CLICK EXIT PORTAL <<");
			return;
		}

		int coins = plugin.getAlchemyCoinsInInventory();
		boolean enoughToFinish = plugin.getProjectedAlchemyPoints() >= config.alchemistTargetPoints();
		boolean nearCoinCap = coins >= 10000;

		if (enoughToFinish || nearCoinCap)
		{
			Rs2TileObjectModel collector = Microbot.getRs2TileObjectCache().query()
				.withId(ObjectID.MAGICTRAINING_COIN_COLLECTOR)
				.nearestOnClientThread();
			if (collector != null)
			{
				String label = enoughToFinish
					? ">> 300 PTS READY! CLICK COIN COLLECTOR TO FINISH <<"
					: ">> NEAR 12K COIN CAP! DEPOSIT COINS NOW <<";
				renderTileObjectHighlight(graphics, collector, enoughToFinish ? ACTION_GREEN : WARN_ORANGE, label);
			}
			if (enoughToFinish)
			{
				return;
			}
		}

		MtaAlchemyItem bestItem = plugin.getBestAlchemyItem();
		int knownBestIdx = plugin.getKnownBestCupboardIndex();
		int nearestUnknownIdx = plugin.getNearestUnknownCupboardIndex();

		for (int i = 0; i < 6; i++)
		{
			GameObject cupboardObj = plugin.getCupboardObject(i);
			if (cupboardObj == null)
			{
				continue;
			}
			MtaAlchemyItem contents = plugin.getCupboardItem(i);
			if (contents == MtaAlchemyItem.EMPTY)
			{
				continue;
			}

			if (i == knownBestIdx && bestItem != null)
			{
				renderGameObjectClickbox(
					graphics,
					cupboardObj,
					ACTION_GREEN,
					">> TAKE " + bestItem.getName().toUpperCase() + " (30 COINS!) <<"
				);
				drawItemIconAboveObject(graphics, cupboardObj, bestItem.getItemId());
			}
			else if (knownBestIdx < 0 && i == nearestUnknownIdx)
			{
				String targetName = bestItem != null ? bestItem.getName() : "30c Item";
				renderGameObjectClickbox(
					graphics,
					cupboardObj,
					NEXT_CYAN,
					">> SEARCH CUPBOARD (Find " + targetName + ") <<"
				);
			}
			else if (contents != MtaAlchemyItem.UNKNOWN && contents.getItemId() > 0)
			{
				renderGameObjectClickbox(graphics, cupboardObj, DIM_GRAY, contents.getName());
				drawItemIconAboveObject(graphics, cupboardObj, contents.getItemId());
			}
		}
	}

	private void renderGraveyardRoom(Graphics2D graphics)
	{
		if (plugin.isRoomComplete(MtaRoomType.GRAVEYARD))
		{
			renderExitPortal(graphics, ">> 200 PTS COMPLETE! CLICK EXIT PORTAL <<");
			return;
		}

		boolean readyToConvert = plugin.isGraveyardReadyToConvert();
		int fruitCount = plugin.getGraveyardFruitCount();
		int boneScore = plugin.getGraveyardBoneScore();

		// 1. If player has Bananas/Peaches and no more bones to convert -> Highlight Food Chute!
		if (fruitCount > 0 && boneScore == 0)
		{
			Rs2TileObjectModel chute = Microbot.getRs2TileObjectCache().query()
				.withId(ObjectID.MAGICTRAINING_GRAVE_FOODSHUTE)
				.nearestOnClientThread();
			if (chute != null)
			{
				renderTileObjectHighlight(
					graphics,
					chute,
					ACTION_GREEN,
					">> 3. CLICK FOOD CHUTE TO DEPOSIT (" + fruitCount + " FRUIT) <<"
				);
			}
			return;
		}

		// 2. If bone score >= 16 (or fills inventory/goal) -> Tell player to STOP clicking bones & cast spell!
		if (readyToConvert)
		{
			Player local = client.getLocalPlayer();
			if (local != null && local.getLocalLocation() != null)
			{
				String spell = plugin.isBonesToPeachesUnlocked() ? "BONES TO PEACHES" : "BONES TO BANANAS";
				Point textLoc = Perspective.getCanvasTextLocation(
					client,
					graphics,
					local.getLocalLocation(),
					">> 2. STOP! CAST " + spell + " NOW (" + boneScore + " FRUIT) <<",
					local.getLogicalHeight() + 40
				);
				if (textLoc != null)
				{
					OverlayUtil.renderTextLocation(
						graphics,
						textLoc,
						">> 2. STOP! CAST " + spell + " NOW (" + boneScore + " FRUIT) <<",
						ACTION_GREEN
					);
				}
			}

			// Also show Food Chute in Cyan as the next step after casting
			Rs2TileObjectModel chute = Microbot.getRs2TileObjectCache().query()
				.withId(ObjectID.MAGICTRAINING_GRAVE_FOODSHUTE)
				.nearestOnClientThread();
			if (chute != null)
			{
				renderTileObjectHighlight(graphics, chute, NEXT_CYAN, "Next: Deposit in Food Chute");
			}
			return;
		}

		// 3. Otherwise -> Highlight the nearest Bone Pile in bright Green!
		Rs2TileObjectModel nearestBonePile = Microbot.getRs2TileObjectCache().query()
			.where(o -> BONE_PILE_IDS.contains(o.getId()))
			.nearestOnClientThread();
		if (nearestBonePile != null)
		{
			renderTileObjectHighlight(
				graphics,
				nearestBonePile,
				ACTION_GREEN,
				">> 1. CLICK BONE PILE (" + boneScore + " / 16 Bananas) <<"
			);
		}
	}

	private void renderTelekineticRoom(Graphics2D graphics)
	{
		if (plugin.isRoomComplete(MtaRoomType.TELEKINETIC))
		{
			renderExitPortal(graphics, ">> 200 PTS COMPLETE! CLICK EXIT PORTAL <<");
			return;
		}

		NPC guardian = plugin.getMazeGuardian();
		if (guardian == null)
		{
			return;
		}

		if (guardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE)
		{
			renderNpcHighlight(graphics, guardian, ACTION_GREEN, ">> MAZE SOLVED! CLICK 'NEW-MAZE' ON GUARDIAN <<");
			return;
		}

		// Draw sliding destination if guardian is moving
		LocalPoint dest = plugin.getGuardianMovingDestination();
		if (dest != null)
		{
			Polygon destPoly = Perspective.getCanvasTilePoly(client, dest);
			if (destPoly != null)
			{
				OverlayUtil.renderPolygon(graphics, destPoly, WARN_ORANGE);
			}
		}

		Direction reqDir = plugin.getRequiredTelekineticDirection();
		Direction myDir = plugin.getPlayerMazeSideDirection();
		boolean inPosition = reqDir != null && reqDir == myDir;
		boolean isSliding = guardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_MOVING;

		// Highlight Optimal Stand Tile #1
		WorldPoint optimal1 = plugin.getOptimalTelekineticStandTile(0);
		if (optimal1 != null)
		{
			String tileLabel = inPosition
				? "IN POSITION (" + reqDir.name() + ") ✓"
				: ">> 1. RUN TO THIS GREEN TILE (" + (reqDir != null ? reqDir.name() : "") + ") <<";
			renderWorldPointTile(graphics, optimal1, ACTION_GREEN, tileLabel);
		}

		// Highlight Next Stand Tile #2 in Cyan so player can pre-move
		WorldPoint optimal2 = plugin.getOptimalTelekineticStandTile(1);
		if (optimal2 != null)
		{
			renderWorldPointTile(graphics, optimal2, NEXT_CYAN, "Next Tile (#2)");
		}

		// Highlight the Maze Guardian statue itself
		if (isSliding)
		{
			renderNpcHighlight(graphics, guardian, WARN_ORANGE, "Sliding... Run to Next Tile!");
		}
		else if (inPosition)
		{
			renderNpcHighlight(graphics, guardian, ACTION_GREEN, ">> 2. CAST TELEKINETIC GRAB ON GUARDIAN <<");
		}
		else
		{
			renderNpcHighlight(graphics, guardian, DANGER_RED, "Stand on Green Tile First");
		}
	}

	private void renderAlKharidPalaceTarget(Graphics2D graphics)
	{
		WorldPoint palaceCenter = new WorldPoint(3293, 3163, 0);
		if (plugin.isInAlKharidPalace())
		{
			Player local = client.getLocalPlayer();
			if (local != null && local.getLocalLocation() != null)
			{
				Point pt = Perspective.getCanvasTextLocation(
					client,
					graphics,
					local.getLocalLocation(),
					">> CAST BONES TO PEACHES NOW! <<",
					local.getLogicalHeight() + 40
				);
				if (pt != null)
				{
					OverlayUtil.renderTextLocation(graphics, pt, ">> CAST BONES TO PEACHES NOW! <<", ACTION_GREEN);
				}
			}
		}
		else
		{
			renderWorldPointTile(graphics, palaceCenter, ACTION_GREEN, ">> STEP INSIDE AL KHARID PALACE <<");
		}
	}

	private void renderExitPortal(Graphics2D graphics, String callout)
	{
		Rs2TileObjectModel exitPortal = Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.MAGICTRAINING_RETURNDOOR)
			.nearestOnClientThread();
		if (exitPortal != null)
		{
			renderTileObjectHighlight(graphics, exitPortal, ACTION_GREEN, callout);
		}
	}

	private void renderTileObjectHighlight(Graphics2D graphics, Rs2TileObjectModel obj, Color color, String label)
	{
		Shape clickbox = obj.getClickbox();
		if (clickbox != null)
		{
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 35);
			OverlayUtil.renderPolygon(graphics, clickbox, color, fill, new BasicStroke(2f));
		}

		LocalPoint lp = obj.getLocalLocation();
		if (lp != null && label != null && !label.isEmpty())
		{
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, label, 140);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
			}
		}
	}

	private void renderGameObjectClickbox(Graphics2D graphics, GameObject obj, Color color, String label)
	{
		Shape clickbox = obj.getClickbox();
		if (clickbox != null)
		{
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 35);
			OverlayUtil.renderPolygon(graphics, clickbox, color, fill, new BasicStroke(2f));
		}

		LocalPoint lp = obj.getLocalLocation();
		if (lp != null && label != null && !label.isEmpty())
		{
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, label, 180);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
			}
		}
	}

	private void drawItemIconAboveObject(Graphics2D graphics, GameObject obj, int itemId)
	{
		if (itemManager == null || itemId <= 0 || obj.getLocalLocation() == null)
		{
			return;
		}
		BufferedImage image = itemManager.getImage(itemId);
		if (image == null)
		{
			return;
		}
		Point canvasLoc = Perspective.getCanvasImageLocation(client, obj.getLocalLocation(), image, 120);
		if (canvasLoc != null)
		{
			graphics.drawImage(image, canvasLoc.getX(), canvasLoc.getY(), null);
		}
	}

	private void renderNpcHighlight(Graphics2D graphics, NPC npc, Color color, String label)
	{
		if (modelOutlineRenderer != null)
		{
			modelOutlineRenderer.drawOutline(npc, 2, color, 4);
		}
		Polygon tilePoly = npc.getCanvasTilePoly();
		if (tilePoly != null)
		{
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 35);
			OverlayUtil.renderPolygon(graphics, tilePoly, color, fill, new BasicStroke(2f));
		}
		if (label != null && !label.isEmpty())
		{
			Point textLoc = npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + 40);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
			}
		}
	}

	private void renderWorldPointTile(Graphics2D graphics, WorldPoint wp, Color color, String label)
	{
		if (wp == null)
		{
			return;
		}
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		if (lp == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly != null)
		{
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 45);
			OverlayUtil.renderPolygon(graphics, poly, color, fill, new BasicStroke(2f));
		}
		if (label != null && !label.isEmpty())
		{
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, label, 40);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
			}
		}
	}
}
