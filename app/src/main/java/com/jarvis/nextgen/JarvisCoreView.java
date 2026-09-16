package com.jarvis.nextgen;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Premium lightweight AI core. Motion is state-driven and audio-reactive; no fake audio loop. */
public final class JarvisCoreView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    private String state = "IDLE";
    private float level;
    private long lastFrame;

    public JarvisCoreView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setState(String s) {
        state = s == null ? "IDLE" : s;
        invalidate();
    }

    public void setAudioLevel(float v) {
        level = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    private float activity() {
        if ("LISTENING".equals(state)) return level * .72f;
        if ("THINKING".equals(state)) return .22f;
        if ("PLANNING".equals(state)) return .28f;
        if ("EXECUTING".equals(state)) return .34f;
        if ("SPEAKING".equals(state)) return .30f;
        if ("SUCCESS".equals(state)) return .48f;
        if ("ERROR".equals(state)) return .12f;
        return .035f;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = System.nanoTime();
        if (lastFrame == 0L) lastFrame = now;
        float dt = Math.min(.08f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        phase += dt * ("IDLE".equals(state) ? .55f : 1.35f);

        float cx = getWidth() * .5f;
        float cy = getHeight() * .51f;
        float base = Math.min(getWidth(), getHeight()) * .5f;
        float a = activity();

        p.clearShadowLayer();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(2, 5, 11));
        c.drawRect(0, 0, getWidth(), getHeight(), p);

        // Subtle radial atmosphere.
        RadialGradient atmosphere = new RadialGradient(cx, cy, base * .48f,
                new int[]{Color.argb(34, 35, 115, 220), Color.argb(10, 15, 55, 120), Color.TRANSPARENT},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP);
        p.setShader(atmosphere);
        c.drawCircle(cx, cy, base * .48f, p);
        p.setShader(null);

        // State-driven rings.
        for (int i = 0; i < 8; i++) {
            float r = base * (.17f + i * .052f) + (float)Math.sin(phase * (1f + i*.035f) + i) * (3f + a*8f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(i == 0 ? 2.2f : 1f);
            int alpha = 24 + i * 15;
            p.setColor(Color.argb(alpha, 65, 170, 255));
            c.drawCircle(cx, cy, r, p);
        }

        // Rotating segmented energy arcs.
        for (int i = 0; i < 4; i++) {
            float r = base * (.25f + i*.047f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.4f - i*.3f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.argb(95 - i*12, 115, 205, 255));
            float start = phase * (18f + i*7f) + i*70f;
            c.drawArc(cx-r, cy-r, cx+r, cy+r, start, 54f + a*55f, false, p);
            c.drawArc(cx-r, cy-r, cx+r, cy+r, start+145f, 26f + a*30f, false, p);
        }

        // Core.
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(70, 175, 255));
        p.setShadowLayer(28f + a*45f, 0, 0, Color.rgb(25, 120, 255));
        c.drawCircle(cx, cy, base*(.105f + a*.035f), p);
        p.clearShadowLayer();

        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(Math.max(15f, base*.055f));
        p.setColor(Color.WHITE);
        c.drawText("JARVIS", cx, cy + base*.018f, p);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(Math.max(9f, base*.026f));
        p.setColor(Color.rgb(155, 200, 235));
        c.drawText(state, cx, cy + base*.16f, p);

        // Real microphone activity is represented only when the VoiceEngine supplies level.
        if ("LISTENING".equals(state)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            p.setColor(Color.argb(150, 100, 210, 255));
            float waveR = base*(.135f + level*.04f);
            c.drawCircle(cx, cy, waveR, p);
        }

        // Sparse orbital particles; deterministic, low-cost and state-driven.
        p.setStyle(Paint.Style.FILL);
        for (int i=0; i<22; i++) {
            double ang = phase*(.22 + (i%4)*.055) + i*Math.PI*2/22;
            float rr = base*(.33f + (i%5)*.018f);
            float x = cx + (float)Math.cos(ang)*rr;
            float y = cy + (float)Math.sin(ang)*rr;
            p.setColor(Color.argb(35 + (i%5)*15, 85, 190, 255));
            c.drawCircle(x, y, 1.1f + (i%3)*.35f, p);
        }

        postInvalidateDelayed(33);
    }
}
