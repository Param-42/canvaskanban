# Building CanvasKanban

## Requirements

- **JDK 21 or newer** (the build is configured for Java 21, and `jpackage` for the
  desktop packages only exists from JDK 14 upwards)
- **Maven 3.8 or newer**
- Windows, Linux or macOS. The build downloads the JavaFX libraries for the operating
  system you are building on automatically.

You do **not** need to install JavaFX separately: Maven downloads it, and the packaged
application brings its own Java runtime with it.

## Install Java 21 and Maven

### Fedora / RHEL

```bash
sudo dnf install java-21-openjdk-devel maven
```

### Ubuntu / Debian

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven
```

### Windows

With [winget](https://learn.microsoft.com/windows/package-manager/winget/):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
```

Or download them by hand:

- JDK 21 (Temurin): <https://adoptium.net/temurin/releases/?version=21>
- Maven: <https://maven.apache.org/download.cgi> (unzip it and add its `bin` folder to `PATH`)

### Check the installation

```bash
java -version     # should print 21 (or newer)
mvn -version      # should print the Maven version and "Java version: 21"
```

If your machine has several Java versions, point Maven at JDK 21:

```bash
# Linux / macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Windows (cmd)
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.1-hotspot
```

## Run the application

From the folder that contains `pom.xml`:

```bash
mvn clean javafx:run
```

Maven compiles the code, downloads the dependencies and starts the window. The first run
takes a little longer because everything has to be downloaded.

You can also build the JAR first and then run it (this is exactly what the packaging
scripts do internally):

```bash
mvn clean package
java -cp "target/libs/*" com.canvaskanban.Launcher
```

On Windows, use `;` instead of `:` in the classpath:

```bat
java -cp "target\libs\*" com.canvaskanban.Launcher
```

## Running it from IntelliJ IDEA

The project is an ordinary Maven project, so IntelliJ needs no special setup:

1. **File > Open** and pick the folder that contains `pom.xml` (not a file inside it).
   IntelliJ imports it as a Maven project and downloads JavaFX, SQLite and JUnit.
2. **File > Project Structure > Project**: set the SDK to **JDK 21** (any JDK 21 or
   newer also works; the build compiles with `<release>21</release>`) and the language
   level to **21**. (With several JDKs installed, also check
   **Settings > Build Tools > Maven > Runner > JRE**.)
3. The JavaFX libraries come from Maven with the classifier for your operating system
   (`javafx-controls-21.0.12-linux.jar` and so on, chosen by a profile in `pom.xml`), so
   there is no JavaFX SDK to configure.

### If IntelliJ is installed as a Flatpak on Linux

Two things are different inside the sandbox; both are easy to deal with.

- **The system JDKs are not visible.** The Flatpak only gets access to your home folder,
  so `/usr/lib/jvm/...` does not exist inside it. Use the JDK that ships with the IDE
  instead: pick the entry that IntelliJ already calls *JetBrains Runtime* (its home is
  `/app/jbr`) as the project SDK. It is a full JDK (it has `javac`), so nothing else
  changes.
- **JavaFX needs the X11 socket.** The Flatpak grants the `fallback-x11` socket, which
  means X11 is only provided when Wayland is *not* in use. On a Wayland session the
  sandbox therefore has no X11 at all, and starting the window fails with
  `java.lang.UnsupportedOperationException: Unable to open DISPLAY` even though IntelliJ
  itself runs fine (its own runtime speaks Wayland). Grant the socket once and restart
  IntelliJ:

  ```bash
  flatpak override --user --socket=x11 com.jetbrains.IntelliJ-IDEA-Community
  ```

  JavaFX then connects through XWayland. To undo it later:

  ```bash
  flatpak override --user --reset com.jetbrains.IntelliJ-IDEA-Community
  ```

  Running from a normal terminal outside the sandbox (`mvn javafx:run`) is not affected by
  any of this, and neither is a non-Flatpak IntelliJ (Toolbox, tarball or distribution
  package).

Then there are two ways to start the window:

| Way | How | Notes |
| --- | --- | --- |
| Maven goal | Maven tool window > **Plugins > javafx > javafx:run**, or a Maven run configuration with the command line `javafx:run` | This is exactly what `mvn javafx:run` does, and it puts JavaFX on the module path |
| Plain Java run configuration | **Run > Edit Configurations > + > Application**, main class `com.canvaskanban.Launcher` | JavaFX is loaded from the class path here; the warning *"Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"* is expected and harmless |

