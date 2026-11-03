package net.runelite.client.plugins.microbot.util.sound;

import com.sun.jna.Library;
import com.sun.jna.Native;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.util.OSType;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared audio notification system for all Microbot helper plugins.
 * <p>
 * Provides zero-latency, cross-platform alert sound playback without blocking
 * the client thread or UI rendering thread. Supports the shared default alert sound
 * as well as custom .wav and .mp3 files.
 */
@Singleton
@Slf4j
public class Rs2SoundAlert {

    private static final String DEFAULT_ALERT_RESOURCE = "alert.wav";
    private static final int DEFAULT_VOLUME = 80;

    private static final ScheduledExecutorService DEFAULT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Microbot-Sound-Alert");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile Rs2SoundAlert instance;

    @Inject
    private AudioPlayer audioPlayer;

    @Inject
    private ScheduledExecutorService injectedExecutor;

    @Getter
    private final AtomicInteger playCount = new AtomicInteger(0);

    public Rs2SoundAlert() {
        instance = this;
    }

    /**
     * Windows Multimedia API interface for playing MP3 files via JNA.
     * Loaded lazily to avoid linkage issues on non-Windows operating systems.
     */
    private interface WinMM extends Library {
        int mciSendStringA(String lpstrCommand, StringBuilder lpstrReturnString, int uReturnLength, int hwndCallback);
    }

    private static class WinMMHolder {
        private static final WinMM INSTANCE = Native.load("winmm", WinMM.class);
    }

    private static Rs2SoundAlert getOrCreateInstance() {
        if (instance == null) {
            synchronized (Rs2SoundAlert.class) {
                if (instance == null) {
                    instance = new Rs2SoundAlert();
                }
            }
        }
        return instance;
    }

    private ScheduledExecutorService getExecutor() {
        return (injectedExecutor != null && !injectedExecutor.isShutdown()) ? injectedExecutor : DEFAULT_EXECUTOR;
    }

    /**
     * Triggers the shared alert sound at default volume (80%).
     */
    public static void triggerAlert() {
        triggerAlert(DEFAULT_VOLUME);
    }

    /**
     * Triggers the shared alert sound with a custom volume percentage (1 - 100%).
     *
     * @param volumePercent Volume level between 1 and 100
     */
    public static void triggerAlert(int volumePercent) {
        triggerAlert(null, volumePercent);
    }

    /**
     * Triggers an alert sound. If {@code customPath} is non-null and points to an existing file,
     * that file is played; otherwise, the shared alert sound is played.
     *
     * @param customPath    Optional path to custom audio file (.wav or .mp3)
     * @param volumePercent Volume level between 1 and 100
     */
    public static void triggerAlert(String customPath, int volumePercent) {
        getOrCreateInstance().playAlert(customPath, volumePercent);
    }

    /**
     * Total number of times an alert sound has been triggered.
     */
    public static int getGlobalPlayCount() {
        return getOrCreateInstance().playCount.get();
    }

    /**
     * Instance method to play the default alert sound.
     */
    public void playAlert() {
        playAlert(null, DEFAULT_VOLUME);
    }

    /**
     * Instance method to play the default alert sound with specified volume.
     */
    public void playAlert(int volumePercent) {
        playAlert(null, volumePercent);
    }

    /**
     * Instance method to play an alert sound.
     */
    public void playAlert(String customPath, int volumePercent) {
        playCount.incrementAndGet();

        ScheduledExecutorService exec = getExecutor();
        exec.submit(() -> playAlertInternal(customPath, volumePercent));
    }

    private void playAlertInternal(String customPath, int volumePercent) {
        float linear = Math.max(1, Math.min(100, volumePercent)) / 100.0f;
        float gainDB = (float) Math.log10(linear) * 20.0f;

        // 1. Try playing custom sound file if provided
        if (customPath != null && !customPath.trim().isEmpty()) {
            File customFile = new File(customPath.trim());
            if (customFile.exists() && customFile.isFile()) {
                if (playCustomFile(customFile, volumePercent, gainDB)) {
                    return;
                }
            } else {
                log.warn("Custom alert sound file not found: {}", customPath);
            }
        }

        // 2. Play bundled shared alert sound
        if (playBundledSound(gainDB)) {
            return;
        }

        // 3. Fallback to system beep if audio subsystem is unavailable
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {
        }
    }

