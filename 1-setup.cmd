@echo off
if not exist "%~dp0.env" copy /Y "%~dp0.env.example" "%~dp0.env" >nul
notepad.exe "%~dp0.env"
echo Save your MySQL settings, then run 2-start.cmd.
pause
