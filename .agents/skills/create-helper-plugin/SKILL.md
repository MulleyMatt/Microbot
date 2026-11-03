---
name: create-helper-plugin
description: >-
  Guides the creation of zero-automation, interactive helper and overlay plugins in Microbot (for PvM, skilling, supply prep, Grand Exchange quick-filters, and Bank filtering) where the player retains 100% manual control over movement, combat, and actions. Use whenever asked to create a helper, guide, or overlay plugin.
---

# Create Helper Plugin Skill (Zero-Automation Interactive Guide)

This skill provides the architectural framework, design patterns, and code templates for creating **100% manual, zero-automation helper plugins** in Microbot.

These plugins eliminate cognitive load—guiding the player on what supplies to withdraw, what to buy at the GE, what teleports to take, and where to click on screen—while leaving **100% of player movement, combat, and interactions in the player's hands**.

---

## 1. Non-Negotiable Core Rules

1. **Zero Botting / Zero Automation**:
   - **NEVER** use `Rs2Walker.walkTo(...)` or movement automation.
   - **NEVER** use interaction loops (`Rs2GameObject.interact(...)`, `Rs2Npc.interact(...)`, `Rs2Inventory.interact(...)`, `Rs2Prayer.toggle(...)`).
   - **NEVER** place automated GE offers (`Rs2GrandExchange.buyItem(...)`).
   - The user physically moves the character, clicks targets, sips potions, switches prayers, and places GE offers.
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

- [Grand Exchange CS2 Search Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/grand-exchange-search.md): Script 750/752 lifecycle, pre-computation, and why CS2 is not reentrant.
- [Bank Filtering Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/bank-filtering.md): Scripts 276, 277, 279, `ScriptCallbackEvent` priority `-1`, and layout packing.
- [Alerts & Sound Guide](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/alerts-and-sound.md): `Rs2SoundAlert` utility, sound debouncing, and sinusoidal screen pulse formulas.
- [State Machines & Step Tracking](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/state-machines.md): Multi-signal transitions, animation flicker debouncing, and batch configurations.
- [Game Entities & Domain Gotchas](file:///c:/DEV/Microbot/.agents/skills/create-helper-plugin/references/game-entities-and-gotchas.md): Verified object IDs (Redwood trees, ladders, chests), farming teleports, and chat filtering.

---

## 5. Plugin Creation Workflow

1. **Clarify Requirements**: Confirm activities, items, teleports, and visual prompts needed. Reaffirm 100% manual control.
2. **Scaffold Components**: Use templates from `templates/` for GE, Bank, and Overlays.
3. **Audio Notifications**: Always use `Rs2SoundAlert.triggerAlert()`. Never use `Toolkit.getDefaultToolkit().beep()`.
4. **Registration**: Tag the plugin with `"user"` in `@PluginDescriptor(tags = {"user", ...})` so it automatically appears in the UI's "My Plugins" tab.
5. **Unit Tests**: Add tests under `runelite-client/src/test/java/...` validating supply math and state progression.
6. **Compile & Deploy**:
   ```powershell
   ./gradlew :client:compileJava
   powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-microbot-jar/scripts/deploy.ps1
   ```
