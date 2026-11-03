package net.runelite.client.plugins.microbot.createhelper.templates;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.bank.BankSearch;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

/**
 * Reusable template for 1-click Bank item filtering via native RuneLite CS2 callbacks.
 * <p>
 * Subscribes to ScriptCallbackEvent with priority = -1 to run after default BankTags filters.
 * Injects a button into InterfaceID.Bankmain.UNIVERSE at x = 380, y = 5 (left of Quest Helper at 408).
 */
@Slf4j
@Singleton
public class BankFilterTemplate {

    @Getter
    @Setter
    private boolean active = false;

    private Set<Integer> cachedFilterItemIds = Collections.emptySet();

    private Widget parent;
    private Widget buttonBackgroundWidget;
    private Widget iconWidget;

    private final Client client;
    private final ClientThread clientThread;
    private final BankSearch bankSearch;

    @Inject
    public BankFilterTemplate(Client client, ClientThread clientThread, BankSearch bankSearch) {
        this.client = client;
        this.clientThread = clientThread;
        this.bankSearch = bankSearch;
    }

    public boolean isBankOpen() {
        Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return universe != null && !universe.isHidden();
    }

    /**
     * Call on WidgetLoaded (InterfaceID.BANKMAIN) or ScriptPostFired (ScriptID.BANKMAIN_BUILD).
     */
    public void init(int iconSpriteId, String tooltip, Runnable missingItemsSupplier) {
        if (!isBankOpen()) return;

        parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (parent == null) return;

        cleanupWidgets();

        int buttonSize = 25;
        int buttonX = 380; // Left of Quest Helper (408)
        int buttonY = 5;

        int backgroundSprite = active
                ? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
                : SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL;

        buttonBackgroundWidget = parent.createChild(-1, WidgetType.GRAPHIC);
        buttonBackgroundWidget.setSpriteId(backgroundSprite);
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
            closeFilter();
        } else {
            activateFilter(missingItemsSupplier);
        }
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    public void activateFilter(Runnable missingItemsSupplier) {
        if (missingItemsSupplier != null) {
            missingItemsSupplier.run();
        }
        active = true;
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED);
            buttonBackgroundWidget.revalidate();
        }
        clientThread.invokeLater(bankSearch::layoutBank);
    }

    public void closeFilter() {
        active = false;
        if (buttonBackgroundWidget != null) {
            buttonBackgroundWidget.setSpriteId(SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL);
            buttonBackgroundWidget.revalidate();
        }
        clientThread.invokeLater(bankSearch::layoutBank);
    }

    public void setFilterItemIds(Set<Integer> itemIds) {
        this.cachedFilterItemIds = itemIds != null ? itemIds : Collections.emptySet();
    }

    /**
     * Subscribe with @Subscribe(priority = -1) in main plugin.
     */
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        String eventName = event.getEventName();
        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();

        if ("bankBuildTab".equals(eventName)) {
            if (active) {
                // Set single tab view / filter mode for Script 277
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
                boolean show = cachedFilterItemIds.contains(itemId);
                // Return 1 to show item, 0 to hide item (cc_sethide)
                intStack[intStackSize - 2] = show ? 1 : 0;
            }
        }
    }

    public void onScriptPreFired(ScriptPreFired event, String headerTitle) {
        if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING && active) {
            Widget bankTitle = client.getWidget(InterfaceID.Bankmain.TITLE);
            if (bankTitle != null && headerTitle != null) {
                bankTitle.setText(headerTitle);
            }
        } else if (event.getScriptId() == ScriptID.BANKMAIN_SEARCH_TOGGLE && active) {
            closeFilter();
        }
    }

    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            active = false;
            cleanupWidgets();
        }
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
