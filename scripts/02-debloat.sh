#!/usr/bin/env bash
# Disables (never uninstalls) bloatware, telemetry and unused system apps.
#
#   ./scripts/02-debloat.sh --dry-run                 show what would be disabled
#   ./scripts/02-debloat.sh                           the "safe" lists: telemetry, feedback, content and ads
#   ./scripts/02-debloat.sh --extra                   also setup wizards, demo mode, backup helpers, unused Google apps
#   ./scripts/02-debloat.sh --optional                also preinstalled apps that some people use (if the profile lists any)
#   ./scripts/02-debloat.sh --stock-launcher          also the TV's own home screens. Only after MinimalTV is installed and default.
#   ./scripts/02-debloat.sh --profile NAME            use packages/NAME instead of detecting the brand ("none" = generic only)
#
# Two kinds of lists are used: packages/generic (Google and Android packages found on every Android TV) and, when the
# brand is recognised, packages/<brand> (see docs/PROFILES.md). If your brand has no profile only the generic lists run;
# use scripts/scan.sh to find your brand's own bloat and docs/PROFILES.md to turn it into a profile.
#
# Every package that is disabled is written to ~/.minimaltv/disabled.log so restore.sh can undo it.
# Packages that are not on your TV are skipped. Read docs/DEBLOAT.md first.
set -u
. "$(dirname "$0")/lib.sh"

dry=0; tiers=(safe); profile=""
while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) dry=1 ;;
    --extra) tiers+=(extra) ;;
    --optional) tiers+=(optional) ;;
    --stock-launcher) tiers+=(stock-launcher) ;;
    --profile) profile="${2:?profile name}"; shift ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) die "unknown option $1 (try --help)" ;;
  esac
  shift
done
require_device

if [ -z "$profile" ]; then profile="$(detect_profile)"; fi
[ "$profile" = "none" ] && profile=""
dirs=("$ROOT/packages/generic")
if [ -n "$profile" ]; then
  [ -d "$ROOT/packages/$profile" ] || die "no such profile: packages/$profile"
  dirs+=("$ROOT/packages/$profile")
  echo "Profile: generic + $profile   ($(device_brand))"
else
  echo "Profile: generic only. No brand profile for \"$(device_brand)\" yet (docs/PROFILES.md)."
fi

lists=()
for t in "${tiers[@]}"; do
  for d in "${dirs[@]}"; do [ -f "$d/$t.txt" ] && lists+=("$d/$t.txt"); done
done

for t in "${tiers[@]}"; do
  if [ "$t" = "stock-launcher" ] && [ "$dry" = 0 ]; then
    is_installed "$LAUNCHER_PKG" || die "install the launcher first (./scripts/01-install-launcher.sh), otherwise the TV would have no home screen."
    tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1 | grep -q "^$LAUNCHER_PKG/" \
      || die "the launcher is installed but is not the default home app. Run: adb shell cmd package set-home-activity $LAUNCHER_ACTIVITY"
  fi
done

[ "$dry" = 0 ] && { [ -f "$STATE/home_before.txt" ] || bash "$ROOT/scripts/00-backup.sh"; }
touch "$STATE/disabled.log" 2>/dev/null || true

n_off=0; n_skip=0; n_missing=0
for l in "${lists[@]}"; do
  echo "== ${l#"$ROOT"/packages/}"
  while read -r pkg; do
    if ! is_installed "$pkg"; then echo "  not on this TV   $pkg"; n_missing=$((n_missing + 1)); continue; fi
    if is_disabled "$pkg"; then echo "  already disabled $pkg"; n_skip=$((n_skip + 1)); continue; fi
    if [ "$dry" = 1 ]; then echo "  would disable    $pkg"; continue; fi
    if tv pm disable-user --user 0 "$pkg" | grep -q "disabled-user"; then
      echo "  disabled         $pkg"
      grep -qx "$pkg" "$STATE/disabled.log" || echo "$pkg" >> "$STATE/disabled.log"
      n_off=$((n_off + 1))
    else
      echo "  FAILED           $pkg"
    fi
  done < <(list_packages "$l")
done
echo
echo "disabled: $n_off, already disabled: $n_skip, not present: $n_missing"
[ "$dry" = 1 ] && echo "(dry run, nothing changed)"
[ "$dry" = 0 ] && echo "Undo with ./scripts/restore.sh. Reboot the TV once for the full effect."
exit 0
