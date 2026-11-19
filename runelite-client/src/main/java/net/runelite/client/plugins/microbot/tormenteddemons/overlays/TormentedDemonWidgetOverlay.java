package net.runelite.client.plugins.microbot.tormenteddemons.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonConfig;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonGearClassifier;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonOverlayUtils;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonPlugin;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonState;
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

/**
 * Widget highlights (ABOVE_WIDGETS):
 * 1. Prayer Tab: Required protection prayer (Green = active, Red = inactive).
 * 2. Inventory Tab:
 *    - Priority 1 (Gold "PUNISH"): Slow Crush/Heavy Ranged punish weapon during Shield Down.
 *    - Priority 2 (Cyan "SPEC"): Spec weapon during 100% accuracy Defenceless phase.
 *    - Priority 3 (Style Color): Opposite style gear switches when demon prays against equipped weapon.
 *    - Utility: Smouldering drops ("USE"), Prayer pots ("PRAY"), Food ("EAT").
 * 3. Magic Tab (Arceuus): Demonic Offering ("OFFER") at 3+ ashes and Death Charge ("SPEC+") near kill.
 */
public class TormentedDemonWidgetOverlay extends Overlay
{
    private static final int VARCLIENT_INVENTORY_TAB = 171;
    private static final int INVENTORY_TAB_INDEX = 3;
    private static final int PRAYER_TAB_INDEX = 5;
    private static final int MAGIC_TAB_INDEX = 6;

    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color PURPLE = new Color(195, 130, 255);

    private final Client client;
    private final TormentedDemonPlugin plugin;
    private final TormentedDemonConfig config;

    @Inject
    public TormentedDemonWidgetOverlay(Client client, TormentedDemonPlugin plugin, TormentedDemonConfig config)
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
        if (!plugin.isInGuthixianTemple())
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeSmallFont());
        int currentTab = client.getVarcIntValue(VARCLIENT_INVENTORY_TAB);

        if (config.prayerMarker() && currentTab == PRAYER_TAB_INDEX && plugin.isInActiveFight())
        {
            renderPrayerMarker(graphics);
        }

        if (currentTab == INVENTORY_TAB_INDEX)
        {
            renderInventoryHighlights(graphics);
        }

        if (config.highlightArceuusSpells() && currentTab == MAGIC_TAB_INDEX)
        {
            renderArceuusPrompts(graphics);
        }

        return null;
    }

    private void renderPrayerMarker(Graphics2D graphics)
    {
        Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
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
            TormentedDemonOverlayUtils.renderWidgetPolygon(graphics, prayerWidget, color, true, !active);
        }
    }

    private void renderInventoryHighlights(Graphics2D graphics)
    {
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

        TormentedDemonState primary = plugin.getPrimaryDemonState();
        boolean activeFight = primary != null && plugin.isInActiveFight();
        boolean shieldDown = activeFight && primary.isShieldDown() && config.highlightPunishWeapon();
        boolean defenceless = activeFight && primary.isDefenceless() && config.highlightSpecWeapon();

        TormentedDemonGearClassifier.CombatStyle targetStyle = activeFight
                ? TormentedDemonGearClassifier.resolveTargetStyleForDemon(primary.getProtectedStyles(), config.combatStylePair())
                : null;
        boolean needStyleSwitch = activeFight
                && config.highlightInventoryGearSwitches()
                && (TormentedDemonGearClassifier.isWeaponPrayedAgainst(plugin.getEquippedWeaponStyle(), primary.getProtectedStyles())
                || (plugin.getEquippedWeaponStyle() != null && plugin.getEquippedWeaponStyle() != targetStyle));

        boolean lowHp = plugin.isLowHpInCombat();
        boolean lowPray = plugin.isLowPrayerInCombat();
        int specEnergy = plugin.getSpecEnergyPercent();

        for (Widget item : children)
        {
            if (item == null || item.isHidden() || item.getItemId() <= 0)
            {
                continue;
            }

            int itemId = item.getItemId();
            String itemName = plugin.getCachedItemName(itemId, item.getName());

            Color color = null;
            String label = null;
            boolean flash = false;

            if (shieldDown && TormentedDemonGearClassifier.isPunishWeapon(itemId, itemName))
            {
                color = GOLD;
                label = "PUNISH";
                flash = true;
            }
            else if (defenceless)
            {
                int specCost = TormentedDemonGearClassifier.getSpecWeaponCostPercent(itemId, itemName);
                if (specCost > 0 && specEnergy >= specCost)
                {
                    color = Color.CYAN;
                    label = "SPEC";
                    flash = true;
                }
            }

            if (color == null && needStyleSwitch
                    && TormentedDemonGearClassifier.isGearForStyle(targetStyle, itemId, itemName))
            {
                color = targetStyle.getColor();
                label = targetStyle.getLabel();
                flash = true;
            }

            if (color == null && config.highlightSmoulderingInInventory()
                    && TormentedDemonGearClassifier.isSmoulderingConsumable(itemId))
            {
                color = Color.ORANGE;
                label = "USE";
            }
            else if (color == null && lowHp && TormentedDemonGearClassifier.isFoodItem(itemId, itemName))
            {
                color = Color.RED;
                label = "EAT";
                flash = true;
            }
            else if (color == null && lowPray && TormentedDemonGearClassifier.isPrayerRestoreItem(itemId, itemName))
            {
                color = Color.CYAN;
                label = "PRAY";
                flash = true;
            }

            if (color != null)
            {
                TormentedDemonOverlayUtils.renderWidgetPolygon(graphics, item, color, true, flash);
                drawBoundsLabel(graphics, item.getBounds(), label, color);
            }
        }
    }

    private void renderArceuusPrompts(Graphics2D graphics)
    {
        if (plugin.getAshesCount() >= 3 && client.getVarbitValue(VarbitID.ARCEUUS_OFFERING_COOLDOWN) == 0)
        {
            Widget offerWidget = client.getWidget(InterfaceID.MagicSpellbook.DEMONIC_OFFERING);
            if (offerWidget != null && !offerWidget.isHidden())
            {
                TormentedDemonOverlayUtils.renderWidgetPolygon(graphics, offerWidget, PURPLE, true, true);
                drawBoundsLabel(graphics, offerWidget.getBounds(), "OFFER", PURPLE);
            }
        }

        TormentedDemonState primary = plugin.getPrimaryDemonState();
        if (primary != null && plugin.isInActiveFight()
                && primary.getEstimatedHp() <= 160
                && plugin.getSpecEnergyPercent() < 100
                && client.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE) == 0
                && client.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN) == 0)
        {
            Widget dcWidget = client.getWidget(InterfaceID.MagicSpellbook.DEATH_CHARGE);
            if (dcWidget != null && !dcWidget.isHidden())
            {
                TormentedDemonOverlayUtils.renderWidgetPolygon(graphics, dcWidget, Color.CYAN, true, true);
                drawBoundsLabel(graphics, dcWidget.getBounds(), "SPEC+", Color.CYAN);
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
