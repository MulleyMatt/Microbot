package net.runelite.client.plugins.microbot.barbarianfishing;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Barbarian Fishing Helper",
        description = "Interactive Barbarian fishing helper with spot marking, left-click Drop swap, AFK red screen flash, and inventory full blue screen flash",
        tags = {"user", "fishing", "barbarian", "helper", "afk", "flash", "drop", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class BarbarianFishingPlugin extends Plugin {

    private static final int SEARCHBOX_LOADED = 750;
    private static final long DROP_SUPPRESS_MILLIS = 1500L;

    public static final Set<Integer> FISHING_ANIMS = ImmutableSet.of(
            AnimationID.FISHING_OILY_ROD,
            AnimationID.FISHING_POLE_CAST,
            AnimationID.FISHING_BARBARIAN_ROD,
            AnimationID.FISHING_PEARL_BARBARIAN_ROD,
            AnimationID.FISHING_PEARL_BARBARIAN_ROD_2
    );

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BarbarianFishingConfig config;

    @Inject
    private BarbarianFishingOverlay hudOverlay;

    @Inject
    private BarbarianFishingSceneOverlay sceneOverlay;

    @Inject
    private BarbarianFishingFlashOverlay flashOverlay;

    @Inject
    private BarbarianFishingInventoryOverlay inventoryOverlay;

    @Inject
    private BarbarianFishingSupplyCalculator supplyCalculator;

    @Inject
    private BarbarianFishingGrandExchangeInterface geInterface;

    @Getter
    private BarbarianFishingStep currentStep = BarbarianFishingStep.CHECKING_SUPPLIES;

    @Getter
    private String actionInstruction = "Checking gear...";

    @Getter
    private boolean isFishing = false;

    @Getter
    private Instant fishingStartTime = null;

    @Getter
    private Instant stoppedFishingTime = null;

    @Getter
    private NPC activeFishingSpot = null;

    private Instant afkFlashStartTime = null;
    private boolean afkFlashSuppressed = false;
    private Instant fullInvDropSuppressUntil = null;

    // Special Attack tracking
    private int lastSpecEnergy = 0;
    private Instant specFlashStartTime = null;
    private boolean specFlashDismissed = false;

    public enum FlashType {
        NONE,
        INVENTORY_FULL,
        AFK,
        SPECIAL_ATTACK
    }

    private final Object soundLock = new Object();
    private FlashType currentAlertType = FlashType.NONE;
    private Instant lastSoundTime = null;

    @Getter
    private int soundPlayCount = 0;

    // Statistics
    @Getter
    private Instant sessionStartTime = null;

    private int startFishingXp = -1;
    private int currentFishingXp = -1;

    @Getter
    private int fishCaught = 0;

    @Getter
    private int troutCaught = 0;

    @Getter
    private int salmonCaught = 0;

    @Getter
    private int sturgeonCaught = 0;

    @Provides
    BarbarianFishingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BarbarianFishingConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(hudOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(flashOverlay);
        overlayManager.add(inventoryOverlay);

        sessionStartTime = Instant.now();
        fishCaught = 0;
        troutCaught = 0;
        salmonCaught = 0;
        sturgeonCaught = 0;
        isFishing = false;
        fishingStartTime = null;
        stoppedFishingTime = null;
        activeFishingSpot = null;
        afkFlashStartTime = null;
        afkFlashSuppressed = false;
        fullInvDropSuppressUntil = null;
        specFlashStartTime = null;
        specFlashDismissed = false;
        currentAlertType = FlashType.NONE;
        lastSoundTime = null;
        soundPlayCount = 0;

        if (client.getGameState() == GameState.LOGGED_IN) {
            startFishingXp = client.getSkillExperience(Skill.FISHING);
            currentFishingXp = startFishingXp;
            lastSpecEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY);
        } else {
            startFishingXp = -1;
            currentFishingXp = -1;
            lastSpecEnergy = 0;
        }

        log.info("Barbarian Fishing Helper plugin started!");
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

        currentAlertType = FlashType.NONE;
        lastSoundTime = null;
        activeFishingSpot = null;
        fullInvDropSuppressUntil = null;

        log.info("Barbarian Fishing Helper plugin stopped.");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            if (startFishingXp == -1) {
                startFishingXp = client.getSkillExperience(Skill.FISHING);
                currentFishingXp = startFishingXp;
            }
        } else {
            currentAlertType = FlashType.NONE;
            lastSoundTime = null;
            isFishing = false;
            activeFishingSpot = null;
            fullInvDropSuppressUntil = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        if (startFishingXp == -1) {
            startFishingXp = client.getSkillExperience(Skill.FISHING);
            currentFishingXp = startFishingXp;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        int anim = localPlayer.getAnimation();
        boolean animFishing = FISHING_ANIMS.contains(anim);

        if (animFishing) {
            if (!isFishing) {
                isFishing = true;
                fishingStartTime = Instant.now();
                stoppedFishingTime = null;
                afkFlashStartTime = null;
                afkFlashSuppressed = false;
            }
            updateActiveFishingSpot(localPlayer);
        } else {
            if (isFishing) {
                isFishing = false;
                stoppedFishingTime = Instant.now();
                fishingStartTime = null;
                activeFishingSpot = null;
            }
        }

        // Update special attack tracking (1000 = 100%)
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

        updateCurrentStep();
        checkAlertSound();
    }

    private void updateCurrentStep() {
        if (!supplyCalculator.hasBarbarianRodEquippedOrInInventory()) {
            currentStep = BarbarianFishingStep.CHECKING_SUPPLIES;
            actionInstruction = "Carry or equip a Barbarian rod";
            return;
        }

        if (!supplyCalculator.hasBaitInInventory()) {
            currentStep = BarbarianFishingStep.CHECKING_SUPPLIES;
            actionInstruction = "Carry feathers, bait, roe, caviar, or offcuts";
            return;
        }

        if (isInventoryFullOfFish()) {
            currentStep = BarbarianFishingStep.INVENTORY_FULL;
            actionInstruction = "Inventory full! Left-click leaping fish to drop";
            return;
        }

        if (isFishing) {
            currentStep = BarbarianFishingStep.FISHING;
            actionInstruction = "Barbarian fishing...";
            return;
        }

        if (isAfk()) {
            currentStep = BarbarianFishingStep.IDLE_AFK;
            actionInstruction = "AFK! Click a Barbarian fishing spot";
            if (afkFlashStartTime == null && !afkFlashSuppressed) {
                afkFlashStartTime = Instant.now();
            }
        } else {
            currentStep = BarbarianFishingStep.FINDING_SPOT;
            actionInstruction = "Click a Barbarian fishing spot";
        }
    }

    private void updateActiveFishingSpot(Player localPlayer) {
        Actor interacting = localPlayer.getInteracting();
        if (interacting instanceof NPC) {
            NPC npc = (NPC) interacting;
            if (BarbarianFishingSceneOverlay.isBarbarianFishingSpot(npc.getId())) {
                activeFishingSpot = npc;
                return;
            }
        }

        Rs2NpcModel nearestSpot = Microbot.getRs2NpcCache().query()
                .where(n -> BarbarianFishingSceneOverlay.isBarbarianFishingSpot(n.getId()))
                .within(2)
                .nearestOnClientThread();

        if (nearestSpot != null && nearestSpot.getNpc() != null) {
            activeFishingSpot = nearestSpot.getNpc();
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (event.getActor() != client.getLocalPlayer()) {
            return;
        }

        int anim = event.getActor().getAnimation();
        if (FISHING_ANIMS.contains(anim)) {
            if (!isFishing) {
                isFishing = true;
                fishingStartTime = Instant.now();
                stoppedFishingTime = null;
                afkFlashStartTime = null;
                afkFlashSuppressed = false;
            }
        } else if (anim == -1 && isFishing) {
            isFishing = false;
            stoppedFishingTime = Instant.now();
            fishingStartTime = null;
            activeFishingSpot = null;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String msg = event.getMessage();
        if (msg == null) {
            return;
        }

        String lower = msg.toLowerCase();
        if (lower.contains("you catch a leaping trout")) {
            fishCaught++;
            troutCaught++;
        } else if (lower.contains("you catch a leaping salmon")) {
            fishCaught++;
            salmonCaught++;
        } else if (lower.contains("you catch a leaping sturgeon")) {
            fishCaught++;
            sturgeonCaught++;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.FISHING) {
            currentFishingXp = event.getXp();
            if (startFishingXp == -1) {
                startFishingXp = currentFishingXp;
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        String option = event.getMenuOption();
        if (option == null) {
            return;
        }

        String cleanOption = Text.removeTags(option);

        if (isFishingInteractionOption(cleanOption)) {
            afkFlashSuppressed = true;
            afkFlashStartTime = null;
        }

        if (isDropOption(cleanOption)) {
            // Immediately stop full-inventory alert when the player begins dropping fish,
            // and reset the idle timer so dropping a full inventory doesn't trigger AFK mid-drop.
            fullInvDropSuppressUntil = Instant.now().plusMillis(DROP_SUPPRESS_MILLIS);
            if (!isFishing) {
                stoppedFishingTime = Instant.now();
                afkFlashStartTime = null;
                afkFlashSuppressed = false;
            }
        }

        if (cleanOption.equalsIgnoreCase("Special Attack")
                || (event.getMenuTarget() != null && event.getMenuTarget().contains("Special Attack"))) {
            specFlashDismissed = true;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            if (!Rs2Inventory.isFull()) {
                fullInvDropSuppressUntil = null;
            }
            clientThread.invokeLater(this::updateCurrentStep);
        }
    }

    /**
     * Runs after MenuEntrySwapperPlugin (priority 0) so left-click on Leaping trout,
     * Leaping salmon, and Leaping sturgeon is always "Drop" while this plugin is active.
     */
    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort event) {
        if (!config.leftClickDropFish() || client.isMenuOpen() || client.isWidgetSelected()) {
            return;
        }

        swapDropToTopIfLeapingFish(client.getMenu());
    }

    boolean swapDropToTopIfLeapingFish(Menu root) {
        if (root == null) {
            return false;
        }

        MenuEntry[] menuEntries = root.getMenuEntries();
        if (menuEntries == null || menuEntries.length < 2) {
            return false;
        }

        int topIdx = menuEntries.length - 1;
        int dropIdx = findLeapingFishDropEntryIndex(menuEntries);
        if (dropIdx >= 0 && dropIdx != topIdx) {
            swapMenuEntries(root, menuEntries, dropIdx, topIdx);
            return true;
        }
        return false;
    }

    private int findLeapingFishDropEntryIndex(MenuEntry[] menuEntries) {
        for (int i = menuEntries.length - 1; i >= 0; i--) {
            MenuEntry entry = menuEntries[i];
            if (entry == null || !isDropOption(entry.getOption())) {
                continue;
            }

            Widget widget = entry.getWidget();
            if (widget == null || WidgetUtil.componentToInterface(widget.getId()) != InterfaceID.INVENTORY) {
                continue;
            }

            int itemId = entry.getItemId() > 0 ? entry.getItemId() : widget.getItemId();
            if (BarbarianFishingSupplyCalculator.isLeapingFish(itemId)) {
                return i;
            }
        }
        return -1;
    }

    private void swapMenuEntries(Menu root, MenuEntry[] entries, int index1, int index2) {
        MenuEntry entry1 = entries[index1];
        MenuEntry entry2 = entries[index2];

        entries[index1] = entry2;
        entries[index2] = entry1;

        if (entry1.getType() == MenuAction.CC_OP_LOW_PRIORITY) {
            entry1.setType(MenuAction.CC_OP);
        }
        if (entry2.getType() == MenuAction.CC_OP_LOW_PRIORITY) {
            entry2.setType(MenuAction.CC_OP);
        }

        root.setMenuEntries(entries);
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

    public boolean isDropOption(String option) {
        if (option == null) {
            return false;
        }
        return Text.removeTags(option).trim().equalsIgnoreCase("Drop");
    }

    public boolean isFishingInteractionOption(String option) {
        if (option == null) {
            return false;
        }
        String clean = Text.removeTags(option).trim();
        return clean.equalsIgnoreCase("Use-rod")
                || clean.equalsIgnoreCase("Fish")
                || clean.equalsIgnoreCase("Lure")
                || clean.equalsIgnoreCase("Bait")
                || clean.equalsIgnoreCase("Cage")
                || clean.equalsIgnoreCase("Net")
                || clean.equalsIgnoreCase("Harpoon");
    }

    public boolean isPlayerMoving() {
        if (client.isClientThread()) {
            Player localPlayer = client.getLocalPlayer();
            return localPlayer != null && localPlayer.getPoseAnimation() != localPlayer.getIdlePoseAnimation();
        }
        return Rs2Player.isMoving();
    }

    public boolean isInventoryFullOfFish() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (!Rs2Inventory.isFull()) {
            return false;
        }
        return supplyCalculator.countLeapingFishInInventory() > 0;
    }

    public boolean isInventoryFullAlertActive() {
        if (!isInventoryFullOfFish()) {
            return false;
        }
        return fullInvDropSuppressUntil == null || !Instant.now().isBefore(fullInvDropSuppressUntil);
    }

    public boolean isAfkAlertActive() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (isInventoryFullOfFish() || isPlayerMoving() || isFishing || !isAfk() || afkFlashSuppressed) {
            return false;
        }

        if (config.afkFlashUntilInteraction()) {
            return true;
        }

        if (afkFlashStartTime == null) {
            afkFlashStartTime = Instant.now();
            return true;
        }
        return Instant.now().isBefore(afkFlashStartTime.plusMillis(2500));
    }

    public boolean isSpecialAttackAlertActive() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (isInventoryFullOfFish() || isPlayerMoving()) {
            return false;
        }
        if (!supplyCalculator.hasSpecHarpoonEquipped() || getSpecialAttackEnergy() < 1000) {
            return false;
        }

        if (config.specFlashUntilUsed()) {
            return true;
        }

        if (specFlashDismissed) {
            return false;
        }

        if (specFlashStartTime == null) {
            specFlashStartTime = Instant.now();
            return true;
        }
        return Instant.now().isBefore(specFlashStartTime.plusMillis(3000));
    }

    public boolean shouldFlashBlue() {
        return config.flashOnInventoryFull() && isInventoryFullAlertActive();
    }

    public boolean shouldFlashRed() {
        return config.flashOnAfk() && !shouldFlashBlue() && isAfkAlertActive();
    }

    public boolean shouldFlashYellow() {
        return config.flashOnSpecialAttack() && !shouldFlashBlue() && !shouldFlashRed() && isSpecialAttackAlertActive();
    }

    public FlashType getActiveAlertType() {
        if (isInventoryFullAlertActive()) {
            return FlashType.INVENTORY_FULL;
        }
        if (isAfkAlertActive()) {
            return FlashType.AFK;
        }
        if (isSpecialAttackAlertActive()) {
            return FlashType.SPECIAL_ATTACK;
        }
        return FlashType.NONE;
    }

    public void checkAlertSound() {
        synchronized (soundLock) {
            FlashType activeAlert = getActiveAlertType();

            if (activeAlert == FlashType.NONE) {
                currentAlertType = FlashType.NONE;
                lastSoundTime = null;
                return;
            }

            boolean soundEnabled = false;
            switch (activeAlert) {
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
                currentAlertType = activeAlert;
                return;
            }

            Instant now = Instant.now();

            if (activeAlert != currentAlertType || lastSoundTime == null) {
                currentAlertType = activeAlert;
                lastSoundTime = now;
                playSystemSound();
                return;
            }

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
        if (isFishing || stoppedFishingTime == null) {
            return false;
        }
        long idleMillis = Duration.between(stoppedFishingTime, Instant.now()).toMillis();
        return idleMillis >= (long) (config.afkDelaySeconds() * 1000L);
    }

    public Duration getFishingDuration() {
        if (!isFishing || fishingStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(fishingStartTime, Instant.now());
    }

    public Duration getIdleDuration() {
        if (isFishing || stoppedFishingTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(stoppedFishingTime, Instant.now());
    }

    public Duration getSessionDuration() {
        if (sessionStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(sessionStartTime, Instant.now());
    }

    public int getXpGained() {
        if (startFishingXp == -1 || currentFishingXp == -1) {
            return 0;
        }
        return Math.max(0, currentFishingXp - startFishingXp);
    }

    public int getXpPerHour() {
        long seconds = getSessionDuration().getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((getXpGained() * 3600L) / seconds);
    }

    public int getFishPerHour() {
        long seconds = getSessionDuration().getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((fishCaught * 3600L) / seconds);
    }
}
