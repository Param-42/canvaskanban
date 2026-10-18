<div align="center">

# CanvasKanban

[![build](https://github.com/Param-42/canvaskanban/actions/workflows/build.yml/badge.svg)](https://github.com/Param-42/canvaskanban/actions/workflows/build.yml)
[![release](https://img.shields.io/github/v/release/Param-42/canvaskanban)](https://github.com/Param-42/canvaskanban/releases/latest)
[![downloads](https://img.shields.io/github/downloads/Param-42/canvaskanban/total)](https://github.com/Param-42/canvaskanban/releases/latest)
[![tests](https://img.shields.io/badge/tests-67%20passing-success)](docs/BUILDING.md)

A small, friendly desktop Kanban board, with everything stored locally in an
SQLite file.

**No account, no server, no internet connection - start it and start moving cards.**

![CanvasKanban with the Minimal Light theme](docs/screenshots/minimal-light.png)

</div>

## Download

No Java is needed: every package carries its own runtime, and neither the install
nor the uninstall ever touches your tasks.

### One command

<div align="center">

| Platform | One command |
| --- | --- |
| **Linux** | `bash -c "$(curl -fsSL https://raw.githubusercontent.com/Param-42/canvaskanban/main/install.sh)"` |
| **Windows** | `powershell -NoProfile -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/Param-42/canvaskanban/main/install.ps1 \| iex"` |

</div>

That is all: the app is installed into your home directory and appears in your menu
(plus a Desktop shortcut on Windows). Run the same command again later to update, or
`--uninstall` (`-Uninstall`) to remove it - your tasks are kept either way.

### Installers

Prefer a classic installer? These download the newest version directly:

<div align="center">

[![Windows installer](https://img.shields.io/badge/Windows-.msi-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/Param-42/canvaskanban/releases/latest/download/CanvasKanban-windows-x64.msi)
[![Ubuntu / Debian installer](https://img.shields.io/badge/Ubuntu%20%2F%20Debian-.deb-E95420?style=for-the-badge&logo=ubuntu&logoColor=white)](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.deb)
[![Fedora / RHEL installer](https://img.shields.io/badge/Fedora%20%2F%20RHEL-.rpm-51A2DA?style=for-the-badge&logo=fedora&logoColor=white)](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.rpm)
[![Portable Linux archive](https://img.shields.io/badge/Linux-.tar.gz-FCC624?style=for-the-badge&logo=linux&logoColor=black)](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.tar.gz)

</div>

| You are on | Download | Then |
| --- | --- | --- |
| Windows | [CanvasKanban-windows-x64.msi](https://github.com/Param-42/canvaskanban/releases/latest/download/CanvasKanban-windows-x64.msi) | double click it and follow the steps |
| Ubuntu / Debian | [canvaskanban-linux-x86_64.deb](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.deb) | double click it, or `sudo apt install ./canvaskanban-linux-x86_64.deb` |
| Fedora / RHEL | [canvaskanban-linux-x86_64.rpm](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.rpm) | `sudo dnf install ./canvaskanban-linux-x86_64.rpm` |
| any Linux, portable | [canvaskanban-linux-x86_64.tar.gz](https://github.com/Param-42/canvaskanban/releases/latest/download/canvaskanban-linux-x86_64.tar.gz) | unpack and run `CanvasKanban/bin/CanvasKanban` |
| Windows, portable | [CanvasKanban-windows-x64.zip](https://github.com/Param-42/canvaskanban/releases/latest/download/CanvasKanban-windows-x64.zip) | unpack and run `CanvasKanban.exe` |

Every link above points at `releases/latest`, so they keep working for future
versions. macOS is not packaged yet - build it from source, see
[docs/BUILDING.md](docs/BUILDING.md).

## Features

- **Boards** - create, rename and delete them. A fresh install starts with one
  example board.
- **Tasks** - title, description, priority (Low / Medium / High), optional due date.
- **Drag and drop** - drag a card into another column to change its status, or up
  and down to reorder it.
- **Six themes** - and each board can have its own.
- **Local storage** - an SQLite file in your user data folder, restored on the next
  start. No cloud, no telemetry, no network access at all.

## Themes

| | | |
| --- | --- | --- |
| ![](docs/screenshots/minimal-light.png) | ![](docs/screenshots/dark-mode.png) | ![](docs/screenshots/midnight-blue.png) |
| ![](docs/screenshots/forest-green.png) | ![](docs/screenshots/lavender.png) | ![](docs/screenshots/warm-sand.png) |

## Using the board

| What you want | What to do |
| --- | --- |
| Add a task | Click **New task**, or the **+** in a column header |
| Edit a task | Double click its card |
| Move a task | Drag the card into another column |
| Change the theme | Pick one in the **Theme** box |

Run it again any time to update; your data is never touched. Everything else about
using it - the full actions list, where the database lives, backup, troubleshooting -
is in [docs/USING.md](docs/USING.md).

## Documentation

- [**docs/USING.md**](docs/USING.md) - every action on the board, where your data lives,
  troubleshooting and known limitations
- [**docs/BUILDING.md**](docs/BUILDING.md) - build from source, run the tests, package
  installers, how continuous integration works
- [**docs/ARCHITECTURE.md**](docs/ARCHITECTURE.md) - the project structure and how the
  code fits together
