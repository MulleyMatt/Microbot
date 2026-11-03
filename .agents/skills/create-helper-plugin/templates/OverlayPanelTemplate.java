package net.runelite.client.plugins.microbot.createhelper.templates;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Reusable template for the HUD overlay panel showing phase, missing supplies, and next action.
 */
public class OverlayPanelTemplate extends OverlayPanel {

    public OverlayPanelTemplate() {
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Helper HUD")
                .color(Color.CYAN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current Phase:")
                .right("Active")
                .rightColor(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next Action:")
                .right("Pray Mage & Attack")
                .rightColor(Color.YELLOW)
                .build());

        return super.render(graphics);
    }
}
