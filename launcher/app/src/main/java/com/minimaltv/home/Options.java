package com.minimaltv.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Close other apps": when you open an app from the launcher (or a remote button) and then another one, the first is
 * closed so apps do not pile up in the memory of a small TV. Only apps that were opened from here are ever closed, and
 * apps on the "keep running" list never are.
 */
final class Options {
    private Options() {}

    private static final int MAX_OPENED = 12;

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences("home", Context.MODE_PRIVATE); }

    /** On by default for TVs with less than 3 GB of RAM, off otherwise. */
    static boolean closeApps(Context c) {
        SharedPreferences p = prefs(c);
        if (p.contains("closeApps")) return p.getBoolean("closeApps", false);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
        return mi.totalMem < 3L * 1024 * 1024 * 1024;
    }

    static void setCloseApps(Context c, boolean on) { prefs(c).edit().putBoolean("closeApps", on).apply(); }

    // ---- apps that must keep running (casting receivers, VPNs, music players...)

    static synchronized Set<String> keep(Context c) { return read(c, "keep"); }

    /** @return true when the app is now on the list */
    static synchronized boolean toggleKeep(Context c, String pkg) {
        Set<String> s = read(c, "keep");
        boolean now = s.add(pkg);
        if (!now) s.remove(pkg);
        write(c, "keep", s);
        return now;
    }

    // ---- apps opened from the launcher, oldest first

    static synchronized void markOpened(Context c, String pkg) {
        if (pkg == null || pkg.equals(c.getPackageName())) return;
        Set<String> s = read(c, "opened");
        s.remove(pkg);
        s.add(pkg);
        List<String> l = new ArrayList<>(s);
        while (l.size() > MAX_OPENED) l.remove(0);
        write(c, "opened", new LinkedHashSet<>(l));
    }

    static synchronized Set<String> opened(Context c) { return read(c, "opened"); }

    static synchronized void forget(Context c, String pkg) {
        Set<String> s = read(c, "opened");
        if (s.remove(pkg)) write(c, "opened", s);
    }

    private static Set<String> read(Context c, String key) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : prefs(c).getString(key, "").split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static void write(Context c, String key, Set<String> s) { prefs(c).edit().putString(key, String.join("\n", s)).apply(); }
}
