@echo off
setlocal

rem === CONFIGURATION ===
set "PLUGIN_DIR=C:\Users\Soma\Documents\GitHub\RuneMod"
set "JAR_OUTPUT_DIR=%PLUGIN_DIR%\autobuilds"
set "LOG_FILE=%PLUGIN_DIR%\build_log.txt"
set "LAST_VER_FILE=%PLUGIN_DIR%\.last_runeLite_version"

cd /d "%PLUGIN_DIR%"
echo ============================== >> "%LOG_FILE%"
echo Build started at %date% %time% >> "%LOG_FILE%"

rem === Get RuneLite version from Gradle ===
for /f "usebackq delims=" %%V in (`gradlew.bat -q printRuneLiteVersion`) do set "LATEST_VERSION=%%V"

if not defined LATEST_VERSION (
    echo [ERROR] Failed to detect RuneLite version! >> "%LOG_FILE%"
    echo [ERROR] Failed to detect RuneLite version!
    exit /b 1
)

echo Resolved RuneLite version: %LATEST_VERSION% >> "%LOG_FILE%"
echo Detected RuneLite version: %LATEST_VERSION%

rem === Load previous version ===
set "PREV="
if exist "%LAST_VER_FILE%" (
    for /f "usebackq delims=" %%A in ("%LAST_VER_FILE%") do set "PREV=%%A"
)

rem === Compare versions ===
if "%LATEST_VERSION%"=="%PREV%" goto :nochange

rem === Save new version ===
> "%LAST_VER_FILE%" echo %LATEST_VERSION%

rem === Build ===
echo Running shadowJar build... >> "%LOG_FILE%"
echo Running shadowJar build...
call gradlew.bat clean shadowJar --refresh-dependencies --console=plain >> "%LOG_FILE%" 2>&1
if errorlevel 1 goto :buildfail

rem === Copy JAR ===
if not exist "%JAR_OUTPUT_DIR%" mkdir "%JAR_OUTPUT_DIR%"
for %%F in (build\libs\*-all.jar) do copy /Y "%%F" "%JAR_OUTPUT_DIR%\runemod-all.jar" >nul

echo ✅ Build complete for RuneLite %LATEST_VERSION% >> "%LOG_FILE%"
echo ✅ Build complete for RuneLite %LATEST_VERSION%
echo. >> "%LOG_FILE%"
goto :eof

:nochange
echo No new RuneLite version (%LATEST_VERSION%) - skipping build. >> "%LOG_FILE%"
echo No new RuneLite version - skipping build.
echo. >> "%LOG_FILE%"
goto :eof

:buildfail
echo [ERROR] Gradle build failed! >> "%LOG_FILE%"
echo Gradle build failed!
exit /b 1