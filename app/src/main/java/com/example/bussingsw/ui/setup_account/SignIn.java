package com.example.bussingsw.ui.setup_account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bussingsw.MainActivity;
import com.example.bussingsw.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignIn extends AppCompatActivity {

    TextInputEditText email, password;
    Button login;
    ProgressDialog progressDialog;

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.emailTextInput);
        password = findViewById(R.id.passwordTextInput);
        login = findViewById(R.id.loginButton);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference(); // Root of Realtime DB

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        login.setOnClickListener(v -> {
            String emailInput = email.getText().toString().trim();
            String passwordInput = password.getText().toString().trim();

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(SignIn.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.show();

            auth.signInWithEmailAndPassword(emailInput, passwordInput)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                String name;
                                String profileUrl = "https://yourdomain.com/default_profile.png";

                                if (emailInput.equals("happyprend@bussing.com")) {
                                    name = "Happy Friend";
                                } else if (emailInput.equals("malupiton@bussing.com")) {
                                    name = "Joel Malupiton";
                                } else {
                                    name = "Bus Driver";
                                }

                                DUser dUser = new DUser(uid, profileUrl, name, emailInput);

                                // 🔧 Set value and log error if it fails
                                rootRef.child("DUsers").child(uid).setValue(dUser)
                                        .addOnSuccessListener(unused -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(SignIn.this, "Logged in and stored data successfully!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignIn.this, MainActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Log.e("FirebaseDB", "Failed to save user: ", e);
                                            Toast.makeText(SignIn.this, "DB Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(SignIn.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
