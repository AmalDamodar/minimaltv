# Complete install guide

This guide assumes you know nothing about `adb`, developer options or the command line. Follow it from top to bottom
and do not skip a step. Every step says what you should see when it worked and what to do when it did not.

There are two ways to install:

- **Option A, with a computer (recommended).** Installs the MinimalTV home screen, removes the bloat, speeds the TV
  up and makes the remote's special buttons work. Everything can be undone. Allow about 30 to 45 minutes the first
  time, mostly waiting and typing.
- **Option B, no computer.** Installs only the MinimalTV home screen, straight from the TV. Allow 10 minutes. See
  [Option B](#option-b-no-computer) at the end.

You can do B first and A later.

## Contents

1. [Before you start](#1-before-you-start)
2. [On the TV: connect it to the internet](#2-on-the-tv-connect-it-to-the-internet)
3. [On the TV: find its IP address](#3-on-the-tv-find-its-ip-address)
4. [On the TV: turn on developer options and USB debugging](#4-on-the-tv-turn-on-developer-options-and-usb-debugging)
5. [On the computer: get ready (Windows, Mac or Linux)](#5-on-the-computer-get-ready)
6. [Connect the computer to the TV](#6-connect-the-computer-to-the-tv)
7. [Install MinimalTV and clean up the TV](#7-install-minimaltv-and-clean-up-the-tv)
8. [Set up the remote's buttons](#8-set-up-the-remotes-buttons)
9. [Check that everything works](#9-check-that-everything-works)
10. [Undo, update, turn debugging off](#10-undo-update-turn-debugging-off)
11. [Option B: no computer](#option-b-no-computer)
12. [When something goes wrong](#when-something-goes-wrong)

---

## 1. Before you start

### What you need

- An **Android TV or Google TV** (not Samsung, LG, Roku or Apple TV: those are different systems and this project
  cannot work on them). Not sure? On the TV's home screen you see rows of apps and there is a **Google Play Store** app.
  Settings, Device Preferences (or System), About shows an Android version. The scripts need **Android 8.0 or newer**.
- The TV's **remote** and the TV switched on.
- A **computer**: Windows 10 or 11, a Mac, or Linux. It must be on the **same home network** (same router) as the TV.
- About 1 GB of free space on the computer is plenty.
- Your Wi-Fi name and password, if the TV is not on the internet yet.

### What will happen, and what will not

- The scripts **disable** apps that you do not need. They never delete anything, and a single command
  (`restore.sh`) switches them all back on. A factory reset of the TV also undoes everything.
- The only thing installed on your TV is the MinimalTV home screen (a normal app, removable like any other).
- Nothing is sent anywhere. The home screen has no internet permission.
- It is still your TV and your responsibility. Read [DEBLOAT.md](DEBLOAT.md) once so you know what gets switched off.
  The project was tested on one Xiaomi TV only; other brands only get the safe generic list, and you can see exactly
  what would change before anything changes (the "dry run" in step 7).

### Words used in this guide

| Word | Meaning |
| --- | --- |
| **Launcher / home screen** | The first screen you see when you press Home. MinimalTV replaces it. |
| **Debloat** | Switching off apps and services the TV came with that you do not need (telemetry, ads, setup wizards). |
| **adb** | "Android Debug Bridge", a free tool from Google that lets a computer send commands to the TV over your network. |
| **Terminal** | A window where you type a command and press Enter. On Windows we use one called **Git Bash**. |
| **IP address** | The TV's address on your home network: four numbers with dots, for example `192.168.1.50`. |
| **Script** | A small file of commands. You start one by typing its name, for example `./scripts/00-backup.sh`. |

### How to read the commands in this guide

Lines in grey boxes are commands. You **type or paste them into the terminal and press Enter**, one line at a time.
Text after a `#` on the same line is a comment; you can leave it out. Do not type the `$` or `>` at the start of a line
if you see one: it is the terminal's own prompt.

---

## 2. On the TV: connect it to the internet

Skip to step 3 if the TV is already online (you can open YouTube and a video plays).

You use the remote's arrow keys and the round **OK** (select) button. **Back** goes back one screen.

### 2A. Wi-Fi

1. Press **Home**. Find the **Settings** icon, a gear, usually at the top right of the home screen. (Many remotes also
   have a gear or "settings" button. Google TV: your profile picture at the top right, then Settings.)
2. Open **Network & Internet** (it can be called just **Network** or **Wi-Fi**).
3. Turn **Wi-Fi** on if it is off. A list of networks appears.
4. Select **your network's name**.
5. Type the password with the on-screen keyboard: move with the arrow keys, press OK on each letter. The key with an up
   arrow is Shift for capital letters, and the `?123` key switches to numbers and symbols. Passwords are case sensitive.
   Tip: the **Google TV** phone app on your phone can type for you (a keyboard icon), which is much faster.
6. Select **Connect** (or the tick). Under your network's name you should see **Connected**.

If it will not connect: check the password, move closer to the router, try the 2.4 GHz version of your network if the
5 GHz one fails, and restart the router.

### 2B. Ethernet cable (more reliable)

Plug a network cable from your router into the TV's **LAN / Ethernet** port. Nothing else to do. In Settings, Network &
Internet you should see **Ethernet** as connected.

### 2C. Check it really is online

Open YouTube or any app and play something. If it plays, you are online.

> **Important for later:** the computer and the TV must be on the **same network**. If your router has a separate
> "Guest" Wi-Fi, do not put one device on the guest network and the other on the main one. Guest networks usually hide
> devices from each other, which makes the connection in step 6 fail.

---

## 3. On the TV: find its IP address

The computer needs the TV's address. You will type it in step 6.

Try these in order (the menu names differ a little between TVs):

1. **Settings, Network & Internet**, then press OK on the network you are connected to. Look for **IP address**.
2. **Settings, Device Preferences, About, Status**. Look for **IP address**.
3. **Settings, Network, Advanced** (or Network details). Look for **IP address**.
4. Your router's page or app lists connected devices with their addresses. Look for the TV by name.

The address looks like **`192.168.1.50`** or `10.0.0.23`: four numbers separated by dots. **Write it down.** Ignore a
longer address with colons in it (that is IPv6).

> The TV's address can change when the router or the TV restarts. If a later step cannot find the TV, come back here,
> read the address again and use the new one. To stop it changing, open your router's settings and look for "DHCP
> reservation", "address reservation" or "static lease", and reserve the TV's address (the steps depend on your router).

---

## 4. On the TV: turn on developer options and USB debugging

This is the step that lets the computer talk to the TV. It is safe and you can switch it off again at the end.

### 4A. Unlock developer options

Developer options are hidden until you tap one item seven times.

1. **Settings, Device Preferences, About.** (Google TV and newer sets: **Settings, System, About**.)
2. Scroll down to the line that shows the **build**. It may be called **Build**, **Build number**, **OS build** or
   **Android TV OS build**.
3. Highlight it and press **OK seven times in a row**, fairly quickly.
4. After a few presses a message counts down ("You are now 3 steps away from being a developer"). After the seventh you
   see **"You are now a developer!"**. If you see "No need, you are already a developer", it is already unlocked.
5. Press **Back** once. A new item **Developer options** now appears in the same menu (in Device Preferences, or in
   System on Google TV).

If you cannot find the build line, look through every item inside About; on some TVs it is under **Software** or
**Version**. Searching the web for "<your TV model> enable developer options" shows the exact line for your model.

### 4B. Turn on USB debugging

1. Open **Developer options**.
2. If there is a switch at the top called **Enable developer options** (or **Developer options**), make sure it is **ON**.
3. Scroll to **USB debugging** and press OK so the switch shows **ON**.
4. If a warning appears ("Allow USB debugging?"), choose **OK**.
5. Also turn **ON** anything called **Network debugging**, **ADB over network** or **ADB debugging over network** if the
   menu has one. Many TVs have none, and that is fine: USB debugging alone is enough on most TVs running Android 10 or
   older.

Do **not** change anything else in Developer options.

### 4C. Android 11 or newer: Wireless debugging (only if the TV has it)

Some newer TVs (Android 11 and up, many Google TVs) have **Wireless debugging** in Developer options instead. If your
Developer options list has **Wireless debugging**:

1. Turn **Wireless debugging** on and confirm.
2. Open it. You see an **IP address & Port** (for example `192.168.1.50:41923`) and an option **Pair device with pairing
   code**. Leave this screen open; you need it in step 6B.

If your TV has both USB debugging and Wireless debugging, turn both on.

---

## 5. On the computer: get ready

Do the section for your computer. You are installing two free tools: **adb** (talks to the TV) and **git** (downloads
this project; optional, you can download a ZIP instead). On Windows you also need a terminal that understands the
scripts, which **Git for Windows** includes (it is called **Git Bash**).

### 5A. Windows 10 or 11

> I wrote the Windows steps from how these tools work. I could not test them on a Windows PC. If a step differs on
> yours, please open an issue so the guide can be fixed.

**Quick way (copy and paste).** Open **PowerShell** (Windows key, type PowerShell, Enter) and paste these two lines. They
install Git (with Git Bash) and Google's adb in one go; click **Yes** if Windows asks for permission, then close
PowerShell, open Git Bash (first half of **Step 3** below) and **skip the `export ADB=...` lines**: the winget version of
adb is found automatically, so just type `adb` wherever this guide says `"$ADB"`. Then go on to **Step 4**.

```powershell
winget install --id Git.Git -e --source winget
winget install --id Google.PlatformTools -e --source winget
```

If `winget` is not recognised, or you prefer to do it by hand, use steps 1 to 3:

**Step 1: install Git for Windows (includes Git Bash)**

1. Go to **https://git-scm.com/download/win** and download the installer (it starts by itself, the file is called
   something like `Git-2.xx.x-64-bit.exe`).
2. Run it. Windows may ask "Do you want to allow this app to make changes?": click **Yes**.
3. Click **Next** on every screen and keep the defaults, then **Install**, then **Finish**.

**Step 2: download Google's adb ("platform-tools")**

1. Go to **https://developer.android.com/tools/releases/platform-tools** and click **Download SDK Platform-Tools for
   Windows**. Accept the terms.
2. In your **Downloads** folder, right-click the file `platform-tools-latest-windows.zip` and choose **Extract All**.
3. Change the destination to **`C:\`** (just the drive, so the result is `C:\platform-tools`) and click **Extract**.
4. Open `C:\platform-tools` in File Explorer. You should see a file called **adb.exe** in it. If you only see another
   folder called platform-tools inside, move that inner folder's contents up so that `C:\platform-tools\adb.exe` exists.

**Step 3: open Git Bash and tell it where adb is**

1. Press the **Windows key**, type **Git Bash**, press **Enter**. A window with a `$` prompt opens. This is your terminal.
2. Copy this line, **right-click in the Git Bash window to paste** (or press Shift+Insert), then press Enter. It stores
   the location of adb permanently:
   ```sh
   echo 'export ADB=/c/platform-tools/adb.exe' >> ~/.bashrc
   ```
3. Close the Git Bash window and open it again (so the setting is read).
4. Check it works:
   ```sh
   "$ADB" version
   ```
   You should see a line starting with **Android Debug Bridge version**. If Windows shows a **Firewall** popup, click
   **Allow access**. If it prints nothing or "command not found", type `export ADB=/c/platform-tools/adb.exe` and press
   Enter, then repeat the check (you will need to type that line each time you open Git Bash).

**Step 4: download this project**

In Git Bash:

```sh
git clone https://github.com/AmalDamodar/minimaltv.git
cd minimaltv
```

(No git? On the project page https://github.com/AmalDamodar/minimaltv click the green **Code** button, then **Download
ZIP**, extract it, then in Git Bash go to the folder, for example `cd ~/Downloads/minimaltv-main`.)

Check you are in the right folder:

```sh
ls
```

You should see `README.md`, `scripts`, `docs`, `launcher` and `packages`.

> **Windows tips.** Git Bash's `~` is your user folder (`C:\Users\<you>`). Paste with right-click or Shift+Insert.
> `Ctrl+C` stops a running command. Always run the scripts from **Git Bash**, not from Command Prompt or PowerShell
> (they cannot run these scripts).

**Prefer WSL?** Instead of Git Bash you can use the Windows Subsystem for Linux: open **PowerShell as administrator**,
run `wsl --install`, restart the PC, open **Ubuntu** from the Start menu and follow the **Linux** section below inside
it.

### 5B. Mac

1. Open **Terminal**: press **Cmd + Space**, type **Terminal**, press Enter. A window with a prompt opens.
2. Check git:
   ```sh
   git --version
   ```
   If a popup offers to install the **Command Line Developer Tools**, click **Install** and wait a few minutes (they
   include git), then run the command again.
3. Install **Homebrew** (a tool that installs other tools). Copy the one-line command from **https://brew.sh**, paste
   it into Terminal and press Enter. It asks for your Mac password (you will not see the letters as you type, that is
   normal). When it finishes it prints a **"Next steps"** section: **run the lines it shows** (they add Homebrew to
   your PATH), then close and reopen Terminal.
4. Install adb:
   ```sh
   brew install --cask android-platform-tools
   ```
5. Check:
   ```sh
   adb version
   ```
   You should see **Android Debug Bridge version ...**. If macOS says it cannot verify the app, open **System Settings,
   Privacy & Security**, scroll down and click **Allow Anyway**, then run it again.
6. Download the project:
   ```sh
   git clone https://github.com/AmalDamodar/minimaltv.git
   cd minimaltv
   ls
   ```
   You should see `README.md`, `scripts`, `docs`, `launcher` and `packages`.

**Local network permission (important on recent macOS).** The first time adb tries to reach the TV, macOS may show
"Terminal would like to find and connect to devices on your local network": click **Allow**. If you missed it, or `adb
connect` times out although you are sure the IP is right, open **System Settings, Privacy & Security, Local Network**,
switch **Terminal** on, then quit and reopen Terminal and run `adb kill-server`.

No Homebrew? Download "SDK Platform-Tools for Mac" from the platform-tools page above, unzip it to your home folder,
and tell the scripts where it is: `echo 'export ADB=$HOME/platform-tools/adb' >> ~/.zshrc`, then reopen Terminal.

### 5C. Linux

1. Open a terminal.
2. Install adb, git and curl. Debian, Ubuntu, Mint:
   ```sh
   sudo apt update && sudo apt install adb git curl
   ```
   Fedora: `sudo dnf install android-tools git curl`. Arch: `sudo pacman -S android-tools git curl`.
3. Check: `adb version`.
4. Download the project:
   ```sh
   git clone https://github.com/AmalDamodar/minimaltv.git
   cd minimaltv
   ```

### If you downloaded the ZIP instead of using git

The scripts must be allowed to run. If a later step says **"Permission denied"**, put `bash ` in front of the command,
for example `bash scripts/00-backup.sh`. The same works everywhere in this guide.

---

## 6. Connect the computer to the TV

Keep the TV **switched on** (not asleep) and look at its screen during this step: it will ask a question.

In the terminal, inside the `minimaltv` folder. **Windows (Git Bash):** use `"$ADB"` wherever this guide says `adb`
(for example `"$ADB" connect ...`). On Mac and Linux just type `adb`.

### 6A. Normal case (USB debugging, most TVs)

```sh
adb connect 192.168.1.50:5555      # replace with YOUR TV's IP from step 3, keep :5555
```

- You should see **`connected to 192.168.1.50:5555`**. A box appears **on the TV**: **"Allow USB debugging?"**. Tick
  **Always allow from this computer** and choose **OK**.
- If you saw `failed to authenticate` or `unauthorized` instead, that box is waiting on the TV. Answer it, then run the
  `adb connect` line again.

Check:

```sh
adb devices
```

You should see exactly this (with your own address):

```
List of devices attached
192.168.1.50:5555       device
```

The word at the end must be **`device`**. If it says **`unauthorized`**, answer the box on the TV. If it says
**`offline`**, run `adb disconnect` then `adb connect 192.168.1.50:5555` again.

### 6B. Wireless debugging (only if your TV has it and 6A fails)

1. On the TV, open **Developer options, Wireless debugging, Pair device with pairing code**. It shows a **pairing
   address** (like `192.168.1.50:37123`) and a **6-digit code**. Keep it on screen.
2. On the computer:
   ```sh
   adb pair 192.168.1.50:37123        # the PAIRING address from the TV
   ```
   Type the 6-digit code when asked. You should see **Successfully paired**.
3. Go back one screen on the TV. The main Wireless debugging screen shows **IP address & Port**, for example
   `192.168.1.50:41923`. This port is **different** from the pairing one. Then:
   ```sh
   adb connect 192.168.1.50:41923
   adb devices
   ```
   You should see the TV listed as `device`. In the scripts, set `ANDROID_SERIAL=192.168.1.50:41923` first (on Mac,
   Linux and Git Bash: `export ANDROID_SERIAL=192.168.1.50:41923`) if more than one device is connected.

### If the connection fails

| What you see | What it means | What to do |
| --- | --- | --- |
| `failed to authenticate` or `unauthorized` | The TV is waiting for you to allow this computer | Look at the TV screen, choose Always allow, run `adb connect` again |
| `Connection refused` | USB debugging is off, or the TV does not accept network connections | Redo step 4 (turn USB debugging on, and Network or Wireless debugging if there is one). Try turning USB debugging off and on, then connect again |
| `Connection timed out`, `No route to host` | Wrong IP, different network, VPN or firewall | Read the IP again (step 3). Put both devices on the same network. Switch off any VPN on the computer. Allow adb in the firewall |
| The TV was found yesterday, not today | The TV's IP changed | Read the IP again (step 3) |
| `more than one device/emulator` | A phone or another TV is also connected | Run `adb disconnect`, then connect to only this TV. Or set `ANDROID_SERIAL=192.168.1.50:5555` |
| `adb: command not found` | adb is not installed or not on the PATH | Redo step 5 for your computer. On Windows use `"$ADB"` |

[ADB.md](ADB.md) and [TROUBLESHOOTING.md](TROUBLESHOOTING.md) have a few more cases.

---

## 7. Install MinimalTV and clean up the TV

All of these commands are typed in the terminal, in the `minimaltv` folder, with the TV connected (`adb devices` shows
it as `device`). On Windows, remember to use **Git Bash**; set `ADB` as in step 5A if you reopened it. Run **one at a
time and read what it prints** before the next.

Nothing here is uninstalled; every change can be undone in step 10.

### Step 7.1: back up the TV's current state

```sh
./scripts/00-backup.sh
```

You should see:

```
Backup written to /home/you/.minimaltv/backup-20261022-093207
Original home app: com.something.launcher/.MainActivity
```

It saved the list of apps, the settings and the name of your current home screen, so the undo command can put them back.

### Step 7.2: preview what would be disabled (changes nothing)

```sh
./scripts/02-debloat.sh --dry-run
```

You should see a profile line and a list like this (your list depends on your TV):

```
Profile: generic + xiaomi   (xiaomi xiaomi)
== generic/safe.txt
  would disable    com.google.android.feedback
  not on this TV   com.google.android.videos
== xiaomi/safe.txt
  would disable    com.miui.tv.analytics
...
disabled: 0, already disabled: 0, not present: 1
(dry run, nothing changed)
```

`would disable` means it will be switched off, `not on this TV` means it is skipped, `already disabled` means nothing to
do. If the profile says **"generic only"**, your TV's brand has no profile yet: only Google's own packages are touched,
which is the safe choice. Read the names; every one has a plain-English explanation in `packages/*/*.txt`.

### Step 7.3: disable the safe list

```sh
./scripts/02-debloat.sh
```

Each line now says `disabled` instead of `would disable`. The TV keeps working normally.

### Step 7.4: install the MinimalTV home screen

```sh
./scripts/01-install-launcher.sh
```

It downloads the app from the project's releases page and installs it. You should see `Success` from adb and the
message "Installed. Press Home on the remote...".

- If the download fails (no internet on the computer, or GitHub is blocked), download `minimaltv.apk` yourself from
  https://github.com/AmalDamodar/minimaltv/releases/latest and run `./scripts/01-install-launcher.sh /path/to/minimaltv.apk`
  (on Windows Git Bash a path looks like `/c/Users/you/Downloads/minimaltv.apk`).

### Step 7.5: make it the home screen

Press **Home** on the remote. You should see the MinimalTV home screen (two tabs, For You and Apps).

- If the TV asks **"Which home app do you want to use?"**, choose **MinimalTV** and **Always**.
- If it still shows the old home screen, run this on the computer, then press Home again:
  ```sh
  adb shell cmd package set-home-activity com.minimaltv.home/.MainActivity
  ```

### Step 7.6: switch off the TV's old home screens

Only after MinimalTV opens when you press Home:

```sh
./scripts/02-debloat.sh --stock-launcher
```

The script refuses (and tells you why) if MinimalTV is not the default home screen, so you cannot lock yourself out.

### Step 7.7 (optional): also remove setup wizards, demo mode and backup helpers

Only on a TV that is **already fully set up** (Wi-Fi and Google account done):

```sh
./scripts/02-debloat.sh --dry-run --extra     # look first
./scripts/02-debloat.sh --extra
```

### Step 7.8: speed tweaks

```sh
./scripts/03-tweaks.sh
```

This makes animations twice as fast, stops some streaming apps from running in the background when you are not using
them, and clears cached files. It prints one line for each thing it did. To use different settings see the header of
`scripts/03-tweaks.sh`.

### Step 7.9: turn on remote button support

```sh
./scripts/04-remote-keys.sh
```

You should see `key service on. On the TV: About > Remote buttons > Add a button.`

### Step 7.9b (optional): install the Files app

A small file manager made for the remote (details in [FILES.md](FILES.md)).

```
./scripts/05-install-files.sh
```

You should see `Success`. Open **Files** from the Apps tab.

### Step 7.10: restart the TV

Unplug the TV from the wall for 10 seconds (or use Settings, Device Preferences, Restart), and plug it back in. When it
starts, it should go straight to MinimalTV. The TV's IP address may have changed; if you run more commands later, read
it again (step 3).

---

## 8. Set up the remote's buttons

Many remotes have buttons for Netflix, Prime Video and so on, an app-drawer button, or a button for the TV maker's own
home screen. After step 7 those buttons do nothing. You assign them here, on the TV, with the remote.

1. On the MinimalTV home screen, move **left** to the vertical bar, then **down** to the last icon (the person icon,
   called **About**). Press **OK**.
2. A panel with information about your TV opens. Choose **Remote buttons**.
3. Choose **Add a button**. The screen says **"Press the button on your remote"**.
4. Press the button you want to assign, for example the **Netflix** button. A screen shows its number (for example
   "Button 193").
5. Choose what it should do:
   - **Open the home screen**
   - **Open the Apps tab**
   - **Open an app...**: then pick the app from the list (for example Netflix).
6. You are back at the list, which now shows for example **Button 193 → Netflix**. Choose **Add a button** to assign
   the next one.
7. Press **Back** when you are done.

To change or remove a button, choose it in the list and pick **Remove**.

If nothing happens when you press a button at step 4, the TV keeps that button for itself (power, volume and some voice
buttons work like this) and it cannot be changed. If **Remote buttons** says **"Button support is off"**, run
`./scripts/04-remote-keys.sh` again (step 7.9).

Also in the About panel:

- **Close other apps: On/Off.** When on, opening an app from MinimalTV (or a remote button) closes the one you were in
  before, so apps do not pile up in a small TV's memory. It is **on by default on TVs with less than 3 GB of RAM**.
- **Keep running.** Apps listed here are never closed. Add casting or AirPlay receivers, VPN apps and music players.
- **Next background**, **Dark mode** and **Close**.

**Add or remove favourite apps** on the For You tab: highlight any app and **hold OK** (press and hold). Or choose the
**Add app** tile.

---

## 9. Check that everything works

Go through this list:

- [ ] Press **Home**: MinimalTV opens and shows your favourite apps.
- [ ] Open an app (for example YouTube) and press **Back**: you land on the MinimalTV home screen.
- [ ] A remote button you set up opens its app, also when pressed from inside another app.
- [ ] The **Apps** tab shows every app on the TV.
- [ ] The TV feels quicker when moving between screens.
- [ ] Optional: on the computer run `./scripts/02-debloat.sh --dry-run`. It should now say `already disabled` for the
      things you disabled.

Something wrong? See [When something goes wrong](#when-something-goes-wrong).

---

## 10. Undo, update, turn debugging off

### Undo everything

On the computer, with the TV connected (step 6):

```sh
./scripts/restore.sh
```

This switches every disabled app back on, puts your original home screen back, restores the animation speed and the
background limits, and turns the button support off. Restart the TV afterwards. To remove MinimalTV itself:

```sh
adb uninstall com.minimaltv.home
```

(If the TV has no home screen at all after you uninstall it, run `./scripts/restore.sh` first, then uninstall.)

### Update MinimalTV later

```sh
cd minimaltv
git pull                         # new scripts and lists
./scripts/01-install-launcher.sh # installs the newest app over the old one; your favourites and buttons are kept
```

### Turn debugging off again (recommended when you are finished)

On the TV: **Settings, Device Preferences, Developer options, USB debugging OFF** (and Network or Wireless debugging
OFF if you turned them on). Your settings stay. You can switch debugging on again whenever you need to run a script.

---

## Option B: no computer

This installs only the MinimalTV home screen. There is no debloat, no speed tweaks and no remote button support, because
those need a computer. It uses your TV's own browser-free way of installing apps.

1. **Allow installs from outside the Play Store.** The setting differs by TV; on most it is **Settings, Device
   Preferences, Security & restrictions, Unknown sources**, where you switch it on for the app you will use in step 3.
   If you cannot find it, step 3 will offer to take you there.
2. **Install Downloader** (a free app by AFTVnews) from the TV's **Google Play Store** (search for "Downloader").
3. Open **Downloader**. In the address box type exactly:
   ```
   github.com/AmalDamodar/minimaltv/releases/latest/download/minimaltv.apk
   ```
   and choose **Go**. If Downloader says installing is blocked, choose **Settings** in the popup and allow
   "Install unknown apps" for Downloader, then try again.
4. When the file has downloaded, choose **Install**, then **Open** or **Done**.
5. Press **Home**. If Android asks which home app to use, choose **MinimalTV** and **Always**.
6. **If it does not ask**, your TV keeps its own home screen as the default and only a computer can change that
   (Option A, step 7.5). You can still open MinimalTV from the apps list.

---

## When something goes wrong

| Problem | What to try |
| --- | --- |
| I cannot find Developer options | You did not tap Build seven times, or tapped on the wrong line. Redo step 4A, looking for the line that shows a build number, and press OK seven times quickly. |
| `adb: command not found` / `"$ADB": No such file` | adb is not installed, or on Windows the path is wrong. Redo step 5. Check that `C:\platform-tools\adb.exe` exists. |
| The TV does not show the "Allow USB debugging?" box | Press Home so the TV shows its own screen (the box can hide behind a full-screen app), run `adb disconnect` and `adb connect <ip>:5555` again, or turn USB debugging off and on. |
| `adb devices` says `unauthorized` | Answer the box on the TV (Always allow). If it never appears: in Developer options use **Revoke USB debugging authorizations**, then connect again. |
| `bash: ./scripts/00-backup.sh: Permission denied` | You downloaded a ZIP. Put `bash ` in front: `bash scripts/00-backup.sh`. |
| `$'\r': command not found` (Windows) | The scripts have Windows line endings. Use `git clone` (not the ZIP), or run `git config --global core.autocrlf false`, delete the folder and clone again. |
| The script says it cannot find the TV | `adb devices` must list the TV as `device`. If more than one device is listed, set `ANDROID_SERIAL=<ip>:5555`. |
| After the install the TV shows no home screen | Run `./scripts/restore.sh`, restart the TV. Details in [TROUBLESHOOTING.md](TROUBLESHOOTING.md). |
| A "Which home app?" popup keeps appearing | Choose MinimalTV and **Always**, or run `adb shell cmd package set-home-activity com.minimaltv.home/.MainActivity`. |
| A remote button does nothing | See step 8 and [REMOTE-KEYS.md](REMOTE-KEYS.md). |

More: [TROUBLESHOOTING.md](TROUBLESHOOTING.md). If you are stuck, open an issue at
https://github.com/AmalDamodar/minimaltv/issues and say your TV's brand, model, Android version (Settings, About), your
computer type, the exact command you ran and what it printed.
