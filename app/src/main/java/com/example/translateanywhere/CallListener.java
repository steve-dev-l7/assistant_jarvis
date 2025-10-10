package com.example.translateanywhere;



import android.content.Context;
import android.content.SharedPreferences;
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
    public CallListener(Context context) {
        this.context = context;
    }





    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        super.onCallStateChanged(state, phoneNumber);




        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
                FetchUser fetchUser=new FetchUser(sharedPreferences.getString("UserId", null), new FetchUser.UserDataCallBack() {
                    @Override
                    public void onUserDataFetched(String[] data) {
                        Name=data[0];
                    }

                    @Override
                    public void onError(Exception e) {
                        Name="null";
                    }
                });


                msg="Hello, this is Jarvis.  "+Name+"  is currently unavailable. Your repeated call has been noted, and he’ll / she'll get back to you as soon as possible.";


            lastIncomingNumber = phoneNumber;

            int count = callCountMap.getOrDefault(phoneNumber, 0) + 1;
            callCountMap.put(phoneNumber, count);

            Log.d("CallListener", "Incoming call from: " + phoneNumber + ", Count: " + count);
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
                }
        }

    }

    private void sendSMS(String number, String message){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, parts, null, null);
        }catch (Exception e){
            Log.d("CallListener",e.getMessage());
        }
    }

    }

