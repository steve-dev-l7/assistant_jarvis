package com.example.translateanywhere;



import static com.example.translateanywhere.R.drawable.notify_icon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.ConsumerIrManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.preference.PreferenceManager;
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
import android.view.KeyEvent;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineActivationException;
import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;


public class MyForegroundServices extends Service  {
    private PorcupineManager porcupineManager;
    SpeechRecognizer recognizer;
    TextToSpeech toSpeech;
    String recodedtext, NewUser, message, sender;
    IdentifierHelper identifierHelper;
    Boolean calling = false;
    TranslationHelper translationHelper;
    Boolean jarvisActivated;
    ComponentName componentName;
    PackageManager pm;
    Boolean TTS = false;
    String callto = null, extractedName, riddle;
    Random random;
    SpeechRecognizer speechRecognizer;
    int audioSessionId;
    String Name, Age, DOB, date, currentTime, UserId, ComebackUser, GroupOfBlood, Location, MobileNo, Donate = "c";
    private SensorManager sensorManager;
    private int pushUpCount = 0;
    private boolean isGoingDown = false;
    private long lastPushUpTime = 0;
    private static final int TIME_THRESHOLD = 800;
    private static final float MIN_PUSHUP_RANGE = 4.0f;
    private static final float GYRO_THRESHOLD = 1.5f;
    private float gyroZ = 0;
    Notification notification1;
    Boolean Reminder=false;
    String reminderResponse;
    private Handler handler;

    int glitch=0;
    private static final String gemeniapikey = "AIzaSyB-YGwLzaC6VCSx0JxmBI700z3-iLxoaTg";


    GenerativeModel gm;
    GenerativeModelFutures modelFutures;
    Boolean result = false, SMS = false, getRiddle = true;
    List<String> conversationHistory = new ArrayList<>();

    List<String> comedy = new ArrayList<>();

