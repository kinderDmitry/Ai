package com.jarvis.nextgen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Local PDF -> rendered page -> on-device OCR pipeline. No fabricated text. */
public final class PdfOcrPipeline {
    private static final int MAX_PAGES = 30;
    private static final int MAX_WIDTH = 1600;
    private static final long PAGE_TIMEOUT_SECONDS = 15;
    private final Context context;

    public PdfOcrPipeline(Context context) { this.context = context.getApplicationContext(); }

    public Result process(Uri uri) {
        if (uri == null) return Result.failed("PDF не выбран.");
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return Result.failed("Android не предоставил доступ к PDF.");
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                int pages = renderer.getPageCount();
                if (pages <= 0) return Result.failed("PDF не содержит страниц.");
                if (pages > MAX_PAGES) return Result.failed("PDF содержит " + pages + " страниц. Безопасный локальный OCR-лимит — " + MAX_PAGES + ".");
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                StringBuilder all = new StringBuilder();
                try {
                    for (int i = 0; i < pages; i++) {
                        try (PdfRenderer.Page page = renderer.openPage(i)) {
                            int width = Math.min(MAX_WIDTH, Math.max(800, page.getWidth() * 2));
                            int height = Math.max(800, (int) (((double) page.getHeight() / Math.max(1, page.getWidth())) * width));
                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            String text = recognize(recognizer, bitmap);
                            bitmap.recycle();
                            all.append("\n--- Страница ").append(i + 1).append(" ---\n");
                            if (text.isEmpty()) all.append("[Текст не распознан]"); else all.append(text);
                        }
                    }
                } finally { recognizer.close(); }
                return Result.ok(all.toString().trim(), pages);
            }
        } catch (Exception e) {
            return Result.failed("Не удалось обработать PDF: " + e.getClass().getSimpleName() + ".");
        }
    }

    private String recognize(TextRecognizer recognizer, Bitmap bitmap) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> value = new AtomicReference<>("");
        final AtomicReference<Exception> error = new AtomicReference<>();
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(r -> { value.set(r.getText() == null ? "" : r.getText().trim()); latch.countDown(); })
                .addOnFailureListener(e -> { error.set(e); latch.countDown(); });
        if (!latch.await(PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw new IllegalStateException("OCR timeout");
        if (error.get() != null) throw error.get();
        return value.get();
    }

    public static final class Result {
        public final boolean success; public final String text; public final int pages; public final String message;
        private Result(boolean success, String text, int pages, String message) { this.success=success; this.text=text; this.pages=pages; this.message=message; }
        public static Result ok(String text, int pages) { return new Result(true, text, pages, text); }
        public static Result failed(String message) { return new Result(false, "", 0, message); }
    }
}
