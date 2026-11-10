package net.runelite.client.plugins.microbot.wildyslayer;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.plugins.microbot.wildyslayer.overlays.WildySlayerFlashOverlay;
import net.runelite.client.plugins.microbot.wildyslayer.overlays.WildySlayerHudOverlay;
import net.runelite.client.plugins.microbot.wildyslayer.overlays.WildySlayerSceneOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Wildy Slayer Helper",
        description = "Wilderness Slayer helper: automatic Royal Seed Pod teleport at or below level 30 Wilderness when an attackable player is nearby, with PKer highlights and alerts",
        tags = {"user", "helper", "wilderness", "slayer", "wildyslayer", "antipk", "seedpod", "pvm", "overlay", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class WildySlayerPlugin extends Plugin
{
    public static final int DEFAULT_MAX_SEED_POD_WILDERNESS_LEVEL = 30;
    public static final int SEED_POD_COOLDOWN_TICKS = 3;
    public static final int PKER_ALERT_COOLDOWN_TICKS = 5;

    private static final Pattern WILDERNESS_LEVEL_PATTERN = Pattern.compile("Level:\\s*(\\d+)");

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WildySlayerConfig config;

    @Inject
    private WildySlayerHudOverlay hudOverlay;

    @Inject
    private WildySlayerSceneOverlay sceneOverlay;

    @Inject
    private WildySlayerFlashOverlay flashOverlay;

    @Getter
    private int totalTicks = 0;

    private int lastSeedPodInvokeTick = -SEED_POD_COOLDOWN_TICKS;
    private int lastPkerAlertTick = -PKER_ALERT_COOLDOWN_TICKS;

    @Getter
    private boolean seedPodTriggeredThisEncounter = false;

    /**
     * Currently detected threat players (PKers) nearby in the Wilderness.
     */
    @Getter
    private final List<Player> detectedThreatPlayers = new ArrayList<>();

    @Provides
    WildySlayerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WildySlayerConfig.class);
    }

    @Override
    protected void startUp()
    {
        reset();
        overlayManager.add(hudOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(flashOverlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(hudOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(flashOverlay);
        reset();
    }

    public void reset()
    {
        totalTicks = 0;
        lastSeedPodInvokeTick = -SEED_POD_COOLDOWN_TICKS;
        lastPkerAlertTick = -PKER_ALERT_COOLDOWN_TICKS;
        seedPodTriggeredThisEncounter = false;
        detectedThreatPlayers.clear();
    }

    // =========================================================================
    // Wilderness State & Pure Helpers
    // =========================================================================

    /**
     * Pure helper: parses the Wilderness level from the PvP icons widget text
     * (e.g. "Level: 24" or "Level: 24<br>96-126").
     */
    public static int parseWildernessLevelText(String widgetText)
    {
        if (widgetText == null || widgetText.isEmpty())
        {
            return 0;
        }
        Matcher matcher = WILDERNESS_LEVEL_PATTERN.matcher(widgetText);
        if (!matcher.find())
        {
            return 0;
        }
        try
        {
            return Math.max(0, Integer.parseInt(matcher.group(1)));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    /**
     * Pure helper: resolves the effective Wilderness level from the widget text or WorldPoint fallback.
     */
    public static int resolveWildernessLevel(int widgetWildernessLevel, WorldPoint worldPoint)
    {
        if (widgetWildernessLevel > 0)
        {
            return widgetWildernessLevel;
        }
        if (worldPoint == null)
        {
            return 0;
        }
        return Math.max(0, Rs2Pvp.getWildernessLevelFrom(worldPoint));
    }

    /**
     * Pure helper: returns whether Royal Seed Pod can be used at the current Wilderness level.
     */
    public static boolean canUseRoyalSeedPod(
            int wildernessLevel,
            int maxSeedPodWildernessLevel,
            boolean teleblocked)
    {
        int clampedMax = Math.max(1, Math.min(DEFAULT_MAX_SEED_POD_WILDERNESS_LEVEL, maxSeedPodWildernessLevel));
        return wildernessLevel >= 1 && wildernessLevel <= clampedMax && !teleblocked;
    }

    /**
     * Pure helper: determines if Auto Royal Seed Pod should fire on the current tick.
     */
    public static boolean shouldTriggerSeedPod(
            boolean autoSeedPodEnabled,
            boolean inWilderness,
            int wildernessLevel,
            int maxSeedPodWildernessLevel,
            boolean teleblocked,
            boolean hasThreatPlayer,
            int totalTicks,
            int lastSeedPodInvokeTick)
    {
        if (!autoSeedPodEnabled || !inWilderness || !hasThreatPlayer)
        {
            return false;
        }
        if (!canUseRoyalSeedPod(wildernessLevel, maxSeedPodWildernessLevel, teleblocked))
        {
            return false;
        }
        return totalTicks - lastSeedPodInvokeTick >= SEED_POD_COOLDOWN_TICKS;
    }

    /**
     * Pure helper: determines if another player in the Wilderness is a potential PKer threat.
     */
    public static boolean isThreatPlayer(
            int localCombatLevel,
            int otherCombatLevel,
            int wildernessLevel,
            boolean isPvpWorld,
            boolean isDeadmanWorld,
            boolean ignoreFriendsAndClan,
            boolean isFriendOrClan,
            WildySlayerConfig.PkerDetectionMode mode)
    {
        return isThreatPlayer(
                localCombatLevel,
                otherCombatLevel,
                wildernessLevel,
                wildernessLevel,
                isPvpWorld,
                isDeadmanWorld,
                ignoreFriendsAndClan,
                isFriendOrClan,
                mode
        );
    }

    /**
     * Pure helper: determines if another player in the Wilderness is a potential PKer threat,
     * accounting for both the local player's and other player's Wilderness level.
     */
    public static boolean isThreatPlayer(
            int localCombatLevel,
            int otherCombatLevel,
            int localWildernessLevel,
            int otherWildernessLevel,
            boolean isPvpWorld,
            boolean isDeadmanWorld,
            boolean ignoreFriendsAndClan,
            boolean isFriendOrClan,
            WildySlayerConfig.PkerDetectionMode mode)
    {
        if (ignoreFriendsAndClan && isFriendOrClan)
        {
            return false;
        }
        if (mode == WildySlayerConfig.PkerDetectionMode.ANY_PLAYER || isDeadmanWorld)
        {
            return true;
        }

        int baseWildy = Math.max(1, Math.max(localWildernessLevel, otherWildernessLevel));
        int effectiveWildy = baseWildy + (isPvpWorld ? 15 : 0);
        return Math.abs(localCombatLevel - otherCombatLevel) <= effectiveWildy;
    }

    public boolean isInWilderness()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return false;
        }
        if (client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1)
        {
            return true;
        }
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        return wp != null && Rs2Pvp.getWildernessLevelFrom(wp) > 0;
    }

    public int getCurrentWildernessLevel()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return 0;
        }

        int widgetLevel = 0;
        Widget wildyWidget = client.getWidget(InterfaceID.PvpIcons.WILDERNESSLEVEL);
        if (wildyWidget != null && !wildyWidget.isHidden())
        {
            widgetLevel = parseWildernessLevelText(wildyWidget.getText());
        }

        WorldPoint localWp = client.getLocalPlayer().getWorldLocation();
        return resolveWildernessLevel(widgetLevel, localWp);
    }

    public boolean isTeleblocked()
    {
        return client != null && client.getVarbitValue(VarbitID.TELEBLOCK_CYCLES) > 0;
    }

    public boolean hasRoyalSeedPodInInventory()
    {
        if (client == null)
        {
            return false;
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        return inv != null && inv.contains(ItemID.MM2_ROYAL_SEED_POD);
    }

    // =========================================================================
    // RuneLite Event Subscriptions
    // =========================================================================

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
        {
            reset();
        }
        else if (state == GameState.LOADING)
        {
            detectedThreatPlayers.clear();
        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event)
    {
        Player player = event.getPlayer();
        if (player == null || !isInWilderness())
        {
            return;
        }

        if (isPlayerThreatToLocal(player))
        {
            if (!detectedThreatPlayers.contains(player))
            {
                detectedThreatPlayers.add(player);
            }
            handlePkerDetected();
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        Player player = event.getPlayer();
        if (player != null)
        {
            detectedThreatPlayers.remove(player);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        totalTicks++;

        if (isInWilderness())
        {
            updateThreatPlayersInWilderness();
            if (!detectedThreatPlayers.isEmpty())
            {
                handlePkerDetected();
            }
            else
            {
                seedPodTriggeredThisEncounter = false;
            }
        }
        else
        {
            detectedThreatPlayers.clear();
            seedPodTriggeredThisEncounter = false;
        }
    }

    // =========================================================================
    // Anti-PK & Client-Thread Safe Royal Seed Pod Teleport
    // =========================================================================

    private boolean isPlayerThreatToLocal(Player other)
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        if (local == null || other == null || other == local)
        {
            return false;
        }

        boolean isFriendOrClan = other.isFriend() || other.isClanMember() || other.isFriendsChatMember();
        int localWildyLevel = getCurrentWildernessLevel();
        WorldPoint otherWp = other.getWorldLocation();
        int otherWildyLevel = otherWp != null ? Rs2Pvp.getWildernessLevelFrom(otherWp) : localWildyLevel;
        if (otherWildyLevel <= 0)
        {
            otherWildyLevel = localWildyLevel;
        }

        boolean isPvpWorld = WorldType.isPvpWorld(client.getWorldType());
        boolean isDeadman = WorldType.isDeadmanWorld(client.getWorldType());

        return isThreatPlayer(
                local.getCombatLevel(),
                other.getCombatLevel(),
                localWildyLevel,
                otherWildyLevel,
                isPvpWorld,
                isDeadman,
                config.ignoreFriendsAndClan(),
                isFriendOrClan,
                config.pkerDetectionMode()
        );
    }

    private void updateThreatPlayersInWilderness()
    {
        detectedThreatPlayers.clear();
        if (client == null || client.getLocalPlayer() == null)
        {
            return;
        }

        for (Player player : client.getPlayers())
        {
            if (isPlayerThreatToLocal(player))
            {
                detectedThreatPlayers.add(player);
            }
        }
    }

    private void handlePkerDetected()
    {
        if (config == null || client == null)
        {
            return;
        }

        if (config.soundAlerts() && totalTicks - lastPkerAlertTick >= PKER_ALERT_COOLDOWN_TICKS)
        {
            Rs2SoundAlert.triggerAlert(config.soundVolume());
            lastPkerAlertTick = totalTicks;
        }

        int wildyLevel = getCurrentWildernessLevel();
        if (shouldTriggerSeedPod(
                config.autoSeedPodOnPker(),
                isInWilderness(),
                wildyLevel,
                config.maxSeedPodWildernessLevel(),
                isTeleblocked(),
                !detectedThreatPlayers.isEmpty(),
                totalTicks,
                lastSeedPodInvokeTick))
        {
            if (invokeRoyalSeedPod())
            {
                lastSeedPodInvokeTick = totalTicks;
                seedPodTriggeredThisEncounter = true;
            }
        }
    }

    /**
     * Non-blocking client-thread safe Royal Seed Pod activation.
     * Invokes "Commune" on ItemID.MM2_ROYAL_SEED_POD directly in InterfaceID.Inventory.ITEMS
     * without switching tabs or calling sleepUntil.
     */
    private boolean invokeRoyalSeedPod()
    {
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv == null || inv.getItems() == null)
        {
            return false;
        }

        Item[] items = inv.getItems();
        int podSlot = -1;
        for (int i = 0; i < items.length; i++)
        {
            if (items[i] != null && items[i].getId() == ItemID.MM2_ROYAL_SEED_POD)
            {
                podSlot = i;
                break;
            }
        }

        if (podSlot == -1)
        {
            return false;
        }

        int identifier = 2; // Standard CC_OP identifier for iop1 ("Commune") on InterfaceID.Inventory.ITEMS
        Rectangle bounds = Rs2UiHelper.getDefaultRectangle();

        Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
        if (invWidget != null && invWidget.getDynamicChildren() != null)
        {
            Widget[] children = invWidget.getDynamicChildren();
            if (podSlot < children.length && children[podSlot] != null)
            {
                Widget podWidget = children[podSlot];
                if (podWidget.getBounds() != null && podWidget.getBounds().width > 0 && podWidget.getBounds().height > 0)
                {
                    bounds = podWidget.getBounds();
                }
                String[] actions = podWidget.getActions();
                if (actions != null)
                {
                    for (int idx = 0; idx < actions.length; idx++)
                    {
                        if (actions[idx] != null && actions[idx].equalsIgnoreCase("Commune"))
                        {
                            identifier = idx + 1;
                            break;
                        }
                    }
                }
            }
        }

        Microbot.doInvoke(
                new NewMenuEntry()
                        .option("Commune")
                        .target("<col=ff9040>Royal seed pod</col>")
                        .identifier(identifier)
                        .opcode(MenuAction.CC_OP.getId())
                        .param0(podSlot)
                        .param1(InterfaceID.Inventory.ITEMS)
                        .itemId(ItemID.MM2_ROYAL_SEED_POD),
                bounds
        );
        return true;
    }
}
