package com.minimaltv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Shared look: 1920x1080 design space scaled by screen height, Outfit type, glass fills. Matches the launcher. */
final class Ui {
    final float u;
    final Typeface thin, light, regular, medium;

    Ui(Context c) {
        u = c.getResources().getDisplayMetrics().heightPixels / 1080f;
        thin = font(c, 200); light = font(c, 300); regular = font(c, 400); medium = font(c, 500);
    }

    private static Typeface font(Context c, int wght) {
        try {
            Typeface t = new Typeface.Builder(c.getAssets(), "Outfit.ttf").setFontVariationSettings("'wght' " + wght).build();
            if (t != null) return t;
        } catch (Exception ignored) { }
        return Typeface.DEFAULT;
    }

    int px(float v) { return Math.round(v * u); }

    TextView text(Context c, float size, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * u);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        return t;
    }

    GradientDrawable round(int fill, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius * u);
        return g;
    }

    GradientDrawable ring(int fill, int stroke, float radius, float strokePx) {
        GradientDrawable g = round(fill, radius);
        g.setStroke(Math.max(2, px(strokePx)), stroke);
        return g;
    }

    /** Parents clip their children by default, which cuts off scaled/ringed focus states at the edges. Turn that off for a whole tree. */
    static void unclip(View v) {
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            g.setClipChildren(false);
            g.setClipToPadding(false);
            for (int i = 0; i < g.getChildCount(); i++) unclip(g.getChildAt(i));
        }
    }

    static final int WHITE = Color.WHITE;
    static final int DIM = 0x99FFFFFF;
}
