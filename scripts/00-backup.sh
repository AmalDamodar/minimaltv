#!/usr/bin/env bash
# Saves what you need to undo everything: package lists, the current home app and the settings that are changed later.
# Safe to run any time. Writes to ~/.minimaltv/backup-<date>/ (override with MINIMALTV_STATE).
set -u
. "$(dirname "$0")/lib.sh"
require_device

dir="$STATE/backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$dir"
tv getprop                                   > "$dir/getprop.txt"
tv pm list packages -f                       > "$dir/packages_all.txt"
tv pm list packages -d                       > "$dir/packages_disabled_before.txt"
tv settings list global                      > "$dir/settings_global.txt"
tv settings list secure                      > "$dir/settings_secure.txt"
tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "$dir/home_default.txt"

# Only the first backup records the original home app, so a later run (after the launcher is installed) cannot overwrite it.
if [ ! -f "$STATE/home_before.txt" ]; then
  tail -n 1 "$dir/home_default.txt" > "$STATE/home_before.txt"
  grep -E '^(window|transition)_animation_scale=|^animator_duration_scale=' "$dir/settings_global.txt" > "$STATE/animation_before.txt" || true
fi

echo "Backup written to $dir"
echo "Original home app: $(cat "$STATE/home_before.txt")"
