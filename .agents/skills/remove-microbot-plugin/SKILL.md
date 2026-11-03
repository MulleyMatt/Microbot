---
name: remove-microbot-plugin
description: >-
  Safely and cleanly uninstalls/removes a Microbot helper plugin or user plugin from the codebase. Removes plugin source files, corresponding unit tests, cleans UI registrations, verifies compilation, and prepares client redeployment.
---

# Remove Microbot Plugin Skill

Use this skill whenever asked to delete, uninstall, or remove a plugin from the Microbot repository (such as when the user decided to remove the Boss Helper plugin).

---

## 1. Safety Checks Before Removal

1. **Confirm Plugin Name and Package**:
   Identify the plugin directory under `runelite-client/src/main/java/net/runelite/client/plugins/microbot/<pluginFolder>/`.
2. **Scan for Inbound References**:
   Before deleting, check if other classes import or reference this plugin:
   ```powershell
   Get-ChildItem -Path "runelite-client/src" -Recurse -Include *.java | Select-String -Pattern "<PluginName>"
   ```
   Pay special attention to:
   - UI lists: [`MicrobotPluginListPanel.java`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/ui/MicrobotPluginListPanel.java)
   - Configuration wiring: Check if config groups are referenced elsewhere.
   - Unit tests: `runelite-client/src/test/java/net/runelite/client/plugins/microbot/<pluginFolder>/`

---

## 2. Removal Execution Steps

### Step 1: Remove Plugin Source Directory
Delete the plugin package:
```powershell
Remove-Item -Recurse -Force "runelite-client/src/main/java/net/runelite/client/plugins/microbot/<pluginFolder>"
```

### Step 2: Remove Corresponding Unit Tests
Delete any associated test directories or test files:
```powershell
if (Test-Path "runelite-client/src/test/java/net/runelite/client/plugins/microbot/<pluginFolder>") {
    Remove-Item -Recurse -Force "runelite-client/src/test/java/net/runelite/client/plugins/microbot/<pluginFolder>"
}
```

### Step 3: Clean Legacy UI Registrations
If the plugin was explicitly named in [`MicrobotPluginListPanel.java`](file:///c:/DEV/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/ui/MicrobotPluginListPanel.java), remove its name or class check from `isUserBuiltPluginName(...)` / `isUserBuiltPlugin(...)`. (Plugins using the automatic `"helper"` or `"user"` tags require no UI edits).

---

## 3. Post-Removal Validation

1. **Verify Clean Compilation**:
   ```powershell
   .\gradlew.bat :client:compileJava
   ```
   Ensure no missing import or class definition errors remain.

2. **Run Remaining Unit Tests**:
   ```powershell
   .\gradlew.bat :client:runUnitTests
   ```

3. **Re-deploy Shaded Jar**:
   Prompt the user or run:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-microbot-jar/scripts/deploy.ps1
   ```
   This ensures the deleted plugin is removed from the active Jagex Launcher client.
