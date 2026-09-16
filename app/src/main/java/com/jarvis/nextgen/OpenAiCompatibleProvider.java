package com.jarvis.nextgen;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Generic HTTPS provider for APIs implementing the OpenAI chat-completions contract. */
public final class OpenAiCompatibleProvider implements LlmProvider {
    private static final String ENDPOINT = "llm.endpoint";
    private static final String KEY = "llm.api_key";
    private static final String MODEL = "llm.model";
    private final SecureStore secure;

    public OpenAiCompatibleProvider(Context context) { secure = new SecureStore(context); }
    @Override public String id() { return "openai_compatible"; }
    @Override public String displayName() { return "OpenAI-compatible HTTPS"; }
    @Override public boolean configured() { return validEndpoint(secure.get(ENDPOINT)) && nonBlank(secure.get(KEY)) && nonBlank(secure.get(MODEL)); }

    public void save(String endpoint, String apiKey, String model) {
        if (!validEndpoint(endpoint)) throw new IllegalArgumentException("Endpoint должен использовать HTTPS.");
        if (!nonBlank(apiKey)) throw new IllegalArgumentException("API key не может быть пустым.");
        if (!nonBlank(model)) throw new IllegalArgumentException("Model не может быть пустым.");
        secure.put(ENDPOINT, endpoint.trim());
        secure.put(KEY, apiKey.trim());
        secure.put(MODEL, model.trim());
    }

    public String endpoint() { return secure.get(ENDPOINT); }
    public String model() { return secure.get(MODEL); }

    @Override public void clearCredentials() { secure.remove(ENDPOINT); secure.remove(KEY); secure.remove(MODEL); }

    @Override public void complete(String system, String user, CompletionCallback callback) {
        new Thread(() -> {
            HttpURLConnection h = null;
            try {
                String endpoint = secure.get(ENDPOINT), key = secure.get(KEY), model = secure.get(MODEL);
                if (!validEndpoint(endpoint) || !nonBlank(key) || !nonBlank(model)) { callback.failure("Онлайн AI не настроен."); return; }
                h = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
                h.setRequestMethod("POST"); h.setConnectTimeout(12000); h.setReadTimeout(45000); h.setDoOutput(true);
                h.setRequestProperty("Authorization", "Bearer " + key); h.setRequestProperty("Content-Type", "application/json");
                JSONArray messages = new JSONArray();
                if (nonBlank(system)) messages.put(new JSONObject().put("role", "system").put("content", system));
                messages.put(new JSONObject().put("role", "user").put("content", user));
                JSONObject body = new JSONObject().put("model", model).put("messages", messages);
                try (OutputStream os = h.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                int code = h.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? h.getInputStream() : h.getErrorStream();
                String raw = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (code < 200 || code >= 300) { callback.failure("AI сервер вернул HTTP " + code + "."); return; }
                JSONObject out = new JSONObject(raw);
                JSONArray choices = out.optJSONArray("choices");
                if (choices == null || choices.length() == 0) { callback.failure("AI не вернул результат."); return; }
                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                String answer = message == null ? "" : message.optString("content", "").trim();
                if (answer.isBlank()) { callback.failure("AI вернул пустой ответ."); return; }
                callback.success(answer);
            } catch (Exception e) {
                callback.failure("Онлайн AI недоступен: " + safe(e.getMessage()));
            } finally { if (h != null) h.disconnect(); }
        }, "jarvis-llm").start();
    }

    private static boolean nonBlank(String s) { return s != null && !s.isBlank(); }
    private static boolean validEndpoint(String s) {
        if (!nonBlank(s)) return false;
        try { URI u = URI.create(s.trim()); return "https".equalsIgnoreCase(u.getScheme()) && u.getHost() != null; }
        catch (Exception e) { return false; }
    }
    private static String safe(String s) { if (s == null || s.isBlank()) return "ошибка сети"; return s.replaceAll("(?i)Bearer\\s+\\S+", "Bearer [REDACTED]"); }
}
