package com.example.translateanywhere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationReader extends NotificationListenerService {
    String sender;
    String message;

    public void onNotificationPosted(StatusBarNotification sbn ) {
        super.onNotificationPosted(sbn);
        if (sbn.getNotification().extras != null) {
            if(sbn.getPackageName().equals("com.whatsapp") || sbn.getPackageName().equals("com.instagram.android")) {
                String m=sbn.getNotification().extras.getString("android.text", "");
                String s=sbn.getNotification().extras.getString("android.title", "");
                if(!m.contains("messages from") || !m.contains("chats WhatsApp") || !s.contains("messages from") || !s.contains("chats WhatsApp") ) {
                    if(m.contains("messages")){
                        return;
                    }
                    message = m;
                    sender = s;
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("sender", sender);
                    editor.putString("message", message);
                    editor.apply();

                }
            }
        }else {
            Log.d("SBN","is empty");
        }
        Log.d("FromThis", message + ":" + sender);

    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("WhatsAppMessage", "Notification Removed: " + sbn.getPackageName());
    }
}

