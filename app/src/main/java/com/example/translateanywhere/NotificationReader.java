package com.example.translateanywhere;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationReader extends NotificationListenerService {
    String sender;
    String message;

    private OkHttpClient client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String NGROK_URL = "https://stably-oversusceptible-anne.ngrok-free.dev/api/chat";
    private static final String OLLAMA_MODEL = "deepseek-r1:8b";

    Map<String, List<String>> conversationMap = new HashMap<>();
    String notifKey, previousMessage = "  ";
    boolean isFeatureEnabled; // Master switch from UI
    Set<String> repliedKeys = new HashSet<>();

    List<String> conversationHistory;
    TextToSpeech toSpeech;
    Set<String> sentReplies = new HashSet<>();

    // 🔴 THE MAGIC TOGGLES FOR DYNAMIC ISLAND & VOICE CONTROL
    public static boolean isAutoReplyEnabled = false; // Voice command controls this!
    public static String unreadSender = "";
    public static String unreadMessage = "";
    public static boolean hasUnreadMessage = false;

    @Override
    public void onCreate() {
        super.onCreate();

        client = new OkHttpClient.Builder()
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .build();

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

        // Master switch from app settings
        SharedPreferences sharedPreferences = getSharedPreferences("Jarvis", MODE_PRIVATE);
        isFeatureEnabled = sharedPreferences.getBoolean("isFeatureEnabled", false);
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Log.d("NotificationReader", ">>> Notification Received from: " + sbn.getPackageName() + " <<<");


        if (sbn.getNotification().extras != null) {
            String packageName = sbn.getPackageName();

            if (packageName.equals("com.instagram.android")) {
                String m = sbn.getNotification().extras.getString("android.text", "");
                String s = sbn.getNotification().extras.getString("android.title", "");

                Log.d("NotificationReader", "Insta Title: " + s + ", Message: " + m);

                // 🔴 LOOP BLOCKER 1: Ignore if the sender is "Steve" or "You"
                String myName = FetchUser.getInstance().isLoaded() ? FetchUser.getInstance().getName() : "Steve";
                if (s != null && (s.equalsIgnoreCase(myName) || s.equalsIgnoreCase("Steve") || s.contains("You"))) {
                    Log.d("LoopBlocker", "Ignored my own notification: " + s);
                    return;
                }

                // 🔴 LOOP BLOCKER 2: Ignore if the message is exactly what Jarvis just sent
                if (sentReplies.contains(m)) {
                    Log.d("LoopBlocker", "Ignored echoed message (Jarvis talking to himself)!");
                    return;
                }

                if (m.startsWith("Jarvis:") || m.contains("Booyah") || m.contains("Steve's laid-back")) {
                    return;
                }

                // Insta specific ignore words
                if ((m.contains("messages from") || m.contains("reels") || m.contains("reel")) || m.contains("Liked") || m.contains("Reacted") || m.contains("sent an attachment")) {
                    return;
                }

                if (m.equals(previousMessage) || m.contains("messages")) {
                    return;
                }

                message = m;
                sender = s;
                notifKey = sbn.getKey();

                if (isAutoReplyEnabled) {
                    if (repliedKeys.contains(notifKey)) {
                        return;
                    }

                    if (message.toLowerCase().contains("emergency") || message.toLowerCase().contains("important")) {
                        notifyImportance(sender);
                    }

                    repliedKeys.add(notifKey);
                    createJarvisReply(sender, message, sbn);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> repliedKeys.remove(notifKey), 5000);
                } else {
                    unreadSender = sender;
                    unreadMessage = message;
                    hasUnreadMessage = true;
                    Log.d("NotificationReader", "Auto reply OFF. Saved in RAM for Island.");

                    if (message.toLowerCase().contains("emergency") || message.toLowerCase().contains("important")) {
                        notifyImportance(sender);
                    }
                }
            }
        }
    }

    // Chinnatha intha log message-aiyum update pannikonga
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("NotificationReader", "Notification Removed: " + sbn.getPackageName());
    }

    private void createJarvisReply(String sender, String Message, StatusBarNotification sbn1) {
        conversationHistory = conversationMap.getOrDefault(sender, new ArrayList<>());

        // Dynamically fetch Steve's details using our Singleton
        String userName = FetchUser.getInstance().isLoaded() ? FetchUser.getInstance().getName() : "Steve";
        String userDob = FetchUser.getInstance().isLoaded() ? FetchUser.getInstance().getDob() : "21-03-2005";
        String location = FetchUser.getInstance().isLoaded() ? FetchUser.getInstance().getLocation() : "Cheyyar";

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are Jarvis, a laid-back, sarcastic personal assistant AI created by ").append(userName).append(".\n");
        systemPrompt.append(userName).append("'s DOB is ").append(userDob).append(" and he is currently in ").append(location).append(".\n");

        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        systemPrompt.append("Current time is: ").append(currentTime).append("\n\n");

        systemPrompt.append("CRITICAL RULES FOR REPLYING:\n")
                .append("1. Respond in a casual, short, and witty style (max 2 lines).\n")
                .append("2. Sprinkle a bit of teasing or humor on ").append(userName).append(", but stay friendly.\n")
                .append("3. Use emojis naturally (max 1 or 2 per reply).\n")
                .append("4. If the message sounds urgent/important, ONLY say: 'I am informed to ").append(userName).append(", he will get back to you.'\n")
                .append("5. If they ask a doubt, give a clear, simple answer.\n")
                .append("6. DO NOT sound formal or robotic. Act like a cool AI answering on behalf of your creator.\n");

        executorService.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", OLLAMA_MODEL);
                jsonBody.put("stream", false);

                JSONArray messages = new JSONArray();

                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt.toString());
                messages.put(systemMsg);

                // Add Conversation History
                for (String entry : conversationHistory) {
                    JSONObject histMsg = new JSONObject();
                    if (entry.startsWith("User: ")) {
                        histMsg.put("role", "user");
                        histMsg.put("content", entry.substring(6));
                    } else if (entry.startsWith("Jarvis: ")) {
                        histMsg.put("role", "assistant");
                        histMsg.put("content", entry.substring(8));
                    }
                    messages.put(histMsg);
                }

                // Add current message
                JSONObject currentMsg = new JSONObject();
                currentMsg.put("role", "user");
                currentMsg.put("content", "Message from " + sender + ": \"" + Message + "\"");
                messages.put(currentMsg);

                jsonBody.put("messages", messages);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(NGROK_URL)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("NotificationReader", "API call failed", e);
                        alterstring("I am having network issues, Steve will reply later.", sbn1, Message);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseString = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseString);
                                String jarvisReply = jsonResponse.getJSONObject("message").getString("content");

                                jarvisReply = jarvisReply.replaceAll("(?s)<think>.*?</think>", "").trim();

                                if (conversationHistory.size() >= 6) {
                                    conversationHistory.remove(0);
                                    conversationHistory.remove(0);
                                }
                                conversationHistory.add("User: " + Message);

                                alterstring(jarvisReply, sbn1, Message);

                            } catch (Exception e) {
                                alterstring("Error processing reply. Steve will check it out.", sbn1, Message);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("NotificationReader", "Failed to build request", e);
            }
        });
    }

    // ✅ FIXED: Clean and exact Auto Reply method
    private void sendAutoReply(Notification notification, String jarvisResponse, StatusBarNotification sbn) {
        if (notification == null || notification.actions == null) return;

        for (Notification.Action action : notification.actions) {
            android.app.RemoteInput[] remoteInputs = action.getRemoteInputs();
            if (remoteInputs != null) {
                for (android.app.RemoteInput remoteInput : remoteInputs) {

                    // We found the actual reply input box!
                    Bundle localReply = new Bundle();
                    localReply.putCharSequence(remoteInput.getResultKey(), jarvisResponse);

                    android.app.RemoteInput[] inputs = new android.app.RemoteInput[1];
                    inputs[0] = remoteInput;

                    Intent localIntent = new Intent();
                    android.app.RemoteInput.addResultsToIntent(inputs, localIntent, localReply);

                    try {
                        action.actionIntent.send(this, 0, localIntent);

                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            cancelNotification(sbn.getKey());
                        }
                        Log.d("JarvisAutoReply", "Successfully sent: " + jarvisResponse);
                        return; // Done sending, exit loop immediately!

                    } catch (PendingIntent.CanceledException e) {
                        Log.e("ReplyError", "Failed to send auto reply", e);
                    }
                }
            }
        }
    }

    private void alterstring(String foralter, StatusBarNotification sbn, String originalMessage) {
        String altered = foralter.replace("*", "")
                .replace("As a large language model", "I am Jarvis")
                .replace("Jarvis:", "")
                .replace("User:", "")
                .replace("TikTok", "Instagram")
                .trim();

        conversationHistory.add("Jarvis: " + altered);
        conversationMap.put(sender, conversationHistory);

        previousMessage = originalMessage;

        // 🔴 LOOP BLOCKER 3: Save what we just sent so we don't reply to it again!
        sentReplies.add(altered);

        // Clear old memory so it doesn't slow down the phone
        if (sentReplies.size() > 20) {
            sentReplies.clear();
        }

        Log.d("Jarvis Response", altered);
        sendAutoReply(sbn.getNotification(), altered, sbn);
    }

    private void notifyImportance(String sender) {
        String safeName = FetchUser.getInstance().isLoaded() ? FetchUser.getInstance().getName() : "Steve";
        String speakText = "Hey " + safeName + ", you might want to check messages from "
                + sender.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")
                + ". They mentioned something important.";

        toSpeech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void destroy() {
        if (toSpeech != null) {
            toSpeech.stop();
            toSpeech.shutdown();
        }
        isAutoReplyEnabled=false;
        stopSelf();
    }
}