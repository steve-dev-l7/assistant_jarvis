package com.example.translateanywhere;

import android.os.Bundle;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {
    private ParticleView particleBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        particleBackground = findViewById(R.id.helpParticleBackground);
        if (particleBackground != null) particleBackground.startAnimation();

        findViewById(R.id.btnHelpBack).setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (particleBackground != null) particleBackground.stopAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (particleBackground != null) particleBackground.startAnimation();
    }
}
