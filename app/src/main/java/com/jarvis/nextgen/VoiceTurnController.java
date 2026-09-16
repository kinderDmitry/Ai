package com.jarvis.nextgen;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;

/** Controls a bounded continuous-conversation session. It never exposes model chain-of-thought. */
public final class VoiceTurnController {
    public interface Host { void startListening(); void stopListening(); void setStatus(String status); }
    private final Host host; private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean enabled = new AtomicBoolean(false); private long silenceTimeoutMs = 1800;
    public VoiceTurnController(Host host){this.host=host;}
    public void setSilenceTimeoutMs(long ms){silenceTimeoutMs=Math.max(600,Math.min(5000,ms));}
    public void enable(){if(enabled.compareAndSet(false,true)){host.setStatus("LISTENING");host.startListening();}}
    public void disable(){if(enabled.getAndSet(false)){handler.removeCallbacksAndMessages(null);host.stopListening();host.setStatus("IDLE");}}
    public void onTurnFinished(){if(!enabled.get())return;host.setStatus("THINKING");handler.postDelayed(()->{if(enabled.get()){host.setStatus("LISTENING");host.startListening();}},silenceTimeoutMs);}
    public boolean enabled(){return enabled.get();}
}
