package com.example.translateanywhere;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    Button Login;
    EditText ID, DOB;
    FirebaseFirestore db;
    String userId, DofB;
    Toolbar toolbar1;
    ProgressDialog progressDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        hideSystemUI();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Login = findViewById(R.id.button);
        ID = findViewById(R.id.ID);
        DOB = findViewById(R.id.DOB);
        db = FirebaseFirestore.getInstance();
        toolbar1=findViewById(R.id.my_toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        Login.setOnClickListener(view -> {
            if (!isValidInput(ID)) {
                ID.setError("This field is required");
                return;
            }
            if (!isValidInput(DOB)) {
                DOB.setError("This field is required");
                return;
            }

            userId = ID.getText().toString().trim();
            DofB = DOB.getText().toString().trim();
            progressDialog=ProgressDialog.show(this,"Login","Login into Your account please wait");
            checkUserAccount();
        });
    }

    private void checkUserAccount() {
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID cannot be empty", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        validateUser(documentSnapshot);
                    } else {
                        Toast.makeText(Login.this, "User ID does not exist. Please create an account.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(Login.this, "Error fetching user data. Try again.", Toast.LENGTH_SHORT).show());
    }

    private void validateUser(DocumentSnapshot documentSnapshot) {
        String storedUserId = documentSnapshot.getString("UserId");
        String storedDOB = documentSnapshot.getString("DOB");

        if (storedUserId != null && storedDOB != null) {
            if (storedUserId.equals(userId) && storedDOB.equals(DofB)) {

                SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("UserId", userId);
                editor.putString("Comeback","The user is just come backed for our app welcome the user");
                editor.apply();
                progressDialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(Login.this, "Incorrect User ID or DOB", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        } else {
            Toast.makeText(Login.this, "User data is incomplete. Please register again.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    private boolean isValidInput(EditText editText) {
        return !editText.getText().toString().trim().isEmpty();
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