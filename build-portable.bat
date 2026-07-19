@echo off
REM ================================================================
REM  Grocery Shop Management System — Portable App Builder
REM  Creates a portable folder (no installer needed) using jpackage
REM  --type app-image. This does NOT require Wix Toolset.
REM  The resulting folder can be zipped and shared.
REM ================================================================

setlocal

echo ============================================================
echo  Building Portable App (no installer required)
echo ============================================================
echo.

REM ── Check Maven build ─────────────────────────────────────────
if not exist "target\GroceryShop.jar" (
    echo Running Maven build first...
    set "MVN=C:\Users\ASUS\.maven\maven-3.9.14\bin\mvn.cmd"
    "%MVN%" clean package -q
    if %ERRORLEVEL% NEQ 0 (
        echo Maven build FAILED. Please check for errors.
        pause
        exit /b 1
    )
    echo Maven build complete.
    echo.
)

REM ── Run jpackage for app-image ────────────────────────────────
if not exist "portable" mkdir portable

jpackage ^
    --type app-image ^
    --name "GroceryShop" ^
    --app-version "1.0.0" ^
    --input "target" ^
    --main-jar "GroceryShop.jar" ^
    --main-class "com.grocery.app.Launcher" ^
    --dest "portable" ^
    --java-options "-Xmx256m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    2>&1

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  SUCCESS! Portable app created in: portable\GroceryShop\
    echo  
    echo  To use:
    echo  1. Copy the entire 'GroceryShop' folder to the shop computer
    echo  2. Double-click GroceryShop.exe inside that folder
    echo  3. No installation required!
    echo ============================================================
) else (
    echo ERROR: Build failed with exit code %ERRORLEVEL%
)

echo.
pause
endlocal
