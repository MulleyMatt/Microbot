package net.runelite.client.plugins.microbot.spindel.overlays;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.client.plugins.microbot.spindel.SpindelBankInterface;
import net.runelite.client.plugins.microbot.spindel.SpindelConfig;
import net.runelite.client.plugins.microbot.spindel.SpindelGrandExchangeInterface;
import net.runelite.client.plugins.microbot.spindel.SpindelPhase;
import net.runelite.client.plugins.microbot.spindel.SpindelPlugin;
import net.runelite.client.plugins.microbot.spindel.SpindelSupplyCalculator;
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
 * HUD status panel for Spindel Helper:
 * - Inside Spindel's cave: displays current Ranged/Magic phase, remaining attacks (1-8),
 *   Auto-Prayer status, active Spiderlings, and Anti-PK / Royal Seed Pod status.
 * - At Bank / Grand Exchange: displays trip supply prep checklist.
 */
public class SpindelHudOverlay extends OverlayPanel
{
    private final Client client;
    private final SpindelPlugin plugin;
    private final SpindelConfig config;
    private final SpindelSupplyCalculator supplyCalculator;
    private final SpindelBankInterface bankInterface;
    private final SpindelGrandExchangeInterface geInterface;

    @Inject
    public SpindelHudOverlay(
            Client client,
            SpindelPlugin plugin,
            SpindelConfig config,
            SpindelSupplyCalculator supplyCalculator,
            SpindelBankInterface bankInterface,
            SpindelGrandExchangeInterface geInterface)
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

        boolean inCave = plugin.isInSpindelCave();
        boolean showSupplyPrep = !inCave && (bankInterface.isBankOpen() || geInterface.isAtGrandExchange());

        if (!inCave && !showSupplyPrep)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(225, 0));

        if (inCave)
        {
            renderInCaveHud();
        }
        else
        {
            renderSupplyPrepHud();
        }

        return super.render(graphics);
    }

    private void renderInCaveHud()
    {
        boolean hasPker = !plugin.getDetectedThreatPlayers().isEmpty();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(hasPker ? "!! PKER IN CAVE !!" : "Spindel Helper")
                .color(hasPker ? Color.RED : new Color(0, 210, 211))
                .build());

        // 1. Anti-PK & Seed Pod status
        if (hasPker)
        {
            Player pker = plugin.getDetectedThreatPlayers().get(0);
            String pkerName = pker.getName() != null ? pker.getName() : "Enemy";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Threat:")
                    .right(pkerName + " (Lvl " + pker.getCombatLevel() + ")")
                    .rightColor(Color.RED)
                    .build());
        }

        boolean hasSeedPod = supplyCalculator.hasRoyalSeedPodInInventory();
        boolean teleblocked = plugin.isTeleblocked();
        if (teleblocked)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Escape:")
                    .right("TELEBLOCKED - EXIT!")
                    .rightColor(Color.RED)
                    .build());
        }
        else
        {
            String podStatus = !hasSeedPod
                    ? "MISSING!"
                    : (config.autoSeedPodOnPker() ? "Ready (Auto ON)" : "Ready (Manual)");
            Color podColor = !hasSeedPod
                    ? Color.RED
                    : (config.autoSeedPodOnPker() ? Color.GREEN : Color.YELLOW);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Seed Pod:")
                    .right(podStatus)
                    .rightColor(podColor)
                    .build());
        }

        // 2. Boss rotation & Prayer status
        if (plugin.getSpindelNpc() != null && !plugin.getSpindelNpc().isDead())
        {
            SpindelPhase phase = plugin.getCurrentPhase();
            int remaining = plugin.getRemainingAttacksInPhase();
            int done = plugin.getAttacksInCurrentPhase();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Phase:")
                    .right(phase.getDisplayName())
                    .rightColor(phase.getColor())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Attacks in Style:")
                    .right(done + " / 8 (" + remaining + " left)")
                    .rightColor(remaining <= 2 ? Color.YELLOW : Color.WHITE)
                    .build());

            Prayer requiredPrayer = plugin.getRequiredOverheadPrayer();
            if (requiredPrayer != null)
            {
                boolean prayerActive = client.isPrayerActive(requiredPrayer);
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Overhead:")
                        .right(phase.getPrayerPrompt())
                        .rightColor(prayerActive ? Color.GREEN : Color.RED)
                        .build());
            }

            if (plugin.isPlayerInWebDanger())
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Hazard:")
                        .right("ON WEB - MOVE!")
                        .rightColor(Color.RED)
                        .build());
            }

            int spiderlingCount = plugin.getSpiderlings().size();
            if (spiderlingCount > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Spiderlings:")
                        .right(spiderlingCount + " active (Keep Boss Pray)")
                        .rightColor(config.spiderlingColor())
                        .build());
            }
        }
        else
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Boss Status:")
                    .right("Waiting for Spindel...")
                    .rightColor(Color.LIGHT_GRAY)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Auto-Prayer:")
                .right(config.autoSwitchPrayers() ? "ON" : "OFF")
                .rightColor(config.autoSwitchPrayers() ? Color.GREEN : Color.GRAY)
                .build());
    }

    private void renderSupplyPrepHud()
    {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Spindel Trip Supplies")
                .color(new Color(0, 210, 211))
                .build());

        List<SpindelSupplyCalculator.SupplyStatus> statuses = supplyCalculator.evaluateSupplies();
        boolean allReady = true;

        for (SpindelSupplyCalculator.SupplyStatus status : statuses)
        {
            SpindelSupplyCalculator.SupplyRequirement req = status.getRequirement();
            boolean ready = status.isReadyInInventory();
            if (!ready)
            {
                allReady = false;
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(req.getDisplayName())
                    .right(status.getCarriedQuantity() + " / " + req.getSingleTripQuantity())
                    .rightColor(ready ? Color.GREEN : Color.YELLOW)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right(allReady ? "Ready for Wilderness!" : "Use Bank/GE Filter")
                .rightColor(allReady ? Color.GREEN : Color.ORANGE)
                .build());
    }
}
