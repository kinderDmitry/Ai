package com.jarvis.nextgen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import java.util.Locale;

/** Read-only contact intelligence: lookup is real and ambiguity is surfaced, never guessed. */
public final class ContactIntelligenceTool implements Tool {
    private final Context context;
    public ContactIntelligenceTool(Context context) { this.context = context.getApplicationContext(); }
    @Override public String name() { return "contact_intelligence"; }
    @Override public String description() { return "Resolve real Android contacts and expose exact/ambiguous matches without modifying contacts."; }
    @Override public boolean canHandle(String input) {
        String x = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return x.contains("найди контакт") || x.contains("найди номер") || x.contains("контакт") && (x.contains("кто") || x.contains("номер"));
    }
    @Override public Models.ToolResult execute(String input) {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужен доступ к контактам.");
        String query = input.replaceFirst("(?is).*?(найди контакт|найди номер|контакт|номер)", "").trim();
        if (query.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Какой контакт найти?");
        ContactResolver.Result result = new ContactResolver(context).resolve(query, 8);
        if (result.hasError()) return Models.ToolResult.fail(Models.ResultCode.FAILED, result.error());
        if (result.matches().isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Контакт «" + query + "» не найден.");
        if (result.matches().size() > 1) {
            StringBuilder out = new StringBuilder("Нашёл несколько совпадений:\n");
            for (ContactResolver.Match m : result.matches()) out.append("• ").append(m.name()).append(" — ").append(m.rawNumber()).append('\n');
            return Models.ToolResult.fail(Models.ResultCode.FAILED, out.toString().trim() + "\nУточните контакт.");
        }
        ContactResolver.Match m = result.matches().get(0);
        return Models.ToolResult.ok("Контакт найден: " + m.name() + " — " + m.rawNumber());
    }
}
