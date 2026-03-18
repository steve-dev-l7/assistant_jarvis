package com.example.translateanywhere;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText etName, etMobile, etLocation, etDOB;
    private TextView tvUserId;
    private SwitchCompat switchMute;
    private SharedPreferences userPrefs, appPrefs;
    private ParticleView particleBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userPrefs = getSharedPreferences("UserData", MODE_PRIVATE);
        appPrefs = getSharedPreferences("JarvisPrefs", MODE_PRIVATE);

        etName = findViewById(R.id.etSettingsName);
        etMobile = findViewById(R.id.etSettingsMobile);
        etLocation = findViewById(R.id.etSettingsLocation);
        etDOB = findViewById(R.id.etSettingsDOB);
        particleBackground = findViewById(R.id.settingsParticleBackground);
        if (particleBackground != null) particleBackground.startAnimation();
        tvUserId = findViewById(R.id.tvSettingsUserId);
        switchMute = findViewById(R.id.switchMute);

        loadData();

        // Mute switch saves IMMEDIATELY on toggle — no Save button needed
        switchMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPrefs.edit().putBoolean("isMuted", isChecked).apply();
            Toast.makeText(this, isChecked ? "System Sound: MUTED" : "System Sound: ACTIVE", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSettingsBack).setOnClickListener(v -> {
            finish();
        });

        findViewById(R.id.btnSettingsSave).setOnClickListener(v -> {
            saveData();
        });
    }

    private void loadData() {
        if (FetchUser.getInstance().isLoaded()) {
            etName.setText(FetchUser.getInstance().getName());
            etMobile.setText(FetchUser.getInstance().getMobile());
            etLocation.setText(FetchUser.getInstance().getLocation());
            etDOB.setText(FetchUser.getInstance().getDob());
            tvUserId.setText(FetchUser.getInstance().getUserId());
        } else {
            etName.setText(userPrefs.getString("UserName", "-"));
            etMobile.setText(userPrefs.getString("Mobile", "-"));
            etLocation.setText(userPrefs.getString("Location", "-"));
            etDOB.setText(userPrefs.getString("DOB", "-"));
            tvUserId.setText(userPrefs.getString("UserId", "JARVIS-PENDING"));
        }

        switchMute.setChecked(appPrefs.getBoolean("isMuted", false));
    }

    private void saveData() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();
        String userId = tvUserId.getText().toString();

        if (userId.equals("JARVIS-PENDING")) {
            Toast.makeText(this, "System ID Error: Cannot sync to cloud", Toast.LENGTH_SHORT).show();
            return;
        }

        // Local Update
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString("UserName", name);
        editor.putString("Mobile", mobile);
        editor.putString("Location", location);
        editor.putString("DOB", dob);
        editor.apply();

        appPrefs.edit().putBoolean("isMuted", switchMute.isChecked()).apply();

        // Firestore Update
        Map<String, Object> update = new HashMap<>();
        update.put("Name", name);
        update.put("Mobile", mobile);
        update.put("Location", location);
        update.put("DOB", dob);

        FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, "Cloud Protocol Synchronized", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Cloud Sync Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
