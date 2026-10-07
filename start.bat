@echo off
setlocal
pushd "%~dp0"
if errorlevel 1 exit /b 1

call mvnw.cmd -B -ntp package
if errorlevel 1 goto failure
if /i "%~1"=="--build-only" goto success

java -jar "target\track-diagram-generator-0.1.0-SNAPSHOT.jar"
if errorlevel 1 goto failure

:success
popd
exit /b 0

:failure
popd
exit /b 1