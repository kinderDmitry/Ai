package com.jarvis.nextgen;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Minimal real system-assistant session surface. Android owns invocation and keyguard policy. */
public final class JarvisVoiceInteractionSessionService extends VoiceInteractionSessionService {
    @Override public VoiceInteractionSession onNewSession(Bundle args) { return new JarvisSession(this); }

    private static final class JarvisSession extends VoiceInteractionSession {
        JarvisSession(VoiceInteractionSessionService service) { super(service); }

        @Override public View onCreateContentView() {
            LinearLayout root = new LinearLayout(getContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);
            root.setPadding(40, 40, 40, 40);
            root.setBackgroundColor(Color.rgb(2,5,10));

            TextView title = new TextView(getContext());
            title.setText("J A R V I S");
            title.setTextColor(Color.WHITE);
            title.setTextSize(26);
            title.setGravity(Gravity.CENTER);
            root.addView(title, new LinearLayout.LayoutParams(-1, 70));

            TextView status = new TextView(getContext());
            status.setText("Системный Assistant активен");
            status.setTextColor(Color.rgb(120,190,255));
            status.setTextSize(14);
            status.setGravity(Gravity.CENTER);
            root.addView(status, new LinearLayout.LayoutParams(-1, 60));

            Button open = new Button(getContext());
            open.setText("ОТКРЫТЬ JARVIS");
            open.setOnClickListener(v -> {
                Intent i = new Intent(getContext(), com.jarvis.nextgen.MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(i);
                finish();
            });
            root.addView(open, new LinearLayout.LayoutParams(-1, 60));
            return root;
        }
    }
}
