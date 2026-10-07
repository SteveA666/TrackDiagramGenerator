@echo off
setlocal
pushd "%~dp0"
if errorlevel 1 goto folder_error

where javac >nul 2>&1
if errorlevel 1 goto missing_java
where java >nul 2>&1
if errorlevel 1 goto missing_java

if not exist "bin" mkdir "bin"
if not exist "bin" goto build_error

echo Compiling Track Diagram Generator...
javac -classpath "." -sourcepath "src" -d "bin" "src\Main.java"
if errorlevel 1 goto build_error

if /i "%~1"=="--build-only" goto success

echo Starting Track Diagram Generator...
java -cp "bin" Main
if errorlevel 1 goto run_error

:success
popd
exit /b 0

:missing_java
echo A Java Development Kit is required.
echo Install a JDK and ensure java and javac are available on PATH.
goto failure

:build_error
echo Compilation failed. See the error above.
goto failure

:run_error
echo The application could not run successfully. See the error above.
goto failure

:folder_error
echo Could not open the application folder.
pause
exit /b 1

:failure
popd
pause
exit /b 1
