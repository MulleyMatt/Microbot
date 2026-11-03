# Visual & Audio Alerts in Microbot Helper Plugins

This reference details the best practices for non-intrusive, reliable screen flashing and sound notifications.

---

## 1. Audio Alerts via `Rs2SoundAlert`

### Why `Toolkit.getDefaultToolkit().beep()` Fails
Standard Java `Toolkit.getDefaultToolkit().beep()` relies on the host OS default system beep sound scheme. On Windows 10/11, system beeps are frequently muted, routed to nonexistent hardware beepers, or suppressed by audio device switches.

### Using `Rs2SoundAlert`
Microbot includes a centralized, asynchronous sound alert engine:
`net.runelite.client.plugins.microbot.util.sound.Rs2SoundAlert`

- **Features**:
  - Embedded `alert.wav` resource played via zero-latency Java Sound (`AudioPlayer` / `javax.sound.sampled`).
  - Native MP3 playback on Windows (via JNA Windows MCI `mciSendString`), macOS (`afplay`), and Linux.
  - Asynchronous background worker thread (`Microbot-Sound-Alert`) — zero client thread or render thread blocking.
  - Logarithmic volume scaling (1-100%).

### Usage Pattern
```java
// Default chime (80% volume)
Rs2SoundAlert.triggerAlert();

// Custom file override with user volume
Rs2SoundAlert.triggerAlert(config.customSoundPath(), config.soundVolume());
```

### Sound Debouncing
Always rate-limit audio playback with a repeat interval (`soundIntervalSeconds`, default ~3s):
```java
if (now.minusSeconds(repeatInterval).isAfter(lastSoundAlertTime)) {
    Rs2SoundAlert.triggerAlert();
    lastSoundAlertTime = now;
}
```

---

## 2. Screen Flash Overlays

### Overlay Settings
Configure the flash overlay with highest rendering precedence:
```java
setLayer(OverlayLayer.ALWAYS_ON_TOP);
setPosition(OverlayPosition.DYNAMIC);
setPriority(OverlayPriority.HIGHEST);
```

### Sinusoidal Alpha Pulsing
Avoid jarring strobes. A sinusoidal pulse creates an unmistakable alert that does not strain the eyes:
```java
long now = System.currentTimeMillis();
double sine = Math.sin(now * (2.0 * Math.PI / 1000.0)); // 1.0s cycle
double factor = 0.25 + 0.75 * ((sine + 1.0) / 2.0); // 0.25 to 1.0
int dynamicAlpha = (int) Math.round(baseColor.getAlpha() * factor);
dynamicAlpha = Math.max(10, Math.min(255, dynamicAlpha));

Color renderColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), dynamicAlpha);
graphics.setColor(renderColor);
graphics.fillRect(0, 0, canvasSize.width, canvasSize.height);
```

### Alert Precedence Hierarchy
When multiple alert conditions could occur simultaneously, order them strictly:
1. **Full Inventory / Critical Blockers** (Blue or Orange): Cannot continue skilling/combat until cleared.
2. **Idle / Inactivity / AFK** (Red): Stopped interacting; action required.
3. **Special Attack Ready** (Yellow): Optional boost available (e.g., Dragon axe spec at 100%).

### Immediate Dismissal UX
Suppress AFK flash immediately when `onMenuOptionClicked` fires with an action ("Chop", "Mine", "Climb", "Bank") so the user is not blinded while the character walks toward the clicked spot.
