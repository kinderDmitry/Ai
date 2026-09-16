package com.jarvis.nextgen;

import android.util.Log;

/** Production-safe diagnostic logging. Never accepts or prints user message content. */
public final class SecureLogger {
    private static final String TAG = "JARVIS";
    private SecureLogger() {}
    public static void info(String event) { Log.i(TAG, sanitize(event)); }
    public static void warn(String event) { Log.w(TAG, sanitize(event)); }
    public static void error(String event, Throwable error) {
        Log.e(TAG, sanitize(event), error == null ? new RuntimeException("no-cause") : error);
    }
    private static String sanitize(String value) {
        if (value == null) return "null";
        String s = value.replaceAll("[\\r\\n\\t]", " ");
        return s.length() > 160 ? s.substring(0, 160) : s;
    }
}
