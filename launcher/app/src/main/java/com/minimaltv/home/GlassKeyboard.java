package com.minimaltv.home;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * D-pad friendly on-screen keyboard on a glass panel. The owner supplies live results through the query callback and may
 * place its own views in {@link #resultsHost}. Also accepts a physical keyboard.
 */
final class GlassKeyboard {
    interface Listener { void onQuery(String q); void onDone(); }

    final GlassPanel panel;
    final FrameLayout resultsHost;
    private final Ui ui;
    private final Listener listener;
    private final TextView field, status;
    private final StringBuilder q = new StringBuilder();
    private View firstKey;

    GlassKeyboard(Context c, Ui ui, Background bg, Listener l, boolean withResults) {
        this.ui = ui;
        this.listener = l;
        panel = new GlassPanel(c, ui, bg, 48);

        TextView title = ui.text(c, 26, 0xB3FFFFFF, ui.regular);
        title.setText("SEARCH");
        panel.addView(title, Widgets.at(ui, 56, 36, 400, 34));

        field = ui.text(c, 54, Color.WHITE, ui.light);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(ui.px(30), 0, ui.px(30), 0);
        field.setBackground(ui.round(0x26000000, 38));
        panel.addView(field, Widgets.at(ui, 56, 76, 1288, 84));

        status = ui.text(c, 24, 0x99FFFFFF, ui.light);
        panel.addView(status, Widgets.at(ui, 70, 170, 1200, 32));

        // QWERTY layout, rows centred so the letter rows keep their stagger
        LinearLayout board = new LinearLayout(c);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setGravity(Gravity.CENTER_HORIZONTAL);
        int kw = ui.px(112), kh = ui.px(78);
        String[] rowsDef = {"1234567890", "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};
        for (String r : rowsDef) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            for (int i = 0; i < r.length(); i++) row.addView(key(c, String.valueOf(r.charAt(i)), String.valueOf(r.charAt(i)), kw, kh, 1));
            board.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.px(90)));
        }
        LinearLayout bottom = new LinearLayout(c);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.addView(key(c, "Space", " ", kw, kh, 4));
        bottom.addView(key(c, "⌫  Delete", "\b", kw, kh, 2));
        bottom.addView(key(c, "Clear", "\u0000", kw, kh, 2));
        bottom.addView(key(c, "Done", "\n", kw, kh, 2));
        board.addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.px(90)));
        panel.addView(board, Widgets.at(ui, 56, 214, 1288, 5 * 90 + 10));

        if (withResults) {
            resultsHost = new FrameLayout(c);
            panel.addView(resultsHost, Widgets.at(ui, 40, 710, 1320, 240));
        } else {
            resultsHost = null;
        }
        Ui.unclip(panel);
        refresh();
    }

    private TextView key(Context c, String label, String code, int w, int h, int span) {
        TextView t = Widgets.chip(c, ui, label);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 32 * ui.u);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(span * (w + ui.px(12)) - ui.px(12), h);
        lp.setMargins(ui.px(6), ui.px(6), ui.px(6), ui.px(6));
        t.setLayoutParams(lp);
        if (code.equals("\n")) { t.setSelected(true); Widgets.styleChip(ui, t); }
        t.setOnClickListener(v -> type(code));
        if (firstKey == null) firstKey = t;
        return t;
    }

    private void type(String code) {
        switch (code) {
            case "\n": listener.onDone(); return;
            case "\b": if (q.length() > 0) q.setLength(q.length() - 1); break;
            case "\u0000": q.setLength(0); break;
            default: if (q.length() < 40) q.append(code.toLowerCase());
        }
        refresh();
        listener.onQuery(q.toString());
    }

    private void refresh() {
        field.setText(q.length() == 0 ? "Type to search" : q + "▏");
        field.setTextColor(q.length() == 0 ? 0x80FFFFFF : Color.WHITE);
    }

    void setStatus(String s) { status.setText(s); }

    String query() { return q.toString(); }

    void setQuery(String s) { q.setLength(0); q.append(s); refresh(); }

    void focusFirst() { if (firstKey != null) firstKey.requestFocus(); }

    /** Physical keyboard support. @return true if consumed */
    boolean handleKey(int key, KeyEvent ev) {
        if (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z) { type(String.valueOf((char) ('A' + key - KeyEvent.KEYCODE_A))); return true; }
        if (key >= KeyEvent.KEYCODE_0 && key <= KeyEvent.KEYCODE_9) { type(String.valueOf((char) ('0' + key - KeyEvent.KEYCODE_0))); return true; }
        if (key == KeyEvent.KEYCODE_SPACE) { type(" "); return true; }
        if (key == KeyEvent.KEYCODE_DEL) { type("\b"); return true; }
        return false;
    }
}
