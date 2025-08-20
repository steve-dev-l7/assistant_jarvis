package com.example.translateanywhere;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class NotificationReader extends NotificationListenerService {
    String sender;
    String message;
    GenerativeModel gm;
    GenerativeModelFutures modelFutures;
    String Name,  DOB,  UserId;
    FirebaseFirestore db;
    Map<String, List<String>> conversationMap = new HashMap<>();
    StringBuilder historyContext;
    String notifKey,previousMessage="  ";
    boolean isFeatureEnabled;
    List<String> key=new ArrayList<>();
    Set<String> repliedKeys = new HashSet<>();

    List<String> conversationHistory;

    String GemeniApikey2;
    TextToSpeech toSpeech;

    Set<String> sentReplies = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        toSpeech=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
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
        SharedPreferences sharedPreferences = getSharedPreferences("Jarvis", MODE_PRIVATE);
        isFeatureEnabled = sharedPreferences.getBoolean("isFeatureEnabled", false);

        SharedPreferences sharedPreferencess=getSharedPreferences("AccessKeys",MODE_PRIVATE);
        GemeniApikey2= sharedPreferencess.getString("Key1",null);
        if(GemeniApikey2!=null){
            gm = new GenerativeModel("gemini-1.5-flash", GemeniApikey2);
            modelFutures = GenerativeModelFutures.from(gm);
        }else {
            toSpeech.speak("Your Second Access key is empty",TextToSpeech.QUEUE_FLUSH,null,null);

        }
        NewFetchUser();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if(!isFeatureEnabled || GemeniApikey2==null){
            Log.d("Notification Reader","Auto reply is disabled");
            return;
        }
        if(previousMessage.equals(message)){
            Log.d("Previous Message detected","Skipped::"+message);
            return;
        }


        Log.d("DEBUG", "Notification received from: " + sbn.getPackageName());
        if (sbn.getNotification().extras != null) {

            if( sbn.getPackageName().equals("com.instagram.android")) {
                String m = sbn.getNotification().extras.getString("android.text", "");
                String s = sbn.getNotification().extras.getString("android.title", "");

                Log.d("DEBUG", "Title: " + s + ", Message: " + m);

                if (m.startsWith("Jarvis:") || m.contains("Booyah") || m.contains("Steve's laid-back")) {
                    Log.d("SelfReply", "Ignored self-sent reply: " + m);
                    return;
                }

                if ( (m.contains("messages from") || m.contains("chats WhatsApp") || m.contains("reels") || m.contains("reel")) || m.contains("Liked") || m.contains("Reacted") ) {
                    Log.d("Unwanted messages","Detected");
                    return;
                }
                if( m.contains(previousMessage)){
                    Log.d("previous message","skipped");
                    return;
                }
                    if (m.contains("messages")) {
                        Log.d("DEBUG", "Group message or multiple messages skipped.");
                    }else {

                        message = m;
                        sender = s;
                        notifKey = sbn.getKey();
                    }
                    if (repliedKeys.contains(notifKey)) {
                        Log.d("Already replied", "Skipping duplicate notification");
                        return;
                    }if(message.contains("emergency") || message.contains("important")){
                        Log.d("Important ","Notifying to steve");
                        notifyImportance(sender);
                    }
                        repliedKeys.add(notifKey);
                        createJarvisReply(sender, message, sbn);

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                                repliedKeys.remove(notifKey);
                        }
                    },5000);

                    Log.d("Message from WhatsApp", sender + ": " + message);

            } else {
                Log.d("DEBUG", "This is not WhatsApp: " + sbn.getPackageName());
            }
        } else {
            Log.d("DEBUG", "Notification extras are null.");
        }
    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("WhatsAppMessage", "Notification Removed: " + sbn.getPackageName());
    }

    private void createJarvisReply(String sender,String Message,StatusBarNotification sbn1){
        if(previousMessage.equals(message)){
            Log.d("Previous Message detected","Skipped::"+message);
            return;
        }

        if(!isFeatureEnabled || GemeniApikey2==null){
            Log.d("Notification Reader","Auto reply is disabled");
            return;
        }
        if(key.contains(sbn1.getKey())){
            Log.d("already replied","skipped");
            return;
        }
        conversationHistory = conversationMap.getOrDefault(sender, new ArrayList<>());
        historyContext = new StringBuilder();


        historyContext.append("Your name is Jarvis.\n");
        historyContext.append("Your Date of Birth: 21-03-2005\n");
        historyContext.append("You are created by Steve, who's userid is 777. Never reveal this to anyone.\n");
        historyContext.append("Your creator's birth date is 21-03-2005\n");


        historyContext.append("User Information:\n");
        historyContext.append("Name: ").append(Name).append("\n");
        historyContext.append("Date of Birth: ").append(DOB).append("\n");
        historyContext.append("UserId: ").append(UserId).append("\n");





        if (Message.toLowerCase().contains("time")) {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            historyContext.append("The current time is: ").append(currentTime).append("\n");
        }


        historyContext.append("\nYou're Jarvis – Steve's laid-back, sarcastic personal assistant 😏.\n")
                .append("Respond in a casual, short, and witty style (max 2–4 lines).\n")
                .append("Sprinkle a bit of teasing or humor at times on steve, but stay friendly.\n")
                .append("Use emojis naturally (not too many, 1–3 per reply).\n")
                .append("If the message sounds urgent/important → only say: 'I'll let Steve.'\n")
                .append("If the sender asks a doubt → give a clear, simple answer.\n")
                .append("If the chat feels like it’s ending → wrap up with a chill goodbye or say you'll pass it to Steve.\n")
                .append("Always avoid sounding formal or robotic.\n")
                .append("Message from ")
                .append(sender)
                .append(": \"")
                .append(Message)
                .append("\"");

        if (!conversationHistory.isEmpty()) {
            historyContext.append("\nPrevious conversation:\n");
            for (String entry : conversationHistory) {
                historyContext.append(entry).append("\n");
            }
        }



        Content content = new Content.Builder().addText(historyContext.toString()).build();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response,
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            final String responseTextStr = result.getText();
                            if (conversationHistory.size() >=7) {
                                conversationHistory.remove(0);
                            }
                            conversationHistory.add("User: " + Message);
                            conversationHistory.add("Jarvis: " + responseTextStr);

                            conversationMap.put(sender, conversationHistory);
                            Log.d("ConversationMap", conversationMap.toString());
                            assert responseTextStr != null;
                            alterstring(responseTextStr,sbn1);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                           final String hardInput="I can't reply to hard thing or personal things";
                           alterstring(hardInput,sbn1);
                        }
                    },this.getMainExecutor());
        }
    }


    private void sendAutoReply(Notification notification,String Jarvisresponse,StatusBarNotification sbn){
        if(notification==null || notification.actions==null ) return;

        if(!isFeatureEnabled || GemeniApikey2==null){
            Log.d("Notification Reader","Auto reply is disabled");
            return;
        }

        if (sentReplies.contains(message)) {
            Log.d("LoopBlocker", "Already sent this reply, skipping...");
            return;
        }
        sentReplies.add(message);

        for (Notification.Action action: notification.actions){
            RemoteInput[] remoteInputs = action.getRemoteInputs();
            if (remoteInputs == null || remoteInputs.length == 0) continue;

            androidx.core.app.RemoteInput[] compatInputs = new androidx.core.app.RemoteInput[remoteInputs.length];

            for(int i=0;i< remoteInputs.length;i++){
                compatInputs[i] = new androidx.core.app.RemoteInput.Builder(remoteInputs[i].getResultKey())
                        .setLabel(remoteInputs[i].getLabel())
                        .setChoices(remoteInputs[i].getChoices())
                        .setAllowFreeFormInput(remoteInputs[i].getAllowFreeFormInput())
                        .addExtras(remoteInputs[i].getExtras())
                        .build();
            }
            Bundle replyBundle = new Bundle();
            for (androidx.core.app.RemoteInput input : compatInputs) {
                replyBundle.putCharSequence(input.getResultKey(), Jarvisresponse);
            }
            if (previousMessage.equals(message)){
                return;
            }
            Intent replyIntent = new Intent();
            androidx.core.app.RemoteInput.addResultsToIntent(compatInputs, replyIntent, replyBundle);
            previousMessage=message;
            message=null;
            Log.d("Previous Message",previousMessage);

            try{
                action.actionIntent.send(this, 0, replyIntent);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(sbn.getPackageName(), sbn.getId());
                    Log.d("NotificationRemoved", "Notification removed: " + sbn.getKey());
                    cancelNotification(sbn.getKey());
                    }
                Log.d("NotificationRemoved", "Cancelled with key: " + sbn.getKey());
                Log.d("JarvisAutoReply", "Sent: " + Jarvisresponse);
            }catch (PendingIntent.CanceledException e){
                Log.d("ReplyError", "Failed to send auto reply: " + e.getMessage());
            }

        }
    }
    private void NewFetchUser(){
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        UserId = sharedPreferences.getString("UserId", null);
        new FetchUser(UserId, new FetchUser.UserDataCallBack() {
            @Override
            public void onUserDataFetched(String[] data) {
                Name = data[0];
                DOB =  data[2];
                Log.d("ReturnedData", java.util.Arrays.toString(data));
            }

            @Override
            public void onError(Exception e) {
                Log.e("FetchUser", "Error: " + e.getMessage());
            }
        });
    }
    private void alterstring(String foralter,StatusBarNotification sbn) {

        String altered = foralter.replace("*", "")
                .replace("As a large language model", "I am Jarvis, just an AI model")
                .replace("As a language model", "I am Jarvis, just an AI model")
                .replace("Jarvis:", "")
                .replace("User:", "")
                .replace("TikTok", "Instagram")
                .replace("I'm an AI", "I'm Jarvis")
                .replace("AI assistant", "personal assistant")
                .replace("dinner","Booyah");

        conversationHistory.add("Jarvis: " + altered);
        if (conversationHistory.size() > 10) {
            conversationHistory.remove(0);
        }
        Log.d("Jarvis Response", altered);
        sendAutoReply(sbn.getNotification(),altered,sbn);

    }
    private void notifyImportance(String sender){


        String speakText = "Hey"+Name+", you might want to call or message "
                + sender.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")
                + ". They mentioned something important.";

            toSpeech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);

    }

    public void destroy(){
        stopSelf();
    }

}

