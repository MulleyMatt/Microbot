package net.runelite.client.plugins.microbot.muspah;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID1;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahFlashOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahInstanceTimerOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahPhaseOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahPrayerHelperOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahPrayerMarkerOverlay;
import net.runelite.client.plugins.microbot.muspah.overlays.MuspahSceneOverlay;
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

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Phantom Muspah Helper",
        description = "Zulrah-style interactive overlay helper for Phantom Muspah (prayer helper, prayer tab marker, carapace form HUD, Bowfa/Sapphire bolt tags, melee kiting border, shockwave safe tiles, and hazard highlights)",
        tags = {"user", "helper", "muspah", "phantom", "boss", "pvm", "overlay", "zulrah", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class MuspahPlugin extends Plugin
{
    public static final int MUSPAH_SIZE = 5;

    public static final Set<Integer> SPIKE_WARNING_SPOTANIMS = ImmutableSet.of(
            SpotanimID.VFX_MUSPAH_SPIKE_WARNING_01,
            SpotanimID.VFX_MUSPAH_SPIKE_WARNING_02,
            SpotanimID.VFX_MUSPAH_SPIKE_SPAWN_01,
            SpotanimID.VFX_MUSPAH_SPIKE_SPAWN_02
    );

    public static final Set<Integer> STATIONARY_SPIKE_OBJECTS = ImmutableSet.of(
            ObjectID1.MUSPAH_SPIKE,
            ObjectID1.MUSPAH_SPIKE_POST,
            ObjectID1.MUSPAH_SPIKE_SURROUNDED
    );

    public static final int PRE_SPIKE_OBJECT_ID = ObjectID1.MUSPAH_SPIKE_PRE;
    public static final int HOMING_SPIKE_OBJECT_ID = ObjectID1.MUSPAH_SPIKE_FOLLOW;
    public static final int SHADOW_ORB_NPC_ID = NpcID.MUSPAH_PROJECTILE;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MuspahConfig config;

    @Inject
    private MuspahInstanceTimerOverlay instanceTimerOverlay;

    @Inject
    private MuspahPhaseOverlay phaseOverlay;

    @Inject
    private MuspahPrayerHelperOverlay prayerHelperOverlay;

    @Inject
    private MuspahPrayerMarkerOverlay prayerMarkerOverlay;

    @Inject
    private MuspahSceneOverlay sceneOverlay;

    @Inject
    private MuspahFlashOverlay flashOverlay;

    @Getter
    private NPC muspahNpc;

    @Getter
    private MuspahForm currentForm;

    @Getter
    private int attackTicks = -1;

    @Getter
    private int magicAttackTicks = -1;

    @Getter
    private int shockwaveTicks = -1;

    @Getter
    private int formTicks = 0;

    @Getter
    private int totalTicks = 0;

    @Getter
    private MuspahSpecialAttack activeSpecialAttack;

    /**
     * Active stationary floor spike objects in the arena (used both for hazard display and shockwave safe-tile math).
     */
    @Getter
    private final Map<GameObject, WorldPoint> stationarySpikes = new HashMap<>();

    /**
     * Pre-emerge floor spike objects (46693).
     */
    @Getter
    private final Map<GameObject, WorldPoint> preSpikes = new HashMap<>();

    /**
     * Moving/homing spikes (46697).
     */
    @Getter
    private final Map<GameObject, WorldPoint> homingSpikes = new HashMap<>();

    /**
     * Incoming spike warning tiles (from GraphicsObjects or form-change player-tile warning) -> remaining ticks.
     */
    @Getter
    private final Map<WorldPoint, Integer> spikeWarningTiles = new HashMap<>();

    /**
     * Wandering shadow orbs (NpcID 12083) during the dark-screen phase.
     */
    @Getter
    private final List<NPC> shadowOrbs = new ArrayList<>();

    /**
     * Tracks whether the player recently fought Muspah in this region, used for the post-kill prayer conservation prompt.
     */
    @Getter
    private boolean postKillPrayerReminder = false;

    public static final int THRALL_RECAST_DEBOUNCE_TICKS = 3;
    public static final int LOW_PRAYER_ALERT_COOLDOWN_TICKS = 8;

    @Getter
    private int lastThrallCastTick = -THRALL_RECAST_DEBOUNCE_TICKS;

    @Getter
    private int lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;

    @Provides
    MuspahConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MuspahConfig.class);
    }

    @Override
    protected void startUp()
    {
        reset();
        overlayManager.add(instanceTimerOverlay);
        overlayManager.add(phaseOverlay);
        overlayManager.add(prayerHelperOverlay);
        overlayManager.add(prayerMarkerOverlay);
        overlayManager.add(sceneOverlay);
        overlayManager.add(flashOverlay);
    }

    @Override
    protected void shutDown()
    {
        reset();
        overlayManager.remove(instanceTimerOverlay);
        overlayManager.remove(phaseOverlay);
        overlayManager.remove(prayerHelperOverlay);
        overlayManager.remove(prayerMarkerOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(flashOverlay);
    }

    public void reset()
    {
        muspahNpc = null;
        currentForm = null;
        attackTicks = -1;
        magicAttackTicks = -1;
        shockwaveTicks = -1;
        formTicks = 0;
        totalTicks = 0;
        lastThrallCastTick = -THRALL_RECAST_DEBOUNCE_TICKS;
        lastLowPrayerAlertTick = -LOW_PRAYER_ALERT_COOLDOWN_TICKS;
        activeSpecialAttack = null;
        stationarySpikes.clear();
        preSpikes.clear();
        homingSpikes.clear();
        spikeWarningTiles.clear();
        shadowOrbs.clear();
        postKillPrayerReminder = false;
        if (instanceTimerOverlay != null)
        {
            instanceTimerOverlay.resetTimer();
        }
    }

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
            stationarySpikes.clear();
            preSpikes.clear();
            homingSpikes.clear();
            spikeWarningTiles.clear();
            shadowOrbs.clear();
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
        if (npcId == SHADOW_ORB_NPC_ID)
        {
            if (!shadowOrbs.contains(npc))
            {
                shadowOrbs.add(npc);
            }
            return;
        }

        MuspahForm form = MuspahForm.fromNpcId(npcId);
        if (form != null)
        {
            muspahNpc = npc;
            currentForm = form;
            formTicks = 0;
            postKillPrayerReminder = false;
            instanceTimerOverlay.setTimerIfNotRunning();
            handleAutoPrayers();
            handleAutoThrall();
            log.debug("Phantom Muspah spawned in form: {}", form);
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        NPC npc = event.getNpc();
        if (npc == null)
        {
            return;
        }

        MuspahForm newForm = MuspahForm.fromNpcId(npc.getId());
        if (newForm != null)
        {
            MuspahForm previousForm = currentForm;
            muspahNpc = npc;
            currentForm = newForm;
            formTicks = 0;

            // Whenever Phantom Muspah changes forms, a spike always spawns under the player's current tile
            if (previousForm != null && previousForm != newForm && client.getLocalPlayer() != null)
            {
                WorldPoint playerPoint = client.getLocalPlayer().getWorldLocation();
                if (playerPoint != null)
                {
                    spikeWarningTiles.put(playerPoint, 4);
                }
            }
            handleAutoPrayers();
            log.debug("Phantom Muspah transformed from {} to {}", previousForm, newForm);
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

        if (npc.getId() == SHADOW_ORB_NPC_ID)
        {
            shadowOrbs.remove(npc);
            return;
        }

        if (npc == muspahNpc || MuspahForm.isMuspahNpc(npc.getId()))
        {
            reset();
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
        MuspahForm form = MuspahForm.fromNpcId(npc.getId());
        if (form == null && npc != muspahNpc)
        {
            return;
        }

        if (muspahNpc == null)
        {
            muspahNpc = npc;
            currentForm = form;
            instanceTimerOverlay.setTimerIfNotRunning();
        }

        int animId = npc.getAnimation();
        if (animId == MuspahSpecialAttack.ANIM_DEATH_1 || animId == MuspahSpecialAttack.ANIM_DEATH_2)
        {
            reset();
            postKillPrayerReminder = true;
            handlePostKillPrayerDisable();
            return;
        }

        handleMuspahAnimation(animId);
        handleAutoPrayers();
    }

    public void handleMuspahAnimation(int animId)
    {
        int baseSpeed = currentForm != null ? currentForm.getAttackSpeedTicks() : 5;

        if (animId == MuspahSpecialAttack.ANIM_MAGIC_ATTACK)
        {
            attackTicks = baseSpeed;
            magicAttackTicks = MuspahSpecialAttack.CHARGED_MAGIC.getDurationTicks();
            activeSpecialAttack = MuspahSpecialAttack.CHARGED_MAGIC;
            triggerAudioAlert();
        }
        else if (animId == MuspahSpecialAttack.ANIM_SHOCKWAVE)
        {
            attackTicks = MuspahSpecialAttack.CENTER_SHOCKWAVE.getDurationTicks();
            shockwaveTicks = MuspahSpecialAttack.CENTER_SHOCKWAVE.getDurationTicks();
            activeSpecialAttack = MuspahSpecialAttack.CENTER_SHOCKWAVE;
            triggerAudioAlert();
        }
        else if (animId == MuspahSpecialAttack.ANIM_SPIKE_SLAM)
        {
            attackTicks = baseSpeed;
            activeSpecialAttack = MuspahSpecialAttack.SPIKE_SLAM;
        }
        else if (animId == MuspahSpecialAttack.ANIM_TELEPORT_DISAPPEAR
                || animId == MuspahSpecialAttack.ANIM_TELEPORT_ATTACK)
        {
            attackTicks = baseSpeed;
            activeSpecialAttack = MuspahSpecialAttack.TELEPORT_ORBS;
        }
        else if (animId == MuspahSpecialAttack.ANIM_MELEE_ATTACK
                || animId == MuspahSpecialAttack.ANIM_RANGED_ATTACK)
        {
            attackTicks = baseSpeed;
        }
    }

    private void triggerAudioAlert()
    {
        if (config != null && config.soundAlerts())
        {
            Rs2SoundAlert.triggerAlert(config.soundVolume());
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        if (muspahNpc == null)
        {
            return;
        }

        GraphicsObject graphicsObject = event.getGraphicsObject();
        if (graphicsObject != null && SPIKE_WARNING_SPOTANIMS.contains(graphicsObject.getId()))
        {
            LocalPoint lp = graphicsObject.getLocation();
            if (lp != null)
            {
                WorldPoint wp = WorldPoint.fromLocal(client, lp);
                if (wp != null)
                {
                    spikeWarningTiles.put(wp, 4);
                }
            }
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

        if (STATIONARY_SPIKE_OBJECTS.contains(id))
        {
            stationarySpikes.put(gameObject, wp);
            preSpikes.entrySet().removeIf(e -> e.getValue().equals(wp));
            spikeWarningTiles.remove(wp);
        }
        else if (id == PRE_SPIKE_OBJECT_ID)
        {
            preSpikes.put(gameObject, wp);
            spikeWarningTiles.put(wp, 3);
        }
        else if (id == HOMING_SPIKE_OBJECT_ID)
        {
            homingSpikes.put(gameObject, wp);
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

        stationarySpikes.remove(gameObject);
        preSpikes.remove(gameObject);
        homingSpikes.remove(gameObject);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (muspahNpc == null)
        {
            // Clear post-kill reminder once player turns off overhead prayers
            if (postKillPrayerReminder && client.getLocalPlayer() != null && client.getLocalPlayer().getOverheadIcon() == null)
            {
                postKillPrayerReminder = false;
            }
            return;
        }

        // Refresh form in case NPC ID updated between ticks
        MuspahForm detectedForm = MuspahForm.fromNpcId(muspahNpc.getId());
        if (detectedForm != null)
        {
            currentForm = detectedForm;
        }

        totalTicks++;
        formTicks++;

        if (attackTicks > 0)
        {
            attackTicks--;
        }
        else if (attackTicks == 0)
        {
            attackTicks = -1;
        }

        if (magicAttackTicks > 0)
        {
            magicAttackTicks--;
            if (magicAttackTicks == 0)
            {
                magicAttackTicks = -1;
                if (activeSpecialAttack == MuspahSpecialAttack.CHARGED_MAGIC)
                {
                    activeSpecialAttack = null;
                }
            }
        }

        if (shockwaveTicks > 0)
        {
            shockwaveTicks--;
            if (shockwaveTicks == 0)
            {
                shockwaveTicks = -1;
                if (activeSpecialAttack == MuspahSpecialAttack.CENTER_SHOCKWAVE)
                {
                    activeSpecialAttack = null;
                }
            }
        }

        if (activeSpecialAttack == MuspahSpecialAttack.SPIKE_SLAM && attackTicks <= 1)
        {
            activeSpecialAttack = null;
        }
        else if (activeSpecialAttack == MuspahSpecialAttack.TELEPORT_ORBS && currentForm != MuspahForm.TELEPORTING && shadowOrbs.isEmpty())
        {
            activeSpecialAttack = null;
        }

        if (!spikeWarningTiles.isEmpty())
        {
            spikeWarningTiles.values().removeIf(ticks -> ticks <= 1);
            spikeWarningTiles.replaceAll((wp, ticks) -> ticks - 1);
        }

        shadowOrbs.removeIf(orb -> orb == null || orb.isDead());

        handleAutoPrayers();
        handleAutoThrall();
        handleLowPrayerAlert();
    }

    /**
     * Triggers an audio alert when the player's Prayer points drop to or below the configured threshold
     * (default: 10) during the Muspah fight. Resets immediately once the player restores Prayer above the threshold.
     */
    private void handleLowPrayerAlert()
    {
        if (config == null || client == null || client.getGameState() != GameState.LOGGED_IN || muspahNpc == null)
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

        if (shouldTriggerLowPrayerAlert(
                config.lowPrayerSoundAlert(),
                currentForm,
                muspahNpc.isDead(),
                currentPrayer,
                threshold,
                totalTicks,
                lastLowPrayerAlertTick))
        {
            Rs2SoundAlert.triggerAlert(config.soundVolume());
            lastLowPrayerAlertTick = totalTicks;
        }
    }

    /**
     * Pure helper: determines whether the low prayer audio alert should fire on the current tick.
     */
    public static boolean shouldTriggerLowPrayerAlert(
            boolean alertEnabled,
            MuspahForm form,
            boolean isMuspahDead,
            int currentPrayerPoints,
            int threshold,
            int currentTick,
            int lastAlertTick)
    {
        if (!alertEnabled || form == null || isMuspahDead)
        {
            return false;
        }
        if (currentPrayerPoints > threshold)
        {
            return false;
        }
        return currentTick - lastAlertTick >= LOW_PRAYER_ALERT_COOLDOWN_TICKS;
    }

    /**
     * Non-blocking client-thread safe Thrall summoner.
     * Invokes the Arceuus Resurrect Thrall spell directly via MenuAction.CC_OP without switching tabs or sleeping.
     */
    private void handleAutoThrall()
    {
        if (config == null || !config.autoCastThrall() || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (muspahNpc == null)
        {
            return;
        }

        boolean thrallActiveOrCooldown = client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) == 1
                || client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) >= 1;
        int spellbookVarbit = client.getVarbitValue(VarbitID.SPELLBOOK);
        boolean hasBookOfTheDead = Rs2Inventory.hasItem(ItemID.BOOK_OF_THE_DEAD)
                || Rs2Equipment.isWearing(ItemID.BOOK_OF_THE_DEAD);

        if (!shouldAttemptThrallCast(
                currentForm,
                muspahNpc.isDead(),
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
                : ThrallType.RANGED;

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

    /**
     * Pure helper: determines whether the plugin should attempt to summon a Thrall on the current tick.
     * Only summons when Muspah is actively targetable (skips dark-screen TELEPORTING phase and dead state),
     * when no Thrall is currently active or on cooldown, on the Arceuus spellbook, with Book of the dead,
     * and respecting the recast tick debounce.
     */
    public static boolean shouldAttemptThrallCast(
            MuspahForm form,
            boolean isMuspahDead,
            boolean thrallActiveOrCooldown,
            int spellbookVarbit,
            boolean hasBookOfTheDead,
            int currentTick,
            int lastCastTick)
    {
        if (form == null || form == MuspahForm.TELEPORTING || isMuspahDead)
        {
            return false;
        }
        if (thrallActiveOrCooldown)
        {
            return false;
        }
        if (spellbookVarbit != Rs2Spellbook.ARCEUUS.getValue())
        {
            return false;
        }
        if (!hasBookOfTheDead)
        {
            return false;
        }
        return currentTick - lastCastTick >= THRALL_RECAST_DEBOUNCE_TICKS;
    }

    /**
     * Pure helper: returns the Thralls for a given ThrallType ordered from highest tier (Greater) to lowest (Lesser).
     */
    public static List<Rs2Thrall> getThrallsByPriority(ThrallType type)
    {
        if (type == null)
        {
            return Collections.emptyList();
        }
        switch (type)
        {
            case RANGED:
                return Arrays.asList(Rs2Thrall.GREATER_SKELETON, Rs2Thrall.SUPERIOR_SKELETON, Rs2Thrall.LESSER_SKELETON);
            case MAGIC:
                return Arrays.asList(Rs2Thrall.GREATER_GHOST, Rs2Thrall.SUPERIOR_GHOST, Rs2Thrall.LESSER_GHOST);
            case MELEE:
                return Arrays.asList(Rs2Thrall.GREATER_ZOMBIE, Rs2Thrall.SUPERIOR_ZOMBIE, Rs2Thrall.LESSER_ZOMBIE);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Pure helper: returns the Prayer point cost required to cast the given Thrall spell.
     */
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

    /**
     * Pure helper: resolves the highest-tier Thrall of the desired style that the player has the Magic level,
     * Prayer points, and runes to cast.
     */
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

    /**
     * Pure helper: maps an Rs2Thrall enum constant to its Arceuus spellbook widget component ID.
     */
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
     * Non-blocking client-thread safe prayer switcher.
     * Uses Rs2Prayer.toggle(Rs2PrayerEnum) (single-arg overload without sleepUntil) only when the prayer state differs.
     */
    private void handleAutoPrayers()
    {
        if (config == null || client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (muspahNpc == null || muspahNpc.isDead() || currentForm == null)
        {
            return;
        }
        if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0)
        {
            return;
        }

        if (config.autoSwitchPrayers())
        {
            Prayer targetOverhead = resolveAutoOverheadPrayer(
                    currentForm,
                    magicAttackTicks,
                    attackTicks,
                    config.autoSmiteShieldPhase()
            );
            Rs2PrayerEnum targetRs2Prayer = toRs2PrayerEnum(targetOverhead);
            if (targetRs2Prayer != null
                    && client.getRealSkillLevel(Skill.PRAYER) >= targetRs2Prayer.getLevel()
                    && !client.isPrayerActive(targetOverhead))
            {
                Rs2Prayer.toggle(targetRs2Prayer);
            }
        }

        if (config.autoOffensivePrayer())
        {
            Rs2PrayerEnum bestRangePrayer = Rs2Prayer.getBestRangePrayer();
            if (bestRangePrayer != null && !Rs2Prayer.isPrayerActive(bestRangePrayer))
            {
                Rs2Prayer.toggle(bestRangePrayer);
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

    /**
     * Pure helper: resolves the target overhead prayer for auto-switching.
     * - Prioritizes Protect from Magic whenever magicAttackTicks > 0 (4-tick purple charge).
     * - During Teal Prayer Shield phase, if autoSmiteShield is enabled, flicks Smite on safe mid-cycle ticks (2..4)
     *   and switches back to Protect from Missiles before Muspah attacks.
     * - Otherwise returns the form's default protection prayer (Melee on Brown, Missiles on Green/Teal/Post-Shield).
     */
    public static Prayer resolveAutoOverheadPrayer(
            MuspahForm form,
            int magicAttackTicks,
            int attackTicks,
            boolean autoSmiteShield)
    {
        if (form == null)
        {
            return null;
        }
        if (magicAttackTicks > 0)
        {
            return Prayer.PROTECT_FROM_MAGIC;
        }
        if (form == MuspahForm.PRAYER_SHIELD && autoSmiteShield && attackTicks >= 2 && attackTicks <= 4)
        {
            return Prayer.SMITE;
        }
        return form.getDefaultPrayer();
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

    /**
     * Returns the overhead prayer the player should currently have active.
     * Automatically prioritizes Protect from Magic when Muspah charges its purple Magic attack (9918).
     */
    public Prayer getRequiredOverheadPrayer()
    {
        if (muspahNpc == null || muspahNpc.isDead() || currentForm == null)
        {
            return null;
        }
        if (magicAttackTicks > 0)
        {
            return Prayer.PROTECT_FROM_MAGIC;
        }
        return currentForm.getDefaultPrayer();
    }

    /**
     * Returns true when Muspah is in the Teal (Prayer Shield) phase and Smite prompt should be shown.
     */
    public boolean shouldPromptSmite()
    {
        return muspahNpc != null
                && !muspahNpc.isDead()
                && currentForm == MuspahForm.PRAYER_SHIELD
                && config.smiteHelper();
    }

    /**
     * Returns the center WorldPoint of Phantom Muspah's 5x5 footprint.
     */
    public WorldPoint getMuspahCenterWorldPoint()
    {
        if (muspahNpc == null)
        {
            return null;
        }
        WorldPoint sw = muspahNpc.getWorldLocation();
        if (sw == null)
        {
            return null;
        }
        int size = getMuspahSize();
        int offset = size / 2;
        return new WorldPoint(sw.getX() + offset, sw.getY() + offset, sw.getPlane());
    }

    public int getMuspahSize()
    {
        if (muspahNpc != null)
        {
            NPCComposition comp = muspahNpc.getTransformedComposition();
            if (comp != null && comp.getSize() > 0)
            {
                return comp.getSize();
            }
        }
        return MUSPAH_SIZE;
    }

    /**
     * Returns the currently computed safe tiles behind floor spikes during the center shockwave.
     */
    public Set<WorldPoint> getCurrentShockwaveSafeTiles()
    {
        WorldPoint center = getMuspahCenterWorldPoint();
        if (center == null || stationarySpikes.isEmpty())
        {
            return Collections.emptySet();
        }
        return calculateShockwaveSafeTiles(center, getMuspahSize(), stationarySpikes.values());
    }

    /**
     * Returns true if the local player is currently standing on one of the shockwave safe tiles.
     */
    public boolean isPlayerOnShockwaveSafeTile()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || localPlayer.getWorldLocation() == null)
        {
            return false;
        }
        return getCurrentShockwaveSafeTiles().contains(localPlayer.getWorldLocation());
    }

    /**
     * Pure helper: calculates safe tiles directly behind floor spikes relative to Muspah's center tile
     * during the center shockwave attack.
     *
     * @param muspahCenter the center WorldPoint of Muspah (sw + 2, sw + 2)
     * @param muspahSize   the NPC tile size (5 for Phantom Muspah)
     * @param spikeTiles   collection of active floor spike WorldPoints
     * @return set of safe WorldPoints shielded by spikes
     */
    public static Set<WorldPoint> calculateShockwaveSafeTiles(
            WorldPoint muspahCenter,
            int muspahSize,
            Collection<WorldPoint> spikeTiles)
    {
        if (muspahCenter == null || spikeTiles == null || spikeTiles.isEmpty())
        {
            return Collections.emptySet();
        }

        Set<WorldPoint> spikeSet = spikeTiles.stream()
                .filter(p -> p != null && p.getPlane() == muspahCenter.getPlane())
                .collect(Collectors.toSet());

        int halfRadius = muspahSize / 2;
        int minBossX = muspahCenter.getX() - halfRadius;
        int maxBossX = muspahCenter.getX() + halfRadius;
        int minBossY = muspahCenter.getY() - halfRadius;
        int maxBossY = muspahCenter.getY() + halfRadius;

        Set<WorldPoint> safeTiles = new LinkedHashSet<>();

        for (WorldPoint spike : spikeSet)
        {
            int dx = spike.getX() - muspahCenter.getX();
            int dy = spike.getY() - muspahCenter.getY();
            if (dx == 0 && dy == 0)
            {
                continue;
            }

            int stepX = Integer.signum(dx);
            int stepY = Integer.signum(dy);

            WorldPoint behindTile = new WorldPoint(
                    spike.getX() + stepX,
                    spike.getY() + stepY,
                    muspahCenter.getPlane()
            );

            // Must not be another spike tile and must be outside Muspah's 5x5 body
            boolean insideBoss = behindTile.getX() >= minBossX && behindTile.getX() <= maxBossX
                    && behindTile.getY() >= minBossY && behindTile.getY() <= maxBossY;

            if (!insideBoss && !spikeSet.contains(behindTile))
            {
                safeTiles.add(behindTile);
            }
        }

        return safeTiles;
    }

    /**
     * Pure helper: computes the Chebyshev tile distance from a player's tile to Muspah's NxN footprint.
     * Returns 0 if inside the footprint, 1 if directly adjacent (in melee range), 2 if 1 tile away from melee range, etc.
     */
    public static int getDistanceToMuspahFootprint(WorldPoint muspahSwTile, int npcSize, WorldPoint playerTile)
    {
        if (muspahSwTile == null || playerTile == null || muspahSwTile.getPlane() != playerTile.getPlane())
        {
            return Integer.MAX_VALUE;
        }

        int minX = muspahSwTile.getX();
        int maxX = muspahSwTile.getX() + Math.max(1, npcSize) - 1;
        int minY = muspahSwTile.getY();
        int maxY = muspahSwTile.getY() + Math.max(1, npcSize) - 1;

        int dx = 0;
        if (playerTile.getX() < minX)
        {
            dx = minX - playerTile.getX();
        }
        else if (playerTile.getX() > maxX)
        {
            dx = playerTile.getX() - maxX;
        }

        int dy = 0;
        if (playerTile.getY() < minY)
        {
            dy = minY - playerTile.getY();
        }
        else if (playerTile.getY() > maxY)
        {
            dy = playerTile.getY() - maxY;
        }

        return Math.max(dx, dy);
    }

    /**
     * Pure helper: returns true if the player is in melee danger during Brown (Melee) form
     * (distance <= 2 tiles from Muspah's 5x5 footprint, meaning in melee range or 1 step away).
     */
    public static boolean isPlayerInMeleeDanger(WorldPoint muspahSwTile, int npcSize, WorldPoint playerTile)
    {
        return getDistanceToMuspahFootprint(muspahSwTile, npcSize, playerTile) <= 2;
    }

    // Package-private setters for unit testing
    void setCurrentFormForTest(MuspahForm form)
    {
        this.currentForm = form;
    }

    void setMagicAttackTicksForTest(int ticks)
    {
        this.magicAttackTicks = ticks;
    }

    void setShockwaveTicksForTest(int ticks)
    {
        this.shockwaveTicks = ticks;
    }
}
