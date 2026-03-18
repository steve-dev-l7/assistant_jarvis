package com.example.translateanywhere;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.airbnb.lottie.LottieAnimationView;

public class GlowActivity extends AppCompatActivity {

    private ObjectAnimator pulseAnimator;
    private LottieAnimationView jarvisSpeaking;
    private TextView liveText;
    private String word;

    // 🔴 NEW: Handler declared globally so we can cancel it if activity dies early
    private final Handler finishHandler = new Handler(Looper.getMainLooper());
    private Runnable finishRunnable;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("ACTION_LIVE_WORD".equals(action)) {
                word = intent.getStringExtra("word");
                if (word != null) {
                    startPulse();
                    Log.d("LiveDetectedWords", "Received word: " + word);
                }
            } else if ("ACTION_FINISH_ACTIVITY".equals(action)) {
                Log.d("GlowActivity", "Finishing due to speech end");
                stopPulse();

                // 🔴 UPDATED: Reduced delay to 500ms for snappier exit
                finishRunnable = () -> finishAndRemoveTask();
                finishHandler.postDelayed(finishRunnable, 500);
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI(getWindow().getDecorView());
        Log.d("GlowActivity", "Launched");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_glow);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        liveText = findViewById(R.id.liveText1);
        liveText.setVisibility(View.VISIBLE);

        jarvisSpeaking = findViewById(R.id.jarvisSpeaking);
        startWaveAnimation();
    }

    private void hideSystemUI(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = view.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }
    }

    private void startWaveAnimation() {
        if (jarvisSpeaking != null) {
            jarvisSpeaking.setVisibility(View.VISIBLE);
            jarvisSpeaking.playAnimation();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registering receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_LIVE_WORD");
        filter.addAction("ACTION_FINISH_ACTIVITY");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    @SuppressLint("SetTextI18n")
    private void startPulse() {
        if (liveText != null) {
            liveText.setText("Hey Jarvis > " + word);
        }

        // Prevent multiple animations overlapping if user speaks fast
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            return;
        }

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                jarvisSpeaking,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.2f, 0.7f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.2f, 0.7f)
        );
        pulseAnimator.setDuration(600);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.start();
    }

    @SuppressLint("SetTextI18n")
    private void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (jarvisSpeaking != null) {
            jarvisSpeaking.cancelAnimation();
            jarvisSpeaking.setScaleX(1f);
            jarvisSpeaking.setScaleY(1f);
        }
        if (liveText != null) {
            liveText.setText("Hey Jarvis >");
        }
    }

    // 🔴 NEW: PROPER CLEANUP FUNCTION
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("GlowActivity", "Destroying GlowActivity, cleaning up memory...");

        // 1. Unregister Receiver (Moved from onStop to here for safer invisible handling)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        // 2. Cancel any pending finish requests to prevent Memory Leak
        if (finishRunnable != null) {
            finishHandler.removeCallbacks(finishRunnable);
        }

        // 3. Stop animations & clear listeners
        stopPulse();
        if (jarvisSpeaking != null) {
            jarvisSpeaking.removeAllUpdateListeners();
            jarvisSpeaking.removeAllAnimatorListeners();
        }

        jarvisSpeaking = null;
        liveText = null;
        pulseAnimator = null;
    }
}