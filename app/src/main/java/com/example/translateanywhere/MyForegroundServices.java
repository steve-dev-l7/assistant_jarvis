package com.example.translateanywhere;


import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;


public class MyForegroundServices extends Service {
    private PorcupineManager porcupineManager;
    SpeechRecognizer speechRecognizer;
    TextToSpeech toSpeech;
    String recodedtext;
    Boolean calling = false;
    TranslationHelper translationHelper;
    Boolean jarvisActivated,deactivation=false,isShuttingDown=false;
    ComponentName componentName;
    PackageManager pm;
    Boolean TTS = false,nullMessage=false;
    String callTo = null;

    boolean askRiddle=true;
    String task;
    private ObjectAnimator pulseAnimator;


    static MyForegroundServices instance;

    int audioSessionId;
    String Name, Age, DOB, date, Location, MobileNo;

    TextView textView;
    Notification notification1;
    Boolean Reminder = false;

    public DynamicIslandManager dynamicIslandManager;

    String reminderResponse;
    private Handler handler;


    AudioManager audioManager;

    LottieAnimationView jarvisSpeaking;
    FirebaseFirestore db;

    public WindowManager windowManager;

    Map<String, String> codeMap = new HashMap<>();

    public static MutableLiveData<String> riddleLiveData;
    View overlayView;
    Boolean nullCallerName=false;

    DatabaseReference databaseReference;

    String WakeWordAccessKey;
    private static final String CHANNEL_ID = "JarvisServiceChannel";
    TelecomManager telecomManager;
    TelephonyManager telephonyManager;
    CallListener callListener;

    Context context;

    Random random;

    IntentExtractor intentExtractor;

    String recognizeLanguage="en-IN";

    JarvisEngine jarvisEngine;

    String[] jarvisSounds = {
            "Uhh Huh?",
            "Yes?",
            "Yep?",
            "Yea?",
            "Hmm?",
            "senpai?",
            "Yeah?",
            "Uhh ha?",
            "yes sir?",
            "Ahh?"
    };

    NotificationReader notificationReader;




    @SuppressLint({"ServiceCast", "SecretInSource"})
    @Override
    public void onCreate() {
        super.onCreate();

        // ===== CORE FLAGS & CONTEXT =====
        initBaseState();

        // ===== HELPERS & DATE =====
        initHelpersAndDate();

        // ===== SPEECH & AUDIO =====
        initSpeechAndAudio();

        // ===== DATABASE =====
        initDatabaseLayer();

        // ===== JARVIS AI =====
        initJarvisCore();

        // ===== TELEPHONY & CALLS =====
        initTelephonyLayer();

        // ===== IR / REMOTE =====
        initIRCodes();

        // ===== OVERLAY UI =====
        initOverlayUI();

        // ===== TTS LISTENERS =====
        initTTSListeners();

        // ===== WAKE WORD & KEYS =====
        initKeysAndWakeWord();

        // ===== TEXT PROCESSOR =====
        initTextProcessor();

        random = new Random();

        instance=this;

    }

