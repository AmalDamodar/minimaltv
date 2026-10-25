package com.minimaltv.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Liquid Glass home: a drifting photo, a glass rail (Search, Home, Settings | Inputs, About), a For You / Apps tab
 * pill and one frosted panel. For You = favourite apps, Apps = everything installed.
 */
public class MainActivity extends Activity {
    private static final int TAB_FOR_YOU = 0, TAB_APPS = 1, TAB_COUNT = 2, SPANS = 40;
    private static final int OV_NONE = 0, OV_SHEET = 1, OV_SEARCH = 2, OV_ABOUT = 3, OV_LEARN = 4;
    private static final String[] TAB_NAMES = {"For You", "Apps"};

    /** The "Add app" tile at the end of For You. */
    private static final class AddTile { }

    private static final class InputItem {
        final String id, label; final Intent intent; final boolean live;
        InputItem(String id, String l, Intent i, boolean live) { this.id = id; label = l; intent = i; this.live = live; }
    }

    private Ui ui;
    private Background bg;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private FrameLayout root, layer, overlay;
    private GlassPanel panel;
    private FrameLayout bSearch, bHome, bSettings, bInputs, bAbout;
    private WifiView wifi;
    private TextView dateView;
    private final TextView[] tabViews = new TextView[TAB_COUNT];
    private TextView clock, toast;
    private RecyclerView grid;
    private GridLayoutManager glm;
    private GridAdapter adapter;
    private GlassKeyboard kb;
    private RecyclerView searchResults;
    private final List<AppEntry> searchItems = new ArrayList<>();

    private List<AppEntry> apps = new ArrayList<>();
    private List<Object> rows = new ArrayList<>();
    private int tab;
    private int overlayKind = OV_NONE;
    private View returnFocus;
    private int pendingFocus = -1;
    private boolean longHandled, receiversOn;
    private ConnectivityManager cm;
    private final List<InputItem> inputItems = new ArrayList<>();

