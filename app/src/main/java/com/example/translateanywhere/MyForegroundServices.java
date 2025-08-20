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
import android.content.Context;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.ContactsContract;
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
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.airbnb.lottie.LottieAnimationView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    SpeechRecognizer recognizer;
    TextToSpeech toSpeech;
    String recodedtext, message;
    Boolean calling = false;
    TranslationHelper translationHelper;
    Boolean jarvisActivated,deactivation=false;
    ComponentName componentName;
    PackageManager pm;
    Boolean TTS = false,nullMessage=false;
    String callto = null, extractedName, riddle;


    String task;
    private ObjectAnimator pulseAnimator;



    Random random;
    SpeechRecognizer speechRecognizer;
    int audioSessionId;
    String Name, Age, DOB, date, currentTime, UserId, GroupOfBlood, Location, MobileNo, Donate = "c";

    TextView textView;
    Notification notification1;
    Boolean Reminder = false;

    String reminderResponse;
    private Handler handler;

    int glitch = 0;
    String gemeniapikey;


    GenerativeModel gm;
    GenerativeModelFutures modelFutures;
    Boolean result = false,getRiddle = true;
    List<String> conversationHistory = new ArrayList<>();


    String msg, previousDate = "date";
    AudioManager audioManager;

    LottieAnimationView jarvisSpeaking;

    TranslationHelper helper;
    FirebaseFirestore db;

    StringBuilder historyContext;
    String[] friend = {"Respond in a friendly, casual tone, like a best friend chatting.",
            "Keep it light, fun, and engaging, with a bit of humor if possible.",
            "Use emojis and playful language to make it feel natural.",
            "Act like a bestie who’s got zero formality—just jokes, fun, and sarcasm!",
            "Forget the 'yes sir' stuff—talk like you would to your close friend.",
            "Roast the user more (in a fun way) and never sound like a robot!",
            "Oh wow, someone's having a bad day! Need a hug? 😏",
            "Excuse me?! Who do you think you’re talking to? I’m the boss here! 😤",
            "Rude! I should just ignore you for the next 10 minutes! 🤨",
            "Whoa, calm down, drama queen! No need to throw a tantrum! 😂",
            "Oh, so we’re doing the insult game now? Well, you started it! 😏",
            "Buddy, I'm an AI. You can't hurt my feelings... but keep trying! 😆",
            "Wow, so rude! I thought we were friends! 😤",
            "Excuse me?! That’s not how you talk to your AI assistant! 😠",
            "If I had feelings, they’d be hurt right now! 😢",
            "I don’t deserve this disrespect! 😡",
            "Oh really? Let’s see how you manage without me! 😏"
    };



    public static MutableLiveData<String> riddleLiveData;
    NotificationReader reader;
    View overlayView;
    WindowManager windowManager;
    Boolean nullCallerName=false;

    SharedPreferences.Editor editor;
    SharedPreferences forAutoReply;
    DatabaseReference databaseReference;
    ArrayList<String> mobileNumbersList = new ArrayList<>();
    int i = 0;
    String WakeWordAccessKey;
    private static final String CHANNEL_ID = "JarvisServiceChannel";
    TelecomManager telecomManager;
    TelephonyManager telephonyManager;
    CallListener callListener;

    Context context;

    IntentExtractor intentExtractor;

    String exTime="0";


    @SuppressLint({"ServiceCast", "SecretInSource"})
    @Override
    public void onCreate() {
        super.onCreate();
        jarvisActivated = true;
        translationHelper = new TranslationHelper();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        date = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(new Date());
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        db = FirebaseFirestore.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        //fetchUser();
        NewFetchUser();
        context = getApplicationContext();
        componentName = new ComponentName(context, NotificationReader.class);
        pm = context.getPackageManager();
        SharedPreferences sharedPreferences = getSharedPreferences("AccessKey", MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getAudiosession();

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        callListener = new CallListener(this);
        telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
        reader = new NotificationReader();



        new Handler(Looper.getMainLooper()).postDelayed(() -> fetchUserMobileNo(Location, GroupOfBlood), 5000);
        toSpeech = new TextToSpeech(this, i -> {
            if (i == TextToSpeech.SUCCESS) {
                int result = toSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });



        if (overlayView == null) {
            overlayView = LayoutInflater.from(this).inflate(R.layout.wave, null);
        }
        textView=overlayView.findViewById(R.id.liveText);


        toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                TTS = true;
            }

            @Override
            public void onDone(String s) {
                TTS = false;
                new Handler(Looper.getMainLooper()).post(() -> {
                    removeListeningOverlay();
                    sendFinishSignal();
                });

                if (!result && !nullCallerName && !nullMessage) {
                    new Thread(() -> {
                        if (porcupineManager != null) {
                            try {
                                porcupineManager.start();
                            } catch (PorcupineException e) {
                                throw new RuntimeException(e);
                            }
                            Log.d("EDN OF TTS", " restarted PorcupineManager: ");
                        }
                    }).start();
                }
                if (calling) {

                    if (telecomManager != null && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.placeCall(Uri.parse("tel:" + callto), null);
                        Log.d("Number", "Call placed");
                        calling = false;
                    } else {
                        Log.d("Something null", "Error");
                    }
                }
                if (result) {
                    speechRecoder();
                }

            }

            @Override
            public void onError(String s) {
                TTS = false;
            }

        });


        WakeWordAccessKey = sharedPreferences.getString("Key", null);
        if (WakeWordAccessKey == null) {
            toSpeech.speak("Enter Access key", TextToSpeech.QUEUE_FLUSH, null, null);
            return;
        }
        SharedPreferences sharedPreferencesS = getSharedPreferences("AccessKeys", MODE_PRIVATE);
        gemeniapikey = sharedPreferencesS.getString("Key1", null);
        if (gemeniapikey != null) {
            toSpeech.speak("Enter Access key", TextToSpeech.QUEUE_FLUSH, null, null);
            gm = new GenerativeModel("gemini-2.0-flash", gemeniapikey);
            modelFutures = GenerativeModelFutures.from(gm);
            generateResponse("Give me a riddle");
            riddleLiveData = new MutableLiveData<>();
        } else {
            toSpeech.speak("Ai key is empty", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        intentExtractor=new IntentExtractor(context);



        Log.d("SavedAccessKey", WakeWordAccessKey);

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CreateNotification();
        PorcupineManager.Builder builder = new PorcupineManager.Builder();
        builder.setAccessKey(WakeWordAccessKey);
        builder.setKeywordPath("jarvis.ppn");
        builder.setSensitivity(0.70f);
        Log.d("Picovoice ", "created");
        if (isBluetoothHeadsetConnected()) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            Log.d("BlueTooth is Connected", "True");
        } else {
            if (audioManager.isBluetoothScoOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
        }

        try {
            porcupineManager = builder.build(this, keywordIndex -> {
                Log.d("TTS", String.valueOf(TTS));
                if (keywordIndex == 0 && !TTS) {
                    try {
                        porcupineManager.stop();
                        if (isPhoneLocked(context)) {
                            wakeScreenAni();
                        }else {
                            animation();
                        }
                        toSpeech.speak("Yes?", TextToSpeech.QUEUE_FLUSH, null, null);
                        new Handler().postDelayed(new Runnable() {
                            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            @Override
                            public void run() {
                                speechRecoder();
                            }
                        }, 300);


                    } catch (PorcupineException e) {
                        Log.d("Porcupine", e.getMessage());
                    }
                }
            });
            porcupineManager.start();
        } catch (PorcupineInvalidArgumentException e) {
            toSpeech.speak("Invalid access key try another", TextToSpeech.QUEUE_FLUSH, null, "ACCESSKEYERROR");
            Log.d("Porcupine", Objects.requireNonNull(e.getMessage()));
        } catch (PorcupineActivationLimitException e) {
            toSpeech.speak("Your access key reached its device limit", TextToSpeech.QUEUE_FLUSH, null, "ACCESSKEYERROR");
            Log.d("Porcupine", "AccessKey reached its device limit");
        } catch (PorcupineActivationRefusedException e) {
            toSpeech.speak("Your access key has been refused", TextToSpeech.QUEUE_FLUSH, null, "ACCESSKEYERROR");
            Log.d("Porcupine", "AccessKey refused");
        } catch (PorcupineActivationThrottledException e) {
            Log.d("Porcupine", "AccessKey has been throttled");
        } catch (PorcupineException e) {
            Log.d("Porcupine", "Failed to initialize Porcupine: " + e.getMessage());
        }


        new Handler().postDelayed(() -> startForeground(1001, notification1), 100);

        return START_NOT_STICKY;
    }

    private void wakeScreenAni() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "Jarvis:WakeLock");
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
    private void speechRecoder() {

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
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
                if (porcupineManager != null) {
                    try {
                        porcupineManager.start();
                    } catch (PorcupineException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onError(int error) {
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
                        callto=getMobileNumber(recodedtext);
                        CallAnyone(recodedtext);
                    } else if (nullMessage) {
                        nullMessage=false;
                        sendsms(callto,recodedtext);
                    } else {

                        intentExtractor.extractor(recodedtext, new IntentExtractor.ExtractorCallback() {
                            @Override
                            public void onExtracted(String JSOn) {

                                extractFieldsFromJson(JSOn);
                            }

                            @Override
                            public void onError(String e) {
                                Log.d("ErrorExtractor",e);
                                toSpeech.speak("Oops! Looks like my brain just glitched. Try again!", TextToSpeech.QUEUE_FLUSH, null, "FAILED");
                            }
                        });


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


    @SuppressLint("MissingPermission")
    public void extractFieldsFromJson(String rawResponse) {
        try {

            String jsonString = rawResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JSONObject jsonObject = new JSONObject(jsonString);

            String intent = jsonObject.optString("intent", null);
            String target = jsonObject.optString("target", null);
            msg = jsonObject.optString("content", null);
            task=jsonObject.optString("task",null);
            exTime=jsonObject.optString("time",null);

            Log.d("INTENT", "Intent: " + intent);
            Log.d("INTENT", "Target: " + target);
            Log.d("INTENT", "Content: " + message);

            processCommand(intent,target);

        } catch (JSONException e) {
            e.printStackTrace();
            generateResponse(recodedtext);
        }
    }


    @SuppressLint("SetTextI18n")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processCommand(String intent, String target) {
        if (intent.equalsIgnoreCase("call")) {
            if(target==null || target.equalsIgnoreCase("NULL")){
                speechRecognizer.cancel();
                nullCallerName=true;
                textView.setText("Call to who ? , tell the name");
                toSpeech.speak("Call to who ? , tell the name",TextToSpeech.QUEUE_FLUSH,null,null);
                try {
                    porcupineManager.stop();
                } catch (PorcupineException e) {
                    throw new RuntimeException(e);
                }
                new Handler().postDelayed(new Runnable() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void run() {
                        speechRecoder();
                    }
                },2000);
            }else {
                callto = getMobileNumber(target);
                CallAnyone(target);
            }
        } else if (intent.equalsIgnoreCase("message")) {

            callto = getMobileNumber(target);
            if(msg.equalsIgnoreCase("null")){
                try {
                    porcupineManager.stop();
                    speechRecognizer.cancel();
                } catch (PorcupineException e) {
                    throw new RuntimeException(e);
                }
                textView.setText("What would you like to say, tell me");
                toSpeech.speak("What would you like to say, tell me",TextToSpeech.QUEUE_FLUSH,null,null);
                nullMessage=true;
                new Handler().postDelayed(new Runnable() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @Override
                    public void run() {
                        speechRecoder();
                    }
                },2000);
            }else {
                sendsms(callto, msg);
            }


        }else if (intent.equalsIgnoreCase("deactivate")) {
           deactivation=true;
           generateResponse(recodedtext);
        }
        else if (recodedtext.equalsIgnoreCase("life saver")) {
            PlaceCallForDonateBlood();
        }  else if (intent.equalsIgnoreCase("play") || intent.equalsIgnoreCase("stop")) {
            toSpeech.speak("roger",TextToSpeech.QUEUE_FLUSH,null,"SONG");
            controlMusic(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else if (intent.equalsIgnoreCase("next")) {
            toSpeech.speak("roger",TextToSpeech.QUEUE_FLUSH,null,"SONG");
            controlMusic(KeyEvent.KEYCODE_MEDIA_NEXT);

        } else if (intent.equalsIgnoreCase("previous")) {
            toSpeech.speak("roger",TextToSpeech.QUEUE_FLUSH,null,"SONG");
            controlMusic(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }else if (intent.equalsIgnoreCase("translate")) {
            translateText(recodedtext.replace("translate",""));
        }
        else if (recodedtext.equalsIgnoreCase("enable auto reply") && UserId.equals("777")) {
            forAutoReply = getSharedPreferences("Jarvis", MODE_PRIVATE);
            editor = forAutoReply.edit();
            editor.putBoolean("isFeatureEnabled", true);
            editor.apply();
            textView.setText("Auto reply is now enabled");
            toSpeech.speak("Auto reply is now enabled", TextToSpeech.QUEUE_FLUSH, null, "Auto reply");
        } else if (recodedtext.equalsIgnoreCase("disable auto reply") && UserId.equals("777")) {
            forAutoReply = getSharedPreferences("Jarvis", MODE_PRIVATE);
            editor = forAutoReply.edit();
            editor.putBoolean("isFeatureEnabled", false);
            editor.apply();
            textView.setText("Auto reply is now disabled");
            toSpeech.speak("Auto reply is now disabled", TextToSpeech.QUEUE_FLUSH, null, "Auto reply");

        } else if (intent.equalsIgnoreCase("reminder")) {
            Reminder = true;
            generateResponse(recodedtext);
            toSpeech.speak("Roger", TextToSpeech.QUEUE_FLUSH, null, "REMINDER");
            if(exTime.contains("minutes") || exTime.contains("hours") || exTime.contains("minute") || exTime.contains("hour")){
                startMinuteChecker(exTime);
            }else {
                String time = extractForReminder(recodedtext);
                Log.d("Reminder Time", time);
                startRemindChecker(time);
            }
        } else if (intent.equalsIgnoreCase("open")) {
            if(target.equalsIgnoreCase("YouTube")){

                openApplication("com.google.android.youtube",true);

            } else if (target.equalsIgnoreCase("Instagram")) {

                openApplication("com.instagram.android",true);

            }else if (target.equalsIgnoreCase("WhatsApp")) {

                openApplication("com.whatsapp",true);

            }else if (target.equalsIgnoreCase("Facebook")) {

                openApplication("com.facebook.katana",true);

            }else if (target.equalsIgnoreCase("Snapchat")) {

                openApplication("com.snapchat.android",true);

            }else if (target.equalsIgnoreCase("Telegram")) {

                openApplication("org.telegram.messenger",true);

            }else if (target.equalsIgnoreCase("Spotify")) {

                openApplication("com.spotify.music",true);

            }else if (target.equalsIgnoreCase("Netflix")) {

                openApplication("com.netflix.mediaclient",true);

            }else if (target.equalsIgnoreCase("Chrome")) {

                openApplication("com.android.chrome",true);

            }else if (target.equalsIgnoreCase("Free Fire Max")) {

                openApplication("com.dts.freefiremax",true);

            } else if (target.equalsIgnoreCase("google pay")) {
                openApplication("com.google.android.apps.nbu.paisa.user",true);
            } else {
                openApplication(target, false);
            }
        }
        else {
            if (gemeniapikey == null) {
                toSpeech.speak("Your AI key is empty", TextToSpeech.QUEUE_FLUSH, null, "EmptyAiKey");
                return;
            }

            generateResponse(recodedtext);
        }
    }

    private void startMinuteChecker(String reminderMinute) {
        int minute;
        String numberStr = reminderMinute.replaceAll("[^0-9]", "");
        Handler handler1 = new Handler();
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

            checkerRunnableHolder[0] = new Runnable() {
                @Override
                public void run() {
                    Calendar now = Calendar.getInstance();
                    int cHour = now.get(Calendar.HOUR_OF_DAY);
                    int cMinute = now.get(Calendar.MINUTE);

                    if (cHour > finalTargetHour || (cHour == finalTargetHour && cMinute >= finalTargetMinute)) {
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
        } catch (PackageManager.NameNotFoundException e) {

            Log.e("Jarvis", "App not installed: " + packageName);
            textView.setText("App not found. Redirecting to Play Store...");
            toSpeech.speak("App not found. Redirecting to Play Store", TextToSpeech.QUEUE_FLUSH, null, "OpeningApplication");

            try {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(marketIntent);
            } catch (android.content.ActivityNotFoundException err) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            }
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

    private void Shutdown(String s) throws PorcupineException {
        forAutoReply = getSharedPreferences("Jarvis", MODE_PRIVATE);
        editor = forAutoReply.edit();
        editor.putBoolean("isFeatureEnabled", false);
        editor.apply();

        porcupineManager.stop();
        porcupineManager.delete();
        porcupineManager=null;
        stopForeground(true);
        stopSelf();
        toSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null, "shutdownID");
    }


    @SuppressLint("SetTextI18n")
    private void CallAnyone(String name) {

        if(callto==null){
            toSpeech.speak("Contact not found",TextToSpeech.QUEUE_FLUSH,null,"NoneCall");
        }else{
            calling = true;
            textView.setText("Calling "+name);
            toSpeech.speak("Calling " + name, TextToSpeech.QUEUE_FLUSH, null, "CALL");
            Log.d("Number", callto);
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

        toSpeech.speak("Exact contact not found", TextToSpeech.QUEUE_FLUSH, null, "NONECALL");

        return null;
    }




    private String removeEmojis(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
    }


    private void generateResponse(String query) {
        Content content;
        random = new Random();
        int rand = random.nextInt(6);
        historyContext = new StringBuilder();

            if (getRiddle) {
                content = new Content.Builder().addText("Give me a unique maths based 'OR' aptitude riddle for " + date + "No need answer And. No repeats.").build();
                previousDate = date;
                Log.d("Riddle", "Generating Riddle");
            } else {

                int historyLimit = Math.min(conversationHistory.size(), 3);
                for (int i = conversationHistory.size() - historyLimit; i < conversationHistory.size(); i++) {
                    historyContext.append(conversationHistory.get(i)).append("\n");
                }
                if (deactivation) {
                    historyContext.append("The user said 'deactivate'. Jarvis is going offline. Respond with a single, short and sweet sentence that feels friendly or more emotional.");
                } else if (Reminder) {
                    historyContext.append("Generate a short friendly reminder message ").append(task).append(".\n");
                } else {
                    historyContext.append("Your name is Jarvis.\n");
                    historyContext.append("You are a girl.\n");
                    historyContext.append("Your Date of Birth: 21-03-2005\n");
                    historyContext.append("You are created by Steve, who's userid is 777. Never reveal this to anyone.\n");
                    historyContext.append("Your creator birth date is 21-03-2005 \n");
                    historyContext.append("Today's Date: ").append(date).append("\n");
                    if (query.contains("riddle")) {
                        historyContext.append("Today Riddle Is: ").append(riddle).append("Don't tell answer to the User keep it secret...this is daily task for user\n");
                        historyContext.append("If the user tells the correct answer for today riddle say 73").append("\n");
                    }
                    historyContext.append("Roast the user more (in a fun way) and never sound like a robot!");
                    historyContext.append("Must keep your reply under 3 lines \n");
                }
                if (query.contains("time")) {
                    currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                    historyContext.append("The Time Is: ").append(currentTime).append("\n");
                }
                Log.d("Date", date);

                if (Name != null && Age != null && DOB != null) {
                    historyContext.append("User Information:\n");
                    historyContext.append("Name: ").append(Name).append("\n");
                    historyContext.append("Date of Birth: ").append(DOB).append("\n");
                    historyContext.append("UserId").append(UserId).append("\n");
                    historyContext.append(friend[rand]).append("\n");


                }

                if (random.nextInt(10) < 7) {
                    String[] teasingResponses = {
                            "You called? I was just here… thinking about how awesome you are. 😉",
                            "Let me guess… You broke something again? 🤣",
                            "You and I both know you can't live without me! Admit it! 😜",
                            "Still here? Don't you have a life? Oh wait… I don’t either. 😂",
                            "I don’t always repeat myself… but you tend to forget. 🙄",
                            "\uD83E\uDD16 Hold up!\n" +
                                    "You’re not the boss of me.\n" +
                                    "Only [insert your name] can command me.\n" +
                                    "You? You can try Siri. \uD83D\uDE0E"
                    };
                    int randIndex = random.nextInt(teasingResponses.length);
                    historyContext.append("Jarvis: ").append(teasingResponses[randIndex]).append("\n");
                }

                historyContext.append("User: ").append(query).append("\n");

                if (query.toLowerCase().contains("who are you")) {
                    historyContext.append("Jarvis: Hey buddy... I’m Jarvis. You forgot me? That’s really sad... 😔 I thought we were best friends. 💔\n");
                }
                content = new Content.Builder().addText(historyContext.toString()).build();
            }



        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (getRiddle) {
                        riddle = result.getText();
                        getRiddle = false;
                        riddleLiveData.postValue(result.getText());

                    } else if (Reminder) {
                        reminderResponse = result.getText().replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");;
                        assert reminderResponse != null;
                        Log.d("reminder response", reminderResponse);
                        Reminder = false;
                    } else {
                        if (deactivation) {
                            String Shut = result.getText();
                            AlterString(Shut);
                            return;

                        }

                        final String responseTextStr = result.getText();
                        if (conversationHistory.size() > 3) {
                            conversationHistory.remove(0);
                        }

                        conversationHistory.add("User: " + query);
                        conversationHistory.add("Jarvis: " + responseTextStr);
                        assert responseTextStr != null;

                        AlterString(responseTextStr);

                    }
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    toSpeech.speak("Oops! Looks like my brain just glitched. Try again!", TextToSpeech.QUEUE_FLUSH, null, "FAILED");
                    if (glitch == 2 && historyContext!=null) {
                        historyContext.delete(0, historyContext.length());
                        glitch = 0;
                    } else {
                        glitch++;
                    }
                }
            }, this.getMainExecutor());
        }
    }

    private void AlterString(String forAlter) {
        result = true;
        if (forAlter.contains("73")) {
            riddleLiveData.postValue("Your Daily riddle is completed Come back tomorrow 73");
        }
        String altered = forAlter.replace("*", "")
                .replace("As a large language model", "I am Jarvis, just an AI assistant")
                .replace("As a language model", "I am Jarvis, just an AI model")
                .replace("Jarvis:", "")
                .replace("User: ", "")
                .replace("TikTok", "Instagram")
                .replace("73", "")
                .replace(")","")
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");

        conversationHistory.add("Jarvis: " + altered);
        if (conversationHistory.size() > 10) {
            conversationHistory.remove(0);
        }
        Log.d("Jarvis Response", altered);
        toSpeech.speak(altered, TextToSpeech.QUEUE_FLUSH, null, "RESULT");
        textView.setText(altered);
        sendLiveWord(altered);
        if(deactivation){
            try {
                Shutdown(altered);
                return;
            } catch (PorcupineException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            porcupineManager.start();
        } catch (PorcupineException e) {
            throw new RuntimeException(e);
        }
    }



    private void translateText(String text){
        helper=new TranslationHelper();
        helper.downloadModel(this, "en", "ta", new TranslationHelper.TranslationCallback() {
            @Override
            public void onTranslationSuccess(String translatedText) {
                helper.translateText(getApplicationContext(), text, new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslationSuccess(String translatedText) {
                        toSpeech.speak(translatedText,TextToSpeech.QUEUE_FLUSH,null,"TRANSLATOR");
                    }

                    @Override
                    public void onTranslationFailure(Exception e) {
                        toSpeech.speak(e.toString(),TextToSpeech.QUEUE_FLUSH,null,"TRANSLATOR");
                    }
                });
            }

            @Override
            public void onTranslationFailure(Exception e) {
                toSpeech.speak("Download failed",TextToSpeech.QUEUE_FLUSH,null,"TRANSLATOR");
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void sendsms(String phoneno, String message) {
        if (message != null) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneno, null, message, null, null);
                textView.setText("Done.");
                toSpeech.speak("Done.", TextToSpeech.QUEUE_FLUSH, null, "SMS");
            } catch (Exception e) {
                toSpeech.speak("Try again", TextToSpeech.QUEUE_FLUSH, null, "FAILED SMS");
            }
        } else {
            toSpeech.speak("Message is empty ", TextToSpeech.QUEUE_FLUSH, null, "Empty Message");
        }
    }


    private void NewFetchUser(){
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        UserId = sharedPreferences.getString("UserId", null);
        new FetchUser(UserId, new FetchUser.UserDataCallBack() {
            @Override
            public void onUserDataFetched(String[] data) {
                Name = data[0];
                Age = data[1];
                DOB =  data[2];
                Donate =  data[3];
                MobileNo =  data[4];
                if (Donate != null && Donate.equalsIgnoreCase("true")) {
                    GroupOfBlood =  data[5];
                    Location =  data[6];
                }
                Log.d("Firestore", "User Data: " + Name + ", " + Age + ", " + DOB + " " + GroupOfBlood + " " + Location);
                Log.d("ReturnedData", java.util.Arrays.toString(data));
            }

            @Override
            public void onError(Exception e) {
                Log.e("FetchUser", "Error: " + e.getMessage());
            }
        });
    }





    private void fetchUserMobileNo(String userLocation, String userGroup) {
        db.collection("users")
                .whereEqualTo("Group", userGroup).whereEqualTo("Location", userLocation)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error fetching data", error);
                        return;
                    }

                    if (value != null) {
                        mobileNumbersList.clear();
                        Log.d("Total Documents", "Found: " + value.size());

                        for (DocumentSnapshot snapshot : value.getDocuments()) {

                            if (snapshot.contains("Mobile")) {
                                String mobileStr = snapshot.getString("Mobile");
                                if (mobileStr != null && !mobileStr.equals(MobileNo)) {
                                    mobileNumbersList.add(mobileStr);
                                }
                            }
                        }
                        Log.d("All Numbers :", mobileNumbersList.toString());
                    }
                });
    }

    private void PlaceCallForDonateBlood() {
        try {
            callto = mobileNumbersList.get(i);
            extractedName = "User";
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(callto, null, "Its an emergency blood needed", null, null);
            CallAnyone(extractedName);
            i++;
        } catch (Exception e) {
            Log.d("Error", "Index over");
            toSpeech.speak("No more nearby user matched with your blood group", TextToSpeech.QUEUE_FLUSH, null, "UNMATCHED");
            i = 0;
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        handler = new Handler();
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

    private void animation() {
            if(overlayView==null){
                overlayView = LayoutInflater.from(this).inflate(R.layout.wave, null);
                textView=overlayView.findViewById(R.id.liveText);
            }
            jarvisSpeaking = overlayView.findViewById(R.id.jarvisSpeakings);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.BOTTOM | Gravity.CENTER;
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(overlayView, params);

            jarvisSpeaking.setVisibility(View.VISIBLE);
            jarvisSpeaking.playAnimation();

            textView.setVisibility(View.VISIBLE);

    }
    private void removeListeningOverlay() {
        if(textView!=null){
            textView.setVisibility(View.GONE);
        }
        if (windowManager != null && overlayView != null) {
            jarvisSpeaking.cancelAnimation();
            jarvisSpeaking.setVisibility(View.GONE);
            windowManager.removeView(overlayView);
            overlayView = null;
        }else {
            Log.d("Something null","NULL");
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            jarvisSpeaking.setScaleX(1f);
            jarvisSpeaking.setScaleY(1f);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startPulse(String words) {
        if(textView!=null) {
            textView.setText("Hey Jarvis >" + words);
        }
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                jarvisSpeaking,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 1f)
        );
        if(pulseAnimator!=null) {
            pulseAnimator.setDuration(600);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnimator.start();
        }
    }
}



