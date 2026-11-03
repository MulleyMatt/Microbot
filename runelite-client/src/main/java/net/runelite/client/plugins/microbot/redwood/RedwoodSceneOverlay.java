package net.runelite.client.plugins.microbot.redwood;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
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
import java.awt.Shape;
import java.awt.Stroke;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RedwoodSceneOverlay extends Overlay {

    private final Client client;
    private final RedwoodPlugin plugin;
    private final RedwoodConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    private static final Stroke CLICKBOX_STROKE = new BasicStroke(2.0f);
    private static final Stroke DEPLETED_STROKE = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);

    public static final int[] READY_REDWOOD_IDS = {
            ObjectID.REDWOODTREE_L,
            ObjectID.REDWOODTREE_R,
            ObjectID.REDWOOD_TREE_FULLYGROWN_1_2,
            ObjectID.REDWOOD_TREE_FULLYGROWN_1_4,
            ObjectID.REDWOOD_TREE_FULLYGROWN_1_6,
            ObjectID.REDWOOD_TREE_FULLYGROWN_1_8
    };

    public static final int[] DEPLETED_REDWOOD_IDS = {
            ObjectID.REDWOODTREE_L_CUT,
            ObjectID.REDWOODTREE_R_CUT,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_1_2,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_1_4,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_1_6,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_1_8
    };

    @Inject
    public RedwoodSceneOverlay(Client client, RedwoodPlugin plugin, RedwoodConfig config, ModelOutlineRenderer modelOutlineRenderer) {
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
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return null;
        }

        int playerPlane = client.getPlane();
        boolean invFull = Rs2Inventory.isFull();

        // 1. Render Redwood Tree Spots
        if (config.highlightTreeSpots() && playerPlane == 1) {
            renderRedwoodSpots(graphics, playerLoc);
        }

        // 2. Render Ladders & Bank Chest Guidance
        if (config.highlightLaddersAndBank()) {
            renderTransitAndBanking(graphics, playerPlane, invFull);
        }

        return null;
    }

    private void renderRedwoodSpots(Graphics2D graphics, WorldPoint playerLoc) {
        // Query ready redwood tree spots
        List<Rs2TileObjectModel> readySpots = Microbot.getRs2TileObjectCache().query()
                .where(o -> isReadyRedwood(o.getId()))
                .within(25)
                .toList();

        TileObject activeSpot = plugin.getActiveTreeSpot();

        for (Rs2TileObjectModel spot : readySpots) {
            boolean isActiveChopSpot = activeSpot != null && spot.getWorldLocation().equals(activeSpot.getWorldLocation());
            Color outlineColor = isActiveChopSpot ? config.activeTreeHighlightColor() : config.treeHighlightColor();
            Color fillColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 40);
            String label = isActiveChopSpot ? "CHOPPING" : "CHOP";

            renderObjectHighlight(graphics, spot, outlineColor, fillColor, CLICKBOX_STROKE, label);
        }

        // Query depleted redwood tree spots if configured
        if (config.highlightDepletedTrees()) {
            List<Rs2TileObjectModel> depletedSpots = Microbot.getRs2TileObjectCache().query()
                    .where(o -> isDepletedRedwood(o.getId()))
                    .within(25)
                    .toList();

            Color depletedColor = config.depletedTreeColor();
            Color depletedFill = new Color(depletedColor.getRed(), depletedColor.getGreen(), depletedColor.getBlue(), 25);

            for (Rs2TileObjectModel spot : depletedSpots) {
                renderObjectHighlight(graphics, spot, depletedColor, depletedFill, DEPLETED_STROKE, "DEPLETED");
            }
        }
    }

    private void renderTransitAndBanking(Graphics2D graphics, int playerPlane, boolean invFull) {
        if (invFull) {
            if (playerPlane == 1) {
                // Upstairs with full inventory -> highlight top rope ladder down
                Rs2TileObjectModel ropeTop = Microbot.getRs2TileObjectCache().query()
                        .where(o -> o.getId() == ObjectID.WCGUILD_REDWOOD_ROPETOP)
                        .within(25)
                        .nearestOnClientThread();

                if (ropeTop != null) {
                    Color color = new Color(255, 140, 0);
                    Color fill = new Color(255, 140, 0, 50);
                    renderObjectHighlight(graphics, ropeTop, color, fill, CLICKBOX_STROKE, "CLIMB DOWN");
                }
            } else if (playerPlane == 0) {
                // On ground floor with full inventory -> highlight bank chest
                Rs2TileObjectModel bankChest = Microbot.getRs2TileObjectCache().query()
                        .where(o -> o.getId() == ObjectID.WCGUILD_BANKCHEST)
                        .within(30)
                        .nearestOnClientThread();

                if (bankChest != null) {
                    Color color = new Color(0, 180, 255);
                    Color fill = new Color(0, 180, 255, 60);
                    renderObjectHighlight(graphics, bankChest, color, fill, CLICKBOX_STROKE, "BANK LOGS");
                }
            }
        } else {
            // Not full, on ground floor -> highlight bottom rope ladder up
            if (playerPlane == 0) {
                Rs2TileObjectModel ropeBottom = Microbot.getRs2TileObjectCache().query()
                        .where(o -> o.getId() == ObjectID.WCGUILD_REDWOOD_ROPEBOTTOM)
                        .within(25)
                        .nearestOnClientThread();

                if (ropeBottom != null) {
                    Color color = new Color(0, 255, 128);
                    Color fill = new Color(0, 255, 128, 50);
                    renderObjectHighlight(graphics, ropeBottom, color, fill, CLICKBOX_STROKE, "CLIMB UP");
                }
            }
        }
    }

    private void renderObjectHighlight(Graphics2D graphics, TileObject object, Color outlineColor, Color fillColor, Stroke stroke, String label) {
        if (object == null) {
            return;
        }

        // Draw model outline
        if (config.drawModelOutline()) {
            try {
                modelOutlineRenderer.drawOutline(object, 2, outlineColor, 4);
            } catch (Exception ignored) {
            }
        }

        // Draw 3D clickbox
        if (config.drawClickbox()) {
            Shape clickbox = object.getClickbox();
            if (clickbox != null) {
                OverlayUtil.renderPolygon(graphics, clickbox, outlineColor, fillColor, stroke);
            }
        }

        // Draw floating action text
        if (config.drawActionText() && label != null && !label.isEmpty()) {
            LocalPoint localPoint = object.getLocalLocation();
            if (localPoint != null) {
                Point textLoc = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 120);
                if (textLoc != null) {
                    OverlayUtil.renderTextLocation(graphics, textLoc, label, outlineColor);
                }
            }
        }
    }

    public static boolean isReadyRedwood(int id) {
        for (int readyId : READY_REDWOOD_IDS) {
            if (readyId == id) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDepletedRedwood(int id) {
        for (int depId : DEPLETED_REDWOOD_IDS) {
            if (depId == id) {
                return true;
            }
        }
        return false;
    }
}
