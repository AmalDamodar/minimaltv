#!/usr/bin/env bash
# Troubleshooting helper: shows the Android key code of each remote button that reaches the system's key handler.
# You normally do not need it: MinimalTV > About > Remote buttons > Add a button learns the buttons on the TV itself.
# Use this if a button never shows up there (the TV may be keeping it for itself).
#   ./scripts/find-keycodes.sh        then press the buttons you care about, then press Enter here
set -u
. "$(dirname "$0")/lib.sh"
require_device

"$ADB" logcat -c
echo "Press the remote buttons you want to map (one at a time, a second apart), then press Enter here."
read -r _
echo
echo "Key codes seen (see the android.view.KeyEvent docs for names):"
"$ADB" logcat -d | tr -d '\r' | grep -E 'interceptKeyTq keycode=' | sed -E 's/.*keycode=([0-9]+).*/\1/' | awk '!seen[$0]++' | while read -r c; do
  echo "  $c"
done
echo
echo "In the order you pressed them. If nothing is listed, your TV does not log keys: run"
echo "  adb shell getevent -l"
echo "press a button and note the scan code, then see docs/REMOTE-KEYS.md."
