package com.minimaltv.home;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes a remote's dedicated buttons (Netflix, app drawer, the manufacturer's own launcher button...) work in any app.
 * Android has no global handler for them once the manufacturer's launcher is disabled, and no setting to remap them.
 * The buttons are assigned on the TV in MinimalTV > About > Remote buttons. Only key events and the package name of
 * the window that came to the front are read; no screen content.
 *
 * It also closes the apps you opened from the launcher when you open another one (see Options).
 */
public class KeyService extends AccessibilityService {
    static final String EXTRA_OPEN_APPS = "open_apps";
    private static final long[] CLOSE_DELAYS_MS = {1500, 4000, 9000};   // let the new app come up first

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Intent> launchCache = new HashMap<>();
    private String front;   // app opened from the launcher that is currently in front

    @Override protected boolean onKeyEvent(KeyEvent ev) {
        if (Keymap.learning) {                                   // the "press a button" screen is open: report the key, do not act on it
            if (!Keymap.assignable(ev.getKeyCode())) return false;
            final Keymap.Learn l = Keymap.learnListener;
            if (ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 0 && l != null) {
                final int code = ev.getKeyCode();
                main.post(() -> l.onKey(code));
            }
            return true;
        }
        Keymap.Entry e = Keymap.find(this, ev.getKeyCode());
        if (e == null) return false;
        if (ev.getAction() != KeyEvent.ACTION_DOWN || ev.getRepeatCount() > 0) return true;   // act once, on press, so there is no wait for the release
        switch (e.action) {
            case Keymap.HOME: performGlobalAction(GLOBAL_ACTION_HOME); break;
            case Keymap.APPS: startActivity(homeIntent().putExtra(EXTRA_OPEN_APPS, true)); break;
            case Keymap.INPUT: openInput(e.pkg); break;
            default: openApp(e.pkg);
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        String p = e.getPackageName().toString();
        if (p.equals(front) || !Options.opened(this).contains(p) || !Options.closeApps(this)) return;
        front = p;
        main.removeCallbacksAndMessages(null);
        // retry: while the old app is still fading out the system treats it as visible and refuses to kill it
        for (long d : CLOSE_DELAYS_MS) main.postDelayed(this::closeOthers, d);
    }

    private void closeOthers() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null || front == null) return;
        for (String p : Options.opened(this)) {
            if (p.equals(front) || Options.keep(this).contains(p)) continue;
            am.killBackgroundProcesses(p);
        }
    }

    private Intent homeIntent() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setPackage(getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    /** Switches to a TV input (HDMI 1, AV, Live TV), the same way the Inputs list on the home screen does. */
    private void openInput(String inputId) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) { }   // the input is gone (renamed or removed): do nothing
    }

    private void openApp(String pkg) {
        Intent i = launchCache.get(pkg);
        if (i == null) {
            i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) return;
            // same shape as a launcher tile, and placed on top of Home so Back returns to the home screen
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            launchCache.put(pkg, i);
        }
        Options.markOpened(this, pkg);
        startActivity(i);
    }

    @Override public void onInterrupt() { }
}
