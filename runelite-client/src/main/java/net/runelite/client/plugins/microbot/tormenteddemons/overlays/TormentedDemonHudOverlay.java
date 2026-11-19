package net.runelite.client.plugins.microbot.tormenteddemons.overlays;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonBankInterface;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonConfig;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonGearClassifier;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonGrandExchangeInterface;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonOverlayUtils;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonPlugin;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonState;
import net.runelite.client.plugins.microbot.tormenteddemons.TormentedDemonSupplyCalculator;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Context-aware HUD status panel for Tormented Demons Helper.
 * Displays live fight mechanics, room safety, Arceuus reminders, and GP/hr profit inside
 * the Ancient Guthixian Temple, and displays the Supply Prep checklist at the Bank / GE.
 */
public class TormentedDemonHudOverlay extends OverlayPanel
{
    private final Client client;
    private final TormentedDemonPlugin plugin;
    private final TormentedDemonConfig config;
    private final TormentedDemonSupplyCalculator supplyCalculator;
    private final TormentedDemonBankInterface bankInterface;
    private final TormentedDemonGrandExchangeInterface geInterface;

    @Inject
    public TormentedDemonHudOverlay(
            Client client,
            TormentedDemonPlugin plugin,
            TormentedDemonConfig config,
            TormentedDemonSupplyCalculator supplyCalculator,
            TormentedDemonBankInterface bankInterface,
            TormentedDemonGrandExchangeInterface geInterface)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.supplyCalculator = supplyCalculator;
        this.bankInterface = bankInterface;
        this.geInterface = geInterface;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showHudOverlay())
        {
            return null;
        }

        boolean inTemple = plugin.isInGuthixianTemple();
        boolean showSupplyPrep = !inTemple && (bankInterface.isBankOpen() || geInterface.isAtGrandExchange());
        if (!inTemple && !showSupplyPrep)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(255, 0));

        if (inTemple)
        {
            renderTempleHud();
        }
        else
        {
            renderSupplyPrepHud();
        }

        return super.render(graphics);
    }

    private void renderTempleHud()
    {
        TormentedDemonState primary = plugin.getPrimaryDemonState();
        boolean activeFight = primary != null && plugin.isInActiveFight();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Tormented Demons")
                .color(activeFight && primary.isShieldDown()
                        ? new Color(255, 210, 40)
                        : new Color(255, 115, 55))
                .build());

        // Room Multi-Combat Status
        int multi = plugin.getTdMultiwayLevel();
        String roomText = multi >= 3 ? "TRIPLE MULTI (3x)!"
                : (multi == 2 ? "DOUBLE MULTI (2x)!" : (multi == 1 ? "Single (Safe)" : "Safe"));
        Color roomColor = multi >= 2 ? Color.RED : Color.GREEN;
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Room:")
                .right(roomText)
                .rightColor(roomColor)
                .build());

        if (plugin.isBoxedInNearWallOrCorner())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Position:")
                    .right("WALL/CORNER BOX-IN!")
                    .rightColor(Color.ORANGE)
                    .build());
        }

        if (activeFight)
        {
            // Required overhead prayer
            Prayer requiredPrayer = primary.getRequiredPrayer();
            String prayerName = formatPrayerName(requiredPrayer);
            boolean prayOn = requiredPrayer != null && client.isPrayerActive(requiredPrayer);
            String prayStatus = requiredPrayer == null
                    ? "Waiting..."
                    : (prayOn ? prayerName + " (ON)" : prayerName + "!")
                    + (primary.isPrayerPredicted() ? " [PRED]" : "");

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Overhead:")
                    .right(prayStatus)
                    .rightColor(requiredPrayer == null ? Color.LIGHT_GRAY : (prayOn ? Color.GREEN : Color.RED))
                    .build());

            // Target attack style + wrong gear warning
            TormentedDemonGearClassifier.CombatStyle targetStyle =
                    TormentedDemonGearClassifier.resolveTargetStyleForDemon(primary.getProtectedStyles(), config.combatStylePair());
            boolean wrongWeapon = TormentedDemonGearClassifier.isWeaponPrayedAgainst(
                    plugin.getEquippedWeaponStyle(), primary.getProtectedStyles());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Attack With:")
                    .right(wrongWeapon ? "SWAP TO " + targetStyle.getLabel() + "!" : targetStyle.getLabel())
                    .rightColor(wrongWeapon ? Color.RED : targetStyle.getColor())
                    .build());

            // 150-dmg overhead swap tracker
            int dmg = primary.getDamageSincePrayerSwap();
            int rem = primary.getDamageRemainingToPrayerSwap();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Prayer Swap:")
                    .right(dmg + " / 150 (" + rem + " left)")
                    .rightColor(rem <= 30 ? Color.YELLOW : Color.WHITE)
                    .build());

            // Shield & 100% Accuracy status
            String phaseText;
            Color phaseColor;
            if (primary.isShieldDown())
            {
                phaseText = "SHIELD DOWN -> PUNISH!";
                phaseColor = new Color(255, 215, 0);
            }
            else if (primary.isDefenceless())
            {
                phaseText = "100% ACCURACY (SPEC!)";
                phaseColor = Color.CYAN;
            }
            else
            {
                phaseText = "100% Acc in ~" + primary.getTicksUntilDefenceless() + "t";
                phaseColor = Color.LIGHT_GRAY;
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Phase:")
                    .right(phaseText)
                    .rightColor(phaseColor)
                    .build());

            // Fire Bomb timer
            boolean fireDanger = plugin.isPlayerInFireBombDanger();
            int fbTicks = primary.getTicksUntilFireBomb();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Fire Bomb:")
                    .right(fireDanger ? "DODGE 2+ TILES (CTRL)!" : "~" + fbTicks + "t")
                    .rightColor(fireDanger ? Color.RED : (fbTicks <= 6 ? Color.ORANGE : Color.LIGHT_GRAY))
                    .build());
        }
        else
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(plugin.isPostKillPrayerReminder() ? "Turn Off Prayers / Loot" : "Ready in Temple")
                    .rightColor(plugin.isPostKillPrayerReminder() ? Color.ORANGE : Color.CYAN)
                    .build());
        }

        // Arceuus & Consumable Prompts
        if (plugin.getAshesCount() >= 3)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Ashes:")
                    .right("Cast Demonic Offering (" + plugin.getAshesCount() + ")")
                    .rightColor(new Color(195, 130, 255))
                    .build());
        }
        if (plugin.isHasSmoulderingConsumable())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Sustain:")
                    .right("Eat Smouldering Drop!")
                    .rightColor(Color.ORANGE)
                    .build());
        }

        // Micro-assist status line
        String assists = "Pray:" + (config.autoSwitchPrayers() ? "ON" : "OFF")
                + " Thrall:" + (config.autoCastThrall() ? "ON" : "OFF");
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Assists:")
                .right(assists)
                .rightColor(Color.CYAN)
                .build());

        // Money-Making Profit Tracker
        if (config.showProfitTracker())
        {
            long elapsedMs = plugin.getSessionStartTimeMs() > 0L
                    ? Math.max(1_000L, System.currentTimeMillis() - plugin.getSessionStartTimeMs())
                    : 0L;
            long killsHr = TormentedDemonOverlayUtils.computeRatePerHour(plugin.getSessionKills(), elapsedMs);
            long gpHr = TormentedDemonOverlayUtils.computeRatePerHour(plugin.getSessionLootGp(), elapsedMs);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Kills:")
                    .right(plugin.getSessionKills() + " (" + killsHr + "/hr)")
                    .rightColor(Color.WHITE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Loot:")
                    .right(TormentedDemonOverlayUtils.formatGp(plugin.getSessionLootGp())
                            + " (" + TormentedDemonOverlayUtils.formatGp(gpHr) + "/hr)")
                    .rightColor(new Color(120, 255, 120))
                    .build());

            if (plugin.getSynapseCount() > 0 || plugin.getBurningClawCount() > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Uniques:")
                        .right("Synapse: " + plugin.getSynapseCount() + " | Claw: " + plugin.getBurningClawCount())
                        .rightColor(new Color(255, 215, 0))
                        .build());
            }
        }
    }

    private void renderSupplyPrepHud()
    {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("TD Prep Checklist")
                .color(Color.CYAN)
                .build());

        List<TormentedDemonSupplyCalculator.SupplyStatus> statuses = supplyCalculator.evaluateSupplies();
        for (TormentedDemonSupplyCalculator.SupplyStatus status : statuses)
        {
            TormentedDemonSupplyCalculator.SupplyRequirement req = status.getRequirement();
            boolean ready = status.isReadyInInventory();
            String countText = status.getCarriedQuantity() + " / " + req.getSingleTripQuantity();
            if (!ready && config.batchCount() > 1)
            {
                countText += " (Bank:" + status.getTotalOwnedWithBank() + ")";
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(req.getDisplayName())
                    .right(countText)
                    .rightColor(ready ? Color.GREEN : (status.getTotalOwnedWithBank() >= req.getSingleTripQuantity() ? Color.YELLOW : Color.RED))
                    .build());
        }
    }

    private static String formatPrayerName(Prayer prayer)
    {
        if (prayer == null)
        {
            return "Unknown";
        }
        switch (prayer)
        {
            case PROTECT_FROM_MELEE:
                return "Protect Melee";
            case PROTECT_FROM_MISSILES:
                return "Protect Missiles";
            case PROTECT_FROM_MAGIC:
                return "Protect Magic";
            default:
                return prayer.name();
        }
    }
}