Do **not** use `com.canvaskanban.MainApp` as the main class of a plain Application run
configuration: because `MainApp` extends `javafx.application.Application`, the Java
launcher refuses to start it from the class path with *"JavaFX runtime components are
missing"*. `Launcher` is a plain class that calls `MainApp.main`, which is why the
packaged application, the JAR and the tests all go through it as well.

Useful VM options for a run configuration:

```
-Dfile.encoding=UTF-8
-Dcanvaskanban.data.dir=/tmp/idea-board
```

The second one lets the program use its own database instead of the real one in your data
folder, which is handy while experimenting.

On a JDK newer than 21 the console also prints *"A restricted method in
java.lang.System has been called"* and suggests
`--enable-native-access=javafx.graphics`. It comes from JavaFX loading its native library
and is harmless; adding `--enable-native-access=ALL-UNNAMED` to the VM options silences
it.

The tests run the same way: right click `src/test/java` > **Run 'All Tests'**, or run a
single test class. The eight user interface tests need a display; on a machine without
one they skip themselves and the other 59 still run.

The `.idea` folder that IntelliJ creates is already listed in `.gitignore`, so the
project files of one person's IDE never end up in the repository.

## Run the tests

```bash
mvn test
```

There are 67 tests: the database layer, the DAOs, the service, the theme CSS files, and
eight tests that build the real user interface and check the columns, the cards, the
themes and the move logic.

The eight user interface tests need a graphics environment. On a machine without a screen
(for example a build server) they skip themselves and the other tests still run - that
also happens in the continuous integration setup, unless a virtual screen is available.

## Build the JAR

```bash
mvn clean package
```

This produces:

- `target/canvaskanban-1.0.0.jar` - the application itself. Its manifest points at
  `com.canvaskanban.Launcher`.
- `target/libs/` - the application JAR plus every runtime dependency (JavaFX, SQLite,
  SLF4J), ready to be turned into a desktop package.

## Build a real desktop package

`jpackage` (part of the JDK 21) can produce an application image that contains its own
Java runtime. Whoever installs it does **not** need Java on their machine.

> Native installers can only be built on the operating system they are meant for: an
> `.msi` on Windows, an `.rpm` on Fedora, a `.deb` on Debian/Ubuntu.

### Linux

```bash
chmod +x package.sh          # only needed once
./package.sh                 # an application image, no extra tools needed
./package.sh app-image,tar   # the application image plus a .tar.gz archive
./package.sh rpm             # a Fedora/RHEL installer, needs rpm-build
./package.sh deb             # a Debian/Ubuntu installer, needs dpkg and fakeroot
```

The scripts run `mvn clean package`, copy the application JAR and its dependencies into
`target/libs`, and then call `jpackage`. The result ends up in `target/dist`:

```bash
# start the packaged application (no Java installation required)
./target/dist/CanvasKanban/bin/CanvasKanban
```

### Windows

```bat
package.bat                  :: an application image
package.bat app-image        :: the same
package.bat msi              :: an installer, needs the WiX Toolset v3
package.bat exe              :: an installer, needs the WiX Toolset v3
```

The result ends up in `target\dist`:

```bat
target\dist\CanvasKanban\CanvasKanban.exe
```

### What ends up inside the package

```
target/dist/CanvasKanban/
  bin/CanvasKanban            the launcher of the application
  lib/app/*.jar               the application and all of its dependencies
  lib/runtime/                a private Java runtime (built with jlink)
```

The Java runtime contains `java.sql` (SQLite), `java.logging`, `java.desktop`,
`java.xml`, `java.naming`, `jdk.unsupported` and the JavaFX modules
(`javafx.base`, `javafx.graphics`, `javafx.controls`), so the application can start
without a JDK being installed. The `java.desktop` module is what allows JavaFX to draw
its own windows and load fonts.

## Continuous integration

`.github/workflows/build.yml` runs on every push and pull request:

1. a matrix job on `ubuntu-latest` and `windows-latest`,
2. Temurin JDK 21 with the Maven cache (`actions/setup-java`),
3. `mvn clean package` and then `mvn test`,
4. `package.sh app-image,tar,deb` on Linux and `package.bat app-image` on Windows,
5. the application image, the deb and the JAR are uploaded as build artifacts.

