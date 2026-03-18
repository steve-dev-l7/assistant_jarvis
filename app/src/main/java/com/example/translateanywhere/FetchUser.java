package com.example.translateanywhere;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FetchUser {

    // Singleton Instance
    private static FetchUser instance;

    private FirebaseFirestore dataBase;

    // User Variables
    private String name = "Commander"; // Default fallback
    private String age = "";
    private String dob = "";

    private String mobile = "";

    private String location = "";

    private String userId ="";

    private boolean isDataLoaded = false;

    public interface OnUserFetchListener {
        void onSuccess();
        void onError(String message);
    }

    // Private constructor for Singleton
    FetchUser() {
        dataBase = FirebaseFirestore.getInstance();
    }

    // Get the single instance
    public static FetchUser getInstance() {
        if (instance == null) {
            instance = new FetchUser();
        }
        return instance;
    }

    // Call this ONCE when your Service/App starts
    // Context-ah parameter-ah vaangikrom
    public void fetchUserData(Context context, OnUserFetchListener listener) {
        // If already loaded, return immediately to save network
        if (isDataLoaded) {
            listener.onSuccess();
            return;
        }

        // SharedPreferences logic ippo Inga vanthuduchu!
        android.content.SharedPreferences preferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        userId = preferences.getString("UserId", null);

        if (userId == null || userId.isEmpty()) {
            listener.onError("UserId not found in local storage");
            return;
        }

        dataBase.collection("users").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            name = documentSnapshot.getString("Name");
                            age = documentSnapshot.getString("Age");
                            dob = documentSnapshot.getString("DOB");

                            mobile = documentSnapshot.getString("Mobile");
                            location = documentSnapshot.getString("Location");

                            isDataLoaded = true;
                            Log.d("FetchUser", "User data fetched and cached successfully.");
                            listener.onSuccess();
                        } else {
                            Log.w("FetchUser", "User document not found");
                            listener.onError("User not found in database");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FetchUser", "Error fetching user data", e);
                        listener.onError("Failed to fetch user data: " + e.getMessage());
                    }
                });
    }

    // --- GETTERS ---
    public String getName() { return name; }
    public String getAge() { return age; }
    public String getDob() { return dob; }
    public String getMobile() { return mobile; }
    public String getLocation() { return location; }
    public boolean isLoaded() { return isDataLoaded; }

    public String getUserId(){return userId;}
}