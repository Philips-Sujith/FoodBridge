@echo off
cd /d "%~dp0"

:: 1. Locate Java runtime
where java >nul 2>&1
if %ERRORLEVEL% equ 0 goto use_sys_java

if exist "%~dp0jre\bin\java.exe" goto use_jre_java
if exist "%~dp0runtime\bin\java.exe" goto use_runtime_java

echo [ERROR] Java runtime was not found on this computer.
echo FoodBridge requires Java 17 or newer to run.
echo Please install Java from https://adoptium.net/
echo.
pause
exit /b 1

:use_jre_java
set "JAVA_CMD=%~dp0jre\bin\java.exe"
goto java_found

:use_runtime_java
set "JAVA_CMD=%~dp0runtime\bin\java.exe"
goto java_found

:use_sys_java
set "JAVA_CMD=java"
goto java_found

:java_found

:: 2. Check if precompiled classes exist; compile if missing
if exist "%~dp0bin\com\foodbridge\Main.class" goto app_ready

echo Compiled application files not found. Compiling first...
call "%~dp0compile.bat"
if %ERRORLEVEL% neq 0 goto compile_error

:app_ready

:: 3. Safe UTF-8 code page setup
if not exist "%SystemRoot%\System32\chcp.com" goto chcp_done
"%SystemRoot%\System32\chcp.com" 65001 >nul 2>&1
:chcp_done

:: 4. Determine Interface Mode (no argument = GUI)
if "%~1"=="" goto mode_gui
if /i "%~1"=="gui" goto mode_gui
if /i "%~1"=="console" goto mode_console

echo.
echo Choose Interface:
echo   [1] AWT Graphical Interface (GUI Mode) - Default
echo   [2] Console UI (Terminal Mode)
set "UI_CHOICE=1"
set /p "UI_CHOICE=Select (1-2, default 1): "
if "%UI_CHOICE%"=="2" goto mode_console
goto mode_gui

:mode_console
set "MAIN_CLASS=com.foodbridge.Main"
goto launch_app

:mode_gui
set "MAIN_CLASS=com.foodbridge.gui.FoodBridgeGUI"
goto launch_app

:launch_app
echo Launching %MAIN_CLASS%...
"%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp "%~dp0bin;%~dp0lib/*" %MAIN_CLASS%
set "APP_EXIT=%ERRORLEVEL%"
if %APP_EXIT% neq 0 goto launch_error
exit /b 0

:launch_error
echo.
echo [ERROR] Application exited with error code %APP_EXIT%.
pause
exit /b %APP_EXIT%

:compile_error
echo.
echo [ERROR] Automatic compilation failed.
echo.
pause
exit /b 1
