#!/usr/bin/env bash
# Shared helpers. Source this file; do not run it.
#
# Environment:
#   ADB            adb binary to use (default: adb)
#   ANDROID_SERIAL device to talk to, e.g. 192.168.1.50:5555 (needed when more than one device is connected)
#   MINIMALTV_STATE  where backups and the restore log are kept (default: ~/.minimaltv)

ADB="${ADB:-adb}"
STATE="${MINIMALTV_STATE:-$HOME/.minimaltv}"
LAUNCHER_PKG="com.minimaltv.home"
LAUNCHER_ACTIVITY="com.minimaltv.home/.MainActivity"
KEY_SERVICE="com.minimaltv.home/com.minimaltv.home.KeyService"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

die() { echo "error: $*" >&2; exit 1; }

require_device() {
  command -v "$ADB" >/dev/null 2>&1 || die "adb not found. Install Android platform-tools, or set ADB=/path/to/adb (see docs/ADB.md)."
  local state
  state="$("$ADB" get-state 2>/dev/null || true)"
  [ "$state" = "device" ] || die "no single TV connected. Run: adb connect <tv-ip>:5555 (see docs/INSTALL.md). If several devices are connected, set ANDROID_SERIAL=<tv-ip>:5555 first."
  mkdir -p "$STATE"
}

tv() { "$ADB" shell "$@" </dev/null | tr -d "\r"; }   # </dev/null: adb shell would otherwise eat the stdin of the surrounding while-read loops

is_installed() { tv pm list packages | grep -qx "package:$1"; }
is_disabled()  { tv pm list packages -d | grep -qx "package:$1"; }

# Print the package names in a packages/*.txt file (comments and blank lines removed).
list_packages() { sed -e 's/#.*//' -e 's/[[:space:]]*$//' -e '/^$/d' "$1"; }

# Lower-case manufacturer and brand of the connected TV, e.g. "xiaomi".
device_brand() { printf '%s %s' "$(tv getprop ro.product.manufacturer)" "$(tv getprop ro.product.brand)" | tr 'A-Z' 'a-z'; }

# Which brand profile (a folder in packages/) fits this TV. Prints nothing when there is none.
detect_profile() {
  local b; b="$(device_brand)"
  case "$b" in
    *xiaomi*|*redmi*|*" mi"*|*"mi "*) echo xiaomi ;;
    *skyworth*|*coocaa*) echo skyworth ;;
  esac
}
