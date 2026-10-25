package com.minimaltv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The remote buttons the user assigned (MinimalTV > About > Remote buttons). Stored as lines "code|action|package".
 * The launcher writes it and KeyService reads it; both live in the same process, so a static cache is enough.
 */
final class Keymap {
    private Keymap() {}

    /** INPUT keeps the TV input id (HDMI 1, AV, Live TV) in the package column. */
    static final int HOME = 0, APPS = 1, APP = 2, INPUT = 3;

    static final class Entry {
        final int code, action; final String pkg;
        Entry(int code, int action, String pkg) { this.code = code; this.action = action; this.pkg = pkg; }
    }

    /** True while the "press a button" screen is open. KeyService then reports every assignable key to learnListener instead of acting on it. */
    static volatile boolean learning;

    interface Learn { void onKey(int code); }

    /** Called on the main thread with the code of the button that was pressed while learning. */
    static volatile Learn learnListener;

    /** Keys that are never offered for assignment: navigation, and the ones every remote needs. */
    static boolean assignable(int key) {
        switch (key) {
            case android.view.KeyEvent.KEYCODE_DPAD_UP: case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT: case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER: case android.view.KeyEvent.KEYCODE_ENTER:
            case android.view.KeyEvent.KEYCODE_NUMPAD_ENTER: case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_ESCAPE: case android.view.KeyEvent.KEYCODE_HOME:
            case android.view.KeyEvent.KEYCODE_UNKNOWN:
                return false;
            default: return true;
        }
    }

    private static volatile List<Entry> cache;

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences("home", Context.MODE_PRIVATE); }

    static List<Entry> all(Context c) {
        List<Entry> l = cache;
        if (l != null) return l;
        List<Entry> out = new ArrayList<>();
        for (String line : prefs(c).getString("keymap", "").split("\n")) {
            String[] f = line.split("\\|", -1);
            if (f.length != 3) continue;
            try { out.add(new Entry(Integer.parseInt(f[0]), Integer.parseInt(f[1]), f[2])); } catch (NumberFormatException ignored) { }
        }
        cache = Collections.unmodifiableList(out);
        return cache;
    }

    static Entry find(Context c, int code) {
        for (Entry e : all(c)) if (e.code == code) return e;
        return null;
    }

    /** Assigns an action to a button (replacing any earlier one for the same button). */
    static void put(Context c, int code, int action, String pkg) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all(c)) if (e.code != code) out.add(e);
        out.add(new Entry(code, action, pkg == null ? "" : pkg));
        save(c, out);
    }

    static void remove(Context c, int code) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all(c)) if (e.code != code) out.add(e);
        save(c, out);
    }

    private static void save(Context c, List<Entry> l) {
        StringBuilder b = new StringBuilder();
        for (Entry e : l) b.append(b.length() == 0 ? "" : "\n").append(e.code).append('|').append(e.action).append('|').append(e.pkg);
        prefs(c).edit().putString("keymap", b.toString()).apply();
        cache = Collections.unmodifiableList(new ArrayList<>(l));
    }
}
