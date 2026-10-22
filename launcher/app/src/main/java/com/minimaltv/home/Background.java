package com.minimaltv.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Slowly drifting photo behind the whole screen. One animator drives the sharp layer and the pre-blurred
 * copies that live inside the glass panels, so the frost always lines up with the picture behind it.
 * Photos are bundled (assets/bg/bgN.jpg + bgN_blur.jpg). Light on RAM: RGB_565, current + next only.
 */
final class Background {
    static final int COUNT = 6;
    private static final long DRIFT_MS = 60_000, SWITCH_MS = 100_000, FADE_MS = 2500;

    private final Context ctx;
    private final Ui ui;
    private final android.content.SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ImageView sharp;
    private final List<ImageView> blurs = new ArrayList<>();
    private ValueAnimator drift;
    private int index;
    private Bitmap curBlur;
    private int frame;
    private boolean running;

    Background(Context c, Ui ui, ViewGroup root) {
        this.ctx = c;
        this.ui = ui;
        prefs = c.getSharedPreferences("bg", Context.MODE_PRIVATE);
        index = prefs.getInt("bg", 0) % COUNT;
        sharp = new ImageView(c);
        sharp.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(sharp, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrim = new View(c);
        scrim.setBackgroundColor(Theme.scrim());   // keeps white text readable on the lighter photos
        root.addView(scrim, 1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.BLACK);
        io.execute(() -> {
            Bitmap s = decode("bg/bg" + (index + 1) + ".jpg"), b = decode("bg/bg" + (index + 1) + "_blur.jpg");
            main.post(() -> {
                curBlur = b;
                sharp.setImageBitmap(s);
                for (ImageView v : blurs) v.setImageDrawable(new BitmapDrawable(ctx.getResources(), curBlur));
            });
        });
    }

    private Bitmap decode(String asset) {
        try (InputStream in = ctx.getAssets().open(asset)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in, null, o);
        } catch (Exception e) {
            return null;
        }
    }

    /** A glass panel registers its (full-screen sized) frost view so it drifts in step with the photo. */
    void register(ImageView blur) {
        if (!blurs.contains(blur)) blurs.add(blur);
        if (curBlur != null) blur.setImageDrawable(new BitmapDrawable(ctx.getResources(), curBlur));
        applyDrift(blur, current);
    }

    void unregister(ImageView blur) { blurs.remove(blur); }

    void start() {
        if (running) return;
        running = true;
        drift = ValueAnimator.ofFloat(0f, 1f);
        drift.setDuration(DRIFT_MS);
        drift.setRepeatMode(ValueAnimator.REVERSE);
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setInterpolator(new LinearInterpolator());
        drift.addUpdateListener(a -> {
            if ((frame++ & 1) == 1) return;     // ~30 fps is plenty for a slow drift and halves GPU work
            float t = (Float) a.getAnimatedValue();
            current = t * t * (3 - 2 * t);      // smoothstep so it eases at both ends
            applyDrift(sharp, current);
            for (ImageView v : blurs) applyDrift(v, current);
        });
        drift.start();
        main.postDelayed(autoNext, SWITCH_MS);
    }

    void stop() {
        running = false;
        if (drift != null) drift.cancel();
        main.removeCallbacks(autoNext);
    }

    private float current;

    private void applyDrift(View v, float t) {
        float s = 1.08f + 0.05f * t;
        v.setScaleX(s);
        v.setScaleY(s);
        v.setTranslationX((0.5f - t) * 60 * ui.u);
        v.setTranslationY((t - 0.5f) * 34 * ui.u);
    }

    private final Runnable autoNext = new Runnable() {
        @Override public void run() {
            next();
            main.postDelayed(this, SWITCH_MS);
        }
    };

    /** Cross-fades to the next photo (also used by the "Next background" action). */
    void next() {
        final int n = (index + 1) % COUNT;
        io.execute(() -> {
            Bitmap s = decode("bg/bg" + (n + 1) + ".jpg"), b = decode("bg/bg" + (n + 1) + "_blur.jpg");
            if (s == null || b == null) return;
            main.post(() -> {
                index = n;
                prefs.edit().putInt("bg", n).apply();
                curBlur = b;
                fade(sharp, new BitmapDrawable(ctx.getResources(), s));
                for (ImageView v : blurs) fade(v, new BitmapDrawable(ctx.getResources(), b));
            });
        });
    }

    private void fade(ImageView v, Drawable to) {
        Drawable from = v.getDrawable();
        if (from == null) { v.setImageDrawable(to); return; }
        if (from instanceof TransitionDrawable) from = ((TransitionDrawable) from).getDrawable(1);
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{from, to});
        td.setCrossFadeEnabled(true);
        v.setImageDrawable(td);
        td.startTransition((int) FADE_MS);
    }
}
