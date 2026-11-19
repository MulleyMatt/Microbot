package net.runelite.client.plugins.microbot.wgs;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.HeadIcon;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsFlashOverlay;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsHudOverlay;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsSceneOverlay;
import net.runelite.client.plugins.microbot.wgs.overlays.WgsWidgetOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@PluginDescriptor(
        name = PluginDescriptor.Default + "WGS Boss Helper",
        description = "Interactive helper for all 3 While Guthix Sleeps boss fights: Surok Magis (Weaken/Bind/Telegrab+Alch/Elemental Surges & Teleorb), Balance Elemental (head colour auto-prayer, 4-attack counter & tribrid prayer reader), and Tormented Demons (auto-prayer, fire bomb dodge & shield drop window)",
        tags = {"user", "helper", "wgs", "while guthix sleeps", "surok", "balance elemental", "tormented demon", "boss", "pvm", "quest", "overlay", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class WgsPlugin extends Plugin
{
    public static final Set<Integer> SUROK_BOSS_IDS = ImmutableSet.of(
            NpcID.DARK_SQUALL_COMBAT,
            NpcID.WGS_SUROK,
            NpcID.WGS_SUROK_TRANSITION
    );

    public static final Set<Integer> SUROK_SURGE_ORB_IDS = ImmutableSet.of(
            NpcID.EARTH_SURGE_RAISED,
            NpcID.WATER_SURGE_RAISED,
            NpcID.FIRE_SURGE_RAISED,
            NpcID.WIND_SURGE_RAISED
    );

    public static final Set<Integer> BALANCE_ELEMENTAL_IDS = ImmutableSet.of(
            NpcID.WGS_BALANCE_ELEMENTAL,
            NpcID.WGS_BALANCE_ELEMENTAL_DYING
    );

    public static final Set<Integer> TORMENTED_DEMON_IDS = ImmutableSet.of(
            NpcID.TORMENTED_DEMON_1,
            NpcID.TORMENTED_DEMON_2,
            NpcID.TORMENTED_DEMON_QUEST_1,
            NpcID.TORMENTED_DEMON_QUEST_2
    );

    public static final int LOW_PRAYER_ALERT_COOLDOWN_TICKS = 8;
    public static final int MECHANIC_ALERT_COOLDOWN_TICKS = 5;
    public static final int TD_SHIELD_DOWN_WINDOW_TICKS = 6;
    public static final int TD_FIRE_BOMB_DURATION_TICKS = 5;

    /**
     * Per-demon runtime state for Tormented Demons (Boss 3).
     */
    @Getter
    public static class TormentedDemonState
    {
        private final NPC npc;
        private Prayer requiredPrayer;
        private Set<HeadIcon> protectedStyles = EnumSet.noneOf(HeadIcon.class);
        private int shieldDownTicks = 0;
        private int fireBombWindupTicks = 0;
        private int lastAttackTick = -10;
        private int lastHealthRatio = -1;

        public TormentedDemonState(NPC npc)
        {
            this.npc = npc;
        }
    }

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WgsConfig config;

    @Inject
    private WgsSupplyCalculator supplyCalculator;

    @Inject
    private WgsBankInterface bankInterface;

    @Inject
    private WgsGrandExchangeInterface geInterface;

    @Inject
    private WgsHudOverlay hudOverlay;

    @Inject
    private WgsPrayerHelperOverlay prayerHelperOverlay;

    @Inject
    private WgsWidgetOverlay widgetOverlay;

    @Inject
    private WgsSceneOverlay sceneOverlay;

    @Inject
    private WgsFlashOverlay flashOverlay;

    @Getter
    private int totalTicks = 0;

    @Getter
    private WgsBossStage activeStage = null;

    // =========================================================================
    // Boss 1: Surok Magis State
    // =========================================================================

    @Getter
    private NPC surokNpc;

    @Getter
    private NPC surokUnconsciousNpc;

    @Getter
    private final List<NPC> strongWarriors = new ArrayList<>();

    @Getter
    private final List<NPC> agileWarriors = new ArrayList<>();

    @Getter
    private final List<NPC> surgeOrbs = new ArrayList<>();

    @Getter
    private final Set<WorldPoint> groundBombTiles = new LinkedHashSet<>();

    @Getter
    private boolean bombInInventory = false;

    @Getter
    private int surokAttacksSinceSpecial = 0;

    private int lastSurokAttackTick = -10;
    private WgsSurokMechanic lastSurokAlertMechanic = WgsSurokMechanic.NONE;

    // =========================================================================
    // Boss 2: Balance Elemental State
    // =========================================================================

    @Getter
    private NPC balanceNpc;

    @Getter
    private WgsBalanceElementalStyle balanceStyle = WgsBalanceElementalStyle.BLUE_MAGIC;

    @Getter
    private int balanceAttacksInStyle = 0;

    @Getter
    private int balanceEstimatedHp = WgsBalanceElementalStyle.MAX_HP;

    @Getter
    private Set<HeadIcon> balanceProtectedStyles = EnumSet.noneOf(HeadIcon.class);

    private int lastBalanceAttackTick = -10;

    // =========================================================================
    // Boss 3: Tormented Demons State
    // =========================================================================

    @Getter
    private final Map<NPC, TormentedDemonState> tormentedDemons = new LinkedHashMap<>();

    @Getter
    private final Map<WorldPoint, Integer> fireBombDangerTiles = new HashMap<>();

    @Getter
    private NPC primaryTormentedDemon;

    @Getter
    private boolean postKillPrayerReminder = false;

    private int lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
    private int lastMechanicAlertTick = -MECHANIC_ALERT_COOLDOWN_TICKS;

    @Provides
    WgsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WgsConfig.class);
    }

    @Override
    protected void startUp()
    {
        reset();
        overlayManager.add(hudOverlay);
        overlayManager.add(prayerHelperOverlay);
        overlayManager.add(widgetOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(flashOverlay);

        clientThread.invokeLater(() -> {
            scanExistingEntities();
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
        overlayManager.remove(widgetOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(flashOverlay);

        clientThread.invokeLater(() -> {
            geInterface.destroy();
            bankInterface.destroy();
        });
        reset();
    }

    public void reset()
    {
        totalTicks = 0;
        activeStage = null;

        surokNpc = null;
        surokUnconsciousNpc = null;
        strongWarriors.clear();
        agileWarriors.clear();
        surgeOrbs.clear();
        groundBombTiles.clear();
        bombInInventory = false;
        surokAttacksSinceSpecial = 0;
        lastSurokAttackTick = -10;
        lastSurokAlertMechanic = WgsSurokMechanic.NONE;

        balanceNpc = null;
        balanceStyle = WgsBalanceElementalStyle.BLUE_MAGIC;
        balanceAttacksInStyle = 0;
        balanceEstimatedHp = WgsBalanceElementalStyle.MAX_HP;
        balanceProtectedStyles = EnumSet.noneOf(HeadIcon.class);
        lastBalanceAttackTick = -10;

        tormentedDemons.clear();
        fireBombDangerTiles.clear();
        primaryTormentedDemon = null;

        postKillPrayerReminder = false;
        lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
        lastMechanicAlertTick = -MECHANIC_ALERT_COOLDOWN_TICKS;
    }

    private void scanExistingEntities()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        for (NPC npc : client.getNpcs())
        {
            registerNpc(npc);
        }
        scanGroundBombsInScene();
        refreshInventoryBombState();
        updateActiveStage();
    }

    private void registerNpc(NPC npc)
    {
        if (npc == null)
        {
            return;
        }
        int id = npc.getId();
        if (SUROK_BOSS_IDS.contains(id))
        {
            surokNpc = npc;
            surokUnconsciousNpc = null;
            postKillPrayerReminder = false;
        }
        else if (id == NpcID.WGS_SUROK_UNCONSCIOUS)
        {
            surokNpc = null;
            surokUnconsciousNpc = npc;
        }
        else if (id == NpcID.DARK_SQUALL_STRONG_MINION)
        {
            if (!strongWarriors.contains(npc))
            {
                strongWarriors.add(npc);
                surokAttacksSinceSpecial = 0;
            }
        }
        else if (id == NpcID.DARK_SQUALL_AGILE_MINION)
        {
            if (!agileWarriors.contains(npc))
            {
                agileWarriors.add(npc);
                surokAttacksSinceSpecial = 0;
            }
        }
        else if (SUROK_SURGE_ORB_IDS.contains(id))
        {
            if (!surgeOrbs.contains(npc))
            {
                surgeOrbs.add(npc);
                surokAttacksSinceSpecial = 0;
            }
        }
        else if (BALANCE_ELEMENTAL_IDS.contains(id))
        {
            balanceNpc = npc;
            if (id == NpcID.WGS_BALANCE_ELEMENTAL_DYING)
            {
                onBossDefeated();
            }
            else
            {
                postKillPrayerReminder = false;
                updateBalanceStyleFromActor(npc);
            }
        }
        else if (TORMENTED_DEMON_IDS.contains(id))
        {
            tormentedDemons.computeIfAbsent(npc, TormentedDemonState::new);
            postKillPrayerReminder = false;
        }
    }

    private void unregisterNpc(NPC npc)
    {
        if (npc == null)
        {
            return;
        }
        int id = npc.getId();
        if (npc == surokNpc || SUROK_BOSS_IDS.contains(id))
        {
            surokNpc = null;
        }
        if (npc == surokUnconsciousNpc || id == NpcID.WGS_SUROK_UNCONSCIOUS)
        {
            surokUnconsciousNpc = null;
        }
        strongWarriors.remove(npc);
        agileWarriors.remove(npc);
        surgeOrbs.remove(npc);

        if (npc == balanceNpc || BALANCE_ELEMENTAL_IDS.contains(id))
        {
            boolean dyingOrDead = npc.isDead() || id == NpcID.WGS_BALANCE_ELEMENTAL_DYING;
            balanceNpc = null;
            balanceProtectedStyles = EnumSet.noneOf(HeadIcon.class);
            if (dyingOrDead)
            {
                onBossDefeated();
            }
        }

        if (tormentedDemons.remove(npc) != null)
        {
            if (npc == primaryTormentedDemon)
            {
                primaryTormentedDemon = null;
            }
            if (tormentedDemons.isEmpty())
            {
                onBossDefeated();
            }
        }
    }

    private void onBossDefeated()
    {
        postKillPrayerReminder = true;
        handlePostKillPrayerDisable();
    }

    private void updateActiveStage()
    {
        if (surokNpc != null || surokUnconsciousNpc != null
                || !strongWarriors.isEmpty() || !agileWarriors.isEmpty()
                || !surgeOrbs.isEmpty() || !groundBombTiles.isEmpty() || bombInInventory)
        {
            activeStage = WgsBossStage.SUROK_MAGIS;
            return;
        }
        if (balanceNpc != null)
        {
            activeStage = WgsBossStage.BALANCE_ELEMENTAL;
            return;
        }
        if (!tormentedDemons.isEmpty())
        {
            activeStage = WgsBossStage.TORMENTED_DEMONS;
            return;
        }
        activeStage = null;
    }

    // =========================================================================
    // Public Query API for Overlays & Tests
    // =========================================================================

    public boolean isInAnyBossFight()
    {
        return activeStage != null;
    }

    public boolean hasTeleorbInInventory()
    {
        if (client == null)
        {
            return false;
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv == null)
        {
            return false;
        }
        for (int id : WgsSupplyCalculator.TELEORB_VARIANT_IDS)
        {
            if (WgsSupplyCalculator.countItemInContainer(inv, id) > 0)
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasDarkSquallRobesEquippedOrInInventory()
    {
        if (client == null)
        {
            return false;
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        for (int id : WgsSupplyCalculator.DARK_SQUALL_ROBE_IDS)
        {
            int count = (inv != null ? WgsSupplyCalculator.countItemInContainer(inv, id) : 0)
                    + (equip != null ? WgsSupplyCalculator.countItemInContainer(equip, id) : 0);
            if (count <= 0)
            {
                return false;
            }
        }
        return true;
    }

    public boolean isStoneOfJasBoostActive()
    {
        return client != null && client.getVarbitValue(VarbitID.WGS_STONE_OF_JAS_BOOST) > 0;
    }

    /**
     * Resolves the current highest-priority Surok Magis mechanic.
     */
    public WgsSurokMechanic getActiveSurokMechanic()
    {
        NPC nearestSurge = getNearestSurgeOrb();
        WgsSurokMechanic surgeMechanic = nearestSurge != null
                ? WgsSurokMechanic.fromSurgeNpcId(nearestSurge.getId())
                : null;

        boolean hasStrong = hasLivingNpc(strongWarriors);
        boolean hasAgile = hasLivingNpc(agileWarriors);
        boolean unconscious = surokUnconsciousNpc != null && surokNpc == null;

        return WgsSurokMechanic.resolveHighestPriority(
                bombInInventory,
                !groundBombTiles.isEmpty(),
                surgeMechanic,
                hasStrong,
                hasAgile,
                unconscious
        );
    }

    public NPC getNearestSurgeOrb()
    {
        if (surgeOrbs.isEmpty())
        {
            return null;
        }
        Player local = client != null ? client.getLocalPlayer() : null;
        WorldPoint playerLoc = local != null ? local.getWorldLocation() : null;
        NPC best = null;
        int bestDist = Integer.MAX_VALUE;
        for (NPC orb : surgeOrbs)
        {
            if (orb == null || orb.isDead())
            {
                continue;
            }
            if (playerLoc == null || orb.getWorldLocation() == null)
            {
                return orb;
            }
            int dist = orb.getWorldLocation().distanceTo(playerLoc);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = orb;
            }
        }
        return best;
    }

    public int getSurokEstimatedHp()
    {
        if (surokNpc == null)
        {
            return 0;
        }
        int ratio = surokNpc.getHealthRatio();
        int scale = surokNpc.getHealthScale();
        if (ratio < 0 || scale <= 0)
        {
            return 450;
        }
        return (int) Math.round(((double) ratio / scale) * 450.0);
    }

    public WgsBalanceElementalStyle.HpPhase getBalanceHpPhase()
    {
        return WgsBalanceElementalStyle.resolveHpPhase(balanceEstimatedHp);
    }

    public int getBalanceRemainingInRotation()
    {
        return Math.max(0, WgsBalanceElementalStyle.ATTACKS_PER_ROTATION - balanceAttacksInStyle);
    }

    public boolean isPlayerInFireBombDanger()
    {
        if (client == null || client.getLocalPlayer() == null || fireBombDangerTiles.isEmpty())
        {
            return false;
        }
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        return wp != null && fireBombDangerTiles.containsKey(wp);
    }

    public List<WorldPoint> computeSafeFireBombEscapeTiles()
    {
        if (client == null || client.getLocalPlayer() == null || fireBombDangerTiles.isEmpty())
        {
            return Collections.emptyList();
        }
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        if (playerWp == null)
        {
            return Collections.emptyList();
        }

        List<WorldPoint> candidates = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++)
        {
            for (int dy = -2; dy <= 2; dy++)
            {
                if (dx == 0 && dy == 0)
                {
                    continue;
                }
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                if (dist > 2)
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
        candidates.sort((a, b) -> Integer.compare(a.distanceTo(playerWp), b.distanceTo(playerWp)));
        return candidates.size() > 4 ? candidates.subList(0, 4) : candidates;
    }

    public TormentedDemonState getPrimaryTormentedDemonState()
    {
        if (primaryTormentedDemon != null && tormentedDemons.containsKey(primaryTormentedDemon))
        {
            return tormentedDemons.get(primaryTormentedDemon);
        }
        for (TormentedDemonState state : tormentedDemons.values())
        {
            if (state != null && state.getNpc() != null && !state.getNpc().isDead())
            {
                return state;
            }
        }
        return null;
    }

    /**
     * Returns the overhead prayer the player should currently activate for the active boss encounter.
     */
    public Prayer getRequiredOverheadPrayer()
    {
        if (activeStage == WgsBossStage.SUROK_MAGIS)
        {
            if (surokNpc != null && !surokNpc.isDead())
            {
                return Prayer.PROTECT_FROM_MAGIC;
            }
            return null;
        }
        if (activeStage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            if (balanceNpc != null && !balanceNpc.isDead() && balanceStyle != null)
            {
                return balanceStyle.getProtectionPrayer();
            }
            return null;
        }
        if (activeStage == WgsBossStage.TORMENTED_DEMONS)
        {
            TormentedDemonState primary = getPrimaryTormentedDemonState();
            if (primary != null && primary.getNpc() != null && !primary.getNpc().isDead())
            {
                return primary.getRequiredPrayer();
            }
            return null;
        }
        return null;
    }

    /**
     * Pure helper: maps Tormented Demon attack animation IDs to the required protection prayer.
     */
    public static Prayer resolveTormentedDemonPrayerFromAnimation(int animationId)
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

    /**
     * Pure helper: maps Tormented Demon projectile IDs to the required protection prayer.
     */
    public static Prayer resolveTormentedDemonPrayerFromProjectile(int projectileId)
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
     * Pure helper: resolves offensive prayer style from config, boss stage, weapon name, and boss overheads.
     */
    public static WgsConfig.OffensivePrayerStyle resolveAutoOffensiveStyle(
            WgsConfig.OffensivePrayerStyle configured,
            WgsBossStage stage,
            String equippedWeaponName,
            Set<HeadIcon> protectedStyles)
    {
        if (configured != null && configured != WgsConfig.OffensivePrayerStyle.AUTO)
        {
            return configured;
        }
        if (stage == WgsBossStage.SUROK_MAGIS)
        {
            return WgsConfig.OffensivePrayerStyle.MAGIC;
        }

        if (equippedWeaponName != null && !equippedWeaponName.isEmpty())
        {
            String lower = equippedWeaponName.toLowerCase(Locale.ROOT);
            if (lower.contains("staff") || lower.contains("wand") || lower.contains("trident")
                    || lower.contains("shadow") || lower.contains("sceptre") || lower.contains("rod"))
            {
                return WgsConfig.OffensivePrayerStyle.MAGIC;
            }
            if (lower.contains("bow") || lower.contains("crossbow") || lower.contains("blowpipe")
                    || lower.contains("ballista") || lower.contains("dart") || lower.contains("chinchompa")
                    || lower.contains("atlatl"))
            {
                return WgsConfig.OffensivePrayerStyle.RANGED;
            }
            return WgsConfig.OffensivePrayerStyle.MELEE;
        }

        if (protectedStyles != null && !protectedStyles.isEmpty())
        {
            if (!protectedStyles.contains(HeadIcon.MELEE))
            {
                return WgsConfig.OffensivePrayerStyle.MELEE;
            }
            if (!protectedStyles.contains(HeadIcon.RANGED))
            {
                return WgsConfig.OffensivePrayerStyle.RANGED;
            }
            if (!protectedStyles.contains(HeadIcon.MAGIC))
            {
                return WgsConfig.OffensivePrayerStyle.MAGIC;
            }
        }
        return WgsConfig.OffensivePrayerStyle.MELEE;
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
            groundBombTiles.clear();
            fireBombDangerTiles.clear();
            strongWarriors.clear();
            agileWarriors.clear();
            surgeOrbs.clear();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        registerNpc(event.getNpc());
        updateActiveStage();
        handleAutoPrayers();
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        NPC npc = event.getNpc();
        if (npc == null)
        {
            return;
        }
        int newId = npc.getId();
        if (newId == NpcID.WGS_SUROK_UNCONSCIOUS)
        {
            surokNpc = null;
            surokUnconsciousNpc = npc;
            strongWarriors.clear();
            agileWarriors.clear();
            surgeOrbs.clear();
            groundBombTiles.clear();
            onBossDefeated();
        }
        else if (newId == NpcID.WGS_BALANCE_ELEMENTAL_DYING)
        {
            balanceNpc = null;
            onBossDefeated();
        }
        else
        {
            registerNpc(npc);
        }
        updateActiveStage();
        handleAutoPrayers();
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        unregisterNpc(event.getNpc());
        updateActiveStage();
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
        int id = npc.getId();
        int animId = npc.getAnimation();

        // Boss 1: Surok Magis attack count
        if (npc == surokNpc || SUROK_BOSS_IDS.contains(id))
        {
            if (surokNpc == null)
            {
                surokNpc = npc;
            }
            if ((animId == AnimationID.SUROK_SUROK_SPELL_CASTING || animId == AnimationID.SUROK_SUROK_SPELL_CASTING_SPOT)
                    && totalTicks != lastSurokAttackTick)
            {
                lastSurokAttackTick = totalTicks;
                surokAttacksSinceSpecial++;
            }
            updateActiveStage();
            return;
        }

        // Boss 2: Balance Elemental head colour / attack counter
        if (npc == balanceNpc || BALANCE_ELEMENTAL_IDS.contains(id))
        {
            if (id == NpcID.WGS_BALANCE_ELEMENTAL_DYING)
            {
                balanceNpc = null;
                onBossDefeated();
                updateActiveStage();
                return;
            }
            if (balanceNpc == null)
            {
                balanceNpc = npc;
            }

            WgsBalanceElementalStyle detected = WgsBalanceElementalStyle.fromAnimationId(animId);
            if (detected != null && detected != balanceStyle)
            {
                balanceStyle = detected;
                balanceAttacksInStyle = 0;
            }
            if (WgsBalanceElementalStyle.isAttackAnimation(animId) && totalTicks - lastBalanceAttackTick >= 2)
            {
                lastBalanceAttackTick = totalTicks;
                balanceAttacksInStyle = Math.min(WgsBalanceElementalStyle.ATTACKS_PER_ROTATION, balanceAttacksInStyle + 1);
            }
            updateActiveStage();
            handleAutoPrayers();
            return;
        }

        // Boss 3: Tormented Demons
        if (TORMENTED_DEMON_IDS.contains(id))
        {
            TormentedDemonState state = tormentedDemons.computeIfAbsent(npc, TormentedDemonState::new);
            if (animId == AnimationID.LUC2_UNDEAD_DEMON_DEATH)
            {
                tormentedDemons.remove(npc);
                if (npc == primaryTormentedDemon)
                {
                    primaryTormentedDemon = null;
                }
                if (tormentedDemons.isEmpty())
                {
                    onBossDefeated();
                }
                updateActiveStage();
                return;
            }

            if (animId == AnimationID.LUC2_UNDEAD_DEMON_EXPLOSION_FIRE)
            {
                state.fireBombWindupTicks = 3;
                state.shieldDownTicks = TD_SHIELD_DOWN_WINDOW_TICKS;
            }

            Prayer attackPrayer = resolveTormentedDemonPrayerFromAnimation(animId);
            if (attackPrayer != null)
            {
                state.requiredPrayer = attackPrayer;
                state.lastAttackTick = totalTicks;
                if (primaryTormentedDemon == null || npc.getInteracting() == client.getLocalPlayer())
                {
                    primaryTormentedDemon = npc;
                }
            }
            updateActiveStage();
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

        // Balance Elemental projectiles as secondary verification
        WgsBalanceElementalStyle balanceProjStyle = WgsBalanceElementalStyle.fromSpotanimOrProjectileId(projId);
        if (balanceProjStyle != null && balanceNpc != null)
        {
            if (balanceProjStyle != balanceStyle)
            {
                balanceStyle = balanceProjStyle;
                balanceAttacksInStyle = 0;
            }
            if (totalTicks - lastBalanceAttackTick >= 2)
            {
                lastBalanceAttackTick = totalTicks;
                balanceAttacksInStyle = Math.min(WgsBalanceElementalStyle.ATTACKS_PER_ROTATION, balanceAttacksInStyle + 1);
            }
            handleAutoPrayers();
            return;
        }

        // Tormented Demon projectiles & fire bomb targets
        if (projId == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_LARGE_PROJ)
        {
            LocalPoint targetLp = projectile.getTarget();
            if (targetLp != null && client != null)
            {
                WorldPoint targetWp = WorldPoint.fromLocal(client, targetLp);
                if (targetWp != null)
                {
                    fireBombDangerTiles.put(targetWp, TD_FIRE_BOMB_DURATION_TICKS);
                }
            }
            return;
        }

        Prayer tdPrayer = resolveTormentedDemonPrayerFromProjectile(projId);
        if (tdPrayer != null && !tormentedDemons.isEmpty())
        {
            TormentedDemonState primary = getPrimaryTormentedDemonState();
            if (primary != null && primary.requiredPrayer == null)
            {
                primary.requiredPrayer = tdPrayer;
                handleAutoPrayers();
            }
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        GraphicsObject gfx = event.getGraphicsObject();
        if (gfx == null)
        {
            return;
        }
        int id = gfx.getId();

        // Balance Elemental spotanims
        WgsBalanceElementalStyle balanceGfxStyle = WgsBalanceElementalStyle.fromSpotanimOrProjectileId(id);
        if (balanceGfxStyle != null && balanceNpc != null && balanceGfxStyle != balanceStyle)
        {
            balanceStyle = balanceGfxStyle;
            balanceAttacksInStyle = 0;
            handleAutoPrayers();
        }

        // Tormented Demon fire bomb ground impact spotanims
        if (id == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_IMPACT_SPOT
                || id == SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_LARGE_IMPACT)
        {
            LocalPoint lp = gfx.getLocation();
            if (lp != null && client != null)
            {
                WorldPoint wp = WorldPoint.fromLocal(client, lp);
                if (wp != null)
                {
                    fireBombDangerTiles.put(wp, TD_FIRE_BOMB_DURATION_TICKS);
                }
            }
        }

        // Tormented Demon shield drop / restore spotanims
        if (id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_SPOT || id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_RESTORE_SPOT)
        {
            LocalPoint lp = gfx.getLocation();
            if (lp != null && client != null)
            {
                WorldPoint wp = WorldPoint.fromLocal(client, lp);
                for (TormentedDemonState state : tormentedDemons.values())
                {
                    WorldPoint npcWp = state.getNpc().getWorldLocation();
                    if (wp != null && npcWp != null && npcWp.distanceTo(wp) <= 3)
                    {
                        state.shieldDownTicks = id == SpotanimID.LUC2_UNDEAD_DEMON_SHIELD_SPOT
                                ? TD_SHIELD_DOWN_WINDOW_TICKS
                                : 0;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (item == null || tile == null)
        {
            return;
        }
        if (item.getId() == ItemID.WGS_BOMB)
        {
            WorldPoint wp = tile.getWorldLocation();
            if (wp != null)
            {
                groundBombTiles.add(wp);
                surokAttacksSinceSpecial = 0;
                updateActiveStage();
            }
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (item == null || tile == null)
        {
            return;
        }
        if (item.getId() == ItemID.WGS_BOMB)
        {
            WorldPoint wp = tile.getWorldLocation();
            if (wp != null)
            {
                groundBombTiles.remove(wp);
                updateActiveStage();
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == net.runelite.api.gameval.InventoryID.INV)
        {
            refreshInventoryBombState();
            updateActiveStage();
        }
        bankInterface.onItemContainerChanged(event);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        totalTicks++;

        cleanupMissingOrDeadNpcs();
        scanGroundBombsInScene();
        refreshInventoryBombState();

        // Update Balance Elemental head colour (pose/anim), HP estimate, and overhead prayers
        if (balanceNpc != null)
        {
            updateBalanceStyleFromActor(balanceNpc);
            int ratio = balanceNpc.getHealthRatio();
            int scale = balanceNpc.getHealthScale();
            if (ratio >= 0 && scale > 0)
            {
                balanceEstimatedHp = (int) Math.round(((double) ratio / scale) * WgsBalanceElementalStyle.MAX_HP);
            }
            balanceProtectedStyles = WgsOverlayUtils.extractProtectedStyles(balanceNpc.getOverheadSpriteIds());
        }

        // Update Tormented Demons overhead prayers, first-hit shield drops, and timers
        updateTormentedDemonStatesOnTick();

        // Decay Fire Bomb danger tiles
        tickDownMap(fireBombDangerTiles);

        updateActiveStage();

        // Clear post-kill prayer reminder once player turns off overheads
        if (postKillPrayerReminder && client.getLocalPlayer() != null
                && client.getLocalPlayer().getOverheadIcon() == null)
        {
            postKillPrayerReminder = false;
        }

        handleAutoPrayers();
        handleEmergencySoundAlerts();
    }

    private void cleanupMissingOrDeadNpcs()
    {
        if (client == null)
        {
            return;
        }
        Set<NPC> presentNpcs = new HashSet<>(client.getNpcs());

        if (surokNpc != null && (!presentNpcs.contains(surokNpc) || surokNpc.isDead()))
        {
            surokNpc = null;
        }
        if (surokUnconsciousNpc != null && !presentNpcs.contains(surokUnconsciousNpc))
        {
            surokUnconsciousNpc = null;
        }
        strongWarriors.removeIf(npc -> npc == null || !presentNpcs.contains(npc) || npc.isDead());
        agileWarriors.removeIf(npc -> npc == null || !presentNpcs.contains(npc) || npc.isDead());
        surgeOrbs.removeIf(npc -> npc == null || !presentNpcs.contains(npc) || npc.isDead());

        if (balanceNpc != null && (!presentNpcs.contains(balanceNpc) || balanceNpc.isDead()))
        {
            boolean wasDead = balanceNpc.isDead();
            balanceNpc = null;
            balanceProtectedStyles = EnumSet.noneOf(HeadIcon.class);
            if (wasDead)
            {
                onBossDefeated();
            }
        }

        Iterator<Map.Entry<NPC, TormentedDemonState>> it = tormentedDemons.entrySet().iterator();
        while (it.hasNext())
        {
            NPC npc = it.next().getKey();
            if (npc == null || !presentNpcs.contains(npc) || npc.isDead())
            {
                it.remove();
                if (npc == primaryTormentedDemon)
                {
                    primaryTormentedDemon = null;
                }
            }
        }
        if (tormentedDemons.isEmpty() && activeStage == WgsBossStage.TORMENTED_DEMONS)
        {
            onBossDefeated();
        }
    }

    private void updateBalanceStyleFromActor(NPC npc)
    {
        if (npc == null)
        {
            return;
        }
        WgsBalanceElementalStyle style = WgsBalanceElementalStyle.fromAnimationId(npc.getAnimation());
        if (style == null)
        {
            style = WgsBalanceElementalStyle.fromAnimationId(npc.getPoseAnimation());
        }
        if (style != null && style != balanceStyle)
        {
            balanceStyle = style;
            balanceAttacksInStyle = 0;
        }
    }

    private void updateTormentedDemonStatesOnTick()
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        NPC bestPrimary = null;
        int bestScore = Integer.MIN_VALUE;

        for (TormentedDemonState state : tormentedDemons.values())
        {
            NPC demon = state.getNpc();
            if (demon == null || demon.isDead())
            {
                continue;
            }
            state.protectedStyles = WgsOverlayUtils.extractProtectedStyles(demon.getOverheadSpriteIds());

            // Detect first damage hit (health ratio appears) -> fire shield drops briefly
            int ratio = demon.getHealthRatio();
            if (state.lastHealthRatio == -1 && ratio >= 0)
            {
                state.shieldDownTicks = TD_SHIELD_DOWN_WINDOW_TICKS;
            }
            if (ratio >= 0)
            {
                state.lastHealthRatio = ratio;
            }

            if (state.shieldDownTicks > 0)
            {
                state.shieldDownTicks--;
            }
            if (state.fireBombWindupTicks > 0)
            {
                state.fireBombWindupTicks--;
            }

            int score = 0;
            if (local != null && demon.getInteracting() == local)
            {
                score += 1000;
            }
            if (state.requiredPrayer != null)
            {
                score += 100 + state.lastAttackTick;
            }
            if (local != null && local.getWorldLocation() != null && demon.getWorldLocation() != null)
            {
                score -= demon.getWorldLocation().distanceTo(local.getWorldLocation());
            }
            if (score > bestScore)
            {
                bestScore = score;
                bestPrimary = demon;
            }
        }

        if (bestPrimary != null)
        {
            primaryTormentedDemon = bestPrimary;
        }
    }

    private void scanGroundBombsInScene()
    {
        groundBombTiles.clear();
        if (client == null || client.getTopLevelWorldView() == null)
        {
            return;
        }
        Scene scene = client.getTopLevelWorldView().getScene();
        if (scene == null)
        {
            return;
        }
        Tile[][][] tiles = scene.getTiles();
        int plane = client.getTopLevelWorldView().getPlane();
        if (tiles == null || plane < 0 || plane >= tiles.length)
        {
            return;
        }
        for (int x = 0; x < Constants.SCENE_SIZE; x++)
        {
            for (int y = 0; y < Constants.SCENE_SIZE; y++)
            {
                Tile tile = tiles[plane][x][y];
                if (tile == null || tile.getGroundItems() == null)
                {
                    continue;
                }
                for (TileItem item : tile.getGroundItems())
                {
                    if (item != null && item.getId() == ItemID.WGS_BOMB)
                    {
                        WorldPoint wp = tile.getWorldLocation();
                        if (wp != null)
                        {
                            groundBombTiles.add(wp);
                        }
                    }
                }
            }
        }
    }

    private void refreshInventoryBombState()
    {
        if (client == null)
        {
            bombInInventory = false;
            return;
        }
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        bombInInventory = inv != null && WgsSupplyCalculator.countItemInContainer(inv, ItemID.WGS_BOMB) > 0;
    }

    private void handleAutoPrayers()
    {
        if (config == null || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (activeStage == null || client.getBoostedSkillLevel(Skill.PRAYER) <= 0)
        {
            return;
        }

        if (config.autoSwitchPrayers())
        {
            Prayer requiredOverhead = getRequiredOverheadPrayer();
            Rs2PrayerEnum targetRs2Prayer = toRs2PrayerEnum(requiredOverhead);
            if (targetRs2Prayer != null
                    && client.getRealSkillLevel(Skill.PRAYER) >= targetRs2Prayer.getLevel()
                    && !client.isPrayerActive(requiredOverhead))
            {
                Rs2Prayer.toggle(targetRs2Prayer);
            }
        }

        if (config.autoOffensivePrayer())
        {
            String weaponName = getEquippedWeaponName();
            Set<HeadIcon> overheads = getCurrentBossOverheads();
            WgsConfig.OffensivePrayerStyle resolvedStyle = resolveAutoOffensiveStyle(
                    config.offensivePrayerStyle(),
                    activeStage,
                    weaponName,
                    overheads
            );
            Rs2PrayerEnum offensivePrayer = getBestOffensivePrayer(resolvedStyle);
            if (offensivePrayer != null && !Rs2Prayer.isPrayerActive(offensivePrayer))
            {
                Rs2Prayer.toggle(offensivePrayer);
            }
        }
    }

    private void handlePostKillPrayerDisable()
    {
        if (config == null || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (!config.autoDisablePrayersOnKill() || (!config.autoSwitchPrayers() && !config.autoOffensivePrayer()))
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

    private void handleEmergencySoundAlerts()
    {
        if (config == null || client == null || activeStage == null)
        {
            return;
        }

        // 1. Low Prayer emergency check
        if (config.lowPrayerSoundAlert())
        {
            int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
            int maxPrayer = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
            boolean criticalBalancePhase = activeStage == WgsBossStage.BALANCE_ELEMENTAL
                    && getBalanceHpPhase().isCritical()
                    && currentPrayer <= maxPrayer / 2;

            if (currentPrayer <= config.lowPrayerThreshold() || criticalBalancePhase)
            {
                if (totalTicks - lastLowPrayerAlertTick >= LOW_PRAYER_ALERT_COOLDOWN_TICKS)
                {
                    Rs2SoundAlert.triggerAlert("", config.soundVolume());
                    lastLowPrayerAlertTick = totalTicks;
                }
            }
            else
            {
                lastLowPrayerAlertTick = totalTicks - LOW_PRAYER_ALERT_COOLDOWN_TICKS;
            }
        }

        // 2. Actionable boss mechanic emergency alerts (no routine style rotation spam)
        if (!config.soundAlerts())
        {
            return;
        }

        if (activeStage == WgsBossStage.SUROK_MAGIS)
        {
            WgsSurokMechanic mechanic = getActiveSurokMechanic();
            if (mechanic != WgsSurokMechanic.NONE
                    && mechanic != WgsSurokMechanic.POST_FIGHT_TELEORB
                    && mechanic != lastSurokAlertMechanic
                    && totalTicks - lastMechanicAlertTick >= MECHANIC_ALERT_COOLDOWN_TICKS)
            {
                Rs2SoundAlert.triggerAlert("", config.soundVolume());
                lastSurokAlertMechanic = mechanic;
                lastMechanicAlertTick = totalTicks;
            }
            else if (mechanic == WgsSurokMechanic.NONE)
            {
                lastSurokAlertMechanic = WgsSurokMechanic.NONE;
            }
        }
        else if (activeStage == WgsBossStage.TORMENTED_DEMONS && isPlayerInFireBombDanger())
        {
            if (totalTicks - lastMechanicAlertTick >= MECHANIC_ALERT_COOLDOWN_TICKS)
            {
                Rs2SoundAlert.triggerAlert("", config.soundVolume());
                lastMechanicAlertTick = totalTicks;
            }
        }
    }

    private String getEquippedWeaponName()
    {
        if (client == null)
        {
            return null;
        }
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip == null)
        {
            return null;
        }
        Item[] items = equip.getItems();
        if (items == null || items.length <= 3 || items[3] == null || items[3].getId() <= 0)
        {
            return null;
        }
        ItemComposition comp = client.getItemDefinition(items[3].getId());
        return comp != null ? comp.getName() : null;
    }

    private Set<HeadIcon> getCurrentBossOverheads()
    {
        if (activeStage == WgsBossStage.BALANCE_ELEMENTAL)
        {
            return balanceProtectedStyles;
        }
        if (activeStage == WgsBossStage.TORMENTED_DEMONS)
        {
            TormentedDemonState primary = getPrimaryTormentedDemonState();
            return primary != null ? primary.getProtectedStyles() : Collections.emptySet();
        }
        return Collections.emptySet();
    }

    private static Rs2PrayerEnum getBestOffensivePrayer(WgsConfig.OffensivePrayerStyle style)
    {
        if (style == null)
        {
            return null;
        }
        switch (style)
        {
            case MAGIC:
                return Rs2Prayer.getBestMagePrayer();
            case RANGED:
                return Rs2Prayer.getBestRangePrayer();
            case MELEE:
                return Rs2Prayer.getBestMeleePrayer();
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
        return (flags[sceneX][sceneY] & 0x240100) == 0;
    }

    private static boolean hasLivingNpc(List<NPC> npcs)
    {
        for (NPC npc : npcs)
        {
            if (npc != null && !npc.isDead())
            {
                return true;
            }
        }
        return false;
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

    public int getNpcSize(NPC npc, int defaultSize)
    {
        if (npc == null)
        {
            return defaultSize;
        }
        NPCComposition comp = npc.getTransformedComposition();
        return comp != null ? Math.max(1, comp.getSize()) : defaultSize;
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
