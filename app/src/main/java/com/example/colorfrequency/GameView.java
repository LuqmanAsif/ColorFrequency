package com.example.colorfrequency;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends View {
    public static final String PREFS = "colorfreq";
    public static final String KEY_HIGH = "high";
    public static final String KEY_SOUND = "sound";

    public interface Listener {
        void onGameOver(int score, int best, boolean newBest, boolean canRevive);
    }

    private static final int[] COLORS = {
            0xFFFF3B5C, // red
            0xFF2EC4FF, // blue
            0xFF3DDC84, // green
            0xFFFFD60A  // yellow
    };
    private static final int BG = 0xFF0B0B14;
    private static final int REVIVE_MIN_SCORE = 5;

    private enum State { PLAYING, OVER }

    private static class Ring {
        float r;
        final int color;
        Ring(float r, int color) { this.r = r; this.color = color; }
    }

    private final Random rnd = new Random();
    private final ArrayList<Ring> rings = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path marker = new Path();
    private final SharedPreferences prefs;

    private Listener listener;
    private State state = State.PLAYING;

    private final float density;
    private float coreR, ringThickness, startR, cx, cy;

    private float rotation = 0f, targetRotation = 0f;
    private int topIndex = 0;
    private float pulse = 0f;

    private int score = 0;
    private int highScore;
    private boolean revivedThisRun = false;
    private float spawnTimer = 1.3f;
    private long lastNanos = 0;

    // audio
    private static final int SAMPLE_RATE = 22050;
    private AudioTrack track;
    private float lastRate = 1f;

    public GameView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        highScore = prefs.getInt(KEY_HIGH, 0);
        setKeepScreenOn(true);
    }

    public void setListener(Listener l) { listener = l; }

    // ---------------------------------------------------------- lifecycle

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f;
        coreR = Math.min(w, h) * 0.12f;
        ringThickness = 22f * density;
        startR = (float) Math.hypot(w, h) / 2f + ringThickness;
    }

    public void onResume() {
        lastNanos = 0;
        if (state == State.PLAYING) {
            startAudio();
            invalidate();
        }
    }

    public void onPause() {
        stopAudio();
    }

    // ---------------------------------------------------------- game control

    public void restart() {
        rings.clear();
        score = 0;
        revivedThisRun = false;
        spawnTimer = 1.3f;
        rotation = targetRotation;
        state = State.PLAYING;
        lastNanos = 0;
        startAudio();
        invalidate();
    }

    public void revive() {
        rings.clear();
        revivedThisRun = true;
        spawnTimer = 1.5f;
        state = State.PLAYING;
        lastNanos = 0;
        startAudio();
        invalidate();
    }

    private void gameOver() {
        state = State.OVER;
        stopAudio();
        final boolean newBest = score > highScore;
        if (newBest) {
            highScore = score;
            prefs.edit().putInt(KEY_HIGH, highScore).apply();
        }
        final int s = score, b = highScore;
        final boolean canRevive = !revivedThisRun && score >= REVIVE_MIN_SCORE;
        post(() -> { if (listener != null) listener.onGameOver(s, b, newBest, canRevive); });
    }

    private float speedFactor() {
        if (score <= 10) {
            return 0.8f + 0.2f * (score / 10f);
        } else {
            return Math.min(1.0f + (score - 10) * 0.025f, 1.8f);
        }
    }

    private void update(float dt) {
        float sf = speedFactor();

        spawnTimer -= dt * sf;
        if (spawnTimer <= 0f) {
            rings.add(new Ring(startR, rnd.nextInt(COLORS.length)));
            spawnTimer = 1.2f + rnd.nextFloat() * 0.6f;
        }

        float speed = 150f * density * sf;
        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            ring.r -= speed * dt;
            if (ring.r - ringThickness / 2f <= coreR) {
                it.remove();
                if (ring.color == topIndex) {
                    score++;
                    pulse = 1f;
                } else {
                    gameOver();
                    return;
                }
            }
        }

        rotation += (targetRotation - rotation) * Math.min(1f, dt * 18f);
        pulse = Math.max(0f, pulse - dt * 4f);
        updateAudioRate(sf);
    }

    // ---------------------------------------------------------- input

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN && state == State.PLAYING) {
            targetRotation += 90f;
            topIndex = (topIndex + 3) % 4; // clockwise 90deg brings the left arc to the top
        }
        return true;
    }

    // ---------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min((now - lastNanos) / 1e9f, 0.05f);
        lastNanos = now;

        if (state == State.PLAYING) update(dt);

        canvas.drawColor(BG);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ringThickness);
        for (Ring ring : rings) {
            paint.setColor(COLORS[ring.color]);
            canvas.drawCircle(cx, cy, ring.r, paint);
        }

        float r = coreR * (1f + 0.12f * pulse);
        oval.set(cx - r, cy - r, cx + r, cy + r);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            paint.setColor(COLORS[i]);
            canvas.drawArc(oval, -135f + i * 90f + rotation, 90f, true, paint);
        }
        paint.setColor(BG);
        canvas.drawCircle(cx, cy, r * 0.45f, paint);

        marker.reset();
        float m = 10f * density;
        marker.moveTo(cx, cy - r - m * 0.3f);
        marker.lineTo(cx - m, cy - r - m * 1.8f);
        marker.lineTo(cx + m, cy - r - m * 1.8f);
        marker.close();
        paint.setColor(Color.WHITE);
        canvas.drawPath(marker, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(48f * density);
        canvas.drawText(String.valueOf(score), cx, 90f * density, paint);
        paint.setTextSize(14f * density);
        paint.setColor(0x99FFFFFF);
        canvas.drawText("BEST " + highScore, cx, 112f * density, paint);

        if (state == State.PLAYING) postInvalidateOnAnimation();
    }

    // ---------------------------------------------------------- procedural audio
    // 1-second looped beat generated in code; a higher playback rate speeds up tempo and pitch together.

    private void startAudio() {
        if (track != null || !prefs.getBoolean(KEY_SOUND, true)) return;
        try {
            short[] pcm = buildLoop();
            track = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    pcm.length * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            track.write(pcm, 0, pcm.length);
            track.setLoopPoints(0, pcm.length, -1);
            track.setVolume(0.6f);
            track.setPlaybackRate((int) (SAMPLE_RATE * speedFactor()));
            track.play();
            lastRate = speedFactor();
        } catch (Exception ex) {
            track = null; // game still works silently
        }
    }

    private void stopAudio() {
        if (track != null) {
            try { track.stop(); } catch (Exception ignored) { }
            track.release();
            track = null;
        }
    }

    private void updateAudioRate(float sf) {
        if (track == null || Math.abs(sf - lastRate) < 0.02f) return;
        lastRate = sf;
        try { track.setPlaybackRate((int) (SAMPLE_RATE * sf)); } catch (Exception ignored) { }
    }

    private short[] buildLoop() {
        int n = SAMPLE_RATE;
        short[] out = new short[n];
        int beats = 4, beatLen = n / beats;
        for (int b = 0; b < beats; b++) {
            double baseFreq = (b % 2 == 0) ? 55.0 : 82.4;
            for (int i = 0; i < beatLen; i++) {
                double t = i / (double) SAMPLE_RATE;
                double env = Math.exp(-t * 14.0);
                double kickF = 120.0 * Math.exp(-t * 25.0) + 40.0;
                double kick = Math.sin(2 * Math.PI * kickF * t) * env;
                double bass = Math.sin(2 * Math.PI * baseFreq * t) * Math.exp(-t * 6.0) * 0.5;
                double hat = 0;
                int hs = beatLen / 2;
                if (i > hs && i < hs + 600) {
                    hat = (rnd.nextDouble() * 2 - 1) * 0.15 * Math.exp(-(i - hs) / 150.0);
                }
                double s = (kick * 0.8 + bass + hat) * 0.8;
                out[b * beatLen + i] = (short) (Math.max(-1, Math.min(1, s)) * 32767);
            }
        }
        return out;
    }
}
