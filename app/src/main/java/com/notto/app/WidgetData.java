package com.notto.app;

import android.content.Context;

/** Small summary of the user's notes, pushed from the web layer for the widgets. */
final class WidgetData {

    private WidgetData() {
    }

    static void set(Context c, String json) {
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().putString("widget", json).apply();
    }

    static String get(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, 0).getString("widget", "");
    }
}
