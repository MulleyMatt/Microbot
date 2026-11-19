package net.runelite.client.plugins.microbot.wgs.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.wgs.WgsBossStage;
import net.runelite.client.plugins.microbot.wgs.WgsConfig;
import net.runelite.client.plugins.microbot.wgs.WgsOverlayUtils;
import net.runelite.client.plugins.microbot.wgs.WgsPlugin;
import net.runelite.client.plugins.microbot.wgs.WgsSurokMechanic;
import net.runelite.client.plugins.microbot.wgs.WgsSupplyCalculator;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Widget highlights (ABOVE_WIDGETS):
 * 1. Prayer tab marker for the required overhead prayer across all 3 bosses.
 * 2. Standard Spellbook marker for Surok Magis specials (Weaken, Bind/Entangle, Telegrab, Alch, opposite Elemental spells).
 * 3. Inventory item marker for Lit Explosive ("ALCH NOW!") and Post-Fight Teleorb ("USE ON SUROK!").
 */
public class WgsWidgetOverlay extends Overlay
{
    private static final int VARCLIENT_INVENTORY_TAB = 171;
    private static final int INVENTORY_TAB_INDEX = 3;
    private static final int PRAYER_TAB_INDEX = 5;
    private static final int MAGIC_TAB_INDEX = 6;

    private final Client client;
    private final WgsPlugin plugin;
    private final WgsConfig config;

    @Inject
    public WgsWidgetOverlay(Client client, WgsPlugin plugin, WgsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInAnyBossFight())
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeSmallFont());
        int currentTab = client.getVarcIntValue(VARCLIENT_INVENTORY_TAB);

        if (config.prayerMarker() && currentTab == PRAYER_TAB_INDEX)
        {
            renderPrayerMarker(graphics);
        }

        if (config.spellbookMarker() && plugin.getActiveStage() == WgsBossStage.SUROK_MAGIS)
        {
            WgsSurokMechanic mechanic = plugin.getActiveSurokMechanic();
            if (mechanic != WgsSurokMechanic.NONE)
            {
                if (currentTab == MAGIC_TAB_INDEX)
                {
                    renderSpellbookMarkers(graphics, mechanic);
                }
                if (currentTab == INVENTORY_TAB_INDEX)
                {
                    renderInventoryItemMarkers(graphics, mechanic);
                }
            }
        }

        return null;
    }

    private void renderPrayerMarker(Graphics2D graphics)
    {
        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
        if (requiredPrayer == null)
        {
            return;
        }

        int componentId = prayerToComponentId(requiredPrayer);
        if (componentId == -1)
        {
            return;
        }

        Widget prayerWidget = client.getWidget(componentId);
        if (prayerWidget != null && !prayerWidget.isHidden())
        {
            boolean active = client.isPrayerActive(requiredPrayer);
            Color color = active ? Color.GREEN : Color.RED;
            WgsOverlayUtils.renderWidgetPolygon(graphics, prayerWidget, color, true, !active);
        }
    }

    private void renderSpellbookMarkers(Graphics2D graphics, WgsSurokMechanic mechanic)
    {
        int magicLevel = client.getBoostedSkillLevel(Skill.MAGIC);
        int[] spellWidgetIds = WgsSurokMechanic.resolveAllSpellWidgetIds(mechanic, magicLevel);
        for (int widgetId : spellWidgetIds)
        {
            Widget spellWidget = client.getWidget(widgetId);
            if (spellWidget != null && !spellWidget.isHidden())
            {
                WgsOverlayUtils.renderWidgetPolygon(graphics, spellWidget, mechanic.getColor(), true, true);
                drawBoundsLabel(graphics, spellWidget.getBounds(), mechanic.getWidgetLabel(), mechanic.getColor());
            }
        }
    }

    private void renderInventoryItemMarkers(Graphics2D graphics, WgsSurokMechanic mechanic)
    {
        Set<Integer> targetIds = new HashSet<>();
        String label = null;
        if (mechanic == WgsSurokMechanic.LIT_EXPLOSIVE_INVENTORY)
        {
            targetIds.add(ItemID.WGS_BOMB);
            label = "ALCH!";
        }
        else if (mechanic == WgsSurokMechanic.POST_FIGHT_TELEORB)
        {
            for (int id : WgsSupplyCalculator.TELEORB_VARIANT_IDS)
            {
                targetIds.add(id);
            }
            label = "USE!";
        }
        else
        {
            return;
        }

        Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
        if (invWidget == null || invWidget.isHidden())
        {
            return;
        }

        Widget[] children = invWidget.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            children = invWidget.getChildren();
        }
        if (children == null)
        {
            return;
        }

        for (Widget item : children)
        {
            if (item != null && item.getItemId() > 0 && targetIds.contains(item.getItemId()))
            {
                WgsOverlayUtils.renderWidgetPolygon(graphics, item, mechanic.getColor(), true, true);
                drawBoundsLabel(graphics, item.getBounds(), label, mechanic.getColor());
            }
        }
    }

    private static void drawBoundsLabel(Graphics2D graphics, Rectangle bounds, String label, Color color)
    {
        if (bounds == null || label == null || label.isEmpty())
        {
            return;
        }
        int x = bounds.x + 2;
        int y = bounds.y + bounds.height - 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(label, x + 1, y + 1);
        graphics.setColor(color);
        graphics.drawString(label, x, y);
    }

    public static int prayerToComponentId(Prayer prayer)
    {
        if (prayer == null)
        {
            return -1;
        }
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                return InterfaceID.Prayerbook.PRAYER13;
            case PROTECT_FROM_MISSILES:
                return InterfaceID.Prayerbook.PRAYER14;
            case PROTECT_FROM_MELEE:
                return InterfaceID.Prayerbook.PRAYER15;
            default:
                return -1;
        }
    }
}
