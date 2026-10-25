# PowerShell script to compile FoodBridge
$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
Set-Location $PSScriptRoot

Write-Host "Compiling FoodBridge Application..." -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sourcesFile = Join-Path $PSScriptRoot "sources_list.txt"
$sources = Get-ChildItem -Path (Join-Path $PSScriptRoot "src") -Recurse -Filter "*.java" | ForEach-Object {
    $_.FullName.Substring($PSScriptRoot.Length + 1).Replace('\', '/')
}
[System.IO.File]::WriteAllLines($sourcesFile, [string[]]$sources, (New-Object System.Text.UTF8Encoding($false)))

$cp = "lib/*"
& javac -encoding UTF-8 -cp $cp -d bin "@$sourcesFile"
$exitCode = $LASTEXITCODE

if (Test-Path $sourcesFile) {
    Remove-Item $sourcesFile -Force -ErrorAction SilentlyContinue
}

if ($exitCode -eq 0) {
    Write-Host "[SUCCESS] Compilation finished successfully into bin/ directory." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed with code $exitCode." -ForegroundColor Red
}
exit $exitCode
