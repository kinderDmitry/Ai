package com.jarvis.nextgen;

import android.content.Context;
import java.util.Locale;

public interface TtsProvider {
    interface Listener { void onStart(); void onProgress(float progress); void onDone(); void onError(String message); }
    boolean available();
    void speak(String text, Locale locale, float rate, Listener listener);
    void stop();
    static TtsProvider android(Context c){ return new AndroidTtsProvider(c); }
}
