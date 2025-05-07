package com.example.bussingsw.ui.bussing_welcomepage;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bussingsw.MainActivity;
import com.example.bussingsw.R;
import com.example.bussingsw.ui.setup_account.SignIn;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Welcome extends AppCompatActivity {

    MaterialButton signIn, register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        signIn = findViewById(R.id.signInButton);

        // Check if the user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If the user is signed in, navigate to MainActivity directly
            navigateToMainActivity();
        } else {
            // If the user is not signed in, show the welcome page
            setContentView(R.layout.activity_welcome);

            findViewById(R.id.signInButton).setOnClickListener(view -> {
                Intent intent = new Intent(Welcome.this, SignIn.class);
                startActivity(intent);
                finish();
            });

        }

    }


    @Override
    public void onBackPressed() {
        // Don't allow going back to the welcome screen once the user proceeds
        finish();
        super.onBackPressed();
    }

    // Method to navigate to MainActivity
    public void navigateToMainActivity() {
        // Use the flags to clear the back stack and prevent going back to WelcomePage
        Intent intent = new Intent(Welcome.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
