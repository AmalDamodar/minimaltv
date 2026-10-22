package com.minimaltv.home;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

final class AppEntry {
    final String pkg;
    final String label;
    final Intent launch;
    final Drawable banner;   // null when the app ships no banner
    final Drawable icon;

    AppEntry(String pkg, String label, ComponentName cn, Drawable banner, Drawable icon) {
        this.pkg = pkg;
        this.label = label;
        this.banner = banner;
        this.icon = icon;
        this.launch = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
}
