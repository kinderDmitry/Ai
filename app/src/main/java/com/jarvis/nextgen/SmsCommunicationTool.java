package com.jarvis.nextgen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Contact-aware SMS hand-off. It never claims delivery because ACTION_SENDTO does not provide it. */
public final class SmsCommunicationTool implements Tool {
    private final Context c;
    public SmsCommunicationTool(Context context) { c = context.getApplicationContext(); }

    @Override public String name() { return "sms_communication"; }
    @Override public String description() { return "Resolve a unique Android contact and prepare an SMS through the device SMS application."; }
    @Override public boolean canHandle(String input) {
        String x = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return (x.contains("смс") || x.contains("sms") || x.contains("напиши") || x.contains("отправь сообщение"))
                && !x.contains("telegram") && !x.contains("телеграм");
    }

    @Override public Models.ToolResult execute(String input) {
        CommunicationRequest req = CommunicationRequest.parse(input);
        String number = extractNumber(input);
        String target = req.target;
        String message = req.message;

        if (number.isBlank() && target.isBlank())
            return Models.ToolResult.fail(Models.ResultCode.FAILED, "Кому отправить SMS?");
        if (message.isBlank())
            return Models.ToolResult.fail(Models.ResultCode.FAILED, "Какой текст отправить?");

        if (number.isBlank()) {
            if (c.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                return Models.ToolResult.fail(Models.ResultCode.PERMISSION_REQUIRED, "Нужен доступ к контактам, чтобы точно определить получателя.");
            List<Contact> matches = findContacts(target);
            if (matches.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Контакт «" + target + "» не найден.");
            if (matches.size() > 1) {
                StringBuilder b = new StringBuilder("Нашёл несколько контактов: ");
                for (int i = 0; i < matches.size(); i++) {
                    if (i > 0) b.append(", ");
                    b.append(matches.get(i).name);
                }
                return Models.ToolResult.fail(Models.ResultCode.FAILED, b + ". Уточните, кому отправить SMS.");
            }
            number = matches.get(0).number;
            target = matches.get(0).name;
        }

        final String finalNumber = PhoneNumberUtils.normalizeNumber(number);
        final String finalTarget = target.isBlank() ? finalNumber : target;
        final String finalMessage = message;
        if (finalNumber == null || finalNumber.isBlank())
            return Models.ToolResult.fail(Models.ResultCode.FAILED, "У получателя нет корректного номера.");

        return Models.ToolResult.confirm(
                "Подготовить SMS для «" + finalTarget + ": «" + finalMessage + "»?",
                () -> openComposer(finalNumber, finalMessage));
    }

    private void openComposer(String number, String text) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)));
        i.putExtra("sms_body", text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { c.startActivity(i); }
        catch (Exception e) { throw new IllegalStateException("На устройстве нет SMS-приложения, принимающего это действие."); }
    }

    private String extractNumber(String input) {
        Matcher m = Pattern.compile("(?:\\+?\\d[\\d ()-]{6,})").matcher(input == null ? "" : input);
        return m.find() ? PhoneNumberUtils.normalizeNumber(m.group()) : "";
    }

    private List<Contact> findContacts(String query) {
        List<Contact> out = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        try (android.database.Cursor cur = c.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + q + "%"},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cur != null) {
                while (cur.moveToNext() && out.size() < 6) {
                    String name = cur.getString(0);
                    String number = cur.getString(1);
                    if (name != null && number != null && !number.isBlank()) out.add(new Contact(name, number));
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private record Contact(String name, String number) {}
}
