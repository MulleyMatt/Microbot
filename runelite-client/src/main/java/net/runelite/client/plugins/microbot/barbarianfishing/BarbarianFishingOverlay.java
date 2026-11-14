package net.runelite.client.plugins.microbot.barbarianfishing;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.time.Duration;

public class BarbarianFishingOverlay extends OverlayPanel {

    private final BarbarianFishingPlugin plugin;
    private final BarbarianFishingConfig config;
    private final BarbarianFishingSupplyCalculator supplyCalculator;

    @Inject
    public BarbarianFishingOverlay(BarbarianFishingPlugin plugin, BarbarianFishingConfig config,
                                   BarbarianFishingSupplyCalculator supplyCalculator) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        this.supplyCalculator = supplyCalculator;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlayPanel()) {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(245, 0));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Barbarian Fishing Helper")
                .color(new Color(60, 200, 180))
                .build());

        BarbarianFishingStep step = plugin.getCurrentStep();
        Color statusColor;
        switch (step) {
            case FISHING:
                statusColor = Color.GREEN;
                break;
            case IDLE_AFK:
                statusColor = new Color(255, 80, 80);
                break;
            case INVENTORY_FULL:
                statusColor = new Color(80, 180, 255);
                break;
            default:
                statusColor = Color.YELLOW;
                break;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right(step.getLabel())
                .rightColor(statusColor)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Action:")
                .right(plugin.getActionInstruction())
                .rightColor(Color.WHITE)
                .build());

        boolean hasRod = supplyCalculator.hasBarbarianRodEquippedOrInInventory();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Rod:")
                .right(supplyCalculator.getRodName())
                .rightColor(hasRod ? Color.GREEN : Color.RED)
                .build());

        int baitCount = supplyCalculator.getTotalBaitCount();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Bait / Feathers:")
                .right(NumberFormat.getIntegerInstance().format(baitCount))
                .rightColor(baitCount > 0 ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Left-Click Drop:")
                .right(config.leftClickDropFish() ? "Active" : "Off")
                .rightColor(config.leftClickDropFish() ? Color.GREEN : Color.GRAY)
                .build());

        if (supplyCalculator.hasSpecHarpoonEquipped()) {
            int specEnergy = plugin.getSpecialAttackEnergy() / 10;
            Color specColor = specEnergy >= 100 ? Color.YELLOW : Color.LIGHT_GRAY;
            String specText = specEnergy >= 100 ? "100% (Ready!)" : specEnergy + "%";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Special Attack:")
                    .right(specText)
                    .rightColor(specColor)
                    .build());
        }

        if (plugin.isFishing()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Fishing:")
                    .right(formatDuration(plugin.getFishingDuration()))
                    .rightColor(Color.GREEN)
                    .build());
        } else {
            boolean isAfk = plugin.isAfk();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Inactive:")
                    .right(formatDuration(plugin.getIdleDuration()))
                    .rightColor(isAfk ? Color.RED : Color.YELLOW)
                    .build());
        }

        if (config.showXpAndRates()) {
            NumberFormat nf = NumberFormat.getIntegerInstance();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Fish Caught:")
                    .right(nf.format(plugin.getFishCaught()) + " (" + nf.format(plugin.getFishPerHour()) + "/hr)")
                    .rightColor(Color.WHITE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Fishing XP:")
                    .right(nf.format(plugin.getXpGained()) + " (" + nf.format(plugin.getXpPerHour()) + "/hr)")
                    .rightColor(Color.WHITE)
                    .build());
        }

        if (config.showSessionTime()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Session Time:")
                    .right(formatDuration(plugin.getSessionDuration()))
                    .rightColor(Color.GRAY)
                    .build());
        }

        return super.render(graphics);
    }

    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "00:00";
        }
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        long hours = absSeconds / 3600;
        long minutes = (absSeconds % 3600) / 60;
        long secs = absSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }
}
