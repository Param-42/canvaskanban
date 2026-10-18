<#
.SYNOPSIS
  CanvasKanban - one-command installer for Windows.

.DESCRIPTION
  Downloads the newest release, unpacks it into %LOCALAPPDATA%\Programs, and
  puts shortcuts into the Start Menu and on the Desktop. Run it again to
  update. The downloaded package brings its own Java runtime, so nothing
  else has to be installed.

  One command, from PowerShell (or download the file and run it instead):

    powershell -NoProfile -ExecutionPolicy Bypass `
      -Command "irm https://raw.githubusercontent.com/Param-42/canvaskanban/main/install.ps1 | iex"

.EXAMPLE
  ./install.ps1                  install (or update) the newest release
  ./install.ps1 -Tag v1.0.0      install a specific release
  ./install.ps1 -Uninstall       remove everything again

  Running the script over an existing installation replaces it (update).
#>
[CmdletBinding()]
param(
  # A specific release to install, for example "v1.0.0".
  # Without this the newest release is downloaded.
  [string]$Tag,

  # Skip the Desktop shortcut and only create the one in the Start Menu.
  [switch]$NoDesktopShortcut,

  # Take a downloaded release .zip (ending with windows-x64.zip) instead of
  # fetching one from GitHub, and install into a sandbox directory instead
  # of %LOCALAPPDATA%\Programs (for local testing of this script).
  [string]$Archive,
  [string]$Dest,

  [switch]$Uninstall
)

$ErrorActionPreference = 'Stop'

$Repo     = if ($env:CANVASKANBAN_REPO) { $env:CANVASKANBAN_REPO } else { 'Param-42/canvaskanban' }
$AppName  = 'CanvasKanban'
$Dest     = if ($Dest) { $Dest } else { Join-Path $env:LOCALAPPDATA 'Programs\CanvasKanban' }
$StartDir = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs'
$DeskDir  = [Environment]::GetFolderPath('Desktop')
$DataDir  = Join-Path $env:APPDATA $AppName   # the board database lives here

# ---------------------------------------------------------------------------
# -Uninstall
# ---------------------------------------------------------------------------
if ($Uninstall) {
  Write-Host "==> Removing $Dest and the shortcuts"
  Remove-Item (Join-Path $StartDir "$AppName.lnk") -ErrorAction SilentlyContinue
  Remove-Item (Join-Path $DeskDir  "$AppName.lnk") -ErrorAction SilentlyContinue
  Remove-Item $Dest -Recurse -Force -ErrorAction SilentlyContinue
  Write-Host "==> Done. Your tasks in $DataDir are kept;"
  Write-Host "    delete that folder yourself if you want to remove them too."
  return
}

# ---------------------------------------------------------------------------
# Pick the archive: a local file (-Archive, for testing) or the newest release
# from GitHub (or the release of -Tag).
# ---------------------------------------------------------------------------
$Zip = if ($Archive) {
  (Get-Item $Archive).FullName
} else {
  $api = "https://api.github.com/repos/$Repo/releases"
  $api = if ($Tag) { "$api/tags/$Tag" } else { "$api/latest" }

  Write-Host "==> Looking up the release on GitHub"
  try { $release = Invoke-RestMethod -Uri $api } catch {
    throw "Could not reach GitHub: $_"
  }

  $asset = $release.assets |
    Where-Object { $_.name -like '*windows-x64.zip' } |
    Select-Object -First 1
  if (-not $asset) { throw "No windows-x64.zip in that release." }

  Write-Host "==> Downloading $($asset.name)"
  $dir = New-Item -ItemType Directory -Path (Join-Path $env:TEMP $AppName) -Force
  $Zip = Join-Path $dir $asset.name
  Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $Zip
  $Zip
}

# ---------------------------------------------------------------------------
# Unpack and install
# ---------------------------------------------------------------------------
Write-Host "==> Installing into $Dest"
if (Test-Path $Dest) { Remove-Item $Dest -Recurse -Force }
New-Item -ItemType Directory -Path $Dest -Force | Out-Null
Expand-Archive -Path $Zip -DestinationPath $Dest -Force

# The .zip contains one folder (CanvasKanban) with CanvasKanban.exe in it.
$Exe = Join-Path $Dest "$AppName\$AppName.exe"
if (-not (Test-Path $Exe)) { throw "$Exe was not found after unpacking." }

# ---------------------------------------------------------------------------
# Shortcuts. The shell writes the .lnk files; the icon comes from the .exe
# itself, which embeds the default application icon.
# ---------------------------------------------------------------------------
Write-Host "==> Creating the shortcuts"

function New-Shortcut([string]$File, [string]$Target) {
  $shell = New-Object -ComObject WScript.Shell
  $lnk = $shell.CreateShortcut($File)
  $lnk.TargetPath = $Target
  $lnk.WorkingDirectory = (Split-Path $Target)
  $lnk.Description = 'A simple desktop Kanban board'
  $lnk.Save()
}

New-Shortcut (Join-Path $StartDir "$AppName.lnk") $Exe
if (-not $NoDesktopShortcut) {
  New-Shortcut (Join-Path $DeskDir "$AppName.lnk") $Exe
}

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
Write-Host ''
Write-Host '==> Installed.'
Write-Host '    Start it from the Start Menu (or the Desktop), or with:'
Write-Host ''
Write-Host "      & '$Exe'"
Write-Host ''
Write-Host '    You do not need Java: the package carries its own runtime.'
Write-Host '    Uninstall with: ./install.ps1 -Uninstall'
