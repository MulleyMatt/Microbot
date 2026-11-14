package net.runelite.client.plugins.microbot.barbarianfishing;

import com.google.common.primitives.Shorts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.gameval.InterfaceID;
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
public class BarbarianFishingGrandExchangeInterface {

    public static final String BARB_FISH_SEARCH_TAG = "barbarian-fishing-supplies";

    @Setter
    @Getter
    private boolean active = false;

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;
    private Widget titleWidget;

    private short[] cachedTradeableIds = new short[0];

    private final Client client;
    private final ClientThread clientThread;
    private final BarbarianFishingConfig config;
    private final ItemManager itemManager;

    @Inject
    public BarbarianFishingGrandExchangeInterface(Client client, ClientThread clientThread,
                                                   BarbarianFishingConfig config, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
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

        int buttonSize = 20;
        int buttonX = 405; // Positioned left of Redwood (430), HerbRun (455), and QuestHelper (480)
        int buttonY = 0;

        buttonBackgroundWidget = createGraphic(parent, "Barbarian Fishing Gear",
                SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL,
                buttonSize, buttonSize, buttonX, buttonY);
        buttonBackgroundWidget.setAction(1, "View Barbarian Fishing Supplies");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

        iconWidget = createGraphic(parent, "Barbarian Fishing Gear",
                net.runelite.api.SpriteID.SKILL_FISHING,
                16, 16, buttonX + 2, buttonY + 2);

        int titleX = 80;
        int titleY = 4;
        int titleWidth = 300;
        int titleHeight = 20;

        titleWidget = parent.createChild(-1, WidgetType.TEXT);
        titleWidget.setText("Barbarian Fishing Supplies");
        titleWidget.setFontId(FontID.BOLD_12);
        titleWidget.setTextColor(JagexColors.DARK_ORANGE_INTERFACE_TEXT.getRGB());
        titleWidget.setTextShadowed(true);
        titleWidget.setOriginalX(titleX);
        titleWidget.setOriginalY(titleY);
        titleWidget.setOriginalWidth(titleWidth);
        titleWidget.setOriginalHeight(titleHeight);
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

        client.setVarcStrValue(VarClientID.MESLAYERINPUT, BARB_FISH_SEARCH_TAG);
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
        if (!BARB_FISH_SEARCH_TAG.equals(input)) {
            return;
        }

        event.consume();

        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(cachedTradeableIds.length);
        client.setGeSearchResultIds(cachedTradeableIds);
    }

    private void refreshTradeableIds() {
        Set<Integer> tradeableIds = new TreeSet<>();
        for (int itemId : BarbarianFishingSupplyCalculator.TRADEABLE_SUPPLIES) {
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
        cachedTradeableIds = Shorts.toArray(shortList);
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
