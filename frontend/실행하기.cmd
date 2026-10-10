@echo off
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js is required. Please install Node.js.
  pause
  exit /b 1
)
call npm.cmd install --no-audit --no-fund
if errorlevel 1 (
  echo Dependency installation failed.
  pause
  exit /b 1
)
call npm.cmd run dev
if errorlevel 1 pause
