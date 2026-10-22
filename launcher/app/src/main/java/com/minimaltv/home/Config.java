package com.minimaltv.home;

/**
 * Starting point only. Favourites, remote buttons and "close other apps" are changed on the TV
 * (hold OK on an app; About > Remote buttons). Edit this file and rebuild to change the defaults.
 * Package names: adb shell pm list packages | grep -i <name>
 */
final class Config {
    private Config() {}

    /**
     * Apps put in "For You" on first run. Apps that are not installed are ignored, so this list can be long:
     * it is a set of common streaming apps, not a promise that your TV has them.
     */
    static final String[] DOCK = {
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "in.startv.hotstar",
        "com.apple.atve.androidtv.appletv",
        "com.spotify.tv.android",
        "com.plexapp.android",
    };

    /** Packages never shown in the grid. */
    static final String[] HIDDEN = {
        "com.minimaltv.home",
    };
}
