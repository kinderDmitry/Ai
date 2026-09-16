package com.jarvis.nextgen;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parser for natural-language communication requests. No Android dependencies. */
public final class CommunicationRequest {
    public enum Channel { TELEGRAM, SMS, CALL, UNKNOWN }
    public final Channel channel;
    public final String target;
    public final String message;

    private CommunicationRequest(Channel channel, String target, String message) {
        this.channel = channel;
        this.target = target == null ? "" : target.trim();
        this.message = message == null ? "" : message.trim();
    }

    public static CommunicationRequest parse(String input) {
        String s = input == null ? "" : input.trim();
        String x = s.toLowerCase(Locale.ROOT);
        Channel ch;
        if (x.contains("telegram") || x.contains("телеграм")) ch = Channel.TELEGRAM;
        else if (x.contains("смс") || x.contains("sms") || x.startsWith("напиши") || x.startsWith("отправь сообщение")) ch = Channel.SMS;
        else if (x.contains("позвони") || x.contains("набери")) ch = Channel.CALL;
        else ch = Channel.UNKNOWN;

        String body = s.replaceFirst("(?is).*?(telegram|телеграм|смс|sms|напиши|отправь сообщение|позвони|набери)\\s*", "").trim();
        body = body.replaceFirst("(?is)^(?:в|через|с помощью)\\s+(?:telegram|телеграм)\\s*", "").trim();
        body = body.replaceFirst("(?is)^(?:на номер|номер)\\s*(?:\\+?\\d[\\d ()-]{6,})\\s*", "").trim();
        Matcher number = Pattern.compile("(?:\\+?\\d[\\d ()-]{6,})").matcher(body);
        if (ch == Channel.CALL) {
            return new CommunicationRequest(ch, number.find() ? number.group() : body, "");
        }
        int colon = body.indexOf(':');
        if (colon > 0) return new CommunicationRequest(ch, body.substring(0, colon), body.substring(colon + 1));
        Matcher dash = Pattern.compile("(?is)^(.+?)\\s+(?:—|-)\\s+(.+)$").matcher(body);
        if (dash.find()) return new CommunicationRequest(ch, dash.group(1), dash.group(2));
        return new CommunicationRequest(ch, body, "");
    }
}
