package net.runelite.client.plugins.microbot.redwood;

import com.google.common.primitives.Shorts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
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
public class RedwoodGrandExchangeInterface {

    public static final String REDWOOD_SEARCH_TAG = "redwood-wc-supplies";

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
    private final RedwoodSupplyCalculator supplyCalculator;
    private final RedwoodConfig config;
    private final ItemManager itemManager;

    @Inject
    public RedwoodGrandExchangeInterface(Client client, ClientThread clientThread,
                                         RedwoodSupplyCalculator supplyCalculator, RedwoodConfig config,
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
        int BUTTON_X = 430; // Positioned left of HerbRun (455) and QuestHelper (480)
        int BUTTON_Y = 0;

        buttonBackgroundWidget = createGraphic(parent, "Redwood Gear",
                SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL,
                BUTTON_SIZE, BUTTON_SIZE, BUTTON_X, BUTTON_Y);
        buttonBackgroundWidget.setAction(1, "View Woodcutting Gear");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

        iconWidget = createGraphic(parent, "Redwood Gear",
                net.runelite.api.SpriteID.SKILL_WOODCUTTING,
                16, 16, BUTTON_X + 2, BUTTON_Y + 2);

        int TITLE_X = 80;
        int TITLE_Y = 4;
        int TITLE_WIDTH = 300;
        int TITLE_HEIGHT = 20;

        titleWidget = parent.createChild(-1, WidgetType.TEXT);
        titleWidget.setText("Redwood Woodcutting Gear");
        titleWidget.setFontId(FontID.BOLD_12);
        titleWidget.setTextColor(JagexColors.DARK_ORANGE_INTERFACE_TEXT.getRGB());
        titleWidget.setTextShadowed(true);
        titleWidget.setOriginalX(TITLE_X);
        titleWidget.setOriginalY(TITLE_Y);
        titleWidget.setOriginalWidth(TITLE_WIDTH);
        titleWidget.setOriginalHeight(TITLE_HEIGHT);
        titleWidget.setXTextAlignment(WidgetTextAlignment.LEFT);
        titleWidget.setYTextAlignment(WidgetTextAlignment.CENTER);
        titleWidget.revalidate();
        titleWidget.setHidden(true);

        if (active) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
            titleWidget.setHidden(false);
        }
    }

    private void handleTagTab(Object ev) {
        if (!active) {
            activateTab();
        } else {
            deactivateTab();
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
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

        refreshTradeableIds();

        client.setVarcStrValue(VarClientID.MESLAYERINPUT, REDWOOD_SEARCH_TAG);
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

        clientThread.invokeLater(() -> updateSearchInterface(true));
    }

    public void deactivateTab() {
        if (!active) {
            return;
        }

        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        if (titleWidget != null) {
            titleWidget.setHidden(true);
        }
        active = false;

        client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 0);

        clientThread.invokeLater(() -> updateSearchInterface(false));
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
        if (!REDWOOD_SEARCH_TAG.equals(input)) {
            return;
        }

        event.consume();

        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(cachedMissingTradeableIds.length);
        client.setGeSearchResultIds(cachedMissingTradeableIds);
    }

    private void refreshTradeableIds() {
        Set<Integer> tradeableIds = new TreeSet<>();
        for (int itemId : RedwoodSupplyCalculator.TRADEABLE_AXES) {
            try {
                if (itemManager.getItemComposition(itemId).isGeTradeable()) {
                    tradeableIds.add(itemId);
                }
            } catch (Exception ignored) {
            }
        }

        List<Short> shortList = tradeableIds.stream()
                .map(Integer::shortValue)
                .collect(Collectors.toList());
        cachedMissingTradeableIds = Shorts.toArray(shortList);
    }

    public boolean isHidden() {
        return !config.enableGeQuickFilter();
    }

    public boolean isAtGrandExchange() {
        WorldPoint playerLoc = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
        if (playerLoc == null) {
            return false;
        }
        return playerLoc.getRegionID() == 12598; // Grand Exchange region
    }

    private Widget createGraphic(Widget parent, String name, int spriteId, int width, int height, int x, int y) {
        Widget widget = parent.createChild(-1, WidgetType.GRAPHIC);
        widget.setName(name);
        widget.setSpriteId(spriteId);
        widget.setOriginalWidth(width);
        widget.setOriginalHeight(height);
        widget.setOriginalX(x);
        widget.setOriginalY(y);
        widget.revalidate();
        return widget;
    }
}
