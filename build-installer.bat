@echo off
REM ================================================================
REM  Grocery Shop Management System — Windows Installer Builder
REM  Run this script AFTER running: mvn clean package
REM  Requirements: JDK 21 (includes jpackage)
REM ================================================================

setlocal

echo ============================================================
echo  Grocery Shop Management System — Packaging Tool
echo ============================================================
echo.

REM ── Detect Java home ──────────────────────────────────────────
set "JAVA_HOME_DETECTED="
for /f "tokens=*" %%i in ('where java 2^>nul') do (
    set "JAVA_HOME_DETECTED=%%~dpi.."
    goto :found_java
)
:found_java

if "%JAVA_HOME%" == "" (
    if not "%JAVA_HOME_DETECTED%" == "" (
        set "JAVA_HOME=%JAVA_HOME_DETECTED%"
    ) else (
        echo ERROR: Java not found. Please install JDK 21.
        pause
        exit /b 1
    )
)

echo Java Home: %JAVA_HOME%

REM ── Check for jpackage ────────────────────────────────────────
set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
if not exist "%JPACKAGE%" (
    echo ERROR: jpackage not found at %JPACKAGE%
    echo Please ensure you have JDK 21 installed (not just JRE).
    pause
    exit /b 1
)

REM ── Check Maven build output ──────────────────────────────────
if not exist "target\GroceryShop.jar" (
    echo ERROR: target\GroceryShop.jar not found.
    echo Please run: mvn clean package
    echo Then run this script again.
    pause
    exit /b 1
)

echo Maven build output found: target\GroceryShop.jar
echo.

REM ── Create installer directory ────────────────────────────────
if not exist "installer" mkdir installer

REM ── Run jpackage ──────────────────────────────────────────────
echo Creating Windows installer...
echo This may take 2-5 minutes. Please wait...
echo.

"%JPACKAGE%" ^
    --type exe ^
    --name "GroceryShop" ^
    --app-version "1.0.0" ^
    --description "Grocery Shop Management System" ^
    --vendor "GroceryShop" ^
    --input "target" ^
    --main-jar "GroceryShop.jar" ^
    --main-class "com.grocery.app.Launcher" ^
    --dest "installer" ^
    --win-shortcut ^
    --win-menu ^
    --win-dir-chooser ^
    --win-shortcut-prompt ^
    --java-options "-Xmx256m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    2>&1

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  SUCCESS! Installer created in the 'installer' folder.
    echo  File: installer\GroceryShop-1.0.0.exe
    echo  
    echo  Share this .exe file with the shop owner.
    echo  They only need to double-click it to install.
    echo ============================================================
) else (
    echo.
    echo ============================================================
    echo  ERROR: jpackage failed with exit code %ERRORLEVEL%
    echo  
    echo  Common fixes:
    echo  1. Make sure Wix Toolset is installed for .exe packaging:
    echo     Download from https://wixtoolset.org/
    echo  2. Or change --type exe to --type app-image for a 
    echo     portable folder instead of an installer
    echo ============================================================
)

echo.
pause
endlocal
