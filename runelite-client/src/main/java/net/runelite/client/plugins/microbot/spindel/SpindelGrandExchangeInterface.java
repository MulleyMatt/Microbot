package net.runelite.client.plugins.microbot.spindel;

import com.google.common.primitives.Shorts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;
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
 * Injects a 1-click "Spindel Supplies" filter button into the Grand Exchange search bar.
 * Pre-computes missing tradeable IDs before firing the search script to prevent CS2 reentrancy issues.
 */
@Slf4j
@Singleton
public class SpindelGrandExchangeInterface
{
    public static final String SEARCH_TAG = "spindel-trip-supplies";
    private static final int SEARCHBOX_LOADED = 750;
    private static final int GRAND_EXCHANGE_REGION_ID = 12598;

    @Getter
    @Setter
    private boolean active = false;

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;

    private short[] cachedMissingTradeableIds = new short[0];

    private final Client client;
    private final ClientThread clientThread;
    private final SpindelSupplyCalculator supplyCalculator;
    private final SpindelConfig config;
    private final ItemManager itemManager;

    @Inject
    public SpindelGrandExchangeInterface(
            Client client,
            ClientThread clientThread,
            SpindelSupplyCalculator supplyCalculator,
            SpindelConfig config,
            ItemManager itemManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.supplyCalculator = supplyCalculator;
        this.config = config;
        this.itemManager = itemManager;
    }

    public boolean isAtGrandExchange()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        return wp != null && wp.getRegionID() == GRAND_EXCHANGE_REGION_ID;
    }

    public void onScriptPostFired(ScriptPostFired event)
    {
        if (!config.enableGeFilter() || event.getScriptId() != SEARCHBOX_LOADED || !isAtGrandExchange())
        {
            return;
        }
        init();
    }

    public void init()
    {
        if (!config.enableGeFilter() || !isAtGrandExchange())
        {
            return;
        }

        parent = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        if (parent == null || parent.isHidden())
        {
            return;
        }

        cleanupWidgets();

        int buttonSize = 20;
        int buttonX = 455; // Left of Quest Helper (480)
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
        buttonBackgroundWidget.setAction(1, "Filter Spindel Supplies");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) ev -> toggle());
        buttonBackgroundWidget.revalidate();

        iconWidget = parent.createChild(-1, WidgetType.GRAPHIC);
        iconWidget.setSpriteId(SpriteID.IconBoss25x25.SPINDEL_VENENATIS);
        iconWidget.setOriginalWidth(buttonSize - 4);
        iconWidget.setOriginalHeight(buttonSize - 4);
        iconWidget.setOriginalX(buttonX + 2);
        iconWidget.setOriginalY(buttonY + 2);
        iconWidget.setXPositionMode(0);
        iconWidget.setYPositionMode(0);
        iconWidget.revalidate();
    }

    public void toggle()
    {
        if (active)
        {
            close();
        }
        else
        {
            activate();
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    public void activate()
    {
        if (active)
        {
            return;
        }
        active = true;

        if (buttonBackgroundWidget != null)
        {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }

        // 1. Pre-compute tradeable missing IDs BEFORE firing search script
        refreshMissingTradeableIds();

        // 2. Set search mode varcs
        client.setVarcStrValue(VarClientID.MESLAYERINPUT, SEARCH_TAG);
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

        // 3. Trigger native search via chatbox listener on next tick
        clientThread.invokeLater(() -> updateSearchInterface(true));
    }

    public void close()
    {
        if (!active)
        {
            return;
        }
        active = false;
        if (buttonBackgroundWidget != null)
        {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
        client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
        clientThread.invokeLater(() -> updateSearchInterface(false));
    }

    public void destroy()
    {
        active = false;
        cleanupWidgets();
    }

    public void onGrandExchangeSearched(GrandExchangeSearched event)
    {
        if (!config.enableGeFilter())
        {
            return;
        }
        String input = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if (!SEARCH_TAG.equals(input))
        {
            return;
        }

        event.consume();
        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(cachedMissingTradeableIds.length);
        client.setGeSearchResultIds(cachedMissingTradeableIds);
    }

    public void refreshMissingTradeableIds()
    {
        Collection<Integer> rawIds = supplyCalculator.getMissingTradeableItemIds();
        Set<Integer> tradeable = rawIds.stream()
                .filter(id -> {
                    try
                    {
                        return itemManager.getItemComposition(id).isGeTradeable();
                    }
                    catch (Exception e)
                    {
                        return true;
                    }
                })
                .collect(Collectors.toCollection(TreeSet::new));
        this.cachedMissingTradeableIds = Shorts.toArray(tradeable);
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

    public void cleanupWidgets()
    {
        if (buttonBackgroundWidget != null)
        {
            buttonBackgroundWidget.setHidden(true);
            buttonBackgroundWidget = null;
        }
        if (iconWidget != null)
        {
            iconWidget.setHidden(true);
            iconWidget = null;
        }
    }
}
