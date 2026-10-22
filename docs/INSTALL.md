# Install guide

Two ways to install. Pick one.

- **Option A, with a computer (recommended).** Installs MinimalTV, removes the bloat, makes the remote buttons work, and
  can undo all of it. About 15 minutes the first time.
- **Option B, no computer.** Installs only the MinimalTV home screen from the TV itself. No debloat, no speed tweaks.
  About 5 minutes.

You can do B first and A later.

## Option A: with a computer

### 1. Before you start

- The TV and the computer must be on the same Wi-Fi or network.
- Plan to spend the first minutes on the TV's own settings, not on the computer.
- Everything the scripts do is undone by `./scripts/restore.sh`, and nothing is uninstalled. Still, read
  [DEBLOAT.md](DEBLOAT.md) once so you know what will be switched off.

### 2. Get a terminal with bash and adb

The scripts are bash scripts that call `adb`.

| Computer | What to do |
| --- | --- |
| **macOS** | Open Terminal. Install adb: `brew install android-platform-tools` (needs [Homebrew](https://brew.sh)), or download Google's [platform-tools](https://developer.android.com/tools/releases/platform-tools) and set `ADB=/path/to/platform-tools/adb`. |
| **Linux** | Open a terminal. Debian/Ubuntu: `sudo apt install adb git`. Other distributions: your package manager's `android-tools` or `adb`. |
| **Windows** | Not tested by me. Install **WSL** (Ubuntu) from the Microsoft Store, open it, then follow the Linux line inside it: `sudo apt install adb git`. Git Bash with Google's platform-tools should also work. |

Check it: `adb version` should print a version.

### 3. Turn on debugging on the TV

Follow [ADB.md](ADB.md): Settings, Device Preferences, About, press Build seven times, then Developer options, USB
debugging on. Note the TV's IP address (Settings, Network).

### 4. Download the project and connect

```sh
git clone https://github.com/AmalDamodar/minimaltv.git
cd minimaltv
adb connect 192.168.1.50:5555        # use your TV's IP
adb devices                          # the TV must be listed as "device"
```

On the TV, accept the "Allow USB debugging?" prompt and choose Always allow. If `adb devices` says `unauthorized`, the
prompt is waiting on the TV.

### 5. Run the steps in this order

| Step | Command | What it does | Time |
| --- | --- | --- | --- |
| 1 | `./scripts/00-backup.sh` | Saves package lists, settings and your current home app to `~/.minimaltv/` | 10 s |
| 2 | `./scripts/02-debloat.sh --dry-run` | Prints what would be disabled. Changes nothing. Read it | 20 s |
| 3 | `./scripts/02-debloat.sh` | Disables telemetry, feedback uploaders and (on a Xiaomi) the brand's content and ads | 30 s |
| 4 | `./scripts/01-install-launcher.sh` | Installs MinimalTV and makes it the home app | 30 s |
| 5 | Press **Home** on the remote | You should see MinimalTV. If a "which home app?" popup appears, choose MinimalTV and Always | |
| 6 | `./scripts/02-debloat.sh --stock-launcher` | Disables the TV's own home screens. The script refuses if MinimalTV is not the default | 20 s |
| 7 | `./scripts/02-debloat.sh --extra` | Optional. Setup wizards, demo mode, backup helpers. Only on a TV that is already set up | 30 s |
| 8 | `./scripts/03-tweaks.sh` | Animations 0.5x, background limits for streaming apps, trims caches | 30 s |
| 9 | `./scripts/04-remote-keys.sh` | Turns on remote button support | 5 s |
| 10 | Restart the TV (unplug for 10 seconds, or Settings, Device Preferences, Restart) | Applies everything | 1 min |

If step 4 cannot download the app (no internet on the computer, or GitHub is blocked), build it yourself
([LAUNCHER.md](LAUNCHER.md)) or download `minimaltv.apk` from the
[releases page](https://github.com/AmalDamodar/minimaltv/releases/latest) and run
`./scripts/01-install-launcher.sh path/to/minimaltv.apk`.

### 6. Set up the remote buttons on the TV

1. In MinimalTV, move left to the rail and down to the bottom icon (About). Press OK.
2. Choose **Remote buttons**, then **Add a button**.
3. Press the button on your remote (for example the Netflix button). Its number appears.
4. Choose what it does: the home screen, the Apps tab, or **Open an app** (then pick the app).
5. Repeat for the other buttons. Back closes the screens.

If nothing happens when you press a button on step 3, the TV keeps that button for itself and it cannot be changed.

Also in About: **Close other apps** (on by default below 3 GB of RAM) closes the app you were in when you open another
one, and **Keep running** lists apps that must never be closed (casting receivers, VPNs, music players).

### 7. Check it worked

- Press Home: MinimalTV opens quickly, For You shows your favourite apps (hold OK on an app to add or remove it).
- Open an app, press Back: you return to MinimalTV.
- A remote button you set up opens its app, from inside another app too.
- `./scripts/02-debloat.sh --dry-run --extra --stock-launcher` now says "already disabled" for the packages it handled.

### 8. When you are done

Turn USB debugging off on the TV again (Developer options).

### Undo everything

```sh
./scripts/restore.sh
```

Then restart the TV. This re-enables every package the scripts disabled, puts the original home app back, resets the
animation scales and the background limits, and turns the button service off. `adb uninstall com.minimaltv.home`
removes MinimalTV itself.

## Option B: no computer

Installs only the home screen. Debloat, tweaks and the "close other apps" button handling need a computer.

1. On the TV, allow installing from outside the Play Store: Settings, Device Preferences, Security & restrictions,
   Unknown sources, allow for the app you will use in step 3.
2. Install **Downloader** (by AFTVnews) from the TV's Play Store.
3. Open Downloader and enter `github.com/AmalDamodar/minimaltv/releases/latest/download/minimaltv.apk`, then install.
4. Press **Home**. If Android asks which home app to use, choose MinimalTV and Always. If it does not ask, your TV
   keeps its own home screen as the default and only a computer can change that (Option A, step 4).

The exact menu names differ between TVs. The remote buttons need the key service, which only a computer can turn on
(`./scripts/04-remote-keys.sh`).

## Trouble

[TROUBLESHOOTING.md](TROUBLESHOOTING.md) covers the common failures: `unauthorized`, no home screen, a button that does
nothing, the "which launcher?" popup.
