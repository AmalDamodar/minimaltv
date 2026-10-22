package com.minimaltv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Wi-Fi fan with the signal drawn in white and the rest dimmed; a small plug for Ethernet; struck through when offline. */
final class WifiView extends View {
    static final int WIFI = 0, ETHERNET = 1, OFFLINE = 2;

    private final Paint on = new Paint(Paint.ANTI_ALIAS_FLAG), off = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int mode = OFFLINE, level;     // level 0..4

    WifiView(Context c) {
        super(c);
        on.setStyle(Paint.Style.STROKE);
        on.setStrokeCap(Paint.Cap.ROUND);
        on.setColor(0xFFFFFFFF);
        off.setStyle(Paint.Style.STROKE);
        off.setStrokeCap(Paint.Cap.ROUND);
        off.setColor(0x4DFFFFFF);
    }

    void set(int mode, int level) {
        if (this.mode == mode && this.level == level) return;
        this.mode = mode;
        this.level = level;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float stroke = w * 0.095f;
        on.setStrokeWidth(stroke);
        off.setStrokeWidth(stroke);
        if (mode == ETHERNET) {
            // a socket with three contacts
            float l = w * 0.14f, r = w * 0.86f, t = h * 0.22f, b = h * 0.66f;
            box.set(l, t, r, b);
            c.drawRoundRect(box, w * 0.1f, w * 0.1f, on);
            for (int i = 0; i < 3; i++) {
                float x = l + (r - l) * (i + 1) / 4f;
                c.drawLine(x, t + stroke * 1.6f, x, t + (b - t) * 0.52f, on);
            }
            c.drawLine(w * 0.5f, b, w * 0.5f, h * 0.86f, on);
            return;
        }
        float cx = w / 2f, cy = h * 0.88f;
        int shown = mode == OFFLINE ? 0 : level;
        float[] radius = {w * 0.22f, w * 0.40f, w * 0.58f};
        for (int i = 2; i >= 0; i--) {
            box.set(cx - radius[i], cy - radius[i], cx + radius[i], cy + radius[i]);
            c.drawArc(box, 225, 90, false, shown >= i + 2 ? on : off);
        }
        Paint dot = shown >= 1 ? on : off;
        dot.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy - stroke * 0.2f, stroke * 0.85f, dot);
        dot.setStyle(Paint.Style.STROKE);
        if (mode == OFFLINE) c.drawLine(w * 0.12f, h * 0.12f, w * 0.88f, h * 0.9f, on);
    }
}
