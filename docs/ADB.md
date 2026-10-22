# Connecting to the TV with adb

> The full beginner walkthrough (TV settings, Windows, Mac, Linux, connecting) is in [INSTALL.md](INSTALL.md). This page is
> the short reference.

All scripts talk to the TV with `adb` (Android Debug Bridge) over Wi-Fi or Ethernet. Nothing is installed on the TV
except the launcher, and nothing needs root.

## 1. Turn on USB debugging on the TV

1. Settings, Device Preferences, About.
2. Press OK on **Build** (or "OS build") seven times. "You are now a developer" appears.
3. Back, then Developer options, and turn **USB debugging** on.
4. Find the TV's IP address under Settings, Network (or in your router's device list).

## 2. Get adb on your computer

Install Android's [platform-tools](https://developer.android.com/tools/releases/platform-tools) (or `brew install
android-platform-tools` on macOS, `sudo apt install adb` on Debian/Ubuntu). If it is not on your `PATH`, set
`ADB=/path/to/adb` before running a script.

## 3. Connect

```sh
adb connect 192.168.1.50:5555     # your TV's IP
adb devices                       # the TV must say "device", not "unauthorized"
```

The first time, the TV asks "Allow USB debugging?". Choose Always allow. If it never appears, look at the TV: the
prompt can be hidden behind the home screen. With more than one device connected, set
`ANDROID_SERIAL=192.168.1.50:5555`.

## When you are done

Turn USB debugging off again (Developer options). Some USB devices lose power while it is on, and it is one less
thing listening on your network.

## macOS: "adb connect" times out

macOS (Sequoia and later) can block command-line tools from reaching devices on your local network until they have
Local Network permission. If `adb connect` fails to reach a TV you can ping, allow your terminal app under System
Settings, Privacy & Security, Local Network, then restart the terminal and `adb kill-server`.
