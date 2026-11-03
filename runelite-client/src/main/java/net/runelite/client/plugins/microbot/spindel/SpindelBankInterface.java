package net.runelite.client.plugins.microbot.spindel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Injects a 1-click "Spindel Supplies" filter button into the Bank interface header
 * using native CS2 callbacks (priority = -1).
 */
@Slf4j
@Singleton
public class SpindelBankInterface
{
    @Getter
    @Setter
    private boolean active = false;

    private Set<Integer> cachedMissingIds = Collections.emptySet();

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;

    private final Client client;
    private final ClientThread clientThread;
    private final BankSearch bankSearch;
    private final SpindelSupplyCalculator supplyCalculator;
    private final SpindelConfig config;
    private final ItemManager itemManager;

    @Inject
    public SpindelBankInterface(
            Client client,
            ClientThread clientThread,
            BankSearch bankSearch,
            SpindelSupplyCalculator supplyCalculator,
            SpindelConfig config,
            ItemManager itemManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.bankSearch = bankSearch;
        this.supplyCalculator = supplyCalculator;
        this.config = config;
        this.itemManager = itemManager;
    }

    public boolean isBankOpen()
    {
        Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return universe != null && !universe.isHidden();
    }

    public void init()
    {
        if (!config.enableBankFilter() || !isBankOpen())
        {
            return;
        }

        parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (parent == null)
        {
            return;
        }

        cleanupWidgets();

        int buttonSize = 25;
        int buttonX = 380; // Left of Quest Helper (408)
        int buttonY = 5;

        int backgroundSprite = active
                ? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
                : SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL;

        buttonBackgroundWidget = createGraphic(
                parent,
                "Spindel Supplies",
                backgroundSprite,
                buttonSize,
                buttonSize,
                buttonX,
                buttonY
        );
        buttonBackgroundWidget.setAction(1, "Toggle Spindel Supply Filter");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

        iconWidget = createGraphic(
                parent,
                "",
                SpriteID.IconBoss25x25.SPINDEL_VENENATIS,
                buttonSize - 4,
                buttonSize - 4,
                buttonX + 2,
                buttonY + 2
        );

        if (active)
        {
            refreshMissingCache();
            clientThread.invokeLater(bankSearch::layoutBank);
        }
    }

    public void destroy()
    {
        if (active)
        {
            active = false;
            clientThread.invokeLater(bankSearch::layoutBank);
        }
        cleanupWidgets();
        parent = null;
        cachedMissingIds = Collections.emptySet();
    }

    private void cleanupWidgets()
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

    private void handleTagTab(ScriptEvent event)
    {
        if (active)
        {
            closeFilter();
        }
        else
        {
            Set<Integer> missing = supplyCalculator.getMissingBankItemIds();
            if (missing.isEmpty())
            {
                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        "<col=00ff88>Spindel Helper:</col> All configured trip supplies are already in your inventory!",
                        null
                );
            }
            else
            {
                activateFilter();
            }
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    public void activateFilter()
    {
        active = true;
        refreshMissingCache();
        if (buttonBackgroundWidget != null)
        {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }

        client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
        clientThread.invokeLater(bankSearch::layoutBank);
    }

    public void closeFilter()
    {
        active = false;
        cachedMissingIds = Collections.emptySet();
        if (buttonBackgroundWidget != null)
        {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        clientThread.invokeLater(bankSearch::layoutBank);
    }

    private void refreshMissingCache()
    {
        Set<Integer> ids = new HashSet<>();
        for (int id : supplyCalculator.getMissingBankItemIds())
        {
            ids.add(id);
            ids.add(itemManager.canonicalize(id));
        }
        cachedMissingIds = ids;
    }

    public boolean shouldShowItem(int itemId)
    {
        if (!active)
        {
            return true;
        }
        return cachedMissingIds.contains(itemId) || cachedMissingIds.contains(itemManager.canonicalize(itemId));
    }

    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        if (!active)
        {
            return;
        }

        String eventName = event.getEventName();
        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();

        if ("bankBuildTab".equals(eventName))
        {
            intStack[intStackSize - 1] = 1;
        }
        else if ("getSearchingTagTab".equals(eventName))
        {
            intStack[intStackSize - 1] = 1;
        }
        else if ("bankSearchFilter".equals(eventName))
        {
            int itemId = intStack[intStackSize - 1];
            boolean show = shouldShowItem(itemId);
            intStack[intStackSize - 2] = show ? 1 : 0;
        }
    }

    public void onScriptPreFired(ScriptPreFired event)
    {
        int scriptId = event.getScriptId();
        if (scriptId == ScriptID.BANKMAIN_FINISHBUILDING && active)
        {
            Widget bankTitle = client.getWidget(InterfaceID.Bankmain.TITLE);
            if (bankTitle != null)
            {
                bankTitle.setText("Spindel Supplies <col=ff9040>(Missing)</col>");
            }
        }
        else if (scriptId == ScriptID.BANKMAIN_SEARCH_TOGGLE && active)
        {
            closeFilter();
        }
    }

    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == ScriptID.BANKMAIN_SEARCHING && active)
        {
            client.getIntStack()[client.getIntStackSize() - 1] = 1;
        }
    }

    public void handleClick(MenuOptionClicked event)
    {
        if (!active)
        {
            return;
        }
        String option = event.getMenuOption();
        if (option == null)
        {
            return;
        }

        boolean clickedOtherTab = option.startsWith("View tab")
                || option.equals("View all items")
                || option.startsWith("View tag tab")
                || option.startsWith("Potion store");
        if (clickedOtherTab)
        {
            closeFilter();
        }
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.INV && event.getContainerId() != InventoryID.BANK)
        {
            return;
        }
        if (!isBankOpen() || !active)
        {
            return;
        }

        refreshMissingCache();
        if (cachedMissingIds.isEmpty())
        {
            closeFilter();
        }
        else
        {
            clientThread.invokeLater(bankSearch::layoutBank);
        }
    }

    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.BANKMAIN)
        {
            init();
        }
    }

    public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == InterfaceID.BANKMAIN)
        {
            destroy();
        }
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
}
