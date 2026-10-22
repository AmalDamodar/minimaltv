#!/usr/bin/env bash
# Undoes everything the other scripts changed: re-enables the disabled packages, puts the original home app back,
# resets the animation scales, the background limits and Private DNS.
# The launcher app stays installed (remove it with: adb uninstall com.minimaltv.home).
set -u
. "$(dirname "$0")/lib.sh"
require_device

key=0
if [ -f "$STATE/disabled.log" ]; then
  while read -r pkg; do
    [ -z "$pkg" ] && continue
    echo "enable $pkg"
    tv pm enable "$pkg" >/dev/null
  done < "$STATE/disabled.log"
  : > "$STATE/disabled.log"
fi

bash "$ROOT/scripts/04-remote-keys.sh" --off >/dev/null 2>&1 || true

if [ -f "$STATE/home_before.txt" ]; then
  home="$(cat "$STATE/home_before.txt")"
  echo "home app -> $home"
  tv cmd package set-home-activity "$home" || true
fi

if [ -f "$STATE/animation_before.txt" ]; then
  while IFS='=' read -r k v; do tv settings put global "$k" "$v"; done < "$STATE/animation_before.txt"
else
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do tv settings put global "$k" 1.0; done
fi

if [ -f "$STATE/appops.log" ]; then
  while read -r pkg; do [ -n "$pkg" ] && tv cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND default; done < "$STATE/appops.log"
  : > "$STATE/appops.log"
fi

if [ -f "$STATE/private_dns_before.txt" ]; then
  was="$(cat "$STATE/private_dns_before.txt")"
  [ "$was" = "null" ] && was="opportunistic"
  tv settings put global private_dns_mode "$was"
fi
echo "Restored. Reboot the TV once."
