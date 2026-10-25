# Remote buttons and closing apps

Many remotes have buttons for Netflix, Prime Video and so on, an app-drawer button, or one for the manufacturer's own
home screen. Those buttons send key codes that the manufacturer's launcher used to catch. Once that launcher is
disabled (or replaced) nothing answers them, and Android has no setting to remap them.

MinimalTV ships a small **accessibility service** (`KeyService`) that sees key events before apps do. You choose what
each button does, on the TV.

## Set it up

1. On the computer: `./scripts/04-remote-keys.sh` (turns the service on; `--off` turns it off).
2. On the TV: MinimalTV, rail, bottom icon (About), **Remote buttons**, **Add a button**.
3. Press the button on your remote. Its code appears; choose **Open the home screen**, **Open the Apps tab**,
   **Open an app** (then pick the app), **Switch to an input** (then pick HDMI 1, HDMI 2, AV or Live TV) or
   **Open the Inputs list**.
4. Repeat for other buttons. Select an entry in the list to remove it.

The remote's own **Input** (Source) button works without any setup: MinimalTV opens its Inputs list when you press
it, because the manufacturer's home screen that used to answer it is disabled. Assign it to something else if you
prefer.

If pressing a button does nothing on that screen, the TV keeps that button for itself (power, volume and some voice keys
are handled before any app sees them) and it cannot be remapped. `./scripts/find-keycodes.sh` shows which codes reach
the system, if you want to check.

Buttons act when pressed (not when released), and apps open **on top of Home**, so Back in the app returns to the home
screen instead of stepping back through every app you opened earlier.

## Close other apps

About, **Close other apps**. When you open an app from MinimalTV (or from a remote button) and later open another one,
the first is closed with `ActivityManager.killBackgroundProcesses`. The call is repeated at 1.5, 4 and 9 seconds because
an app that is still fading out counts as visible and is not killed.

- It is **on by default on TVs with less than 3 GB of RAM** and off otherwise.
- Only apps that were opened from MinimalTV or a remote button are ever closed. Anything else (a casting receiver, a
  VPN, an app started some other way) is left alone.
- **Keep running** (About) is a list of apps that are never closed. Put casting and AirPlay receivers, VPNs and music
  players there.
- Opening the home screen closes nothing: the app you left is still there until you open another one.

## Privacy and limits

- The service asks to filter key events and to receive window-change events. It does not ask to read window content,
  so it cannot see what is on screen; it learns only the package name of the app that came to the front, to know what
  to close. MinimalTV has no network permission, so nothing can leave the TV.
- In my tests `adb shell input keyevent <code>` was **not** delivered to the service, so you cannot test a button from
  the computer. Press it on the remote.
- It cannot change what an app does on Back. Netflix, for instance, goes to its profile screen instead of exiting; use
  the remote's own close or exit button.
- If an Android update turns the service off, run `./scripts/04-remote-keys.sh` again. Your button assignments are kept.
