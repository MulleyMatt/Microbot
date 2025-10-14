package net.runelite.client.plugins.microbot.MullyScripts.EdgevilleCrafter;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;

public class MullyJewelryOverlay extends OverlayPanel {

    @Inject
    MullyJewelryOverlay(MullyJewelryPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Mully's Jewelry")
                    .color(ColorUtil.fromHex("0077B6"))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            if (MullyJewelryScript.state != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("State:")
                        .right(MullyJewelryScript.state.name())
                        .build());
            }

        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }

        return super.render(graphics);
    }
}
