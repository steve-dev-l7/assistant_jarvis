    package com.example.translateanywhere;

    import android.Manifest;
    import android.animation.ObjectAnimator;
    import android.animation.PropertyValuesHolder;
    import android.animation.ValueAnimator;
    import android.annotation.SuppressLint;
    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.content.pm.PackageManager;
    import android.graphics.Color;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Handler;
    import android.speech.RecognitionListener;
    import android.speech.SpeechRecognizer;
    import android.util.Log;
    import android.view.View;
    import android.view.WindowInsets;
    import android.view.WindowInsetsController;
    import android.view.WindowManager;
    import android.widget.TextView;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;
    import androidx.localbroadcastmanager.content.LocalBroadcastManager;


    import com.airbnb.lottie.LottieAnimationView;

    import java.util.Objects;


    public class GlowActivity extends AppCompatActivity  {

        private ObjectAnimator pulseAnimator;
        LottieAnimationView jarvisSpeaking;

        TextView liveText;
        String word;

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if ("ACTION_LIVE_WORD".equals(action)) {
                    word = Objects.requireNonNull(intent.getStringExtra("word"));
                    startPulse();
                    Log.d("LiveDetectedWords", "Received word: " + word);

                } else if ("ACTION_FINISH_ACTIVITY".equals(action)) {
                    Log.d("GlowActivity", "Finishing due to speech end");
                    stopPulse();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finishAndRemoveTask();
                        }
                    },2000);


                }
            }
        };


        @SuppressLint("MissingInflatedId")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            hideSystemUI(getWindow().getDecorView());
            Log.d("GlowActivity","Launched");
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_glow);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;

            });
            liveText=findViewById(R.id.liveText1);
            liveText.setVisibility(View.VISIBLE);
            startWaveAnimation();

        }
        private void hideSystemUI(View view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController insetsController = view.getWindowInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
                    insetsController.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                view.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
        }

        private void startWaveAnimation() {

            jarvisSpeaking = findViewById(R.id.jarvisSpeaking);
            jarvisSpeaking.setVisibility(View.VISIBLE);
            jarvisSpeaking.playAnimation();



        }
        protected void onStart() {
            super.onStart();
            IntentFilter filter = new IntentFilter();
            filter.addAction("ACTION_LIVE_WORD");
            filter.addAction("ACTION_FINISH_ACTIVITY");
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
        }

        @Override
        protected void onStop() {
            super.onStop();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }

        @SuppressLint("SetTextI18n")
        private void startPulse() {
            liveText.setText("Hey Jarvis > "+word);
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

        private void stopPulse() {
            if (pulseAnimator != null) {
                jarvisSpeaking.cancelAnimation();
                pulseAnimator.cancel();
                jarvisSpeaking.setScaleX(1f);
                jarvisSpeaking.setScaleY(1f);
            }
        }



    }