package net.runelite.client.plugins.microbot.tormenteddemons;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.HeadIcon;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonFlashOverlay;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonHudOverlay;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonSceneOverlay;
import net.runelite.client.plugins.microbot.tormenteddemons.overlays.TormentedDemonWidgetOverlay;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.microbot.util.magic.thralls.Rs2Thrall;
import net.runelite.client.plugins.microbot.util.magic.thralls.ThrallType;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Tormented Demons Helper",
        description = "Interactive money-making PvM helper for Tormented Demons (auto-prayer with post-fire-bomb prediction, auto-thralls, shift-click walk-through, punish/spec/switch gear highlights, fire bomb dodge solver, profit tracker, and 1-click Bank & GE filters)",
        tags = {"user", "helper", "tormented", "demons", "td", "synapse", "burning claws", "pvm", "slayer", "overlay", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class TormentedDemonPlugin extends Plugin
{
    public static final Set<Integer> ANCIENT_GUTHIXIAN_TEMPLE_REGIONS = ImmutableSet.of(16196, 16197, 16452, 16453);

    public static final Set<Integer> TORMENTED_DEMON_IDS = ImmutableSet.of(
            NpcID.TORMENTED_DEMON_1,
            NpcID.TORMENTED_DEMON_2
    );

    public static final int FIRE_BOMB_DANGER_TICKS = 3;
    public static final int THRALL_RECAST_DEBOUNCE_TICKS = 3;
    public static final int ALERT_COOLDOWN_TICKS = 8;
    public static final long JACKPOT_BANNER_DURATION_MS = 10_000L;

    private static final int BLOCKED_TILE_MASK = 0x240100;

    @Value
    public static class GroundDropInfo
    {
        int itemId;
        String name;
        int quantity;
        long totalValueGp;
        WorldPoint worldPoint;
    }

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private TormentedDemonConfig config;

    @Inject
    private TormentedDemonSupplyCalculator supplyCalculator;

    @Inject
    private TormentedDemonBankInterface bankInterface;

    @Inject
    private TormentedDemonGrandExchangeInterface geInterface;

    @Inject
    private TormentedDemonHudOverlay hudOverlay;

    @Inject
    private TormentedDemonPrayerHelperOverlay prayerHelperOverlay;

    @Inject
    private TormentedDemonSceneOverlay sceneOverlay;

    @Inject
    private TormentedDemonWidgetOverlay widgetOverlay;

    @Inject
    private TormentedDemonFlashOverlay flashOverlay;

    @Getter
    private final Map<NPC, TormentedDemonState> tormentedDemons = new LinkedHashMap<>();

    @Getter
    private final Map<WorldPoint, Integer> fireBombDangerTiles = new LinkedHashMap<>();

    @Getter
    private final Map<WorldPoint, List<GroundDropInfo>> groundDrops = new LinkedHashMap<>();

    @Getter
    private NPC primaryDemon;

    @Getter
    private int totalTicks = 0;

    @Getter
    private int tdMultiwayLevel = 0;

    @Getter
    private int walkableAdjacentTileCount = 8;

    @Getter
    private TormentedDemonGearClassifier.CombatStyle equippedWeaponStyle;

    @Getter
    private int specEnergyPercent = 0;

    @Getter
    private int ashesCount = 0;

    @Getter
    private boolean hasSmoulderingConsumable = false;

    @Getter
    private boolean postKillPrayerReminder = false;

    private final Map<Integer, String> cachedItemNames = new LinkedHashMap<>();

    // Session money-making tracker
    @Getter
    private int sessionKills = 0;

    @Getter
    private long sessionStartTimeMs = 0L;

    @Getter
    private long sessionLootGp = 0L;

    @Getter
    private int synapseCount = 0;

    @Getter
    private int burningClawCount = 0;

    @Getter
    private String jackpotDropName;

    @Getter
    private long jackpotDropTimestampMs = 0L;

    private int lastThrallCastTick = -THRALL_RECAST_DEBOUNCE_TICKS;
    private int lastLowResourceAlertTick = -ALERT_COOLDOWN_TICKS;
    private int lastFireBombAlertTick = -ALERT_COOLDOWN_TICKS;
    private int lastJackpotAlertTick = -ALERT_COOLDOWN_TICKS;

    @Provides
    TormentedDemonConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TormentedDemonConfig.class);
    }

    @Override
    protected void startUp()
    {
        resetRuntimeState();
        overlayManager.add(hudOverlay);
        overlayManager.add(prayerHelperOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(widgetOverlay);
        overlayManager.add(flashOverlay);

        clientThread.invokeLater(() -> {
            scanExistingState();
            if (geInterface.isAtGrandExchange())
            {
                geInterface.init();
            }
            if (bankInterface.isBankOpen())
            {
                bankInterface.init();
            }
        });
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(hudOverlay);
        overlayManager.remove(prayerHelperOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(widgetOverlay);
        overlayManager.remove(flashOverlay);

        clientThread.invokeLater(() -> {
            geInterface.destroy();
            bankInterface.destroy();
        });
        resetRuntimeState();
    }

    public void resetRuntimeState()
    {
        tormentedDemons.clear();
        fireBombDangerTiles.clear();
        groundDrops.clear();
        primaryDemon = null;
        totalTicks = 0;
        tdMultiwayLevel = 0;
        walkableAdjacentTileCount = 8;
        equippedWeaponStyle = null;
        specEnergyPercent = 0;
        ashesCount = 0;
        hasSmoulderingConsumable = false;
        postKillPrayerReminder = false;
        lastThrallCastTick = -THRALL_RECAST_DEBOUNCE_TICKS;
        lastLowResourceAlertTick = -ALERT_COOLDOWN_TICKS;
        lastFireBombAlertTick = -ALERT_COOLDOWN_TICKS;
        lastJackpotAlertTick = -ALERT_COOLDOWN_TICKS;
    }

    public void resetSessionTracker()
    {
        sessionKills = 0;
        sessionStartTimeMs = 0L;
        sessionLootGp = 0L;
        synapseCount = 0;
        burningClawCount = 0;
        jackpotDropName = null;
        jackpotDropTimestampMs = 0L;
    }

    private void scanExistingState()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        for (NPC npc : client.getNpcs())
        {
            registerNpc(npc);
        }
        refreshPlayerContext();
        selectPrimaryDemon();
    }

    private void registerNpc(NPC npc)
    {
        if (npc == null || !TORMENTED_DEMON_IDS.contains(npc.getId()) || npc.isDead())
        {
            return;
        }
        TormentedDemonState state = tormentedDemons.computeIfAbsent(npc, TormentedDemonState::new);
        state.onProtectedStylesChanged(TormentedDemonOverlayUtils.extractProtectedStyles(npc.getOverheadSpriteIds()));
    }

    private void unregisterNpc(NPC npc)
    {
        if (npc == null)
        {
            return;
        }
        TormentedDemonState removed = tormentedDemons.remove(npc);
        if (removed != null)
        {
            boolean wasPrimary = (npc == primaryDemon);
            if (wasPrimary)
            {
                primaryDemon = null;
            }
            if ((removed.isInCombatWithPlayer() || wasPrimary) && !hasAnyEngagedDemon())
            {
                onCombatEnded();
            }
        }
    }

    private void onCombatEnded()
    {
        postKillPrayerReminder = true;
        handlePostKillPrayerDisable();
    }

    // =========================================================================
    // Temple & Fight Queries
    // =========================================================================

    public boolean isInGuthixianTemple()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return false;
        }
        if (tdMultiwayLevel > 0 || !tormentedDemons.isEmpty())
        {
            return true;
        }
        Player local = client.getLocalPlayer();
        if (local == null || local.getWorldLocation() == null)
        {
            return false;
        }
        return ANCIENT_GUTHIXIAN_TEMPLE_REGIONS.contains(local.getWorldLocation().getRegionID());
    }

    public boolean isInActiveFight()
    {
        return hasAnyEngagedDemon();
    }

    public boolean hasAnyEngagedDemon()
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        for (TormentedDemonState state : tormentedDemons.values())
        {
            if (isDemonEngagedWithPlayer(state, local))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isDemonEngagedWithPlayer(TormentedDemonState state, Player local)
    {
        if (state == null || state.getNpc() == null || state.getNpc().isDead())
        {
            return false;
        }
        NPC npc = state.getNpc();
        if (state.isInCombatWithPlayer())
        {
            return true;
        }
        if (local != null)
        {
            return npc.getInteracting() == local || local.getInteracting() == npc;
        }
        return false;
    }

    public TormentedDemonState getPrimaryDemonState()
    {
        if (primaryDemon != null && tormentedDemons.containsKey(primaryDemon))
        {
            TormentedDemonState state = tormentedDemons.get(primaryDemon);
            if (state != null && state.getNpc() != null && !state.getNpc().isDead())
            {
                return state;
            }
        }
        Player local = client != null ? client.getLocalPlayer() : null;
        for (TormentedDemonState state : tormentedDemons.values())
        {
            if (isDemonEngagedWithPlayer(state, local))
            {
                return state;
            }
        }
        return null;
    }

    public Prayer getRequiredOverheadPrayer()
    {
        TormentedDemonState primary = getPrimaryDemonState();
        if (primary == null || !isDemonEngagedWithPlayer(primary, client != null ? client.getLocalPlayer() : null))
        {
            return null;
        }
        return primary.getRequiredPrayer();
    }

    public boolean isPlayerInFireBombDanger()
    {
        if (client == null || client.getLocalPlayer() == null || fireBombDangerTiles.isEmpty())
        {
            return false;
        }
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        if (playerWp == null)
        {
            return false;
        }
        for (WorldPoint danger : fireBombDangerTiles.keySet())
        {
            if (danger.distanceTo(playerWp) <= 1)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isInMultiCombatWarningRoom()
    {
        return config != null && config.warnMultiCombatRooms() && tdMultiwayLevel >= 2;
    }

    public boolean isBoxedInNearWallOrCorner()
    {
        return config != null && config.warnWallCornerBoxIn() && isInActiveFight() && walkableAdjacentTileCount <= 4;
    }

    public boolean isLowHpInCombat()
    {
        if (client == null || config == null || !isInActiveFight())
        {
            return false;
        }
        return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.lowHpThreshold();
    }

    public boolean isLowPrayerInCombat()
    {
        if (client == null || config == null || !isInActiveFight())
        {
            return false;
        }
        return client.getBoostedSkillLevel(Skill.PRAYER) <= config.lowPrayerThreshold();
    }

    public boolean isJackpotBannerActive()
    {
        return jackpotDropName != null && (System.currentTimeMillis() - jackpotDropTimestampMs) <= JACKPOT_BANNER_DURATION_MS;
    }

    public List<WorldPoint> computeSafeFireBombEscapeTiles()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return Collections.emptyList();
        }
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        if (playerWp == null)
        {
            return Collections.emptyList();
        }

        TormentedDemonState primary = getPrimaryDemonState();
        WorldPoint demonWp = primary != null && primary.getNpc() != null ? primary.getNpc().getWorldLocation() : null;

        List<WorldPoint> candidates = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++)
        {
            for (int dy = -3; dy <= 3; dy++)
            {
                int cheb = Math.max(Math.abs(dx), Math.abs(dy));
                if (cheb < 2)
                {
                    continue;
                }
                WorldPoint wp = new WorldPoint(playerWp.getX() + dx, playerWp.getY() + dy, playerWp.getPlane());
                if (fireBombDangerTiles.containsKey(wp) || !isTileWalkable(wp))
                {
                    continue;
                }
                candidates.add(wp);
            }
        }

        if (candidates.isEmpty())
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    if (dx == 0 && dy == 0)
                    {
                        continue;
                    }
                    WorldPoint wp = new WorldPoint(playerWp.getX() + dx, playerWp.getY() + dy, playerWp.getPlane());
                    if (!fireBombDangerTiles.containsKey(wp) && isTileWalkable(wp))
                    {
                        candidates.add(wp);
                    }
                }
            }
        }

        candidates.sort((a, b) -> {
            int openA = countWalkableNeighbors(a) >= 5 ? 1 : 0;
            int openB = countWalkableNeighbors(b) >= 5 ? 1 : 0;
            if (openA != openB)
            {
                return Integer.compare(openB, openA);
            }
            int distCmp = Integer.compare(a.distanceTo(playerWp), b.distanceTo(playerWp));
            if (distCmp != 0)
            {
                return distCmp;
            }
            if (demonWp != null)
            {
                // Slightly prefer stepping away from demon so its melee approach is immediately obvious
                return Integer.compare(b.distanceTo(demonWp), a.distanceTo(demonWp));
            }
            return 0;
        });

        return candidates.size() > 4 ? candidates.subList(0, 4) : candidates;
    }

    // =========================================================================
    // Pure Decision Helpers (Unit Tested)
    // =========================================================================

    public static Prayer resolvePrayerFromAnimation(int animationId)
    {
        switch (animationId)
        {
            case AnimationID.LUC2_UNDEAD_DEMON_FIREY_BALLS:
                return Prayer.PROTECT_FROM_MAGIC;
            case AnimationID.LUC2_UNDEAD_DEMON_SPARE_RIBS:
                return Prayer.PROTECT_FROM_MISSILES;
            case AnimationID.LUC2_UNDEAD_DEMON_MELEE:
                return Prayer.PROTECT_FROM_MELEE;
            default:
                return null;
        }
    }

    public static Prayer resolvePrayerFromProjectile(int projectileId)
    {
        switch (projectileId)
        {
            case SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_PROJ:
                return Prayer.PROTECT_FROM_MAGIC;
            case SpotanimID.LUC2_RIB_BONE_SHARD_PROJECTILE:
                return Prayer.PROTECT_FROM_MISSILES;
            default:
                return null;
        }
    }

    /**
     * Predicts the Tormented Demon's next attack style immediately after a Fire Bomb special.
     * - If the demon begins walking toward the player, it switched to Melee.
     * - If it remains stationary for 2+ ticks and previously used Magic/Ranged, it swapped to the other ranged/magic style.
     * - If it previously used Melee and remains stationary for 2+ ticks, guesses Ranged (50/50 non-Melee, refined on attack).
     */
    public static Prayer predictPostFireBombPrayer(
            Prayer previousStyle,
            boolean movedTowardPlayer,
            boolean stationaryConfirmed)
    {
        if (movedTowardPlayer)
        {
            return Prayer.PROTECT_FROM_MELEE;
        }
        if (!stationaryConfirmed || previousStyle == null)
        {
            return null;
        }
        switch (previousStyle)
        {
            case PROTECT_FROM_MAGIC:
                return Prayer.PROTECT_FROM_MISSILES;
            case PROTECT_FROM_MISSILES:
                return Prayer.PROTECT_FROM_MAGIC;
            case PROTECT_FROM_MELEE:
                return Prayer.PROTECT_FROM_MISSILES;
            default:
                return null;
        }
    }

    public static Rs2PrayerEnum toRs2PrayerEnum(Prayer prayer)
    {
        if (prayer == null)
        {
            return null;
        }
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                return Rs2PrayerEnum.PROTECT_MAGIC;
            case PROTECT_FROM_MISSILES:
                return Rs2PrayerEnum.PROTECT_RANGE;
            case PROTECT_FROM_MELEE:
                return Rs2PrayerEnum.PROTECT_MELEE;
            default:
                return null;
        }
    }

    public static boolean shouldAttemptThrallCast(
            boolean inActiveFight,
            boolean hasLivingEngagedDemon,
            boolean thrallActiveOrCooldown,
            int spellbookVarbit,
            boolean hasBookOfTheDead,
            int currentTick,
            int lastCastTick)
    {
        if (!inActiveFight || !hasLivingEngagedDemon || thrallActiveOrCooldown)
        {
            return false;
        }
        if (spellbookVarbit != Rs2Spellbook.ARCEUUS.getValue() || !hasBookOfTheDead)
        {
            return false;
        }
        return currentTick - lastCastTick >= THRALL_RECAST_DEBOUNCE_TICKS;
    }

    public static List<Rs2Thrall> getThrallsByPriority(ThrallType type)
    {
        if (type == null)
        {
            return Collections.emptyList();
        }
        switch (type)
        {
            case MAGIC:
                return Arrays.asList(Rs2Thrall.GREATER_GHOST, Rs2Thrall.SUPERIOR_GHOST, Rs2Thrall.LESSER_GHOST);
            case RANGED:
                return Arrays.asList(Rs2Thrall.GREATER_SKELETON, Rs2Thrall.SUPERIOR_SKELETON, Rs2Thrall.LESSER_SKELETON);
            case MELEE:
                return Arrays.asList(Rs2Thrall.GREATER_ZOMBIE, Rs2Thrall.SUPERIOR_ZOMBIE, Rs2Thrall.LESSER_ZOMBIE);
            default:
                return Collections.emptyList();
        }
    }

    public static int getThrallPrayerCost(Rs2Thrall thrall)
    {
        if (thrall == null)
        {
            return 0;
        }
        switch (thrall)
        {
            case GREATER_GHOST:
            case GREATER_SKELETON:
            case GREATER_ZOMBIE:
                return 6;
            case SUPERIOR_GHOST:
            case SUPERIOR_SKELETON:
            case SUPERIOR_ZOMBIE:
                return 4;
            case LESSER_GHOST:
            case LESSER_SKELETON:
            case LESSER_ZOMBIE:
                return 2;
            default:
                return 0;
        }
    }

    public static Rs2Thrall resolveBestThrall(
            ThrallType type,
            int boostedMagicLevel,
            int boostedPrayerPoints,
            Predicate<Rs2Thrall> runeCheck)
    {
        for (Rs2Thrall candidate : getThrallsByPriority(type))
        {
            if (boostedMagicLevel >= candidate.getRequiredLevel()
                    && boostedPrayerPoints >= getThrallPrayerCost(candidate)
                    && (runeCheck == null || runeCheck.test(candidate)))
            {
                return candidate;
            }
        }
        return null;
    }

    public static int thrallToComponentId(Rs2Thrall thrall)
    {
        if (thrall == null)
        {
            return -1;
        }
        switch (thrall)
        {
            case LESSER_GHOST:
                return InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST;
            case LESSER_SKELETON:
                return InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON;
            case LESSER_ZOMBIE:
                return InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE;
            case SUPERIOR_GHOST:
                return InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST;
            case SUPERIOR_SKELETON:
                return InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON;
            case SUPERIOR_ZOMBIE:
                return InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE;
            case GREATER_GHOST:
                return InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST;
            case GREATER_SKELETON:
                return InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON;
            case GREATER_ZOMBIE:
                return InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE;
            default:
                return -1;
        }
    }

    /**
     * Pure menu swap helper: promotes "Walk here" above a Tormented Demon's left-click entry
     * when Shift is held (or Ctrl is held during Fire Bomb danger).
     */
    public static boolean swapWalkHereOverTormentedDemon(Menu root)
    {
        if (root == null)
        {
            return false;
        }
        MenuEntry[] entries = root.getMenuEntries();
        if (entries == null || entries.length < 2)
        {
            return false;
        }

        int topIdx = entries.length - 1;
        MenuEntry topEntry = entries[topIdx];
        if (!isTormentedDemonEntry(topEntry) || topEntry.getType() == MenuAction.WALK)
        {
            return false;
        }

        int walkIdx = -1;
        for (int i = topIdx - 1; i >= 0; i--)
        {
            MenuEntry entry = entries[i];
            if (entry != null && entry.getType() == MenuAction.WALK)
            {
                walkIdx = i;
                break;
            }
        }
        if (walkIdx < 0)
        {
            return false;
        }

        MenuEntry walkEntry = entries[walkIdx];
        entries[walkIdx] = topEntry;
        entries[topIdx] = walkEntry;
        root.setMenuEntries(entries);
        return true;
    }

    private static boolean isTormentedDemonEntry(MenuEntry entry)
    {
        if (entry == null)
        {
            return false;
        }
        NPC npc = entry.getNpc();
        if (npc != null && TORMENTED_DEMON_IDS.contains(npc.getId()))
        {
            return true;
        }
        return isTormentedDemonMenuTarget(entry.getTarget());
    }

    public static boolean isTormentedDemonMenuTarget(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }
        String clean = Text.removeTags(target).toLowerCase(Locale.ROOT);
        return clean.contains("tormented demon");
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
            resetRuntimeState();
        }
        else if (state == GameState.LOADING)
        {
            fireBombDangerTiles.clear();
            groundDrops.clear();
            tormentedDemons.clear();
            primaryDemon = null;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        registerNpc(event.getNpc());
        selectPrimaryDemon();
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        registerNpc(event.getNpc());
        selectPrimaryDemon();
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        unregisterNpc(event.getNpc());
        selectPrimaryDemon();
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC))
        {
            return;
        }
        NPC npc = (NPC) actor;
        if (!TORMENTED_DEMON_IDS.contains(npc.getId()))
        {
            return;
        }

        int animId = npc.getAnimation();
        TormentedDemonState state = tormentedDemons.computeIfAbsent(npc, TormentedDemonState::new);

        if (animId == AnimationID.LUC2_UNDEAD_DEMON_DEATH)
        {
            unregisterNpc(npc);
            selectPrimaryDemon();
            return;
        }

        Player local = client != null ? client.getLocalPlayer() : null;
        boolean targetedAtPlayer = local != null && (npc.getInteracting() == local || local.getInteracting() == npc || state.isInCombatWithPlayer());

        if (animId == AnimationID.LUC2_UNDEAD_DEMON_EXPLOSION_FIRE)
        {
            state.onFireBombStarted(totalTicks, npc.getWorldLocation());
            if (local != null && targetedAtPlayer && local.getWorldLocation() != null)
            {
                fireBombDangerTiles.put(local.getWorldLocation(), FIRE_BOMB_DANGER_TICKS);
                triggerFireBombAlert();
            }
            if (targetedAtPlayer)
            {
                primaryDemon = npc;
            }
            return;
        }

        Prayer attackPrayer = resolvePrayerFromAnimation(animId);
        if (attackPrayer != null)
        {
            state.onConfirmedAttack(attackPrayer, totalTicks);
            if (targetedAtPlayer || primaryDemon == null)
            {
                primaryDemon = npc;
                postKillPrayerReminder = false;
            }
            handleAutoPrayers();
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        Projectile projectile = event.getProjectile();
        if (projectile == null)
        {
            return;
        }
        int projId = projectile.getId();

        if (projId == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_LARGE_PROJ)
        {
            LocalPoint targetLp = projectile.getTarget();
            if (targetLp != null && client != null)
            {
                WorldPoint wp = WorldPoint.fromLocal(client, targetLp);
                if (wp != null)
                {
                    fireBombDangerTiles.put(wp, FIRE_BOMB_DANGER_TICKS);
                    triggerFireBombAlert();
                }
            }
            return;
        }

        Prayer projPrayer = resolvePrayerFromProjectile(projId);
        if (projPrayer == null)
        {
            return;
        }

        Player local = client != null ? client.getLocalPlayer() : null;
        Actor source = projectile.getSourceActor();
        Actor target = projectile.getTargetActor();
        if (local != null && target != null && target != local)
        {
            return;
        }

        if (source instanceof NPC && tormentedDemons.containsKey(source))
        {
            NPC demon = (NPC) source;
            TormentedDemonState state = tormentedDemons.get(demon);
            state.onConfirmedAttack(projPrayer, totalTicks);
            primaryDemon = demon;
            postKillPrayerReminder = false;
            handleAutoPrayers();
        }
        else
        {
            TormentedDemonState primary = getPrimaryDemonState();
            if (primary != null)
            {
                primary.onConfirmedAttack(projPrayer, totalTicks);
                handleAutoPrayers();
            }
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC))
        {
            return;
        }
        NPC npc = (NPC) actor;
        TormentedDemonState state = tormentedDemons.get(npc);
        if (state == null)
        {
            return;
        }

        if (npc.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_SPOT))
        {
            state.onShieldDownSpotanim(totalTicks);
        }
        if (npc.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_RESTORE_SPOT)
                || npc.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_RESTORE_SPOT_BIG))
        {
            state.onShieldRestored();
        }
        if (npc.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_DEPLETE_SPOT))
        {
            state.onDefencelessDetected();
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        GraphicsObject gfx = event.getGraphicsObject();
        if (gfx == null || client == null)
        {
            return;
        }
        int id = gfx.getId();
        LocalPoint lp = gfx.getLocation();
        if (lp == null)
        {
            return;
        }
        WorldPoint wp = WorldPoint.fromLocal(client, lp);
        if (wp == null)
        {
            return;
        }

        if (id == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_IMPACT_SPOT
                || id == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_LARGE_IMPACT)
        {
            fireBombDangerTiles.put(wp, FIRE_BOMB_DANGER_TICKS);
            return;
        }

        boolean isShieldSpot = id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_SPOT;
        boolean isShieldRestore = id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_RESTORE_SPOT
                || id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_RESTORE_SPOT_BIG;
        boolean isShieldDeplete = id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_DEPLETE_SPOT;

        if (isShieldSpot || isShieldRestore || isShieldDeplete)
        {
            for (TormentedDemonState state : tormentedDemons.values())
            {
                WorldPoint demonWp = state.getNpc().getWorldLocation();
                if (demonWp != null && demonWp.distanceTo(wp) <= 3)
                {
                    if (isShieldSpot)
                    {
                        state.onShieldDownSpotanim(totalTicks);
                    }
                    else if (isShieldRestore)
                    {
                        state.onShieldRestored();
                    }
                    else
                    {
                        state.onDefencelessDetected();
                    }
                }
            }
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();
        if (!(actor instanceof NPC) || hitsplat == null)
        {
            return;
        }
        NPC npc = (NPC) actor;
        TormentedDemonState state = tormentedDemons.get(npc);
        if (state == null)
        {
            return;
        }
        if (hitsplat.getHitsplatType() == HitsplatID.HEAL)
        {
            return;
        }

        int amount = hitsplat.getAmount();
        if (amount > 0)
        {
            state.recordDamage(amount);
            if (!state.isInCombatWithPlayer())
            {
                state.onFirstHitEngaged(totalTicks);
                primaryDemon = npc;
                postKillPrayerReminder = false;
            }
        }

        if (hitsplat.isMine())
        {
            state.onPlayerHitConsumedShield(totalTicks);
            if (primaryDemon == null)
            {
                primaryDemon = npc;
                postKillPrayerReminder = false;
            }
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        if (event == null || event.getNpc() == null || !TORMENTED_DEMON_IDS.contains(event.getNpc().getId()))
        {
            return;
        }

        if (sessionStartTimeMs == 0L)
        {
            sessionStartTimeMs = System.currentTimeMillis();
        }
        sessionKills++;

        for (ItemStack stack : event.getItems())
        {
            if (stack == null)
            {
                continue;
            }
            int id = stack.getId();
            int qty = Math.max(1, stack.getQuantity());
            long price = itemManager != null ? Math.max(0L, (long) itemManager.getItemPrice(id)) : 0L;
            sessionLootGp += price * qty;

            if (id == ItemID.TORMENTED_SYNAPSE)
            {
                synapseCount += qty;
                triggerJackpotDrop("TORMENTED SYNAPSE!");
            }
            else if (id == ItemID.BONE_CLAW)
            {
                burningClawCount += qty;
                triggerJackpotDrop("BURNING CLAW!");
            }
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        if (!isInGuthixianTemple())
        {
            return;
        }
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (item == null || tile == null)
        {
            return;
        }
        int id = item.getId();
        if (!TormentedDemonGearClassifier.isValuableTempleDrop(id))
        {
            return;
        }

        WorldPoint wp = tile.getWorldLocation();
        if (wp == null)
        {
            return;
        }
        ItemComposition comp = client != null ? client.getItemDefinition(id) : null;
        String name = comp != null ? comp.getName() : ("Item " + id);
        long gePrice = itemManager != null ? (long) itemManager.getItemPrice(id) * Math.max(1, item.getQuantity()) : 0L;
        groundDrops.computeIfAbsent(wp, k -> new ArrayList<>())
                .add(new GroundDropInfo(id, name, Math.max(1, item.getQuantity()), gePrice, wp));

        if (id == ItemID.TORMENTED_SYNAPSE)
        {
            triggerJackpotDrop("TORMENTED SYNAPSE!");
        }
        else if (id == ItemID.BONE_CLAW)
        {
            triggerJackpotDrop("BURNING CLAW!");
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (item == null || tile == null || groundDrops.isEmpty())
        {
            return;
        }
        WorldPoint wp = tile.getWorldLocation();
        if (wp == null)
        {
            return;
        }
        List<GroundDropInfo> dropsAtTile = groundDrops.get(wp);
        if (dropsAtTile == null)
        {
            return;
        }
        dropsAtTile.removeIf(info -> info.getItemId() == item.getId());
        if (dropsAtTile.isEmpty())
        {
            groundDrops.remove(wp);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == net.runelite.api.gameval.InventoryID.INV
                || event.getContainerId() == net.runelite.api.gameval.InventoryID.WORN)
        {
            refreshPlayerContext();
        }
        bankInterface.onItemContainerChanged(event);
    }

    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort event)
    {
        if (config == null || !config.shiftClickWalkThrough() || client == null)
        {
            return;
        }
        if (client.isMenuOpen() || client.isWidgetSelected())
        {
            return;
        }
        boolean shiftHeld = client.isKeyPressed(KeyCode.KC_SHIFT);
        boolean ctrlHeldInFireDanger = client.isKeyPressed(KeyCode.KC_CONTROL) && isPlayerInFireBombDanger();
        if (!shiftHeld && !ctrlHeldInFireDanger)
        {
            return;
        }
        swapWalkHereOverTormentedDemon(client.getMenu());
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        totalTicks++;

        cleanupDeadOrDespawnedDemons();
        tickDownMap(fireBombDangerTiles);
        updateDemonsOnTick();
        selectPrimaryDemon();
        refreshPlayerContext();

        if (postKillPrayerReminder && client.getLocalPlayer() != null
                && client.getLocalPlayer().getOverheadIcon() == null)
        {
            postKillPrayerReminder = false;
        }

        handleAutoPrayers();
        handleAutoThrall();
        handleAlerts();
    }

    private void cleanupDeadOrDespawnedDemons()
    {
        if (client == null)
        {
            return;
        }
        List<NPC> presentNpcs = client.getNpcs();
        boolean engagedRemoved = false;

        Iterator<Map.Entry<NPC, TormentedDemonState>> it = tormentedDemons.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<NPC, TormentedDemonState> entry = it.next();
            NPC npc = entry.getKey();
            if (npc == null || !presentNpcs.contains(npc) || npc.isDead())
            {
                if (entry.getValue().isInCombatWithPlayer() || npc == primaryDemon)
                {
                    engagedRemoved = true;
                }
                if (npc == primaryDemon)
                {
                    primaryDemon = null;
                }
                it.remove();
            }
        }

        if (engagedRemoved && !hasAnyEngagedDemon())
        {
            onCombatEnded();
        }
    }

    private void updateDemonsOnTick()
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        WorldPoint playerWp = local != null ? local.getWorldLocation() : null;

        for (TormentedDemonState state : tormentedDemons.values())
        {
            NPC demon = state.getNpc();
            if (demon == null || demon.isDead())
            {
                continue;
            }

            // Detect first engagement even if hitsplat missed (e.g. player interacted or demon targeted player)
            if (!state.isInCombatWithPlayer() && local != null
                    && (demon.getInteracting() == local || local.getInteracting() == demon))
            {
                state.onFirstHitEngaged(totalTicks);
                postKillPrayerReminder = false;
            }

            state.onProtectedStylesChanged(TormentedDemonOverlayUtils.extractProtectedStyles(demon.getOverheadSpriteIds()));

            int ratio = demon.getHealthRatio();
            int scale = demon.getHealthScale();
            if (ratio >= 0 && scale > 0)
            {
                state.setLastHealthRatio(ratio);
                state.setEstimatedHp((int) Math.round(((double) ratio / scale) * TormentedDemonState.MAX_HP));
            }

            if (demon.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_DEPLETE_SPOT))
            {
                state.onDefencelessDetected();
            }
            if (demon.hasSpotAnim(SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_SPOT))
            {
                state.onShieldDownSpotanim(totalTicks);
            }

            state.onTick();

            // Post-Fire-Bomb movement prediction
            if (state.isAwaitingPostFireBombStyle() && playerWp != null)
            {
                WorldPoint demonWp = demon.getWorldLocation();
                WorldPoint anchor = state.getPostFireBombAnchorLocation();
                boolean movedTowardPlayer = false;
                if (demonWp != null && anchor != null)
                {
                    boolean steppedCloser = !demonWp.equals(anchor) && demonWp.distanceTo(playerWp) < anchor.distanceTo(playerWp);
                    boolean walkingAnim = demon.getPoseAnimation() == AnimationID.LUC2_UNDEAD_DEMON_WALK && demonWp.distanceTo(playerWp) > 2;
                    movedTowardPlayer = steppedCloser || walkingAnim;
                }
                boolean stationaryConfirmed = !movedTowardPlayer && state.getPostFireBombTicks() >= 2;
                Prayer predicted = predictPostFireBombPrayer(state.getStyleBeforeFireBomb(), movedTowardPlayer, stationaryConfirmed);
                if (predicted != null)
                {
                    state.setPredictedPrayer(predicted);
                }
            }
        }
    }

    private void selectPrimaryDemon()
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        WorldPoint playerWp = local != null ? local.getWorldLocation() : null;

        NPC best = null;
        int bestScore = Integer.MIN_VALUE;

        for (TormentedDemonState state : tormentedDemons.values())
        {
            NPC demon = state.getNpc();
            if (demon == null || demon.isDead() || !isDemonEngagedWithPlayer(state, local))
            {
                continue;
            }
            int score = 0;
            if (local != null && local.getInteracting() == demon)
            {
                score += 2000;
            }
            if (local != null && demon.getInteracting() == local)
            {
                score += 1000;
            }
            if (state.getRequiredPrayer() != null)
            {
                score += 200 + state.getLastAttackTick();
            }
            if (playerWp != null && demon.getWorldLocation() != null)
            {
                score -= demon.getWorldLocation().distanceTo(playerWp);
            }
            if (score > bestScore)
            {
                bestScore = score;
                best = demon;
            }
        }

        primaryDemon = best;
    }

    private void refreshPlayerContext()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        tdMultiwayLevel = client.getVarbitValue(VarbitID.TD_MULTIWAY_INDICATOR);
        specEnergyPercent = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;

        Player local = client.getLocalPlayer();
        if (local != null && local.getWorldLocation() != null)
        {
            walkableAdjacentTileCount = countWalkableNeighbors(local.getWorldLocation());
        }

        // Equipped weapon classification
        equippedWeaponStyle = null;
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip != null && equip.getItems() != null && equip.getItems().length > 3)
        {
            Item weapon = equip.getItems()[3];
            if (weapon != null && weapon.getId() > 0)
            {
                ItemComposition comp = client.getItemDefinition(weapon.getId());
                String name = comp != null ? comp.getName() : "";
                equippedWeaponStyle = TormentedDemonGearClassifier.classifyWeaponStyle(weapon.getId(), name);
            }
        }

        // Inventory ashes & Smouldering consumables
        int ashes = 0;
        boolean smouldering = false;
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null && inv.getItems() != null)
        {
            for (Item item : inv.getItems())
            {
                if (item == null || item.getId() <= 0)
                {
                    continue;
                }
                int id = item.getId();
                if (!cachedItemNames.containsKey(id))
                {
                    ItemComposition comp = client.getItemDefinition(id);
                    if (comp != null && comp.getName() != null)
                    {
                        cachedItemNames.put(id, comp.getName());
                    }
                }
                if (TormentedDemonGearClassifier.isAshesForOffering(id))
                {
                    ashes += Math.max(1, item.getQuantity());
                }
                if (TormentedDemonGearClassifier.isSmoulderingConsumable(id))
                {
                    smouldering = true;
                }
            }
        }
        ashesCount = ashes;
        hasSmoulderingConsumable = smouldering;
    }

    public String getCachedItemName(int itemId, String widgetName)
    {
        String cached = cachedItemNames.get(itemId);
        if (cached != null && !cached.isEmpty())
        {
            return cached;
        }
        return widgetName != null ? Text.removeTags(widgetName) : "";
    }

    private void handleAutoPrayers()
    {
        if (config == null || !config.autoSwitchPrayers() || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0 || !hasAnyEngagedDemon())
        {
            return;
        }

        Prayer requiredOverhead = getRequiredOverheadPrayer();
        Rs2PrayerEnum targetRs2Prayer = toRs2PrayerEnum(requiredOverhead);
        if (targetRs2Prayer != null
                && client.getRealSkillLevel(Skill.PRAYER) >= targetRs2Prayer.getLevel()
                && !client.isPrayerActive(requiredOverhead))
        {
            Rs2Prayer.toggle(targetRs2Prayer);
        }
    }

    private void handlePostKillPrayerDisable()
    {
        if (config == null || !config.autoDisablePrayersOnKill() || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        for (Rs2PrayerEnum prayer : Rs2PrayerEnum.values())
        {
            if (Rs2Prayer.isPrayerActive(prayer))
            {
                Rs2Prayer.toggle(prayer);
            }
        }
        postKillPrayerReminder = false;
    }

    private void handleAutoThrall()
    {
        if (config == null || !config.autoCastThrall() || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        TormentedDemonState primary = getPrimaryDemonState();
        boolean hasLivingEngagedDemon = primary != null && primary.getNpc() != null && !primary.getNpc().isDead();
        boolean thrallActiveOrCooldown = client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) == 1
                || client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) >= 1;
        int spellbookVarbit = client.getVarbitValue(VarbitID.SPELLBOOK);
        boolean hasBookOfTheDead = Rs2Inventory.hasItem(ItemID.BOOK_OF_THE_DEAD)
                || Rs2Equipment.isWearing(ItemID.BOOK_OF_THE_DEAD);

        if (!shouldAttemptThrallCast(
                isInActiveFight(),
                hasLivingEngagedDemon,
                thrallActiveOrCooldown,
                spellbookVarbit,
                hasBookOfTheDead,
                totalTicks,
                lastThrallCastTick))
        {
            return;
        }

        ThrallType desiredType = config.thrallStyle() != null
                ? config.thrallStyle().getThrallType()
                : ThrallType.MAGIC;

        Rs2Thrall targetThrall = resolveBestThrall(
                desiredType,
                client.getBoostedSkillLevel(Skill.MAGIC),
                client.getBoostedSkillLevel(Skill.PRAYER),
                Rs2Magic::hasRequiredRunes
        );
        if (targetThrall == null)
        {
            return;
        }

        int componentId = thrallToComponentId(targetThrall);
        if (componentId == -1)
        {
            return;
        }

        Rectangle bounds = Rs2UiHelper.getDefaultRectangle();
        Widget spellWidget = client.getWidget(componentId);
        if (spellWidget != null && spellWidget.getBounds() != null
                && spellWidget.getBounds().width > 0 && spellWidget.getBounds().height > 0)
        {
            bounds = spellWidget.getBounds();
        }

        Microbot.doInvoke(
                new NewMenuEntry()
                        .option("Cast")
                        .target("<col=00ff00>" + targetThrall.getName() + "</col>")
                        .identifier(1)
                        .opcode(MenuAction.CC_OP.getId())
                        .param0(-1)
                        .param1(componentId)
                        .itemId(-1),
                bounds
        );
        lastThrallCastTick = totalTicks;
    }

    private void handleAlerts()
    {
        if (config == null || !config.soundAlerts() || client == null)
        {
            return;
        }
        if ((isLowHpInCombat() || isLowPrayerInCombat())
                && totalTicks - lastLowResourceAlertTick >= ALERT_COOLDOWN_TICKS)
        {
            Rs2SoundAlert.triggerAlert("", config.soundVolume());
            lastLowResourceAlertTick = totalTicks;
        }
    }

    private void triggerFireBombAlert()
    {
        if (config != null && config.soundAlerts() && totalTicks - lastFireBombAlertTick >= 4)
        {
            Rs2SoundAlert.triggerAlert("", config.soundVolume());
            lastFireBombAlertTick = totalTicks;
        }
    }

    private void triggerJackpotDrop(String dropLabel)
    {
        long now = System.currentTimeMillis();
        if (jackpotDropName != null && jackpotDropName.equals(dropLabel) && (now - jackpotDropTimestampMs) < 3_000L)
        {
            return;
        }
        jackpotDropName = dropLabel;
        jackpotDropTimestampMs = now;
        if (config != null && config.jackpotDropAlert() && config.soundAlerts()
                && totalTicks - lastJackpotAlertTick >= ALERT_COOLDOWN_TICKS)
        {
            Rs2SoundAlert.triggerAlert("", config.soundVolume());
            lastJackpotAlertTick = totalTicks;
        }
    }

    public int getNpcSize(NPC npc, int defaultSize)
    {
        if (npc == null)
        {
            return defaultSize;
        }
        NPCComposition comp = npc.getTransformedComposition();
        return comp != null ? Math.max(1, comp.getSize()) : defaultSize;
    }

    private int countWalkableNeighbors(WorldPoint center)
    {
        if (center == null)
        {
            return 8;
        }
        int walkable = 0;
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dy = -1; dy <= 1; dy++)
            {
                if (dx == 0 && dy == 0)
                {
                    continue;
                }
                if (isTileWalkable(new WorldPoint(center.getX() + dx, center.getY() + dy, center.getPlane())))
                {
                    walkable++;
                }
            }
        }
        return walkable;
    }

    private boolean isTileWalkable(WorldPoint wp)
    {
        if (client == null || client.getTopLevelWorldView() == null || wp == null)
        {
            return false;
        }
        LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
        if (lp == null)
        {
            return false;
        }
        int sceneX = lp.getSceneX();
        int sceneY = lp.getSceneY();
        if (sceneX < 0 || sceneY < 0 || sceneX >= Constants.SCENE_SIZE || sceneY >= Constants.SCENE_SIZE)
        {
            return false;
        }
        if (client.getTopLevelWorldView().getCollisionMaps() == null)
        {
            return true;
        }
        int plane = client.getTopLevelWorldView().getPlane();
        int[][] flags = client.getTopLevelWorldView().getCollisionMaps()[plane].getFlags();
        return (flags[sceneX][sceneY] & BLOCKED_TILE_MASK) == 0;
    }

    private static void tickDownMap(Map<WorldPoint, Integer> map)
    {
        if (map.isEmpty())
        {
            return;
        }
        Iterator<Map.Entry<WorldPoint, Integer>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<WorldPoint, Integer> entry = it.next();
            int next = entry.getValue() - 1;
            if (next <= 0)
            {
                it.remove();
            }
            else
            {
                entry.setValue(next);
            }
        }
    }

    // =========================================================================
    // Bank & Grand Exchange Event Delegation
    // =========================================================================

    @Subscribe(priority = -1)
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        bankInterface.onScriptCallbackEvent(event);
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event)
    {
        bankInterface.onScriptPreFired(event);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        bankInterface.onScriptPostFired(event);
        geInterface.onScriptPostFired(event);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        bankInterface.handleClick(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        bankInterface.onWidgetLoaded(event);
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        bankInterface.onWidgetClosed(event);
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event)
    {
        geInterface.onGrandExchangeSearched(event);
    }
}
