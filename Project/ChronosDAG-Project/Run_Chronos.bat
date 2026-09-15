@echo off
title ChronosDAG Launcher
color 0B

echo ==========================================
echo        ChronosDAG Project Builder
echo ==========================================
echo.
echo Compiling the latest Java files...
javac src\*.java src\dsa\*.java src\ui\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed! Please check your code.
    pause
    exit /b
)

echo Compilation successful! Launching application...
echo.
java -cp src Main

exit