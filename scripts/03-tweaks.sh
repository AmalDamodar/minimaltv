#!/usr/bin/env bash
# Speed tweaks that need no root. Each one can be skipped and is undone by restore.sh.
#
#   ./scripts/03-tweaks.sh                       animations 0.5x, release cached files, background limits for streaming apps
#   ./scripts/03-tweaks.sh --animations 0.5      choose a scale (1.0 = stock, 0.5 = twice as fast, 0 = off)
#   ./scripts/03-tweaks.sh --no-background       skip the background limits
#   ./scripts/03-tweaks.sh --dns dns.adguard-dns.com   also set Private DNS (blocks many ad and tracker domains for every app)
#
# Background limit = `cmd appops set <app> RUN_ANY_IN_BACKGROUND ignore` for the apps in packages/background-limit.txt:
# they can no longer start themselves, run services or wake up at boot while you are not using them.
set -u
. "$(dirname "$0")/lib.sh"

anim="0.5"; bg=1; dns=""
while [ $# -gt 0 ]; do
  case "$1" in
    --animations) anim="${2:?scale}"; shift ;;
    --no-background) bg=0 ;;
    --dns) dns="${2:?host}"; shift ;;
    -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
    *) die "unknown option $1 (try --help)" ;;
  esac
  shift
done
require_device
[ -f "$STATE/home_before.txt" ] || bash "$ROOT/scripts/00-backup.sh"

for k in window_animation_scale transition_animation_scale animator_duration_scale; do tv settings put global "$k" "$anim"; done
echo "animation scales set to $anim"

tv pm trim-caches 8G >/dev/null 2>&1 || true
echo "cached files trimmed"

if [ "$bg" = 1 ]; then
  : > "$STATE/appops.log"
  while read -r pkg; do
    is_installed "$pkg" || continue
    tv cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
    echo "$pkg" >> "$STATE/appops.log"
    echo "background limited: $pkg"
  done < <(list_packages "$ROOT/packages/background-limit.txt")
fi

if [ -n "$dns" ]; then
  [ -f "$STATE/private_dns_before.txt" ] || tv settings get global private_dns_mode > "$STATE/private_dns_before.txt"
  tv settings put global private_dns_mode hostname
  tv settings put global private_dns_specifier "$dns"
  echo "Private DNS set to $dns"
fi
echo "Done. Reboot the TV once."
