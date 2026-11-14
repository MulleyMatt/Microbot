package net.runelite.client.plugins.microbot.barbarianfishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

@Slf4j
public class BarbarianFishingSceneOverlay extends Overlay {

    public static final int[] BARBARIAN_SPOT_IDS = {
            NpcID._0_39_54_BRUT_FISHING_SPOT,
            NpcID._0_19_55_BRUT_FISHING_SPOT
    };

    private static final Stroke SPOT_STROKE = new BasicStroke(2.0f);

    private final Client client;
    private final BarbarianFishingPlugin plugin;
    private final BarbarianFishingConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public BarbarianFishingSceneOverlay(Client client, BarbarianFishingPlugin plugin,
                                        BarbarianFishingConfig config, ModelOutlineRenderer modelOutlineRenderer) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.highlightFishingSpots()) {
            return null;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        List<Rs2NpcModel> spots = Microbot.getRs2NpcCache().query()
                .where(n -> isBarbarianFishingSpot(n.getId()))
                .within(25)
                .toList();

        NPC activeSpot = plugin.getActiveFishingSpot();

        for (Rs2NpcModel spotModel : spots) {
            NPC npc = spotModel.getNpc();
            if (npc == null) {
                continue;
            }

            boolean isActiveSpot = activeSpot != null && activeSpot.getIndex() == npc.getIndex();
            Color outlineColor = isActiveSpot ? config.activeSpotHighlightColor() : config.spotHighlightColor();
            Color fillColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 40);
            String label = isActiveSpot ? "FISHING" : "FISH";

            renderSpotHighlight(graphics, npc, outlineColor, fillColor, label);
        }

        return null;
    }

    private void renderSpotHighlight(Graphics2D graphics, NPC npc, Color outlineColor, Color fillColor, String label) {
        if (config.drawModelOutline()) {
            try {
                modelOutlineRenderer.drawOutline(npc, 2, outlineColor, 4);
            } catch (Exception ignored) {
            }
        }

        if (config.drawClickbox()) {
            java.awt.Polygon tilePoly = npc.getCanvasTilePoly();
            if (tilePoly != null) {
                OverlayUtil.renderPolygon(graphics, tilePoly, outlineColor, fillColor, SPOT_STROKE);
            }
        }

        if (config.drawActionText() && label != null && !label.isEmpty()) {
            Point textLoc = npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + 40);
            if (textLoc != null) {
                OverlayUtil.renderTextLocation(graphics, textLoc, label, outlineColor);
            }
        }
    }

    public static boolean isBarbarianFishingSpot(int npcId) {
        for (int id : BARBARIAN_SPOT_IDS) {
            if (id == npcId) {
                return true;
            }
        }
        return false;
    }
}
