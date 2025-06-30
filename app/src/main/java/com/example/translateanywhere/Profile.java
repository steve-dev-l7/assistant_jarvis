package com.example.translateanywhere;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Profile extends AppCompatActivity {
    Button otp, save;
    EditText name, age, id, dob, mobileNo, Location, GOB;
    FirebaseFirestore db;
    String Name, Age, Id, dateofbirth, mobile, Loc, Donate, Group, generatedOtp, sentOtp;
    ProgressDialog progressDialog;
    Toolbar toolbar1;
    TextView GoToLogIn;
    CheckBox donate;

    Boolean testing=true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        hideSystemUI();
        setContentView(R.layout.activity_profile);
        db = FirebaseFirestore.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        otp = findViewById(R.id.save);
        name = findViewById(R.id.name);
        age = findViewById(R.id.age);
        id = findViewById(R.id.id);
        dob = findViewById(R.id.dob);
        mobileNo = findViewById(R.id.mobile);
        GoToLogIn = findViewById(R.id.gologin);
        Location = findViewById(R.id.location);
        donate = findViewById(R.id.bloodDonate);
        GOB = findViewById(R.id.BloodGroup);
        save = findViewById(R.id.saveUser);



        toolbar1 = findViewById(R.id.my_toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        GoToLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        });

        otp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isValidInput(name)) {
                    name.setError("Name cannot be empty!");
                    return;
                }
                if (!isValidInput(age)) {
                    age.setError("Age cannot be empty!");
                    return;
                }
                if (!isValidInput(id)) {
                    id.setError("ID cannot be empty!");
                    return;
                }
                if (!isValidInput(dob)) {
                    dob.setError("Date of Birth cannot be empty!");
                    return;
                }
                if (!isValidInput(mobileNo)) {
                    mobileNo.setError("Mobile number cannot Be empty! ");
                    return;
                }
                if (!isValidInput(Location)) {
                    Location.setError("Location cannot Be empty! ");
                    return;
                }
                Name = String.valueOf(name.getText()).toLowerCase().trim();
                Age = String.valueOf(age.getText()).toLowerCase().trim();
                Id = String.valueOf(id.getText()).toLowerCase().trim();
                dateofbirth = String.valueOf(dob.getText()).toLowerCase().trim();
                mobile = String.valueOf(mobileNo.getText()).toLowerCase().trim();
                Loc = String.valueOf(Location.getText()).toLowerCase().trim();
                if (donate.isChecked()) {
                    Donate = "true";
                    if (!isValidInput(GOB)) {
                        GOB.setError(" BloodGroup cannot number Be empty! ");
                        return;
                    }
                    Group = String.valueOf(GOB.getText()).toLowerCase().trim();

                } else {
                    Donate = "false";
                }
                checkUsersId(Id);
            }
        });
    }

    private void StoretoFireStore(String Name, String Age, String UserId, String DOB, String no, String L, String D, String G) {
        progressDialog = ProgressDialog.show(this, "Updating Profile", "Please Be Patient");
        Map<String, Object> user = new HashMap<>();
        user.put("Name", Name);
        user.put("Age", Age);
        user.put("UserId", UserId);
        user.put("DOB", DOB);
        user.put("Mobile", no);
        user.put("Location", L);
        user.put("Donate", D);
        user.put("Group", G);

        db.collection("users").document(UserId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Log.d("Firestore", "User data saved successfully!");
                        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("UserId", UserId);
                        editor.putString("NewUser", "The User New For Our App Please Welcome The User And Introduce The User");
                        editor.apply();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("UserId", UserId);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Error Please Try Again After Few Minutes", Toast.LENGTH_SHORT).show();
                        Log.d("Firestore", "Error saving data", e);
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void checkUsersId(String UserId) {
        if (UserId == null || UserId.isEmpty()) {
            Log.e("Firestore", "UserId is empty!");
            return;
        }
        db.collection("users").document(UserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.e("Firestore", "UserId already exists!");
                        Toast.makeText(Profile.this, "User ID already exists! If you have already account click login", Toast.LENGTH_LONG).show();
                    } else {
                            StoretoFireStore(Name, Age, Id, dateofbirth, mobile, Loc, Donate, Group);

                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking UserId", e));
    }

    private boolean isValidInput(EditText editText) {
        String input = editText.getText().toString().trim();
        return !input.isEmpty();
    }


    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}