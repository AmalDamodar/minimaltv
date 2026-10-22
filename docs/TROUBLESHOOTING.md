# Troubleshooting

**`adb connect` fails or says "unauthorized".** See [ADB.md](ADB.md). Accept the prompt on the TV; on macOS allow
your terminal under Local Network.

**The TV still shows its old home screen after installing the launcher.** Make the launcher the default:

```sh
adb shell cmd package set-home-activity com.minimaltv.home/.MainActivity
```

If the TV asks which home app to use, pick "MinimalTV" and choose Always. Disabling the stock launchers
(`02-debloat.sh --stock-launcher`) removes the competition. If `set-home-activity` has no effect on your TV, the TV's own home button may keep opening its home screen. Use the remote button assignment
(About, Remote buttons) to send a button to MinimalTV instead.

**A "which launcher?" popup appears.** Android forgot the default home app, which happens when another home app is installed or this
one is updated. Choose MinimalTV and Always, or run `adb shell cmd package set-home-activity com.minimaltv.home/.MainActivity`.

**I disabled the stock launchers and see no home screen.** Run `./scripts/restore.sh`, or
`adb shell pm enable com.google.android.tvlauncher` and the home screen package of your brand (`adb shell pm list packages -d` shows what is disabled).

**A remote button does nothing.**
1. `adb shell settings get secure enabled_accessibility_services` should contain
   `com.minimaltv.home/com.minimaltv.home.KeyService`. If not, run `./scripts/04-remote-keys.sh`. The Remote buttons
   screen on the TV says "Button support is off" when it is not.
2. `adb shell dumpsys accessibility | grep -A2 "Bound services"` should list "MinimalTV".
3. The button must be assigned: About, Remote buttons. If pressing it on the "Add a button" screen shows nothing, the
   TV keeps that button for itself; `./scripts/find-keycodes.sh` shows which codes reach the system.
4. You cannot test with `adb shell input keyevent`; press the real button.

**A streaming app was closed when I opened another one.** That is **Close other apps** (About). Turn it off there, or
add the app to **Keep running**.

**Voice button (microphone) stopped working.** On Android 10 the Bluetooth remote can only pair and stream audio
while Location is on. If you turned Location off, turn it back on: `adb shell settings put secure location_mode 3`.
(If the microphone was already dead, the remote's hardware is the likely cause.)

**The TV forgot the animation speed or the background limits after an update.** Run `./scripts/03-tweaks.sh` again.

**Screenshots come back black over HDMI or Live TV.** `screencap` cannot capture protected video planes. It works on
the launcher and in apps.

**An app I need is gone.** `adb shell pm list packages -d` shows what is disabled; `adb shell pm enable <package>`
brings it back, or run `./scripts/restore.sh` for everything.

**Completely reset.** `./scripts/restore.sh`, then `adb uninstall com.minimaltv.home`. A factory reset also re-enables
everything the scripts disabled.
