package com.jarvis.nextgen;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.Locale;

/** Real Android voice abstraction: SpeechRecognizer + TTS. */
public final class VoiceEngine implements TextToSpeech.OnInitListener {
    public interface Listener {
        void onListening();
        void onLevel(float level);
        void onSpeech(String text);
        void onThinking();
        void onError(int code);
    }
    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean continuous;
    private boolean initialized;

    public VoiceEngine(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        tts = new TextToSpeech(this.context, this);
    }
    public void setContinuous(boolean enabled) { continuous = enabled; }
    public boolean isContinuous() { return continuous; }
    public void start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError(-1000);
            return;
        }
        stopRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { listener.onListening(); }
            public void onBeginningOfSpeech() { listener.onListening(); }
            public void onRmsChanged(float rms) { listener.onLevel(Math.min(1f, Math.max(0f, (rms + 2f) / 12f))); }
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { listener.onThinking(); }
            public void onError(int code) { listener.onError(code); if (continuous) restartLater(); }
            public void onResults(Bundle b) {
                ArrayList<String> results = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results != null && !results.isEmpty()) listener.onSpeech(results.get(0));
                if (continuous) restartLater();
            }
            public void onPartialResults(Bundle b) {}
            public void onEvent(int a, Bundle b) {}
        });
        IntentBuilder.start(recognizer);
    }
    private void restartLater() {
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.postDelayed(() -> { if (continuous) start(); }, 350);
    }
    private static final class IntentBuilder {
        static void start(SpeechRecognizer r) {
            android.content.Intent i = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            r.startListening(i);
        }
    }
    public void speak(String text) {
        if (!initialized || tts == null) return;
        tts.setLanguage(new Locale("ru", "RU"));
        tts.setSpeechRate(.98f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply");
    }
    public void stop() {
        continuous = false;
        stopRecognizer();
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    }
    private void stopRecognizer() { if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; } }
    @Override public void onInit(int status) { initialized = status == TextToSpeech.SUCCESS; }
}
