#!/usr/bin/env bash
# Collects what you need to build a debloat profile for your TV's brand (docs/PROFILES.md). Read-only: changes nothing.
#
#   ./scripts/scan.sh               prints the report and saves it to ~/.minimaltv/scan-<model>.txt
#
# The report lists the device, the home apps, what is using memory, what starts at boot, and every system package that
# is not a plain Android or Google one (usually the manufacturer's own apps). Some of those are bloat and some are
# essential (TV input, HDMI, tuner, remote); the report cannot tell which. You decide, one package at a time.
set -u
. "$(dirname "$0")/lib.sh"
require_device

model="$(tv getprop ro.product.model | tr ' /' '__')"
out="$STATE/scan-${model:-tv}.txt"
{
  echo "# MinimalTV scan $(date +%F)"
  echo
  echo "## Device"
  for k in ro.product.manufacturer ro.product.brand ro.product.model ro.build.version.release ro.build.version.sdk ro.board.platform ro.hardware; do
    printf '%-28s %s\n' "$k" "$(tv getprop "$k")"
  done
  echo "RAM                          $(tv grep MemTotal /proc/meminfo | tr -s ' ')"
  echo
  echo "## Home apps"
  tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | grep '/' || tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1
  echo
  echo "## Using the most memory right now (PSS)"
  tv dumpsys meminfo | sed -n '/Total PSS by process/,/^$/p' | sed -n '2,26p'
  echo
  echo "## Receive the boot broadcast (start themselves at boot)"
  tv cmd package query-receivers --brief -a android.intent.action.BOOT_COMPLETED | grep '/' | cut -d/ -f1 | sort -u | tr '\n' ' '
  echo; echo
  echo "## System packages that are not plain Android/Google (candidates; some are essential)"
  tv pm list packages -s | sed 's/^package://' | sort | grep -Ev '^(android|com\.android\.|com\.google\.android\.)' | while read -r p; do
    if is_disabled "$p"; then echo "  [disabled] $p"; else echo "             $p"; fi
  done
  echo
  echo "## Apps you installed"
  tv pm list packages -3 | sed 's/^package://' | sort
} | tee "$out"
echo
echo "Saved to $out"
echo "Next: docs/PROFILES.md explains how to turn this into a profile (and how to share it)."
