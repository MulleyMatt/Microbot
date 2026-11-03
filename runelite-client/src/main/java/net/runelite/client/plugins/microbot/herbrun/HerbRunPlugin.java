package net.runelite.client.plugins.microbot.herbrun;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.CropState;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingPatch;
import net.runelite.client.plugins.microbot.util.farming.Rs2Farming;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.poh.PohPresence;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Herb Run Helper",
        description = "Herb Run Helper with dual strategies (Pure Ranarr & Pro Split), GE buyer helper, and optimal route guidance",
        tags = {"farming", "herb", "run", "helper", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class HerbRunPlugin extends Plugin {

    private static final int SEARCHBOX_LOADED = 750;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HerbRunConfig config;

    @Inject
    private HerbRunOverlay hudOverlay;

    @Inject
    private HerbRunSceneOverlay sceneOverlay;

    @Inject
    private HerbRunInventoryOverlay inventoryOverlay;

    @Inject
    private HerbRunSupplyCalculator supplyCalculator;

    @Inject
    private HerbRunGrandExchangeInterface geInterface;

    @Inject
    private HerbRunBankInterface bankInterface;

    @Getter
    private HerbRunStep currentStep = HerbRunStep.CHECKING_SUPPLIES;

    @Getter
    private HerbPatch currentPatch = null;

    @Getter
    private final Set<HerbPatch> completedPatches = new HashSet<>();

    private final Set<HerbPatch> compostedPatches = new HashSet<>();
    private final Set<HerbPatch> plantedPatches = new HashSet<>();

    @Getter
    private String actionInstruction = "Checking inventory & supplies...";

    @Provides
    HerbRunConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbRunConfig.class);
    }

    @Override
    protected void startUp() {
        log.info("Herb Run Helper plugin started");
        overlayManager.add(hudOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(inventoryOverlay);

        resetRunState();

        clientThread.invokeLater(() -> {
            HerbPatch.updateAllUnlocks(client);
            if (geInterface.isAtGrandExchange()) {
                geInterface.init();
            }
            if (bankInterface.isBankOpen()) {
                bankInterface.init();
            }
        });
    }

    @Override
    protected void shutDown() {
        log.info("Herb Run Helper plugin stopped");
        overlayManager.remove(hudOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(inventoryOverlay);

        clientThread.invokeLater(() -> {
            geInterface.destroy();
            bankInterface.destroy();
        });
        resetRunState();
    }

    public void resetRunState() {
        completedPatches.clear();
        compostedPatches.clear();
        plantedPatches.clear();
        currentPatch = null;
        currentStep = HerbRunStep.CHECKING_SUPPLIES;
        actionInstruction = "Checking supplies...";
    }

    public boolean isStrategyPureRanarr() {
        return config.strategy() == HerbRunStrategy.PURE_RANARR;
    }

    public boolean isDiseaseFreePatch(HerbPatch patch) {
        return patch != null && patch.isDiseaseFree();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(HerbRunConfig.GROUP)) {
            return;
        }

        if (event.getKey().equals("strategy") || event.getKey().startsWith("troll") || event.getKey().startsWith("weiss")) {
            HerbPatch.updateAllUnlocks(client);
            checkNextPatchTarget();
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (config.highlightMissingInGe() && event.getScriptId() == SEARCHBOX_LOADED && geInterface.isAtGrandExchange()) {
            geInterface.init();
        }
        bankInterface.onScriptPostFired(event);
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event) {
        if (!config.highlightMissingInGe()) {
            return;
        }

        geInterface.onGrandExchangeSearched(event);
    }

    @Subscribe(priority = -1)
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        bankInterface.onScriptCallbackEvent(event);
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        bankInterface.onScriptPreFired(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        bankInterface.onWidgetLoaded(event);
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        bankInterface.onWidgetClosed(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        bankInterface.onItemContainerChanged(event);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        bankInterface.handleClick(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String msg = event.getMessage().toLowerCase();

        if (msg.contains("you treat the herb patch with") || msg.contains("soil has already been treated")
                || msg.contains("the dirt has already been treated") || msg.contains("already treated")) {
            if (currentPatch != null) {
                compostedPatches.add(currentPatch);
                log.info("Patch marked as composted: {}", currentPatch.getPatchName());
            }
        } else if (msg.contains("you plant a") || msg.contains("you plant an")) {
            if (currentPatch != null) {
                plantedPatches.add(currentPatch);
                compostedPatches.add(currentPatch);
                log.info("Seed planted at: {}", currentPatch.getPatchName());
            }
        } else if (msg.contains("the herb patch is now empty")) {
            if (currentPatch != null) {
                log.info("Herb patch emptied at: {}", currentPatch.getPatchName());
            }
        } else if (msg.contains("the leprechaun exchanges") || msg.contains("noted herbs")) {
            log.info("Herbs noted on Tool Leprechaun.");
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!Microbot.isLoggedIn()) {
            return;
        }

        if (client.getTickCount() % 10 == 0) {
            HerbPatch.updateAllUnlocks(client);
        }

        // GE button widgets are (re)created on SEARCHBOX_LOADED (see onScriptPostFired).
        // Re-initialising here every tick would leak new child widgets into the chatbox layer.
        if (!geInterface.isAtGrandExchange() && geInterface.isActive()) {
            geInterface.destroy();
        }

        List<HerbPatch> enabledPatches = supplyCalculator.getEnabledPatches();
        if (enabledPatches.isEmpty()) {
            currentStep = HerbRunStep.FINISHED;
            actionInstruction = "No patches enabled in config!";
            return;
        }

        // Check if player has required supplies for at least 1 run
        if (!supplyCalculator.hasAllSuppliesForRun()) {
            currentStep = HerbRunStep.MISSING_SUPPLIES;
            if (geInterface.isAtGrandExchange()) {
                actionInstruction = "At GE: Click 'Herb Run Supplies' button in search to buy";
            } else {
                actionInstruction = "Missing supplies! Check HUD panel.";
            }
            return;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return;
        }

        // Check if player is near any enabled patch (within 30 tiles)
        HerbPatch nearbyPatch = findNearbyPatch(playerLoc, enabledPatches, 30);

        if (nearbyPatch != null) {
            currentPatch = nearbyPatch;
            updatePatchActions(nearbyPatch);
        } else {
            // Player is between patches or travelling
            checkNextPatchTarget();
        }
    }

    private HerbPatch findNearbyPatch(WorldPoint playerLoc, List<HerbPatch> patches, int maxDistance) {
        for (HerbPatch patch : patches) {
            if (!completedPatches.contains(patch) && playerLoc.distanceTo(patch.getLocation()) <= maxDistance) {
                return patch;
            }
        }
        return null;
    }

    public static boolean isUnnotedHerb(Rs2ItemModel item) {
        if (item == null || item.isNoted() || item.getName() == null) {
            return false;
        }
        String name = item.getName().toLowerCase();
        return name.startsWith("grimy ") ||
                name.equals("ranarr weed") ||
                name.equals("toadflax") ||
                name.equals("irit leaf") ||
                name.equals("avantoe") ||
                name.equals("kwuarm") ||
                name.equals("snapdragon") ||
                name.equals("cadantine") ||
                name.equals("lantadyme") ||
                name.equals("dwarf weed") ||
                name.equals("torstol") ||
                name.equals("guam leaf") ||
                name.equals("marrentill") ||
                name.equals("tarromin") ||
                name.equals("harralander");
    }

    public boolean hasUnnotedHerbs() {
        return Rs2Inventory.items().anyMatch(HerbRunPlugin::isUnnotedHerb);
    }

    private void updatePatchActions(HerbPatch patch) {
        Optional<FarmingPatch> fpOpt = Rs2Farming.getHerbPatchAt(patch.getLocation());
        if (!fpOpt.isPresent()) {
            fpOpt = Rs2Farming.getPatchByRegionAndVarbit(patch.getFarmingRegionName(), patch.getVarbit());
        }
        CropState cropState = fpOpt.map(Rs2Farming::predictPatchState).orElse(null);

        // 1. Dead patch
        if (cropState == CropState.DEAD) {
            currentStep = HerbRunStep.CLEARING;
            actionInstruction = "Clear dead patch with Spade";
            return;
        }

        // 2. Harvestable
        if (cropState == CropState.HARVESTABLE) {
            if (Rs2Inventory.isFull() && hasUnnotedHerbs()) {
                currentStep = HerbRunStep.NOTING_HERBS;
                actionInstruction = "Inventory full: Note herbs on Tool Leprechaun";
                return;
            }
            currentStep = HerbRunStep.HARVESTING;
            actionInstruction = "Harvest herbs from " + patch.getPatchName();
            return;
        }

        // 3. Empty -> Composting & Planting
        if (cropState == CropState.EMPTY) {
            if (!compostedPatches.contains(patch)) {
                currentStep = HerbRunStep.COMPOSTING;
                actionInstruction = "Use Ultracompost on patch";
                return;
            } else {
                currentStep = HerbRunStep.PLANTING;
                String seedName = (isStrategyPureRanarr() || patch.isDiseaseFree()) ? "Ranarr seed" : "Toadflax seed";
                actionInstruction = "Plant " + seedName;
                return;
            }
        }

        // 4. Growing -> check if herbs need to be noted on Tool Leprechaun
        if (cropState == CropState.GROWING || plantedPatches.contains(patch)) {
            plantedPatches.add(patch);
            if (hasUnnotedHerbs()) {
                currentStep = HerbRunStep.NOTING_HERBS;
                actionInstruction = "Use herbs on Tool Leprechaun";
                return;
            }

            // Patch successfully planted & herbs noted -> Mark completed and move forward
            completedPatches.add(patch);
            checkNextPatchTarget();
            return;
        }

        // Fallback default: Harvest/inspect patch
        currentStep = HerbRunStep.HARVESTING;
        actionInstruction = "Harvest or inspect " + patch.getPatchName();
    }

    public String getTeleportItemName(HerbPatch patch) {
        if (patch == null) {
            return "";
        }
        for (int altId : patch.getAlternateTeleportItemIds()) {
            if (Rs2Equipment.isWearing(altId) || Rs2Inventory.hasItem(altId)) {
                if (altId == ItemID.POH_TABLET_TELEPORTTOHOUSE) return "Teleport to house";
                if (altId == ItemID.SKILLCAPE_CONSTRUCTION || altId == ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED) return "Construction cape";
                if (altId == ItemID.SKILLCAPE_MAX || altId == ItemID.SKILLCAPE_MAX_WORN) return "Max cape";
                if (altId == ItemID.STRONGHOLD_TELEPORT_BASALT) return "Stony basalt";
                if (altId == ItemID.NZONE_TELETAB_TROLLHEIM || altId == 11747) return "Trollheim teleport";
                if (altId == ItemID.WEISS_TELEPORT_BASALT) return "Icy basalt";
                if (altId == ItemID.LUNAR_TABLET_CATHERBY_TELEPORT) return "Catherby tablet";
                if (altId == ItemID.POH_TABLET_CAMELOTTELEPORT || altId == 8010) return "Camelot teleport";
                if (altId == ItemID.ECTOPHIAL) return "Ectophial";
                if (altId == ItemID.LUMBRIDGE_RING_MEDIUM || altId == ItemID.LUMBRIDGE_RING_HARD || altId == ItemID.LUMBRIDGE_RING_ELITE) return "Explorer's ring";
                if (altId == ItemID.ARDY_CAPE_MEDIUM || altId == ItemID.ARDY_CAPE_HARD || altId == ItemID.ARDY_CAPE_ELITE) return "Ardougne cloak";
                if (altId == ItemID.SKILLCAPE_FARMING || altId == ItemID.SKILLCAPE_FARMING_TRIMMED) return "Farming cape";
                if (altId >= ItemID.JEWL_NECKLACE_OF_SKILLS_1 && altId <= ItemID.JEWL_NECKLACE_OF_SKILLS_6) return "Skills necklace";
            }
        }
        return patch.getPrimaryTeleportName();
    }

    private void setTeleportInstruction(HerbPatch patch) {
        if (patch.matchesTeleportId(ItemID.POH_TABLET_TELEPORTTOHOUSE) && PohPresence.isInHouse()) {
            actionInstruction = "In POH: Take portal/nexus to " + patch.getPatchName();
        } else {
            String teleName = getTeleportItemName(patch);
            actionInstruction = "Teleport to " + patch.getPatchName() + " (" + teleName + ")";
        }
    }

    private void checkNextPatchTarget() {
        List<HerbPatch> enabledPatches = supplyCalculator.getEnabledPatches();

        // 1. Target the first incomplete patch for which the player has a teleport
        for (HerbPatch patch : enabledPatches) {
            if (!completedPatches.contains(patch) && !plantedPatches.contains(patch) && supplyCalculator.hasTeleportForPatch(patch)) {
                currentPatch = patch;
                currentStep = HerbRunStep.TELEPORTING;
                setTeleportInstruction(patch);
                return;
            }
        }

        // 2. Next, check any remaining incomplete patches
        for (HerbPatch patch : enabledPatches) {
            if (!completedPatches.contains(patch) && !plantedPatches.contains(patch)) {
                currentPatch = patch;
                currentStep = HerbRunStep.TELEPORTING;
                setTeleportInstruction(patch);
                return;
            }
        }

        // All patches completed
        currentPatch = null;
        currentStep = HerbRunStep.FINISHED;
        actionInstruction = "All patches planted! Next run in ~80m.";
    }
}
