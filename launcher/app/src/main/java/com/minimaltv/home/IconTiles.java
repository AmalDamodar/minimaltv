package com.minimaltv.home;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Turns any launcher icon into one clean rounded square. Full-bleed art is kept as it is, art with
 * letterbox bars is trimmed, and art on a transparent background is centred on a calm tile, so every
 * app in the grid has the same shape and weight.
 */
final class IconTiles {
    private IconTiles() {}

    private static final int RAW = 256, OUT = 256;
    private static final float RADIUS = 0.235f;

    static Drawable tile(Resources res, Drawable src) {
        if (src == null) return null;
        try {
            Bitmap raw = render(src);
            int[] px = new int[RAW * RAW];
            raw.getPixels(px, 0, RAW, 0, 0, RAW, RAW);
            Bitmap out = Bitmap.createBitmap(OUT, OUT, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, OUT, OUT), OUT * RADIUS, OUT * RADIUS, Path.Direction.CW);
            c.clipPath(clip);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

            Rect box = opaqueBox(px);
            if (box == null) return src;
            boolean fullBleed = cornersOpaque(px) || (box.width() >= RAW * 0.92f && box.height() >= RAW * 0.92f);
            if (fullBleed) {
                Rect crop = trimBands(px);
                c.drawBitmap(raw, centreSquare(crop), new Rect(0, 0, OUT, OUT), p);
            } else if (solidness(px, box) >= 0.85f) {
                // a solid card with transparent margins (e.g. a wide white banner): fill the tile with its centre
                c.drawBitmap(raw, centreSquare(box), new Rect(0, 0, OUT, OUT), p);
            } else {
                c.drawColor(luminance(px) < 0.55f ? 0xFFEDEDF2 : 0xFF202026);
                float inner = OUT * 0.68f;
                float scale = Math.min(inner / box.width(), inner / box.height());
                float w = box.width() * scale, h = box.height() * scale;
                RectF dst = new RectF((OUT - w) / 2f, (OUT - h) / 2f, (OUT + w) / 2f, (OUT + h) / 2f);
                c.drawBitmap(raw, box, dst, p);
            }
            return new BitmapDrawable(res, out);
        } catch (Exception e) {
            return src;
        }
    }

    private static Bitmap render(Drawable d) {
        Bitmap b = Bitmap.createBitmap(RAW, RAW, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (d instanceof AdaptiveIconDrawable) {
            // the layers are 108 units wide and only the middle 72 are visible: draw them oversized so the art fills the square
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) d;
            int m = RAW / 4;
            if (a.getBackground() != null) { a.getBackground().setBounds(-m, -m, RAW + m, RAW + m); a.getBackground().draw(c); }
            if (a.getForeground() != null) { a.getForeground().setBounds(-m, -m, RAW + m, RAW + m); a.getForeground().draw(c); }
        } else {
            d.setBounds(0, 0, RAW, RAW);
            d.draw(c);
        }
        return b;
    }

    private static Rect opaqueBox(int[] px) {
        int l = RAW, t = RAW, r = -1, b = -1;
        for (int y = 0; y < RAW; y++) for (int x = 0; x < RAW; x++) {
            if (Color.alpha(px[y * RAW + x]) > 24) { if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y; }
        }
        return r < 0 ? null : new Rect(l, t, r + 1, b + 1);
    }

    /** Share of the box that is opaque. */
    private static float solidness(int[] px, Rect box) {
        int n = 0;
        for (int y = box.top; y < box.bottom; y++) for (int x = box.left; x < box.right; x++) if (Color.alpha(px[y * RAW + x]) > 200) n++;
        return n / (float) (box.width() * box.height());
    }

    private static boolean cornersOpaque(int[] px) {
        int n = 5;
        int[][] at = { {0, 0}, {RAW - n, 0}, {0, RAW - n}, {RAW - n, RAW - n} };
        for (int[] o : at) for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            if (Color.alpha(px[(o[1] + y) * RAW + o[0] + x]) < 250) return false;
        }
        return true;
    }

    private static float luminance(int[] px) {
        double sum = 0;
        int n = 0;
        for (int v : px) {
            if (Color.alpha(v) < 128) continue;
            sum += (0.299 * Color.red(v) + 0.587 * Color.green(v) + 0.114 * Color.blue(v)) / 255.0;
            n++;
        }
        return n == 0 ? 0.5f : (float) (sum / n);
    }

    /**
     * Crop off letterbox bars: a run of rows (or columns) in one solid colour, directly followed by rows of a
     * different solid colour. A plain background that simply surrounds the art is left alone.
     */
    private static Rect trimBands(int[] px) {
        int max = RAW / 5;
        int t = bar(px, max, false, false), b = bar(px, max, false, true);
        int l = bar(px, max, true, false), r = bar(px, max, true, true);
        if (t + b + l + r < 2) return new Rect(0, 0, RAW, RAW);
        return new Rect(l, t, RAW - r, RAW - b);
    }

    /** Thickness of the bar on one side, 0 when there is none. */
    private static int bar(int[] px, int max, boolean column, boolean far) {
        int i = 0;
        int[] first = solid(px, at(0, far), column);
        if (first == null) return 0;
        while (i < max) {
            int[] m = solid(px, at(i, far), column);
            if (m == null || dist(m, first) > 24) break;
            i++;
        }
        if (i == 0 || i >= max) return 0;
        int[] next = solid(px, at(i, far), column);
        return next != null && dist(next, first) >= 36 ? i : 0;
    }

    private static int at(int i, boolean far) { return far ? RAW - 1 - i : i; }

    private static int dist(int[] a, int[] b) { return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]); }

    /** Mean colour of a row or column when it is a single solid colour, else null. */
    private static int[] solid(int[] px, int index, boolean column) {
        long r = 0, g = 0, b = 0;
        for (int i = 0; i < RAW; i++) {
            int v = column ? px[i * RAW + index] : px[index * RAW + i];
            r += Color.red(v); g += Color.green(v); b += Color.blue(v);
        }
        int[] m = { (int) (r / RAW), (int) (g / RAW), (int) (b / RAW) };
        for (int i = 0; i < RAW; i++) {
            int v = column ? px[i * RAW + index] : px[index * RAW + i];
            if (Math.abs(Color.red(v) - m[0]) > 20 || Math.abs(Color.green(v) - m[1]) > 20 || Math.abs(Color.blue(v) - m[2]) > 20) return null;
        }
        return m;
    }

    /** The largest centred square inside a rect, so a trimmed icon fills the tile without stretching. */
    private static Rect centreSquare(Rect r) {
        int s = Math.min(r.width(), r.height());
        int x = r.left + (r.width() - s) / 2, y = r.top + (r.height() - s) / 2;
        return new Rect(x, y, x + s, y + s);
    }
}
