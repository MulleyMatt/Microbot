package net.runelite.client.plugins.microbot.herbrun;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;

@Slf4j
public class HerbRunSceneOverlay extends Overlay {

    private final Client client;
    private final HerbRunPlugin plugin;
    private final HerbRunConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    private static final Stroke CLICKBOX_STROKE = new BasicStroke(2.0f);
    private static final Stroke TILE_STROKE = new BasicStroke(1.5f);

    @Inject
    public HerbRunSceneOverlay(Client client, HerbRunPlugin plugin, HerbRunConfig config, ModelOutlineRenderer modelOutlineRenderer) {
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
        if (!config.highlightPatches()) {
            return null;
        }

        HerbPatch currentPatch = plugin.getCurrentPatch();
        if (currentPatch == null) {
            return null;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null || playerLoc.distanceTo(currentPatch.getLocation()) > 35) {
            return null;
        }

        HerbRunStep step = plugin.getCurrentStep();
        Color highlightColor = getStepColor(step);
        Color fillColor = new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 60);

        // Find patch using Queryable API
        Rs2TileObjectModel patchModel = Microbot.getRs2TileObjectCache().query()
                .where(o -> o != null && o.getWorldLocation() != null &&
                        (o.getWorldLocation().equals(currentPatch.getLocation()) || o.getId() == currentPatch.getObjectId()))
                .within(30)
                .nearestOnClientThread();

        TileObject patchObject = patchModel;

        if (patchObject != null) {
            // Draw model outline
            try {
                modelOutlineRenderer.drawOutline(patchModel.getTileObject() != null ? patchModel.getTileObject() : patchObject, 2, highlightColor, 4);
            } catch (Exception ignored) {
            }

            // Draw clickbox polygon
            if (config.drawPatchClickbox()) {
                Shape clickbox = patchObject.getClickbox();
                if (clickbox != null) {
                    OverlayUtil.renderPolygon(graphics, clickbox, highlightColor, fillColor, CLICKBOX_STROKE);
                }
            }

            // Draw text over the patch
            String actionText = getPatchActionTag(step, currentPatch);
            LocalPoint localPoint = patchObject.getLocalLocation();
            if (localPoint != null && actionText != null) {
                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, actionText, 100);
                if (textPoint != null) {
                    graphics.setFont(new Font("Arial", Font.BOLD, 13));
                    OverlayUtil.renderTextLocation(graphics, textPoint, actionText, highlightColor);
                }
            }
        } else {
            // Fallback: render tile polygon if object model is not yet loaded
            LocalPoint lp = LocalPoint.fromWorld(client, currentPatch.getLocation());
            if (lp != null) {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly != null) {
                    OverlayUtil.renderPolygon(graphics, poly, highlightColor, fillColor, TILE_STROKE);
                }
            }
        }

        // Highlight Tool Leprechaun if in NOTING_HERBS step
        if (config.highlightLeprechaun() && step == HerbRunStep.NOTING_HERBS) {
            Rs2NpcModel leprechaunModel = Microbot.getRs2NpcCache().query()
                    .withName("Tool Leprechaun")
                    .within(25)
                    .nearestOnClientThread();

            NPC leprechaun = leprechaunModel != null ? leprechaunModel.getNpc() : null;
            if (leprechaun != null && leprechaun.getLocalLocation() != null) {
                try {
                    modelOutlineRenderer.drawOutline(leprechaun, 2, Color.CYAN, 4);
                } catch (Exception ignored) {
                }

                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, leprechaun.getLocalLocation(), "NOTE HERBS", 120);
                if (textPoint != null) {
                    graphics.setFont(new Font("Arial", Font.BOLD, 13));
                    OverlayUtil.renderTextLocation(graphics, textPoint, ">> NOTE HERBS HERE <<", Color.CYAN);
                }
            }
        }

        return null;
    }

    private Color getStepColor(HerbRunStep step) {
        switch (step) {
            case HARVESTING:
                return new Color(50, 220, 50); // Vibrant Green
            case CLEARING:
                return new Color(230, 50, 50); // Bright Red
            case WEEDING:
                return new Color(240, 200, 30); // Yellow
            case COMPOSTING:
                return new Color(60, 200, 240); // Cyan
            case PLANTING:
                return new Color(80, 255, 120); // Bright Mint/Green
            case NOTING_HERBS:
                return new Color(180, 100, 240); // Purple
            default:
                return new Color(255, 170, 0); // Orange
        }
    }

    private String getPatchActionTag(HerbRunStep step, HerbPatch patch) {
        switch (step) {
            case HARVESTING:
                return ">> CLICK TO HARVEST <<";
            case CLEARING:
                return ">> CLEAR DEAD PATCH (USE SPADE) <<";
            case WEEDING:
                return ">> RAKE WEEDS <<";
            case COMPOSTING:
                return ">> USE COMPOST ON PATCH <<";
            case PLANTING:
                String seedName = (plugin.isDiseaseFreePatch(patch) || plugin.isStrategyPureRanarr()) ? "RANARR SEED" : "TOADFLAX SEED";
                return ">> PLANT " + seedName + " <<";
            case NOTING_HERBS:
                return "Herbs Harvested (Go to Leprechaun)";
            default:
                return patch.getPatchName() + " Herb Patch";
        }
    }
}
