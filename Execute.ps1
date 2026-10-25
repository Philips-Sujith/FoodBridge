# FoodBridge One-Click PowerShell Launcher
# Can be run via: powershell -ExecutionPolicy Bypass -File .\Execute.ps1
$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
Set-Location $PSScriptRoot

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "              FOODBRIDGE: ONE-CLICK LAUNCHER                       " -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check java and javac
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Java runtime 'java' was not found on this computer." -ForegroundColor Red
    Write-Host "FoodBridge requires Java Development Kit (JDK 17 or higher)." -ForegroundColor Red
    Write-Host "Please install JDK 17+ from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Java compiler 'javac' was not found on this computer." -ForegroundColor Red
    Write-Host "FoodBridge requires Java Development Kit (JDK 17 or higher)." -ForegroundColor Red
    Write-Host "Please install JDK 17+ from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# 2. Check db.properties
$dbPropPath = Join-Path $PSScriptRoot "db.properties"
$dbPropExample = Join-Path $PSScriptRoot "db.properties.example"
if (-not (Test-Path $dbPropPath)) {
    Write-Host "[SETUP] 'db.properties' not found. Initializing from template..." -ForegroundColor Yellow
    if (Test-Path $dbPropExample) {
        Copy-Item -Path $dbPropExample -Destination $dbPropPath
        Write-Host "[SETUP] Created db.properties (defaulting to zero-setup embedded H2 mode)." -ForegroundColor Green
    }
}

# 3. Print database mode
$dbMode = "embedded"
if (Test-Path $dbPropPath) {
    Get-Content $dbPropPath | ForEach-Object {
        if ($_ -match "^\s*db\.mode\s*=\s*(.+)$") {
            $dbMode = $matches[1].Trim()
        }
    }
}
Write-Host "[DATABASE] Using database mode: $dbMode" -ForegroundColor Green
Write-Host ""

# 4. Compile
Write-Host "[1/2] Compiling FoodBridge source files..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "compile.ps1")
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# 5. Launch GUI
Write-Host ""
Write-Host "[2/2] Launching FoodBridge Graphical Interface (GUI)..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "run.ps1") -Mode gui
