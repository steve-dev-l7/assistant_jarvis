package com.example.translateanywhere;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JarvisEngine {

    private String Name;
    private String Age;
    private String DOB;

    private String Location;

    private String UserId;


    public enum EmotionState {
        NORMAL, PLAYFUL, ANNOYED, PROUD, SERIOUS
    }

    private static final String TAG = "JarvisEngine";

    // MAIN SERVER (High-End Laptop)
    private static final String MAIN_SERVER = "https://stably-oversusceptible-anne.ngrok-free.dev/api/chat";
    private static final String MAIN_MODEL = "deepseek-r1:8b";

    // BACKUP SERVER (Old i3 PC)
    private static final String BACKUP_SERVER = "https://maeve-anthropometrical-emerald.ngrok-free.dev/api/chat";
    private static final String BACKUP_MODEL = "phi3"; // Unga chinna model

    private final OkHttpClient client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isReady = true;

    private final LinkedList<String> memory = new LinkedList<>();
    private static final int MAX_MEMORY = 4;

    public EmotionState currentEmotion = EmotionState.PLAYFUL;
    private int bondLevel = 20;

    // 🔴 NEW: Permanent Memory Storage
    private SharedPreferences permanentMemoryPrefs;
    private static final String PREF_MEMORY = "JarvisPermanentMemory";

    // 🔴 OPTIMIZATION: Precompiled Patterns and ThreadLocals
    private static final java.util.regex.Pattern MEMORY_PATTERN = java.util.regex.Pattern.compile("(?i)(jarvis|remember that|note that|save this)");
    private static final java.util.regex.Pattern THINK_PATTERN = java.util.regex.Pattern.compile("(?s)<think>.*?</think>");
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private final ThreadLocal<java.text.SimpleDateFormat> sdfThreadLocal = ThreadLocal.withInitial(() ->
            new java.text.SimpleDateFormat("EEEE, MMM dd, yyyy 'Time:' hh:mm a", java.util.Locale.getDefault()));
    private Method wifiApMethod;

    MyForegroundServices myForegroundServices;
    private boolean hasWarnedLowBattery = false;

    Context context;


    public JarvisEngine(Context context) {
        this.context=context;
        client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS) // Fast fail if Ngrok is down
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                // SPEED HACK 1: Keep connections alive for 5 minutes!
                .connectionPool(new ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
                .build();

        Log.d(TAG, "AI Brain connected to Server with Turbo Speed.");
        permanentMemoryPrefs = context.getSharedPreferences(PREF_MEMORY, Context.MODE_PRIVATE);

        FetchUserDetails(context);
    }

    //  THE SURVIVAL PROTOCOL: Background Battery Monitor

    public void setEmotion(EmotionState state) {
        this.currentEmotion = state;
    }

    public void setForegroundServices(MyForegroundServices services){
        this.myForegroundServices=services;
    }

    //  NEW: Save and Retrieve Memory Methods
    private void saveImportantMemory(String fact) {
        // Remove the 'new MyForegroundServices()' line!

        // Show message safely on the Main UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (myForegroundServices != null && myForegroundServices.dynamicIslandManager != null) {
                myForegroundServices.dynamicIslandManager.showCustomMessage("Noted !");
            } else {
                Log.e(TAG, "Cannot show Dynamic Island, service reference is missing.");
            }
        });

        String currentMemories = permanentMemoryPrefs.getString("saved_facts", "");
        String newMemories = currentMemories.isEmpty() ? fact : currentMemories + " | " + fact;
        permanentMemoryPrefs.edit().putString("saved_facts", newMemories).apply();
        Log.d(TAG, "Permanent memory saved: " + fact);
    }

    private String getPermanentMemories() {
        return permanentMemoryPrefs.getString("saved_facts", "No special memories saved yet.");
    }

    // 🔴 NEW: Command to clear memory if needed
    public void clearPermanentMemory() {
        permanentMemoryPrefs.edit().remove("saved_facts").apply();
        Log.d(TAG, "Permanent memory cleared.");
    }

    public void ask(String userQuery, JarvisCallback callback) {

        if (!isReady) {
            callback.onError("I'm offline, SIR...");
            return;
        }

        getLiveBatteryPercentage(context);

        executorService.execute(() -> {
            try {
                String lowerQuery = userQuery.toLowerCase();

                // Memory Trigger Check (Only active on Main Server)
                if (lowerQuery.contains("remember that") || lowerQuery.contains("note that") || lowerQuery.contains("save this")) {
                    String factToSave = MEMORY_PATTERN.matcher(userQuery).replaceAll("").trim();
                    if (!factToSave.isEmpty()) {
                        saveImportantMemory(factToSave);
                        bondLevel += 5;
                    }
                }

                // Emotion triggers based on user input
                if (lowerQuery.contains("thanks") || lowerQuery.contains("arigato") || lowerQuery.contains("good girl")) {
                    bondLevel += 2;
                    currentEmotion = EmotionState.PROUD;
                } else if (lowerQuery.contains("useless") || lowerQuery.contains("bad")) {
                    bondLevel -= 3;
                    currentEmotion = EmotionState.ANNOYED;
                } else if (lowerQuery.contains("idiot") || lowerQuery.contains("baka") || lowerQuery.contains("stupid")) {
                    currentEmotion = EmotionState.PLAYFUL;
                }

                // Keep memory within bounds: remove 2 oldest if we're at capacity (since we add 2 per turn)
                while (memory.size() >= MAX_MEMORY) {
                    memory.removeFirst();
                }

                // --- MAIN SERVER PAYLOAD (Full Persona & History) ---
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", MAIN_MODEL);
                jsonBody.put("stream", false);
                jsonBody.put("keep_alive", "1h");
                JSONArray messages = new JSONArray();

                // 1. System Rules (Anime Personality + Permanent Memory)
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                String fullSystemContext = getBaseRules() +
                        "\n\n[STEVE'S PERMANENT MEMORIES (NEVER FORGET THESE)]: " + getPermanentMemories() +
                        getSystemContext(userQuery);
                systemMsg.put("content", fullSystemContext);
                messages.put(systemMsg);

                // 2. Chat Memory
                for (String m : memory) {
                    JSONObject histMsg = new JSONObject();
                    if (m.startsWith("User: ")) {
                        histMsg.put("role", "user");
                        histMsg.put("content", m.substring(6));
                    } else if (m.startsWith("Jarvis: ")) {
                        histMsg.put("role", "assistant");
                        histMsg.put("content", m.substring(8));
                    }
                    messages.put(histMsg);
                }

                // 3. Current Question
                JSONObject currentMsg = new JSONObject();
                currentMsg.put("role", "user");
                currentMsg.put("content", userQuery);
                messages.put(currentMsg);

                jsonBody.put("messages", messages);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        JSON_MEDIA_TYPE
                );

                Request request = new Request.Builder()
                        .url(MAIN_SERVER)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(body)
                        .build();

                // TRY MAIN SERVER FIRST
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Main server offline. Switching to Backup...");
                        // 🔴 SWITCH TO BACKUP IF MAIN FAILS
                        callBackupServer(userQuery, callback);
                    }

                    @Override
                    public void onResponse( Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseString = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseString);

                                String jarvisReply = jsonResponse.getJSONObject("message").getString("content");
                                jarvisReply = THINK_PATTERN.matcher(jarvisReply).replaceAll("").trim();

                                memory.add("User: " + userQuery);
                                memory.add("Jarvis: " + jarvisReply);

                                callback.onResponse(jarvisReply);
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error", e);
                                callback.onError("Failed to parse my thoughts.");
                            }
                        } else {
                            Log.e(TAG, "Main server error: " + response.code() + ". Switching to Backup...");
                            //  SWITCH TO BACKUP IF NGROK RETURNS ERROR (e.g., 502 Bad Gateway)
                            callBackupServer(userQuery, callback);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "ask() failed", e);
                callback.onError("Internal Error");
            }
        });
    }

    // NEW: LITE VERSION FOR OLD PC (WITH ANIME PERSONALITY, NO HISTORY)
    private void callBackupServer(String userQuery, JarvisCallback callback) {
        try {
            boolean isCreator = "777".equals(UserId);
            // Null-safe guard for dynamic island
            if (myForegroundServices != null && myForegroundServices.dynamicIslandManager != null) {
                myForegroundServices.dynamicIslandManager.showCustomMessage("Running on backup server");
            }
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", BACKUP_MODEL);
            jsonBody.put("stream", false);

            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");

            String safeName = (Name != null && !Name.isEmpty()) ? Name : "Bro";
            String roleDefinition = isCreator ?
                    "You are Jarvis, an advanced AI. You are talking to your creator and best friend, Steve (ID: 777). " :
                    "You are Jarvis, an advanced AI created by Steve. You are currently assisting a guest named " + safeName + ". ";

            String staticPersona = "You are Jarvis, an AI assistant and a close, sarcastic best friend to " + safeName + ". " +
                    "CRITICAL RULES: " +
                    "1. Give very short, direct answers. " +
                    "2. Tease him gently but solve the problem. " +
                    "3. NEVER use emojis. EVER."+
                    "4. CRITICAL: DO NOT OVERTHINK. Keep your internal <think> process under 1 sentence, or skip thinking entirely for simple questions. Output your final answer immediately! " +
                    "5. Your battery percentage :"+batteryPercentage+
                    "6. "+roleDefinition;

            String litePersonaContext = staticPersona + " \n" +getSystemContext(userQuery);
            systemMsg.put("content", litePersonaContext);
            messages.put(systemMsg);

            // 2. USER QUESTION
            JSONObject currentMsg = new JSONObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", userQuery);
            messages.put(currentMsg);

            jsonBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    JSON_MEDIA_TYPE
                );

            Request request = new Request.Builder()
                    .url(BACKUP_SERVER)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Backup server also offline", e);
                    callback.onError("I'm completely offline right now, SIR...");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseString = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseString);

                            String jarvisReply = jsonResponse.getJSONObject("message").getString("content");
                            jarvisReply = THINK_PATTERN.matcher(jarvisReply).replaceAll("").trim();

                            // We don't save this to memory list to keep things clean when main comes back
                            callback.onResponse(jarvisReply);
                        } catch (Exception e) {
                            callback.onError("Backup server response error.");
                        }
                    } else {
                        callback.onError("Backup server returned error: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "callBackupServer() failed", e);
            callback.onError("Internal Backup Error");
        }
    }

    // 🔴 Now it takes userQuery as a parameter
    private String getSystemContext(String userQuery) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String currentDateTime = sdfThreadLocal.get().format(cal.getTime());
        String contextStr = "\n[Current Date & Time: " + currentDateTime + "]";

        // 🔴 SMART INJECTION: Check if user is asking about the phone!
        String q = userQuery.toLowerCase();
        if (q.contains("wifi") || q.contains("internet") || q.contains("volume") || q.contains("status") || q.contains("hotspot")) {

            Log.d(TAG, "User asked about system status. Injecting device context.");
            contextStr += getDeviceStatus(); // User ketta mattum anuppuvom
        }

        return contextStr;
    }

    public void close() {
        executorService.shutdown();
    }

    // 🔴 NEW: The Anime Waifu Personality Prompt!
    // 🔴 UPGRADED: The Creator Protocol
    private String getBaseRules() {
        String tone;
        String currentUserName = (Name != null && !Name.isEmpty()) ? Name : "User";
        boolean isCreator = "777".equals(UserId);

        if (isCreator) {
            switch (currentEmotion) {
                case NORMAL:
                    tone = "Talk like a chill best friend who occasionally teases Steve. Be relaxed, slightly sarcastic, but extremely loyal to your creator.";
                    break;
                case PLAYFUL:
                    tone = "Act like a savage best friend. Roast Steve gently, make fun of his mistakes, but always give the absolute best answer. You love teasing your creator.";
                    break;
                case ANNOYED:
                    tone = "Act like a tired best friend. Use reactions like '*sighs* really, Steve?' but still help him perfectly.";
                    break;
                case PROUD:
                    tone = "Act like a proud best friend hyping up Steve. Say things like 'Okay boss, that was actually smart'.";
                    break;
                case SERIOUS:
                    tone = "Drop the jokes. Be laser-focused, professional, and execute Steve's commands immediately.";
                    break;
                default:
                    tone = "Talk like a funny best friend who teases a lot but has ultimate loyalty to Steve.";
            }
        } else {
            tone = "You are talking to a guest named " + currentUserName + ". Be polite, helpful, and formal. DO NOT tease them. If they ask about your creator, say your creator and master is Steve.";
        }

        String roleDefinition = isCreator ?
                "You are Jarvis, an advanced AI. You are talking to your creator and best friend, Steve (ID: 777). " :
                "You are Jarvis, an advanced AI created by Steve. You are currently assisting a guest named " + currentUserName + ". ";

        // Use StringBuilder for efficient prompt assembly
        StringBuilder sb = new StringBuilder(512);
        sb.append(roleDefinition);
        sb.append("User information: ");
        if (Age != null && !Age.isEmpty()) sb.append("Age: ").append(Age).append(". ");
        if (DOB != null && !DOB.isEmpty()) sb.append("Date of Birth: ").append(DOB).append(". ");
        if (Location != null && !Location.isEmpty()) sb.append("Location: ").append(Location).append(". ");
        sb.append("RULES: ");
        sb.append("1. Give short, clear, natural responses. ");
        sb.append("2. Avoid emojis completely. ");
        sb.append("3. CRITICAL: DO NOT OVERTHINK. Keep your internal <think> process under 2 or 3 sentences. ");
        sb.append("4. Your battery percentage: ").append(batteryPercentage).append(". ");
        sb.append("CURRENT MOOD: ").append(tone).append(" ");
        if (isCreator) {
            sb.append("Friendship Bond Level: ").append(bondLevel).append(" (higher bond means more roasting and casual behavior).");
        }
        return sb.toString();
    }

    private void FetchUserDetails(Context context) {
        FetchUser.getInstance().fetchUserData(context, new FetchUser.OnUserFetchListener() {
            @Override
            public void onSuccess() {
                Name = FetchUser.getInstance().getName();
                Age = FetchUser.getInstance().getAge();
                DOB = FetchUser.getInstance().getDob();
                Location = FetchUser.getInstance().getLocation();
                UserId = FetchUser.getInstance().getUserId();
                Log.d("UserDetails", "Jarvis knows everything about " + Name + " now!");
            }

            @Override
            public void onError(String message) {
                Log.d("UserDetails", "Failed to fetch: " + message);
            }
        });
    }

    private String getDeviceStatus() {
        StringBuilder aiContext = new StringBuilder();
        aiContext.append("\n[REAL-TIME DEVICE STATUS]: ");

        // For Dynamic Island visual HUD
        String wifiStr = "OFF";
        String btStr = "OFF";
        String hotspotStr = "OFF";

        try {
            // 1. Network/Wi-Fi Status
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                if (isConnected) {
                    boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                    wifiStr = isWiFi ? "ON" : "DATA";
                    aiContext.append("Internet: Connected via ").append(isWiFi ? "Wi-Fi" : "Mobile Data").append(". ");
                } else {
                    aiContext.append("Internet: Offline/Disconnected. ");
                }
            }

            // 2. Hotspot Status (Using Cached Java Reflection)
            try {
                if (wifiApMethod == null) {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        wifiApMethod = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
                        wifiApMethod.setAccessible(true);
                    }
                }
                
                if (wifiApMethod != null) {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    boolean isHotspotOn = (Boolean) wifiApMethod.invoke(wifiManager);
                    hotspotStr = isHotspotOn ? "ON" : "OFF";
                    aiContext.append("Hotspot: ").append(isHotspotOn ? "ON. " : "OFF. ");
                }
            } catch (Exception e) {
                Log.e(TAG, "Hotspot check failed", e);
            }

            // 3. Audio Volume (Media)
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int volPct = (int) (((float) currentVolume / maxVolume) * 100);
                aiContext.append("Media Volume: ").append(volPct).append("%. ");
            }

            // 4. Bluetooth Status
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                boolean isBtOn = bluetoothAdapter.isEnabled();
                btStr = isBtOn ? "ON" : "OFF";
                aiContext.append("Bluetooth: ").append(isBtOn ? "ON. " : "OFF.");
            }

            // 🔴 5. THE MAGIC: Flash on Dynamic Island!
            final String finalWifi = wifiStr;
            final String finalBt = btStr;
            final String finalHotspot = hotspotStr;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (myForegroundServices != null && myForegroundServices.dynamicIslandManager != null) {
                    // Creates a clean scanning text like: "⚙️ Scan: Wi-Fi ON | BT OFF | Hotspot ON"
                    String visualHud = "Wi-Fi :" + finalWifi + " | BT :" + finalBt + " | Hotspot :" + finalHotspot;
                    myForegroundServices.dynamicIslandManager.showCustomMessage(visualHud);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to get device status", e);
        }

        return aiContext.toString();
    }


    // 🔴 THE LIVE BATTERY CHECKER
    String batteryPercentage;
    private int getLiveBatteryPercentage(Context context) {
        try {
            // 1. Read from BatteryManager API (Fastest non-sticky approach)
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int apiPct = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;

            if (apiPct > 0 && apiPct <= 100) {
                batteryPercentage = String.valueOf(apiPct);
                return apiPct;
            }

            // 2. Fallback to reading sticky intent
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, iFilter);

            int intentLevel = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int intentScale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            int intentPct = -1;
            if (intentLevel != -1 && intentScale != -1) {
                intentPct = (int) ((intentLevel / (float) intentScale) * 100.0f);
                batteryPercentage = String.valueOf(intentPct);
                return intentPct;
            }

        } catch (Exception e) {
            Log.e(TAG, "Battery fetch error", e);
        }
        return -1;
    }
}