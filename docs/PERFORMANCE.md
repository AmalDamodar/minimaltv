# What makes it faster

`./scripts/03-tweaks.sh` and the debloat do the following. Measurements are from my TV (MiTV-MOOQ0, 1.8 GB total of
which little is free, Android 10) and were taken once, not averaged, so treat them as a rough guide.

## What changes

1. **Fewer things running.** Disabled system packages (telemetry, content rows, cleaner, Mi Link, sync adapters,
   setup wizards) do not start at boot and do not hold memory. The stock home screens and recommendation rows load content
   rows in the background, which is why they are in their own list.
2. **Animations at 0.5x.** `window_animation_scale`, `transition_animation_scale` and `animator_duration_scale` are
   set to 0.5, so menus and app switches take half as long. This is the cheapest "feels faster" change there is.
   Use `--animations 1.0` to put it back, or `0` to switch animations off.
3. **Background limits.** `cmd appops set <app> RUN_ANY_IN_BACKGROUND ignore` for streaming apps
   (`packages/background-limit.txt`): they cannot start services or wake themselves while you are not using them.
4. **One streaming app at a time.** With **Close other apps** on (About), MinimalTV closes the app you opened earlier when
   you open another one (see [REMOTE-KEYS.md](REMOTE-KEYS.md)). Android would otherwise keep them all in memory behind
   the new one, and Back would walk through them in reverse. It is on by default below 3 GB of RAM.
5. **A launcher that is cheap.** No live blur, no network, RGB_565 backgrounds, drift animation at 30 fps and stopped
   when the launcher is not in front.
6. **Trimmed caches.** `pm trim-caches` frees cached files if storage is short; on my TV there was nothing to free.

## My numbers

- Memory still available (free plus reclaimable), `/proc/meminfo` `MemAvailable`: about 815 MB after the first debloat,
  about 900 MB after the second pass (setup wizards, sync adapters, background limits).
- A reboot with all of this in place reached the home screen with the launcher focused in under about 45 seconds
  (measured once, from sending `reboot`).

## Things that look useful but are not

- **"Clean master" style apps** and RAM boosters. Android already kills cached apps when it needs memory; these only
  add another background app.
- **Developer options, "Background process limit".** Setting it to "at most 1 or 2 processes" tends to make apps restart
  constantly, which is slower overall.
- **Disabling Play Services** or the Play Store. Many apps stop working or crash.

## Private DNS (optional)

`./scripts/03-tweaks.sh --dns dns.adguard-dns.com` sets Android's Private DNS to a blocking resolver. It stops many
ad and tracker domains for every app on the TV. It does not remove YouTube's own video ads. `restore.sh` puts the
previous mode back.