    // ---------------------------------------------------------------- lifecycle

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ui = new Ui(this);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Favs.init(this);
        Theme.load(this);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        tab = Math.max(0, Math.min(TAB_COUNT - 1, prefs.getInt("tab", TAB_FOR_YOU)));
        buildUi();
        loadApps();
    }

    @Override protected void onStart() {
        super.onStart();
        bg.start();
        registerReceivers();
        updateClock();
    }

    @Override protected void onStop() {
        super.onStop();
        bg.stop();
        unregisterReceivers();
        hideOverlay(false);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // HOME pressed while already here: back to the first tab
        hideOverlay(false);
        if (intent.getBooleanExtra(KeyService.EXTRA_OPEN_APPS, false)) {   // remote's app drawer button
            if (tab != TAB_APPS) setTab(TAB_APPS, true);
            else focusFirstTile();
        } else if (tab != TAB_FOR_YOU) setTab(TAB_FOR_YOU, true);
        else focusFirstTile();
    }

    @Override protected void onDestroy() { super.onDestroy(); io.shutdownNow(); }

    // ---------------------------------------------------------------- UI

    private FrameLayout railBtn(LinearLayout parent, int icon, Runnable action) {
        FrameLayout b = Widgets.railButton(this, ui, icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(84), ui.px(84));
        lp.topMargin = ui.px(8);
        lp.bottomMargin = ui.px(8);
        parent.addView(b, lp);
        b.setOnClickListener(v -> { returnFocus = v; action.run(); });
        return b;
    }

    private void buildUi() {
        root = new FrameLayout(this);
        setContentView(root);
        bg = new Background(this, ui, root);
        layer = new FrameLayout(this);
        root.addView(layer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // rail: Search, Home, Settings | Inputs, About
        GlassPanel rail = new GlassPanel(this, ui, bg, 54);
        LinearLayout rl = new LinearLayout(this);
        rl.setOrientation(LinearLayout.VERTICAL);
        rl.setGravity(Gravity.CENTER_HORIZONTAL);
        rl.setPadding(0, ui.px(28), 0, ui.px(28));
        rail.addView(rl, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bSearch = railBtn(rl, R.drawable.ic_search, this::openSearch);
        bHome = railBtn(rl, R.drawable.ic_home, () -> { setTab(TAB_FOR_YOU, true); });
        bSettings = railBtn(rl, R.drawable.ic_settings, () -> startSafe(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        View gap = new View(this);
        rl.addView(gap, new LinearLayout.LayoutParams(1, 0, 1f));
        bInputs = railBtn(rl, R.drawable.ic_input, this::openInputs);
        bAbout = railBtn(rl, R.drawable.ic_person, this::openAbout);
        bHome.setSelected(true);
        Widgets.styleRail(ui, bHome);
        layer.addView(rail, Widgets.at(ui, 56, 96, 108, 900));

        // tab pill
        GlassPanel pill = new GlassPanel(this, ui, bg, 44);
        LinearLayout tl = new LinearLayout(this);
        tl.setOrientation(LinearLayout.HORIZONTAL);
        tl.setGravity(Gravity.CENTER_VERTICAL);
        tl.setPadding(ui.px(10), 0, ui.px(10), 0);
        pill.addView(tl, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < TAB_COUNT; i++) {
            final int idx = i;
            TextView t = Widgets.chip(this, ui, TAB_NAMES[i]);
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 30 * ui.u);
            t.setSelected(i == tab);
            Widgets.styleChip(ui, t);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(184), ui.px(70));
            lp.leftMargin = ui.px(4);
            lp.rightMargin = ui.px(4);
            tl.addView(t, lp);
            tabViews[i] = t;
            t.setOnFocusChangeListener((v, f) -> { Widgets.styleChip(ui, t); if (f && tab != idx) setTab(idx, false); });
            t.setOnClickListener(v -> { if (tab != idx) setTab(idx, false); focusFirstTile(); });
        }
        layer.addView(pill, Widgets.at(ui, 214, 60, TAB_COUNT * 192 + 20, 90));

        // top right: Wi-Fi, time with AM/PM, and the date under it
        LinearLayout statusCol = new LinearLayout(this);
        statusCol.setOrientation(LinearLayout.VERTICAL);
        statusCol.setGravity(Gravity.END);
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        wifi = new WifiView(this);
        timeRow.addView(wifi, new LinearLayout.LayoutParams(ui.px(44), ui.px(38)));
        clock = ui.text(this, 46, Ui.WHITE, ui.light);
        clock.setGravity(Gravity.END);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.leftMargin = ui.px(22);
        timeRow.addView(clock, cl);
        statusCol.addView(timeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(58)));
        dateView = ui.text(this, 25, 0xB3FFFFFF, ui.regular);
        dateView.setGravity(Gravity.END);
        statusCol.addView(dateView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(34)));
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(ui.px(520), ViewGroup.LayoutParams.WRAP_CONTENT);
        sl.leftMargin = ui.px(1344);
        sl.topMargin = ui.px(58);
        layer.addView(statusCol, sl);

        // content panel
        panel = new GlassPanel(this, ui, bg, 46);
        layer.addView(panel, Widgets.at(ui, 214, 176, 1650, 860));
        grid = new RecyclerView(this);
        glm = new GridLayoutManager(this, SPANS);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int p) {
                return tab == TAB_FOR_YOU ? 8 : 5;   // For You: 5 big tiles per row, Apps: 8 smaller ones
            }
        });
        grid.setLayoutManager(glm);
        grid.setClipToPadding(false);
        grid.setPadding(ui.px(28), ui.px(28), ui.px(28), ui.px(40));
        grid.setClipChildren(false);
        grid.setItemAnimator(null);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new GridAdapter(rows);
        grid.setAdapter(adapter);
        panel.addView(grid, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Theme.overlay());
        overlay.setVisibility(View.GONE);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Ui.unclip(layer);
        kb = new GlassKeyboard(this, ui, bg, new GlassKeyboard.Listener() {
            @Override public void onQuery(String q) { runSearch(q); }
            @Override public void onDone() { hideOverlay(true); }
        }, true);
        searchResults = new RecyclerView(this);
        searchResults.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        searchResults.setClipToPadding(false);
        searchResults.setPadding(ui.px(16), 0, ui.px(16), 0);
        searchResults.setItemAnimator(null);
        searchResults.setAdapter(new SearchAdapter());
        kb.resultsHost.addView(searchResults, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** True when the focused view is a tile of the grid. */
    private boolean gridFocused() {
        if (overlayKind != OV_NONE) return false;
        View f = getCurrentFocus();
        return f != null && f.getParent() == grid;
    }

    // ---------------------------------------------------------------- data

    private void loadApps() {
        io.execute(() -> {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> ris = pm.queryIntentActivities(q, 0);
            Set<String> hidden = new HashSet<>();
            Collections.addAll(hidden, Config.HIDDEN);
            List<AppEntry> all = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (ResolveInfo ri : ris) {
                String pkg = ri.activityInfo.packageName;
                if (hidden.contains(pkg) || !seen.add(pkg)) continue;
                Drawable rawIcon = bestIcon(pm, ri);
                Drawable icon = IconTiles.tile(getResources(), rawIcon);
                all.add(new AppEntry(pkg, String.valueOf(ri.loadLabel(pm)), new ComponentName(pkg, ri.activityInfo.name), null, icon));
            }
            Collections.sort(all, (a, b) -> a.label.compareToIgnoreCase(b.label));
            List<String> pkgsSorted = new ArrayList<>();
            for (AppEntry a : all) pkgsSorted.add(a.pkg);
            Favs.pinNewApps(pkgsSorted);
            runOnUiThread(() -> { apps = all; setTab(tab, true); });
        });
    }

    /** The launcher icon at the largest density the app ships, so small bitmaps are not blown up from a low-density copy. */
    private Drawable bestIcon(PackageManager pm, ResolveInfo ri) {
        Drawable d = ri.loadIcon(pm);
        try {
            int id = ri.activityInfo.icon != 0 ? ri.activityInfo.icon : ri.activityInfo.applicationInfo.icon;
            if (id != 0 && !(d instanceof android.graphics.drawable.AdaptiveIconDrawable)) {
                Drawable big = pm.getResourcesForApplication(ri.activityInfo.applicationInfo).getDrawableForDensity(id, 640, null);
                if (big != null && big.getIntrinsicWidth() > d.getIntrinsicWidth()) return big;
            }
        } catch (Exception ignored) { }
        return d;
    }

    private AppEntry appFor(String pkg) {
        for (AppEntry e : apps) if (e.pkg.equals(pkg)) return e;
        return null;
    }

    private void setTab(int t, boolean focus) {
        tab = t;
        prefs.edit().putInt("tab", t).apply();
        for (int i = 0; i < TAB_COUNT; i++) { tabViews[i].setSelected(i == t); Widgets.styleChip(ui, tabViews[i]); }
        rebuild();
        if (focus) focusFirstTile();
    }

    private void rebuild() {
        rows = new ArrayList<>();
        if (tab == TAB_FOR_YOU) {
            for (String p : Favs.list()) { AppEntry e = appFor(p); if (e != null) rows.add(e); }
            rows.add(new AddTile());       // the list always ends with an add tile, so it is never an empty hole
        } else {
            rows.addAll(apps);
        }
        adapter.data = rows;
        adapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------------------- focus helpers

    private void focusFirstTile() {
        String last = prefs.getString("lastPkg", null);
        int pos = -1;
        if (tab == TAB_APPS && last != null) {
            for (int i = 0; i < rows.size(); i++) {
                Object o = rows.get(i);
                if (o instanceof AppEntry && ((AppEntry) o).pkg.equals(last)) { pos = i; break; }
            }
        }
        if (pos < 0) pos = rows.isEmpty() ? -1 : 0;
        if (pos < 0) { tabViews[tab].requestFocus(); return; }
        focusIn(pos);
    }

    /** Focus a tile; if it is not laid out yet the adapter takes focus when it attaches. */
    private void focusIn(int pos) {
        if (rows.isEmpty()) { tabViews[tab].requestFocus(); return; }
        pos = Math.max(0, Math.min(pos, rows.size() - 1));
        pendingFocus = pos;
        grid.scrollToPosition(pos);
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh != null) { pendingFocus = -1; vh.itemView.requestFocus(); }
    }

    /** Position of the focused tile in the grid, or -1. */
    private int focusedTile() {
        if (!gridFocused()) return -1;
        int pos = grid.getChildAdapterPosition(getCurrentFocus());
        return pos >= 0 && pos < rows.size() ? pos : -1;
    }

    /** Tile in the next row of the grid that sits closest to the column of pos, or -1 when pos is in the last row. */
    private int gridBelow(int pos) {
        GridLayoutManager.SpanSizeLookup l = glm.getSpanSizeLookup();
        int group = l.getSpanGroupIndex(pos, SPANS);
        float cx = l.getSpanIndex(pos, SPANS) + l.getSpanSize(pos) / 2f;
        int target = -1, targetGroup = -1;
        float best = Float.MAX_VALUE;
        for (int p = pos + 1; p < rows.size(); p++) {
            int g = l.getSpanGroupIndex(p, SPANS);
            if (g == group) continue;
            if (targetGroup >= 0 && g != targetGroup) break;
            targetGroup = g;
            float d = Math.abs(l.getSpanIndex(p, SPANS) + l.getSpanSize(p) / 2f - cx);
            if (d < best) { best = d; target = p; }
        }
        return target;
    }

    private static boolean isSelect(int key) {
        return key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private int focusedTabIndex() {
        View f = getCurrentFocus();
        for (int i = 0; i < TAB_COUNT; i++) if (tabViews[i] == f) return i;
        return -1;
    }

    private boolean onRail(View f) { return f == bSearch || f == bHome || f == bSettings || f == bInputs || f == bAbout; }

    // ---------------------------------------------------------------- keys

    @Override public boolean onKeyDown(int key, KeyEvent ev) {
        if (overlayKind == OV_LEARN) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) { hideOverlay(true); return true; }
            if (ev.getRepeatCount() == 0 && Keymap.assignable(key)) { learned(key); }   // keys that reach the app (the service reports the rest)
            return true;
        }
        if (overlayKind != OV_NONE) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) { hideOverlay(true); return true; }
            if (overlayKind == OV_SEARCH && kb.handleKey(key, ev)) return true;
            return super.onKeyDown(key, ev);
        }
        View f = getCurrentFocus();
        if (key == KeyEvent.KEYCODE_BACK) {
            if (tab != TAB_FOR_YOU) setTab(TAB_FOR_YOU, true);
            return true;                                   // the launcher never exits
        }
        int pos = focusedTile();
        if (pos >= 0) {
            if (isSelect(key)) {
                if (ev.getRepeatCount() == 0) { longHandled = false; ev.startTracking(); }
                return true;
            }
            GridLayoutManager.SpanSizeLookup l = glm.getSpanSizeLookup();
            if (key == KeyEvent.KEYCODE_DPAD_UP && l.getSpanGroupIndex(pos, SPANS) == 0) { tabViews[tab].requestFocus(); return true; }
            if (key == KeyEvent.KEYCODE_DPAD_LEFT && l.getSpanIndex(pos, SPANS) == 0) { bHome.requestFocus(); return true; }
            // down / right stay inside the list: automatic focus search can leave it for the rail
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) { int t = gridBelow(pos); if (t >= 0) focusIn(t); return true; }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                int n = pos + 1;
                if (n < rows.size() && l.getSpanGroupIndex(n, SPANS) == l.getSpanGroupIndex(pos, SPANS)) focusIn(n);
                return true;
            }
        }
        int ti = focusedTabIndex();
        if (ti >= 0) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) { if (ti == 0) bHome.requestFocus(); else tabViews[ti - 1].requestFocus(); return true; }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) { if (ti < TAB_COUNT - 1) tabViews[ti + 1].requestFocus(); return true; }
            if (key == KeyEvent.KEYCODE_DPAD_UP) return true;
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) { focusFirstTile(); return true; }
        }
        if (onRail(f)) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) return true;
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) { tabViews[tab].requestFocus(); return true; }
            // one fixed step through the rail; never let automatic focus search jump into the content
            View[] rail = {bSearch, bHome, bSettings, bInputs, bAbout};
            int ri = 0;
            for (int i = 0; i < rail.length; i++) if (rail[i] == f) ri = i;
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) { if (ri < rail.length - 1) rail[ri + 1].requestFocus(); return true; }
            if (key == KeyEvent.KEYCODE_DPAD_UP) { if (ri > 0) rail[ri - 1].requestFocus(); return true; }
        }
        return super.onKeyDown(key, ev);
    }

    @Override public boolean onKeyLongPress(int key, KeyEvent ev) {
        int pos = focusedTile();
        if (isSelect(key) && pos >= 0) {
            longHandled = true;
            Object o = rows.get(pos);
            if (o instanceof AppEntry) toggleFavApp((AppEntry) o, pos);
            return true;
        }
        return super.onKeyLongPress(key, ev);
    }

    @Override public boolean onKeyUp(int key, KeyEvent ev) {
        int pos = focusedTile();
        if (isSelect(key) && pos >= 0) {
            if (!longHandled && !ev.isCanceled()) activateRow(rows.get(pos));
            longHandled = false;
            return true;
        }
        return super.onKeyUp(key, ev);
    }

    private void activateRow(Object o) {
        if (o instanceof AddTile) {
            returnFocus = null;
            openAppPicker();
        } else if (o instanceof AppEntry) {
            AppEntry e = (AppEntry) o;
            prefs.edit().putString("lastPkg", e.pkg).apply();
            startSafe(e.launch);
        }
    }

    private void toggleFavApp(AppEntry e, int pos) {
        boolean now = Favs.toggle(e.pkg);
        flash(now ? e.label + " added to For You" : e.label + " removed from For You");
        if (tab == TAB_FOR_YOU && !now) {
            rebuild();
            focusIn(pos);
        } else {
            adapter.notifyItemChanged(pos);
        }
    }

    private void startSafe(Intent i) {
        if (i == null) { flash("App not available"); return; }
        if (i.getComponent() != null) Options.markOpened(this, i.getComponent().getPackageName());
        try { startActivity(i); } catch (ActivityNotFoundException | SecurityException e) { flash("Couldn't open that"); }
    }

    private void flash(String msg) {
        if (toast == null) {
            toast = ui.text(this, 30, Color.WHITE, ui.regular);
            toast.setGravity(Gravity.CENTER);
            toast.setPadding(ui.px(36), 0, ui.px(36), 0);
            toast.setBackground(ui.round(0xE61C1C1E, 34));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(68));
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.bottomMargin = ui.px(36);
            root.addView(toast, lp);
        }
        toast.setText(msg);
        toast.setAlpha(1f);
        toast.setVisibility(View.VISIBLE);
        toast.animate().cancel();
        toast.animate().alpha(0f).setStartDelay(1500).setDuration(400).start();
    }

    // ---------------------------------------------------------------- overlays

    private void showOverlay(int kind, View content, float w, float h) {
        overlay.removeAllViews();
        Ui.unclip(content);
        overlay.addView(content, new FrameLayout.LayoutParams(ui.px(w), ui.px(h), Gravity.CENTER));
        overlay.setVisibility(View.VISIBLE);
        overlayKind = kind;
        layer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    private Runnable onOverlayClosed;

    private void hideOverlay(boolean restoreFocus) {
        if (overlayKind == OV_NONE) return;
        Runnable after = onOverlayClosed;
        onOverlayClosed = null;
        overlay.setVisibility(View.GONE);
        overlay.removeAllViews();
        overlayKind = OV_NONE;
        Keymap.learning = false;
        Keymap.learnListener = null;
        layer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        if (after != null) after.run();
        if (restoreFocus) {
            if (returnFocus != null && returnFocus.isShown()) returnFocus.requestFocus();
            else focusFirstTile();
        }
    }

    // pick which apps appear in For You
    private void openAppPicker() {
        List<String> labels = new ArrayList<>(), ids = new ArrayList<>();
        for (AppEntry e : apps) { labels.add(e.label); ids.add(e.pkg); }
        openChecklist("Add apps to For You", labels, ids, Favs::has, Favs::toggle, false, () -> { rebuild(); focusFirstTile(); });
    }

    private interface Test { boolean on(String id); }
    private interface Click { void click(String id); }

    /** A two-column list of apps. Each press calls click; with closeOnClick the first press also closes the list (a single choice). */
    private void openChecklist(String heading, List<String> labels, List<String> ids, Test on, Click click, boolean closeOnClick, Runnable closed) {
        GlassPanel p = new GlassPanel(this, ui, bg, 48);
        TextView t = ui.text(this, 26, 0xB3FFFFFF, ui.regular);
        t.setText(heading.toUpperCase(Locale.ROOT));
        p.addView(t, Widgets.at(ui, 56, 36, 800, 34));
        TextView hint = ui.text(this, 22, 0x99FFFFFF, ui.light);
        hint.setText(closeOnClick ? "OK  choose        Back  cancel" : "OK  add or remove        Back  done");
        hint.setGravity(Gravity.END);
        p.addView(hint, Widgets.at(ui, 700, 40, 560, 30));
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        list.setClipToPadding(false);
        list.setClipChildren(false);
        list.setPadding(ui.px(36), ui.px(14), ui.px(36), ui.px(36));
        list.setItemAnimator(null);
        final int cellW = (1300 - 72) / 2;
        list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                FrameLayout row = new FrameLayout(MainActivity.this);
                row.setLayoutParams(new RecyclerView.LayoutParams(ui.px(cellW), ui.px(88)));
                row.setFocusable(true);
                row.setClickable(true);
                row.setPadding(ui.px(8), ui.px(8), ui.px(8), ui.px(8));
                FrameLayout inner = new FrameLayout(MainActivity.this);
                inner.setBackground(ui.round(Theme.soft(), 24));
                row.addView(inner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                TextView name = ui.text(MainActivity.this, 29, Color.WHITE, ui.regular);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(ui.px(cellW - 130), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
                nl.leftMargin = ui.px(26);
                inner.addView(name, nl);
                TextView check = ui.text(MainActivity.this, 34, Color.WHITE, ui.regular);
                inner.addView(check, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
                ((FrameLayout.LayoutParams) check.getLayoutParams()).rightMargin = ui.px(24);
                Ui.unclip(row);
                row.setTag(new TextView[]{name, check});
                row.setOnFocusChangeListener((v, f) -> {
                    inner.setBackground(ui.round(f ? 0xF2FFFFFF : Theme.soft(), 24));
                    name.setTextColor(f ? 0xFF101012 : Color.WHITE);
                    check.setTextColor(f ? 0xFF101012 : Color.WHITE);
                });
                row.setOnClickListener(v -> {
                    int pos = list.getChildAdapterPosition(v);
                    if (pos < 0) return;
                    click.click(ids.get(pos));
                    if (closeOnClick) { hideOverlay(false); return; }
                    list.getAdapter().notifyItemChanged(pos);
                });
                return new RecyclerView.ViewHolder(row) { };
            }

            @Override public void onBindViewHolder(RecyclerView.ViewHolder vh, int pos) {
                TextView[] tv = (TextView[]) vh.itemView.getTag();
                tv[0].setText(labels.get(pos));
                tv[1].setText(on.on(ids.get(pos)) ? "✓" : "");
            }

            @Override public int getItemCount() { return labels.size(); }
        });
        p.addView(list, Widgets.at(ui, 0, 92, 1300, 770));
        onOverlayClosed = closed;
        showOverlay(OV_SHEET, p, 1300, 880);
        list.post(() -> {
            RecyclerView.ViewHolder vh = list.findViewHolderForAdapterPosition(0);
            if (vh != null) vh.itemView.requestFocus();
        });
    }

    // inputs: glass list of HDMI / AV / Live TV
    private void openInputs() {
        collectInputs();
        List<String> labels = new ArrayList<>();
        for (InputItem i : inputItems) labels.add(i.label);
        if (labels.isEmpty()) { flash("No inputs found"); return; }
        openList("Inputs", labels, i -> { hideOverlay(false); startSafe(inputItems.get(i).intent); });
    }

    private interface IndexPick { void on(int i); }

    private void openList(String heading, List<String> labels, IndexPick pick) { openList(heading, labels, pick, 640, 0); }

    private void openList(String heading, List<String> labels, IndexPick pick, int width, int focusIdx) {
        GlassPanel p = new GlassPanel(this, ui, bg, 48);
        TextView t = ui.text(this, 26, 0xB3FFFFFF, ui.regular);
        t.setText(heading.toUpperCase(Locale.ROOT));
        p.addView(t, Widgets.at(ui, 56, 36, 600, 34));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(ui.px(28), ui.px(8), ui.px(28), ui.px(28));
        View first = null;
        for (int i = 0; i < labels.size(); i++) {
            final int idx = i;
            TextView r = ui.text(this, 31, Color.WHITE, ui.regular);
            r.setText(labels.get(i));
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(ui.px(28), 0, ui.px(28), 0);
            r.setFocusable(true);
            r.setClickable(true);
            r.setBackground(ui.round(0x00FFFFFF, 24));
            r.setOnFocusChangeListener((v, f) -> { r.setBackground(ui.round(f ? 0xF2FFFFFF : 0x00FFFFFF, 24)); r.setTextColor(f ? 0xFF101012 : Color.WHITE); });
            r.setOnClickListener(v -> pick.on(idx));
            col.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.px(84)));
            if (i == Math.min(focusIdx, labels.size() - 1)) first = r;
        }
        p.addView(col, Widgets.at(ui, 0, 84, width, labels.size() * 84 + 40));
        showOverlay(OV_SHEET, p, width, 120 + labels.size() * 84);
        if (first != null) first.requestFocus();
    }

    private void collectInputs() {
        inputItems.clear();
        TvInputManager m = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (m == null) return;
        String tuner = null;
        for (TvInputInfo info : m.getTvInputList()) {
            int type = info.getType();
            if (type == TvInputInfo.TYPE_TUNER) {
                String id = info.getId();
                if (isSystemPackage(info.getServiceInfo().packageName) && (tuner == null || id.compareTo(tuner) < 0)) tuner = id;
                continue;
            }
            if (!info.isPassthroughInput()) continue;
            CharSequence l = info.loadCustomLabel(this);
            if (l == null || l.length() == 0) l = info.loadLabel(this);
            String label = String.valueOf(l);
            if (type != TvInputInfo.TYPE_HDMI && !label.toUpperCase(Locale.ROOT).startsWith("AV") && type != TvInputInfo.TYPE_COMPOSITE) continue;
            if (type == TvInputInfo.TYPE_COMPOSITE && !label.toUpperCase(Locale.ROOT).contains("AV")) label = "AV";
            inputItems.add(new InputItem(info.getId(), label, new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), false));
        }
        if (tuner != null) {
            inputItems.add(new InputItem(tuner, "Live TV", new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(tuner))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), true));
        }
        Collections.sort(inputItems, (a, b) -> {
            int ra = rank(a), rb = rank(b);
            return ra != rb ? ra - rb : a.label.compareToIgnoreCase(b.label);
        });
    }

    private static int rank(InputItem i) {
        if (i.live) return 2;
        return i.label.toUpperCase(Locale.ROOT).startsWith("HDMI") ? 0 : 1;
    }

    private boolean isSystemPackage(String pkg) {
        try { return (getPackageManager().getApplicationInfo(pkg, 0).flags & ApplicationInfo.FLAG_SYSTEM) != 0; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    // about
    private String network() {
        try {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
            if (nc == null) return "Offline";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
        } catch (RuntimeException ignored) { }
        return "Offline";
    }

    private static String gb(long bytes) { return String.format(Locale.US, "%.1f GB", bytes / 1073741824.0); }

    private void openAbout() {
        GlassPanel p = new GlassPanel(this, ui, bg, 52);
        TextView t = ui.text(this, 26, 0xB3FFFFFF, ui.regular);
        t.setText("ABOUT THIS TV");
        p.addView(t, Widgets.at(ui, 64, 40, 600, 34));
        TextView model = ui.text(this, 64, Color.WHITE, ui.thin);
        model.setText(Build.MODEL);
        p.addView(model, Widgets.at(ui, 60, 84, 760, 84));

        StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
        String[][] info = {
            {"Manufacturer", Build.MANUFACTURER},
            {"Android", Build.VERSION.RELEASE + "  (API " + Build.VERSION.SDK_INT + ")"},
            {"Security patch", Build.VERSION.SECURITY_PATCH},
            {"Network", network()},
            {"Storage free", gb(sf.getAvailableBytes()) + " of " + gb(sf.getTotalBytes())},
            {"Memory", gb(mi.totalMem)},
        };
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        for (String[] r : info) {
            FrameLayout row = new FrameLayout(this);
            TextView k = ui.text(this, 29, 0xB3FFFFFF, ui.light);
            k.setText(r[0]);
            row.addView(k, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
            TextView v = ui.text(this, 29, Color.WHITE, ui.regular);
            v.setText(r[1]);
            row.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
            col.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.px(58)));
        }
        p.addView(col, Widgets.at(ui, 64, 190, 892, 6 * 58));

        TextView buttons = Widgets.chip(this, ui, "Remote buttons");
        TextView closeApps = Widgets.chip(this, ui, Options.closeApps(this) ? "Close other apps: On" : "Close other apps: Off");
        TextView keep = Widgets.chip(this, ui, "Keep running");
        buttons.setOnClickListener(v -> { returnFocus = bAbout; openRemoteButtons(); });
        closeApps.setOnClickListener(v -> { Options.setCloseApps(this, !Options.closeApps(this)); closeApps.setText(Options.closeApps(this) ? "Close other apps: On" : "Close other apps: Off"); });
        keep.setOnClickListener(v -> { returnFocus = bAbout; openKeepRunning(); });
        LinearLayout actions0 = new LinearLayout(this);
        actions0.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66)));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66));
        l1.leftMargin = ui.px(16);
        actions0.addView(closeApps, l1);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66));
        l2.leftMargin = ui.px(16);
        actions0.addView(keep, l2);
        p.addView(actions0, Widgets.at(ui, 76, 580, 880, 70));
        TextView next = Widgets.chip(this, ui, "Next background");
        TextView close = Widgets.chip(this, ui, "Close");
        TextView mode = Widgets.chip(this, ui, Theme.dark ? "Light mode" : "Dark mode");
        next.setOnClickListener(v -> bg.next());
        mode.setOnClickListener(v -> { Theme.set(this, !Theme.dark); recreate(); });
        close.setOnClickListener(v -> hideOverlay(true));
        LinearLayout actions = new LinearLayout(this);
        actions.addView(next, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66)));
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66));
        ml.leftMargin = ui.px(16);
        actions.addView(mode, ml);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ui.px(66));
        cl.leftMargin = ui.px(16);
        actions.addView(close, cl);
        p.addView(actions, Widgets.at(ui, 76, 664, 880, 70));
        showOverlay(OV_ABOUT, p, 1020, 780);
        buttons.requestFocus();
    }

    // ---------------------------------------------------------------- remote buttons

    private static final String[] ACTION_NAMES = {"the home screen", "the Apps tab"};

    /** The name of a TV input for the button list; the raw id if that input no longer exists. */
    private String inputLabel(String id) {
        collectInputs();
        for (InputItem i : inputItems) if (i.id.equals(id)) return "input " + i.label;
        return "an input that is gone";
    }

    private String labelOf(String pkg) {
        for (AppEntry e : apps) if (e.pkg.equals(pkg)) return e.label;
        return pkg;
    }

    private boolean keyServiceOn() {
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return on != null && on.contains(getPackageName() + "/" + KeyService.class.getName());
    }

    private void openRemoteButtons() {
        List<String> labels = new ArrayList<>();
        List<Keymap.Entry> entries = Keymap.all(this);
        if (!keyServiceOn()) labels.add("Button support is off: run scripts/04-remote-keys.sh");
        int offset = labels.size();
        for (Keymap.Entry e : entries) {
            labels.add("Button " + e.code + "   \u2192   " + (e.action == Keymap.APP ? labelOf(e.pkg) : e.action == Keymap.INPUT ? inputLabel(e.pkg) : ACTION_NAMES[e.action]));
        }
        labels.add("\uFF0B  Add a button");
        final int n = entries.size();
        openList("Remote buttons", labels, i -> {
            if (i < offset) return;
            if (i - offset == n) { startLearning(); return; }
            Keymap.Entry e = entries.get(i - offset);
            List<String> ops = new ArrayList<>();
            ops.add("Remove button " + e.code);
            ops.add("Keep it");
            openList("Button " + e.code, ops, j -> { if (j == 0) Keymap.remove(this, e.code); openRemoteButtons(); });
        }, 900, labels.size() - 1);
    }

    private void startLearning() {
        GlassPanel p = new GlassPanel(this, ui, bg, 48);
        TextView t = ui.text(this, 26, 0xB3FFFFFF, ui.regular);
        t.setText("NEW BUTTON");
        p.addView(t, Widgets.at(ui, 56, 36, 600, 34));
        TextView big = ui.text(this, 52, Color.WHITE, ui.light);
        big.setText("Press the button on your remote");
        big.setGravity(Gravity.CENTER);
        p.addView(big, Widgets.at(ui, 40, 180, 920, 80));
        TextView hint = ui.text(this, 28, 0xB3FFFFFF, ui.regular);
        hint.setText("The Netflix button, the app drawer, the manufacturer's own launcher button, any extra key.\nIf nothing happens, the TV keeps that button for itself.\nBack  cancel");
        hint.setGravity(Gravity.CENTER);
        hint.setSingleLine(false);
        p.addView(hint, Widgets.at(ui, 40, 300, 920, 220));
        Keymap.learnListener = this::learned;
        Keymap.learning = true;
        showOverlay(OV_LEARN, p, 1000, 600);
        layer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    private void learned(int key) {
        if (overlayKind != OV_LEARN) return;
        Keymap.learning = false;
        Keymap.learnListener = null;
        final int code = key;
        List<String> ops = new ArrayList<>();
        ops.add("Open the home screen");
        ops.add("Open the Apps tab");
        ops.add("Open an app\u2026");
        ops.add("Switch to an input (HDMI 1\u2026)");
        openList("Button " + code + " (" + KeyEvent.keyCodeToString(code).replace("KEYCODE_", "") + ")", ops, i -> {
            if (i == Keymap.HOME || i == Keymap.APPS) { Keymap.put(this, code, i, null); openRemoteButtons(); return; }
            if (i == Keymap.INPUT) {
                collectInputs();
                if (inputItems.isEmpty()) { flash("No inputs found"); openRemoteButtons(); return; }
                List<String> names = new ArrayList<>();
                for (InputItem it : inputItems) names.add(it.label);
                openList("Switch to this input with button " + code, names, j -> { Keymap.put(this, code, Keymap.INPUT, inputItems.get(j).id); openRemoteButtons(); });
                return;
            }
            List<String> labels = new ArrayList<>(), ids = new ArrayList<>();
            for (AppEntry e : apps) { labels.add(e.label); ids.add(e.pkg); }
            openChecklist("Open this app with button " + code, labels, ids, id -> false, id -> Keymap.put(this, code, Keymap.APP, id), true, this::openRemoteButtons);
        });
    }

    private void openKeepRunning() {
        List<String> labels = new ArrayList<>(), ids = new ArrayList<>();
        for (AppEntry e : apps) { labels.add(e.label); ids.add(e.pkg); }
        openChecklist("Never close these apps", labels, ids, id -> Options.keep(this).contains(id), id -> Options.toggleKeep(this, id), false, null);
    }

    // search: keyboard + live results
    private void openSearch() {
        kb.setQuery("");
        runSearch("");
        showOverlay(OV_SEARCH, kb.panel, 1400, 990);
        kb.focusFirst();
    }

    private void runSearch(String q) {
        searchItems.clear();
        String ql = q.toLowerCase(Locale.ROOT).trim();
        if (!ql.isEmpty()) {
            for (AppEntry e : apps) if (e.label.toLowerCase(Locale.ROOT).contains(ql)) searchItems.add(e);
        }
        while (searchItems.size() > 24) searchItems.remove(searchItems.size() - 1);
        searchResults.getAdapter().notifyDataSetChanged();
        kb.setStatus(ql.isEmpty() ? "Search your apps" : searchItems.size() + (searchItems.size() == 1 ? " result" : " results"));
    }

    // ---------------------------------------------------------------- clock / receivers

    private final BroadcastReceiver clockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); }
    };
    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    private void registerReceivers() {
        if (receiversOn) return;
        receiversOn = true;
        IntentFilter t = new IntentFilter();
        t.addAction(Intent.ACTION_TIME_TICK);
        t.addAction(Intent.ACTION_TIME_CHANGED);
        t.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(clockReceiver, t);
        IntentFilter p = new IntentFilter();
        p.addAction(Intent.ACTION_PACKAGE_ADDED);
        p.addAction(Intent.ACTION_PACKAGE_REMOVED);
        p.addAction(Intent.ACTION_PACKAGE_CHANGED);
        p.addDataScheme("package");
        registerReceiver(pkgReceiver, p);
        try { cm.registerDefaultNetworkCallback(netCallback); } catch (RuntimeException ignored) { }
        try { Network n = cm.getActiveNetwork(); updateNetwork(n == null ? null : cm.getNetworkCapabilities(n)); } catch (RuntimeException ignored) { }
    }

    private void unregisterReceivers() {
        if (!receiversOn) return;
        receiversOn = false;
        try { unregisterReceiver(clockReceiver); } catch (RuntimeException ignored) { }
        try { unregisterReceiver(pkgReceiver); } catch (RuntimeException ignored) { }
        try { cm.unregisterNetworkCallback(netCallback); } catch (RuntimeException ignored) { }
    }

    private void updateClock() {
        Date now = new Date();
        String time = new SimpleDateFormat("h:mm", Locale.getDefault()).format(now);
        String ampm = new SimpleDateFormat("a", Locale.US).format(now).toUpperCase(Locale.US);
        SpannableStringBuilder sb = new SpannableStringBuilder(time + " " + ampm);
        sb.setSpan(new android.text.style.RelativeSizeSpan(0.5f), time.length() + 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        clock.setText(sb);
        dateView.setText(new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now));
    }

    /** Connection icon: Wi-Fi with its signal, Ethernet, or offline. */
    private void updateNetwork(NetworkCapabilities nc) {
        int mode = WifiView.OFFLINE, level = 0;
        if (nc != null) {
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) mode = WifiView.ETHERNET;
            else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                mode = WifiView.WIFI;
                int rssi = Build.VERSION.SDK_INT >= 29 ? nc.getSignalStrength() : Integer.MIN_VALUE;
                if (rssi == Integer.MIN_VALUE) {
                    try { rssi = ((android.net.wifi.WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getRssi(); }
                    catch (RuntimeException e) { rssi = -60; }
                }
                level = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 5);
            }
        }
        wifi.set(mode, level);
    }

    private final ConnectivityManager.NetworkCallback netCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities nc) { runOnUiThread(() -> updateNetwork(nc)); }
        @Override public void onLost(Network n) { runOnUiThread(() -> updateNetwork(null)); }
    };

    // ---------------------------------------------------------------- adapters

    private ViewOutlineProvider round(float r) {
        return new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r * ui.u); }
        };
    }

    private final class GridAdapter extends RecyclerView.Adapter<GridAdapter.AppVH> {
        List<Object> data;
        GridAdapter(List<Object> data) { this.data = data; }

        final class AppVH extends RecyclerView.ViewHolder {
            final FrameLayout tile; final ImageView icon; final TextView star, name, plus;
            AppVH(FrameLayout cell, FrameLayout tile, ImageView icon, TextView star, TextView name, TextView plus) { super(cell); this.tile = tile; this.icon = icon; this.star = star; this.name = name; this.plus = plus; }
        }

        /** The ring is a sibling view outside the clipped tile, so it stays visible on white icons. */
        private void focusAnim(FrameLayout tile, View ring, TextView name, boolean f) {
            tile.animate().scaleX(f ? 1.1f : 1f).scaleY(f ? 1.1f : 1f).setDuration(160).start();
            ring.animate().scaleX(f ? 1.1f : 1f).scaleY(f ? 1.1f : 1f).alpha(f ? 1f : 0f).setDuration(160).start();
            name.setTextColor(f ? Color.WHITE : 0xD9FFFFFF);
        }

        /** Size of a tile: For You uses larger tiles than the Apps grid. */
        private float scale(int type) { return type == TAB_FOR_YOU ? 1.4f : 1f; }

        @Override public int getItemViewType(int pos) { return tab; }

        private View makeRing(FrameLayout cell, float s, float r) {
            View ring = new View(MainActivity.this);
            ring.setBackground(ui.ring(0x1AFFFFFF, 0xFFFFFFFF, r + 5, 4f));
            ring.setAlpha(0f);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ui.px(152 * s + 14), ui.px(152 * s + 14));
            lp.leftMargin = ui.px(24 * s - 7);
            lp.topMargin = ui.px(10 - 7);
            cell.addView(ring, 0, lp);
            return ring;
        }

        @Override public AppVH onCreateViewHolder(ViewGroup parent, int type) {
            final float s = scale(type);
            FrameLayout cell = new FrameLayout(MainActivity.this);
            cell.setLayoutParams(new RecyclerView.LayoutParams(ui.px(200 * s), ui.px(246 * s)));
            cell.setClipChildren(false);
            cell.setFocusable(true);
            FrameLayout tile = new FrameLayout(MainActivity.this);
            tile.setBackground(ui.round(Theme.tile(), 36));
            tile.setOutlineProvider(round(36 * s));
            tile.setClipToOutline(true);
            FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(ui.px(152 * s), ui.px(152 * s));
            tl.leftMargin = ui.px(24 * s);
            tl.topMargin = ui.px(10);
            cell.addView(tile, tl);
            View ring = makeRing(cell, s, 36 * s);
            ImageView icon = new ImageView(MainActivity.this);
            tile.addView(icon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView plus = ui.text(MainActivity.this, 76 * s, 0xE6FFFFFF, ui.thin);
            plus.setText("＋");
            plus.setGravity(Gravity.CENTER);
            plus.setVisibility(View.GONE);
            tile.addView(plus, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView star = ui.text(MainActivity.this, 28, 0xFFFFFFFF, ui.regular);
            star.setText("★");
            star.setShadowLayer(ui.px(5), 0, ui.px(1), 0xE6000000);   // dark edge: the star stays visible on white icons
            FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            sl.setMargins(0, ui.px(6), ui.px(10), 0);
            tile.addView(star, sl);
            TextView name = ui.text(MainActivity.this, 24 + 4 * (s - 1), 0xD9FFFFFF, ui.regular);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(ui.px(196 * s), ui.px(32 + 8 * (s - 1)));
            nl.topMargin = ui.px(10 + 152 * s + 20);
            cell.addView(name, nl);
            cell.setOnFocusChangeListener((v, f) -> focusAnim(tile, ring, name, f));
            Ui.unclip(cell);
            return new AppVH(cell, tile, icon, star, name, plus);
        }

        @Override public void onBindViewHolder(AppVH h, int pos) {
            Object o = data.get(pos);
            boolean f = h.itemView.hasFocus();
            h.tile.setScaleX(f ? 1.1f : 1f);
            h.tile.setScaleY(f ? 1.1f : 1f);
            if (o instanceof AddTile) {
                h.name.setText("Add app");
                h.icon.setImageDrawable(null);
                h.plus.setVisibility(View.VISIBLE);
                h.star.setVisibility(View.GONE);
                h.tile.setBackground(ui.round(Theme.soft(), 36 * scale(tab)));
                return;
            }
            AppEntry e = (AppEntry) o;
            h.plus.setVisibility(View.GONE);
            h.tile.setBackground(ui.round(Theme.tile(), 36 * scale(tab)));
            h.name.setText(e.label);
            Drawable d = e.icon != null ? e.icon : e.banner;
            float aspect = d == null || d.getIntrinsicHeight() <= 0 ? 1f : d.getIntrinsicWidth() / (float) d.getIntrinsicHeight();
            boolean square = Math.abs(aspect - 1f) < 0.15f;
            h.icon.setScaleType(square ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            h.icon.setPadding(square ? 0 : ui.px(14), square ? 0 : ui.px(14), square ? 0 : ui.px(14), square ? 0 : ui.px(14));
            h.icon.setImageDrawable(d);
            h.star.setVisibility(tab == TAB_APPS && Favs.has(e.pkg) ? View.VISIBLE : View.GONE);   // star = shown in For You
        }

        @Override public void onViewAttachedToWindow(AppVH h) {
            if (pendingFocus >= 0 && h.getBindingAdapterPosition() == pendingFocus) {
                pendingFocus = -1;
                h.itemView.post(() -> h.itemView.requestFocus());
            }
        }

        @Override public int getItemCount() { return data.size(); }
    }

    private final class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {
        final class VH extends RecyclerView.ViewHolder {
            final FrameLayout inner; final ImageView icon; final TextView name;
            VH(FrameLayout row, FrameLayout inner, ImageView icon, TextView name) { super(row); this.inner = inner; this.icon = icon; this.name = name; }
        }

        @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
            FrameLayout row = new FrameLayout(MainActivity.this);
            row.setLayoutParams(new RecyclerView.LayoutParams(ui.px(200), ui.px(220)));
            row.setFocusable(true);
            row.setClickable(true);
            row.setPadding(ui.px(8), ui.px(8), ui.px(8), ui.px(8));
            FrameLayout inner = new FrameLayout(MainActivity.this);
            inner.setBackground(ui.round(Theme.soft(), 26));
            row.addView(inner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ImageView icon = new ImageView(MainActivity.this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(ui.px(120), ui.px(120), Gravity.CENTER_HORIZONTAL);
            il.topMargin = ui.px(18);
            inner.addView(icon, il);
            TextView name = ui.text(MainActivity.this, 22, Color.WHITE, ui.regular);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(ui.px(176), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            nl.bottomMargin = ui.px(20);
            inner.addView(name, nl);
            VH h = new VH(row, inner, icon, name);
            row.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p < 0) return;
                hideOverlay(false);
                startSafe(searchItems.get(p).launch);
            });
            row.setOnFocusChangeListener((v, f) -> {
                inner.setBackground(ui.round(f ? 0xF2FFFFFF : Theme.soft(), 26));
                name.setTextColor(f ? 0xFF101012 : Color.WHITE);
            });
            return h;
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            AppEntry e = searchItems.get(pos);
            h.name.setText(e.label);
            h.icon.setImageDrawable(e.icon != null ? e.icon : e.banner);
        }

        @Override public int getItemCount() { return searchItems.size(); }
    }
}
