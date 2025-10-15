package net.runelite.client.plugins.microbot.MullyScripts.DebugRecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PluginDescriptor(
        name = PluginDescriptor.Mully + "Debug Recorder",
        description = "Logs all player interactions (WidgetID, ItemID, ObjectID, Position, Context, Movement) in JSON",
        enabledByDefault = false
)
public class DebugRecorderPlugin extends Plugin implements KeyListener {

    @Inject private Client client;
    @Inject private KeyManager keyManager;
    @Inject private OverlayManager overlayManager;
    @Inject private DebugRecorderOverlay overlay;
    @Inject private DebugRecorderConfig config;

    private static final Logger log = LoggerFactory.getLogger(DebugRecorderPlugin.class);

    private boolean recording = false;
    private List<Map<String, Object>> interactionLog;
    private Gson gson;
    private FileWriter writer;
    private String filePath;
    private volatile String lastGeneratedPath = "";

    private final int TOGGLE_KEY = KeyEvent.VK_R; // Start/stop recording
    private final int GENERATE_KEY = KeyEvent.VK_G; // Open generator

    @Override
    protected void startUp() throws Exception {
        gson = new GsonBuilder().setPrettyPrinting().create();
        keyManager.registerKeyListener(this);
        overlayManager.add(overlay);
        interactionLog = new ArrayList<>();
        Microbot.log("Debug Recorder loaded. Press 'R' to start/stop recording. Press 'G' to generate a script.");
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(this);
        overlayManager.remove(overlay);
        stopRecording();
    }

    private void startRecording() throws IOException {
        if (recording) return;
        recording = true;
        interactionLog.clear();

        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop";
        File recordingsFolder = new File(desktopPath, "MicrobotRecordings");

        if (!recordingsFolder.exists() && recordingsFolder.mkdirs()) {
            Microbot.log("Created folder: " + recordingsFolder.getAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        filePath = recordingsFolder.getAbsolutePath() + File.separator + "Recording_" + timestamp + ".json";

        writer = new FileWriter(filePath, false);
        Microbot.log("🎥 Recording started. Saving to: " + filePath);
    }

    private void stopRecording() {
        if (!recording) return;
        recording = false;
        try {
            writer.write(gson.toJson(interactionLog));
            writer.flush();
            writer.close();
            Microbot.log("🛑 Recording stopped. Saved " + interactionLog.size() + " interactions.");
            Microbot.log("File saved: " + filePath);
        } catch (IOException e) {
            Microbot.log("Error saving recording: " + e.getMessage());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Toggle recording
        if (code == TOGGLE_KEY) {
            if (recording) stopRecording();
            else {
                try {
                    startRecording();
                } catch (IOException ex) {
                    Microbot.log("Failed to start recording: " + ex.getMessage());
                }
            }
        }

        // Open script generator (file picker)
        if (code == GENERATE_KEY) {
            SwingUtilities.invokeLater(() -> {
                try {
                    String userHome = System.getProperty("user.home");
                    File startDir = new File(userHome, "Desktop/MicrobotRecordings");
                    JFileChooser chooser = new JFileChooser(startDir);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json")); // ✅ show only JSON
                    int result = chooser.showOpenDialog(null);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selected = chooser.getSelectedFile();
                        Microbot.log("Generating script for: " + selected.getAbsolutePath());
                        String generated = RecordingScriptGenerator.generateFromJson(selected.getAbsolutePath());
                        if (generated != null) {
                            lastGeneratedPath = generated;
                            Microbot.log("✅ Generated script: " + generated);
                        } else {
                            Microbot.log("⚠️ Generation failed for: " + selected.getAbsolutePath());
                        }
                    }
                } catch (Exception ex) {
                    Microbot.log("Error opening file chooser: " + ex.getMessage());
                }
            });
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!recording || client.getGameState() != GameState.LOGGED_IN) return;

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("tick", client.getTickCount());
        record.put("timestamp", System.currentTimeMillis());
        record.put("option", event.getMenuOption());
        record.put("target", event.getMenuTarget());
        record.put("itemID", event.getItemId());
        record.put("objectID", event.getId());
        record.put("widgetID", event.getWidgetId());

        WorldPoint pos = Rs2Player.getWorldLocation();
        if (pos != null) {
            record.put("position", Map.of("x", pos.getX(), "y", pos.getY(), "plane", pos.getPlane()));
        }

        record.put("context", getContext());
        interactionLog.add(record);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!recording || client.getGameState() != GameState.LOGGED_IN) return;

        Map<String, Object> tickRecord = new LinkedHashMap<>();
        tickRecord.put("tick", client.getTickCount());
        tickRecord.put("timestamp", System.currentTimeMillis());
        tickRecord.put("event", "tick");

        WorldPoint pos = Rs2Player.getWorldLocation();
        if (pos != null) {
            tickRecord.put("position", Map.of("x", pos.getX(), "y", pos.getY(), "plane", pos.getPlane()));
        }

        tickRecord.put("context", getContext());
        interactionLog.add(tickRecord);
    }

    private Map<String, Object> getContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        try {
            // High-level activity detection
            if (Rs2Bank.isOpen()) context.put("state", "banking");
            else if (Rs2Combat.inCombat()) context.put("state", "combat");
            else if (Rs2Player.isAnimating()) context.put("state", "interacting");
            else context.put("state", "idle");

            // Movement detection
            Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer != null && localPlayer.getWorldLocation() != null) {
                WorldPoint current = localPlayer.getWorldLocation();
                LocalPoint localDest = client.getLocalDestinationLocation();
                WorldPoint dest = localDest != null ? WorldPoint.fromLocal(client, localDest) : null;

                if (dest == null || current.equals(dest)) {
                    context.put("movement", "idle");
                } else {
                    boolean running = client.getVarpValue(173) == 1; // Varp 173 = run toggle
                    context.put("movement", running ? "running" : "walking");
                    context.put("destination", Map.of(
                            "x", dest.getX(),
                            "y", dest.getY(),
                            "plane", dest.getPlane()
                    ));
                }
            }

        } catch (Exception ignored) {}
        return context;
    }

    // Overlay accessors
    public boolean isRecording() { return recording; }
    public int getRecordedCount() { return interactionLog != null ? interactionLog.size() : 0; }
    public String getLastGeneratedPath() { return lastGeneratedPath; }

    @Provides
    DebugRecorderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DebugRecorderConfig.class);
    }

    @Subscribe
    public void onConfigChanged(final ConfigChanged event){
        if (!(event.getGroup().equals(DebugRecorderConfig.configGroup))) return;
    }
}
