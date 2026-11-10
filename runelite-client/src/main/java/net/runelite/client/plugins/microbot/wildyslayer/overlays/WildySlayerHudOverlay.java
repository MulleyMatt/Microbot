package net.runelite.client.plugins.microbot.wildyslayer.overlays;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerConfig;
import net.runelite.client.plugins.microbot.wildyslayer.WildySlayerPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * HUD status panel for Wildy Slayer Helper while inside the Wilderness.
 */
public class WildySlayerHudOverlay extends OverlayPanel
{
    private static final int MIN_COMBAT_LEVEL = 3;

    private final Client client;
    private final WildySlayerPlugin plugin;
    private final WildySlayerConfig config;

    @Inject
    public WildySlayerHudOverlay(
            Client client,
            WildySlayerPlugin plugin,
            WildySlayerConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showHudOverlay() || !plugin.isInWilderness())
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(225, 0));

        boolean hasPker = !plugin.getDetectedThreatPlayers().isEmpty();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(hasPker ? "!! PKER DETECTED !!" : "Wildy Slayer Helper")
                .color(hasPker ? Color.RED : new Color(0, 210, 211))
                .build());

        int wildyLevel = plugin.getCurrentWildernessLevel();
        int maxSeedPodLevel = config.maxSeedPodWildernessLevel();
        boolean inSeedPodRange = wildyLevel >= 1 && wildyLevel <= maxSeedPodLevel;

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Wilderness Lvl:")
                .right(wildyLevel > 0 ? ("Level " + wildyLevel) : "Wilderness")
                .rightColor(inSeedPodPodColor(wildyLevel, maxSeedPodLevel))
                .build());

        Player local = client.getLocalPlayer();
        if (local != null && wildyLevel > 0)
        {
            int combat = local.getCombatLevel();
            int minBracket = Math.max(MIN_COMBAT_LEVEL, combat - wildyLevel);
            int maxBracket = Math.min(Experience.MAX_COMBAT_LEVEL, combat + wildyLevel);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Attack Range:")
                    .right(minBracket + " - " + maxBracket)
                    .rightColor(Color.WHITE)
                    .build());
        }

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

        boolean hasSeedPod = plugin.hasRoyalSeedPodInInventory();
        boolean teleblocked = plugin.isTeleblocked();

        if (teleblocked)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Escape:")
                    .right("TELEBLOCKED!")
                    .rightColor(Color.RED)
                    .build());
        }
        else if (!hasSeedPod)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Seed Pod:")
                    .right("MISSING!")
                    .rightColor(Color.RED)
                    .build());
        }
        else if (wildyLevel > maxSeedPodLevel)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Seed Pod:")
                    .right("Above Lvl " + maxSeedPodLevel + "!")
                    .rightColor(Color.ORANGE)
                    .build());
        }
        else
        {
            String podStatus = config.autoSeedPodOnPker() ? "Ready (Auto ON)" : "Ready (Manual)";
            Color podColor = config.autoSeedPodOnPker() ? Color.GREEN : Color.YELLOW;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Seed Pod:")
                    .right(podStatus)
                    .rightColor(podColor)
                    .build());
        }

        return super.render(graphics);
    }

    private static Color inSeedPodPodColor(int wildyLevel, int maxSeedPodLevel)
    {
        if (wildyLevel <= 0)
        {
            return Color.LIGHT_GRAY;
        }
        return wildyLevel <= maxSeedPodLevel ? Color.GREEN : Color.ORANGE;
    }
}
