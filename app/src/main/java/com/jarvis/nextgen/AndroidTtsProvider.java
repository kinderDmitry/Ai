package com.jarvis.nextgen;

import android.content.Context;import android.speech.tts.*;import java.util.Locale;import java.util.UUID;

public final class AndroidTtsProvider implements TtsProvider, TextToSpeech.OnInitListener {
 private final Context c; private TextToSpeech tts; private boolean ready; private Listener pending;
 public AndroidTtsProvider(Context c){this.c=c.getApplicationContext();tts=new TextToSpeech(this.c,this);}
 public boolean available(){return ready&&tts!=null;}
 public void speak(String text,Locale locale,float rate,Listener l){if(!available()){if(l!=null)l.onError("Android TTS недоступен.");return;}pending=l;int r=tts.setLanguage(locale);if(r==TextToSpeech.LANG_MISSING_DATA||r==TextToSpeech.LANG_NOT_SUPPORTED){l.onError("Язык TTS недоступен.");return;}tts.setSpeechRate(Math.max(.5f,Math.min(2f,rate)));String id=UUID.randomUUID().toString();tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String x){if(pending!=null)pending.onStart();}public void onDone(String x){if(pending!=null){pending.onProgress(1f);pending.onDone();}}public void onError(String x){if(pending!=null)pending.onError("Ошибка TTS.");}});tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id);}
 public void stop(){if(tts!=null)tts.stop();pending=null;}
 public void close(){stop();if(tts!=null){tts.shutdown();tts=null;}}
 public void onInit(int status){ready=status==TextToSpeech.SUCCESS;}
}
