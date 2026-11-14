---
name: create-helper-plugin
description: >-
  Guides the creation of interactive helper and overlay plugins in Microbot (for PvM, skilling, supply prep, Grand Exchange quick-filters, Bank filtering, and optional client-thread-safe micro-assists like auto-prayer, auto-thrall, anti-PK seed pod, and left-click menu swaps) where the player retains manual control over movement, combat, and core actions. Use whenever asked to create a helper, guide, or overlay plugin.
---

# Create Helper Plugin Skill (Interactive Guide & Micro-Assists)

This skill provides the architectural framework, design patterns, and code templates for creating **interactive helper plugins** in Microbot.

These plugins eliminate cognitive load—guiding the player on what supplies to withdraw, what to buy at the GE, what teleports to take, and where to click on screen—while keeping movement, positioning, target selection, eating, and banking in the player's hands, optionally paired with **opt-in, client-thread-safe micro-assists** when requested.

---

## 1. Non-Negotiable Core Rules

1. **Manual Core Control + Opt-In Micro-Assists**:
   - **NEVER** build full-script botting loops: no `Rs2Walker.walkTo(...)`, no automated combat/skilling interaction loops (`Rs2GameObject.interact(...)`, `Rs2Npc.interact(...)`), and no automated GE offers (`Rs2GrandExchange.buyItem(...)`).
   - **Opt-In Micro-Assist Toggles (Allowed when requested)**: Plugins may include configurable toggles for **Auto-Switch Prayers** (`Rs2Prayer.toggle(Rs2PrayerEnum)`), **Auto-Cast Thralls** (`MenuAction.CC_OP`), **Anti-PK Auto Royal Seed Pod** (`MenuAction.CC_OP` on `InterfaceID.Inventory.ITEMS`), and **Left-Click Menu Entry Swaps** (`@Subscribe(priority = -1) onPostMenuSort`).
   - **NEVER** call `Rs2Inventory.interact(...)` or `Rs2Magic.cast(...)` on the client thread — they switch tabs and call `sleepUntil(...)`, freezing or throwing on the client thread. Use direct `MenuAction.CC_OP` via `Microbot.doInvoke(...)` instead (see [`micro-assists-and-menu-swaps.md`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/micro-assists-and-menu-swaps.md)).
2. **Microbot Code Standards**:
   - Never instantiate caches or queryables directly — always use `Microbot.getRs2XxxCache().query()` or `.getStream()`.
   - Never block or sleep on the client thread.
   - Never use static sleeps — helper plugins rely purely on RuneLite event callbacks (`GameTick`, `ItemContainerChanged`, `ScriptCallbackEvent`, etc.).
   - Target Java 11 bytecode compatibility.
3. **Verification Before Deployment**:
   - Include unit tests verifying supply math, state logic, and item ID mappings.
   - Validate compilation via `./gradlew :client:compileJava` and run unit tests via `./gradlew :client:runUnitTests --tests <TestClass>`.
4. **Knowledge Maintenance**:
   - When new widget IDs, CS2 quirks, or entity patterns are discovered during development, document them in the appropriate modular file in `references/` (do NOT append large narratives to this root `SKILL.md`).

---

## 2. Standard 5-Pillar Architecture

Every interactive helper plugin consists of components under `net.runelite.client.plugins.microbot.<feature>`:

```
net.runelite.client.plugins.microbot.<feature>/
├── <Feature>Plugin.java              # Lifecycle, RuneLite event subscriptions, state tracking
├── <Feature>Config.java              # User settings (batch count, alert toggles, sound volume)
├── <Feature>Step.java                # Step / phase enum
├── <Feature>SupplyCalculator.java    # Scans inventory, equipment, bank cache; calculates missing items
├── <Feature>GrandExchangeInterface.java # 1-click GE search filter button
├── <Feature>BankInterface.java       # 1-click Bank item filter button
├── <Feature>Overlay.java             # HUD status panel (OverlayPanel)
├── <Feature>SceneOverlay.java        # 3D clickboxes, outlines, floating text prompts (ABOVE_SCENE)
├── <Feature>InventoryOverlay.java    # Highlights needed inventory items (ABOVE_WIDGETS)
└── <Feature>FlashOverlay.java        # Pulsing screen alerts for AFK/full inventory (ALWAYS_ON_TOP)
```

