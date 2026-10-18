#!/usr/bin/env bash
#
# CanvasKanban - builds the application and creates a distributable package.
#
# Usage:
#   ./package.sh                 # builds an app-image (works everywhere)
#   ./package.sh app-image       # same as above
#   ./package.sh rpm             # Fedora / RHEL (needs rpm-build)
#   ./package.sh deb             # Debian / Ubuntu (needs dpkg and fakeroot)
#   ./package.sh app-image,tar   # app-image plus a .tar.gz archive for sharing
#
# Notes:
#   * Every native package must be built on its own operating system.
#   * The app-image already contains a Java runtime, so the user does not have to
#     install Java to run it.
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# java, javac and jpackage all live in JAVA_HOME/bin, which is not always on the PATH
# (inside a container, for example). Adding it here means the checks below and jpackage
# itself always find the same JDK.
if [ -n "${JAVA_HOME:-}" ] && [ -d "$JAVA_HOME/bin" ]; then
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi

APP_NAME="CanvasKanban"
MAIN_CLASS="com.canvaskanban.Launcher"     # a plain class, never MainApp (see Launcher.java)
VENDOR="CanvasKanban"
DESCRIPTION="A simple desktop Kanban board built with JavaFX and SQLite."

TYPES="${1:-app-image}"
DEST_DIR="target/dist"
LIBS_DIR="target/libs"
JAVAFX_DIR="target/javafx-modules"

# ---------------------------------------------------------------------------
# 1. Find the version, so the JAR name does not have to be typed by hand.
# ---------------------------------------------------------------------------
VERSION="$(grep -m1 -oP '(?<=<version>)[^<]+' pom.xml)"
ARTIFACT="$(grep -m1 -oP '(?<=<artifactId>)[^<]+' pom.xml)"
MAIN_JAR="${ARTIFACT}-${VERSION}.jar"

echo "==> CanvasKanban ${VERSION}"
echo "==> main jar   : ${MAIN_JAR}"
echo "==> main class : ${MAIN_CLASS}"

# ---------------------------------------------------------------------------
# 2. Check the tools
# ---------------------------------------------------------------------------
command -v mvn >/dev/null 2>&1 || { echo "ERROR: Maven (mvn) was not found. Install Maven first."; exit 1; }
command -v jpackage >/dev/null 2>&1 || { echo "ERROR: jpackage was not found. It is part of JDK 21 (or newer)."; exit 1; }

# jpackage builds the private runtime with jlink, which needs the jmod files of the JDK
# that jpackage belongs to. Some distributions ship the JDK without them (on Fedora they
# are in java-21-openjdk-jmods), and jlink then falls back to the JDK image itself, which
# only works if that image was not modified by the distribution.
JPACKAGE_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v jpackage)")")")"
if [ ! -d "${JPACKAGE_HOME}/jmods" ]; then
  echo "WARNING: ${JPACKAGE_HOME} has no jmods directory."
  echo "         jlink falls back to the JDK image and may refuse to run on it."
  echo "         Install the jmods of that JDK (for example: dnf install java-21-openjdk-jmods)."
fi

# ---------------------------------------------------------------------------
# 3. Build the application (this also copies the runtime dependencies to target/libs)
# ---------------------------------------------------------------------------
echo "==> mvn clean package"
mvn clean package

if [ ! -f "target/${MAIN_JAR}" ]; then
  echo "ERROR: target/${MAIN_JAR} was not created."
  echo "       Files in target/: $(ls target/*.jar 2>/dev/null | tr '\n' ' ')"
  exit 1
fi

# ---------------------------------------------------------------------------
# 4. Collect everything that has to be inside the application.
#    The application's own JAR is needed as well - copying the dependencies only
#    would produce an application that can not start.
# ---------------------------------------------------------------------------
mkdir -p "$LIBS_DIR"
cp -f "target/${MAIN_JAR}" "$LIBS_DIR/"
echo "==> files in ${LIBS_DIR}:"
ls -1 "$LIBS_DIR"

# JavaFX has to be resolved as modules by jpackage, so the JavaFX JARs are also
# placed in their own folder. They stay in target/libs too, because the application
# itself loads them from the classpath.
mkdir -p "$JAVAFX_DIR"
rm -f "$JAVAFX_DIR"/*.jar
cp -f "$LIBS_DIR"/javafx-*.jar "$JAVAFX_DIR/"
echo "==> JavaFX modules:"
ls -1 "$JAVAFX_DIR"

# ---------------------------------------------------------------------------
# 5. Build the packages
# ---------------------------------------------------------------------------
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"

build_package() {
  local type="$1"

  if [ "$type" = "tar" ]; then
    # The archive is written next to the application image, into the same folder that
    # gets uploaded and released, so nothing has to be collected from two places.
    local archive="${DEST_DIR}/${APP_NAME}-${VERSION}-linux-$(uname -m).tar.gz"
    echo "==> creating ${archive}"
    # Built under a temporary name first, so that tar never reads the archive it writes.
    tar -C "$DEST_DIR" -czf "${archive}.tmp" "${APP_NAME}"
    mv -f "${archive}.tmp" "$archive"
    echo "    -> ${archive}"
    return
  fi

  if [ "$type" = "rpm" ] && ! command -v rpmbuild >/dev/null 2>&1; then
    echo "WARNING: 'rpm' requested but rpmbuild is missing."
    echo "         On Fedora install it with: sudo dnf install rpm-build"
    return
  fi

  if [ "$type" = "deb" ] && { ! command -v dpkg >/dev/null 2>&1 || ! command -v fakeroot >/dev/null 2>&1; }; then
    echo "WARNING: 'deb' requested but dpkg/fakeroot are missing."
    echo "         On Debian/Ubuntu install them with: sudo apt install dpkg fakeroot"
    return
  fi

  echo "==> jpackage --type ${type}"
  jpackage \
    --type "$type" \
    --name "$APP_NAME" \
    --app-version "$VERSION" \
    --vendor "$VENDOR" \
    --description "$DESCRIPTION" \
    --input "$LIBS_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --module-path "$JAVAFX_DIR" \
    --add-modules javafx.controls,java.sql,java.logging,jdk.unsupported,java.desktop,java.xml,java.naming \
    --java-options "-Dfile.encoding=UTF-8" \
    --dest "$DEST_DIR"
}

IFS=',' read -r -a REQUESTED <<< "$TYPES"
for type in "${REQUESTED[@]}"; do
  build_package "$type"
done

# ---------------------------------------------------------------------------
# 6. Report
# ---------------------------------------------------------------------------
echo
echo "==> Done. Contents of ${DEST_DIR}:"
ls -1 "$DEST_DIR"

if [ -x "${DEST_DIR}/${APP_NAME}/bin/${APP_NAME}" ]; then
  echo
  echo "Run the packaged application with:"
  echo "  ${DEST_DIR}/${APP_NAME}/bin/${APP_NAME}"
  echo "(no separate Java installation is needed, the runtime is included)"
fi
