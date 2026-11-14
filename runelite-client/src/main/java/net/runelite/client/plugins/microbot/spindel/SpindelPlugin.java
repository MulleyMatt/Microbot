package net.runelite.client.plugins.microbot.spindel;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
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
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.ObjectID1;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelFlashOverlay;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelHudOverlay;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelPrayerMarkerOverlay;
import net.runelite.client.plugins.microbot.spindel.overlays.SpindelSceneOverlay;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Spindel Helper",
        description = "Wilderness Spindel fight helper: 8-Ranged / 8-Magic auto-prayer rotation, expanding Web hazard & safe tiles, Spiderling highlights, anti-PK instant Royal Seed Pod teleport, and Bank/GE supply filters",
        tags = {"user", "helper", "spindel", "venenatis", "wilderness", "boss", "pvm", "antipk", "overlay", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class SpindelPlugin extends Plugin
{
    public static final int SPINDEL_NPC_ID = NpcID.VENENATIS_SINGLES;
    public static final int VENENATIS_NPC_ID = NpcID.VENENATIS;
    public static final int SPINDEL_SPIDERLING_ID = NpcID.SPINDEL_SPIDERLING;
    public static final int VENENATIS_SPIDERLING_ID = NpcID.VENENATIS_SPIDERLING;

    public static final int ANIM_RANGED = AnimationID.NPC_VENENATIS_RANGED_01;
    public static final int ANIM_MAGIC = AnimationID.NPC_VENENATIS_MAGIC_01;
    public static final int ANIM_MELEE = AnimationID.NPC_VENENATIS_MELEE_01;
    public static final int ANIM_DEATH = AnimationID.NPC_VENENATIS_DEATH_01;
    public static final int ANIM_SPAWN = AnimationID.NPC_VENENATIS_SPAWN_01;
    public static final int ANIM_WEB_PROJECTILE = AnimationID.VFX_VENENATIS_WEB_PROJECTILE;

    public static final int PROJ_RANGED = SpotanimID.FX_VENENATIS_RANGED_PROJECTILE;
    public static final int PROJ_MAGIC = SpotanimID.FX_VENENATIS_MAGIC_PROJECTILE;
    public static final int PROJ_WEB = SpotanimID.FX_VENENATIS_WEB_PROJECTILE;

    public static final Set<Integer> WEB_OBJECT_IDS = ImmutableSet.of(
            ObjectID1.WBR_VENENATIS_WEB_CENTRE,
            ObjectID1.WBR_VENENATIS_WEB_EDGE,
            ObjectID1.WBR_VENENATIS_WEB_CORNER,
            ObjectID.VENENATIS_WEBBED_FLOOR
    );

    public static final Set<Integer> CAVE_EXIT_OBJECT_IDS = ImmutableSet.of(
            ObjectID1.WILD_BOSS_ESCAPE_CAVE_EXIT_VENENATIS,
            ObjectID1.WILD_BOSS_ESCAPE_CAVE_EXIT01,
            ObjectID1.WILD_BOSS_ESCAPE_CAVE_EXIT02
    );

    public static final int WEB_CHASM_REGION_ID = 6580;
    public static final int SILK_CHASM_REGION_ID = 13727;
    public static final int SPINDEL_WILDERNESS_LEVEL = 29;
    public static final int SPINDEL_SIZE = 5;
    public static final int SPINDEL_ATTACK_SPEED_TICKS = 6;
    public static final int WEB_EXPANSION_RADIUS = 3;
    public static final int SEED_POD_COOLDOWN_TICKS = 3;
    public static final int LOW_PRAYER_ALERT_COOLDOWN_TICKS = 8;
    public static final int PKER_ALERT_COOLDOWN_TICKS = 5;
    public static final int WEB_ALERT_COOLDOWN_TICKS = 5;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpindelConfig config;

    @Inject
    private SpindelSupplyCalculator supplyCalculator;

    @Inject
    private SpindelBankInterface bankInterface;

    @Inject
    private SpindelGrandExchangeInterface geInterface;

    @Inject
    private SpindelHudOverlay hudOverlay;

    @Inject
    private SpindelPrayerHelperOverlay prayerHelperOverlay;

    @Inject
    private SpindelPrayerMarkerOverlay prayerMarkerOverlay;

    @Inject
    private SpindelSceneOverlay sceneOverlay;

    @Inject
    private SpindelFlashOverlay flashOverlay;

    @Getter
    private NPC spindelNpc;

    @Getter
    private SpindelPhase currentPhase = SpindelPhase.RANGED;

    /**
     * Number of attacks (0..7) already fired in the current 8-attack style phase.
     */
    @Getter
    private int attacksInCurrentPhase = 0;

    /**
     * Ticks remaining until Spindel's next standard attack.
     */
    @Getter
    private int attackTicks = -1;

    @Getter
    private int totalTicks = 0;

    private int lastAttackRecordedTick = -10;
    private int lastSeedPodInvokeTick = -SEED_POD_COOLDOWN_TICKS;
    private int lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
    private int lastPkerAlertTick = -PKER_ALERT_COOLDOWN_TICKS;
    private int lastWebAlertTick = -WEB_ALERT_COOLDOWN_TICKS;

    @Getter
    private boolean seedPodTriggeredThisEncounter = false;

    @Getter
    private boolean postKillPrayerReminder = false;

    /**
     * Active floor web GameObjects -> WorldPoint.
     */
    @Getter
    private final Map<GameObject, WorldPoint> activeWebObjects = new HashMap<>();

    /**
     * Center tiles of expanding webs (from WBR_VENENATIS_WEB_CENTRE or incoming web projectiles) -> remaining ticks.
     */
    @Getter
    private final Map<WorldPoint, Integer> webCenterTiles = new HashMap<>();

    /**
     * Cave exit GameObjects in the arena -> WorldPoint.
     */
    @Getter
    private final Map<GameObject, WorldPoint> caveExitObjects = new HashMap<>();

    /**
     * Active Spindel's Spiderling NPCs in the cave.
     */
    @Getter
    private final List<NPC> spiderlings = new ArrayList<>();

    /**
     * Currently detected threat players (PKers) inside the cave.
     */
    @Getter
    private final List<Player> detectedThreatPlayers = new ArrayList<>();

    @Provides
    SpindelConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SpindelConfig.class);
    }

    @Override
    protected void startUp()
    {
        reset();
        overlayManager.add(hudOverlay);
        overlayManager.add(prayerHelperOverlay);
        overlayManager.add(prayerMarkerOverlay);
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
        overlayManager.remove(prayerMarkerOverlay);
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
        spindelNpc = null;
        currentPhase = SpindelPhase.RANGED;
        attacksInCurrentPhase = 0;
        attackTicks = -1;
        totalTicks = 0;
        lastAttackRecordedTick = -10;
        lastSeedPodInvokeTick = -SEED_POD_COOLDOWN_TICKS;
        lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
        lastPkerAlertTick = -PKER_ALERT_COOLDOWN_TICKS;
        lastWebAlertTick = -WEB_ALERT_COOLDOWN_TICKS;
        seedPodTriggeredThisEncounter = false;
        postKillPrayerReminder = false;
        activeWebObjects.clear();
        webCenterTiles.clear();
        caveExitObjects.clear();
        spiderlings.clear();
        detectedThreatPlayers.clear();
    }

    private void scanExistingEntities()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        for (NPC npc : client.getNpcs())
        {
            if (npc == null)
            {
                continue;
            }
            if (isSpindelBossId(npc.getId()))
            {
                spindelNpc = npc;
            }
            else if (isSpiderlingId(npc.getId()) && !spiderlings.contains(npc))
            {
                spiderlings.add(npc);
            }
        }
    }

    public static boolean isSpindelBossId(int npcId)
    {
        return npcId == SPINDEL_NPC_ID || npcId == VENENATIS_NPC_ID;
    }

    public static boolean isSpiderlingId(int npcId)
    {
        return npcId == SPINDEL_SPIDERLING_ID || npcId == VENENATIS_SPIDERLING_ID;
    }

    public boolean isInSpindelCave()
    {
        if (spindelNpc != null)
        {
            return true;
        }
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        if (wp == null)
        {
            return false;
        }
        int regionId = wp.getRegionID();
        return regionId == WEB_CHASM_REGION_ID || regionId == SILK_CHASM_REGION_ID;
    }

    public boolean isTeleblocked()
    {
        return client != null && client.getVarbitValue(VarbitID.TELEBLOCK_CYCLES) > 0;
    }

    public int getRemainingAttacksInPhase()
    {
        return Math.max(1, SpindelPhase.ATTACKS_PER_PHASE - attacksInCurrentPhase);
    }

    public int getTotalCycleProgress()
    {
        int base = currentPhase == SpindelPhase.RANGED ? 0 : SpindelPhase.ATTACKS_PER_PHASE;
        return base + attacksInCurrentPhase;
    }

    public Prayer getRequiredOverheadPrayer()
    {
        if (spindelNpc == null || spindelNpc.isDead() || currentPhase == null)
        {
            return null;
        }
        return currentPhase.getDefaultPrayer();
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
            activeWebObjects.clear();
            webCenterTiles.clear();
            caveExitObjects.clear();
            spiderlings.clear();
            detectedThreatPlayers.clear();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if (npc == null)
        {
            return;
        }

        int npcId = npc.getId();
        if (isSpiderlingId(npcId))
        {
            if (!spiderlings.contains(npc))
            {
                spiderlings.add(npc);
            }
            return;
        }

        if (isSpindelBossId(npcId))
        {
            spindelNpc = npc;
            currentPhase = SpindelPhase.RANGED;
            attacksInCurrentPhase = 0;
            attackTicks = SPINDEL_ATTACK_SPEED_TICKS;
            postKillPrayerReminder = false;
            handleAutoPrayers();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        if (npc == null)
        {
            return;
        }

        if (isSpiderlingId(npc.getId()))
        {
            spiderlings.remove(npc);
            return;
        }

        if (npc == spindelNpc || isSpindelBossId(npc.getId()))
        {
            spindelNpc = null;
            currentPhase = SpindelPhase.RANGED;
            attacksInCurrentPhase = 0;
            attackTicks = -1;
            activeWebObjects.clear();
            webCenterTiles.clear();
            spiderlings.clear();
            postKillPrayerReminder = true;
            handlePostKillPrayerDisable();
        }
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
        if (!isSpindelBossId(npc.getId()) && npc != spindelNpc)
        {
            return;
        }

        if (spindelNpc == null)
        {
            spindelNpc = npc;
        }

        int animId = npc.getAnimation();
        if (animId == ANIM_DEATH)
        {
            spindelNpc = null;
            currentPhase = SpindelPhase.RANGED;
            attacksInCurrentPhase = 0;
            attackTicks = -1;
            activeWebObjects.clear();
            webCenterTiles.clear();
            spiderlings.clear();
            postKillPrayerReminder = true;
            handlePostKillPrayerDisable();
            return;
        }

        if (animId == ANIM_RANGED || animId == ANIM_MAGIC || animId == ANIM_MELEE)
        {
            recordBossAttack(animId);
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

        int id = projectile.getId();
        if (id == PROJ_WEB)
        {
            LocalPoint targetLp = projectile.getTarget();
            if (targetLp != null && client != null)
            {
                WorldPoint targetWp = WorldPoint.fromLocal(client, targetLp);
                if (targetWp != null)
                {
                    webCenterTiles.put(targetWp, 5);
                }
            }
        }
        else if ((id == PROJ_RANGED || id == PROJ_MAGIC) && spindelNpc != null)
        {
            int syntheticAnim = (id == PROJ_RANGED) ? ANIM_RANGED : ANIM_MAGIC;
            recordBossAttack(syntheticAnim);
            handleAutoPrayers();
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        if (gameObject == null)
        {
            return;
        }

        int id = gameObject.getId();
        WorldPoint wp = gameObject.getWorldLocation();
        if (wp == null)
        {
            return;
        }

        if (WEB_OBJECT_IDS.contains(id))
        {
            activeWebObjects.put(gameObject, wp);
            if (id == ObjectID1.WBR_VENENATIS_WEB_CENTRE)
            {
                webCenterTiles.put(wp, 18);
            }
        }
        else if (CAVE_EXIT_OBJECT_IDS.contains(id))
        {
            caveExitObjects.put(gameObject, wp);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject gameObject = event.getGameObject();
        if (gameObject == null)
        {
            return;
        }

        WorldPoint removedWp = activeWebObjects.remove(gameObject);
        if (removedWp != null && gameObject.getId() == ObjectID1.WBR_VENENATIS_WEB_CENTRE)
        {
            webCenterTiles.remove(removedWp);
        }
        caveExitObjects.remove(gameObject);
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event)
    {
        Player player = event.getPlayer();
        if (player == null || !isInSpindelCave())
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

        if (!geInterface.isAtGrandExchange() && geInterface.isActive())
        {
            geInterface.destroy();
        }

        if (!webCenterTiles.isEmpty())
        {
            webCenterTiles.values().removeIf(ticks -> ticks <= 1);
            webCenterTiles.replaceAll((wp, ticks) -> ticks - 1);
        }

        spiderlings.removeIf(npc -> npc == null || npc.isDead());

        // 1. Always scan for PKers and Web hazards if inside Spindel's cave
        if (isInSpindelCave())
        {
            updateThreatPlayersInCave();
            if (!detectedThreatPlayers.isEmpty())
            {
                handlePkerDetected();
            }
            else
            {
                seedPodTriggeredThisEncounter = false;
            }
            handleWebDangerAlert();
        }
        else
        {
            detectedThreatPlayers.clear();
            seedPodTriggeredThisEncounter = false;
            lastWebAlertTick = -WEB_ALERT_COOLDOWN_TICKS;
        }

        // 2. Boss tick & prayer handling
        if (spindelNpc == null || spindelNpc.isDead())
        {
            if (postKillPrayerReminder && client.getLocalPlayer() != null && client.getLocalPlayer().getOverheadIcon() == null)
            {
                postKillPrayerReminder = false;
            }
            return;
        }

        if (attackTicks > 0)
        {
            attackTicks--;
        }
        else if (attackTicks == 0)
        {
            attackTicks = -1;
        }

        handleAutoPrayers();
        handleLowPrayerAlert();
    }

    // =========================================================================
    // Bank & Grand Exchange Event Forwarding
    // =========================================================================

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        geInterface.onScriptPostFired(event);
        bankInterface.onScriptPostFired(event);
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event)
    {
        geInterface.onGrandExchangeSearched(event);
    }

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
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        bankInterface.onItemContainerChanged(event);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        bankInterface.handleClick(event);
    }

    // =========================================================================
    // 8-Ranged / 8-Magic Rotation Logic
    // =========================================================================

    @Value
    public static class RotationState
    {
        SpindelPhase phase;
        int attacksInPhase;
        boolean phaseSwitched;
    }

    /**
     * Pure helper: advances Spindel's 8-Ranged / 8-Magic attack cycle given an attack animation ID.
     * - Automatically self-syncs if a Ranged (9989) or Magic (9990) animation is observed while in the opposite phase.
     * - Immediately flips to the next phase upon completing the 8th attack in the current phase so the player
     *   switches protection prayers BEFORE the 1st attack of the new style lands.
     */
    public static RotationState computeNextRotationState(
            SpindelPhase currentPhase,
            int currentAttacksInPhase,
            int animationId)
    {
        SpindelPhase effectivePhase = currentPhase != null ? currentPhase : SpindelPhase.RANGED;
        int count = Math.max(0, currentAttacksInPhase);
        boolean switched = false;

        if (animationId == ANIM_RANGED)
        {
            if (effectivePhase != SpindelPhase.RANGED)
            {
                effectivePhase = SpindelPhase.RANGED;
                count = 1;
                switched = true;
            }
            else
            {
                count++;
            }
        }
        else if (animationId == ANIM_MAGIC)
        {
            if (effectivePhase != SpindelPhase.MAGIC)
            {
                effectivePhase = SpindelPhase.MAGIC;
                count = 1;
                switched = true;
            }
            else
            {
                count++;
            }
        }
        else if (animationId == ANIM_MELEE)
        {
            count++;
        }
        else
        {
            return new RotationState(effectivePhase, count, false);
        }

        if (count >= SpindelPhase.ATTACKS_PER_PHASE)
        {
            effectivePhase = effectivePhase.getNextPhase();
            count = 0;
            switched = true;
        }

        return new RotationState(effectivePhase, count, switched);
    }

    private void recordBossAttack(int animId)
    {
        if (totalTicks - lastAttackRecordedTick <= 1)
        {
            return;
        }
        lastAttackRecordedTick = totalTicks;
        attackTicks = SPINDEL_ATTACK_SPEED_TICKS;

        RotationState next = computeNextRotationState(currentPhase, attacksInCurrentPhase, animId);
        this.currentPhase = next.getPhase();
        this.attacksInCurrentPhase = next.getAttacksInPhase();
    }

    // =========================================================================
    // Client-Thread Safe Auto-Prayer
    // =========================================================================

    private void handleAutoPrayers()
    {
        if (config == null || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (spindelNpc == null || spindelNpc.isDead() || currentPhase == null)
        {
            return;
        }
        if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0)
        {
            return;
        }

        if (config.autoSwitchPrayers())
        {
            Prayer requiredOverhead = currentPhase.getDefaultPrayer();
            Rs2PrayerEnum targetPrayer = toRs2PrayerEnum(requiredOverhead);
            if (targetPrayer != null
                    && client.getRealSkillLevel(Skill.PRAYER) >= targetPrayer.getLevel()
                    && !client.isPrayerActive(requiredOverhead))
            {
                Rs2Prayer.toggle(targetPrayer);
            }
        }

        if (config.autoOffensivePrayer())
        {
            Rs2PrayerEnum bestOffensive = resolveBestOffensivePrayer(config.offensivePrayerStyle());
            if (bestOffensive != null && !Rs2Prayer.isPrayerActive(bestOffensive))
            {
                Rs2Prayer.toggle(bestOffensive);
            }
        }
    }

    private Rs2PrayerEnum resolveBestOffensivePrayer(SpindelConfig.OffensivePrayerStyle style)
    {
        if (style == null)
        {
            return Rs2Prayer.getBestMeleePrayer();
        }
        switch (style)
        {
            case RANGED:
                return Rs2Prayer.getBestRangePrayer();
            case MAGIC:
                return Rs2Prayer.getBestMagePrayer();
            case MELEE:
            default:
                return Rs2Prayer.getBestMeleePrayer();
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
            case SMITE:
                return Rs2PrayerEnum.SMITE;
            default:
                return null;
        }
    }

    // =========================================================================
    // Anti-PK & Client-Thread Safe Royal Seed Pod Teleport
    // =========================================================================

    /**
     * Pure helper: determines if another player in the cave is a potential PKer threat.
     */
    public static boolean isThreatPlayer(
            int localCombatLevel,
            int otherCombatLevel,
            int wildernessLevel,
            boolean isPvpWorld,
            boolean isDeadmanWorld,
            boolean ignoreFriendsAndClan,
            boolean isFriendOrClan,
            SpindelConfig.PkerDetectionMode mode)
    {
        if (ignoreFriendsAndClan && isFriendOrClan)
        {
            return false;
        }
        if (mode == SpindelConfig.PkerDetectionMode.ANY_PLAYER || isDeadmanWorld)
        {
            return true;
        }

        int effectiveWildy = Math.max(1, wildernessLevel) + (isPvpWorld ? 15 : 0);
        return Math.abs(localCombatLevel - otherCombatLevel) <= effectiveWildy;
    }

    private boolean isPlayerThreatToLocal(Player other)
    {
        Player local = client != null ? client.getLocalPlayer() : null;
        if (local == null || other == null || other == local)
        {
            return false;
        }

        boolean isFriendOrClan = other.isFriend() || other.isClanMember() || other.isFriendsChatMember();
        WorldPoint localWp = local.getWorldLocation();
        int wildyLevel = localWp != null ? Rs2Pvp.getWildernessLevelFrom(localWp) : SPINDEL_WILDERNESS_LEVEL;
        if (wildyLevel <= 0)
        {
            wildyLevel = SPINDEL_WILDERNESS_LEVEL;
        }

        boolean isPvpWorld = WorldType.isPvpWorld(client.getWorldType());
        boolean isDeadman = WorldType.isDeadmanWorld(client.getWorldType());

        return isThreatPlayer(
                local.getCombatLevel(),
                other.getCombatLevel(),
                wildyLevel,
                isPvpWorld,
                isDeadman,
                config.ignoreFriendsAndClan(),
                isFriendOrClan,
                config.pkerDetectionMode()
        );
    }

    private void updateThreatPlayersInCave()
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

        if (config.autoSeedPodOnPker()
                && !isTeleblocked()
                && totalTicks - lastSeedPodInvokeTick >= SEED_POD_COOLDOWN_TICKS)
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

    // =========================================================================
    // Expanding Web Hazard & Safe Escape Tile Math
    // =========================================================================

    /**
     * Returns all currently hazardous web tiles (both spawned web objects and the 7x7 expansion zone
     * around any active web center / incoming web projectile).
     */
    public Set<WorldPoint> getAllWebHazardTiles()
    {
        return calculateExpandedWebTiles(activeWebObjects.values(), webCenterTiles.keySet());
    }

    /**
     * Pure helper: computes the full set of hazardous web tiles from individual web objects and 7x7 web centers.
     */
    public static Set<WorldPoint> calculateExpandedWebTiles(
            Collection<WorldPoint> webObjectTiles,
            Collection<WorldPoint> centerTiles)
    {
        Set<WorldPoint> hazardTiles = new LinkedHashSet<>();
        if (webObjectTiles != null)
        {
            for (WorldPoint wp : webObjectTiles)
            {
                if (wp != null)
                {
                    hazardTiles.add(wp);
                }
            }
        }
        if (centerTiles != null)
        {
            for (WorldPoint center : centerTiles)
            {
                if (center == null)
                {
                    continue;
                }
                for (int dx = -WEB_EXPANSION_RADIUS; dx <= WEB_EXPANSION_RADIUS; dx++)
                {
                    for (int dy = -WEB_EXPANSION_RADIUS; dy <= WEB_EXPANSION_RADIUS; dy++)
                    {
                        hazardTiles.add(new WorldPoint(
                                center.getX() + dx,
                                center.getY() + dy,
                                center.getPlane()
                        ));
                    }
                }
            }
        }
        return hazardTiles;
    }

    public boolean isPlayerInWebDanger()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        return playerWp != null && getAllWebHazardTiles().contains(playerWp);
    }

    /**
     * Pure helper: calculates up to 4 closest safe escape tiles 1..4 tiles away from the player
     * that are outside the web hazard zone and outside Spindel's 5x5 footprint.
     */
    public static List<WorldPoint> calculateWebSafeEscapeTiles(
            WorldPoint playerTile,
            Set<WorldPoint> webHazardTiles,
            WorldPoint bossSwTile,
            int bossSize)
    {
        if (playerTile == null || webHazardTiles == null || webHazardTiles.isEmpty())
        {
            return Collections.emptyList();
        }

        int minBossX = bossSwTile != null ? bossSwTile.getX() : Integer.MIN_VALUE;
        int maxBossX = bossSwTile != null ? bossSwTile.getX() + Math.max(1, bossSize) - 1 : Integer.MIN_VALUE;
        int minBossY = bossSwTile != null ? bossSwTile.getY() : Integer.MIN_VALUE;
        int maxBossY = bossSwTile != null ? bossSwTile.getY() + Math.max(1, bossSize) - 1 : Integer.MIN_VALUE;

        List<WorldPoint> candidates = new ArrayList<>();
        for (int dx = -4; dx <= 4; dx++)
        {
            for (int dy = -4; dy <= 4; dy++)
            {
                if (dx == 0 && dy == 0)
                {
                    continue;
                }
                WorldPoint candidate = new WorldPoint(
                        playerTile.getX() + dx,
                        playerTile.getY() + dy,
                        playerTile.getPlane()
                );
                if (webHazardTiles.contains(candidate))
                {
                    continue;
                }
                boolean insideBoss = bossSwTile != null
                        && candidate.getPlane() == bossSwTile.getPlane()
                        && candidate.getX() >= minBossX && candidate.getX() <= maxBossX
                        && candidate.getY() >= minBossY && candidate.getY() <= maxBossY;
                if (!insideBoss)
                {
                    candidates.add(candidate);
                }
            }
        }

        candidates.sort(Comparator
                .comparingInt((WorldPoint wp) -> Math.max(
                        Math.abs(wp.getX() - playerTile.getX()),
                        Math.abs(wp.getY() - playerTile.getY())
                ))
                .thenComparingInt(wp -> Math.abs(wp.getX() - playerTile.getX())
                        + Math.abs(wp.getY() - playerTile.getY())));

        return candidates.subList(0, Math.min(4, candidates.size()));
    }

    public int getSpindelSize()
    {
        if (spindelNpc != null)
        {
            NPCComposition comp = spindelNpc.getTransformedComposition();
            if (comp != null && comp.getSize() > 0)
            {
                return comp.getSize();
            }
        }
        return SPINDEL_SIZE;
    }

    private void handleLowPrayerAlert()
    {
        if (config == null || client == null || spindelNpc == null || spindelNpc.isDead())
        {
            return;
        }

        int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
        int threshold = config.lowPrayerThreshold();
        if (currentPrayer > threshold)
        {
            lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
            return;
        }

        if (config.lowPrayerSoundAlert() && totalTicks - lastLowPrayerAlertTick >= LOW_PRAYER_ALERT_COOLDOWN_TICKS)
        {
            Rs2SoundAlert.triggerAlert(config.soundVolume());
            lastLowPrayerAlertTick = totalTicks;
        }
    }

    private void handleWebDangerAlert()
    {
        if (config == null || client == null)
        {
            return;
        }

        if (!isPlayerInWebDanger())
        {
            lastWebAlertTick = -WEB_ALERT_COOLDOWN_TICKS;
            return;
        }

        if (config.soundAlerts() && totalTicks - lastWebAlertTick >= WEB_ALERT_COOLDOWN_TICKS)
        {
            Rs2SoundAlert.triggerAlert(config.soundVolume());
            lastWebAlertTick = totalTicks;
        }
    }
}