    private void initTextProcessor() {
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && "ACTION_PROCESS_TEXT".equals(intent.getAction())) {
                    String text = intent.getStringExtra("text");
                    if (text != null && !text.isEmpty()) {
                        recodedtext = text;
                        Log.d("TextProcessor", "Received text: " + text);

                        if (nullCallerName) {
                            nullCallerName = false;
                            callTo = getMobileNumber(recodedtext);
                            CallAnyone(recodedtext);
                        } else if (nullMessage) {
                            nullMessage = false;
                            sendsms(callTo, recodedtext, recodedtext);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intentExtractor.extract(recodedtext, new IntentExtractor.ExtractorCallback() {
                                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                                    @Override
                                    public void onResult(String intent, String target, String message, String time) {
                                        processCommand(intent, target, message, time);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }, new IntentFilter("ACTION_PROCESS_TEXT"));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1️⃣ Create persistent foreground notification
        CreateNotification();

        // 2️⃣ Build Porcupine wake word manager
        PorcupineManager.Builder builder = new PorcupineManager.Builder()
                .setAccessKey(WakeWordAccessKey)
                        .setKeywordPath("jarvis.ppn")
                                .setSensitivity(0.8f);

        Log.d("Picovoice", "Porcupine builder created");

        // 3️⃣ Handle Bluetooth headset audio routing
        if (isBluetoothHeadsetConnected()) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            Log.d("Bluetooth", "Headset connected - SCO started");
        } else {
            if (audioManager.isBluetoothScoOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
                Log.d("Bluetooth", "SCO stopped");
            }
        }

        // 4️⃣ Pick random reply for TTS

        try {
            // 5️⃣ Initialize Porcupine wake word detection
            porcupineManager = builder.build(this, keywordIndex -> {
                Log.d("Porcupine", "Wake word index: " + keywordIndex + " | TTS=" + TTS);


                if (TTS) return;

                try {
                    porcupineManager.stop();


                    if (isPhoneLocked(context)) {
                        wakeScreen();
                    } else {
                        animation();
                    }
                    if (keywordIndex == 0) {
                        String jarvisSound = jarvisSounds[random.nextInt(jarvisSounds.length)];
                        toSpeech.speak(jarvisSound, TextToSpeech.QUEUE_FLUSH, null, null);
                        new Handler().postDelayed(() -> {
                            try {
                                speechRecoder("en-IN");
                            } catch (Exception ex) {
                                Log.e("SpeechRecoder", "Error starting recognition: " + ex.getMessage());
                            }
                        }, 300);
                    }


                } catch (PorcupineException e) {
                    Log.e("Porcupine", "Error handling wake word: " + e.getMessage());
                }
            });


            porcupineManager.start();
            Log.d("Porcupine", "Listening started");

        } catch (PorcupineInvalidArgumentException e) {
            speakAndLog("Invalid access key. Try another.", e);
        } catch (PorcupineActivationLimitException e) {
            speakAndLog("Your access key reached its device limit.", e);
        } catch (PorcupineActivationRefusedException e) {
            speakAndLog("Your access key has been refused.", e);
        } catch (PorcupineActivationThrottledException e) {
            speakAndLog("Access key has been throttled.", e);
        } catch (PorcupineException e) {
            speakAndLog("Failed to initialize Porcupine: " + e.getMessage(), e);
        }


        new Handler().postDelayed(() -> startForeground(1001, notification1), 100);

        return START_NOT_STICKY;
    }

    public void speakAndLog(String message, Exception e) {
        Log.e("Porcupine", message, e);
        if(toSpeech!=null){
            toSpeech.speak(message,TextToSpeech.QUEUE_FLUSH,null,"ONGOING");
            char[] res = message.toCharArray();
            StringBuilder adder = new StringBuilder();

            Handler handler = new Handler(Looper.getMainLooper());

            for (int i = 0; i < res.length; i++) {
                int index = i;
                handler.postDelayed(() -> {
                    adder.append(res[index]);
                    textView.setText(adder.toString());
                    sendLiveWord(adder.toString());
                }, index * 40); // typing speed
            }
        }
    }




    private void wakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "Jarvis:WakeLock"
        );

        wl.acquire(3000);

        Log.d("JarvisService", "Trying to start GlowActivity");

       Intent intent = new Intent(getApplicationContext(), GlowActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        try {
            getApplicationContext().startActivity(intent);
            Log.d("JarvisService", "GlowActivity launched");
        } catch (Exception e) {
            Log.e("JarvisService", "Failed to start activity: " + e.getMessage());
        }
    }

    public boolean isPhoneLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        boolean isScreenOn;
        isScreenOn = powerManager.isInteractive();

        boolean isKeyguardLocked = keyguardManager.isKeyguardLocked();

        return !isScreenOn || isKeyguardLocked;
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void speechRecoder(String lang) {

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);


        if (isBluetoothHeadsetConnected()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                audioManager.startBluetoothSco();
                Log.d("Bluetooth", "Bluetooth headset connected, using headset mic.");
            } else {
                Log.e("Bluetooth", "Missing BLUETOOTH_CONNECT permission!");
            }
        } else {
            if (audioManager.isBluetoothScoOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                dynamicIslandManager.updateState("LISTEN");
                Log.d("SpeechRecognizer", "Ready for speech...");

            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Listening...");
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onEndOfSpeech() {

                dynamicIslandManager.updateState("PROCESS");
            }

            @Override
            public void onError(int error) {
                dynamicIslandManager.updateState("IDLE");
                try {
                    porcupineManager.start();
                } catch (PorcupineException e) {
                    throw new RuntimeException(e);
                }
                nullCallerName=false;
                nullMessage=false;
                sendFinishSignal();
                removeListeningOverlay();

            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    recodedtext = matches.get(0);
                    Log.d("SpeechRecognizer", "Recognized: " + recodedtext);
                    sendLiveWord(recodedtext);
                    if(nullCallerName){
                        nullCallerName=false;
                        callTo=getMobileNumber(recodedtext);
                        CallAnyone(recodedtext);
                    } else if (nullMessage) {
                        nullMessage=false;
                        sendsms(callTo,recodedtext,recodedtext);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            intentExtractor.extract(recodedtext, new IntentExtractor.ExtractorCallback() {
                                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                                @Override
                                public void onResult(String intent, String target, String message, String time) {
                                    processCommand(intent,target,message,time);
                                }
                            });

                        }
                    }
                    if(!isPhoneLocked(context)) {
                        startPulse(recodedtext);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> partialResults = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialResults != null && !partialResults.isEmpty()) {
                    String word = partialResults.get(0);
                    sendLiveWord(word);
                    if (!isPhoneLocked(context)) {
                        startPulse(word);
                    }
                    Log.d("SpeechRecognizer", "Partial result: " + partialResults.get(0));
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void sendLiveWord(String word) {
        Intent intent = new Intent("ACTION_LIVE_WORD");
        intent.putExtra("word", word);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendFinishSignal() {
        Intent intent = new Intent("ACTION_FINISH_ACTIVITY");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }









    @SuppressLint("SetTextI18n")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processCommand(String intent, String target, String Message, String Time) {
        dynamicIslandManager.updateState("PROCESS");

        // ---------- Normalize ----------
        if (intent == null) intent = "UNKNOWN";
        intent = intent.trim().toUpperCase();

        task = Message; // 🔥 task = message mapping (important fix)

        Log.d("ProcessIntent", "Intent=" + intent +
                ", Target=" + target +
                ", Task=" + task +
                ", Time=" + Time);

        // ---------- EMERGENCY (Checks 'recodedtext', so it stays outside switch) ----------


        // ---------- ALL INTENTS HANDLED IN A CLEAN SWITCH ----------
        switch (intent) {

            case "CALL":
                handleCall(target);
                break;

            case "MESSAGE":
                handleMessage(target, task);
                return;

            case "DEACTIVATE":
                deactivation = true;
                shutdown();
                return;

            case "COPY_NUMBER":
                copyNumber(target);
                return;

            // Multiple cases calling the same method
            case "PLAY_MUSIC":
            case "STOP_MUSIC":
                controlMusicWithSpeech(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                return;

            case "NEXT_MUSIC":
                controlMusicWithSpeech(KeyEvent.KEYCODE_MEDIA_NEXT);
                return;

            case "PREVIOUS_MUSIC":
                controlMusicWithSpeech(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                return;

            case "TRANSLATE":
                handleTranslate();
                return;

            case "REMINDER":
                handleReminder(Time);
                return;

            case "SAVE":
                saveNumber(target, task);
                return;

            case "SHARE_CONTACT":
                ShareContact(task, target);
                return;

            case "INSTALL":
                toSpeech.speak(
                        "Got it, I’ll open the Play Store right away. Just hit Install.",
                        TextToSpeech.QUEUE_FLUSH, null, "INSTALL"
                );
                openPlayStore(task);
                return;

            case "ENABLE_AUTO_REPLY":

                dynamicIslandManager.showCustomMessage("Auto reply enabled");
                NotificationReader.isAutoReplyEnabled = true;
                speakAndLog("I will reply to all messages", null);
                return; // 🔥 Added missing return statement

            case "DISABLE_AUTO_REPLY":
                dynamicIslandManager.showCustomMessage("Auto reply disabled");
                NotificationReader.isAutoReplyEnabled = false;
                speakAndLog("Auto reply is disabled", null);
                return; // 🔥 Added missing return statement

            case "OPEN":
                openKnownApp(target);
                return;
            case "CLEAR_MEMORY":
                jarvisEngine.clearPermanentMemory();
                speakAndLog("Done !",null);
                return;
            case "TURN_ON_WIFI":
                toggleWiFi(true);
                return;
            case "TURN_OFF_WIFI":
                toggleWiFi(false);
                return;
            case "TURN_ON_HOTSPOT":
                toggleHotspot(true);
                return;
            case "TURN_OFF_HOTSPOT":
                toggleHotspot(false);
                return;
            case "TURN_ON_FLASHLIGHT":
                toggleFlashlight(true);
                return;
            case "TURN_OFF_FLASHLIGHT":
                toggleFlashlight(false);
                return;
            case "TURN_ON_DATA":
                toggleMobileData(true);
                return;
            case "TURN_OFF_DATA":
                toggleMobileData(false);
                return;

            case "TURN_ON":
            case "TURN_OFF":
                turnOnBlueTooth();
                return;



            case "UNKNOWN":
            default:
                // ---------- FALLBACK (AI) ----------
                handleUnknown();
        }
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void handleCall(String target) {
        if (target == null || target.isEmpty()) {
            askCallerName();
            return;
        }


        if(target.matches("^[0-9 ]+$")){
            callTo = target.replaceAll("\\s+", "");  // remove spaces before saving
            CallAnyone(callTo);
            return;
        }
        callTo = getMobileNumber(target);
        CallAnyone(target);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void handleMessage(String target, String message) {
        callTo = getMobileNumber(target);
        if(callTo==null) {speakAndLog("Caller number is not found",null);return;}
        Log.d("CallNumber da",callTo);
        if (message == null || message.isEmpty()) {
            askMessageContent();
            return;
        }
        sendsms(callTo, message,target);
    }

    private void controlMusicWithSpeech(int keyCode) {
        toSpeech.speak("roger", TextToSpeech.QUEUE_FLUSH, null, "SONG");
        controlMusic(keyCode);
    }

    private void openKnownApp(String target) {

        if (target == null) return;

        switch (target.toLowerCase()) {
            case "youtube":
                openApplication("com.google.android.youtube", true); break;
            case "instagram":
                openApplication("com.instagram.android", true); break;
            case "whatsapp":
                openApplication("com.whatsapp", true); break;
            case "facebook":
                openApplication("com.facebook.katana", true); break;
            case "snapchat":
                openApplication("com.snapchat.android", true); break;
            case "telegram":
                openApplication("org.telegram.messenger", true); break;
            case "spotify":
                openApplication("com.spotify.music", true); break;
            case "netflix":
                openApplication("com.netflix.mediaclient", true); break;
            case "chrome":
                openApplication("com.android.chrome", true); break;
            default:
                openApplication(target, false);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void askCallerName() {

        speechRecognizer.cancel();
        nullCallerName = true;

        textView.setText("Call to who? Tell the name");
        toSpeech.speak(
                "Call to who? Tell the name",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ASK_CALLER"
        );

        try {
            porcupineManager.stop();
        } catch (PorcupineException e) {
            Log.d("Porcupine Error", Objects.requireNonNull(e.getMessage()));
        }

        new Handler().postDelayed(() -> {
            speechRecoder(recognizeLanguage);
        }, 2000);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void askMessageContent() {

        new Handler(Looper.getMainLooper()).post(() -> {

            try {
                porcupineManager.stop();
                speechRecognizer.cancel();
            } catch (PorcupineException e) {
                e.printStackTrace();
            }

            textView.setText("What would you like to say?");

            toSpeech.speak(
                    "What would you like to say?",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ASK_MESSAGE"
            );

            nullMessage = true;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                speechRecoder(recognizeLanguage);
            }, 2000);

        });
    }

    private void handleTranslate() {

        translateText(recodedtext, new TranslationHelper.TranslationCallback() {
            @Override
            public void onTranslationSuccess(String translatedText) {
                speakAndLog(translatedText, null);
            }

            @Override
            public void onTranslationFailure(Exception e) {
                speakAndLog(
                        e != null ? e.getMessage() : "Translation failed",
                        null
                );
            }
        });
    }





    private void handleReminder(String time) {
        Reminder = true;
        getOnDeviceResponse(recodedtext);
        toSpeech.speak("Roger", TextToSpeech.QUEUE_FLUSH, null, "REMINDER");

        if (time.contains("minute") || time.contains("hour")) {
            startMinuteChecker(time);
        } else {
            String extracted = extractForReminder(recodedtext);
            startRemindChecker(extracted);
        }
    }

    private void handleUnknown() {
        changeJarvisEmotion(recodedtext);
        getOnDeviceResponse(recodedtext);
    }


    private void startMinuteChecker(String reminderMinute) {
        int minute;
        String numberStr = reminderMinute.replaceAll("[^0-9]", "");
        Handler handler1 = new Handler(android.os.Looper.getMainLooper());
        final Runnable[] checkerRunnableHolder = new Runnable[1];

        if (!numberStr.isEmpty()) {
            minute = Integer.parseInt(numberStr);

            Calendar initialCalendar = Calendar.getInstance();
            int targetHour = initialCalendar.get(Calendar.HOUR_OF_DAY);
            int targetMinute = initialCalendar.get(Calendar.MINUTE);

            if (reminderMinute.contains("hour")) {
                targetHour = (targetHour + minute) % 24;
            } else {
                targetMinute += minute;
                if (targetMinute >= 60) {
                    targetHour = (targetHour + (targetMinute / 60)) % 24;
                    targetMinute = targetMinute % 60;
                }
            }

            int finalTargetHour = targetHour;
            int finalTargetMinute = targetMinute;

            // 🔴 NEW: Calculate and format the target time for Dynamic Island
            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.set(Calendar.HOUR_OF_DAY, finalTargetHour);
            targetCalendar.set(Calendar.MINUTE, finalTargetMinute);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            String formattedTime = sdf.format(targetCalendar.getTime());

            // 🔴 NEW: Show confirmation on Dynamic Island
            if (dynamicIslandManager != null) {
                dynamicIslandManager.showCustomMessage("⏰ Reminder set for " + formattedTime);
            }

            checkerRunnableHolder[0] = new Runnable() {
                @Override
                public void run() {
                    Calendar now = Calendar.getInstance();
                    int cHour = now.get(Calendar.HOUR_OF_DAY);
                    int cMinute = now.get(Calendar.MINUTE);

                    if (cHour > finalTargetHour || (cHour == finalTargetHour && cMinute >= finalTargetMinute)) {

                        // 🔴 OPTIONAL: Island-la reminder ring aagurathaiyum kaatalaam
                        if (dynamicIslandManager != null) {
                            dynamicIslandManager.showCustomMessage("🔔 Reminder Ringing!");
                        }

                        toSpeech.speak(reminderResponse, TextToSpeech.QUEUE_FLUSH, null, "REMINDER");
                        handler1.removeCallbacks(checkerRunnableHolder[0]);
                    } else {
                        handler1.postDelayed(this, 10 * 1000);
                        Log.d("Not match", String.valueOf(cMinute));
                    }
                }
            };

            handler1.post(checkerRunnableHolder[0]);
        }
    }

    private void copyNumber(String name){
        ClipboardManager clipboardManager=(ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String number=getMobileNumber(name);

        if(number!=null && !number.equals("null")) {
            ClipData clipData = ClipData.newPlainText("label", number);
            clipboardManager.setPrimaryClip(clipData);
            speakAndLog("Done",null);
        }else {

            toSpeech.speak("Ahem...., I didn't hear that, can you come again.", TextToSpeech.QUEUE_FLUSH, null, "CopyNumber");
        }

    }

    @SuppressLint({"SetTextI18n", "QueryPermissionsNeeded"})
    private void openApplication(String appName, Boolean isDirectPackage) {


        PackageManager pm = getPackageManager();
        String packageName;
        if(!isDirectPackage){
            packageName=getPackageNameByAppName(context,appName);
        }else {
            packageName=appName;
        }

        try {

            if (packageName != null) {
                pm.getPackageInfo(packageName, 0);


                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    textView.setText("Opening app...");
                    toSpeech.speak("Roger", TextToSpeech.QUEUE_FLUSH, null, "OpeningApplication");
                    startActivity(launchIntent);
                } else {
                    textView.setText("App installed but cannot be launched");
                    toSpeech.speak("App is installed but has no launcher", TextToSpeech.QUEUE_FLUSH, null, "OpeningApplication");
                    Log.e("Jarvis", "App installed but has no launchable intent: " + packageName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {

            Log.e("Jarvis", "App not installed: " + packageName);
            textView.setText("App not found. Redirecting to Play Store...");
            toSpeech.speak("App not found. Redirecting to Play Store", TextToSpeech.QUEUE_FLUSH, null, "OpeningApplication");

            openPlayStore(appName);
        }
    }

    private void openPlayStore(String appName){
        try {
            Intent downloadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + appName + "&c=apps"));
            downloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            downloadIntent.setPackage("com.android.vending");
            startActivity(downloadIntent);
        }catch (ActivityNotFoundException e){
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=" + appName + "&c=apps"));
            startActivity(intent);
        }


    }

    @SuppressLint("QueryPermissionsNeeded")
    private String getPackageNameByAppName(Context context, String appName) {
        PackageManager pm = context.getPackageManager();


        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = pm.queryIntentActivities(mainIntent, 0);

        String cleanedAppName = normalizeAppName(appName);

        for (ResolveInfo info : launchables) {
            String label = info.loadLabel(pm).toString();
            String packageName = info.activityInfo.packageName;
            String cleanedLabel = normalizeAppName(label);

            Log.d("AppMatchDebug", "Comparing: '" + cleanedAppName + "' with '" + cleanedLabel + "'");

            if (cleanedLabel.equals(cleanedAppName) ||
                    cleanedLabel.startsWith(cleanedAppName) ||
                    cleanedLabel.contains(cleanedAppName)) {
                Log.d("AppMatch", "Matched: " + label + " => " + packageName);
                return packageName;
            }
        }

        return null;
    }






    private String normalizeAppName(String name) {
        if (name == null) return "";
        return name
                .replaceAll("[\\p{So}\\p{Cn}\\p{C}]", "")
                .toLowerCase()
                .trim();
    }





    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void shutdown() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        Log.d("JarvisService", "Initiating Jarvis Shutdown...");

        // 1. Voice First!
        if (toSpeech != null) {
            toSpeech.speak("Shutting down", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        // 2. Kill Speech Components Immediately
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.stopListening();
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
            } catch (Exception e) {
                Log.e("JarvisService", "Error destroying SpeechRecognizer", e);
            }
            removeListeningOverlay();
            sendFinishSignal();
        });

        instance = null;

        // 3. Safe Receiver Unregister
        if (unlockReceiver != null) {
            try {
                unregisterReceiver(unlockReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("JarvisService", "Receiver already unregistered", e);
            }
            unlockReceiver = null;
        }

        // 4. Null Checks for Managers
        if (notificationReader != null) {
            notificationReader.destroy();
            notificationReader = null;
        }

        if (dynamicIslandManager != null) {
            dynamicIslandManager.destroy();
            dynamicIslandManager = null;
        }

        if (jarvisEngine != null) {
            jarvisEngine.close();
            jarvisEngine = null;
        }

        jarvisSpeaking = null;

        // 5. Safe Porcupine Shutdown
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (Exception e) {
                Log.e("JarvisService", "Porcupine Exception during shutdown", e);
            } finally {
                porcupineManager = null;
            }
        }

        // 6. Stop the service
        stopForeground(true);
        stopSelf();
    }


    @SuppressLint("SetTextI18n")
    private void CallAnyone(String name) {
        name=name.trim();

        if(callTo==null){
            speakAndLog("I didn't see, "+name+" in your contact!.", null);

        }else{
            calling = true;
            speakAndLog("Calling "+name,null);
            dynamicIslandManager.showCustomMessage("Calling " +name);
            Log.d("Number", callTo);
        }


    }

    @SuppressLint({"Range", "SetTextI18n"})
    private String getMobileNumber(String contactName) {
        ContentResolver contentResolver = getContentResolver();
        String cleanedInputName = removeEmojis(contactName).trim();

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                if (name == null) continue;

                String cleanedContactName = removeEmojis(name).trim();

                if (cleanedContactName.equalsIgnoreCase(cleanedInputName)) {
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                            new String[]{contactId},
                            null
                    );

                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            int type = phoneCursor.getInt(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                                String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                phoneCursor.close();
                                cursor.close();
                                return number;
                            }
                        }
                        phoneCursor.close();
                    }
                }

            } while (cursor.moveToNext());
            cursor.close();
        }
        return null;
    }




    private String removeEmojis(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
    }


//---------------------- OnDevice Jarvis Response -----------------------------

    private void getOnDeviceResponse(String Query){
        if (dynamicIslandManager != null) {
            dynamicIslandManager.updateState("THINK");
        }
        jarvisEngine.ask(Query, new JarvisCallback() {
            @Override
            public void onResponse(String response) {
                if(Reminder) {
                    reminderResponse=response;
                    return;
                }
                AlterString(response);
                if (dynamicIslandManager != null) {
                    dynamicIslandManager.updateState("IDLE");
                }
            }

            @Override
            public void onError(String error) {
                speakAndLog(error,null);
                if (dynamicIslandManager != null) {
                    dynamicIslandManager.updateState("IDLE");
                }
            }
        });
    }




    private void translateText(String text, TranslationHelper.TranslationCallback callback) {
        TranslationHelper helper = new TranslationHelper();

        helper.downloadModel(this, "en", "ta", new TranslationHelper.TranslationCallback() {
            @Override
            public void onTranslationSuccess(String ignored) {
                helper.translateText(getApplicationContext(), text, new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslationSuccess(String translatedText) {
                        callback.onTranslationSuccess(translatedText);
                    }

                    @Override
                    public void onTranslationFailure(Exception e) {
                        callback.onTranslationFailure(e);
                    }
                });
            }

            @Override
            public void onTranslationFailure(Exception e) {
                callback.onTranslationFailure(e);
            }
        });
    }


    @SuppressLint("SetTextI18n")
    private void sendsms(String phoneno, String message, String name) {
        Log.d("Sms error da",phoneno);
        if (message != null) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneno, null, message, null, null);

                toSpeech.speak("Done.", TextToSpeech.QUEUE_FLUSH, null, "SMS");
            } catch (Exception e) {
                Log.d("SMS error da",e.getMessage());
                toSpeech.speak("Try again", TextToSpeech.QUEUE_FLUSH, null, "FAILED SMS");
            }
        } else {
            toSpeech.speak("Message is empty ", TextToSpeech.QUEUE_FLUSH, null, "Empty Message");
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void turnOnBlueTooth(){

        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter==null){
            speakAndLog("Bluetooth is not supported",null);
        }
        else if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
            speakAndLog("Bluetooth is turned on",null);
        }else {
            bluetoothAdapter.disable();
            speakAndLog("Bluetooth is turned off",null);
        }

    }


