# What gets disabled, and why

The scripts only ever run `pm disable-user --user 0 <package>`. Nothing is uninstalled, no system partition is
touched, and a factory reset or `scripts/restore.sh` brings everything back. Each package that is disabled is
appended to `~/.minimaltv/disabled.log` so it can be re-enabled exactly.

The lists are in `packages/`: `generic/` (Google and Android packages found on any Android TV) and one folder per brand
(`xiaomi/` is the only one so far; see [PROFILES.md](PROFILES.md) to add yours). One package per line with a comment.
The script detects the brand and uses `generic` plus the brand folder. Packages that are not on your TV are skipped.

| List | Option | What is in it | Risk |
| --- | --- | --- | --- |
| `safe.txt` | (default) | Generic: feedback and bug-report uploaders, Play Movies, Play Games. Xiaomi: telemetry, "Mi channels", web content, cleaner, screensaver, Mi Link, floating frame, gallery | Very low |
| `stock-launcher.txt` | `--stock-launcher` | Google TV / Android TV home screens and recommendation rows; Xiaomi's PatchWall | Needs MinimalTV installed and default first. The script refuses otherwise |
| `extra.txt` | `--extra` | Setup wizards, retail demo mode, DSU, printing, HTML viewer, screensavers, calendar and contacts sync, YouTube Music, TalkBack, backup helpers; Xiaomi: built-in Alexa | Low once the TV is set up. Do not run it on a TV you have not set up yet. Leave TalkBack and Alexa alone if you use them |
| `optional.txt` | `--optional` | Brand-specific preinstalled apps some people use (Xiaomi: Mi Music, MX Player, the preinstalled browser, TV lock) | Your call |

Always start with:

```sh
./scripts/02-debloat.sh --dry-run
./scripts/02-debloat.sh --dry-run --extra --stock-launcher
```

If your brand has no profile the script says so and applies the generic lists only. `./scripts/scan.sh` helps you find
your brand's own packages.

## Kept on purpose

These stay enabled on the Xiaomi TV the lists were made on, because the TV (or the remote) depends on them. Other
brands have their own equivalents (TV input, HDMI, tuner, remote services): find them with `scan.sh` and leave them be.

- `com.mediatek.*` and `mitv.service`: TV input, HDMI, tuner, remote hot keys.
- Play Store and Play Services, Gboard, the package installer and permission controller.
- Bluetooth and the remote's service (`com.google.android.tv.remote.service`).
- Google Assistant (`com.google.android.katniss`, about 85 MB of RAM). Disable it yourself if you never use voice
  search: `adb shell pm disable-user --user 0 com.google.android.katniss`. The voice button then does nothing.
- Chromecast built-in (`com.google.android.apps.mediashell`), Miracast (`com.xiaomi.mitv.smartshare` and the WFD
  helper), the update service (`com.xiaomi.mitv.updateservice`).
- All the streaming apps.

One package was tried and left out of the lists: `com.xiaomi.android.tvsetup.partnercustomizer`. Disabling it works,
but a receiver of it logs a (harmless) crash at every boot.

## Finding bloat on your own TV

```sh
adb shell pm list packages -s                # system apps
adb shell pm list packages -3                # apps you installed
adb shell dumpsys package <pkg> | less       # what it declares: receivers, services, permissions
adb shell dumpsys meminfo | head -40         # what is using memory right now
adb shell cmd package query-receivers --brief -a android.intent.action.BOOT_COMPLETED
                                             # what starts itself at boot
```

Disable one package at a time and use the TV for a bit. If something breaks:

```sh
adb shell pm enable <package>
```

## If the TV shows no home screen

This happens only if the stock launchers were disabled without a working replacement. From a computer:

```sh
./scripts/restore.sh
# or by hand:
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable <your brand's home screen package>   # e.g. com.mitv.tvhome.atv on Xiaomi
```

## Things changed outside the package lists

`03-tweaks.sh` changes settings and app-ops, not packages (see [PERFORMANCE.md](PERFORMANCE.md)). The remote button
service is enabled by `04-remote-keys.sh` (see [REMOTE-KEYS.md](REMOTE-KEYS.md)). `restore.sh` reverts those too.
