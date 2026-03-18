package com.example.translateanywhere;



import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CallListener extends PhoneStateListener {
    final Context context;
    private String lastIncomingNumber = null;
    private final Map<String, Integer> callCountMap = new HashMap<>();


    String Name;

    String msg;

    MyForegroundServices myForegroundServices;

    FetchUser fetchUser;
    public CallListener(Context context) {
        this.context = context;
        myForegroundServices=new MyForegroundServices();
    }





    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        super.onCallStateChanged(state, phoneNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:

                SharedPreferences userData = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
                Name = userData.getString("UserName", "Commander");

                msg="Hello, this is Jarvis.  "+Name+"  is currently unavailable. Your repeated call has been noted, and he’ll / she'll get back to you as soon as possible.";

                lastIncomingNumber = phoneNumber;

                int count = callCountMap.getOrDefault(phoneNumber, 0) + 1;
                callCountMap.put(phoneNumber, count);

                Log.d("CallListener", "Incoming call from: " + phoneNumber + ", Count: " + count);
                isNumberSavedInContacts(context,lastIncomingNumber);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:

                lastIncomingNumber=null;
                break;


            case TelephonyManager.CALL_STATE_IDLE:
                if(lastIncomingNumber!=null){
                    int counts;
                    counts = callCountMap.getOrDefault(lastIncomingNumber,0);

                    if(counts%2==0){
                        sendSMS(lastIncomingNumber,msg);
                        Log.d("CallListener","Sending SMS");
                    }

                    if(!isNumberSavedInContacts(context, lastIncomingNumber)){
                        Log.d("CallListener","Unknown caller: " + lastIncomingNumber + ". Asking for name.");
                        String message = "Hey this is Jarvis. " + Name + " forgot to save your number. May I know your name? Just reply with your name without any extra words.";
                        sendSMS(lastIncomingNumber, message);

                        SharedPreferences prefs = context.getSharedPreferences("JarvisMemory", Context.MODE_PRIVATE);

                        // FIX: Clean the number but keep the full digits for state identification
                        String cleanNumber = lastIncomingNumber.replaceAll("[^0-9]", "");
                        if (cleanNumber.length() > 10) {
                            cleanNumber = cleanNumber.substring(cleanNumber.length() - 10);
                        }

                        prefs.edit().putBoolean("waiting_for_name_" + cleanNumber, true).apply();
                        Log.d("CallListener", "Waiting for name from memory key (10-digit): " + cleanNumber);
                    }
                }
        }

    }

    private void sendSMS(String number, String message){
        try {
            Log.d("CallListener","Sending Sms to "+number);
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, parts, null, null);
        }catch (Exception e){
            Log.d("CallListener",e.getMessage());
        }

    }

    @SuppressLint("Range")
    private boolean isNumberSavedInContacts(Context context,String phoneNumber) {
        if(phoneNumber == null || phoneNumber.isEmpty()) return false;

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        String[] projection =new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME};

        try(Cursor cursor = context.getContentResolver().query(uri,projection,null,null,null)){
            if(cursor!=null && cursor.moveToFirst()){
                 String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                 myForegroundServices.speakAndLog(name+" Calling",null);

                 return true;
            }
        }catch (Exception e){
            Log.d("JarvisEngine", "Error checking contact", e);
        }
        Log.d("CallListener","Number not saved in contacts");
        return false;
    }
}