---

## 3. Reusable Templates

Production-ready templates are located in `templates/`:

- [`GrandExchangeSearchFilterTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/GrandExchangeSearchFilterTemplate.java): Safe 1-click GE filter button (prevents CS2 reentrancy crashes).
- [`BankFilterTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/BankFilterTemplate.java): Native CS2 bank layout filtering for missing supplies.
- [`ScreenFlashOverlayTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/ScreenFlashOverlayTemplate.java): Sinusoidal alpha pulsing full-screen alert paired with `Rs2SoundAlert`.
- [`SupplyCalculatorTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/SupplyCalculatorTemplate.java): Missing item math across Inventory, Equipment, and Bank.
- [`OverlayPanelTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/OverlayPanelTemplate.java): Clean HUD status panel.
- [`SceneOverlayTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/SceneOverlayTemplate.java): 3D clickboxes and model outlines.
- [`InventoryOverlayTemplate.java`](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/templates/InventoryOverlayTemplate.java): Widget highlights for target items.

---

## 4. Specialized Reference Guides

When implementing advanced features, consult the dedicated guides in `references/` instead of re-reading large source files:

- [Micro-Assists & Menu Swaps Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/micro-assists-and-menu-swaps.md): Client-thread-safe auto-prayer, Arceuus thrall casting, Anti-PK Royal Seed Pod, and left-click `onPostMenuSort` menu swapping.
- [Grand Exchange CS2 Search Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/grand-exchange-search.md): Script 750/752 lifecycle, pre-computation, and why CS2 is not reentrant.
- [Bank Filtering Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/bank-filtering.md): Scripts 276, 277, 279, `ScriptCallbackEvent` priority `-1`, and layout packing.
- [Alerts & Sound Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/alerts-and-sound.md): `Rs2SoundAlert` utility, sound debouncing, boss alert restraint, and sinusoidal screen pulse formulas.
- [State Machines & Step Tracking](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/state-machines.md): Multi-signal transitions, animation flicker debouncing, optional consumables, and region checks vs cached widgets.
- [Game Entities & Domain Gotchas](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/game-entities-and-gotchas.md): Verified object/NPC/item IDs (Redwoods, Farming, Muspah, MTA, Spindel, Barbarian Fishing, Wildy Slayer) and chat filtering.

---

## 5. Plugin Creation Workflow

0. **Sync Upstream `main` First**: Before planning or writing code for any new plugin or update, run the [`sync-upstream-main`](file:///c:/DEV/Microbot/.agents/skills/sync-upstream-main/SKILL.md) workflow so the workspace has the latest upstream `main` changes.
1. **Clarify Requirements**: Confirm activities, items, teleports, visual prompts, and any opt-in micro-assist toggles (auto-prayer, auto-thrall, anti-PK seed pod, left-click swaps) needed.
2. **Scaffold Components**: Use templates from `templates/` for GE, Bank, and Overlays, and `references/micro-assists-and-menu-swaps.md` for client-thread-safe assists.
3. **Audio Notifications**: Always use `Rs2SoundAlert.triggerAlert()`. Never use `Toolkit.getDefaultToolkit().beep()`.
4. **Registration**:
   - Always include `"user"` in `@PluginDescriptor(tags = {"user", ...})` so the plugin appears in the UI's **"My Plugins"** tab. (Do NOT rely on `"helper"` alone — external Plugin Hub helpers use `"helper"` and belong in **"Installed"**.)
   - For passive background utility plugins that should stay locked on (like `CacheWarningFilterPlugin` and `AgentServerPlugin`), set `enabledByDefault = true, alwaysOn = true` so `AlwaysOnPlugins.isLocked(...)` displays them as `"Always on"` top-level rows in **"My Plugins"**.
5. **Unit Tests**: Add tests under `runelite-client/src/test/java/...` validating supply math, state progression, and any pure micro-assist / menu-swap helpers.
6. **Compile, Test & Auto-Deploy**:
   - Run `:client:compileJava` and `:client:runUnitTests --tests ...`.
   - Once tests pass, **automatically deploy** the shaded jar (as long as `RuneLite.exe` is not running and locking `RuneLite.jar`; if it is running, notify the user to close RuneLite and run `deploy.ps1`):
     ```powershell
     powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-microbot-jar/scripts/deploy.ps1
     ```
