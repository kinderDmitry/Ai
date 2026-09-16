package com.jarvis.nextgen;

import android.content.Context;
import java.util.Locale;

/** Small persistent conversation context. It deliberately stores only explicit, useful context. */
public final class ContextEngine {
    private final android.content.SharedPreferences prefs;
    private String lastLocation;
    private String lastTopic;

    public ContextEngine(Context context) {
        prefs = context.getSharedPreferences("jarvis_context", Context.MODE_PRIVATE);
        lastLocation = prefs.getString("last_location", "");
        lastTopic = prefs.getString("last_topic", "");
    }

    public synchronized void observe(String input) {
        String x = input.toLowerCase(Locale.ROOT);
        if (x.contains("москв")) lastLocation = "Москва";
        if (x.contains("берлин")) lastLocation = "Берлин";
        if (x.contains("лондон")) lastLocation = "Лондон";
        if (!input.isBlank()) lastTopic = input.trim();
        prefs.edit().putString("last_location", lastLocation).putString("last_topic", lastTopic).apply();
    }

    public synchronized String enrich(String input) {
        String x = input.trim();
        if ((x.equalsIgnoreCase("а завтра?") || x.equalsIgnoreCase("завтра?")) && !lastLocation.isBlank()) {
            return "погода завтра в " + lastLocation;
        }
        return x;
    }

    public synchronized String location() { return lastLocation; }
    public synchronized String lastTopic() { return lastTopic; }
    public synchronized void clear() { lastLocation = ""; lastTopic = ""; prefs.edit().clear().apply(); }
}
