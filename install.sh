#!/usr/bin/env bash
#
# CanvasKanban - one-command installer for Linux.
#
# Downloads the newest release, unpacks it into ~/.local, and puts the
# application into the desktop menu. Run it again to update.
#
# One command, from any directory (nothing else has to be installed - the
# downloaded package brings its own Java runtime):
#
#   bash -c "$(curl -fsSL https://raw.githubusercontent.com/${REPO}/main/install.sh)"
#
# Or download this script and run it instead:
#
#   ./install.sh              install (or update) the newest release
#   ./install.sh v1.0.0       install a specific release
#   ./install.sh --uninstall  remove everything again
#
set -euo pipefail

REPO="${CANVASKANBAN_REPO:-Param-42/canvaskanban}"
# The destination is under the user's home, so no root is needed.
DEST="${CANVASKANBAN_DEST:-${HOME}/.local/opt/canvaskanban}"
BIN_DIR="${HOME}/.local/bin"
DESKTOP_DIR="${HOME}/.local/share/applications"
ICON_DIR="${HOME}/.local/share/icons/hicolor/512x512/apps"

APP_NAME="CanvasKanban"
APP_ID="canvaskanban"

# ---------------------------------------------------------------------------
# --uninstall
# ---------------------------------------------------------------------------
if [ "${1:-}" = "--uninstall" ]; then
  echo "==> Removing ${DEST}, the menu entry and the command"
  rm -f  "${BIN_DIR}/${APP_ID}"
  rm -f  "${DESKTOP_DIR}/${APP_ID}.desktop"
  rm -f  "${ICON_DIR}/${APP_ID}.png"
  rm -rf "${DEST}"
  echo "==> Done. The board database in ~/.local/share/canvaskanban is kept;"
  echo "    delete it yourself if you want to remove your data as well."
  exit 0
fi

# ---------------------------------------------------------------------------
# Pick the archive: a local file (CANVASKANBAN_ARCHIVE, for testing) or the
# newest release from GitHub (or the release of a tag passed as an argument).
# ---------------------------------------------------------------------------
ARCH="$(uname -m)"
TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT

if [ -n "${CANVASKANBAN_ARCHIVE:-}" ]; then
  # For local testing: unpack a downloaded archive instead of fetching one.
  cp -f "$CANVASKANBAN_ARCHIVE" "${TMP}/${APP_NAME}.tar.gz"
else
  if command -v curl >/dev/null 2>&1; then
    FETCH() { curl -fsSL "$1" -o "$2"; }
  elif command -v wget >/dev/null 2>&1; then
    FETCH() { wget -qO "$2" "$1"; }
  else
    echo "ERROR: install curl or wget first." >&2
    exit 1
  fi

  api="https://api.github.com/repos/${REPO}/releases"
  if [ -n "${1:-}" ]; then api="${api}/tags/$1"; else api="${api}/latest"; fi

  echo "==> Looking up the release on GitHub"
  JSON="${TMP}/release.json"
  if ! FETCH "$api" "$JSON"; then
    echo "ERROR: could not reach GitHub. (offline?)"
    exit 1
  fi

  grep -m1 -o '"browser_download_url": *"[^"]*linux-'"${ARCH}"'\.tar\.gz"' "$JSON" \
    | cut -d'"' -f4 > "${TMP}/url" || true
  if [ ! -s "${TMP}/url" ]; then
    echo "ERROR: no linux-${ARCH} archive in that release."
    exit 1
  fi

  URL="$(xargs < "${TMP}/url")"
  echo "==> Downloading ${URL}"
  FETCH "$URL" "${TMP}/${APP_NAME}.tar.gz"
fi

# ---------------------------------------------------------------------------
# Unpack and install
# ---------------------------------------------------------------------------
echo "==> Installing into ${DEST}"
rm -rf "$DEST"
mkdir -p "$DEST" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_DIR"
tar -xzf "${TMP}/${APP_NAME}.tar.gz" -C "$TMP"
mv "${TMP}/${APP_NAME}" "$DEST/"

# The launcher is a small program, and it works out of the box: it provides its
# own subfolder with the runtime. So all that is left is the links.
LAUNCHER="$DEST/${APP_NAME}/bin/${APP_NAME}"
chmod +x "$LAUNCHER" 2>/dev/null || true

# A command that works from any shell, for people who run programs from a
# terminal. The launcher locates its own folder, so a symlink is fine.
#
# (jpackage does the same in the system-wide .deb and .rpm packages.)
ln -sf "$LAUNCHER" "${BIN_DIR}/${APP_ID}"

# The menu entry: this is what GNOME / KDE / Xfce / ... show in their
# application lists.
cp -f "$DEST/${APP_NAME}/lib/${APP_NAME}.png" "${ICON_DIR}/${APP_ID}.png" 2>/dev/null || true
cat > "${DESKTOP_DIR}/${APP_ID}.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=${APP_NAME}
GenericName=Kanban board
Comment=A simple desktop Kanban board
Exec=${LAUNCHER}
Icon=${ICON_DIR}/${APP_ID}.png
Terminal=false
Categories=Office;ProjectManagement;Utility;
StartupWMClass=${APP_NAME}
EOF

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
echo
echo "==> Installed."
echo "    Start it from the menu of your desktop (Graphics / Office), or with:"
echo
echo "      ${BIN_DIR}/${APP_ID}"
echo
echo "    If '${APP_ID}' can not be found, log out and in again, or add this"
echo "    line to your ~/.bashrc:"
echo "      export PATH=\"\$HOME/.local/bin:\$PATH\""
echo
echo "    Uninstall with: ${0##*/} --uninstall"