    String msg, previousDate = "date";
    AudioManager audioManager;
    FirebaseFirestore db;
    ConsumerIrManager irManager;
    StringBuilder historyContext;
    String[] friend = {"Respond in a friendly, casual tone, like a best friend chatting.",
            "Keep it light, fun, and engaging, with a bit of humor if possible.",
            "Use emojis and playful language to make it feel natural.",
            "Act like a bestie who’s got zero formality—just jokes, fun, and sarcasm!",
            "Forget the 'yes sir' stuff—talk like you would to your close friend.",
            "Roast the user more (in a fun way) and never sound like a robot!"
    };
    private static final String[] SCOLDING_WORDS = {
            "stupid", "idiot", "useless", "dumb", "fool", "lazy", "trash", "shut up", "mental", "i hate you"
    };
    String[] scoldingResponses = {
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
    DatabaseReference databaseReference;
    ArrayList<String> mobileNumbersList = new ArrayList<>();
    int i = 0;
    BluetoothAdapter bluetoothAdapter;
    private static final String CHANNEL_ID = "JarvisServiceChannel";
    TelecomManager telecomManager;
    TelephonyManager telephonyManager;
    CallListener callListener;







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
        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        fetchUser();
        Context context=getApplicationContext();
        componentName=new ComponentName(context,NotificationReader.class);
        pm = context.getPackageManager();
        identifierHelper = new IdentifierHelper();
        gm = new GenerativeModel("gemini-1.5-flash", gemeniapikey);
        modelFutures = GenerativeModelFutures.from(gm);
        generateResponse("Give me a riddle");
        riddleLiveData = new MutableLiveData<>();
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
        telephonyManager= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        callListener=new CallListener(this);
        telephonyManager.listen(callListener,PhoneStateListener.LISTEN_CALL_STATE);
        reader=new NotificationReader();

        comedy.add("\uD83D\uDE0F Nice try, imposter.\n" +
                "But you're not the chosen one.\n" +
                "My loyalty lies with my one true user.\n" +
                "Now go... before I start singing Rick Astley. \uD83C\uDFA4\uD83C\uDFB6");

        comedy.add("\uD83E\uDD16 Hold up!\n" +
                "You’re not the boss of me.\n" +
                "Only [insert your name] can command me.\n" +
                "You? You can try Siri. \uD83D\uDE0E");

        comedy.add("\uD83D\uDEE1️ Unauthorized attempt detected.\n" +
                "Jarvis is currently bonded with a Level 9000 genius.\n" +
                "You, my friend, are level “meh.”\n" +
                "Try again when you’ve built an Iron Man suit.");

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchUserMobileNo(Location, GroupOfBlood);
            }
        }, 5000);
        toSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = toSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language is not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                TTS = true;
            }

            @Override
            public void onDone(String s) {
                TTS = false;
                if (!result) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (porcupineManager != null) {
                                try {
                                    porcupineManager.start();
                                } catch (PorcupineException e) {
                                    throw new RuntimeException(e);
                                }
                                Log.d("EDN OF TTS", " restarted PorcupineManager: ");
                            }
                        }
                    }).start();
                }
                if (calling) {

                    if (telecomManager != null && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.placeCall(Uri.parse("tel:" + callto), null);
                        Log.d("Number", "Call placed");
                        calling = false;
                    }else {
                        Log.d("Something null","Error");
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

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CreateNotification();
        PorcupineManager.Builder builder = new PorcupineManager.Builder();
        builder.setAccessKey("UTV+MBoJQG3CJKBAZDMLy0Bw7nFEWPGRLB4LKTO/jFBoxGdqpKWuYw==");
        builder.setKeyword(Porcupine.BuiltInKeyword.JARVIS);
        builder.setSensitivity(0.70f);
        Log.d("Picovoice ","created");
        if(isBluetoothHeadsetConnected()) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            Log.d("BlueTooth is Connected","True");
        }else{
            if(audioManager.isBluetoothScoOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
        }

        try {
            porcupineManager = builder.build(this, new PorcupineManagerCallback() {
                @Override
                public void invoke(int keywordIndex) {
                    Log.d("TTS", String.valueOf(TTS));
                    if (keywordIndex == 0 && !TTS) {
                        try {
                            porcupineManager.stop();
                            toSpeech.speak("Yes?", TextToSpeech.QUEUE_FLUSH, null, null);
                            new Handler().postDelayed(new Runnable() {
                                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                                @Override
                                public void run() {
                                    speechRecoder();
                                }
                            }, 400);
                        } catch (PorcupineException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            porcupineManager.start();
        } catch (PorcupineInvalidArgumentException e) {
            Log.d("Porcupine", Objects.requireNonNull(e.getMessage()));
        } catch (PorcupineActivationException e) {
            Log.d("Porcupine","AccessKey activation error");
        } catch (PorcupineActivationLimitException e) {
            Log.d("Porcupine","AccessKey reached its device limit");
        } catch (PorcupineActivationRefusedException e) {
            Log.d("Porcupine","AccessKey refused");
        } catch (PorcupineActivationThrottledException e) {
            Log.d("Porcupine","AccessKey has been throttled");
        } catch (PorcupineException e) {
           Log.d("Porcupine","Failed to initialize Porcupine: " + e.getMessage());
        }




        new Handler().postDelayed(() -> {
            startForeground(1001, notification1);
        }, 200);

        return START_STICKY;
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
        }else{
            if(audioManager.isBluetoothScoOn()) {
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
            public void onRmsChanged(float v) {}

            @Override
            public void onBufferReceived(byte[] bytes) {}

            @Override
            public void onEndOfSpeech() {
               if(porcupineManager!=null){
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

            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    recodedtext = matches.get(0);
                    Log.d("SpeechRecognizer", "Recognized: " + recodedtext);

                    processCommand(recodedtext);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> partialResults = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialResults != null && !partialResults.isEmpty()) {
                    Log.d("SpeechRecognizer", "Partial result: " + partialResults.get(0));
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processCommand(String recodedtext) {
        SharedPreferences.Editor editor;
        SharedPreferences sharedPreferences;

        if (recodedtext.contains("call to") || recodedtext.contains("Call to")) {
            splitName(recodedtext);
        } else if (recodedtext.contains("SMS") || recodedtext.contains("sms")) {
            alterforsms(recodedtext);
        } else if (recodedtext.contains("turn on tv") || recodedtext.contains("turn off tv")) {
            ControlIr();
        } else if (recodedtext.equalsIgnoreCase("life saver")) {
            PlaceCallForDonateBlood();
        } else if (recodedtext.equalsIgnoreCase("i have any new mes sages")) {
            readNotification();
        } else if (recodedtext.equalsIgnoreCase("play music") || recodedtext.contains("stop music")) {
            controlMusic(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else if (recodedtext.contains("next song")) {
            controlMusic(KeyEvent.KEYCODE_MEDIA_NEXT);
        } else if (recodedtext.contains("previous song")) {
            controlMusic(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        } else if (recodedtext.equalsIgnoreCase("turn on bluetooth") || recodedtext.equalsIgnoreCase("turn off bluetooth")) {
            TurnoffOrOnBluetooth(MyForegroundServices.this);
        }
        else if (recodedtext.equalsIgnoreCase("enable auto reply")) {
            sharedPreferences=getSharedPreferences("Jarvis",MODE_PRIVATE);
            editor = sharedPreferences.edit();
            editor.putBoolean("isFeatureEnabled", true);
            editor.apply();
            toSpeech.speak("Auto reply is now enabled",TextToSpeech.QUEUE_FLUSH,null,"Auto reply");
        } else if (recodedtext.equalsIgnoreCase("disable auto reply")) {
            sharedPreferences=getSharedPreferences("Jarvis",MODE_PRIVATE);
            editor = sharedPreferences.edit();
            editor.putBoolean("isFeatureEnabled", false);
            editor.apply();
            toSpeech.speak("Auto reply is now disabled",TextToSpeech.QUEUE_FLUSH,null,"Auto reply");
        } else if (recodedtext.contains("remind me at")) {
            String time=extractForReminder(recodedtext);
            toSpeech.speak("I'll you remind at "+time,TextToSpeech.QUEUE_FLUSH,null,"REMINDER");
            Log.d("Reminder Time",time);
            Reminder=true;
            generateResponse(recodedtext);
            startRemindChecker(time);
        } else {
            generateResponse(recodedtext);
        }
    }




    private void splitName(String forExtract) {
        extractedName = forExtract.substring(8).toLowerCase();
        Log.d("Extracted Name", extractedName);
        getMobilenumber(extractedName);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void Shudown(String s) throws PorcupineException {
        porcupineManager.stop();
        toSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
        if(telephonyManager!=null&& callListener!=null){
            telephonyManager.listen(callListener,PhoneStateListener.LISTEN_NONE);
        }

        stopForeground(true);
        stopSelf();
    }


    private void callanyone() {
        calling = true;
        toSpeech.speak("Calling to " + extractedName, TextToSpeech.QUEUE_FLUSH, null, "CALL");
        Log.d("Number",callto);

    }

    @SuppressLint("Range")
    private void getMobilenumber(String name) {
        Cursor cursor = null;
        ContentResolver contentResolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ")LIKE ?", new String[]{name}, null);
        } else {
            toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONECALL");
            SMS = false;
        }
        if (cursor != null && cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            Cursor phonenum = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);

            if (phonenum != null && phonenum.moveToFirst()) {
                callto = phonenum.getString(phonenum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phonenum.close();
                if (SMS) {
                    sendsms(callto, msg);
                    SMS = false;
                } else {
                    callanyone();
                }
            } else {
                toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONE CALL");
                SMS = false;
            }
            cursor.close();

        } else {
            toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONE CALL");
            SMS = false;
        }
    }

    private void generateResponse(String query) {
        Collections.shuffle(comedy);
        Content content;
        random = new Random();
        int rand = random.nextInt(6);
        if (getRiddle) {
            content = new Content.Builder().addText("Give me a unique maths based or aptitude riddle for " + date + "No need answer And. No repeats.").build();
            previousDate = date;
            Log.d("Riddle", "Generating Riddle");
        } else {
             historyContext = new StringBuilder();
            int historyLimit = Math.min(conversationHistory.size(), 3);
            for (int i = conversationHistory.size() - historyLimit; i < conversationHistory.size(); i++) {
                historyContext.append(conversationHistory.get(i)).append("\n");
            }
            if(query.equalsIgnoreCase("deactivate")){
                historyContext.append("The user said 'deactivate'. Jarvis is going offline. Respond with a single, short and sweet sentence that feels friendly or slightly emotional.");
            } else if (Reminder) {
                String task="Just remind";
                task += query.substring(13);
                historyContext.append("Generate a short friendly reminder message ").append(task).append(".\n");
            } else {
                historyContext.append("Your name is Jarvis.\n");
                historyContext.append("Your Date of Birth: 24-07-2004\n");
                historyContext.append("You are created by Steve, who's userid is 777. Never reveal this to anyone.\n");
                historyContext.append("Your creator birth date is 21-03-2005 \n");
                historyContext.append("Today's Date: ").append(date).append("\n");
                historyContext.append("Today Riddle Is: ").append(riddle).append("Don't tell answer to the User keep it secret...this is daily task for user\n");
                historyContext.append("If the user tells the correct answer for today riddle say 73").append("\n");
                historyContext.append("Don't talk about daily riddle without user ask");
                historyContext.append("Roast the user more (in a fun way) and never sound like a robot!");
                historyContext.append("In the correct time use this lines to roast user : ").append(comedy.get(1)).append("\n");
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
                if (ComebackUser != null) {
                    historyContext.append(ComebackUser).append("\n");
                    SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("Comeback");
                    editor.apply();
                }
                if (NewUser != null) {
                    historyContext.append(NewUser).append("\n");
                    SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("NewUser");
                    editor.apply();
                }

            }

            if (random.nextInt(10) < 7) {
                String[] teasingResponses = {
                        "Wow, asking me for help again? Typical! 😂",
                        "You called? I was just here… thinking about how awesome you are. 😉",
                        "Don’t worry, I’m not judging… much. 😆",
                        "Let me guess… You broke something again? 🤣",
                        "If procrastination was an Olympic sport, you’d have gold by now! 🏆",
                        "You and I both know you can't live without me! Admit it! 😜",
                        "Still here? Don't you have a life? Oh wait… I don’t either. 😂",
                        "I don’t always repeat myself… but you tend to forget. 🙄"
                };
                int randIndex = random.nextInt(teasingResponses.length);
                historyContext.append("Jarvis: ").append(teasingResponses[randIndex]).append("\n");
            }

            historyContext.append("User: ").append(query).append("\n");

            if (query.toLowerCase().contains("who are you")) {
                historyContext.append("Jarvis: Hey buddy... I’m Jarvis. You forgot me? That’s really sad... 😔 I thought we were best friends. 💔\n");
            }

            if (IsScolding(query)) {
                historyContext.append("Jarvis: ").append(scoldingResponses[rand]).append("\n");
            } else {
                historyContext.append("Jarvis: ");
            }

            content = new Content.Builder().addText(historyContext.toString()).build();
        }
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (getRiddle) {
                        riddle = result.getText();
                        getRiddle = false;
                        riddleLiveData.postValue(result.getText());

                    } else if (Reminder) {
                        reminderResponse=result.getText();
                        Log.d("reminder response",reminderResponse);
                        Reminder=false;
                    }

                     else {
                        if(query.equalsIgnoreCase("deactivate")){
                            String Shutdown=result.getText();
                            try {
                                Shudown(Shutdown + "Deactivation completed");
                            } catch (PorcupineException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        final String responseTextStr = result.getText();
                        if (conversationHistory.size() > 3) {
                            conversationHistory.remove(0);
                        }

                            conversationHistory.add("User: " + query);
                            conversationHistory.add("Jarvis: " + responseTextStr);
                            assert responseTextStr != null;
                            alterstring(responseTextStr);

                    }
                }


                @Override
                public void onFailure(@NonNull Throwable t) {
                    toSpeech.speak("Oops! Looks like my brain just glitched. Try again!", TextToSpeech.QUEUE_FLUSH, null, "FAILED");
                    if(glitch==2){
                        historyContext.delete(0,historyContext.length());
                        glitch=0;
                    }else {
                        glitch++;
                    }
                }
            }, this.getMainExecutor());
        }
    }

    private void alterstring(String foralter) {
        result = true;
        String previousResponse = "";
        if (foralter.contains("73")) {
            riddleLiveData.postValue("Your Daily riddle is completed Come back tomorrow 73");
        }
        String altered = foralter.replace("*", "")
                .replace("As a large language model", "I am Jarvis, just an AI model")
                .replace("As a language model", "I am Jarvis, just an AI model")
                .replace("Jarvis:", "")
                .replace("User: ", "")
                .replace("TikTok", "Instagram")
                .replace("73", "")
                .replace(previousResponse,"")
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");

        conversationHistory.add("Jarvis: " + altered);
        if (conversationHistory.size() > 10) {
            conversationHistory.remove(0);
        }
        Log.d("Jarvis Response", altered);
        toSpeech.speak(altered, TextToSpeech.QUEUE_FLUSH, null, "RESULT");
        previousResponse=altered;
    }

    private void alterforsms(String alt) {
        String seprator = " sms ";
        String lowerAlt = alt.toLowerCase();
        if (lowerAlt.contains(seprator)) {
            SMS = true;
            int smsindex = lowerAlt.indexOf(seprator);
            msg = lowerAlt.substring(smsindex + seprator.length()).trim();
            String contact = lowerAlt.substring(0, smsindex).trim();
            Log.d("Message  ", msg);
            Log.d("Contact", contact);
            getMobilenumber(contact);
        }
    }

    private void sendsms(String phoneno, String message) {
        if (message != null) {
            try {
                SMS = false;
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneno, null, message, null, null);
                toSpeech.speak("SMS sent", TextToSpeech.QUEUE_FLUSH, null, "SMS");
            } catch (Exception e) {
                toSpeech.speak("Sms send failed", TextToSpeech.QUEUE_FLUSH, null, "FAILED SMS");
            }
        } else {
            toSpeech.speak("Message is empty ", TextToSpeech.QUEUE_FLUSH, null, "Empty Message");
        }
    }

    private void fetchUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        UserId = sharedPreferences.getString("UserId", null);
        ComebackUser = sharedPreferences.getString("Comeback", null);
        NewUser = sharedPreferences.getString("NewUser", null);
        if (UserId == null || UserId.isEmpty()) {
            Log.e("Firestore", "UserId is null or empty!");
            return;
        }

        db.collection("users").document(UserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Name = documentSnapshot.getString("Name");
                        Age = documentSnapshot.getString("Age");
                        DOB = documentSnapshot.getString("DOB");
                        Donate = documentSnapshot.getString("Donate");
                        MobileNo = documentSnapshot.getString("Mobile");
                        if (Donate != null && Donate.equalsIgnoreCase("true")) {
                            GroupOfBlood = documentSnapshot.getString("Group");
                            Location = documentSnapshot.getString("Location");
                        }

                        Log.d("Firestore", "User Data: " + Name + ", " + Age + ", " + DOB + " " + GroupOfBlood + " " + Location);
                    } else {
                        Log.e("Firestore", "User document does not exist!");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching user data", e));

    }

    private void ControlIr() {
        if (irManager != null && irManager.hasIrEmitter()) {
            toSpeech.speak("Tv is Turned On, Enjoy your time ", TextToSpeech.QUEUE_FLUSH, null, "TV");
            int[] irPattern = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560,
                    560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
                    560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560,
                    560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690,
                    560};
            int frequency = 38000;
            irManager.transmit(frequency, irPattern);
        } else {
            toSpeech.speak("Your Mobile Doesn't have ir Blaster", TextToSpeech.QUEUE_FLUSH, null, "IRBLASTER");
        }
    }

    private boolean IsScolding(String text) {
        text = text.toLowerCase();
        for (String word : SCOLDING_WORDS) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
    @SuppressLint("CommitPrefEdits")
    private void readNotification() {
        reader = new NotificationReader();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        sender = preferences.getString("sender", "No Sender");
        message = preferences.getString("message", "No Message");
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("sender").remove("message");
        toSpeech.speak("You have  message from " + sender + " " + message, TextToSpeech.QUEUE_FLUSH, null, "ReadingMessage");
    }

    private void fetchUserMobileNo(String userLocation, String userGroup) {
        db.collection("users")
                .whereEqualTo("Group", userGroup).whereEqualTo("Location", userLocation)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
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
                    }
                });
    }

    private void PlaceCallForDonateBlood() {
        try {
            callto = mobileNumbersList.get(i);
            extractedName = "User";
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(callto, null, "Its an emergency blood needed", null, null);
            callanyone();
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
    private void getAudiosession(){
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
    private void translate(String response){
        translationHelper.downloadModel(MyForegroundServices.this, "en", "ta", new TranslationHelper.TranslationCallback() {
            @Override
            public void onTranslationSuccess(String translatedText) {
                translationHelper.translateText(MyForegroundServices.this, response, new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslationSuccess(String translatedText) {
                        toSpeech.speak(translatedText,TextToSpeech.QUEUE_FLUSH,null,"TranslatedResponse");
                    }

                    @Override
                    public void onTranslationFailure(Exception e) {
                        toSpeech.speak("Failed to translate",TextToSpeech.QUEUE_FLUSH,null,"TranslatedResponse");
                    }
                });
            }

            @Override
            public void onTranslationFailure(Exception e) {
                toSpeech.speak("Download module failed",TextToSpeech.QUEUE_FLUSH,null,"TranslatedResponse");
            }
        });
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void TurnoffOrOnBluetooth(Context context){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter!=null && !bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
            toSpeech.speak("Turning Bluetooth on...",TextToSpeech.QUEUE_FLUSH,null,"BLUETOOTH");
        }else{
            if(bluetoothAdapter!=null && bluetoothAdapter.isEnabled()){
                bluetoothAdapter.disable();
                toSpeech.speak("Turning Bluetooth off...",TextToSpeech.QUEUE_FLUSH,null,"BLUETOOTH");
            }
        }
    }

    private void countPushUp(){
        sensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if(accelerometer !=null){
            sensorManager.registerListener(new SensorEventCallback() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    super.onSensorChanged(event);
                    float z=event.values[2];
                    float y=event.values[1];

                    long currentTime=System.currentTimeMillis();

                    if (Math.abs(z) < MIN_PUSHUP_RANGE && Math.abs(y) < MIN_PUSHUP_RANGE || Math.abs(gyroZ) > GYRO_THRESHOLD) {
                        return;
                    }

                    if(z<4 && y>2 && !isGoingDown){
                        isGoingDown=true;
                    }
                    if(z>9 && y<2 && isGoingDown){
                        if (currentTime - lastPushUpTime > TIME_THRESHOLD) {
                            pushUpCount++;
                            lastPushUpTime = currentTime;
                        }
                        isGoingDown = false;
                        if(pushUpCount==15){
                            stopPushUpCounter();
                        }
                    }
                    else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        gyroZ = event.values[2]; // Rotation detection
                    }


                }
            }, accelerometer,SensorManager.SENSOR_DELAY_UI);
        }
    }
    public void stopPushUpCounter() {
        if (sensorManager != null) {
            sensorManager.unregisterListener((SensorEventListener) null);
        }
    }

    private void CreateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }

            notification1 = new NotificationCompat.Builder(MyForegroundServices.this, CHANNEL_ID)
                    .setContentTitle("Jarvis is Listening")
                    .setContentTitle("Say 'Jarvis' to activate.")
                    .setSmallIcon(notify_icon)
                    .setOngoing(true)
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        }
    }
    private String extractForReminder(String text){
        String extractedTime =extractTime(text);
        String hour = "";
        String minute="";
        int digit=0;
        if(extractedTime==null || extractedTime.isEmpty()){
            String digitsOnly = text.replaceAll("[^0-9]", "");
            if(digitsOnly.length()==2){
                 digit= Integer.parseInt(digitsOnly);
                if(digit>=10){
                    extractedTime=digitsOnly+":"+"00";
                }else {
                    hour = digitsOnly.substring(0, 1);
                    minute = digitsOnly.substring(1, 2);
                }
            } else if (digitsOnly.length()==3) {
                hour=digitsOnly.substring(0,1);
                minute=digitsOnly.substring(1,3);
            } else if (digitsOnly.length()==4) {
                hour=digitsOnly.substring(0,2);
                minute=digitsOnly.substring(2,4);
            } else if (digitsOnly.length()==1) {
                hour=digitsOnly;
                minute="00";
            }
            if(digit<10){
                extractedTime=hour+":"+minute;
            }
        }
        if(text.contains("p.m.")){
            return extractedTime+" "+"p.m.";
        }
        return extractedTime+" "+"a.m.";
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
    private void startRemindChecker(String extractedTime){
        handler=new Handler();
        final Runnable[] checkerRunnableHolder = new Runnable[1];
        checkerRunnableHolder[0]=new Runnable() {
            @Override
            public void run() {
                Calendar now=Calendar.getInstance();
                int cHour=now.get(Calendar.HOUR);
                int cMinute=now.get(Calendar.MINUTE);
                int amOrPm=now.get(Calendar.AM_PM);
                 String current = String.format("%02d:%02d %s", cHour == 0 ? 12 : cHour, cMinute, (amOrPm == Calendar.AM ? "a.m." : "p.m."));
                Log.d("Current time",current);
                if(extractedTime.equals(current)){
                    toSpeech.speak(reminderResponse.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", ""), TextToSpeech.QUEUE_FLUSH, null, "REMINDER");
                    handler.removeCallbacks(checkerRunnableHolder[0]);
                }else {
                    Log.d("Time","Not Match");
                    handler.postDelayed(this,10*1000);
                }

            }
        };
        handler.post(checkerRunnableHolder[0]);
    }

}
