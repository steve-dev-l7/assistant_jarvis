package com.example.translateanywhere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class JarvisBrainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen black window
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_jarvis_brain);

        ParticleView particleView = findViewById(R.id.particleView);
        if (particleView != null) {
            particleView.startAnimation();
        }

        ImageButton btnBack = findViewById(R.id.btnBrainBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ParticleView particleView = findViewById(R.id.particleView);
        if (particleView != null) particleView.stopAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ParticleView particleView = findViewById(R.id.particleView);
        if (particleView != null) particleView.startAnimation();
    }
}
