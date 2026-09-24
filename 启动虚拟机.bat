@echo off
REM Start the middleware VM (CentOS 7) in headless mode
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-vm.ps1"
pause
