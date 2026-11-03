package net.runelite.client.plugins.microbot.redwood;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;

public class RedwoodOverlay extends OverlayPanel {

    private final RedwoodPlugin plugin;
    private final RedwoodConfig config;
    private final RedwoodSupplyCalculator supplyCalculator;

    @Inject
    public RedwoodOverlay(RedwoodPlugin plugin, RedwoodConfig config, RedwoodSupplyCalculator supplyCalculator) {
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
        panelComponent.setPreferredSize(new Dimension(240, 0));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Redwood Helper")
                .color(new Color(60, 200, 100))
                .build());

        RedwoodStep step = plugin.getCurrentStep();
        Color statusColor;
        switch (step) {
            case WOODCUTTING:
                statusColor = Color.GREEN;
                break;
            case IDLE_AFK:
                statusColor = new Color(255, 80, 80);
                break;
            case INVENTORY_FULL:
            case BANKING:
                statusColor = new Color(80, 180, 255);
                break;
            case CLIMBING_UP:
            case CLIMBING_DOWN:
                statusColor = Color.ORANGE;
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

        String axeName = supplyCalculator.getAxeName();
        boolean hasAxe = supplyCalculator.hasAxeEquippedOrInInventory();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Tool:")
                .right(axeName)
                .rightColor(hasAxe ? Color.GREEN : Color.RED)
                .build());

        if (supplyCalculator.hasSpecAxeEquipped()) {
            int specEnergy = plugin.getSpecialAttackEnergy() / 10;
            Color specColor = specEnergy >= 100 ? Color.YELLOW : Color.LIGHT_GRAY;
            String specText = specEnergy >= 100 ? "100% (Ready!)" : specEnergy + "%";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Special Attack:")
                    .right(specText)
                    .rightColor(specColor)
                    .build());
        }

        // Inactivity / Activity Timer
        if (plugin.isChopping()) {
            Duration choppingDuration = plugin.getChoppingDuration();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Chopping:")
                    .right(formatDuration(choppingDuration))
                    .rightColor(Color.GREEN)
                    .build());
        } else {
            Duration idleDuration = plugin.getIdleDuration();
            boolean isAfk = plugin.isAfk();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Inactive:")
                    .right(formatDuration(idleDuration))
                    .rightColor(isAfk ? Color.RED : Color.YELLOW)
                    .build());
        }

        if (config.showSessionTime()) {
            Duration sessionDuration = plugin.getSessionDuration();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Session Time:")
                    .right(formatDuration(sessionDuration))
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
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
}
