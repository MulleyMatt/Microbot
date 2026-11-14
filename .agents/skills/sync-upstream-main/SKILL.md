---
name: sync-upstream-main
description: >-
  Fetches the latest upstream Microbot main branch (chsami/Microbot), updates local and origin main, merges main into boss-helper-plugin while preserving all custom "My Plugins" UI invariants, runs verification tests, and pushes so the workspace is up-to-date before starting any new work.
---

# Sync Upstream Main Skill

Use this skill to keep the `boss-helper-plugin` branch synchronized with upstream `chsami/Microbot` (`main`).

---

## 1. When to Run

- **Always run automatically before starting any new work planning or coding** in this repository, or whenever asked to sync, update, merge, or rebase with `main`.
- **Always use `git merge main` (never `git rebase`)**:
  - Merge keeps commit history append-only so `origin/boss-helper-plugin` never diverges from local history.
  - This prevents the IDE **"Sync Changes / Pull"** button from merging stale pre-rebase remote commits back on top of the branch.

---

## 2. Automated Sync Procedure

### Step 1: Check Working Tree
```powershell
git status --short
```
If uncommitted local changes exist, stash or confirm with the user before merging.

### Step 2: Fetch Upstream & Origin
```powershell
git fetch upstream main
git fetch origin
```

### Step 3: Check If Upstream Has New Commits
```powershell
git rev-list --count HEAD..upstream/main
```
- **If `0`**: Check `git rev-list --count main..upstream/main` and `git status -sb` to verify local `main` and `origin/boss-helper-plugin` are aligned. If everything is already up to date, report it and proceed immediately with the user's task.
- **If `> 0`**:
  1. Fast-forward/update local `main` and push `origin/main`:
     ```powershell
     git branch -f main upstream/main
     git push origin main
     ```
  2. Ensure `boss-helper-plugin` is checked out and merge `main`:
     ```powershell
     git checkout boss-helper-plugin
     git merge main --no-edit
     ```

---

## 3. Custom UI Invariants to Preserve on Conflict

Upstream `main` frequently updates the Microbot plugin list UI and config files. Whenever merge conflicts occur in these three files, preserve these exact invariants:

### 1. [`MicrobotPluginListPanel.java`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/ui/MicrobotPluginListPanel.java)
- **`ROWS.isBuiltIn(MicrobotPluginListItem row)`**:
  - Must return `row.isNoSwitch() && !isUserBuiltPlugin(row.getPluginConfig());` so user-built `alwaysOn` plugins (`AgentServerPlugin`, `CacheWarningFilterPlugin`) stay as top-level rows in **"My Plugins"** instead of being tucked into the collapsible `"Always on & settings"` section.
- **`isUserBuiltPlugin(MicrobotPluginConfigurationDescriptor)`**:
  - Must return `false` immediately if `descriptor == null || descriptor.getInternalPluginHubName() != null` so external Plugin Hub plugins (like `[Syn] Zulrah Overlay Helper`) never appear in **"My Plugins"**.
- **`isUserBuiltPlugin(Plugin)` & `isUserBuiltPlugin(MicrobotPluginConfigurationDescriptor)` tag check**:
  - Match `"user"` and `"userplugin"` tags **only**. **NEVER** match `"helper"` (external Hub plugins use the `"helper"` tag).
- **`PluginListMode` (`USER_PLUGINS` vs `INSTALLED_PLUGINS`) & `setMode(PluginListMode)`**:
  - Filter `currentModeList` so `USER_PLUGINS` includes `isUser || (installedTabHidden && ROWS.isBuiltIn(item))` and `INSTALLED_PLUGINS` includes `!isUser`.
- **Hub Suggestions & Health Banner**:
  - Scope `hubHits` and `healthBanner` strictly to `mode == PluginListMode.INSTALLED_PLUGINS` so **"My Plugins"** stays clean.

### 2. [`MicrobotTopLevelConfigPanel.java`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/ui/MicrobotTopLevelConfigPanel.java)
- Preserve `myPluginsTab` (`PluginListMode.USER_PLUGINS`) and `installedPluginsTab` (`PluginListMode.INSTALLED_PLUGINS`), the right-click popup menus (`setupTabPopupMenus()`), and `updateInstalledTabVisibility()` (which calls `pluginListPanel.refresh()`).
- Preserve upstream constructor dependencies (`MicrobotCloudPanel`, `MicrobotHelpMenu`, `pluginListPanel.setOpenHub(...)`).

### 3. [`MicrobotConfig.java`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/MicrobotConfig.java)
- Preserve `keyShowInstalledPluginsTab = "showInstalledPluginsTab"` and `showInstalledPluginsTab()` in `generalSection` (adjusting its `position` value if upstream added a new `@ConfigItem` in `generalSection`).

---

## 4. Post-Merge Verification & Push

After resolving any conflicts (or after any non-trivial merge):

1. **Compile & Run Custom Plugin + UI Unit Tests**:
   ```powershell
   .\gradlew.bat :client:compileJava :client:compileTestJava :client:runUnitTests --tests "net.runelite.client.plugins.microbot.ui.MicrobotPluginListPanelModeTest" --tests "net.runelite.client.plugins.microbot.ui.MicrobotPluginListPanelTest" --tests "net.runelite.client.plugins.microbot.herbrun.*" --tests "net.runelite.client.plugins.microbot.redwood.*" --tests "net.runelite.client.plugins.microbot.muspah.*" --tests "net.runelite.client.plugins.microbot.mta.*" --tests "net.runelite.client.plugins.microbot.spindel.*" --tests "net.runelite.client.plugins.microbot.wildyslayer.*" --tests "net.runelite.client.plugins.microbot.barbarianfishing.*" --tests "net.runelite.client.plugins.microbot.cachewarningfilter.*"
   ```
2. **Push Merged Branch to `origin`**:
   ```powershell
   git push origin boss-helper-plugin
   ```
   Pushing immediately keeps `origin/boss-helper-plugin` in sync so the IDE never creates conflicting pull-merges.
