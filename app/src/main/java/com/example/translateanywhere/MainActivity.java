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

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import static com.example.translateanywhere.R.menu.menu_main;

import android.speech.tts.UtteranceProgressListener;

import android.telecom.TelecomManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;





import com.google.firebase.FirebaseApp;


import java.util.Locale;
import java.util.concurrent.Executor;



public class MainActivity extends AppCompatActivity {
    Intent  intent1;
    TextToSpeech toSpeech;
    TextView textView, Riddle, txttask;
    Button wakeJarvis;
    private boolean isTextToSpeechInitialized = false;

    Boolean test;

    ProgressDialog progressDialog;
    Toolbar toolbar1;
    TranslationHelper translationHelper;
    SpeechRecognizer speechRecognizer;
    Boolean wakeup = false;

    int currentPermissionIndex = 0;
    ConstraintLayout constraintLayout;
    ScrollView scrollView, scrollView1;
    BiometricPrompt biometricPrompt;
    BiometricPrompt.PromptInfo promptInfo;
    Executor executor;


    private static final int PERMISSION_REQUEST_CODE = 101;


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
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };





    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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

        if (UserId == null) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && wakeup) {
                    startForegroundService(intent1);
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
        textView = findViewById(R.id.textview);
        setSupportActionBar(toolbar1);
        Riddle = findViewById(R.id.txtRiddle1);
        scrollView = findViewById(R.id.scrollView2);
        constraintLayout = findViewById(R.id.Task);
        txttask = findViewById(R.id.txttask);
        scrollView1 = findViewById(R.id.scrollView3);
        wakeJarvis=findViewById(R.id.btnWakeJarvis);



        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        BiometricManager biometricManager=BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()){
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    Toast.makeText(this, "Error 7", Toast.LENGTH_SHORT).show();
                    break;


                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    Toast.makeText(this, "Error 6", Toast.LENGTH_SHORT).show();
                    break;

                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    Toast.makeText(this, "Error 8", Toast.LENGTH_SHORT).show();
                    break;
        }
        executor=ContextCompat.getMainExecutor(this);



        Riddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isServiceisRunning(MyForegroundServices.class)) {
                    MyForegroundServices.riddleLiveData.observe(MainActivity.this, new Observer<String>() {
                        @Override
                        public void onChanged(String riddle) {
                            if (riddle.contains("73")) {
                                Riddle.setText("The daily riddle is completed come back tomorrow");
                            }   else {
                                Riddle.setText(riddle);
                                Riddle.setEnabled(false);
                                toSpeech.speak("Your Riddle Is " + riddle, TextToSpeech.QUEUE_FLUSH, null, null);
                            }

                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Activate Jarvis First ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        wakeJarvis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog=ProgressDialog.show(MainActivity.this,"Activating Jarvis","Please be patient");
                if (isServiceisRunning(MyForegroundServices.class)) {
                    Toast.makeText(MainActivity.this, "Jarvis already running", Toast.LENGTH_SHORT).show();
                    return;
                }
                WakeUpJarvis();
            }
        });
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
                .setMessage("Update your profile for jarvis to know who are you!")
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

    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.EnterKey) {

                getAccessKey();

        }
        if(id==R.id.help){
            if(isServiceisRunning(MyForegroundServices.class)){
                Toast.makeText(this, "Deactivate Jarvis to paste key", Toast.LENGTH_SHORT).show();

            }else {
                getAiApiKeys();
            }
        }

        if(id==R.id.contact){
            String url="https://steve-dev-l7.github.io/Jarvis_support/";

            Intent intent2=new Intent(Intent.ACTION_VIEW);
            intent2. setData(Uri.parse(url));
            startActivity(intent2);
        }
        return true;
    }

    private void getAccessKey() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Paste Your AccessKey");
        final EditText input = new EditText(getApplicationContext());
        input.setHint("Type here...");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(50, 40, 50, 40);

        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isServiceisRunning(MyForegroundServices.class)) {
                    Toast.makeText(MainActivity.this, "Deactivate Jarvis and change your key", Toast.LENGTH_SHORT).show();
                    return;
                }
                String userAccessKey = input.getText().toString();
                SharedPreferences saveKey=getSharedPreferences("AccessKey",MODE_PRIVATE);
                SharedPreferences.Editor editor=saveKey.edit();
                editor.putString("Key",userAccessKey);
                editor.apply();
                Toast.makeText(MainActivity.this, "Key saved, Activate Jarvis and check your access key is valid", Toast.LENGTH_SHORT).show();
                Log.d("Access key",userAccessKey);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    private void getAiApiKeys(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter AI Keys");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);


        final EditText input = new EditText(this);
        input.setHint("Enter first key");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input);


        final EditText input1 = new EditText(this);
        input1.setHint("Enter second key");
        input1.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input1);





        builder.setView(layout);


        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key1 = input.getText().toString();
                String key2= input1.getText().toString();
                SharedPreferences saveKey=getSharedPreferences("AccessKeys",MODE_PRIVATE);
                SharedPreferences.Editor editor=saveKey.edit();
                editor.putString("Key1",key1);
                editor.putString("Key2",key2);


                editor.apply();
                Toast.makeText(MainActivity.this, "Key saved, Activate Jarvis and check your access key is valid", Toast.LENGTH_SHORT).show();


            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        builder.show();
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
                toSpeech.speak("Jarvis activated", TextToSpeech.QUEUE_FLUSH, null, "INTRODUCING");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                },2000);

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                progressDialog.dismiss();
            }
        });
        promptInfo=new BiometricPrompt.PromptInfo.Builder().setTitle("Jarvis Security")
                .setDescription("Place your fingerprint or use password to activate 'Jarvis'")
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
}