On Linux the tests run under `Xvfb` so that the eight user interface tests really
execute; if `Xvfb` is not installed they skip themselves instead of failing.

Pushing a tag such as `v1.0.0` additionally builds the installers - a Fedora RPM in a
Fedora container and a Windows MSI on a Windows runner (with the WiX Toolset v3) - and
then attaches everything to a GitHub release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Troubleshooting

**"Error: JavaFX runtime components are missing, and are required to run this
application"**
You started `com.canvaskanban.MainApp` with `java -cp`. Use `mvn javafx:run`, or start
`com.canvaskanban.Launcher` instead - that is what the scripts and the packaged
application do.

**`mvn: command not found`**
Maven is not on your `PATH`. On Fedora: `sudo dnf install maven`. On Windows, add
Maven's `bin` folder to the `PATH` environment variable.

**`jpackage: command not found`**
`jpackage` ships with the JDK, not with a JRE. Install a full JDK 21 (for example
`java-21-openjdk-devel` on Fedora) and make sure `JAVA_HOME` points at it.

**The window does not appear / `Unable to open DISPLAY`**
JavaFX needs a graphics environment. On a server without a screen, run the tests only
(`mvn test` - the user interface tests will skip themselves) and use the packaged
application on a machine with a desktop.

**`mvn clean package` fails while downloading JavaFX**
The first build needs an internet connection. Behind a proxy, configure Maven's
`settings.xml`.

**`WARNING: Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`**
Harmless. It appears because the application loads JavaFX from the classpath, which is
exactly what makes a plain `java -cp "target/libs/*" com.canvaskanban.Launcher` start
work. The window and the themes are unaffected.

## What was verified, and what was not

Verified on Linux (Fedora, XWayland) with Temurin JDK 21.0.12.1 and Maven 3.9.16, and on
the continuous integration runners on GitHub (Ubuntu and Windows, see the badge above):

- `mvn clean package` - build success, 67 tests, and the JAR plus `target/libs` are
  produced.
- `mvn test` - 67 tests pass with a display; without a display the 59 non-UI tests pass
  and the 8 UI tests skip themselves.
- The real window was driven by an operating system level robot: creating a task through
  the dialog, editing one by double clicking its card, dragging a card from *To Do* to
  *Done*, from *Done* to *In Progress* and to the top of its own column, switching the
  theme through the combo box, and deleting a task through the right click menu with its
  confirmation dialog. All of those checks passed.
- All six themes were rendered and screenshotted (the images come from those runs).
- `./package.sh app-image` produced `target/dist/CanvasKanban` with a private runtime that
  contains the JavaFX modules plus `java.sql`, `java.logging`, `java.desktop`,
  `java.xml`, `java.naming` and `jdk.unsupported`. The packaged application started,
  created its database and - after a restart - restored the same board, the same task
  order and the same theme without duplicating the example data. `./package.sh
  app-image,tar` additionally writes `target/dist/CanvasKanban-1.0.0-linux-x86_64.tar.gz`
  (146 files), which unpacks to exactly that application image.
- `./package.sh rpm` produced `canvaskanban-1.0.0-1.x86_64.rpm`: 43 MB, 137 files,
  installed under `/opt/canvaskanban`, with the usual `libX11`, `libXtst` and `alsa-lib`
  requirements of a JavaFX application. It was built inside a `Fedora 43` container,
  because this machine has no `rpmbuild` - which is also why the workflow has a Fedora
  job of its own.
- The Windows build is verified by continuous integration: on `windows-latest` the
  workflow runs `mvn clean package`, `mvn test` and `package.bat app-image`, and uploads
  the resulting application image as a build artifact.
- The Linux installers were built in CI on the `v1.0.0` tag: the `.deb` came out of the
  Ubuntu runner job, and the `.rpm` out of the Fedora container (both attached to the
  release). The new `install.sh` and the release `.tar.gz` were tested further: the
  script installs the archive into a test directory, the launcher starts through the
  `~/.local/bin` symlink (JavaFX loads and only fails to open a display, as expected) and
  `--uninstall` removes everything again.
- The Windows `.msi` was built by the new CI job with the WiX Toolset v3 and attached to
  the release. Running the built `.msi` on a real Windows machine has not been exercised
  from this Linux machine; double clicking it is the remaining verification.

Not verified here:

- macOS: the JavaFX classifier for macOS is configured in `pom.xml`, but it was not built
  here.
