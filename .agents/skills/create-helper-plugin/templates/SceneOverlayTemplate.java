package net.runelite.client.plugins.microbot.createhelper.templates;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.graphics.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Reusable template for rendering 3D clickboxes, model outlines, and floating action prompts.
 */
public class SceneOverlayTemplate extends Overlay {
    private final Client client;
    private final ModelOutlineRenderer modelOutlineRenderer;

    public SceneOverlayTemplate(Client client, ModelOutlineRenderer modelOutlineRenderer) {
        this.client = client;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Example: Render Clickbox
        // Shape clickbox = object.getClickbox();
        // if (clickbox != null) {
        //     OverlayUtil.renderPolygon(graphics, clickbox, Color.CYAN);
        // }

        // Example: Render Model Outline
        // modelOutlineRenderer.drawOutline(npc, 2, Color.YELLOW, 4);

        // Example: Render Floating Action Text
        // LocalPoint lp = npc.getLocalLocation();
        // Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, ">> ATTACK <<", 120);
        // if (textLoc != null) {
        //     OverlayUtil.renderTextLocation(graphics, textLoc, ">> ATTACK <<", Color.GREEN);
        // }

        return null;
    }
}
