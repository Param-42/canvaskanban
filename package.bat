@echo off
rem ===========================================================================
rem  CanvasKanban - builds the application and creates a Windows package.
rem
rem  Usage:
rem    package.bat              builds an application image (works everywhere)
rem    package.bat app-image    the same as above
rem    package.bat msi          Windows installer, needs WiX Toolset v3
rem    package.bat exe          Windows installer, needs WiX Toolset v3
rem
rem  Notes:
rem    * Run this from the project folder.
rem    * Maven, java and jpackage have to be on the PATH (JDK 21 or newer).
rem    * The application image contains its own Java runtime, so the people who
rem      install the result do not need Java themselves.
rem    * Installers (msi, exe) must be built on Windows, they can not be built
rem      on Linux or macOS.
rem ===========================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem java, javac and jpackage all live in JAVA_HOME\bin, which is not always on the
rem PATH. Adding it here means the checks below and jpackage itself always find the
rem same JDK.
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

set APP_NAME=CanvasKanban
set MAIN_CLASS=com.canvaskanban.Launcher
set VENDOR=CanvasKanban
set DESCRIPTION=A simple desktop Kanban board built with JavaFX and SQLite.

set TYPE=%1
if "%TYPE%"=="" set TYPE=app-image

rem ---------------------------------------------------------------------------
rem 1. Read the version and the artifact name from pom.xml, so that the JAR name
rem    does not have to be typed by hand.
rem ---------------------------------------------------------------------------
for /f "usebackq delims=" %%v in (`powershell -NoProfile -Command "[xml]$p = Get-Content -Raw pom.xml; ($p.project.version).Trim()"`) do set VERSION=%%v
for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "[xml]$p = Get-Content -Raw pom.xml; ($p.project.artifactId).Trim()"`) do set ARTIFACT=%%a
set MAIN_JAR=%ARTIFACT%-%VERSION%.jar

echo === CanvasKanban %VERSION%
echo === main jar   : %MAIN_JAR%
echo === main class : %MAIN_CLASS%
echo === package    : %TYPE%

rem ---------------------------------------------------------------------------
rem 2. Check the tools
rem ---------------------------------------------------------------------------
where mvn >nul 2>nul
if errorlevel 1 (
  echo ERROR: Maven ^(mvn^) was not found. Install Maven first.
  exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
  echo ERROR: jpackage was not found. It is part of JDK 21 or newer.
  exit /b 1
)

rem ---------------------------------------------------------------------------
rem 3. Build. This also copies the runtime dependencies into target\libs.
rem ---------------------------------------------------------------------------
echo.
echo === mvn clean package
call mvn clean package
if errorlevel 1 exit /b 1

if not exist "target\%MAIN_JAR%" (
  echo ERROR: target\%MAIN_JAR% was not created.
  exit /b 1
)

rem ---------------------------------------------------------------------------
rem 4. Collect everything that has to be inside the application. The
rem    application's own JAR is needed too - copying the dependencies only
rem    would produce an application that can not start.
rem ---------------------------------------------------------------------------
if not exist "target\libs" mkdir "target\libs"
copy /y "target\%MAIN_JAR%" "target\libs\" >nul

rem JavaFX has to be resolvable as modules by jpackage, so the JavaFX JARs are
rem copied into their own folder as well. They stay in target\libs too, because
rem the application loads them from the classpath.
if not exist "target\javafx-modules" mkdir "target\javafx-modules"
del /q "target\javafx-modules\*.jar" >nul 2>nul
copy /y target\libs\javafx-*.jar target\javafx-modules\ >nul
if errorlevel 1 (
  echo ERROR: no JavaFX JARs in target\libs. Was the build successful?
  exit /b 1
)

echo === files in target\libs:
dir /b "target\libs"

rem ---------------------------------------------------------------------------
rem 5. Build the package
rem ---------------------------------------------------------------------------
if exist "target\dist" rmdir /s /q "target\dist"

echo.
echo === jpackage --type %TYPE%
jpackage ^
  --type %TYPE% ^
  --name %APP_NAME% ^
  --app-version %VERSION% ^
  --vendor %VENDOR% ^
  --description "%DESCRIPTION%" ^
  --input "target\libs" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class %MAIN_CLASS% ^
  --module-path "target\javafx-modules" ^
  --add-modules javafx.controls,java.sql,java.logging,jdk.unsupported,java.desktop,java.xml,java.naming ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --dest "target\dist"
if errorlevel 1 exit /b 1

echo.
echo === Done. The package is in target\dist
echo === Run the application with: target\dist\%APP_NAME%\%APP_NAME%.exe
endlocal
