[CmdletBinding()]
param(
    [switch]$SkipBuild = $false,
    [switch]$ForceKill = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=== Microbot Shaded Jar Deployment ===" -ForegroundColor Cyan

# 1. Determine Project Root and Jagex/RuneLite Target Dir
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..\..\..")
if (!(Test-Path (Join-Path $projectRoot "build.gradle.kts"))) {
    if (Test-Path "c:\DEV\Microbot\build.gradle.kts") {
        $projectRoot = "c:\DEV\Microbot"
    } else {
        $projectRoot = Get-Location
    }
}
Write-Host "Project Root: $projectRoot"

$rldir = "$env:LOCALAPPDATA\RuneLite"
if (!(Test-Path $rldir)) {
    throw "RuneLite installation folder not found at: $rldir"
}

# 2. Build Shaded Jar (unless skipped)
if (-not $SkipBuild) {
    Write-Host "`n[1/4] Building latest shaded jar via Gradle..." -ForegroundColor Yellow
    $java21Path = "C:\Program Files\Java\jdk-21"
    if (Test-Path $java21Path) {
        $env:JAVA_HOME = $java21Path
        $env:PATH = "$java21Path\bin;$env:PATH"
    }

    Push-Location $projectRoot
    try {
        & .\gradlew.bat :client:assemble
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle assemble failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "`n[1/4] Skipping build (using existing jar)..." -ForegroundColor Yellow
}

# 3. Locate Shaded Jar
$buildLibs = Join-Path $projectRoot "runelite-client\build\libs"
$jarFile = Get-ChildItem -Path $buildLibs -Filter "client-*-shaded.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (!$jarFile) {
    $jarFile = Get-ChildItem -Path $buildLibs -Filter "microbot-*.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}

if (!$jarFile -or !(Test-Path $jarFile.FullName)) {
    throw "No shaded jar found in $buildLibs. Please run build without -SkipBuild."
}
Write-Host "Source Jar: $($jarFile.FullName) ($([math]::Round($jarFile.Length / 1MB, 2)) MB)"

# 4. Check Running Client Instances
$activeProcesses = Get-Process -Name "RuneLite" -ErrorAction SilentlyContinue
if ($activeProcesses) {
    if ($ForceKill) {
        Write-Host "Closing running RuneLite process..." -ForegroundColor Yellow
        $activeProcesses | Stop-Process -Force
        Start-Sleep -Seconds 1
    } else {
        Write-Host "Note: RuneLite is currently open. If the jar copy encounters a file lock, close the client first or use -ForceKill." -ForegroundColor Yellow
    }
}

# 5. Backup originals if not already backed up
Write-Host "`n[2/4] Ensuring backups exist..." -ForegroundColor Yellow
$officialJarBak = Join-Path $rldir "RuneLite.jar.official.bak"
$officialConfigBak = Join-Path $rldir "config.json.official.bak"
$targetJar = Join-Path $rldir "RuneLite.jar"
$targetConfig = Join-Path $rldir "config.json"

if ((Test-Path $targetJar) -and !(Test-Path $officialJarBak)) {
    Copy-Item $targetJar $officialJarBak -Force
    Write-Host "Created official jar backup: $officialJarBak" -ForegroundColor Green
}

if ((Test-Path $targetConfig) -and !(Test-Path $officialConfigBak)) {
    Copy-Item $targetConfig $officialConfigBak -Force
    Write-Host "Created official config backup: $officialConfigBak" -ForegroundColor Green
}

# 6. Copy Shaded Jar to Target
Write-Host "`n[3/4] Copying shaded jar to $targetJar..." -ForegroundColor Yellow
Copy-Item $jarFile.FullName $targetJar -Force

# 7. Configure config.json
Write-Host "`n[4/4] Verifying config.json launcher wiring..." -ForegroundColor Yellow
if (Test-Path $targetConfig) {
    $configContent = Get-Content $targetConfig -Raw
    if ($configContent -match '"mainClass"\s*:\s*"net.runelite.launcher.Launcher"') {
        $newContent = $configContent -replace '"mainClass"\s*:\s*"net.runelite.launcher.Launcher"', '"mainClass": "net.runelite.client.RuneLite"'
        Set-Content -Path $targetConfig -Value $newContent -Encoding UTF8
        Write-Host "Updated mainClass in config.json to net.runelite.client.RuneLite" -ForegroundColor Green
    } else {
        Write-Host "config.json mainClass already points to net.runelite.client.RuneLite" -ForegroundColor Gray
    }
}

$finalJar = Get-Item $targetJar
Write-Host "`n Deployment Complete!" -ForegroundColor Green
Write-Host "Target: $($finalJar.FullName)"
Write-Host "Size: $([math]::Round($finalJar.Length / 1MB, 2)) MB"
Write-Host "Updated: $($finalJar.LastWriteTime)"
Write-Host "`nYou can now open Jagex Launcher and click 'Play'!" -ForegroundColor Cyan
