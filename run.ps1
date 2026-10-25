param(
    [string]$Mode = ""
)

# PowerShell script to run FoodBridge
$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
Set-Location $PSScriptRoot

$javaCmd = "java"
$bundledJre = Join-Path $PSScriptRoot "jre\bin\java.exe"
$bundledRuntime = Join-Path $PSScriptRoot "runtime\bin\java.exe"

if (Test-Path $bundledJre) {
    $javaCmd = $bundledJre
} elseif (Test-Path $bundledRuntime) {
    $javaCmd = $bundledRuntime
}

if (-not (Test-Path (Join-Path $PSScriptRoot "bin\com\foodbridge\Main.class"))) {
    Write-Host "Compiled files not found. Compiling first..." -ForegroundColor Yellow
    & (Join-Path $PSScriptRoot "compile.ps1")
}

$appClass = "com.foodbridge.Main"
if ($Mode.ToLower() -eq "gui") {
    $appClass = "com.foodbridge.gui.FoodBridgeGUI"
} elseif ($Mode.ToLower() -eq "console") {
    $appClass = "com.foodbridge.Main"
} elseif ($Mode -eq "") {
    Write-Host "`nChoose Interface Mode:" -ForegroundColor Cyan
    Write-Host "  [1] Console UI (Terminal Mode) - Default"
    Write-Host "  [2] AWT Graphical Interface (GUI Mode)"
    $choice = Read-Host "Select (1-2, default 1)"
    if ($choice -eq "2") {
        $appClass = "com.foodbridge.gui.FoodBridgeGUI"
    }
}

Write-Host "Launching $appClass..." -ForegroundColor Green
& $javaCmd "-Dfile.encoding=UTF-8" -cp "bin;lib/*" $appClass

