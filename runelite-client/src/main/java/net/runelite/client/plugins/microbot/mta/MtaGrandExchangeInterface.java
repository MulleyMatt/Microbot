package net.runelite.client.plugins.microbot.mta;

import com.google.common.primitives.Shorts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.JagexColors;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MtaGrandExchangeInterface
{
	public static final String MTA_SEARCH_TAG = "mta-diary-supplies";

	@Setter
	@Getter
	private boolean active = false;

	private Widget parent;
	private Widget buttonBackgroundWidget;
	private Widget iconWidget;
	private Widget titleWidget;

	private short[] cachedMissingTradeableIds = new short[0];

	private final Client client;
	private final ClientThread clientThread;
	private final MtaConfig config;
	private final ItemManager itemManager;

	private Supplier<List<MtaSupplyCalculator.SupplyItem>> missingTradeableSupplier = Collections::emptyList;

	@Inject
	public MtaGrandExchangeInterface(
		Client client,
		ClientThread clientThread,
		MtaConfig config,
		ItemManager itemManager
	)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.itemManager = itemManager;
	}

	public void setMissingTradeableSupplier(Supplier<List<MtaSupplyCalculator.SupplyItem>> supplier)
	{
		this.missingTradeableSupplier = supplier != null ? supplier : Collections::emptyList;
	}

	public void init()
	{
		if (isHidden() || !isAtGrandExchange())
		{
			return;
		}

		parent = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
		if (parent == null)
		{
			return;
		}

		cleanupWidgets();

		int buttonSize = 20;
		int buttonX = 455; // Left of Quest Helper (480)
		int buttonY = 0;

		buttonBackgroundWidget = createGraphic(
			parent,
			"MTA Diary Supplies",
			SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL,
			buttonSize,
			buttonSize,
			buttonX,
			buttonY
		);
		buttonBackgroundWidget.setAction(1, "Filter MTA Diary Items");
		buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

		iconWidget = createGraphic(
			parent,
			"",
			SpriteID.Staticons.MAGIC,
			buttonSize - 4,
			buttonSize - 4,
			buttonX + 2,
			buttonY + 2
		);

		titleWidget = createTitle(parent);

		if (active)
		{
			active = false;
			activateTab();
		}
	}

	public void destroy()
	{
		if (active)
		{
			closeOptions();
		}
		cleanupWidgets();
		parent = null;
		active = false;
	}

	private void cleanupWidgets()
	{
		if (iconWidget != null)
		{
			iconWidget.setHidden(true);
			iconWidget = null;
		}
		if (buttonBackgroundWidget != null)
		{
			buttonBackgroundWidget.setHidden(true);
			buttonBackgroundWidget = null;
		}
		if (titleWidget != null)
		{
			titleWidget.setHidden(true);
			titleWidget = null;
		}
	}

	public boolean isHidden()
	{
		Widget widget = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
		return widget == null || widget.isHidden();
	}

	public boolean isAtGrandExchange()
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldLocation() == null)
		{
			return false;
		}
		WorldPoint geCenter = new WorldPoint(3164, 3489, 0);
		return client.getLocalPlayer().getWorldLocation().distanceTo(geCenter) <= 25;
	}

	private void handleTagTab(ScriptEvent event)
	{
		if (active)
		{
			closeOptions();
		}
		else
		{
			activateTab();
		}
		client.playSoundEffect(SoundEffectID.UI_BOOP);
	}

	public void closeOptions()
	{
		active = false;
		if (buttonBackgroundWidget != null)
		{
			buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
			buttonBackgroundWidget.revalidate();
		}
		if (titleWidget != null)
		{
			titleWidget.setHidden(true);
		}

		client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
		client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
		clientThread.invokeLater(() -> updateSearchInterface(false));
	}

	public void activateTab()
	{
		if (active)
		{
			return;
		}

		if (buttonBackgroundWidget != null)
		{
			buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
			buttonBackgroundWidget.revalidate();
		}
		if (titleWidget != null)
		{
			titleWidget.setHidden(false);
		}
		active = true;

		// Precompute missing tradeable IDs BEFORE firing CS2 search script
		refreshMissingTradeableItems();

		client.setVarcStrValue(VarClientID.MESLAYERINPUT, MTA_SEARCH_TAG);
		client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
		clientThread.invokeLater(() -> updateSearchInterface(true));
	}

	private void updateSearchInterface(boolean hideSearchBox)
	{
		Widget geSearchBox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		if (geSearchBox == null)
		{
			return;
		}
		Object[] scriptArgs = geSearchBox.getOnKeyListener();
		if (scriptArgs == null)
		{
			return;
		}
		client.runScript(scriptArgs);
		geSearchBox.setHidden(hideSearchBox);
	}

	public void onGrandExchangeSearched(GrandExchangeSearched event)
	{
		final String input = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
		if (!MTA_SEARCH_TAG.equals(input))
		{
			return;
		}

		event.consume();

		client.setGeSearchResultIndex(0);
		client.setGeSearchResultCount(cachedMissingTradeableIds.length);
		client.setGeSearchResultIds(cachedMissingTradeableIds);
	}

	public void refreshMissingTradeableItems()
	{
		List<Integer> missingIds = missingTradeableSupplier.get().stream()
			.map(MtaSupplyCalculator.SupplyItem::getItemId)
			.filter(id -> itemManager == null || itemManager.getItemComposition(id).isGeTradeable())
			.distinct()
			.collect(Collectors.toList());

		if (missingIds.isEmpty())
		{
			missingIds = List.of(
				ItemID.COSMICRUNE,
				ItemID.SOULRUNE,
				ItemID.BLOODRUNE,
				ItemID.NATURERUNE,
				ItemID.LAWRUNE,
				ItemID.WATERRUNE,
				ItemID.MUD_BATTLESTAFF,
				ItemID.STAFF_OF_FIRE,
				ItemID.STAFF_OF_AIR,
				ItemID.SHARK,
				ItemID.ANGLERFISH,
				ItemID._4DOSESTAMINA,
				ItemID.RING_OF_DUELING_8,
				ItemID.BONES
			).stream()
				.filter(id -> itemManager == null || itemManager.getItemComposition(id).isGeTradeable())
				.collect(Collectors.toList());
		}

		Set<Integer> sortedIds = new TreeSet<>(missingIds);
		cachedMissingTradeableIds = Shorts.toArray(sortedIds);
	}

	private Widget createGraphic(Widget container, String name, int spriteId, int width, int height, int x, int y)
	{
		Widget widget = container.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(width);
		widget.setOriginalHeight(height);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setSpriteId(spriteId);
		widget.setOnOpListener(ScriptID.NULL);
		widget.setHasListener(true);
		widget.setName(name);
		widget.revalidate();
		return widget;
	}

	private Widget createTitle(Widget container)
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		Widget widget = container.createChild(-1, WidgetType.TEXT);
		if (chatbox == null)
		{
			return widget;
		}

		widget.setOriginalWidth(chatbox.getWidth());
		widget.setOriginalHeight(chatbox.getHeight());
		widget.setOriginalX(0);
		widget.setOriginalY(0);
		widget.setTextShadowed(false);
		widget.setXTextAlignment(WidgetTextAlignment.CENTER);
		widget.setYTextAlignment(WidgetTextAlignment.CENTER);

		int runs = config.runsToPurchase();
		String runLabel = runs == 1 ? "1x" : runs + "x";
		widget.setText("<col=00ff88>MTA Diary</col> Missing Supplies (" + runLabel + ")");
		widget.setFontId(FontID.BOLD_12);
		widget.setTextColor(JagexColors.CHAT_GAME_EXAMINE_TEXT_OPAQUE_BACKGROUND.getRGB());

		if (!active)
		{
			widget.setHidden(true);
		}
		widget.revalidate();
		return widget;
	}
}
