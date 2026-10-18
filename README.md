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

<div align="center">

| Platform | One command |
| --- | --- |
| **Linux** | `bash -c "$(curl -fsSL https://raw.githubusercontent.com/Param-42/canvaskanban/main/install.sh)"` |
| **Windows** | `powershell -NoProfile -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/Param-42/canvaskanban/main/install.ps1 \| iex"` |

</div>

That is all: the app is installed into your home directory, shows up in the menu
(Start Menu and Desktop on Windows), and runs it again any time to update. Remove
it again with `--uninstall` (`-Uninstall` on Windows).

Prefer a classic installer? Take one from the
[latest release](https://github.com/Param-42/canvaskanban/releases/latest):

| You are on | Download | Install |
| --- | --- | --- |
| Fedora / RHEL | `canvaskanban-*.rpm` | `sudo dnf install ./canvaskanban-*.rpm` |
| Ubuntu / Debian | `canvaskanban_*.deb` | double click it |
| Windows | `CanvasKanban-*.msi` | double click it |
| anything Linux | `CanvasKanban-*-linux-*.tar.gz` | unpack and run `CanvasKanban/bin/CanvasKanban` |

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
