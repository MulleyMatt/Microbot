package net.runelite.client.plugins.microbot.wgs.overlays;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.wgs.WgsBalanceElementalStyle;
import net.runelite.client.plugins.microbot.wgs.WgsBankInterface;
import net.runelite.client.plugins.microbot.wgs.WgsBossStage;
import net.runelite.client.plugins.microbot.wgs.WgsConfig;
import net.runelite.client.plugins.microbot.wgs.WgsGrandExchangeInterface;
import net.runelite.client.plugins.microbot.wgs.WgsOverlayUtils;
import net.runelite.client.plugins.microbot.wgs.WgsPlugin;
import net.runelite.client.plugins.microbot.wgs.WgsSurokMechanic;
import net.runelite.client.plugins.microbot.wgs.WgsSupplyCalculator;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Context-aware HUD status panel for While Guthix Sleeps Boss Helper.
 * Displays live fight guidance when fighting Surok Magis, Balance Elemental, or Tormented Demons,
 * and displays the selected boss supply checklist at the Bank / Grand Exchange.
 */
public class WgsHudOverlay extends OverlayPanel
{
    private final Client client;
    private final WgsPlugin plugin;
    private final WgsConfig config;
    private final WgsSupplyCalculator supplyCalculator;
    private final WgsBankInterface bankInterface;
    private final WgsGrandExchangeInterface geInterface;

