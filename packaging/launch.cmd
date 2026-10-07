@echo off
setlocal
cd /d "%~dp0"
java -jar "TrackDiagramGenerator.jar"
if errorlevel 1 (
  echo Java 26 or newer is required. Please check the error above.
  pause
)
