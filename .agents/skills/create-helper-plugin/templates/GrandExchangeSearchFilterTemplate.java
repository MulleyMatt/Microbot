package net.runelite.client.plugins.microbot.createhelper.templates;

import com.google.common.primitives.Shorts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reusable template for injecting a 1-click filter button into the Grand Exchange search bar.
 * <p>
 * CRITICAL CS2 REENTRANCY RULE:
 * Never run scripts (e.g. quest checks, supply evaluations) inside onGrandExchangeSearched.
 * Always pre-compute missing tradeable IDs into cachedMissingTradeableIds BEFORE triggering search.
 * Never set client.setVarcIntValue(66, 1) or call script 751 directly.
 */
@Slf4j
@Singleton
public class GrandExchangeSearchFilterTemplate {

    public static final String SEARCH_TAG = "my-plugin-supplies";
    private static final int SEARCHBOX_LOADED = 750;

    @Getter
    @Setter
    private boolean active = false;

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;

    private short[] cachedMissingTradeableIds = new short[0];

    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;

    @Inject
    public GrandExchangeSearchFilterTemplate(Client client, ClientThread clientThread, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
    }

    /**
     * Call from main plugin on @Subscribe ScriptPostFired.
     */
    public void onScriptPostFired(ScriptPostFired event, int iconSpriteId, String tooltip, Runnable missingItemsSupplier) {
        if (event.getScriptId() != SEARCHBOX_LOADED) {
            return;
        }

        parent = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        if (parent == null || parent.isHidden()) {
            return;
        }

        cleanupWidgets();

        int buttonSize = 20;
        int buttonX = 455; // Left of Quest Helper (480) with zero collision
        int buttonY = 0;

        buttonBackgroundWidget = parent.createChild(-1, WidgetType.GRAPHIC);
        buttonBackgroundWidget.setSpriteId(active
                ? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
                : SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
        buttonBackgroundWidget.setOriginalWidth(buttonSize);
        buttonBackgroundWidget.setOriginalHeight(buttonSize);
        buttonBackgroundWidget.setOriginalX(buttonX);
        buttonBackgroundWidget.setOriginalY(buttonY);
        buttonBackgroundWidget.setXPositionMode(0);
        buttonBackgroundWidget.setYPositionMode(0);
        buttonBackgroundWidget.setHasListener(true);
        buttonBackgroundWidget.setAction(1, tooltip);
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) ev -> toggle(missingItemsSupplier));
        buttonBackgroundWidget.revalidate();

        // Icon inside button
        iconWidget = parent.createChild(-1, WidgetType.GRAPHIC);
        iconWidget.setSpriteId(iconSpriteId);
        iconWidget.setOriginalWidth(buttonSize - 4);
        iconWidget.setOriginalHeight(buttonSize - 4);
        iconWidget.setOriginalX(buttonX + 2);
        iconWidget.setOriginalY(buttonY + 2);
        iconWidget.setXPositionMode(0);
        iconWidget.setYPositionMode(0);
        iconWidget.revalidate();
    }

    public void toggle(Runnable missingItemsSupplier) {
        if (active) {
            close();
        } else {
            activate(missingItemsSupplier);
        }
    }

    public void activate(Runnable missingItemsSupplier) {
        if (active) return;
        active = true;

        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }

        // 1. Precompute IDs BEFORE firing search script
        if (missingItemsSupplier != null) {
            missingItemsSupplier.run();
        }

        // 2. Set search mode varcs
        client.setVarcStrValue(VarClientID.MESLAYERINPUT, SEARCH_TAG);
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

        // 3. Trigger native search via chatbox listener on next tick
        clientThread.invokeLater(() -> updateSearchInterface(true));
    }

    public void close() {
        active = false;
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
        clientThread.invokeLater(() -> updateSearchInterface(false));
    }

    /**
     * Call from main plugin on @Subscribe GrandExchangeSearched.
     * Keep this strictly in-memory — NEVER call scripts or quest checks here.
     */
    public void onGrandExchangeSearched(GrandExchangeSearched event) {
        final String input = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if (!SEARCH_TAG.equals(input)) {
            return;
        }

        event.consume();

        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(cachedMissingTradeableIds.length);
        client.setGeSearchResultIds(cachedMissingTradeableIds);
    }

    public void setMissingTradeableItemIds(Collection<Integer> rawItemIds) {
        Set<Integer> tradeable = rawItemIds.stream()
                .filter(id -> itemManager.getItemComposition(id).isGeTradeable())
                .collect(Collectors.toCollection(TreeSet::new));
        this.cachedMissingTradeableIds = Shorts.toArray(tradeable);
    }

    private void updateSearchInterface(boolean hideSearchBox) {
        Widget geSearchBox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
        if (geSearchBox == null) return;
        Object[] scriptArgs = geSearchBox.getOnKeyListener();
        if (scriptArgs == null) return;
        client.runScript(scriptArgs);
        geSearchBox.setHidden(hideSearchBox);
    }

    public void cleanupWidgets() {
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setHidden(true);
            buttonBackgroundWidget = null;
        }
        if (iconWidget != null) {
            iconWidget.setHidden(true);
            iconWidget = null;
        }
    }
}
