package net.runelite.client.plugins.microbot.bosshelper;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.bosshelper.model.BossPredictionResponse;
import net.runelite.client.plugins.microbot.bosshelper.model.BossState;
import net.runelite.client.plugins.microbot.bosshelper.model.ProjectileDto;
import net.runelite.client.plugins.microbot.bosshelper.model.TargetBoss;
import net.runelite.client.plugins.microbot.bosshelper.model.WorldPointDto;
import net.runelite.client.plugins.microbot.bosshelper.network.AgentServerClient;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Boss Helper",
        description = "AI-assisted boss helper using Agent Server",
        tags = {"boss", "helper", "combat", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class BossHelperPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private BossHelperConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PrayerOverlay prayerOverlay;

    @Inject
    private SceneOverlay sceneOverlay;

    @Inject
    private ClientThread clientThread;

    @Getter
    private NPC currentBoss;

    @Getter
    private int fightTicks = 0;

    @Getter
    private volatile Prayer recommendedPrayer = null;

    @Getter
    private final List<WorldPoint> safeTiles = new CopyOnWriteArrayList<>();

    @Getter
    private final List<WorldPoint> hazardTiles = new CopyOnWriteArrayList<>();

    private final Map<WorldPoint, Integer> timedHazards = new ConcurrentHashMap<>();

    private AgentServerClient agentClient;
    private ExecutorService networkExecutor;

    private volatile boolean awaitingResponse = false;
    private volatile boolean agentServerConnected = false;

    // Local heuristics tracking state
    private int hunllefAttackCount = 0;
    private Prayer hunllefCurrentStyle = Prayer.PROTECT_FROM_MISSILES;

    @Provides
    BossHelperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BossHelperConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Starting Boss Helper Plugin");
        agentClient = new AgentServerClient();
        networkExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "BossHelper-Worker-" + count.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        overlayManager.add(prayerOverlay);
        overlayManager.add(sceneOverlay);
        prayerOverlay.registerMouseListener();

        resetState();
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Stopping Boss Helper Plugin");
        overlayManager.remove(prayerOverlay);
        overlayManager.remove(sceneOverlay);
        prayerOverlay.unregisterMouseListener();

        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
            networkExecutor = null;
        }

        resetState();
    }

    private void resetState() {
        currentBoss = null;
        fightTicks = 0;
        recommendedPrayer = null;
        safeTiles.clear();
        hazardTiles.clear();
        timedHazards.clear();
        hunllefAttackCount = 0;
        hunllefCurrentStyle = Prayer.PROTECT_FROM_MISSILES;
        awaitingResponse = false;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Decrement and prune timed hazards
        decayTimedHazards();

        // Refresh or detect active boss
        updateCurrentBoss();

        if (currentBoss != null && !currentBoss.isDead()) {
            fightTicks++;
            dispatchTelemetrySnapshot();
        } else {
            if (fightTicks > 0) {
                fightTicks = 0;
                recommendedPrayer = null;
                safeTiles.clear();
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc == null) {
            return;
        }

        if (isTargetBoss(npc)) {
            currentBoss = npc;
            fightTicks = 0;
            hunllefAttackCount = 0;
            hunllefCurrentStyle = Prayer.PROTECT_FROM_MISSILES;
            safeTiles.clear();
            hazardTiles.clear();
            timedHazards.clear();
            log.debug("Boss spawned: {} (ID: {})", npc.getName(), npc.getId());
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc != null && npc == currentBoss) {
            log.debug("Boss despawned: {}", npc.getName());
            resetState();
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        Projectile projectile = event.getProjectile();
        LocalPoint pos = event.getPosition();
        if (projectile == null || pos == null) {
            return;
        }

        int id = projectile.getId();
        WorldPoint landingPoint = WorldPoint.fromLocal(client, pos);

        // Vorkath Projectiles
        if (id == 1481 && landingPoint != null) {
            // Vorkath fireball / mortar landing point -> 3x3 hazard zone
            addHazardArea(landingPoint, 1, 4);
        } else if (id == 1483 && landingPoint != null) {
            // Vorkath acid pool
            timedHazards.put(landingPoint, 15);
            rebuildHazardTilesList();
        } else if (id == 395) {
            // Vorkath Magic Attack
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MAGIC;
            }
        } else if (id == 393) {
            // Vorkath Range Attack
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MISSILES;
            }
        }

        // Hunllef Projectiles
        if (id == 1707 || id == 1708) {
            // Hunllef Magic attack projectile
            hunllefAttackCount++;
            if (hunllefAttackCount >= 4) {
                hunllefAttackCount = 0;
                hunllefCurrentStyle = Prayer.PROTECT_FROM_MISSILES;
            }
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = hunllefCurrentStyle;
            }
        } else if (id == 1711) {
            // Hunllef Ranged attack projectile
            hunllefAttackCount++;
            if (hunllefAttackCount >= 4) {
                hunllefAttackCount = 0;
                hunllefCurrentStyle = Prayer.PROTECT_FROM_MAGIC;
            }
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = hunllefCurrentStyle;
            }
        } else if (id == 1713) {
            // Hunllef Prayer-disabling attack
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = hunllefCurrentStyle;
            }
        }

        // Zulrah Projectiles
        if (id == 1046) {
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MAGIC;
            }
        } else if (id == 1044) {
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MISSILES;
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (actor == null || actor != currentBoss) {
            return;
        }

        int anim = actor.getAnimation();

        // Hunllef attack start frame animations
        if (anim == 8418 || anim == 8419) {
            // Magic attack animation start
            hunllefCurrentStyle = Prayer.PROTECT_FROM_MAGIC;
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MAGIC;
            }
        } else if (anim == 8412 || anim == 8413) {
            // Ranged attack animation start
            hunllefCurrentStyle = Prayer.PROTECT_FROM_MISSILES;
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MISSILES;
            }
        } else if (anim == 8420 || anim == 8421) {
            // Melee stomp animation start
            if (!agentServerConnected && config.enableLocalFallback()) {
                recommendedPrayer = Prayer.PROTECT_FROM_MELEE;
            }
        }
    }

    private void updateCurrentBoss() {
        if (currentBoss != null && !currentBoss.isDead()) {
            return;
        }

        TargetBoss target = config.targetBoss();
        for (NPC npc : client.getNpcs()) {
            if (npc != null && !npc.isDead() && matchesBoss(npc, target)) {
                currentBoss = npc;
                break;
            }
        }
    }

    private boolean isTargetBoss(NPC npc) {
        return matchesBoss(npc, config.targetBoss());
    }

    private boolean matchesBoss(NPC npc, TargetBoss target) {
        if (npc == null || npc.getName() == null) {
            return false;
        }

        int id = npc.getId();
        String name = npc.getName().toLowerCase();

        switch (target) {
            case ZULRAH:
                return isZulrah(id);
            case HUNLLEF:
                return isHunllef(id);
            case VORKATH:
                return isVorkath(id);
            case GENERIC_HAZARDS:
                return true;
            case AUTO_DETECT:
            default:
                return isZulrah(id) || isHunllef(id) || isVorkath(id)
                        || name.contains("zulrah") || name.contains("hunllef") || name.contains("vorkath");
        }
    }

    private boolean isZulrah(int id) {
        return id == NpcID.ZULRAH || id == NpcID.ZULRAH_2043 || id == NpcID.ZULRAH_2044;
    }

    private boolean isHunllef(int id) {
        return id == NpcID.CRYSTALLINE_HUNLLEF || id == NpcID.CRYSTALLINE_HUNLLEF_9022
                || id == NpcID.CRYSTALLINE_HUNLLEF_9023 || id == NpcID.CRYSTALLINE_HUNLLEF_9024
                || id == NpcID.CORRUPTED_HUNLLEF || id == NpcID.CORRUPTED_HUNLLEF_9036
                || id == NpcID.CORRUPTED_HUNLLEF_9037 || id == NpcID.CORRUPTED_HUNLLEF_9038;
    }

    private boolean isVorkath(int id) {
        return id == NpcID.VORKATH || id == NpcID.VORKATH_8058 || id == NpcID.VORKATH_8059
                || id == NpcID.VORKATH_8060 || id == NpcID.VORKATH_8061 || id == NpcID.VORKATH_11959;
    }

    private void dispatchTelemetrySnapshot() {
        if (awaitingResponse || networkExecutor == null || networkExecutor.isShutdown()) {
            return;
        }

        var player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        List<ProjectileDto> projectileDtos = new ArrayList<>();
        for (Projectile p : client.getProjectiles()) {
            if (p != null) {
                WorldPoint targetWp = p.getTargetActor() != null
                        ? p.getTargetActor().getWorldLocation()
                        : (p.getTargetPoint() != null ? p.getTargetPoint() : null);
                projectileDtos.add(ProjectileDto.fromProjectile(p, targetWp));
            }
        }

        String activeProtection = Rs2Prayer.getActiveProtectionPrayer() != null
                ? Rs2Prayer.getActiveProtectionPrayer().name()
                : "NONE";

        BossState state = BossState.builder()
                .targetBoss(config.targetBoss())
                .bossNpcId(currentBoss.getId())
                .bossNpcName(currentBoss.getName())
                .bossAnimation(currentBoss.getAnimation())
                .fightTick(fightTicks)
                .playerWorldLocation(WorldPointDto.fromWorldPoint(player.getWorldLocation()))
                .playerAnimation(player.getAnimation())
                .activeProjectiles(projectileDtos)
                .activeProtectionPrayer(activeProtection)
                .timestamp(System.currentTimeMillis())
                .build();

        awaitingResponse = true;

        networkExecutor.submit(() -> {
            try {
                agentClient.predictBossState(config.agentServerUrl(), state)
                        .thenAccept(response -> {
                            awaitingResponse = false;
                            if (response != null) {
                                agentServerConnected = true;
                                handleAgentResponse(response);
                            } else {
                                agentServerConnected = false;
                                if (config.enableLocalFallback()) {
                                    applyLocalFallback();
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            awaitingResponse = false;
                            agentServerConnected = false;
                            if (config.enableLocalFallback()) {
                                applyLocalFallback();
                            }
                            return null;
                        });
            } catch (Exception e) {
                awaitingResponse = false;
                agentServerConnected = false;
                if (config.enableLocalFallback()) {
                    applyLocalFallback();
                }
            }
        });
    }

    private void handleAgentResponse(BossPredictionResponse response) {
        if (response.getRecommendedPrayer() != null) {
            Prayer prayer = response.toRecommendedPrayer();
            if (prayer != null) {
                this.recommendedPrayer = prayer;
            }
        }

        List<WorldPoint> newSafeTiles = response.toSafeWorldPoints();
        safeTiles.clear();
        safeTiles.addAll(newSafeTiles);

        List<WorldPoint> newHazardTiles = response.toHazardWorldPoints();
        if (!newHazardTiles.isEmpty()) {
            hazardTiles.clear();
            hazardTiles.addAll(newHazardTiles);
        }
    }

    private void applyLocalFallback() {
        if (currentBoss == null) {
            return;
        }

        int id = currentBoss.getId();

        // Zulrah heuristic
        if (isZulrah(id)) {
            if (id == NpcID.ZULRAH) {
                // Green / Ranged form
                recommendedPrayer = Prayer.PROTECT_FROM_MISSILES;
            } else if (id == NpcID.ZULRAH_2043) {
                // Turquoise / Magic form
                recommendedPrayer = Prayer.PROTECT_FROM_MAGIC;
            } else if (id == NpcID.ZULRAH_2044) {
                // Crimson / Melee form
                recommendedPrayer = Prayer.PROTECT_FROM_MELEE;
            }
        } else if (isHunllef(id)) {
            // Hunllef heuristic based on style tracker
            recommendedPrayer = hunllefCurrentStyle;
        } else if (isVorkath(id)) {
            // Default Vorkath protection is missiles (ranged attacks)
            if (recommendedPrayer == null) {
                recommendedPrayer = Prayer.PROTECT_FROM_MISSILES;
            }
        }

        calculateLocalSafeTiles();
    }

    private void calculateLocalSafeTiles() {
        var player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        WorldPoint playerLoc = player.getWorldLocation();
        if (playerLoc == null) {
            return;
        }

        List<WorldPoint> nearbySafe = new ArrayList<>();
        // Search 5x5 around player for tiles not in hazard list
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                WorldPoint tile = playerLoc.dx(dx).dy(dy);
                if (!hazardTiles.contains(tile)) {
                    nearbySafe.add(tile);
                }
            }
        }

        safeTiles.clear();
        safeTiles.addAll(nearbySafe);
    }

    private void addHazardArea(WorldPoint center, int radius, int ticks) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                WorldPoint p = center.dx(dx).dy(dy);
                timedHazards.put(p, ticks);
            }
        }
        rebuildHazardTilesList();
    }

    private void decayTimedHazards() {
        if (timedHazards.isEmpty()) {
            return;
        }

        boolean changed = false;
        Iterator<Map.Entry<WorldPoint, Integer>> iterator = timedHazards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WorldPoint, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
                changed = true;
            } else {
                entry.setValue(remaining);
            }
        }

        if (changed) {
            rebuildHazardTilesList();
        }
    }

    private void rebuildHazardTilesList() {
        hazardTiles.clear();
        hazardTiles.addAll(timedHazards.keySet());
    }
}
