package net.runelite.client.plugins.microbot.mta;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;

public class MtaInventoryOverlay extends Overlay
{
	private static final Color ACTION_GREEN = new Color(0, 255, 100);
	private static final Color SPELL_CYAN = new Color(0, 220, 255);
	private static final Color EQUIP_ORANGE = new Color(255, 165, 0);
	private static final Color DANGER_RED = new Color(255, 60, 60);

	private final Client client;
	private final MtaPlugin plugin;
	private final MtaConfig config;
	private final MtaSupplyCalculator supplyCalculator;

	@Inject
	public MtaInventoryOverlay(
		Client client,
		MtaPlugin plugin,
		MtaConfig config,
		MtaSupplyCalculator supplyCalculator
	)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.supplyCalculator = supplyCalculator;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showInventoryAndSpellOverlay())
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());

		MtaRoomType activeRoom = plugin.getActiveRoom();
		if (activeRoom != null)
		{
			renderActiveRoomWidgets(graphics, activeRoom);
			return null;
		}

		MtaStep step = plugin.getCurrentStep();
		if (step == MtaStep.BUY_BONES_TO_PEACHES)
		{
			renderMtaShopHighlights(graphics);
		}
		else if (step == MtaStep.TRAVEL_TO_MTA)
		{
			highlightInventoryItems(graphics, MtaSupplyCalculator.RING_OF_DUELING_IDS, ACTION_GREEN, "TELEPORT");
		}
		else if (step == MtaStep.CAST_BONES_TO_PEACHES)
		{
			if (!supplyCalculator.hasRequiredStaffEquippedForRoom(MtaRoomType.GRAVEYARD))
			{
				Set<Integer> staves = supplyCalculator.getInventoryStaffIdsForRoom(MtaRoomType.GRAVEYARD);
				highlightInventoryItems(graphics, staves, EQUIP_ORANGE, "EQUIP");
			}
			highlightInventoryItems(graphics, Set.of(ItemID.BONES, ItemID.BIG_BONES), SPELL_CYAN, "READY");
			highlightSpellWidget(graphics, InterfaceID.MagicSpellbook.BONES_PEACHES, ACTION_GREEN, "CAST!");
		}

		return null;
	}

	private void renderActiveRoomWidgets(Graphics2D graphics, MtaRoomType room)
	{
		// 1. Check if the player has the wrong elemental staff equipped -> highlight staff in inventory
		if (!supplyCalculator.hasRequiredStaffEquippedForRoom(room))
		{
			Set<Integer> staffIds = supplyCalculator.getInventoryStaffIdsForRoom(room);
			highlightInventoryItems(graphics, staffIds, EQUIP_ORANGE, "EQUIP!");
		}

		switch (room)
		{
			case ENCHANTMENT:
				if (!plugin.isRoomComplete(MtaRoomType.ENCHANTMENT))
				{
					highlightInventoryItems(
						graphics,
						Set.of(ItemID.MAGICTRAINING_DRAGONSTONE),
						ACTION_GREEN,
						"14 PT!"
					);
					MtaEnchantShape bonus = plugin.getBonusShape();
					if (bonus != null)
					{
						highlightInventoryItems(
							graphics,
							Set.of(bonus.getItemId()),
							ACTION_GREEN,
							"9 PT!"
						);
					}
					int dstoneCount = supplyCalculator.countItemInContainer(InventoryID.INV, ItemID.MAGICTRAINING_DRAGONSTONE);
					int bonusCount = bonus != null ? supplyCalculator.countItemInContainer(InventoryID.INV, bonus.getItemId()) : 0;
					if (dstoneCount > 0 || bonusCount > 0)
					{
						highlightSpellWidget(graphics, InterfaceID.MagicSpellbook.ENCHANT_7, SPELL_CYAN, "CAST");
						highlightSpellWidget(graphics, InterfaceID.MagicSpellbook.ENCHANT_JEWELLERY, SPELL_CYAN, "CAST");
					}
				}
				break;

			case ALCHEMY:
				if (!plugin.isRoomComplete(MtaRoomType.ALCHEMY)
					&& plugin.getProjectedAlchemyPoints() < config.alchemistTargetPoints())
				{
					MtaAlchemyItem best = plugin.getBestAlchemyItem();
					if (best != null && best.getItemId() > 0)
					{
						highlightInventoryItems(
							graphics,
							Set.of(best.getItemId()),
							ACTION_GREEN,
							"30c ALCH"
						);
						if (supplyCalculator.countItemInContainer(InventoryID.INV, best.getItemId()) > 0)
						{
							highlightSpellWidget(graphics, InterfaceID.MagicSpellbook.HIGH_ALCHEMY, SPELL_CYAN, "ALCH");
						}
					}
				}
				break;

			case GRAVEYARD:
				if (plugin.isLowHpInGraveyard())
				{
					highlightInventoryItems(
						graphics,
						MtaSupplyCalculator.IN_ROOM_HEALING_IDS,
						DANGER_RED,
						"EAT HP!"
					);
				}
				if (!plugin.isRoomComplete(MtaRoomType.GRAVEYARD))
				{
					if (plugin.isGraveyardReadyToConvert())
					{
						highlightInventoryItems(
							graphics,
							Set.of(
								ItemID.MAGICTRAINING_BONES1,
								ItemID.MAGICTRAINING_BONES2,
								ItemID.MAGICTRAINING_BONES3,
								ItemID.MAGICTRAINING_BONES4
							),
							SPELL_CYAN,
							"READY"
						);
						int spellId = plugin.isBonesToPeachesUnlocked()
							? InterfaceID.MagicSpellbook.BONES_PEACHES
							: InterfaceID.MagicSpellbook.BONES_BANANAS;
						highlightSpellWidget(graphics, spellId, ACTION_GREEN, "CAST NOW!");
					}
					else if (plugin.getGraveyardFruitCount() > 0 && plugin.getGraveyardBoneScore() == 0)
					{
						highlightInventoryItems(
							graphics,
							Set.of(ItemID.BANANA, ItemID.PEACH),
							ACTION_GREEN,
							"DEPOSIT"
						);
					}
				}
				break;

			case TELEKINETIC:
				if (!plugin.isRoomComplete(MtaRoomType.TELEKINETIC))
				{
					var reqDir = plugin.getRequiredTelekineticDirection();
					var myDir = plugin.getPlayerMazeSideDirection();
					if (reqDir != null && reqDir == myDir)
					{
						highlightSpellWidget(graphics, InterfaceID.MagicSpellbook.TELEGRAB, ACTION_GREEN, "CAST");
					}
				}
				break;
		}
	}

	private void renderMtaShopHighlights(Graphics2D graphics)
	{
		Widget rewardsContainer = client.getWidget(InterfaceID.MagictrainingShop.REWARDS);
		if (rewardsContainer != null && !rewardsContainer.isHidden() && rewardsContainer.getDynamicChildren() != null)
		{
			for (Widget child : rewardsContainer.getDynamicChildren())
			{
				if (child != null && child.getItemId() == ItemID.MAGICTRAINING_PEACHSPELL)
				{
					drawWidgetBox(graphics, child.getBounds(), ACTION_GREEN, "1. SELECT");
				}
			}
		}

		Widget confirmBtn = client.getWidget(InterfaceID.MagictrainingShop.CONFIRM);
		if (confirmBtn != null && !confirmBtn.isHidden())
		{
			drawWidgetBox(graphics, confirmBtn.getBounds(), SPELL_CYAN, "2. CONFIRM");
		}
	}

	public void highlightInventoryItems(Graphics2D graphics, Set<Integer> targetItemIds, Color color, String tag)
	{
		if (targetItemIds == null || targetItemIds.isEmpty())
		{
			return;
		}
		Widget inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);
		if (inventoryWidget == null || inventoryWidget.isHidden())
		{
			return;
		}
		Widget[] children = inventoryWidget.getChildren();
		if (children == null)
		{
			return;
		}

		for (Widget item : children)
		{
			if (item != null && item.getItemId() > 0 && targetItemIds.contains(item.getItemId()))
			{
				drawWidgetBox(graphics, item.getBounds(), color, tag);
			}
		}
	}

	private void highlightSpellWidget(Graphics2D graphics, int packedWidgetId, Color color, String tag)
	{
		Widget spellWidget = client.getWidget(packedWidgetId);
		if (spellWidget == null || spellWidget.isHidden())
		{
			return;
		}
		drawWidgetBox(graphics, spellWidget.getBounds(), color, tag);
	}

	private void drawWidgetBox(Graphics2D graphics, Rectangle bounds, Color color, String label)
	{
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
		graphics.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);

		graphics.setStroke(new BasicStroke(2f));
		graphics.setColor(color);
		graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);

		if (label != null && !label.isEmpty())
		{
			int textX = bounds.x + 2;
			int textY = bounds.y + bounds.height - 2;
			graphics.setColor(Color.BLACK);
			graphics.drawString(label, textX + 1, textY + 1);
			graphics.setColor(color);
			graphics.drawString(label, textX, textY);
		}
	}
}