    private boolean playBundledSound(float gainDB) {
        // Try playing via RuneLite's AudioPlayer first
        if (audioPlayer != null) {
            try {
                audioPlayer.play(Rs2SoundAlert.class, DEFAULT_ALERT_RESOURCE, gainDB);
                return true;
            } catch (Throwable t) {
                log.debug("AudioPlayer play failed for shared alert: {}", t.getMessage());
            }
        }

        // Fallback: direct javax.sound.sampled Clip playback
        try (InputStream stream = Rs2SoundAlert.class.getResourceAsStream(DEFAULT_ALERT_RESOURCE)) {
            if (stream != null) {
                try (BufferedInputStream bis = new BufferedInputStream(stream);
                     AudioInputStream ais = AudioSystem.getAudioInputStream(bis)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    applyGain(clip, gainDB);
                    clip.start();
                    return true;
                }
            }
        } catch (Throwable t) {
            log.debug("Direct Clip playback failed for shared alert: {}", t.getMessage());
        }

        return false;
    }

    private boolean playCustomFile(File file, int volumePercent, float gainDB) {
        String lowerName = file.getName().toLowerCase();

        if (lowerName.endsWith(".wav")) {
            if (audioPlayer != null) {
                try {
                    audioPlayer.play(file, gainDB);
                    return true;
                } catch (Throwable t) {
                    log.debug("AudioPlayer custom file failed: {}", t.getMessage());
                }
            }
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 AudioInputStream ais = AudioSystem.getAudioInputStream(bis)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                applyGain(clip, gainDB);
                clip.start();
                return true;
            } catch (Throwable t) {
                log.warn("Could not play custom WAV file: {}", t.getMessage());
            }
        } else if (lowerName.endsWith(".mp3")) {
            return playMp3Platform(file, volumePercent);
        }

        return false;
    }

    private boolean playMp3Platform(File file, int volumePercent) {
        OSType os = OSType.getOSType();

        if (os == OSType.Windows) {
            return playMp3Windows(file, volumePercent);
        } else if (os == OSType.MacOS) {
            return playMp3Mac(file);
        } else if (os == OSType.Linux) {
            return playMp3Linux(file);
        }

        return false;
    }

    private boolean playMp3Windows(File file, int volumePercent) {
        try {
            String alias = "rs2Alert_" + System.nanoTime();
            String path = file.getAbsolutePath().replace("\\", "/");
            int openRes = WinMMHolder.INSTANCE.mciSendStringA("open \"" + path + "\" type mpegvideo alias " + alias, null, 0, 0);
            if (openRes != 0) {
                log.warn("MCI open failed with code {}", openRes);
                return false;
            }

            int mciVolume = Math.max(0, Math.min(1000, volumePercent * 10));
            WinMMHolder.INSTANCE.mciSendStringA("setaudio " + alias + " volume to " + mciVolume, null, 0, 0);
            WinMMHolder.INSTANCE.mciSendStringA("play " + alias, null, 0, 0);

            // Automatically close MCI alias after 5 seconds
            getExecutor().schedule(() -> {
                try {
                    WinMMHolder.INSTANCE.mciSendStringA("close " + alias, null, 0, 0);
                } catch (Throwable ignored) {
                }
            }, 5, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            log.warn("Error playing MP3 on Windows: {}", t.getMessage());
            return false;
        }
    }

    private boolean playMp3Mac(File file) {
        try {
            new ProcessBuilder("afplay", file.getAbsolutePath()).start();
            return true;
        } catch (Throwable t) {
            log.warn("Error playing MP3 on macOS via afplay: {}", t.getMessage());
            return false;
        }
    }

    private boolean playMp3Linux(File file) {
        String[] players = {"paplay", "mpg123", "ffplay", "aplay"};
        for (String player : players) {
            try {
                new ProcessBuilder(player, file.getAbsolutePath()).start();
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private void applyGain(Clip clip, float gainDB) {
        try {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (control != null) {
                float min = control.getMinimum();
                float max = control.getMaximum();
                float clampedGain = Math.max(min, Math.min(max, gainDB));
                control.setValue(clampedGain);
            }
        } catch (Throwable ignored) {
        }
    }
}
