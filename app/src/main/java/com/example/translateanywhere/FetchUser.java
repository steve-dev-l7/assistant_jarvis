package com.example.translateanywhere;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class FetchUser {

    FirebaseFirestore dataBase;
    DatabaseReference databaseReference;

    String[] userData;

    public interface UserDataCallBack {
        void onUserDataFetched(String[] data);
        void onError(Exception e);
    }

    public FetchUser(String UserID, UserDataCallBack callBack){
        dataBase = FirebaseFirestore.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        dataBase.collection("users").document(UserID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        if (documentSnapshot.exists()) {
                            userData = new String[7];
                            userData[0] = documentSnapshot.getString("Name");
                            userData[1] = documentSnapshot.getString("Age");
                            userData[2] = documentSnapshot.getString("DOB");
                            userData[3] = documentSnapshot.getString("Donate");
                            userData[4] = documentSnapshot.getString("Mobile");
                            if (userData[3] != null && userData[3].equalsIgnoreCase("true")) {
                                userData[5] = documentSnapshot.getString("Group");
                                userData[6] = documentSnapshot.getString("Location");
                            }
                            Log.d("UserData", Arrays.toString(userData));
                            callBack.onUserDataFetched(userData);
                        }else {
                            callBack.onError(new Exception("Document does not exist"));
                        }
                    }

                }) .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching user data", e);
                    callBack.onError(e);
                });
    }
}
