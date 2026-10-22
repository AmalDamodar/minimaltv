package com.minimaltv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Small focusable controls in the glass style: round rail buttons and pill chips. */
final class Widgets {
    private Widgets() {}


    /** Round icon button for the rail. View "selected" = current section (teal), focus = white ring. */
    static FrameLayout railButton(Context c, Ui ui, int iconRes) {
        FrameLayout b = new FrameLayout(c);
        b.setFocusable(true);
        b.setClickable(true);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ui.px(46), ui.px(46));
        lp.gravity = Gravity.CENTER;
        b.addView(icon, lp);
        b.setOnFocusChangeListener((v, f) -> styleRail(ui, b));
        b.setClipChildren(false);
        styleRail(ui, b);
        return b;
    }

    static void styleRail(Ui ui, View b) {
        boolean sel = b.isSelected(), foc = b.hasFocus();
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(sel ? 0x4DFFFFFF : foc ? 0x33FFFFFF : 0x00FFFFFF);
        if (foc) g.setStroke(Math.max(2, ui.px(2.5f)), 0xE6FFFFFF);
        b.setBackground(g);
        b.animate().scaleX(foc ? 1.08f : 1f).scaleY(foc ? 1.08f : 1f).setDuration(140).start();
    }

    /** Pill chip. "selected" = active filter (white), focus = ring. */
    static TextView chip(Context c, Ui ui, String text) {
        TextView t = ui.text(c, 26, Color.WHITE, ui.regular);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setPadding(ui.px(26), 0, ui.px(26), 0);
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnFocusChangeListener((v, f) -> styleChip(ui, t));
        styleChip(ui, t);
        return t;
    }

    static void styleChip(Ui ui, TextView t) {
        boolean sel = t.isSelected(), foc = t.hasFocus();
        GradientDrawable g = ui.round(sel ? Theme.chipSelected() : Theme.chip(), 30);
        if (foc) g.setStroke(Math.max(2, ui.px(2.5f)), sel && !Theme.dark ? 0xCC000000 : 0xE6FFFFFF);
        t.setBackground(g);
        t.setTextColor(sel ? Theme.chipSelectedText() : Color.WHITE);
        t.animate().scaleX(foc ? 1.05f : 1f).scaleY(foc ? 1.05f : 1f).setDuration(120).start();
    }

    static FrameLayout.LayoutParams at(Ui ui, float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : ui.px(w), h < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : ui.px(h));
        lp.leftMargin = ui.px(x);
        lp.topMargin = ui.px(y);
        return lp;
    }
}
