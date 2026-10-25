# Files, a file manager for the remote

A small app for browsing the TV's internal storage and USB drives with the remote. Most TVs have no file manager of
their own, and the ones that exist are built for a mouse or a touch screen. Files is made for the D-pad.

**The screen, top to bottom**

- **Files** and a **Search** box. Search looks in the current folder and everything under it.
- **Shortcuts**: Internal storage, each USB drive, and the Downloads, Videos, Music, Pictures and Documents folders
  (only the ones that exist). The one you are in is lit.
- The **folder name** with its path below it, and two buttons on the right: **Sort and show** (sort by name, size, date
  or type, ascending or descending, show hidden files) and **layout** (tiles or list). Both choices are remembered.
- The **files**, as tiles (icon top left, name bottom left) or as a list. The first tile is **New folder**.

**Remote controls**

- **OK** opens a folder or a file; **Back** goes up one folder (or clears a search).
- **Hold OK** (or press the **Menu** button) on a tile or row for its menu: **Open**, **Copy**, **Move**, **Rename**,
  **Delete**, **Details**. In list layout the three dots at the end of each row do the same and can be selected.
- Copy and Move wait for a destination: open that folder, then choose **Paste here**. A bar at the top shows what is
  waiting, with progress for big files and Cancel.
- Delete asks first, and the question starts on **Cancel** so one stray press on OK deletes nothing.
- The standard top-level folders (Download, DCIM, Android and so on) can only be copied, not moved, renamed or deleted.
- OK on a file hands it to whatever app on the TV can show it (a video in a video player, an APK in the system
  installer). If no app can, you get a message.
- Deleting, renaming and moving tell Android's media database, so the TV's media apps do not keep showing files that
  are gone.
- No network permission, no ads, no accounts. Dark only, to match the home screen's glass look.

## Install

With the TV connected (see [INSTALL.md](INSTALL.md)):

```
./scripts/05-install-files.sh
```

This installs the app and grants it storage access. If you would rather build it yourself:

```
cd files
./gradlew :app:assembleRelease      # or assembleDebug
```

Then run the script again; it picks up the APK you built. Pass a path to install a specific file.

Android 10 and older use the classic storage permissions. On Android 11 or newer the app needs the "all files access"
permission; the script sets it, and if that fails the app's first screen has an **Allow access** button that opens the
system setting (some TVs do not have that screen).

## Open it at a folder from another app

Files can be opened at a folder by other apps with an explicit intent:

```
adb shell am start -n com.minimaltv.files/.MainActivity --es com.minimaltv.files.PATH '/storage/emulated/0/Download'
```

The [AirPlay TV](https://github.com/AmalDamodar/airplay-tv) receiver uses this for its **Show in folder** button.
