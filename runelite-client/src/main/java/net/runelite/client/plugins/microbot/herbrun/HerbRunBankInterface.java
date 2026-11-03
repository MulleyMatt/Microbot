package net.runelite.client.plugins.microbot.herbrun;

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

@Slf4j
@Singleton
public class HerbRunBankInterface {

    @Getter
    @Setter
    private boolean active = false;

    private boolean autoFilteredThisSession = false;

    /** Missing-supply ids snapshot used by the per-slot bank filter callback. */
    private Set<Integer> cachedMissingIds = Collections.emptySet();

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;

    private final Client client;
    private final ClientThread clientThread;
    private final BankSearch bankSearch;
    private final HerbRunSupplyCalculator supplyCalculator;
    private final HerbRunConfig config;
    private final ItemManager itemManager;

    @Inject
    public HerbRunBankInterface(Client client, ClientThread clientThread, BankSearch bankSearch,
                                HerbRunSupplyCalculator supplyCalculator, HerbRunConfig config,
                                ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.bankSearch = bankSearch;
        this.supplyCalculator = supplyCalculator;
        this.config = config;
        this.itemManager = itemManager;
    }

    public boolean isBankOpen() {
        Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return universe != null && !universe.isHidden();
    }

    public void init() {
        if (!isBankOpen()) {
            return;
        }

        parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (parent == null) {
            return;
        }

        cleanupWidgets();

        int BUTTON_SIZE = 25;
        int BUTTON_X = 380; // Left of QuestHelper (408)
        int BUTTON_Y = 5;

        int backgroundSprite = active
                ? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
                : SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL;

        buttonBackgroundWidget = createGraphic(parent, "Herb Run Supplies",
                backgroundSprite,
                BUTTON_SIZE, BUTTON_SIZE, BUTTON_X, BUTTON_Y);
        buttonBackgroundWidget.setAction(1, "Toggle Herb Run Filter");
        buttonBackgroundWidget.setOnOpListener((JavaScriptCallback) this::handleTagTab);

        // Farming skill icon on the bank button
        iconWidget = createGraphic(parent, "", SpriteID.Staticons2.FARMING,
                BUTTON_SIZE - 6, BUTTON_SIZE - 6, BUTTON_X + 3, BUTTON_Y + 3);

        if (config.filterBankForMissingSupplies() && !autoFilteredThisSession) {
            Set<Integer> missingBankItems = supplyCalculator.getMissingBankItemIds();
            if (!missingBankItems.isEmpty()) {
                log.info("Herb Run: Auto-filtering bank for {} missing supplies", missingBankItems.size());
                autoFilteredThisSession = true;
                activateFilter();
            }
        } else if (active) {
            refreshMissingCache();
            clientThread.invokeLater(bankSearch::layoutBank);
        }
    }

    public void destroy() {
        if (active) {
            active = false;
            clientThread.invokeLater(bankSearch::layoutBank);
        }
        cleanupWidgets();
        parent = null;
        cachedMissingIds = Collections.emptySet();
        autoFilteredThisSession = false;
    }

    private void cleanupWidgets() {
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setHidden(true);
            buttonBackgroundWidget = null;
        }
        if (iconWidget != null) {
            iconWidget.setHidden(true);
            iconWidget = null;
        }
    }

    private void handleTagTab(ScriptEvent event) {
        if (active) {
            closeFilter();
        } else {
            Set<Integer> missing = supplyCalculator.getMissingBankItemIds();
            if (missing.isEmpty()) {
                // Nothing to show: activating would just display an empty bank.
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "<col=00ff88>Herb Run:</col> None of your missing supplies are in this bank - buy them at the GE.", null);
            } else {
                activateFilter();
            }
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    public void activateFilter() {
        active = true;
        refreshMissingCache();
        log.debug("Herb Run: bank filter active, showing {} item ids", cachedMissingIds.size());
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }

        // Set bank tab to 0 so all items across bank tabs are filtered
        client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
        clientThread.invokeLater(bankSearch::layoutBank);
    }

    public void closeFilter() {
        active = false;
        cachedMissingIds = Collections.emptySet();
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }

        clientThread.invokeLater(bankSearch::layoutBank);
    }

    private void refreshMissingCache() {
        Set<Integer> ids = new HashSet<>();
        for (int id : supplyCalculator.getMissingBankItemIds()) {
            ids.add(id);
            ids.add(itemManager.canonicalize(id));
        }
        cachedMissingIds = ids;
    }

    public boolean shouldShowItem(int itemId) {
        if (!active) {
            return true;
        }
        // Called once per bank slot during a layout - use the cached set, never recompute here.
        return cachedMissingIds.contains(itemId) || cachedMissingIds.contains(itemManager.canonicalize(itemId));
    }

    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        String eventName = event.getEventName();

        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();

        if ("bankBuildTab".equals(eventName)) {
            if (active) {
                // Set single tab view / filter mode for script 277
                intStack[intStackSize - 1] = 1;
            }
        } else if ("getSearchingTagTab".equals(eventName)) {
            if (active) {
                // Return true so TriggerBankLayout treats tag tab as active
                intStack[intStackSize - 1] = 1;
            }
        } else if ("bankSearchFilter".equals(eventName)) {
            if (active) {
                int itemId = intStack[intStackSize - 1];
                boolean show = shouldShowItem(itemId);
                // Return 1 to show item, 0 to hide item
                intStack[intStackSize - 2] = show ? 1 : 0;
            }
        }
    }

    public void onScriptPreFired(ScriptPreFired event) {
        int scriptId = event.getScriptId();

        if (scriptId == ScriptID.BANKMAIN_FINISHBUILDING) {
            if (active) {
                Widget bankTitle = client.getWidget(InterfaceID.Bankmain.TITLE);
                if (bankTitle != null) {
                    bankTitle.setText("Herb Run Supplies <col=ff9040>(Missing)</col>");
                }
            }
        } else if (scriptId == ScriptID.BANKMAIN_SEARCH_TOGGLE) {
            if (active) {
                // Close filter if native bank search toggle is pressed
                closeFilter();
            }
        }
    }

    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == ScriptID.BANKMAIN_SEARCHING) {
            if (active) {
                // Return 1 (true) from bankmain_searching so CS2 builds the bank in search/filter mode
                client.getIntStack()[client.getIntStackSize() - 1] = 1;
            }
        }
    }

    public void handleClick(MenuOptionClicked event) {
        String option = event.getMenuOption();
        if (option == null) {
            return;
        }

        // If player clicked another tab or potion store, close herb run filter
        boolean clickedOtherTab = option.startsWith("View tab") ||
                option.equals("View all items") ||
                option.startsWith("View tag tab") ||
                option.startsWith("Potion store");

        if (active && clickedOtherTab) {
            closeFilter();
        }
    }

    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV && event.getContainerId() != InventoryID.BANK) {
            return;
        }

        if (!isBankOpen()) {
            return;
        }

        if (active) {
            refreshMissingCache();
            if (cachedMissingIds.isEmpty()) {
                log.info("Herb Run: All missing bank supplies withdrawn! Restoring normal bank view.");
                closeFilter();
            } else {
                clientThread.invokeLater(bankSearch::layoutBank);
            }
        } else if (config.filterBankForMissingSupplies() && !autoFilteredThisSession) {
            Set<Integer> missingBankItems = supplyCalculator.getMissingBankItemIds();
            if (!missingBankItems.isEmpty()) {
                autoFilteredThisSession = true;
                activateFilter();
            }
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            init();
        }
    }

    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            destroy();
        }
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
}
