package net.runelite.client.plugins.microbot.mta;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

public class MtaOverlay extends OverlayPanel
{
	private static final Color HEADER_COLOR = new Color(0, 230, 255);
	private static final Color DONE_COLOR = new Color(80, 255, 120);
	private static final Color ACTIVE_COLOR = new Color(255, 220, 60);
	private static final Color WARN_COLOR = new Color(255, 110, 70);
	private static final Color SUBTLE_COLOR = new Color(190, 190, 190);

	private final MtaPlugin plugin;
	private final MtaConfig config;
	private final MtaSupplyCalculator supplyCalculator;

	@Inject
	public MtaOverlay(MtaPlugin plugin, MtaConfig config, MtaSupplyCalculator supplyCalculator)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.supplyCalculator = supplyCalculator;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showHudOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(285, 0));

		// Title
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("MTA Lumbridge Hard Diary")
			.color(HEADER_COLOR)
			.build());

		// Current Phase
		MtaStep step = plugin.getCurrentStep();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Phase:")
			.right(step.getTitle())
			.rightColor(step == MtaStep.DIARY_COMPLETE ? DONE_COLOR : Color.WHITE)
			.build());

		// Prominent Next Action Box
		panelComponent.getChildren().add(LineComponent.builder()
			.left("DO THIS:")
			.leftColor(ACTIVE_COLOR)
			.right(plugin.getActionInstruction())
			.rightColor(Color.GREEN)
			.build());

		if (plugin.getSubInstruction() != null && !plugin.getSubInstruction().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("")
				.right(plugin.getSubInstruction())
				.rightColor(SUBTLE_COLOR)
				.build());
		}

		// 4-Room Pizazz Points Progress Table
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Bones to Peaches Progress")
			.color(ACTIVE_COLOR)
			.build());

		renderRoomProgressLine(MtaRoomType.ENCHANTMENT, 0);
		renderRoomProgressLine(MtaRoomType.ALCHEMY, plugin.getActiveRoom() == MtaRoomType.ALCHEMY ? plugin.getAlchemyCoinsInInventory() / 100 : 0);
		renderRoomProgressLine(
			MtaRoomType.GRAVEYARD,
			plugin.getActiveRoom() == MtaRoomType.GRAVEYARD
				? Math.max(0, plugin.getProjectedGraveyardPoints() - plugin.getGraveyardPoints())
				: 0
		);
		renderRoomProgressLine(MtaRoomType.TELEKINETIC, 0);

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Bones to Peaches:")
			.right(plugin.isBonesToPeachesUnlocked() ? "UNLOCKED ✓" : "Locked (Buy Upstairs)")
			.rightColor(plugin.isBonesToPeachesUnlocked() ? DONE_COLOR : SUBTLE_COLOR)
			.build());

		if (config.trackAlKharidPalaceCast())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Al Kharid Palace Cast:")
				.right(plugin.isDiaryTaskComplete() ? "COMPLETE ✓" : "Pending")
				.rightColor(plugin.isDiaryTaskComplete() ? DONE_COLOR : SUBTLE_COLOR)
				.build());
		}

		// Room-specific live stats when inside a room
		MtaRoomType activeRoom = plugin.getActiveRoom();
		if (activeRoom != null)
		{
			renderActiveRoomDetails(activeRoom);
		}
		else if (step == MtaStep.PREPARE_SUPPLIES || step == MtaStep.TRAVEL_TO_MTA)
		{
			renderSupplyChecklist();
		}

		return super.render(graphics);
	}

	private void renderRoomProgressLine(MtaRoomType room, int heldBonusPoints)
	{
		int pts = plugin.getPointsForRoom(room);
		int target = plugin.getTargetPointsForRoom(room);
		boolean complete = plugin.isRoomComplete(room);

		String rightText;
		Color rightColor;
		if (complete)
		{
			rightText = pts + " / " + target + " ✓";
			rightColor = DONE_COLOR;
		}
		else if (heldBonusPoints > 0)
		{
			int projected = pts + heldBonusPoints;
			rightText = pts + " (+" + heldBonusPoints + " held) / " + target;
			rightColor = projected >= target ? DONE_COLOR : ACTIVE_COLOR;
		}
		else
		{
			rightText = pts + " / " + target;
			rightColor = plugin.getActiveRoom() == room ? ACTIVE_COLOR : Color.WHITE;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left(room.getRoomName() + ":")
			.right(rightText)
			.rightColor(rightColor)
			.build());
	}

	private void renderActiveRoomDetails(MtaRoomType room)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(room.getRoomName() + " Guide")
			.color(HEADER_COLOR)
			.build());

		boolean staffOk = supplyCalculator.hasRequiredStaffEquippedForRoom(room);
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Required Staff:")
			.right(staffOk ? room.getRecommendedStaffName() + " ✓" : "EQUIP " + room.getRecommendedStaffName().toUpperCase() + "!")
			.rightColor(staffOk ? DONE_COLOR : WARN_COLOR)
			.build());

		switch (room)
		{
			case ENCHANTMENT:
				MtaEnchantShape bonus = plugin.getBonusShape();
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Current Bonus Shape:")
					.right(bonus != null ? bonus.getDisplayName() + " (+9 pts)" : "Detecting...")
					.rightColor(bonus != null ? bonus.getThemeColor() : Color.WHITE)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Floor Dragonstones:")
					.right(plugin.getDragonstoneSpawns().size() + " (14 pts each)")
					.rightColor(plugin.getDragonstoneSpawns().isEmpty() ? SUBTLE_COLOR : DONE_COLOR)
					.build());
				int remEnch = Math.max(0, config.enchantmentTargetPoints() - plugin.getEnchantmentPoints());
				int castsLeft = (int) Math.ceil(remEnch / 9.0);
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Est. Lvl-7 Casts Left:")
					.right(String.valueOf(castsLeft))
					.rightColor(Color.WHITE)
					.build());
				break;

			case ALCHEMY:
				MtaAlchemyItem best = plugin.getBestAlchemyItem();
				MtaAlchemyItem free = plugin.getFreeAlchemyItem();
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Best 30-Coin Item:")
					.right(best != null ? best.getName() : "Reading widget...")
					.rightColor(DONE_COLOR)
					.build());
				if (free != null)
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left("Free Alch (0 Runes):")
						.right(free.getName())
						.rightColor(HEADER_COLOR)
						.build());
				}
				int coins = plugin.getAlchemyCoinsInInventory();
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Coins in Inventory:")
					.right(coins + " (+" + (coins / 100) + " pts)")
					.rightColor(coins >= 10000 ? WARN_COLOR : ACTIVE_COLOR)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("30c Alchs Remaining:")
					.right(String.valueOf(plugin.getRemaining30CoinAlchsNeeded()))
					.rightColor(Color.WHITE)
					.build());
				break;

			case GRAVEYARD:
				int score = plugin.getGraveyardBoneScore();
				boolean ready = plugin.isGraveyardReadyToConvert();
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Held Bone Value:")
					.right(score + " / 16 Bananas")
					.rightColor(ready ? DONE_COLOR : ACTIVE_COLOR)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Held Bananas/Peaches:")
					.right(String.valueOf(plugin.getGraveyardFruitCount()))
					.rightColor(plugin.getGraveyardFruitCount() > 0 ? DONE_COLOR : SUBTLE_COLOR)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Action Status:")
					.right(ready ? "CAST BONES TO BANANAS!" : (plugin.getGraveyardFruitCount() > 0 ? "DEPOSIT IN CHUTE!" : "GRAB BONES FROM PILE"))
					.rightColor(ready || plugin.getGraveyardFruitCount() > 0 ? DONE_COLOR : Color.WHITE)
					.build());
				break;

			case TELEKINETIC:
				var reqDir = plugin.getRequiredTelekineticDirection();
				var myDir = plugin.getPlayerMazeSideDirection();
				boolean inPos = reqDir != null && reqDir == myDir;
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Required Maze Side:")
					.right(reqDir != null ? reqDir.name() : "Solved / None")
					.rightColor(ACTIVE_COLOR)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Your Position:")
					.right(inPos ? "IN POSITION (CAST NOW!) ✓" : (myDir != null ? myDir.name() + " (Move to Green Tile)" : "Move to Green Tile"))
					.rightColor(inPos ? DONE_COLOR : WARN_COLOR)
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Pulls Remaining:")
					.right(String.valueOf(plugin.getTelekineticMoves().size()))
					.rightColor(Color.WHITE)
					.build());
				break;
		}
	}

	private void renderSupplyChecklist()
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Supply & Gear Checklist")
			.color(HEADER_COLOR)
			.build());

		List<MtaSupplyCalculator.SupplyItem> supplies = supplyCalculator.calculateSupplies(
			plugin.getTelekineticPoints(),
			plugin.getAlchemistPoints(),
			plugin.getEnchantmentPoints(),
			plugin.getGraveyardPoints(),
			plugin.isBonesToPeachesUnlocked()
		);

		for (MtaSupplyCalculator.SupplyItem item : supplies)
		{
			String status;
			Color color;
			if (item.isSatisfied())
			{
				status = item.getCurrentPossession() + "/" + item.getRequiredQuantity() + " ✓";
				color = DONE_COLOR;
			}
			else if (item.canWithdrawFromBank())
			{
				status = "Withdraw " + item.getMissingForRun() + " (Bank)";
				color = HEADER_COLOR;
			}
			else if (item.canPartiallyWithdrawFromBank())
			{
				status = "Bank: " + item.getBankQuantity() + " | Buy: " + item.getGeBuyQuantity(config.runsToPurchase());
				color = ACTIVE_COLOR;
			}
			else if (item.isTradeable())
			{
				status = "Buy " + item.getGeBuyQuantity(config.runsToPurchase()) + " (GE)";
				color = WARN_COLOR;
			}
			else
			{
				status = "Missing";
				color = WARN_COLOR;
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(item.getName())
				.right(status)
				.rightColor(color)
				.build());
		}
	}
}
