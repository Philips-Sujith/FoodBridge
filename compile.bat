@echo off
setlocal enabledelayedexpansion

:: 1. Ensure working directory is the script directory
cd /d "%~dp0"

echo ==========================================================
echo Compiling FoodBridge Application...
echo ==========================================================

:: 2. Locate javac: prefer JDK if available
set "JAVAC_CMD="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    )
)

if not defined JAVAC_CMD (
    where javac >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set "JAVAC_CMD=javac"
    )
)

if defined JAVAC_CMD goto javac_found

echo [ERROR] JDK compiler javac was not found on this computer.
echo Please install a JDK (Java 17 or 21) from https://adoptium.net/
echo and ensure javac is available in your PATH or JAVA_HOME.
echo.
pause
exit /b 1

:javac_found

:: 3. Clean and recreate bin directory safely
if not exist "%~dp0bin" (
    mkdir "%~dp0bin" 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Could not create bin directory. Check folder permissions.
        pause
        exit /b 1
    )
)

:: 4. Generate source file list using javac argument-file (@sources_list.txt) mechanism
:: Avoids CMD wildcard expansion limits (e.g. *.java fails on Windows CMD)
set "SOURCES_FILE=%~dp0sources_list.txt"
if exist "%SOURCES_FILE%" del /f /q "%SOURCES_FILE%" 2>nul

for /r "%~dp0src" %%f in (*.java) do (
    set "FPATH=%%f"
    set "RELPATH=!FPATH:%~dp0=!"
    set "RELPATH=!RELPATH:\=/!"
    echo !RELPATH!>> "%SOURCES_FILE%"
)

if not exist "%SOURCES_FILE%" (
    echo [ERROR] No Java source files found in src directory.
    pause
    exit /b 1
)

:: 5. Compile all sources using argument file
echo Compiling source files using @sources_list.txt...
"%JAVAC_CMD%" -encoding UTF-8 -d "%~dp0bin" -cp "%~dp0lib/*" @"%SOURCES_FILE%"
set COMPILE_STATUS=%ERRORLEVEL%

:: 6. Clean up temporary source list file
if exist "%SOURCES_FILE%" del /f /q "%SOURCES_FILE%" 2>nul

if %COMPILE_STATUS% EQU 0 (
    echo.
    echo [SUCCESS] Compilation finished successfully into bin\ directory.
) else (
    echo.
    echo [ERROR] Compilation failed with exit code %COMPILE_STATUS%.
)

echo.
exit /b %COMPILE_STATUS%


