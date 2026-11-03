package net.runelite.client.plugins.microbot.herbrun;

import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

public class HerbRunOverlay extends OverlayPanel {

    private final HerbRunPlugin plugin;
    private final HerbRunConfig config;
    private final HerbRunSupplyCalculator supplyCalculator;
    private final HerbRunGrandExchangeInterface geInterface;
    private final HerbRunBankInterface bankInterface;

    @Inject
    public HerbRunOverlay(HerbRunPlugin plugin, HerbRunConfig config,
                          HerbRunSupplyCalculator supplyCalculator,
                          HerbRunGrandExchangeInterface geInterface,
                          HerbRunBankInterface bankInterface) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        this.supplyCalculator = supplyCalculator;
        this.geInterface = geInterface;
        this.bankInterface = bankInterface;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlayPanel()) {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(250, 0));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Herb Run Helper")
                .color(new Color(80, 220, 100))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Strategy:")
                .right(config.strategy() == HerbRunStrategy.PURE_RANARR ? "Pure Ranarr" : "Pro Split")
                .rightColor(Color.YELLOW)
                .build());

        boolean atGe = geInterface.isAtGrandExchange();
        HerbRunStep step = plugin.getCurrentStep();
        HerbPatch currentPatch = plugin.getCurrentPatch();
        int completedCount = plugin.getCompletedPatches().size();
        int totalPatches = supplyCalculator.getEnabledPatches().size();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Progress:")
                .right(completedCount + " / " + totalPatches)
                .rightColor(Color.WHITE)
                .build());

        int runs = config.runsToPurchase();
        String runLabel = runs == 1 ? "1 run" : runs + " runs";

        if (step == HerbRunStep.FINISHED) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("Run Complete!")
                    .rightColor(Color.GREEN)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Herbs Growing:")
                    .right("Ready in ~80m")
                    .rightColor(Color.WHITE)
                    .build());
        } else if (geInterface.isActive() || ((step == HerbRunStep.MISSING_SUPPLIES || step == HerbRunStep.CHECKING_SUPPLIES) && atGe && !supplyCalculator.getMissingTradeableItems(runs).isEmpty())) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("-- GE Quick-Buy (" + runLabel + ") --")
                    .color(Color.CYAN)
                    .build());

            List<HerbRunSupplyCalculator.SupplyItem> toBuy = supplyCalculator.getMissingTradeableItems(runs);
            for (HerbRunSupplyCalculator.SupplyItem item : toBuy) {
                int qty = item.getGeBuyQuantity(runs);
                if (qty > 0) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(item.getName())
                            .right("Buy x" + qty)
                            .rightColor(Color.PINK)
                            .build());
                }
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Quick Filter:")
                    .right("Click button in GE search")
                    .rightColor(Color.LIGHT_GRAY)
                    .build());
        } else if (step == HerbRunStep.MISSING_SUPPLIES || step == HerbRunStep.CHECKING_SUPPLIES) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("Missing Supplies")
                    .rightColor(Color.ORANGE)
                    .build());

            if (bankInterface.isActive()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Bank Filter:")
                        .right("Showing Needed")
                        .rightColor(Color.GREEN)
                        .build());
            }

            List<HerbRunSupplyCalculator.SupplyItem> supplies = supplyCalculator.calculateSupplies();
            int shown = 0;
            for (HerbRunSupplyCalculator.SupplyItem item : supplies) {
                if (item.isMissingFromInventory() && !item.isEquipmentBoost()) {
                    if (shown++ >= 6) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("...")
                                .right("and more")
                                .rightColor(Color.GRAY)
                                .build());
                        break;
                    }
                    String prefix;
                    Color color;
                    if (item.canWithdrawFromBank()) {
                        prefix = "[Bank] ";
                        color = Color.CYAN;
                    } else if (item.canPartiallyWithdrawFromBank()) {
                        prefix = "[Bank " + item.getBankQuantity() + "] ";
                        color = Color.YELLOW;
                    } else if (item.isTradeable()) {
                        prefix = "[GE] ";
                        color = Color.PINK;
                    } else {
                        prefix = "[Need] ";
                        color = Color.RED;
                    }

                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(prefix + item.getName())
                            .right("x" + item.getMissingForRun())
                            .rightColor(color)
                            .build());
                }
            }
        } else {
            if (currentPatch != null) {
                String typeTag = currentPatch.isDiseaseFree() ? "[Disease-Free]" : "[Standard]";
                Color typeColor = currentPatch.isDiseaseFree() ? Color.CYAN : Color.LIGHT_GRAY;

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Target Patch:")
                        .right(currentPatch.getPatchName())
                        .rightColor(Color.GREEN)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Type:")
                        .right(typeTag)
                        .rightColor(typeColor)
                        .build());
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Action:")
                    .right(plugin.getActionInstruction() != null ? plugin.getActionInstruction() : "")
                    .rightColor(Color.ORANGE)
                    .build());
        }

        return super.render(graphics);
    }
}
