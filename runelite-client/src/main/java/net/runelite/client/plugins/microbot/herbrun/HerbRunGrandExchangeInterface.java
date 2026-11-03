package net.runelite.client.plugins.microbot.herbrun;

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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class HerbRunGrandExchangeInterface {

    public static final String HERB_RUN_SEARCH_TAG = "herb-run-supplies";

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
    private final HerbRunSupplyCalculator supplyCalculator;
    private final HerbRunConfig config;
    private final ItemManager itemManager;

    @Inject
    public HerbRunGrandExchangeInterface(Client client, ClientThread clientThread,
                                         HerbRunSupplyCalculator supplyCalculator, HerbRunConfig config,
                                         ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.supplyCalculator = supplyCalculator;
        this.config = config;
        this.itemManager = itemManager;
    }

    public void init() {
        if (isHidden() || !isAtGrandExchange()) {
            return;
        }

        parent = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        if (parent == null) {
            return;
        }

        // Clean up previously created widgets to avoid duplicates
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setHidden(true);
            buttonBackgroundWidget = null;
        }
        if (iconWidget != null) {
            iconWidget.setHidden(true);
            iconWidget = null;
        }
        if (titleWidget != null) {
            titleWidget.setHidden(true);
            titleWidget = null;
        }

        int BUTTON_SIZE = 20;
        int BUTTON_X = 455; // Left of QuestHelper (480)
        int BUTTON_Y = 0;

        buttonBackgroundWidget = createGraphic(parent, "Herb Run Supplies",
                SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL,
                BUTTON_SIZE, BUTTON_SIZE, BUTTON_X, BUTTON_Y);
        buttonBackgroundWidget.setAction(1, "View Herb Run Items");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

        // Farming skill icon on the button
        iconWidget = createGraphic(parent, "", SpriteID.Staticons2.FARMING,
                BUTTON_SIZE - 4, BUTTON_SIZE - 4, BUTTON_X + 2, BUTTON_Y + 2);

        titleWidget = createTitle(parent);

        if (active) {
            active = false;
            activateTab();
        }
    }

    public void destroy() {
        if (active) {
            closeOptions();
        }

        if (iconWidget != null) {
            iconWidget.setHidden(true);
            iconWidget = null;
        }
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setHidden(true);
            buttonBackgroundWidget = null;
        }
        if (titleWidget != null) {
            titleWidget.setHidden(true);
            titleWidget = null;
        }
        parent = null;
        active = false;
    }

    public boolean isHidden() {
        Widget widget = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        return widget == null || widget.isHidden();
    }

    public boolean isAtGrandExchange() {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldLocation() == null) {
            return false;
        }
        WorldPoint geCenter = new WorldPoint(3164, 3489, 0);
        return client.getLocalPlayer().getWorldLocation().distanceTo(geCenter) <= 25;
    }

    private void handleTagTab(ScriptEvent event) {
        if (active) {
            closeOptions();
        } else {
            activateTab();
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    public void closeOptions() {
        active = false;
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        if (titleWidget != null) {
            titleWidget.setHidden(true);
        }

        client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

        clientThread.invokeLater(() -> updateSearchInterface(false));
    }

    public void activateTab() {
        if (active) {
            return;
        }

        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }
        if (titleWidget != null) {
            titleWidget.setHidden(false);
        }
        active = true;

        refreshMissingTradeableItems();

        client.setVarcStrValue(VarClientID.MESLAYERINPUT, HERB_RUN_SEARCH_TAG);
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

        clientThread.invokeLater(() -> updateSearchInterface(true));
    }

    private void updateSearchInterface(boolean hideSearchBox) {
        Widget geSearchBox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
        if (geSearchBox == null) {
            return;
        }

        Object[] scriptArgs = geSearchBox.getOnKeyListener();
        if (scriptArgs == null) {
            return;
        }

        client.runScript(scriptArgs);
        geSearchBox.setHidden(hideSearchBox);
    }

    public void onGrandExchangeSearched(GrandExchangeSearched event) {
        final String input = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if (!HERB_RUN_SEARCH_TAG.equals(input)) {
            return;
        }

        event.consume();

        if (cachedMissingTradeableIds == null || cachedMissingTradeableIds.length == 0) {
            refreshMissingTradeableItems();
        }

        log.info("Herb Run GE search triggered. Populating {} items into search results.", cachedMissingTradeableIds.length);

        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(cachedMissingTradeableIds.length);
        client.setGeSearchResultIds(cachedMissingTradeableIds);
    }

    public void refreshMissingTradeableItems() {
        List<Integer> missingIds = supplyCalculator.getMissingTradeableItems(config.runsToPurchase())
                .stream()
                .map(HerbRunSupplyCalculator.SupplyItem::getItemId)
                .filter(id -> itemManager.getItemComposition(id).isGeTradeable())
                .distinct()
                .collect(Collectors.toList());

        if (missingIds.isEmpty()) {
            // If none missing, provide all general herb run tradeable supplies
            missingIds = List.of(
                    ItemID.RANARR_SEED,
                    ItemID.TOADFLAX_SEED,
                    ItemID.BUCKET_ULTRACOMPOST,
                    ItemID.STRONGHOLD_TELEPORT_BASALT,
                    ItemID.WEISS_TELEPORT_BASALT,
                    ItemID.POH_TABLET_TELEPORTTOHOUSE,
                    ItemID.LUNAR_TABLET_CATHERBY_TELEPORT,
                    ItemID.POH_TABLET_CAMELOTTELEPORT,
                    ItemID.POH_TABLET_ARDOUGNETELEPORT,
                    ItemID.POH_TABLET_FALADORTELEPORT,
                    ItemID.SPADE,
                    ItemID.DIBBER
            ).stream()
             .filter(id -> itemManager.getItemComposition(id).isGeTradeable())
             .collect(Collectors.toList());
        }

        Set<Integer> sortedIds = new TreeSet<>(missingIds);
        cachedMissingTradeableIds = Shorts.toArray(sortedIds);
    }

    private Widget createGraphic(Widget container, String name, int spriteId, int width, int height, int x, int y) {
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

    private Widget createTitle(Widget container) {
        Widget chatbox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);

        Widget widget = container.createChild(-1, WidgetType.TEXT);
        if (chatbox == null) {
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
        String runLabel = runs == 1 ? "1 run" : runs + " runs";
        widget.setText("<col=00ff88>Herb Run</col> Missing Supplies (" + runLabel + ")");
        widget.setFontId(FontID.BOLD_12);
        widget.setTextColor(JagexColors.CHAT_GAME_EXAMINE_TEXT_OPAQUE_BACKGROUND.getRGB());

        if (!active) {
            widget.setHidden(true);
        }

        widget.revalidate();
        return widget;
    }
}
