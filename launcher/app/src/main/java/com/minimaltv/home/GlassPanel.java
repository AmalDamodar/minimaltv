package com.minimaltv.home;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Frosted glass: the pre-blurred photo (aligned with the real background), a translucent white fill with a soft
 * diagonal sheen, and a thin light edge. With no Background (e.g. over video) it is a plain dark translucent glass.
 */
final class GlassPanel extends FrameLayout {
    private final Background bg;
    private final ImageView frost;
    private final int[] loc = new int[2];

    GlassPanel(Context c, Ui ui, Background bg, float radius) {
        super(c);
        this.bg = bg;
        final float r = radius * ui.u;
        setClipChildren(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); }
        });
        if (bg != null) {
            frost = new ImageView(c);
            frost.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int sw = c.getResources().getDisplayMetrics().widthPixels, sh = c.getResources().getDisplayMetrics().heightPixels;
            addView(frost, new FrameLayout.LayoutParams(sw, sh));
        } else {
            frost = null;
        }
        // tint sits UNDER the content (a foreground drawable would paint over, and dim, the children)
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                Theme.glass(bg != null));
        fill.setCornerRadius(r);
        View tint = new View(c);
        tint.setBackground(fill);
        addView(tint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // only the thin edge is a foreground, and it has no fill
        GradientDrawable edge = new GradientDrawable();
        edge.setColor(0);
        edge.setCornerRadius(r);
        edge.setStroke(Math.max(2, Math.round(1.6f * ui.u)), Theme.glassEdge(bg != null));
        setForeground(edge);
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (frost == null) return;
        getLocationOnScreen(loc);
        // keep the frost view pinned to the screen origin, whatever the panel's position (re-applied on every layout)
        frost.layout(-loc[0], -loc[1], -loc[0] + frost.getLayoutParams().width, -loc[1] + frost.getLayoutParams().height);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bg != null && frost != null) bg.unregister(frost);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (bg != null && frost != null) bg.register(frost);
    }
}
