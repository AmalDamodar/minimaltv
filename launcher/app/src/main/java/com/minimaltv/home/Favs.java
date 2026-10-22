package com.minimaltv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Favourite apps (shown in "For You"), kept in order. First run is seeded from Config.DOCK. */
final class Favs {
    private Favs() {}

    private static SharedPreferences prefs;
    private static final List<String> pkgs = new ArrayList<>();

    static synchronized void init(Context c) {
        if (prefs != null) return;
        prefs = c.getApplicationContext().getSharedPreferences("favs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("seeded", false)) {
            for (String p : Config.DOCK) pkgs.add(p);
            prefs.edit().putBoolean("seeded", true).putString("list", join()).apply();
        } else {
            String s = prefs.getString("list", "");
            for (String p : s.split(",")) if (!p.isEmpty()) pkgs.add(p);
        }
    }

    private static String join() {
        StringBuilder b = new StringBuilder();
        for (String p : pkgs) b.append(b.length() == 0 ? "" : ",").append(p);
        return b.toString();
    }

    static synchronized boolean has(String pkg) { return pkgs.contains(pkg); }

    static synchronized List<String> list() { return new ArrayList<>(pkgs); }

    /** @return true when the app is now a favourite */
    static synchronized boolean toggle(String pkg) {
        boolean now;
        if (pkgs.contains(pkg)) { pkgs.remove(pkg); now = false; }
        else { pkgs.add(pkg); now = true; }
        prefs.edit().putString("list", join()).apply();
        return now;
    }

    /**
     * Anything new is pinned automatically: an app that was never seen before is appended. The first run only
     * records what is installed, so nothing but Config.DOCK is pinned then.
     *
     * @return true when "For You" changed
     */
    static synchronized boolean pinNewApps(Collection<String> installed) {
        boolean first = !prefs.contains("seenApps");
        Set<String> seen = readSet("seenApps");
        if (first) seen.addAll(installed);
        boolean changed = false;
        for (String p : installed) {
            if (seen.add(p)) {
                if (!pkgs.contains(p)) { pkgs.add(p); changed = true; }
            }
        }
        prefs.edit().putString("seenApps", String.join("\n", seen)).putString("list", join()).apply();
        return changed;
    }

    private static Set<String> readSet(String key) {
        Set<String> out = new HashSet<>();
        for (String s : prefs.getString(key, "").split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }
}
