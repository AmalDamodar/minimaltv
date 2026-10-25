#!/usr/bin/env bash
# Installs the Files app (a small remote-friendly file manager) and gives it access to the TV's storage.
#   ./scripts/05-install-files.sh              uses files/app/build/outputs/apk/release/app-release.apk if you built it,
#                                              otherwise downloads the release APK
#   ./scripts/05-install-files.sh path/to.apk  installs that file
set -eu
. "$(dirname "$0")/lib.sh"
require_device

FILES_PKG="com.minimaltv.files"
apk="${1:-}"
if [ -z "$apk" ]; then
  built="$ROOT/files/app/build/outputs/apk/release/app-release.apk"
  if [ -f "$built" ]; then
    apk="$built"
  else
    apk="$STATE/minimaltv-files.apk"
    echo "Downloading the Files app..."
    curl -fL -o "$apk" "https://github.com/AmalDamodar/minimaltv/releases/latest/download/minimaltv-files.apk" \
      || die "download failed. Build it yourself (docs/FILES.md) or pass an APK path."
  fi
fi
[ -f "$apk" ] || die "$apk not found"

echo "Installing $apk ..."
"$ADB" install -r "$apk"

# Storage access without opening a settings screen the TV may not have: the classic permissions work on Android 10 and
# older, the "all files" app-op on Android 11 and newer. Whichever does not apply is ignored.
tv pm grant "$FILES_PKG" android.permission.READ_EXTERNAL_STORAGE || true
tv pm grant "$FILES_PKG" android.permission.WRITE_EXTERNAL_STORAGE || true
tv cmd appops set "$FILES_PKG" MANAGE_EXTERNAL_STORAGE allow || true

echo
echo "Installed. Open Files from the Apps tab. To remove it: adb uninstall $FILES_PKG"
