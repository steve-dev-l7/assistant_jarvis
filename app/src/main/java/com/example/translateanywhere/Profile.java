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
    EditText name, age, id, dob, mobileNo, Location, GOB, Checkotp;
    FirebaseFirestore db;
    String Name, Age, Id, dateofbirth, mobile, Loc, Donate, Group, generatedOtp, sentOtp;
    ProgressDialog progressDialog;
    Toolbar toolbar1;
    TextView GoToLogIn,txt1,txt2,txt3,txt4,txt5,txt6,change;
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
        Checkotp = findViewById(R.id.CheckOtp);
        save = findViewById(R.id.saveUser);

        txt1 =findViewById(R.id.txt1);
        txt2 =findViewById(R.id.txt2);
        txt3 =findViewById(R.id.txt3);
        txt4 =findViewById(R.id.txt4);
        txt5 =findViewById(R.id.txt5);
        txt6 =findViewById(R.id.txt6);
        change =findViewById(R.id.change);

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
                        if(!testing) {
                            name.setVisibility(View.GONE);
                            age.setVisibility(View.GONE);
                            id.setVisibility(View.GONE);
                            dob.setVisibility(View.GONE);
                            mobileNo.setVisibility(View.GONE);
                            Location.setVisibility(View.GONE);
                            GOB.setVisibility(View.GONE);
                            donate.setVisibility(View.GONE);
                            save.setVisibility(View.VISIBLE);
                            Checkotp.setVisibility(View.VISIBLE);

                            txt1.setVisibility(View.GONE);
                            txt2.setVisibility(View.GONE);
                            txt3.setVisibility(View.GONE);
                            txt4.setVisibility(View.GONE);
                            txt5.setVisibility(View.GONE);
                            txt6.setVisibility(View.GONE);
                            change.setText("Enter Otp");

                            sentOtp = String.valueOf(Checkotp.getText());
                            generatedOtp = String.valueOf(new Random().nextInt(9000 - 1000 + 1) + 1000);
                            Log.d("GeneratedOtp is :", generatedOtp);

                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                generatedOtp = String.valueOf(new Random().nextInt(9000 - 1000 + 1) + 1000);
                                Log.d("GeneratedOtp", "OTP Generated: " + generatedOtp);

                                smsManager.sendTextMessage(mobile, null, "Here use this : " + generatedOtp, null, null);
                                Toast.makeText(Profile.this, "OTP Sent Successfully!", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.d("SMS Error", "Failed to send SMS", e);
                                Toast.makeText(Profile.this, "Failed to send OTP. Check permissions or try again.", Toast.LENGTH_LONG).show();
                            }

                            save.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (!isValidInput(Checkotp)) {
                                        return;
                                    }
                                    verifyOtp();
                                }
                            });
                        }
                        else {
                            StoretoFireStore(Name, Age, Id, dateofbirth, mobile, Loc, Donate, Group);
                        }

                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking UserId", e));
    }

    private boolean isValidInput(EditText editText) {
        String input = editText.getText().toString().trim();
        return !input.isEmpty();
    }

    private void verifyOtp() {
        String enteredOtp = Checkotp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Checkotp.setError("Please enter the OTP sent to your mobile");
            return;
        }

        if (enteredOtp.equals(generatedOtp)) {
            StoretoFireStore(Name, Age, Id, dateofbirth, mobile, Loc, Donate, Group);
        } else {
            Checkotp.setError("Invalid OTP. Please check your mobile number.");
        }
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