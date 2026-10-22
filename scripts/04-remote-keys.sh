#!/usr/bin/env bash
# Turns on MinimalTV's key service (an Android accessibility service). With it on you can assign any button of your
# remote (Netflix, app drawer, the manufacturer's own launcher button, ...) on the TV itself:
#   MinimalTV > About (rail, bottom) > Remote buttons > Add a button
# It also makes "Close other apps" work. It reads key presses and the name of the app that is in front, no screen content.
#
#   ./scripts/04-remote-keys.sh          enable
#   ./scripts/04-remote-keys.sh --off    disable
set -eu
. "$(dirname "$0")/lib.sh"
require_device

current="$(tv settings get secure enabled_accessibility_services)"
[ "$current" = "null" ] && current=""
without="$(printf '%s' "$current" | tr ':' '\n' | grep -vx "$KEY_SERVICE" | paste -sd: - || true)"

if [ "${1:-}" = "--off" ]; then
  tv settings put secure enabled_accessibility_services "${without:-}"
  [ -z "$without" ] && tv settings put secure accessibility_enabled 0
  echo "key service off"
  exit 0
fi

is_installed "$LAUNCHER_PKG" || die "install the launcher first (./scripts/01-install-launcher.sh)"
tv settings put secure enabled_accessibility_services "${without:+$without:}$KEY_SERVICE"
tv settings put secure accessibility_enabled 1
echo "key service on. On the TV: About > Remote buttons > Add a button."
