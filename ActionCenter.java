package com.jarvis.nextgen;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Persistent, bounded task history. An item is updated in-place so ACTIVE never remains stale. */
public final class ActionCenter {
    public enum Status { ACTIVE, SCHEDULED, COMPLETED, FAILED, CANCELLED }
    public record Item(long id, String title, Status status) {}
    private static final int MAX_ITEMS = 60;
    private final SharedPreferences prefs;

    public ActionCenter(Context context) { prefs = context.getSharedPreferences("actions", Context.MODE_PRIVATE); }
    private JSONArray load() { try { return new JSONArray(prefs.getString("items", "[]")); } catch (Exception e) { return new JSONArray(); } }

    public synchronized long start(String title) {
        long id = System.currentTimeMillis();
        JSONArray a = load();
        try { JSONObject o = new JSONObject(); o.put("id", id); o.put("title", title); o.put("status", Status.ACTIVE.name()); a.put(o); save(a); }
        catch (Exception ignored) {}
        return id;
    }

    public synchronized void finish(long id, String title, boolean ok) { update(id, ok ? Status.COMPLETED : Status.FAILED, title); }
    public synchronized void cancel(long id, String title) { update(id, Status.CANCELLED, title); }

    private void update(long id, Status status, String fallbackTitle) {
        JSONArray a = load(); boolean found = false;
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                if (o.optLong("id") == id) { o.put("status", status.name()); found = true; break; }
            } catch (Exception ignored) {}
        }
        if (!found) {
            try { JSONObject o = new JSONObject(); o.put("id", id); o.put("title", fallbackTitle); o.put("status", status.name()); a.put(o); } catch (Exception ignored) {}
        }
        save(a);
    }

    private void save(JSONArray a) {
        JSONArray out = new JSONArray();
        int start = Math.max(0, a.length() - MAX_ITEMS);
        for (int i = start; i < a.length(); i++) out.put(a.opt(i));
        prefs.edit().putString("items", out.toString()).apply();
    }

    public synchronized List<Item> recent() {
        List<Item> result = new ArrayList<>(); JSONArray a = load();
        for (int i = a.length() - 1; i >= 0; i--) try {
            JSONObject o = a.getJSONObject(i);
            result.add(new Item(o.getLong("id"), o.getString("title"), Status.valueOf(o.getString("status"))));
        } catch (Exception ignored) {}
        return result;
    }
}
