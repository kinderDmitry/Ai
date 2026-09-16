package com.jarvis.nextgen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Real Telegram hand-off using Android intents/deep links. It deliberately never claims
 * that a message was sent because Telegram's public Android hand-off does not provide
 * a reliable delivery acknowledgement to a third-party app.
 */
public final class TelegramCommunicationTool implements Tool {
    private final Context c;
    public TelegramCommunicationTool(Context context) { this.c = context.getApplicationContext(); }
    @Override public String name() { return "telegram_communication"; }
    @Override public String description() { return "Resolve a real contact and open Telegram with a prepared message; sending remains inside Telegram unless an official API is configured."; }
    @Override public boolean canHandle(String input) {
        String x = input == null ? "" : input.toLowerCase(java.util.Locale.ROOT);
        return (x.contains("telegram") || x.contains("телеграм")) &&
                (x.contains("напиши") || x.contains("отправь") || x.contains("сообщ") || x.contains("send"));
    }
    @Override public Models.ToolResult execute(String input) {
        if (c.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужен доступ к контактам, чтобы точно определить получателя Telegram.");
        }
        CommunicationRequest req = CommunicationRequest.parse(input);
        if (req.target.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Не удалось определить получателя Telegram.");
        if (req.message.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Какой текст подготовить для Telegram?");

        String number = findUniqueNumber(req.target);
        if (number == null) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Контакт «" + req.target + "» не найден или найден неоднозначно.");
        final String normalized = PhoneNumberUtils.normalizeNumber(number);
        final String target = req.target;
        final String text = req.message;
        return Models.ToolResult.confirm("Подготовить сообщение в Telegram для «" + target + ": «" + text + "»?", () -> openTelegram(normalized, text));
    }
    private String findUniqueNumber(String query) {
        List<String> numbers = new ArrayList<>();
        try (android.database.Cursor cur = c.getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER},
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + query + "%"},
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cur != null) while (cur.moveToNext() && numbers.size() < 3) numbers.add(cur.getString(0));
        } catch (Exception e) { return null; }
        return numbers.size() == 1 ? numbers.get(0) : null;
    }
    private void openTelegram(String number, String text) {
        Intent deep = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=" + Uri.encode(number) + "&text=" + Uri.encode(text)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(deep);
            return;
        } catch (Exception ignored) { }
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain").setPackage("org.telegram.messenger")
                .putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { c.startActivity(share); }
        catch (Exception e) { throw new IllegalStateException("Telegram не установлен или не поддерживает доступный Android-механизм."); }
    }
}
