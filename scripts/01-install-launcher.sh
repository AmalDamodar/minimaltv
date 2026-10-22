#!/usr/bin/env bash
# Installs the launcher and makes it the home app.
#   ./scripts/01-install-launcher.sh              uses launcher/app/build/outputs/apk/debug/app-debug.apk if you built it,
#                                                 otherwise downloads the latest release APK
#   ./scripts/01-install-launcher.sh path/to.apk  installs that file
set -eu
. "$(dirname "$0")/lib.sh"
require_device
[ -f "$STATE/home_before.txt" ] || "$ROOT/scripts/00-backup.sh"   # remember the original home app first

apk="${1:-}"
if [ -z "$apk" ]; then
  built="$ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
  if [ -f "$built" ]; then
    apk="$built"
  else
    apk="$STATE/minimaltv.apk"
    echo "Downloading the latest release APK..."
    curl -fL -o "$apk" "https://github.com/AmalDamodar/minimaltv/releases/latest/download/minimaltv.apk" \
      || die "download failed. Build it yourself (docs/LAUNCHER.md) or pass an APK path."
  fi
fi
[ -f "$apk" ] || die "$apk not found"

echo "Installing $apk ..."
"$ADB" install -r "$apk"
tv cmd package set-home-activity "$LAUNCHER_ACTIVITY" || true
echo
echo "Installed. Press Home on the remote. If the TV still opens its old home screen,"
echo "run ./scripts/02-debloat.sh --stock-launcher to disable it (see docs/DEBLOAT.md)."
