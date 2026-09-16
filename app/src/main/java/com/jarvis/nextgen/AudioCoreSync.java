package com.jarvis.nextgen;


/** Bridges real TTS lifecycle to the visual core; no synthetic speech waveform is generated. */
public final class AudioCoreSync implements TtsProvider.Listener {
 private final JarvisCoreView core; public AudioCoreSync(JarvisCoreView core){this.core=core;}
 public void onStart(){core.setState("SPEAKING");core.setAudioLevel(.25f);}
 public void onProgress(float p){core.setAudioLevel(.18f + .18f*(1f-p));}
 public void onDone(){core.setAudioLevel(0f);core.setState("SUCCESS");}
 public void onError(String m){core.setAudioLevel(0f);core.setState("ERROR");}
}
