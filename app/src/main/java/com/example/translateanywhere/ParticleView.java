package com.example.translateanywhere;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleView extends View {

    private static final int   PARTICLE_COUNT    = 240; 
    private static final int   BG_STAR_COUNT     = 120; 
    private static final float MAX_SPEED         = 0.8f; 
    private static final float CONNECT_DISTANCE  = 180f;
    private static final int   FRAME_MS          = 16;   

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> bgStars   = new ArrayList<>();
    
    private final Paint  dotPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  linePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  glowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  nebulaPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng          = new Random();

    private boolean running = false;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            update();
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    private static final int COLOR_CORE       = Color.argb(255, 255, 255, 220);
    private static final int COLOR_AMBER      = Color.argb(255, 255, 170, 50);
    private static final int COLOR_NEBULA     = Color.argb(35, 255, 80, 0);

    public ParticleView(Context context) {
        super(context);
        init();
    }

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        spawnParticles(w, h);
    }

    private void spawnParticles(int w, int h) {
        particles.clear();
        bgStars.clear();
        float cx = w / 2f;
        float cy = h / 2f;

        // 1. Distant Universe Stars
        for (int i = 0; i < BG_STAR_COUNT; i++) {
            bgStars.add(new Particle(
                rng.nextFloat() * w,
                rng.nextFloat() * h,
                (rng.nextFloat() - 0.5f) * 0.08f,
                (rng.nextFloat() - 0.5f) * 0.08f,
                0.4f + rng.nextFloat() * 1.2f,
                80 + rng.nextInt(120)
            ));
        }

        // 2. Focused but scattered neural particles
        float spawnW = w * 0.8f;
        float spawnH = h * 0.8f;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = (w - spawnW)/2 + rng.nextFloat() * spawnW;
            float y = (h - spawnH)/2 + rng.nextFloat() * spawnH;

            float speed = 0.2f + rng.nextFloat() * MAX_SPEED;
            float vx    = (rng.nextFloat() - 0.5f) * speed;
            float vy    = (rng.nextFloat() - 0.5f) * speed;

            float size  = rng.nextFloat() < 0.12f ? 3f + rng.nextFloat() * 4f : 0.8f + rng.nextFloat() * 1.8f;
            int alpha = 130 + rng.nextInt(120);
            particles.add(new Particle(x, y, vx, vy, size, alpha));
        }
    }

    private void update() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        for (Particle p : bgStars) {
            p.x += p.vx; p.y += p.vy;
            if (p.x < 0) p.x = w; if (p.x > w) p.x = 0;
            if (p.y < 0) p.y = h; if (p.y > h) p.y = 0;
        }

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            // Soft Bouncing from edges - Keeps them scattered but contained
            if (p.x < 0 || p.x > w) { p.vx *= -1; p.x = Math.max(0, Math.min(w, p.x)); }
            if (p.y < 0 || p.y > h) { p.vy *= -1; p.y = Math.max(0, Math.min(h, p.y)); }

            // Minimal drift for life
            p.vx += (rng.nextFloat() - 0.5f) * 0.008f;
            p.vy += (rng.nextFloat() - 0.5f) * 0.008f;

            float spd = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
            if (spd > MAX_SPEED) {
                p.vx = (p.vx / spd) * MAX_SPEED;
                p.vy = (p.vy / spd) * MAX_SPEED;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        nebulaPaint.setShader(new RadialGradient(cx, cy, Math.max(w, h) * 0.7f,
            new int[]{COLOR_NEBULA, Color.argb(12, 180, 40, 0), Color.TRANSPARENT},
            null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, Math.max(w, h) * 0.7f, nebulaPaint);

        for (Particle p : bgStars) {
            dotPaint.setColor(Color.argb(p.alpha, 255, 230, 180));
            canvas.drawCircle(p.x, p.y, p.size, dotPaint);
        }

        if (!particles.isEmpty()) {
            int n = particles.size();
            for (int i = 0; i < n; i++) {
                Particle a = particles.get(i);
                for (int j = i + 1; j < n; j += 6) { 
                    Particle b = particles.get(j);
                    float ddx = a.x - b.x;
                    float ddy = a.y - b.y;
                    float d   = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                    if (d < CONNECT_DISTANCE) {
                        float ratio = 1f - d / CONNECT_DISTANCE;
                        int   alpha = (int)(ratio * ratio * 130);
                        linePaint.setColor(Color.argb(alpha, 255, 170, 40));
                        linePaint.setStrokeWidth(ratio * 1.1f);
                        canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
                    }
                }
            }
        }

        for (Particle p : particles) {
            glowPaint.setShader(new RadialGradient(
                    p.x, p.y, p.size * 5,
                    new int[]{ Color.argb(p.alpha / 2, 255, 190, 50), Color.TRANSPARENT },
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(p.x, p.y, p.size * 5, glowPaint);

            dotPaint.setColor(Color.argb(p.alpha, 255, 245, 190));
            canvas.drawCircle(p.x, p.y, p.size * 0.85f, dotPaint);
        }

        float coreR = Math.min(w, h) * 0.28f;
        Paint ambientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ambientPaint.setShader(new RadialGradient(cx, cy, coreR,
                new int[]{Color.argb(80, 255, 210, 100), Color.argb(20, 200, 50, 0), Color.TRANSPARENT},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, coreR, ambientPaint);
    }

    public void startAnimation() {
        if (!running) {
            running = true;
            post(tick);
        }
    }

    public void stopAnimation() {
        running = false;
        removeCallbacks(tick);
    }

    private static class Particle {
        float x, y, vx, vy, size;
        int   alpha;
        Particle(float x, float y, float vx, float vy, float size, int alpha) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.size = size; this.alpha = alpha;
        }
    }
}