    private void FetchUserDetails(Context context) {
        // Verum Context mattum pass pandrom!
        FetchUser.getInstance().fetchUserData(context, new FetchUser.OnUserFetchListener() {
            @Override
            public void onSuccess() {
                Name = FetchUser.getInstance().getName();
                Age = FetchUser.getInstance().getAge();
                DOB = FetchUser.getInstance().getDob();
                Location = FetchUser.getInstance().getLocation();


                Log.d("UserDetails", "Jarvis knows everything about " + Name + " now!");
            }

            @Override
            public void onError(String message) {
                Log.d("UserDetails", "Failed to fetch: " + message);
            }
        });
    }





    private void controlMusic(int key) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, key);
        audioManager.dispatchMediaKeyEvent(event);

        event = new KeyEvent(KeyEvent.ACTION_UP, key);
        audioManager.dispatchMediaKeyEvent(event);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void getAudiosession() {
        int sampleRate = 44100;

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioSessionId = audioRecord.getAudioSessionId();
        Log.d("audiosession: ", String.valueOf(audioSessionId));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.d("Bluetooth", "Bluetooth is OFF or not available");
            return false;
        }

        int connectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        Log.d("Bluetooth", "Connection State: " + connectionState);

        return connectionState == BluetoothProfile.STATE_CONNECTED;
    }

    private void CreateNotification() {

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Jarvis Foreground Service",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Notification channel for Jarvis assistant");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jarvis is Active")
                .setContentText("Say 'Hey Jarvis' to listen")
                .setSmallIcon(R.drawable.ic_jarvis_small_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        notification1 = builder.build();
    }

    private String extractForReminder(String text) {
        String extractedTime = extractTime(text);
        String hour = "";
        String minute = "";
        int digit = 0;
        if (extractedTime == null || extractedTime.isEmpty()) {
            String digitsOnly = text.replaceAll("[^0-9]", "");
            if (digitsOnly.length() == 2) {
                digit = Integer.parseInt(digitsOnly);
                if (digit >= 10) {
                    extractedTime = digitsOnly + ":" + "00";
                } else {
                    hour = digitsOnly.substring(0, 1);
                    minute = digitsOnly.substring(1, 2);
                }
            } else if (digitsOnly.length() == 3) {
                hour = digitsOnly.substring(0, 1);
                minute = digitsOnly.substring(1, 3);
            } else if (digitsOnly.length() == 4) {
                hour = digitsOnly.substring(0, 2);
                minute = digitsOnly.substring(2, 4);
            } else if (digitsOnly.length() == 1) {
                hour = digitsOnly;
                minute = "00";
            }
            if (digit < 10) {
                extractedTime = hour + ":" + minute;
            }
        }
        if (text.contains("p.m.")) {
            return extractedTime + " " + "p.m.";
        }
        return extractedTime + " " + "a.m.";
    }

    public String extractTime(String input) {
        Pattern pattern = Pattern.compile("\\b\\d{1,2}:\\d{1,2}\\b");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    @SuppressLint("DefaultLocale")
    private void startRemindChecker(String extractedTime) {
        handler = new Handler(android.os.Looper.getMainLooper());
        final Runnable[] checkerRunnableHolder = new Runnable[1];
        checkerRunnableHolder[0] = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                int cHour = now.get(Calendar.HOUR);
                int cMinute = now.get(Calendar.MINUTE);
                int amOrPm = now.get(Calendar.AM_PM);
                String current = String.format("%d:%02d %s", cHour == 0 ? 12 : cHour, cMinute, (amOrPm == Calendar.AM ? "a.m." : "p.m."));
                Log.d("Current time", current);
                if (extractedTime.equals(current)) {
                    toSpeech.speak(reminderResponse.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", ""), TextToSpeech.QUEUE_FLUSH, null, "REMINDER");
                    handler.removeCallbacks(checkerRunnableHolder[0]);
                } else {
                    Log.d("Time", "Not Match");
                    handler.postDelayed(this, 10 * 1000);
                }

            }
        };
        handler.post(checkerRunnableHolder[0]);
    }


    private void saveNumber(String name, String Number){

        String phoneNumber=Number.replace(" ","");
        Log.d("Length", phoneNumber);

        if(phoneNumber.length()!=10 || !phoneNumber.matches("\\d+")){
            toSpeech.speak(" Hmm...... i think the number is invalid,.. fix that number dude.  ",TextToSpeech.QUEUE_FLUSH,null,"SAVINGCONTACT");
            showPopup(phoneNumber,name);

        }else {

            ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();

            contentProviderOperations.add(ContentProviderOperation.newInsert(
                            ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .build());


            contentProviderOperations.add(ContentProviderOperation.newInsert(
                            ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());


            contentProviderOperations.add(ContentProviderOperation.newInsert(
                            ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());


            try {

                getContentResolver().applyBatch(ContactsContract.AUTHORITY, contentProviderOperations);
                toSpeech.speak("Done", TextToSpeech.QUEUE_FLUSH, null, "SAVINGCONTACT");

            } catch (Exception e) {
                Log.d("Saving Error", Objects.requireNonNull(e.getMessage()));
                toSpeech.speak("Failed to save contact", TextToSpeech.QUEUE_FLUSH, null, "SAVINGCONTACT");
            }
        }


    }

    private void ShareContact(String contactName, String targetName){

        String contactNumber;

        Log.d("Share Contact",contactName+" "+targetName);

        if(contactName == null || targetName==null){
            toSpeech.speak("I didn't get that , can you come again..?",TextToSpeech.QUEUE_FLUSH,null,"SHARE");
            return;
        }

        if(contactName.contains("my contact number")){
            contactNumber="Here "+Name+"'s Contact Number "+MobileNo;
        }else{
            contactNumber="Here "+contactName+"'s Contact Number "+getMobileNumber(contactName);
        }

        String targetNumber=getMobileNumber(targetName);
        sendsms(targetNumber,contactNumber,targetName);



    }



    View popupView;
    

    private void showPopup(String wrongNumber, String name) {
        // 🔴 FIX 1: Only check popupView. If you check windowManager, it might block showing if another overlay is active.
        if (popupView != null) return;

        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.popup_edit_number, null);

        EditText editPhone = popupView.findViewById(R.id.editPhone);
        EditText editName = popupView.findViewById(R.id.editName);
        Button btnSave = popupView.findViewById(R.id.btnSave);
        Button btnCancel = popupView.findViewById(R.id.btnCancel);

        editPhone.setText(wrongNumber);
        editName.setText(name);

        // 🔴 FIX 2: Proper Flags for EditText in WindowManager
        // If you use FLAG_NOT_FOCUSABLE, the keyboard will NEVER open to type in the EditText.
        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.6f;

        // Add view cleanly in one step
        windowManager.addView(popupView, params);

        btnSave.setOnClickListener(v -> {
            String corrected = editPhone.getText().toString().trim();
            String correctedName = editName.getText().toString().trim();
            saveNumber(correctedName, corrected); // Assuming this exists
            removePopup();
        });

        btnCancel.setOnClickListener(view -> removePopup());
    }

    private void removePopup() {
        if (windowManager != null && popupView != null) {
            try {
                // 🔴 FIX 3: Prevent "View not attached" crash
                if (popupView.isAttachedToWindow()) {
                    windowManager.removeView(popupView);
                }
            } catch (Exception e) {
                Log.e("JarvisService", "Error removing popup", e);
            }
            popupView = null; // Always nullify to free memory
        }
    }



// ...

    private void animation() {
        // 🔴 FIX 7: Force UI updates to run on the Main Thread!
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isShuttingDown) return;
            try {

                if (windowManager == null) {
                    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                }

                overlayView = LayoutInflater.from(this).inflate(R.layout.wave, null);
                textView = overlayView.findViewById(R.id.liveText);
                jarvisSpeaking = overlayView.findViewById(R.id.jarvisSpeakings);

                WindowManager.LayoutParams params = getLayoutParams();

                params.gravity = Gravity.BOTTOM | Gravity.CENTER;

                // Adding view to window
                windowManager.addView(overlayView, params);

                jarvisSpeaking.setVisibility(View.VISIBLE);
                jarvisSpeaking.playAnimation();
                textView.setVisibility(View.VISIBLE);

                Log.d("JarvisService", "✅ Animation Overlay successfully added to screen!");

            } catch (Exception e) {
                // If WindowManager fails (like missing permission), this will print exactly WHY!
                Log.e("JarvisService", "❌ Error showing animation: " + e.getMessage(), e);
            }
        });
    }

    private static WindowManager.LayoutParams getLayoutParams() {
        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR|
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
    }


    private void removeListeningOverlay() {
        // 🔴 FIX: Removed lock check. Always try to remove overlay if it exists.

        // 🔴 THE FIX: Push all UI removal and animation stopping to the Main Thread!
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                stopPulse(); // Stop animations safely on the Main Thread

                if (windowManager != null && overlayView != null) {
                    if (jarvisSpeaking != null) {
                        jarvisSpeaking.cancelAnimation();
                        jarvisSpeaking.setVisibility(View.GONE);
                    }

                    if (overlayView.isAttachedToWindow()) {
                        windowManager.removeView(overlayView);
                    }
                    overlayView = null;
                } else {
                    Log.d("Overlay", "Already NULL");
                }
            } catch (Exception e) {
                Log.e("JarvisService", "Error removing overlay", e);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void startPulse(String words) {
        if (isShuttingDown) return;
        // 🔴 THE NEW FIX: If the view is missing, force create it right now!
        if (jarvisSpeaking == null || overlayView == null) {
            Log.d("JarvisService", "UI not ready yet, forcing overlay creation...");
            animation(); // This will inflate the view and set jarvisSpeaking
        }

        // Safety check just in case animation() failed for some reason
        if (jarvisSpeaking == null) {
            Log.e("JarvisService", "jarvisSpeaking is STILL null. Skipping animation.");
            return;
        }

        if (textView != null) {
            textView.setText("Hey Jarvis > " + words);
        }

        // Don't restart if already running to prevent flicker/memory usage
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            return;
        }

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                jarvisSpeaking,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 1f)
        );
        pulseAnimator.setDuration(600);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.start();
    }

    // 🔴 NEW: Helper method to clean up the pulse
    private void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (jarvisSpeaking != null) {
            jarvisSpeaking.setScaleX(1f);
            jarvisSpeaking.setScaleY(1f);
        }
    }

    // Init functions-----------------------------------------------------------------------

    private void initBaseState() {
        jarvisActivated = true;
        context = getApplicationContext();
    }


    private void initHelpersAndDate() {
        translationHelper = new TranslationHelper();

        date = new SimpleDateFormat(
                "EEEE, MMM d, yyyy",
                Locale.getDefault()
        ).format(new Date());

        pm = context.getPackageManager();
        componentName = new ComponentName(context, NotificationReader.class);
    }

    private void initSpeechAndAudio() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                0
        );

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        getAudiosession();




    }


    private void initDatabaseLayer() {
        db = FirebaseFirestore.getInstance();
        databaseReference = FirebaseDatabase
                .getInstance()
                .getReference("users");

        FetchUserDetails(context);

    }


    private void initJarvisCore() {
        intentExtractor = new IntentExtractor(MyForegroundServices.this);
        riddleLiveData = new MutableLiveData<>();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        dynamicIslandManager = new DynamicIslandManager(this, windowManager);
        dynamicIslandManager.createDynamicIsland();
        jarvisEngine = new JarvisEngine(this);
        jarvisEngine.setForegroundServices(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        registerReceiver(unlockReceiver, filter);
    }


    private void initTelephonyLayer() {

        telecomManager = (TelecomManager)
                getSystemService(Context.TELECOM_SERVICE);

        telephonyManager = (TelephonyManager)
                getSystemService(TELEPHONY_SERVICE);

        callListener = new CallListener(this);
        telephonyManager.listen(
                callListener,
                PhoneStateListener.LISTEN_CALL_STATE
        );
    }


    private void initIRCodes() {
        codeMap.put("volume up","00FF01FE");
        codeMap.put("volume down","00FF817E");
        codeMap.put("channel +","00FF53AC");
        codeMap.put("channel -","00FF619E");
        codeMap.put("btn mute","00FFBB44");
        codeMap.put("btn unmute","00FFBB44");
        codeMap.put("btn on","00FF39C6");
        codeMap.put("btn off","00FF39C6");
        codeMap.put("0","00FFE11E");
        codeMap.put("1","00FF49B6");
        codeMap.put("2","00FFC936");
        codeMap.put("3","00FF33CC");
        codeMap.put("4","00FF718E");
        codeMap.put("5","00FFF10E");
        codeMap.put("6","00FF13EC");
        codeMap.put("7","00FF51AE");
        codeMap.put("8","00FFD12E");
        codeMap.put("9","00FF23DC");
    }


    private void initOverlayUI() {
        if (overlayView == null) {
            overlayView = LayoutInflater
                    .from(this)
                    .inflate(R.layout.wave, null);
        }
        textView = overlayView.findViewById(R.id.liveText);
    }
    private void initTTSListeners() {

        toSpeech = new TextToSpeech(this, i -> {
            if (i == TextToSpeech.SUCCESS) {
                toSpeech.setLanguage(Locale.getDefault());
                toSpeech.setSpeechRate(1.07f);
            }
        });

        toSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(String s) {
                        TTS = true;

                    }

                    @Override
                    public void onDone(String s) {
                        TTS = false;
                        dynamicIslandManager.updateState("IDLE");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            removeListeningOverlay();
                            sendFinishSignal();
                        });
                        if(calling){
                            if (ActivityCompat.checkSelfPermission(MyForegroundServices.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            telecomManager.placeCall(Uri.parse("tel:" + callTo),null);
                            calling=false;
                        }
                        sendFinishSignal();
                        removeListeningOverlay();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                if (porcupineManager != null && !nullCallerName && !nullMessage) {
                                    porcupineManager.start();
                                    Log.d("Porcupine", "Resumed listening after TTS Done");
                                }
                            } catch (PorcupineException e) {
                                Log.e("Porcupine", "Error restarting after TTS: " + e.getMessage());
                            }
                        });

                    }

                    @Override
                    public void onError(String s) {
                        TTS = false;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                if (porcupineManager != null && !nullCallerName && !nullMessage) {
                                    porcupineManager.start();
                                    Log.d("Porcupine", "Resumed listening after TTS Error");
                                }
                            } catch (PorcupineException e) {
                                Log.e("Porcupine", "Error restarting after TTS Error: " + e.getMessage());
                            }
                        });
                    }
                });
    }

    private void initKeysAndWakeWord() {

        getOnDeviceResponse("Give me a riddle without answer");
        SharedPreferences sp =
                getSharedPreferences("AccessKey", MODE_PRIVATE);
        WakeWordAccessKey = sp.getString("Key", null);

    }

    private void AlterString(String result){
        String altered=result;

        altered=altered.replace("\\n","");
        altered=altered.replaceAll("\\*","");
        if(askRiddle){
            askRiddle=false;
            riddleLiveData.postValue(altered);
            return;
        }
        speakAndLog(altered,null);
    }
    private BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("IslandReceiver", "Broadcast Received: " + action);

            // Screen On aanaalum sari, Unlock aanaalum sari ithu work aagum
            if (Intent.ACTION_USER_PRESENT.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {

                Log.d("IslandReceiver", "Checking RAM for messages... hasUnread: " + NotificationReader.hasUnreadMessage);

                if (NotificationReader.hasUnreadMessage) {
                    String displayMsg = "💬 " + NotificationReader.unreadSender + ": " + NotificationReader.unreadMessage;
                    Log.d("IslandReceiver", "Showing Message on Island: " + displayMsg);

                    if (dynamicIslandManager != null) {
                        dynamicIslandManager.showCustomMessage(displayMsg);
                    }

                    // Clear the RAM
                    NotificationReader.hasUnreadMessage = false;
                    NotificationReader.unreadSender = "";
                    NotificationReader.unreadMessage = "";
                }
            }
        }
    };
    private void changeJarvisEmotion(String text) {

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Ellathaiyum small letters-ku mathidrom, appothan check panna easy
        String lowerText = text.toLowerCase();

        // 1. PROUD (Deredere Mode - Happy & Affectionate)
        // Neenga praise panna Jarvis semma happy aagiduva
        if (lowerText.contains("thanks") || lowerText.contains("good girl") ||
                lowerText.contains("awesome") || lowerText.contains("love you") ||
                lowerText.contains("super") || lowerText.contains("great job")) {

            jarvisEngine.setEmotion(JarvisEngine.EmotionState.PROUD);

            Log.d("JarvisEmotion", "Mood Changed: PROUD 😍");
        }

        // 2. ANNOYED (Tsundere Mode - Angry but caring)
        
        else if (lowerText.contains("idiot") || lowerText.contains("baka") ||
                lowerText.contains("useless") || lowerText.contains("stupid") ||
                lowerText.contains("shut up") || lowerText.contains("bad")) {

            jarvisEngine.setEmotion(JarvisEngine.EmotionState.ANNOYED);
            Log.d("JarvisEmotion", "Mood Changed: ANNOYED 😤");
        }

        // 3. SERIOUS (Kuudere Mode - Laser Focused)
        // Urgent / Work time la over-ah vilayada koodathu
        else if (lowerText.contains("emergency") || lowerText.contains("important") ||
                lowerText.contains("focus") || lowerText.contains("serious") ||
                lowerText.contains("work") || lowerText.contains("code")) {

            jarvisEngine.setEmotion(JarvisEngine.EmotionState.SERIOUS);
            Log.d("JarvisEmotion", "Mood Changed: SERIOUS 🤖");
        }

        // 4. PLAYFUL (Cheeky/Teasing Mode)
        // Fun-ah pesumbothu
        else if (lowerText.contains("joke") || lowerText.contains("funny") ||
                lowerText.contains("boring") || lowerText.contains("play") ||
                lowerText.contains("tease")) {
            jarvisEngine.setEmotion(JarvisEngine.EmotionState.PLAYFUL);
            Log.d("JarvisEmotion", "Mood Changed: PLAYFUL 😉");
        }

        // 5. NORMAL (Reset to default sweet mode)
        // Normal-ah irukka sonna
        else if (lowerText.contains("relax") || lowerText.contains("calm down") ||
                lowerText.contains("normal") || lowerText.contains("reset")) {

            jarvisEngine.setEmotion(JarvisEngine.EmotionState.NORMAL);
            Log.d("JarvisEmotion", "Mood Changed: NORMAL 😊");
        }
    }

    private void toggleWiFi(boolean enable) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(panelIntent);
            speakAndLog("Opening WiFi settings, Commander.", null);
        } else {
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(enable);
                speakAndLog("WiFi turned " + (enable ? "on" : "off"), null);
            }
        }
    }

    private void toggleFlashlight(boolean enable) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, enable);
            speakAndLog("Flashlight turned " + (enable ? "on" : "off"), null);
        } catch (Exception e) {
            Log.e("JarvisAutomation", "Error toggling flashlight", e);
            speakAndLog("I couldn't control the flashlight, Commander.", e);
        }
    }

    private void toggleHotspot(boolean enable) {
        // Hotspot is very restricted. Usually just opens the settings.
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            speakAndLog("Opening Hotspot settings, Commander.", null);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            speakAndLog("I'll open wireless settings for you to toggle the hotspot.", null);
        }
    }

    private void toggleMobileData(boolean enable) {
        // Mobile data is also restricted.
        Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        speakAndLog("Opening Mobile Data settings, Commander.", null);
    }
}



