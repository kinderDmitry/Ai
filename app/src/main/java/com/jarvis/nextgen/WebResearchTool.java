package com.jarvis.nextgen;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.json.JSONObject;

/** Real web research: search -> retrieve pages -> extract lightweight text -> attribute sources. */
public final class WebResearchTool implements Tool {
    private final Context context;
    private static final int MAX_RESULTS = 6;
    private static final int MAX_PAGE_CHARS = 6000;
    private static final int MAX_SEARCH_BYTES = 700_000;
    private static final int MAX_SOURCES_IN_REPORT = 5;

    public WebResearchTool(Context context) { this.context = context.getApplicationContext(); }
    @Override public String name() { return "web_research"; }
    @Override public String description() { return "Search the live web, retrieve public pages, extract readable text and return attributed sources. Never invent missing information."; }
    @Override public boolean canHandle(String s) {
        String x = s.toLowerCase(Locale.ROOT);
        return x.contains("исследуй в интернете") || x.contains("проведи исследование") ||
               x.contains("найди информацию") || x.contains("найди сведения") ||
               x.contains("изучи в интернете") || x.contains("web research") ||
               x.contains("исследование в интернете") || x.contains("исследуй") ||
               x.contains("research") || x.contains("найди источники") || x.contains("сравни источники");
    }
    @Override public JSONObject schema() {
        return ToolSchemaCatalog.schema(this);
    }
    @Override public Models.ToolResult execute(String input) {
        String query = extractQuery(input);
        if (query.isBlank()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Что именно исследовать в интернете?");
        try {
            List<Result> results = search(query);
            if (results.isEmpty()) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Поиск не вернул доступных результатов.");
            StringBuilder out = new StringBuilder("Веб-исследование: \"" ).append(query).append("\"\n");
            out.append("Источники: ").append(Math.min(results.size(), MAX_SOURCES_IN_REPORT)).append("\n\n");
            int successful = 0;
            Set<String> seen = new LinkedHashSet<>();
            int sourceNo = 0;
            for (Result r : results) {
                if (sourceNo >= MAX_SOURCES_IN_REPORT) break;
                String normalized = normalizeUrl(r.url);
                if (normalized.isBlank() || !seen.add(normalized)) continue;
                Page page = fetch(normalized);
                sourceNo++;
                out.append("[Источник ").append(sourceNo).append("]\n")
                        .append("Название: ").append(r.title.isBlank() ? "Без названия" : r.title).append('\n')
                        .append("URL: ").append(normalized).append('\n')
                        .append("Домен: ").append(domain(normalized)).append('\n');
                if (!r.snippet.isBlank()) out.append("Поисковый фрагмент: ").append(r.snippet).append('\n');
                if (page.ok) {
                    successful++;
                    out.append("Извлечённый текст: ").append(page.text.isBlank() ? "текст не извлечён" : page.text).append("\n");
                } else {
                    out.append("Страница не прочитана: ").append(page.error).append('\n');
                }
                out.append('\n');
            }
            if (successful == 0) return Models.ToolResult.fail(Models.ResultCode.FAILED, "Найденные источники не удалось прочитать. Открыть результаты поиска можно через web_search.");
            out.append("Проверка источников: ").append(successful).append(" из ").append(sourceNo).append(" страниц прочитано.\n");
            out.append("Важно: JARVIS показывает фактические источники и извлечённые фрагменты; итоговые выводы должны опираться на них, а изменяемые страницы могут обновиться.");
            return Models.ToolResult.ok(out.toString().trim());
        } catch (SocketTimeoutException e) {
            return Models.ToolResult.fail(Models.ResultCode.TIMEOUT, "Веб-исследование превысило лимит времени.");
        } catch (Exception e) {
            return Models.ToolResult.fail(Models.ResultCode.OFFLINE, "Не удалось выполнить веб-исследование: " + safe(e.getMessage()));
        }
    }

    private String extractQuery(String input) {
        try {
            JSONObject o = new JSONObject(input);
            String q = o.optString("query", "").trim();
            if (!q.isBlank()) return q;
        } catch (Exception ignored) {}
        String q = input == null ? "" : input.trim();
        q = q.replaceFirst("(?is).*?(исследуй в интернете|проведи исследование|найди информацию|найди сведения|изучи в интернете|найди источники|сравни источники|исследуй|web research|research|исследование в интернете)\\s*", "").trim();
        return q;
    }

    private List<Result> search(String query) throws Exception {
        URL u = new URL("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        String html = request(u, MAX_SEARCH_BYTES, "text/html");
        List<Result> list = new ArrayList<>();
        Matcher m = Pattern.compile("<a[^>]+class=\\\"result__a\\\"[^>]+href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (m.find() && list.size() < MAX_RESULTS) {
            String url = decodeSearchUrl(m.group(1));
            if (!url.startsWith("http://") && !url.startsWith("https://")) continue;
            String title = clean(m.group(2));
            String snippet = "";
            int end = Math.min(html.length(), m.end() + 1800);
            String around = html.substring(m.end(), end);
            Matcher sm = Pattern.compile("<a[^>]+class=\\\"result__snippet\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(around);
            if (sm.find()) snippet = clean(sm.group(1));
            list.add(new Result(title, url, snippet));
        }
        return list;
    }

    private Page fetch(String target) throws Exception {
        try {
            URL u = new URL(target);
            String html = request(u, 1_200_000, "text/html,application/xhtml+xml,text/plain");
            String title = "";
            Matcher tm = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
            if (tm.find()) title = clean(tm.group(1));
            String text = extractText(html);
            return new Page(true, title, text, "");
        } catch (Exception e) {
            return new Page(false, "", "", safe(e.getMessage()));
        }
    }

    private String request(URL url, int maxBytes, String accept) throws Exception {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setInstanceFollowRedirects(true); c.setConnectTimeout(8000); c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "JARVIS/1.10 Android Web Research");
        c.setRequestProperty("Accept", accept);
        int code = c.getResponseCode();
        if (code < 200 || code >= 400) throw new IOException("HTTP " + code);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int total = 0, n;
            while ((n = in.read(b)) != -1) { total += n; if (total > maxBytes) break; out.write(b, 0, Math.min(n, maxBytes - (total - n))); if (total >= maxBytes) break; }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally { c.disconnect(); }
    }

    private String extractText(String html) {
        String s = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
                .replaceAll("(?is)<svg[^>]*>.*?</svg>", " ")
                .replaceAll("(?is)<[^>]+>", " ");
        s = clean(s);
        if (s.length() > MAX_PAGE_CHARS) s = s.substring(0, MAX_PAGE_CHARS) + "…";
        return s;
    }

    private String normalizeUrl(String value) {
        try {
            URL u = new URL(value);
            String protocol = u.getProtocol().toLowerCase(Locale.ROOT);
            if (!protocol.equals("https") && !protocol.equals("http")) return "";
            return u.toURI().normalize().toString();
        } catch (Exception e) { return ""; }
    }

    private String domain(String value) {
        try { return new URL(value).getHost(); } catch (Exception e) { return "неизвестен"; }
    }

    private String decodeSearchUrl(String value) {
        try {
            String v = value.replace("&amp;", "&");
            Uri u = Uri.parse(v);
            String uddg = u.getQueryParameter("uddg");
            return uddg == null ? URLDecoder.decode(v, StandardCharsets.UTF_8) : uddg;
        } catch (Exception e) { return value; }
    }
    private String clean(String s) {
        return android.text.Html.fromHtml(s == null ? "" : s, android.text.Html.FROM_HTML_MODE_LEGACY)
                .toString().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
    private String safe(String s) { return s == null || s.isBlank() ? "ошибка сети" : s; }
    private record Result(String title, String url, String snippet) {}
    private record Page(boolean ok, String title, String text, String error) {}
}
