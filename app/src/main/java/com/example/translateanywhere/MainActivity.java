package com.example.translateanywhere;



import android.Manifest;
import android.annotation.SuppressLint;

import android.app.ActivityManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.activity.EdgeToEdge;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import android.speech.tts.UtteranceProgressListener;

import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.FirebaseApp;


import java.util.Locale;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {
    Intent  intent1;
    TextToSpeech toSpeech;
    ImageButton wakeJarvis;
    private ParticleView particleBackground;
    FrameLayout btnProfile;
    LinearLayout chatContainer;
    ScrollView chatScroll;
    EditText etChatInput;
    FrameLayout btnChatSend;
    private boolean isTextToSpeechInitialized = false;

    ProgressDialog progressDialog;
    Toolbar toolbar1;
    TranslationHelper translationHelper;
    SpeechRecognizer speechRecognizer;
    Boolean wakeup = false;

    int currentPermissionIndex = 0;
    BiometricPrompt biometricPrompt;
    BiometricPrompt.PromptInfo promptInfo;
    Executor executor;

    JarvisEngine jarvisEngine;
    TextView txtSignalStrength;

    ImageView imgProfileMenu, imgMainProfile;
    ActivityResultLauncher<String[]> mOpenDocument;
    TextView tvWakeStatus;

    @SuppressLint("InlinedApi")
    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
    };





    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        particleBackground = findViewById(R.id.mainParticleBackground);
        if (particleBackground != null) particleBackground.startAnimation();
        
        mOpenDocument = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                    prefs.edit().putString("ProfileImageUri", uri.toString()).apply();
                    if (imgProfileMenu != null) {
                        imgProfileMenu.setImageURI(uri);
                    }
                    if (imgMainProfile != null) {
                        imgMainProfile.setImageURI(uri);
                        imgMainProfile.setImageTintList(null);
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        });

        requestNextPermission();
        intent1 = new Intent(getApplicationContext(), MyForegroundServices.class);
        FirebaseApp.initializeApp(this);
        chechProfile();
        hideSystemUI();
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (!getPackageName().equals(telecomManager.getDefaultDialerPackage())) {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
            startActivity(intent);
        }
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String UserId = sharedPreferences.getString("UserId", null);


        if (UserId != null ) {
            FetchUser.getInstance().fetchUserData(this, new FetchUser.OnUserFetchListener() {
                @Override
                public void onSuccess() {
                    Log.d("UserDetails", "Jarvis knows everything about   now!");
                    runOnUiThread(() -> updateProfileBanner());
                }

                @Override
                public void onError(String message) {
                    Log.d("UserDetails", "Failed to fetch: " + message);
                }
            });

        }else {
            updateprofile();
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, 1234); // or use ActivityResultLauncher on AndroidX
        }


        translationHelper = new TranslationHelper();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        toSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                toSpeech.setEngineByPackageName("com.google.android.tts");

                toSpeech.setLanguage(Locale.US);

                int result = toSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    isTextToSpeechInitialized = true;
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
        toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                if (wakeup) {
                    startForegroundService(intent1);
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onError(String s) {

            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });
        

        toolbar1 = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar1);
        wakeJarvis = findViewById(R.id.btnWakeJarvis);
        btnProfile = findViewById(R.id.btnProfile);
        chatContainer = findViewById(R.id.chatContainer);
        chatScroll = findViewById(R.id.chatScroll);
        etChatInput = findViewById(R.id.etChatInput);
        btnChatSend = findViewById(R.id.btnChatSend);
        jarvisEngine = new JarvisEngine(this);
        txtSignalStrength = findViewById(R.id.txtSignalStrength);
        tvWakeStatus = findViewById(R.id.tvWakeStatus);

        // Signal Strength / Battery Monitor
        BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) ((level / (float) scale) * 100);
                if (txtSignalStrength != null) {
                    txtSignalStrength.setText(batteryPct + "%");
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Send Button Logic
        btnChatSend.setOnClickListener(v -> {
            String text = etChatInput.getText().toString().trim();
            if (!text.isEmpty()) {
                addChatMessage("YOU: " + text, false);
                etChatInput.setText("");
                handleQuery(text);
            }
        });

        // Add Breathing/Pulsing Animation to Wake Button
        animateWakeButton();


        // Initial Jarvis Greeting
        addChatMessage("Hi, How can I assist your mission today, Commander?", true);

        // Profile Menu Logic
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        LinearLayout customMenu = findViewById(R.id.custom_menu);
        
        imgProfileMenu = findViewById(R.id.imgProfileMenu);
        imgMainProfile = findViewById(R.id.imgMainProfile);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) customMenu.getLayoutParams();
        params.width = (int) (width * 0.7);
        customMenu.setLayoutParams(params);

        updateProfileBanner();

        imgProfileMenu.setOnClickListener(v -> {
            mOpenDocument.launch(new String[]{"image/*"});
        });

        findViewById(R.id.btnMenuSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        // Wake Button Logic — handled by the unified listener below (after biometric setup)

        // Profile Button Logic
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.END);
        });

        // Menu Options Click Listeners
        findViewById(R.id.btnMenuWakewordKey).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            getAccessKey();
        });

        findViewById(R.id.btnMenuLogout).setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        findViewById(R.id.btnMenuHelp).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
        });

        findViewById(R.id.btnMenuJarvisBrain).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(MainActivity.this, JarvisBrainActivity.class));
        });

        findViewById(R.id.btnMenuContactUs).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            String url = "https://steve-dev-l7.github.io/Jarvis_support/";
            Intent intentUrl = new Intent(Intent.ACTION_VIEW);
            intentUrl.setData(Uri.parse(url));
            startActivity(intentUrl);
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Error 7", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Error 6", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "Error 8", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
        }
        executor = ContextCompat.getMainExecutor(this);

        // Unified Wake Button Toggle
        wakeJarvis.setOnClickListener(v -> {
            SharedPreferences saveKey = getSharedPreferences("AccessKey", MODE_PRIVATE);
            String key = saveKey.getString("Key", null);
            if (key == null || key.isEmpty()) {
                Toast.makeText(MainActivity.this, "Access denied: Set your Wakeword Key first", Toast.LENGTH_SHORT).show();
                getAccessKey();
                return;
            }

            if (isServiceisRunning(MyForegroundServices.class)) {
                Toast.makeText(this, "Jarvis already running", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isInternetAvailable(MainActivity.this)) {
                showNoInternetDialog();
                return;
            }
            // START Jarvis
            progressDialog = ProgressDialog.show(MainActivity.this, "Activating Jarvis", "Please be patient");
            WakeUpJarvis();
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                    prefs.edit().remove("UserId").remove("ProfileImageUri").apply();

                    SharedPreferences appPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    appPrefs.edit().putBoolean("isProfileUpdated", false).apply();

                    Intent logoutIntent = new Intent(this, MainActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void handleQuery(String text) {
        jarvisEngine.ask(text, new JarvisCallback() {
            @Override
            public void onResponse(String response) {
                addChatMessage(response, true);
            }

            @Override
            public void onError(String error) {
                addChatMessage("System Error: " + error, true);
            }
        });
    }






    @Override
    protected void onResume() {
        super.onResume();
        updateProfileBanner();
        if (particleBackground != null) particleBackground.startAnimation();
    }

    private void updateProfileBanner() {
        TextView txtMenuName = findViewById(R.id.txtMenuName);
        TextView txtMenuUserId = findViewById(R.id.txtMenuUserId);
        TextView txtMenuMobile = findViewById(R.id.txtMenuMobile);

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        String name = prefs.getString("UserName", FetchUser.getInstance().getName());
        String mobile = prefs.getString("Mobile", FetchUser.getInstance().getMobile());
        String systemId = prefs.getString("UserId", FetchUser.getInstance().getUserId());

        if (txtMenuName != null) {
            txtMenuName.setText("Username: " + name);
            txtMenuUserId.setText("User ID: " + systemId);
            txtMenuMobile.setText("Mobile: " + mobile);
        }

        String savedUri = prefs.getString("ProfileImageUri", null);
        if (savedUri != null) {
            Uri profileUri = Uri.parse(savedUri);
            if (imgProfileMenu != null) {
                try {
                    imgProfileMenu.setImageURI(profileUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (imgMainProfile != null) {
                try {
                    imgMainProfile.setImageURI(profileUri);
                    imgMainProfile.setImageTintList(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void chechProfile() {
        SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isProfileUpdated = preferences.getBoolean("isProfileUpdated", false);

        if (!isProfileUpdated) {
            updateprofile();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isProfileUpdated", true);
            editor.apply();
        }
    }

    private void updateprofile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Update Your Profile")
                .setMessage("Update your profile for jarvis to know who you are!")
                .setCancelable(false)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent2 = new Intent(getApplicationContext(), Profile.class);
                        startActivity(intent2);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Removed onCreatePanelMenu and onOptionsItemSelected as they are now handled by Profile Menu

    private void getAccessKey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_access_key, null);
        builder.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.etDialogKey);
        
        SharedPreferences saveKey = getSharedPreferences("AccessKey", MODE_PRIVATE);
        String currentKey = saveKey.getString("Key", "");
        input.setText(currentKey);

        AlertDialog alertDialog = builder.create();

        dialogView.findViewById(R.id.btnDialogSave).setOnClickListener(v -> {
            if (isServiceisRunning(MyForegroundServices.class)) {
                Toast.makeText(MainActivity.this, "Deactivate Jarvis and change your key", Toast.LENGTH_SHORT).show();
                return;
            }
            String userAccessKey = input.getText().toString();
            SharedPreferences.Editor editor = saveKey.edit();
            editor.putString("Key", userAccessKey);
            editor.apply();
            Toast.makeText(MainActivity.this, "Key saved, Activate Jarvis to check your access key is valid", Toast.LENGTH_SHORT).show();
            Log.d("Access key", userAccessKey);
            alertDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void WakeUpJarvis() {

        biometricPrompt=new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                wakeup = true;
                toSpeech.speak("Jarvis activated!", TextToSpeech.QUEUE_FLUSH, null, "ACTIVATING");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                },5000);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this, "Activation failed", Toast.LENGTH_SHORT).show();
            }
        });
        promptInfo=new BiometricPrompt.PromptInfo.Builder().setTitle("Jarvis Security")
                .setDescription("Place your fingerprint or use password to activate 'Jarvis.'")
                .setDeviceCredentialAllowed(true)
                .build();

        biometricPrompt.authenticate(promptInfo);

    }

    protected void onStart() {
        super.onStart();

    }
    private boolean isServiceisRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service1 : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service1.service.getClassName())) {
                    return true;
                }
            }

        }
        return false;
    }


    private void requestNextPermission() {

        if (currentPermissionIndex < permissions.length) {
            String permission = permissions[currentPermissionIndex];
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 101);
            } else {
                currentPermissionIndex++;
                requestNextPermission();
            }
        } else {

            Toast.makeText(this, "All permissions granted or handled", Toast.LENGTH_SHORT).show();
        }
    }
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] requestedPermissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, requestedPermissions, grantResults);

        if (requestCode == 101) {
            // Move to next permission whether granted or denied
            currentPermissionIndex++;
            requestNextPermission();
        }
    }
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    private void showNoInternetDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false); // back press block

        builder.setTitle("No Internet Connection");
        builder.setMessage(
                "Please turn on the internet to use Jarvis.\n\n" +
                        "Note: Jarvis uses Google voice recognition, so an internet connection is required only to record and process your voice."
        );


        builder.setPositiveButton("Turn On Internet", (dialog, which) -> {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Exit", (dialog, which) -> {
            dialog.dismiss();
            finish(); // app exit
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void animateWakeButton() {
        if (wakeJarvis != null) {
            android.view.animation.Animation wave = new android.view.animation.ScaleAnimation(
                    1.0f, 1.15f, 1.0f, 1.15f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
            wave.setDuration(1500);
            wave.setRepeatCount(android.view.animation.Animation.INFINITE);
            wave.setRepeatMode(android.view.animation.Animation.REVERSE);
            wave.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            wakeJarvis.startAnimation(wave);
        }
    }

    // Removed showProfileMenu as it is handled via DrawerLayout now


    public void addChatMessage(String message, boolean isJarvis) {
        runOnUiThread(() -> {
            if (chatContainer == null) return;

            View chatView;
            if (isJarvis) {
                chatView = getLayoutInflater().inflate(R.layout.item_chat_jarvis, chatContainer, false);
                TextView txtMessage = chatView.findViewById(R.id.txtJarvisMessage);
                txtMessage.setText(message);
            } else {
                chatView = getLayoutInflater().inflate(R.layout.item_chat_user, chatContainer, false);
                TextView txtMessage = chatView.findViewById(R.id.txtUserMessage);
                txtMessage.setText(message);
            }

            chatContainer.addView(chatView);

            // Auto-scroll to bottom
            chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (particleBackground != null) particleBackground.stopAnimation();
    }
}
