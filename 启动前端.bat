@echo off
title VidMind Frontend (localhost:40000)
cd /d "%~dp0client"
echo Starting VidMind frontend on http://localhost:40000 ...
echo Keep this window OPEN while demoing. Press Ctrl+C to stop.
call npm run dev -- --port 40000
pause
