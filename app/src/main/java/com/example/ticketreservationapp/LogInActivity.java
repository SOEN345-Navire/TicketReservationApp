package com.example.ticketreservationapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class LogInActivity extends AppCompatActivity {

    private EditText email, password, phone;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize FirebaseAuth instance
        auth = Authentification.getAuth();

        email = findViewById(R.id.email_edittext);
        password = findViewById(R.id.password_edittext);
        phone = findViewById(R.id.phone_edittext);

        TextView registerLink = findViewById(R.id.register_link);
        registerLink.setOnClickListener(v -> startActivity(new Intent(LogInActivity.this, RegisterActivity.class)));


        Button logMail = findViewById(R.id.loginMail_button);
        logMail.setOnClickListener(v -> logInWithEmail());
        Button logPhone = findViewById(R.id.loginPhone_button);
        logPhone.setOnClickListener(v -> logInWithPhone());
    }

    private void logInWithEmail() {
        String mailString = email.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (mailString.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(mailString, pass).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(LogInActivity.this, "Login successful!", Toast.LENGTH_LONG).show();
                // Navigate to MainActivity
                startActivity(new Intent(LogInActivity.this, MainActivity.class));
                finish();
            } else {
                // Login failed
                Toast.makeText(LogInActivity.this, "Login failed! Please try again later", Toast.LENGTH_LONG).show();

            }
        });
    }

    private void logInWithPhone() {
        String phoneString = phone.getText().toString().trim();

        if (phoneString.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_LONG).show();
        }
    }

}
