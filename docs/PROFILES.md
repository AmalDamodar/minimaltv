# Brand profiles

The packages that are safe to disable differ by manufacturer. `packages/generic/` holds the Google and Android packages
that every Android TV has. A **profile** is a folder next to it, named for the brand, with that brand's own packages:

```
packages/
  generic/            used on every TV
    safe.txt  extra.txt  stock-launcher.txt
  xiaomi/             used when the TV reports Xiaomi
    safe.txt  extra.txt  optional.txt  stock-launcher.txt
  skyworth/           used when the TV reports Skyworth, Coocaa or AcerPure (Skyworth's TVs sold in India)
    safe.txt  extra.txt  optional.txt  stock-launcher.txt
  background-limit.txt
```

`./scripts/02-debloat.sh` detects the brand from `ro.product.manufacturer` / `ro.product.brand` and uses
`generic` plus the matching folder. `--profile NAME` picks one by hand, `--profile none` uses only the generic lists.

## File meanings

| File | Contains | Applied when |
| --- | --- | --- |
| `safe.txt` | Telemetry, ads, content rows, "cleaners". Nothing a normal TV needs | by default |
| `extra.txt` | Setup wizards, demo mode, backup helpers. Only after the TV is set up | `--extra` |
| `optional.txt` | Preinstalled apps some people use | `--optional` |
| `stock-launcher.txt` | The manufacturer's and Google's home screens and recommendation rows | `--stock-launcher`, and only once MinimalTV is installed and the default home |

One package per line, `#` starts a comment. Say what the package is: that comment is what protects the next person.

## Write a profile for your brand

1. Connect (`docs/ADB.md`) and run `./scripts/scan.sh`. It prints the device, what uses memory, what starts at boot and
   every system package that is not a plain Android or Google one, and saves that to `~/.minimaltv/scan-<model>.txt`.
2. Go through the list. The manufacturer's own packages (`com.<brand>...`) are the candidates. **Some are essential:**
   anything about TV input, HDMI, the tuner, the remote, Bluetooth, the update service, the hardware abstraction. When
   in doubt leave it out.
3. Look closer at one: `adb shell dumpsys package <pkg> | less` (what it declares) and its name in the memory list.
4. Disable **one at a time** and use the TV: `adb shell pm disable-user --user 0 <pkg>`. Check HDMI, the tuner if you use
   it, the remote, Wi-Fi, and an app or two. Reboot once. If anything breaks: `adb shell pm enable <pkg>`.
5. Put what survived in `packages/<brand>/safe.txt` (and `extra.txt` etc.), each with a comment.
6. Add the brand to `detect_profile` in `scripts/lib.sh` (a `case` on the manufacturer or brand string).
7. Check with `./scripts/02-debloat.sh --dry-run --extra --stock-launcher`, then for real, then `./scripts/restore.sh` to
   prove the undo works.

## Sending a profile

Open a pull request with the folder and the model(s), Android version and RAM you tested on (the header of the scan
file has them). Say what you checked after disabling. A profile that was tried on a real TV is worth far more than a
long list copied from somewhere; do not add packages you have not tested.
