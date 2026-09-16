package com.jarvis.nextgen;

import android.content.Context;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves real Android contacts without guessing when the result is ambiguous. */
public final class ContactResolver {
    private final Context context;
    public ContactResolver(Context context) { this.context = context.getApplicationContext(); }

    public Result resolve(String query, int limit) {
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) return Result.empty();
        Map<String, Match> unique = new LinkedHashMap<>();
        try (android.database.Cursor cur = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + q + "%", "%" + q + "%"},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cur == null) return Result.empty();
            while (cur.moveToNext() && unique.size() < Math.max(1, limit)) {
                long id = cur.getLong(0);
                String name = cur.getString(1);
                String raw = cur.getString(2);
                int type = cur.getInt(3);
                String normalized = PhoneNumberUtils.normalizeNumber(raw == null ? "" : raw);
                if (name == null || normalized == null || normalized.isBlank()) continue;
                String key = id + ":" + normalized;
                unique.putIfAbsent(key, new Match(id, name, raw, normalized, type));
            }
        } catch (Exception e) {
            return Result.error("Не удалось прочитать контакты.");
        }
        List<Match> matches = new ArrayList<>(unique.values());
        matches.sort((a, b) -> Integer.compare(score(b, q), score(a, q)));
        return new Result(matches, "");
    }

    private int score(Match m, String q) {
        String name = m.name().toLowerCase(Locale.ROOT);
        String needle = q.toLowerCase(Locale.ROOT);
        if (name.equals(needle)) return 100;
        if (name.startsWith(needle)) return 80;
        if (name.contains(needle)) return 60;
        return m.normalizedNumber().contains(PhoneNumberUtils.normalizeNumber(q)) ? 50 : 0;
    }

    public record Match(long contactId, String name, String rawNumber, String normalizedNumber, int numberType) {}
    public record Result(List<Match> matches, String error) {
        static Result empty() { return new Result(List.of(), ""); }
        static Result error(String message) { return new Result(List.of(), message); }
        public boolean hasError() { return !error.isBlank(); }
    }
}
