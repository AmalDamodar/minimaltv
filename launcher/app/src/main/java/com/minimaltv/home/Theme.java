package com.minimaltv.home;

import android.content.Context;

/** Light (frosted white) or dark (dark glass, dimmer wallpaper) look. The choice is remembered. */
final class Theme {
    private Theme() {}

    static boolean dark;

    static void load(Context c) { dark = c.getSharedPreferences("home", Context.MODE_PRIVATE).getBoolean("dark", false); }

    static void set(Context c, boolean d) {
        dark = d;
        c.getSharedPreferences("home", Context.MODE_PRIVATE).edit().putBoolean("dark", d).apply();
    }

    /** Dimming layer over the wallpaper. */
    static int scrim() { return dark ? 0x99000000 : 0x40000000; }

    /** Fill of a glass panel; over a photo it is frosted white (light) or smoked glass (dark). */
    static int[] glass(boolean photo) {
        if (!photo) return new int[]{0xE6141416, 0xE61A1A1D};
        return dark ? new int[]{0xBF141418, 0xB30C0C10, 0xBF16161B} : new int[]{0x2EFFFFFF, 0x0AFFFFFF, 0x16FFFFFF};
    }

    static int glassEdge(boolean photo) { return photo ? (dark ? 0x2EFFFFFF : 0x66FFFFFF) : 0x33FFFFFF; }

    /** Tile behind an app icon. */
    static int tile() { return dark ? 0x26FFFFFF : 0x59000000; }

    /** Soft fill for list rows, headers and sheet items. */
    static int soft() { return dark ? 0x1CFFFFFF : 0x26FFFFFF; }

    static int chip() { return dark ? 0x24FFFFFF : 0x30FFFFFF; }
    static int chipSelected() { return dark ? 0xFF4A4A50 : 0xF2FFFFFF; }
    static int chipSelectedText() { return dark ? 0xFFFFFFFF : 0xFF101012; }

    /** Dimming behind sheets (search, inputs, about). */
    static int overlay() { return dark ? 0xAA000000 : 0x66000000; }
}
