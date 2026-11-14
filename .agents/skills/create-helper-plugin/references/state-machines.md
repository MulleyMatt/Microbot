# State Machines & Robust Step Tracking

Helper plugins guide the player through multi-phase activities (e.g. Herb Runs, Slayer tasks, Boss encounters). Fragile state tracking causes the overlay to get stuck or desync from the player's true progress.

---

## 1. Multi-Signal Advancement

Never rely on a single event type to advance state. Network latency, missed game ticks, or non-standard player actions will cause single-signal trackers to fail.

Combine multiple complementary signals:
1. **Varbits / Game State**: (e.g., farming patch stages, quest progress).
2. **Item Container Changes**: (e.g., herbs in inventory, seed count decrements, tool presence).
3. **Menu Option Clicks**: (e.g., player clicks "Harvest", "Note", or "Teleport").
4. **Chat Messages**: (e.g., "The patch is fully grown", "You dig up...", "The leprechaun notes your herbs").

### Example: Herb Patch State Machine
```
Step 1: TELEPORT_TO_PATCH (Waiting until player enters patch region)
Step 2: HARVEST_HERBS (Waiting until patch transitions to empty OR inventory fills)
Step 3: NOTE_AT_LEPRECHAUN (Triggered if herbs > 0 and patch is empty)
Step 4: PLANT_SEED (Triggered when patch is empty and seed dibber in inv)
Step 5: COMPOST_PATCH (Triggered after seed planted)
Step 6: PROCEED_NEXT_PATCH
```

---

## 2. Animation Debounce Pattern

When tracking combat or skilling activity (chopping, mining, fishing):
- Player animations can momentarily flicker to `-1` for 1 tick during resource roll checks while character is still actively engaged.
- Always enforce an idle debounce threshold (e.g. 1.8 to 2.5 seconds) before flagging the player as `IDLE_AFK`:
  ```java
  if (player.getAnimation() != -1) {
      lastActiveAnimationTime = Instant.now();
  }
  boolean isAfk = Instant.now().minusMillis(afkThresholdMs).isAfter(lastActiveAnimationTime);
  ```

---

## 3. Configurable Batching vs Single-Run

Never hardcode run counts (e.g. assuming 5 runs of supplies). Always provide a configuration setting (`runsToPurchase()` or `batchCount()`) defaulting to 1 so players with limited inventory or capital are not blocked by the supply calculator.

---

## 4. Optional Consumables Must Never Block Phase Progression

Distinguish clearly between **hard requirements** (tools, staves, required runes, quest/diary items) and **optional consumables** (food counts, stamina/prayer potion counts):
- Hard requirements block transition out of `PREPARE_SUPPLIES` until met.
- Optional consumables should be shown in the supply checklist / GE filter, **never** trap the state machine in `PREPARE_SUPPLIES` when the player brings fewer pieces of food or skips an optional potion (e.g., bringing 2 Sharks instead of 4 in MTA).

---

## 5. Strict Region Verification vs. Cached Widgets

OSRS often keeps closed room/minigame interface widgets cached in memory after a player teleports or walks out (for example, Mage Training Arena room widgets remain non-null after leaving a chamber):
- **Never** rely solely on `widget != null` or `!widget.isHidden()` to decide whether the player is inside a room.
- Always require strict `WorldPoint.getRegionID()` (and `plane`, when floors share a region ID like MTA region `13462`) checks alongside any widget checks.
- Provide a clean outside-room fallback step (e.g., `"Go to MTA & Choose a Room"`) whenever the player is not inside an active room region.
