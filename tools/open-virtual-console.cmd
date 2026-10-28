@echo off
rem Opens the Pleos Connect Virtual Console (emulator v2.1.0).
rem The adb port forward is lost whenever the emulator restarts, so this re-creates it first.
adb forward tcp:8088 tcp:8088 >nul
if errorlevel 1 (
    echo adb forward failed. Is the Pleos emulator running?
    pause
    exit /b 1
)
start "" http://127.0.0.1:8088/