    @Inject
    public WgsHudOverlay(
            Client client,
            WgsPlugin plugin,
            WgsConfig config,
            WgsSupplyCalculator supplyCalculator,
            WgsBankInterface bankInterface,
            WgsGrandExchangeInterface geInterface)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.supplyCalculator = supplyCalculator;
        this.bankInterface = bankInterface;
        this.geInterface = geInterface;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showHudOverlay())
        {
            return null;
        }

        WgsBossStage activeStage = plugin.getActiveStage();
        boolean showSupplyPrep = activeStage == null
                && (bankInterface.isBankOpen() || geInterface.isAtGrandExchange());

        if (activeStage == null && !showSupplyPrep)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(250, 0));

        if (activeStage == WgsBossStage.SUROK_MAGIS)
        {
            renderSurokHud();
        }
        else if (activeStage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            renderBalanceElementalHud();
        }
        else if (activeStage == WgsBossStage.TORMENTED_DEMONS)
        {
            renderTormentedDemonsHud();
        }
        else
        {
            renderSupplyPrepHud();
        }

        return super.render(graphics);
    }

    private void renderSurokHud()
    {
        WgsSurokMechanic mechanic = plugin.getActiveSurokMechanic();
        boolean urgent = mechanic != WgsSurokMechanic.NONE;

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("WGS: Surok Magis")
                .color(urgent ? mechanic.getColor() : new Color(0, 210, 255))
                .build());

        if (plugin.getSurokNpc() != null)
        {
            boolean protectMagic = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Overhead:")
                    .right(protectMagic ? "Protect Magic (ON)" : "PROTECT MAGIC OFF!")
                    .rightColor(protectMagic ? Color.GREEN : Color.RED)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Surok HP:")
                    .right("~" + plugin.getSurokEstimatedHp() + " / 450")
                    .rightColor(Color.WHITE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Next Special:")
                    .right(plugin.getSurokAttacksSinceSpecial() + " / 7-8 attacks")
                    .rightColor(plugin.getSurokAttacksSinceSpecial() >= 6 ? Color.YELLOW : Color.LIGHT_GRAY)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Action:")
                .right(mechanic.getActionPrompt())
                .rightColor(mechanic.getColor())
                .build());

        if (mechanic == WgsSurokMechanic.NONE)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Spellbook:")
                    .right("Standard Combat Only")
                    .rightColor(new Color(180, 230, 255))
                    .build());
        }
        else if (mechanic == WgsSurokMechanic.POST_FIGHT_TELEORB)
        {
            boolean hasOrb = plugin.hasTeleorbInInventory();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Teleorb:")
                    .right(hasOrb ? "Use on Surok body!" : "Check Inventory!")
                    .rightColor(hasOrb ? Color.MAGENTA : Color.RED)
                    .build());
        }

        if (!plugin.hasDarkSquallRobesEquippedOrInInventory() && plugin.getSurokUnconsciousNpc() == null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Robes:")
                    .right("Dark Squall missing!")
                    .rightColor(Color.ORANGE)
                    .build());
        }

        addAutoPrayerLine();
    }

    private void renderBalanceElementalHud()
    {
        WgsBalanceElementalStyle style = plugin.getBalanceStyle();
        WgsBalanceElementalStyle.HpPhase hpPhase = plugin.getBalanceHpPhase();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("WGS: Balance Elemental")
                .color(style != null ? style.getColor() : Color.CYAN)
                .build());

        if (style != null)
        {
            boolean prayOn = client.isPrayerActive(style.getProtectionPrayer());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Head Colour:")
                    .right(style.getDisplayName())
                    .rightColor(style.getColor())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Overhead:")
                    .right(prayOn ? style.getPrayerPrompt() + " (ON)" : style.getPrayerPrompt() + "!")
                    .rightColor(prayOn ? Color.GREEN : Color.RED)
                    .build());
        }

        int remaining = plugin.getBalanceRemainingInRotation();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Switch In:")
                .right(plugin.getBalanceAttacksInStyle() + " / 4 (" + remaining + " left)")
                .rightColor(remaining <= 1 ? Color.YELLOW : Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("HP Phase:")
                .right("~" + plugin.getBalanceEstimatedHp() + " (" + hpPhase.getLabel() + ")")
                .rightColor(hpPhase.getColor())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Phase Note:")
                .right(hpPhase.getMechanicNote())
                .rightColor(hpPhase.isCritical() ? Color.ORANGE : Color.LIGHT_GRAY)
                .build());

        String attackRec = WgsOverlayUtils.recommendPlayerAttackStyle(
                plugin.getBalanceProtectedStyles(),
                WgsBossStage.BALANCE_ELEMENTAL
        );
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Attack With:")
                .right(attackRec)
                .rightColor(Color.GREEN)
                .build());

        int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
        int maxPrayer = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
        int pct = (currentPrayer * 100) / maxPrayer;
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Prayer:")
                .right(currentPrayer + " / " + maxPrayer + " (" + pct + "%)")
                .rightColor(pct <= 50 && hpPhase.isCritical() ? Color.RED : Color.WHITE)
                .build());

        addAutoPrayerLine();
    }

    private void renderTormentedDemonsHud()
    {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("WGS: Tormented Demons")
                .color(new Color(255, 110, 60))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Stone of Jas:")
                .right(plugin.isStoneOfJasBoostActive() ? "ACTIVE (255 Boost)" : "Not Active")
                .rightColor(plugin.isStoneOfJasBoostActive() ? Color.GREEN : Color.YELLOW)
                .build());

        WgsPlugin.TormentedDemonState primary = plugin.getPrimaryTormentedDemonState();
        if (primary != null)
        {
            Prayer req = primary.getRequiredPrayer();
            String reqName = req != null ? prayerShortName(req) : "Wait for 1st attack";
            boolean active = req != null && client.isPrayerActive(req);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Overhead:")
                    .right(req == null ? reqName : (active ? reqName + " (ON)" : reqName + " OFF!"))
                    .rightColor(req == null ? Color.YELLOW : (active ? Color.GREEN : Color.RED))
                    .build());

            String atkRec = WgsOverlayUtils.recommendPlayerAttackStyle(
                    primary.getProtectedStyles(),
                    WgsBossStage.TORMENTED_DEMONS
            );
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Weakness:")
                    .right(atkRec)
                    .rightColor(new Color(140, 235, 255))
                    .build());

            if (primary.getShieldDownTicks() > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Fire Shield:")
                        .right("DOWN (" + primary.getShieldDownTicks() + "t) - HEAVY HIT!")
                        .rightColor(Color.GREEN)
                        .build());
            }
            else
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Fire Shield:")
                        .right("UP (Demonbane/Abyssal/Water)")
                        .rightColor(Color.LIGHT_GRAY)
                        .build());
            }
        }

        if (plugin.isPlayerInFireBombDanger())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hazard:")
                    .right("FIRE TILE - STEP OFF!")
                    .rightColor(Color.RED)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Safe Tip:")
                .right("SW Column = Guardian Safespot")
                .rightColor(Color.LIGHT_GRAY)
                .build());

        addAutoPrayerLine();
    }

    private void renderSupplyPrepHud()
    {
        WgsBossStage prepStage = config.prepBossStage();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("WGS Prep: " + prepStage.getShortName())
                .color(new Color(0, 210, 211))
                .build());

        List<WgsSupplyCalculator.SupplyStatus> statuses = supplyCalculator.evaluateSupplies();
        for (WgsSupplyCalculator.SupplyStatus status : statuses)
        {
            WgsSupplyCalculator.SupplyRequirement req = status.getRequirement();
            int carried = status.getCarriedQuantity();
            int needed = req.getSingleTripQuantity();
            boolean ready = status.isReadyInInventory();

            String rightText;
            Color rightColor;
            if (ready)
            {
                rightText = carried + "/" + needed + " Ready";
                rightColor = Color.GREEN;
            }
            else if (status.getMissingFromTotalOwned() == 0)
            {
                rightText = carried + "/" + needed + " (In Bank)";
                rightColor = Color.YELLOW;
            }
            else if (req.isTradeable())
            {
                rightText = carried + "/" + needed + " (Need " + status.getMissingFromTotalOwned() + " GE)";
                rightColor = Color.RED;
            }
            else
            {
                rightText = carried + "/" + needed + " (Quest Item)";
                rightColor = Color.ORANGE;
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(req.getDisplayName() + ":")
                    .right(rightText)
                    .rightColor(rightColor)
                    .build());
        }

        if (bankInterface.isBankOpen())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Bank Filter:")
                    .right(bankInterface.isActive() ? "ON (Missing)" : "Click Top-Right Button")
                    .rightColor(bankInterface.isActive() ? Color.GREEN : Color.LIGHT_GRAY)
                    .build());
        }
        else if (geInterface.isAtGrandExchange())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("GE Filter:")
                    .right(geInterface.isActive() ? "ON" : "Click Search Bar Icon")
                    .rightColor(geInterface.isActive() ? Color.GREEN : Color.LIGHT_GRAY)
                    .build());
        }
    }

    private void addAutoPrayerLine()
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Auto-Prayer:")
                .right(config.autoSwitchPrayers() ? "ON" : "OFF")
                .rightColor(config.autoSwitchPrayers() ? Color.GREEN : Color.GRAY)
                .build());
    }

    private static String prayerShortName(Prayer prayer)
    {
        if (prayer == null)
        {
            return "None";
        }
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                return "Protect Magic";
            case PROTECT_FROM_MISSILES:
                return "Protect Missiles";
            case PROTECT_FROM_MELEE:
                return "Protect Melee";
            default:
                return prayer.name();
        }
    }
}
