@echo off
cd /d "%~dp0"

echo ===================================================================
echo               FOODBRIDGE: ONE-CLICK LAUNCHER                       
echo ===================================================================
echo.

:: 1. Check Java runtime
where java >nul 2>&1
if %ERRORLEVEL% equ 0 goto java_ok

echo [ERROR] Java runtime 'java' was not found on this computer.
echo FoodBridge requires Java Development Kit (JDK 17 or higher).
echo Please install JDK 17+ from: https://adoptium.net/
echo.
pause
exit /b 1

:java_ok

:: 2. Check Java compiler
where javac >nul 2>&1
if %ERRORLEVEL% equ 0 goto javac_ok

if not defined JAVA_HOME goto javac_missing
if not exist "%JAVA_HOME%\bin\javac.exe" goto javac_missing
set "PATH=%JAVA_HOME%\bin;%PATH%"
goto javac_ok

:javac_missing
echo [ERROR] Java compiler 'javac' was not found on this computer.
echo FoodBridge requires Java Development Kit (JDK 17 or higher).
echo Please install JDK 17+ from: https://adoptium.net/
echo Ensure javac is added to your PATH or JAVA_HOME environment variable.
echo.
pause
exit /b 1

:javac_ok

:: 3. Copy db.properties.example to db.properties ONLY if db.properties does not exist
if exist "%~dp0db.properties" goto db_ready

if not exist "%~dp0db.properties.example" goto db_example_missing
copy "%~dp0db.properties.example" "%~dp0db.properties" >nul
if %ERRORLEVEL% neq 0 goto db_copy_failed
echo [SETUP] Initialized db.properties from db.properties.example template.
goto db_ready

:db_example_missing
echo [WARN] db.properties.example template not found.
goto db_ready

:db_copy_failed
echo [ERROR] Failed to initialize db.properties from template.
pause
exit /b 1

:db_ready

:: 4. Print active database mode
set "DB_MODE=unknown"
if not exist "%~dp0db.properties" goto print_mode

for /f "tokens=1,2 delims==" %%A in ('type "%~dp0db.properties" 2^>nul ^| findstr /i "^db\.mode"') do (
    set "DB_MODE=%%B"
)
set "DB_MODE=%DB_MODE: =%"

:print_mode
echo [DATABASE] Using database mode: %DB_MODE%

:: 5. Compile application
echo.
echo [1/2] Compiling FoodBridge...
call "%~dp0compile.bat"
if %ERRORLEVEL% neq 0 goto compile_failed

:: 6. Launch GUI
echo.
echo [2/2] Launching FoodBridge GUI...
call "%~dp0run.bat" gui
set "LAUNCH_STATUS=%ERRORLEVEL%"
if %LAUNCH_STATUS% neq 0 goto run_failed
exit /b 0

:compile_failed
echo.
echo [ERROR] Compilation failed. Please check the errors above.
echo.
pause
exit /b 1

:run_failed
echo.
echo [ERROR] FoodBridge GUI exited with error code %LAUNCH_STATUS%.
pause
exit /b %LAUNCH_STATUS%
