---
name: deploy-microbot-jar
description: >-
  Builds the latest Microbot shaded jar and deploys it to the local Jagex Launcher / RuneLite directory (%LOCALAPPDATA%\RuneLite\RuneLite.jar). Use whenever the user asks to deploy, update, install, or replace the Microbot shaded jar for the Jagex Launcher.
---

# Deploy Microbot Shaded Jar to Jagex Launcher

This skill compiles the latest codebase changes into a Microbot shaded jar (`client-1.13.2-shaded.jar`) and installs it into the local RuneLite installation path (`%LOCALAPPDATA%\RuneLite\`), allowing immediate launching directly from the official **Jagex Launcher**.

## Automated Execution

To run the automated deployment script directly:

```powershell
powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-microbot-jar/scripts/deploy.ps1
```

Optional flags:
- `-SkipBuild`: Skips `./gradlew :client:assemble` if the shaded jar has already been compiled.
- `-ForceKill`: Forcefully closes any running `RuneLite` process before copying.

---

## Step-by-Step Manual Procedure

If running steps individually or troubleshooting:

### 1. Build the Latest Shaded Jar
Ensure JDK 21 (or 17+) is active:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :client:assemble
```
The output jar will be created at `runelite-client/build/libs/client-*-shaded.jar`.

### 2. Verify Client Processes
If RuneLite is currently running, Windows file locks may prevent overwriting `RuneLite.jar`. Check running processes:
```powershell
Get-Process -Name "RuneLite", "java" -ErrorAction SilentlyContinue
```
Close the client or terminate if needed.

### 3. Ensure Official Backups Exist
Check `$env:LOCALAPPDATA\RuneLite\`:
- Back up `RuneLite.jar` to `RuneLite.jar.official.bak` if not already present.
- Back up `config.json` to `config.json.official.bak` if not already present.

### 4. Copy Shaded Jar
Copy the newly built shaded jar:
```powershell
Copy-Item "runelite-client\build\libs\client-1.13.2-shaded.jar" "$env:LOCALAPPDATA\RuneLite\RuneLite.jar" -Force
```

### 5. Verify Launcher Wiring (`config.json`)
Check `$env:LOCALAPPDATA\RuneLite\config.json` and ensure `"mainClass"` is set to `"net.runelite.client.RuneLite"`:
```json
{
  "classPath": [
    "RuneLite.jar"
  ],
  "mainClass": "net.runelite.client.RuneLite"
}
```

### 6. Verify and Launch
1. Check the timestamp and file size of `$env:LOCALAPPDATA\RuneLite\RuneLite.jar` (~68 MB).
2. Instruct the user to open the **Jagex Launcher**, select **RuneLite**, and click **Play**.
