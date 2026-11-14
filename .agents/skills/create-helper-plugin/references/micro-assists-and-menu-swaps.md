# Client-Thread-Safe Micro-Assists & Menu Swaps

Helper plugins keep movement, positioning, target selection, eating, and banking in the player's hands while offering **opt-in, configurable micro-assist toggles** (auto-prayers, auto-thralls, anti-PK emergency seed pod, and left-click menu swaps).

---

## Why Standard `Rs2Inventory.interact` & `Rs2Magic.cast` Fail on the Client Thread

Helper plugins react instantaneously inside `@Subscribe onGameTick`, `onPlayerSpawned`, or `onAnimationChanged` callbacks—all of which execute on RuneLite's **Client Thread**.

- **NEVER** call `Rs2Inventory.interact(...)` or `Rs2Magic.cast(...)` on the client thread: they call `Rs2Tab.switchTo(...)` which invokes `Global.sleepUntil(...)`, causing client-thread blocking errors or freezing the client.
- **Instead**, either invoke the target widget directly via `Microbot.doInvoke(new NewMenuEntry()..., bounds)` with `MenuAction.CC_OP` (which works regardless of which side-panel tab is currently open), or call the non-blocking single-argument `Rs2Prayer.toggle(Rs2PrayerEnum)`.

---

## Pattern 1: Client-Thread-Safe Auto-Prayer Switching

Used in [`MuspahPlugin`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/muspah/MuspahPlugin.java#L870-L959) and `SpindelPlugin`:

- Always guard with `client.getBoostedSkillLevel(Skill.PRAYER) > 0`, `client.getRealSkillLevel(Skill.PRAYER) >= targetRs2Prayer.getLevel()`, and `!client.isPrayerActive(targetOverhead)`.
- Call single-argument `Rs2Prayer.toggle(targetRs2Prayer)` (no `sleepUntil`).
- Provide clear config toggles:
  - `autoSwitchPrayers()` (overhead protection)
  - `autoOffensivePrayer()` (e.g., `Rs2Prayer.getBestRangePrayer()`)
  - `autoDisablePrayersOnKill()` (loops `Rs2PrayerEnum.values()` when boss dies/despawns to stop prayer drain while looting)

```java
// Extract pure static decision helper for unit testing:
Prayer targetOverhead = resolveAutoOverheadPrayer(currentForm, magicAttackTicks, attackTicks, config.autoSmiteShieldPhase());
Rs2PrayerEnum targetRs2Prayer = toRs2PrayerEnum(targetOverhead);
if (targetRs2Prayer != null
        && client.getRealSkillLevel(Skill.PRAYER) >= targetRs2Prayer.getLevel()
        && !client.isPrayerActive(targetOverhead)) {
    Rs2Prayer.toggle(targetRs2Prayer);
}
```

---

## Pattern 2: Client-Thread-Safe Arceuus Thrall Summoning

Used in [`MuspahPlugin`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/muspah/MuspahPlugin.java#L653-L864):

1. Check varbits & gear:
   - `client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) == 0`
   - `client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) == 0`
   - `client.getVarbitValue(VarbitID.SPELLBOOK) == Rs2Spellbook.ARCEUUS.getValue()`
   - `Rs2Inventory.hasItem(ItemID.BOOK_OF_THE_DEAD) || Rs2Equipment.isWearing(ItemID.BOOK_OF_THE_DEAD)`
   - Enforce a 3-tick recast debounce (`THRALL_RECAST_DEBOUNCE_TICKS = 3`) and skip untargetable boss phases (e.g., `MuspahForm.TELEPORTING`).
2. Resolve the highest unlocked tier (`GREATER` -> `SUPERIOR` -> `LESSER`) checking boosted Magic level, Prayer cost (`6` / `4` / `2`), and `Rs2Magic::hasRequiredRunes`.
3. Cast via `Microbot.doInvoke` with `MenuAction.CC_OP` (no tab switch):

```java
int componentId = thrallToComponentId(targetThrall); // e.g., InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON
Rectangle bounds = Rs2UiHelper.getDefaultRectangle();
Widget spellWidget = client.getWidget(componentId);
if (spellWidget != null && spellWidget.getBounds() != null
        && spellWidget.getBounds().width > 0 && spellWidget.getBounds().height > 0) {
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
```

---

## Pattern 3: Anti-PK Detection & Auto Royal Seed Pod (`<= 30` Wildy)

Used in `SpindelPlugin` and [`WildySlayerPlugin`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/wildyslayer/WildySlayerPlugin.java#L157-L540):

1. Subscribe to **both** `@Subscribe onPlayerSpawned(PlayerSpawned)` (same-tick reaction) and `@Subscribe onGameTick(GameTick)`.
2. Check Wilderness status & level (`InterfaceID.PvpIcons.WILDERNESSLEVEL` text with `Rs2Pvp.getWildernessLevelFrom(worldPoint)` fallback) and ensure `!isTeleblocked()` (`client.getVarbitValue(VarbitID.TELEBLOCK_CYCLES) == 0`).
3. Evaluate threat players via combat bracket math (accounting for `localWildernessLevel`, `otherWildernessLevel`, PvP worlds `+15`, and `ignoreFriendsAndClan`).
4. Locate `ItemID.MM2_ROYAL_SEED_POD` in `InventoryID.INVENTORY` and invoke `"Commune"` directly on `InterfaceID.Inventory.ITEMS` with a 3-tick cooldown:

```java
int identifier = 2; // Standard CC_OP identifier for iop1 ("Commune") on InterfaceID.Inventory.ITEMS
Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
// Optionally scan podWidget.getActions() for "Commune" -> identifier = idx + 1;

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
```

---

## Pattern 4: Left-Click Menu Entry Swapping (`onPostMenuSort`)

Used in [`BarbarianFishingPlugin`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/barbarianfishing/BarbarianFishingPlugin.java#L452-L515):

- Register `@Subscribe(priority = -1)` on `PostMenuSort` so it runs **after** RuneLite's `MenuEntrySwapperPlugin` (priority `0`).
- Always guard against open right-click menus and active spell/item selection (`client.isMenuOpen() || client.isWidgetSelected()`).
- Verify the entry belongs to `InterfaceID.INVENTORY` via `WidgetUtil.componentToInterface(widget.getId()) == InterfaceID.INVENTORY`.
- Swap the matching entry with `menuEntries[menuEntries.length - 1]`, upgrade `MenuAction.CC_OP_LOW_PRIORITY` to `MenuAction.CC_OP` (otherwise low-priority ops like `Drop` do not become left-click), and call `root.setMenuEntries(entries)`.

```java
@Subscribe(priority = -1)
public void onPostMenuSort(PostMenuSort event) {
    if (!config.leftClickDropFish() || client.isMenuOpen() || client.isWidgetSelected()) {
        return;
    }
    swapDropToTopIfTarget(client.getMenu());
}
```
