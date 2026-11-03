package net.runelite.client.plugins.microbot.redwood;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Toolkit;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Redwood Woodcutting Helper",
        description = "Interactive Redwood woodcutting helper with tree spot marking, AFK red screen flash, and inventory full blue screen flash",
        tags = {"woodcutting", "redwood", "helper", "afk", "flash", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class RedwoodPlugin extends Plugin {

    private static final int SEARCHBOX_LOADED = 750;

    public static final Set<Integer> WOODCUTTING_ANIMS = ImmutableSet.of(
            AnimationID.WOODCUTTING_BRONZE,
            AnimationID.WOODCUTTING_IRON,
            AnimationID.WOODCUTTING_STEEL,
            AnimationID.WOODCUTTING_BLACK,
            AnimationID.WOODCUTTING_MITHRIL,
            AnimationID.WOODCUTTING_ADAMANT,
            AnimationID.WOODCUTTING_RUNE,
            AnimationID.WOODCUTTING_GILDED,
            AnimationID.WOODCUTTING_DRAGON,
            AnimationID.WOODCUTTING_DRAGON_OR,
            AnimationID.WOODCUTTING_INFERNAL,
            AnimationID.WOODCUTTING_3A_AXE,
            AnimationID.WOODCUTTING_CRYSTAL,
            AnimationID.WOODCUTTING_TRAILBLAZER,
            AnimationID.WOODCUTTING_2H_BRONZE,
            AnimationID.WOODCUTTING_2H_IRON,
            AnimationID.WOODCUTTING_2H_STEEL,
            AnimationID.WOODCUTTING_2H_BLACK,
            AnimationID.WOODCUTTING_2H_MITHRIL,
            AnimationID.WOODCUTTING_2H_ADAMANT,
            AnimationID.WOODCUTTING_2H_RUNE,
            AnimationID.WOODCUTTING_2H_DRAGON,
            AnimationID.WOODCUTTING_2H_CRYSTAL,
            AnimationID.WOODCUTTING_2H_CRYSTAL_INACTIVE,
            AnimationID.WOODCUTTING_2H_3A
    );

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private RedwoodConfig config;

    @Inject
    private RedwoodOverlay hudOverlay;

    @Inject
    private RedwoodSceneOverlay sceneOverlay;

    @Inject
    private RedwoodFlashOverlay flashOverlay;

    @Inject
    private RedwoodInventoryOverlay inventoryOverlay;

    @Inject
    private RedwoodSupplyCalculator supplyCalculator;

    @Inject
    private RedwoodGrandExchangeInterface geInterface;

    @Getter
    private RedwoodStep currentStep = RedwoodStep.CHECKING_SUPPLIES;

    @Getter
    private String actionInstruction = "Checking gear...";

    @Getter
    private boolean isChopping = false;

    @Getter
    private Instant choppingStartTime = null;

    @Getter
    private Instant stoppedChoppingTime = null;

    @Getter
    private TileObject activeTreeSpot = null;

    private Instant lastUserActionTime = Instant.now();
    private Instant afkFlashStartTime = null;
    private boolean afkFlashSuppressed = false;

    // Special Attack tracking
    private int lastSpecEnergy = 0;
    private Instant specFlashStartTime = null;
    private boolean specFlashDismissed = false;

    // Inventory full & banking movement tracking
    public static final int BANKING_GRACE_PERIOD_SECONDS = 10;
    @Getter
    private boolean hasStartedMovingToBank = false;
    private Instant lastBankMovementActionTime = null;

    // Flash Sound Alert Tracking
    public enum FlashType {
        NONE,
        INVENTORY_FULL,
        AFK,
        SPECIAL_ATTACK
    }

    private final Object soundLock = new Object();
    private FlashType currentFlashType = FlashType.NONE;
    private Instant lastSoundTime = null;

    @Getter
    private int soundPlayCount = 0;

    // Statistics
    @Getter
    private Instant sessionStartTime = null;

    private int startWoodcuttingXp = -1;
    private int currentWoodcuttingXp = -1;

    @Getter
    private int logsChopped = 0;

    @Getter
    private int birdNests = 0;

    @Provides
    RedwoodConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RedwoodConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(hudOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(flashOverlay);
        overlayManager.add(inventoryOverlay);

        sessionStartTime = Instant.now();
        lastUserActionTime = Instant.now();
        logsChopped = 0;
        birdNests = 0;
        isChopping = false;
        choppingStartTime = null;
        stoppedChoppingTime = null;
        activeTreeSpot = null;
        afkFlashStartTime = null;
        afkFlashSuppressed = false;
        specFlashStartTime = null;
        specFlashDismissed = false;
        currentFlashType = FlashType.NONE;
        lastSoundTime = null;
        soundPlayCount = 0;
        hasStartedMovingToBank = false;
        lastBankMovementActionTime = null;

        if (client.getGameState() == GameState.LOGGED_IN) {
            startWoodcuttingXp = client.getSkillExperience(Skill.WOODCUTTING);
            currentWoodcuttingXp = startWoodcuttingXp;
            lastSpecEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY);
        } else {
            startWoodcuttingXp = -1;
            currentWoodcuttingXp = -1;
            lastSpecEnergy = 0;
        }

        log.info("Redwood Helper plugin started!");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(hudOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(flashOverlay);
        overlayManager.remove(inventoryOverlay);

        if (geInterface != null && geInterface.isActive()) {
            geInterface.deactivateTab();
        }

        currentFlashType = FlashType.NONE;
        lastSoundTime = null;
        hasStartedMovingToBank = false;
        lastBankMovementActionTime = null;

        log.info("Redwood Helper plugin stopped.");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            if (startWoodcuttingXp == -1) {
                startWoodcuttingXp = client.getSkillExperience(Skill.WOODCUTTING);
                currentWoodcuttingXp = startWoodcuttingXp;
            }
        } else {
            currentFlashType = FlashType.NONE;
            lastSoundTime = null;
            hasStartedMovingToBank = false;
            lastBankMovementActionTime = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        if (startWoodcuttingXp == -1) {
            startWoodcuttingXp = client.getSkillExperience(Skill.WOODCUTTING);
            currentWoodcuttingXp = startWoodcuttingXp;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        int anim = localPlayer.getAnimation();
        boolean animChopping = WOODCUTTING_ANIMS.contains(anim);

        // Update chopping state
        if (animChopping) {
            if (!isChopping) {
                isChopping = true;
                choppingStartTime = Instant.now();
                stoppedChoppingTime = null;
                afkFlashStartTime = null;
                afkFlashSuppressed = false;
            }
            updateActiveTreeSpot();
        } else {
            if (isChopping) {
                isChopping = false;
                stoppedChoppingTime = Instant.now();
                choppingStartTime = null;
                activeTreeSpot = null;
            }
        }

        // Update special attack tracking
        int currentSpec = client.getVarpValue(VarPlayerID.SA_ENERGY);
        if (currentSpec >= 1000) {
            if (lastSpecEnergy < 1000 && specFlashStartTime == null) {
                specFlashStartTime = Instant.now();
                specFlashDismissed = false;
            }
        } else {
            specFlashDismissed = false;
            specFlashStartTime = null;
        }
        lastSpecEnergy = currentSpec;

        // Track movement and banking transit when inventory is full
        if (Rs2Inventory.isFull()) {
            if (isPlayerMoving()) {
                hasStartedMovingToBank = true;
                lastBankMovementActionTime = Instant.now();
            }
        } else {
            hasStartedMovingToBank = false;
            lastBankMovementActionTime = null;
        }

        // Determine current step
        updateCurrentStep();

        // Check flash sounds (supports minimized/unfocused client)
        checkFlashSound();
    }

    private void updateCurrentStep() {
        if (!supplyCalculator.hasAxeEquippedOrInInventory()) {
            currentStep = RedwoodStep.CHECKING_SUPPLIES;
            actionInstruction = "Equip or carry a woodcutting axe";
            return;
        }

        boolean invFull = Rs2Inventory.isFull();
        int playerPlane = client.getPlane();

        if (invFull) {
            boolean headingToBank = isActivelyGoingToBank();
            if (playerPlane == 1) {
                currentStep = RedwoodStep.CLIMBING_DOWN;
                actionInstruction = headingToBank
                        ? "Going to bank: Climb down rope ladder"
                        : "Inventory full! Climb down rope ladder";
            } else {
                currentStep = RedwoodStep.BANKING;
                actionInstruction = headingToBank
                        ? "Going to bank: Deposit logs in bank chest"
                        : "Inventory full! Click bank chest to deposit logs";
            }
            return;
        }

        // Inventory is not full
        if (playerPlane == 0) {
            currentStep = RedwoodStep.CLIMBING_UP;
            actionInstruction = "Climb rope ladder up to redwood canopy";
            return;
        }

        // Player is on upper level (plane 1)
        if (isChopping) {
            currentStep = RedwoodStep.WOODCUTTING;
            actionInstruction = "Woodcutting redwood tree...";
            return;
        }

        if (isAfk()) {
            currentStep = RedwoodStep.IDLE_AFK;
            actionInstruction = "AFK! Click an available redwood spot to chop";
            if (afkFlashStartTime == null && !afkFlashSuppressed) {
                afkFlashStartTime = Instant.now();
            }
        } else {
            currentStep = RedwoodStep.FINDING_TREE;
            actionInstruction = "Click an available redwood tree spot";
        }
    }

    private void updateActiveTreeSpot() {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        WorldPoint playerLoc = local.getWorldLocation();
        if (playerLoc == null) {
            return;
        }

        // Find nearest ready redwood spot within 2 tiles
        Rs2TileObjectModel nearestSpot = Microbot.getRs2TileObjectCache().query()
                .where(o -> RedwoodSceneOverlay.isReadyRedwood(o.getId()))
                .within(2)
                .nearestOnClientThread();

        if (nearestSpot != null) {
            activeTreeSpot = nearestSpot;
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (event.getActor() != client.getLocalPlayer()) {
            return;
        }

        int anim = event.getActor().getAnimation();
        if (WOODCUTTING_ANIMS.contains(anim)) {
            if (!isChopping) {
                isChopping = true;
                choppingStartTime = Instant.now();
                stoppedChoppingTime = null;
                afkFlashStartTime = null;
                afkFlashSuppressed = false;
            }
        } else if (anim == -1 && isChopping) {
            isChopping = false;
            stoppedChoppingTime = Instant.now();
            choppingStartTime = null;
            activeTreeSpot = null;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String msg = event.getMessage();
        if (msg.contains("You get some redwood logs")) {
            logsChopped++;
        } else if (msg.contains("bird's nest falls out")) {
            birdNests++;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.WOODCUTTING) {
            currentWoodcuttingXp = event.getXp();
            if (startWoodcuttingXp == -1) {
                startWoodcuttingXp = currentWoodcuttingXp;
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        lastUserActionTime = Instant.now();

        String option = event.getMenuOption();
        if (option == null) {
            return;
        }

        // When the user clicks to chop a tree, climb a ladder, or open a bank chest,
        // suppress the AFK flash so they are not blinded while walking to target!
        if (option.equalsIgnoreCase("Chop") || option.equalsIgnoreCase("Climb")
                || option.equalsIgnoreCase("Climb-up") || option.equalsIgnoreCase("Climb-down")
                || option.equalsIgnoreCase("Bank") || option.equalsIgnoreCase("Use")) {
            afkFlashSuppressed = true;
            afkFlashStartTime = null;
        }

        // Suppress inventory full alert when user clicks an action towards banking or moving
        if (Rs2Inventory.isFull() && isBankingOrMovementOption(option)) {
            hasStartedMovingToBank = true;
            lastBankMovementActionTime = Instant.now();
        }

        // Dismiss special attack flash when user activates spec
        if (option.equalsIgnoreCase("Special Attack")
                || (event.getMenuTarget() != null && event.getMenuTarget().contains("Special Attack"))) {
            specFlashDismissed = true;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            // Re-evaluate step when inventory changes (e.g. logs banked)
            if (!Rs2Inventory.isFull()) {
                hasStartedMovingToBank = false;
                lastBankMovementActionTime = null;
            }
            clientThread.invokeLater(this::updateCurrentStep);
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == SEARCHBOX_LOADED && geInterface.isAtGrandExchange()) {
            geInterface.init();
        }
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event) {
        geInterface.onGrandExchangeSearched(event);
    }

    // Flash condition evaluation
    public boolean isPlayerMoving() {
        if (client.isClientThread()) {
            Player localPlayer = client.getLocalPlayer();
            return localPlayer != null && localPlayer.getPoseAnimation() != localPlayer.getIdlePoseAnimation();
        }
        return Rs2Player.isMoving();
    }

    public boolean isActivelyGoingToBank() {
        if (!Rs2Inventory.isFull()) {
            return false;
        }

        // 1. If bank interface is open, user is actively banking
        if (Rs2Bank.isOpen()) {
            return true;
        }

        // 2. If character is actively moving (walking or running)
        if (isPlayerMoving()) {
            hasStartedMovingToBank = true;
            lastBankMovementActionTime = Instant.now();
            return true;
        }

        // 3. If user clicked to move/bank or recently moved, grant grace period
        if (hasStartedMovingToBank && lastBankMovementActionTime != null) {
            long secondsSinceMove = Duration.between(lastBankMovementActionTime, Instant.now()).getSeconds();
            if (secondsSinceMove < BANKING_GRACE_PERIOD_SECONDS) {
                return true;
            } else {
                // Grace period expired without moving or opening bank -> player is idle/AFK with full inventory
                hasStartedMovingToBank = false;
                lastBankMovementActionTime = null;
            }
        }

        return false;
    }

    public boolean isBankingOrMovementOption(String option) {
        if (option == null) {
            return false;
        }
        return option.equalsIgnoreCase("Walk here")
                || option.equalsIgnoreCase("Climb")
                || option.equalsIgnoreCase("Climb-up")
                || option.equalsIgnoreCase("Climb-down")
                || option.equalsIgnoreCase("Bank")
                || option.equalsIgnoreCase("Use")
                || option.equalsIgnoreCase("Open")
                || option.equalsIgnoreCase("Deposit")
                || option.equalsIgnoreCase("Deposit-all");
    }

    public boolean shouldFlashBlue() {
        if (!config.flashOnInventoryFull()) {
            return false;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (!Rs2Inventory.isFull()) {
            return false;
        }
        if (config.dismissInventoryFlashOnMovement() && isActivelyGoingToBank()) {
            return false;
        }
        return true;
    }

    public boolean shouldFlashRed() {
        if (!config.flashOnAfk()) {
            return false;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        // Do not flash AFK red when inventory is full (inventory full alert handles full bags)
        if (Rs2Inventory.isFull()) {
            return false;
        }
        // Moving players are not AFK
        if (isPlayerMoving()) {
            return false;
        }
        // Blue full inventory flash takes precedence
        if (shouldFlashBlue()) {
            return false;
        }
        // Active chopping does not flash
        if (isChopping) {
            return false;
        }
        // Only flash if idle threshold passed
        if (!isAfk()) {
            return false;
        }
        // If user already clicked or suppressed flash
        if (afkFlashSuppressed) {
            return false;
        }

        if (config.afkFlashUntilInteraction()) {
            return true;
        } else {
            // Fixed duration mode: 2.5 seconds
            if (afkFlashStartTime == null) {
                afkFlashStartTime = Instant.now();
                return true;
            }
            return Instant.now().isBefore(afkFlashStartTime.plusMillis(2500));
        }
    }

    public boolean shouldFlashYellow() {
        if (!config.flashOnSpecialAttack()) {
            return false;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        // Do not flash spec yellow when inventory is full or character is moving
        if (Rs2Inventory.isFull() || isPlayerMoving()) {
            return false;
        }
        // Full inventory blue flash and AFK red flash take precedence
        if (shouldFlashBlue() || shouldFlashRed()) {
            return false;
        }
        // Must have a special attack axe equipped
        if (!supplyCalculator.hasSpecAxeEquipped()) {
            return false;
        }
        // Special attack must be at 100% (1000)
        if (getSpecialAttackEnergy() < 1000) {
            return false;
        }
        // Flashes continuously until special attack is used
        if (config.specFlashUntilUsed()) {
            return true;
        } else {
            if (specFlashDismissed) {
                return false;
            }
            // Flash for 3.0 seconds upon reaching 100%
            if (specFlashStartTime == null) {
                specFlashStartTime = Instant.now();
                return true;
            }
            return Instant.now().isBefore(specFlashStartTime.plusMillis(3000));
        }
    }

    public FlashType getActiveFlashType() {
        if (shouldFlashBlue()) {
            return FlashType.INVENTORY_FULL;
        }
        if (shouldFlashRed()) {
            return FlashType.AFK;
        }
        if (shouldFlashYellow()) {
            return FlashType.SPECIAL_ATTACK;
        }
        return FlashType.NONE;
    }

    public void checkFlashSound() {
        synchronized (soundLock) {
            FlashType activeFlash = getActiveFlashType();

            if (activeFlash == FlashType.NONE) {
                currentFlashType = FlashType.NONE;
                lastSoundTime = null;
                return;
            }

            boolean soundEnabled = false;
            switch (activeFlash) {
                case INVENTORY_FULL:
                    soundEnabled = config.playInventoryFullSound();
                    break;
                case AFK:
                    soundEnabled = config.playAfkSound();
                    break;
                case SPECIAL_ATTACK:
                    soundEnabled = config.playSpecialAttackSound();
                    break;
                default:
                    break;
            }

            if (!soundEnabled) {
                currentFlashType = activeFlash;
                return;
            }

            Instant now = Instant.now();

            // Flash just started or changed type
            if (activeFlash != currentFlashType || lastSoundTime == null) {
                currentFlashType = activeFlash;
                lastSoundTime = now;
                playSystemSound();
                return;
            }

            // Flash is ongoing
            int intervalSeconds = config.soundIntervalSeconds();
            if (intervalSeconds > 0) {
                long millisElapsed = Duration.between(lastSoundTime, now).toMillis();
                if (millisElapsed >= (long) intervalSeconds * 1000L) {
                    lastSoundTime = now;
                    playSystemSound();
                }
            }
        }
    }

    public void playSystemSound() {
        soundPlayCount++;
        int volume = config != null ? config.soundVolume() : 80;
        String customPath = config != null ? config.customSoundPath() : null;
        Rs2SoundAlert.triggerAlert(customPath, volume);
    }

    public int getSpecialAttackEnergy() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return 0;
        }
        return client.getVarpValue(VarPlayerID.SA_ENERGY);
    }

    public boolean isAfk() {
        if (isChopping) {
            return false;
        }
        if (stoppedChoppingTime == null) {
            return false;
        }
        long idleMillis = Duration.between(stoppedChoppingTime, Instant.now()).toMillis();
        return idleMillis >= (long) (config.afkDelaySeconds() * 1000L);
    }

    public Duration getChoppingDuration() {
        if (!isChopping || choppingStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(choppingStartTime, Instant.now());
    }

    public Duration getIdleDuration() {
        if (isChopping || stoppedChoppingTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(stoppedChoppingTime, Instant.now());
    }

    public Duration getSessionDuration() {
        if (sessionStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(sessionStartTime, Instant.now());
    }

    public int getXpGained() {
        if (startWoodcuttingXp == -1 || currentWoodcuttingXp == -1) {
            return 0;
        }
        return Math.max(0, currentWoodcuttingXp - startWoodcuttingXp);
    }

    public int getXpPerHour() {
        Duration duration = getSessionDuration();
        long seconds = duration.getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((getXpGained() * 3600L) / seconds);
    }

    public int getLogsPerHour() {
        Duration duration = getSessionDuration();
        long seconds = duration.getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((logsChopped * 3600L) / seconds);
    }
}
