package com.jarvis.nextgen;

import android.content.Context;
import android.database.Cursor;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Real local-document access through Android Storage Access Framework. */
public final class DocumentWorkspace {
    private static final int MAX_TEXT_BYTES = 5_000_000;
    private final Context context;
    public DocumentWorkspace(Context context) { this.context = context.getApplicationContext(); }

    public Result inspect(Uri uri) {
        if (uri == null) return Result.failed("Файл не выбран.");
        String name = displayName(uri);
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        try {
            if (mime.equals("application/pdf") || name.toLowerCase(Locale.ROOT).endsWith(".pdf")) return inspectPdf(uri, name, mime);
            if (isTextLike(mime, name)) return inspectText(uri, name, mime);
            return Result.unsupported("Файл «" + name + "» выбран, но встроенное извлечение текста для формата " + mime + " не поддерживается. Выберите TXT/CSV/JSON/XML/MD/HTML или подключите отдельный парсер.");
        } catch (Exception e) {
            return Result.failed("Не удалось прочитать «" + name + ": " + e.getClass().getSimpleName() + ".");
        }
    }

    private Result inspectText(Uri uri, String name, String mime) throws Exception {
        byte[] bytes = readLimited(uri, MAX_TEXT_BYTES);
        String text = decode(bytes);
        if (name.toLowerCase(Locale.ROOT).endsWith(".html") || mime.contains("html")) text = stripHtml(text);
        String sha = sha256(bytes);
        if (text.trim().isEmpty()) return Result.ok("Файл: " + name + "\nТип: " + mime + "\nSHA-256: " + sha + "\n\nТекстового содержимого не найдено.");
        return Result.ok("Файл: " + name + "\nТип: " + mime + "\nРазмер: " + bytes.length + " байт\nSHA-256: " + sha + "\n\n" + text);
    }

    private Result inspectPdf(Uri uri, String name, String mime) throws Exception {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return Result.failed("Android не предоставил доступ к PDF.");
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                int pages = renderer.getPageCount();
                return Result.ok("PDF: " + name + "\nТип: " + mime + "\nСтраниц: " + pages + "\n\nAndroid PdfRenderer подтверждает структуру PDF, но сам по себе не извлекает текст. Для анализа текста нужен PDF-парсер/OCR.");
            }
        }
    }

    public byte[] readBytes(Uri uri, int maxBytes) throws Exception { return readLimited(uri, Math.max(1, maxBytes)); }

    private byte[] readLimited(Uri uri, int limit) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Android не предоставил доступ к файлу.");
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int total = 0, n;
            while ((n = in.read(buffer)) != -1) { total += n; if (total > limit) throw new IllegalStateException("Файл превышает безопасный лимит " + limit + " байт."); out.write(buffer, 0, n); }
            return out.toByteArray();
        }
    }

    private String decode(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0]&255)==0xEF && (bytes[1]&255)==0xBB && (bytes[2]&255)==0xBF) return new String(bytes, 3, bytes.length-3, StandardCharsets.UTF_8);
        if (bytes.length >= 2 && (bytes[0]&255)==0xFF && (bytes[1]&255)==0xFE) return new String(bytes, 2, bytes.length-2, StandardCharsets.UTF_16LE);
        if (bytes.length >= 2 && (bytes[0]&255)==0xFE && (bytes[1]&255)==0xFF) return new String(bytes, 2, bytes.length-2, StandardCharsets.UTF_16BE);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isTextLike(String mime, String name) {
        String n=name.toLowerCase(Locale.ROOT);
        return mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") || n.endsWith(".txt") || n.endsWith(".csv") || n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".md") || n.endsWith(".html") || n.endsWith(".htm");
    }
    private String stripHtml(String value) { return value.replaceAll("(?s)<script.*?</script>|<style.*?</style>", " ").replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s{2,}", " ").trim(); }
    private String sha256(byte[] b) throws Exception { MessageDigest d=MessageDigest.getInstance("SHA-256"); StringBuilder s=new StringBuilder(); for(byte x:d.digest(b)) s.append(String.format(Locale.ROOT,"%02x",x)); return s.toString(); }
    private String displayName(Uri uri) {
        try (Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) { if(c!=null&&c.moveToFirst()) return c.getString(0); } catch(Exception ignored) {}
        String s=uri.getLastPathSegment(); return s==null?"выбранный файл":s;
    }
    public static final class Result {
        public final boolean success; public final boolean supported; public final String message;
        private Result(boolean success, boolean supported, String message){this.success=success;this.supported=supported;this.message=message;}
        public static Result ok(String m){return new Result(true,true,m);} public static Result failed(String m){return new Result(false,true,m);} public static Result unsupported(String m){return new Result(false,false,m);}
    }
}
