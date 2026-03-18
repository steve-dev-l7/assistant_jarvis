package com.example.translateanywhere;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
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
    Set<String> sentReplies = new HashSet<>();

    // 🔴 THE MAGIC TOGGLES FOR DYNAMIC ISLAND & VOICE CONTROL
    public static boolean isAutoReplyEnabled = false; // Voice command controls this!
    public static String unreadSender = "";
    public static String unreadMessage = "";
    public static boolean hasUnreadMessage = false;

    MyForegroundServices myForegroundServices;

    @Override
    public void onCreate() {
        super.onCreate();

        client = new OkHttpClient.Builder()
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .build();

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

            if ("com.google.android.apps.messaging".equals(packageName) || "com.android.mms".equals(packageName)) {
                Bundle extras = sbn.getNotification().extras;
                if (extras != null) {
                    // In notifications, 'title' is the sender name/number, 'text' is the message
                    String senderNumber = extras.getString("android.title");
                    CharSequence messageCharSeq = extras.getCharSequence("android.text");

                    if (senderNumber != null && messageCharSeq != null) {
                        String messageBody = messageCharSeq.toString();
                        Log.d("NotificationReader", "🚨 SMS INTERCEPTED! Sender: " + senderNumber + " | Msg: " + messageBody);

                        // Process the name saving logic
                        checkAndSaveContact(getApplicationContext(), senderNumber, messageBody);
                    }
                }
            }

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

    private void checkAndSaveContact(Context context, String senderNumber, String messageBody) {
        SharedPreferences prefs = context.getSharedPreferences("JarvisMemory", Context.MODE_PRIVATE);

        String cleanNumber = senderNumber.replaceAll("[^0-9]", "");
        String lookupKey = cleanNumber;
        if (lookupKey.length() > 10) {
            lookupKey = lookupKey.substring(lookupKey.length() - 10);
        }

        boolean isWaitingForName = prefs.getBoolean("waiting_for_name_" + lookupKey, false);

        if (isWaitingForName) {
            Log.d("JarvisSmsObserver", "Reply received for name request from " + lookupKey + ": " + messageBody);
            String newContactName = messageBody.trim();
            
            // Use the full cleanNumber (including country code if present) for saving to contacts
            saveNewContact(context, newContactName, cleanNumber);
            
            prefs.edit().remove("waiting_for_name_" + lookupKey).apply();
            sendThankYouSMS(senderNumber, newContactName);
            Log.d("JarvisSmsObserver", "Successfully saved contact: " + newContactName + " with number: " + cleanNumber);
        } else {
        Log.d("JarvisSmsObserver", "Normal SMS from " + cleanNumber + ": " + messageBody);

        if (messageBody != null) {
            // 1. Check if the message actually contains OTP-related words
            String lowerMsg = messageBody.toLowerCase();
            if (lowerMsg.contains("otp") || lowerMsg.contains("code") || lowerMsg.contains("pin") || lowerMsg.contains("verification")) {

                // 2. REGEX MAGIC: Find any 4 to 8-digit number in the text
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{4,8}\\b");
                java.util.regex.Matcher matcher = pattern.matcher(messageBody);

                if (matcher.find()) {
                    String extractedOTP = matcher.group(); // The exact OTP number

                    // 3. Copy to Clipboard
                    android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Jarvis OTP", extractedOTP);
                    if (clipboardManager != null) {
                        clipboardManager.setPrimaryClip(clip);
                    }

                    Log.d("JarvisSmsObserver", "🚨 OTP Extracted & Copied: " + extractedOTP);

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MyForegroundServices.instance.dynamicIslandManager.showCustomMessage("📋 OTP Copied: " + extractedOTP);
                        }
                    },100);

                }
            }
        }
    }
    }



    private void sendThankYouSMS(String phoneNumber, String name) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String replyMessage = "Thank you, " + name + ". I have successfully saved your contact in my device.";
            ArrayList<String> parts = smsManager.divideMessage(replyMessage);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        } catch (Exception e) {
            Log.e("JarvisSmsObserver", "Failed to send SMS", e);
        }
    }

    private void saveNewContact(Context context, String name, String phoneNumber) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int rawContactInsertIndex = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            Log.e("JarvisSmsObserver", "Failed to save contact", e);
        }
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

        myForegroundServices.speakAndLog(speakText, null);
    }

    public void destroy() {

        isAutoReplyEnabled=false;
        stopSelf();
    }
}