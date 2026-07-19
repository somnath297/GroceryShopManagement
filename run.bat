@echo off
REM ================================================================
REM  Grocery Shop Management System — Quick Run Script
REM  Launches the app directly without building an installer.
REM  Useful for development and testing.
REM ================================================================

setlocal

set "MVN=C:\Users\ASUS\.maven\maven-3.9.14\bin\mvn.cmd"

if not exist "%MVN%" (
    REM Try PATH
    set "MVN=mvn"
)

echo Launching Grocery Shop Management System...
"%MVN%" javafx:run

endlocal
