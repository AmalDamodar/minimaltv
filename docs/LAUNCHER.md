# MinimalTV, the home screen

Package `com.minimaltv.home`. Plain Java and AndroidX RecyclerView, no network permission, no analytics, no ads.
Android 8.0 (API 26) and up, landscape, made for a 1920x1080 TV and a remote. Checked on Android 10; Android's lint
reports no calls that need a newer API than 26.

## Using it

- **Tabs:** For You (the apps you picked) and Apps (everything installed). Back from Apps returns to For You. The
  launcher never exits on Back.
- **For You:** press OK on the **+** tile and tick apps, or hold OK on any app in either tab to add or remove it
  (a star on an app in the Apps tab means it is in For You). On first run For You is filled with the common streaming
  apps that are installed (`Config.DOCK`). Newly installed apps are added to For You on their own;
  remove them with a long press if you do not want them there.
- **Rail:** Search (on-screen keyboard; a USB keyboard works too), Home, Settings, then Inputs (HDMI, AV, Live TV when
  the TV has a tuner) and About (model, Android version, network, storage, memory, next background, light or dark
  mode, **Remote buttons**, **Close other apps** and **Keep running**; see [REMOTE-KEYS.md](REMOTE-KEYS.md)).
- **Top right:** Wi-Fi icon with signal strength (or Ethernet, or offline), the time with AM/PM, and the date.
  The Wi-Fi network name is not shown: Android 10 only gives apps the name when Location is on.
- **Home button while already home:** back to the first tab and first tile.

## Build it

You need JDK 17 and the Android SDK (platform 34 and build tools). Android Studio brings both.

```sh
cd launcher
echo "sdk.dir=$HOME/Android/Sdk" > local.properties       # wherever your SDK is
./gradlew assembleDebug
# the APK: app/build/outputs/apk/debug/app-debug.apk
```

Then `./scripts/01-install-launcher.sh` installs the APK you just built. The debug build is signed with the debug
key, which is fine for installing on your own TV.

## Change it

`launcher/app/src/main/java/com/minimaltv/home/Config.java` has the apps that are put in For You on first run and the
packages that are hidden from the grid. Rebuild and run `01-install-launcher.sh` again; your settings, favourites and
remote buttons survive a reinstall (`pm install -r`). Everything else is changed on the TV.

**Backgrounds:** `python3 tools/make_backgrounds.py my1.jpg my2.jpg ...` writes the sharp and the pre-blurred copy of
each photo and updates `Background.COUNT`. The photo drifts slowly and cross-fades to the next one every 100 seconds
or so.

## How it stays light

Measured on the test TV (Android 10, 1.8 GB total): about 77 MB of memory (PSS) with the For You and Apps tabs loaded.

- The frosted glass is not a live blur. Each panel shows a pre-blurred copy of the photo, drawn at the same offset as
  the photo behind it, so there is nothing to blur on the GPU.
- Backgrounds are decoded as RGB_565 and only the current and next one are kept.
- The drift animation runs at about 30 fps and stops when the launcher is not in front.
- The clock uses the system's minute tick and the network icon a network callback; there is no polling.
- Focus is a one-step-per-key D-pad handler for the grid and the rail, so focus never jumps somewhere unexpected.

## Look

No accent colour: white and translucent greys over the photo. Focus is a white ring, selected is a lighter fill.
Parent views never clip the focus ring.
