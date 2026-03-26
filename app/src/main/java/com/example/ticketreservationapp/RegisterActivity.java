package com.example.ticketreservationapp;

import static com.example.ticketreservationapp.Authentification.emailRegex;
import static com.example.ticketreservationapp.Authentification.passwordRegex;
import static com.example.ticketreservationapp.R.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class RegisterActivity extends AppCompatActivity {
    private EditText email, confirmPassword, password;
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_register);

        // Initialize FirebaseAuth instance
        auth = Authentification.getAuth();

        email = findViewById(id.email_edittext);
        password = findViewById(id.password_edittext);
        confirmPassword = findViewById(id.confirmPassword_edittext);

        Button registerMail = findViewById(id.registerMail_button);
        registerMail.setOnClickListener(v -> registerWithEmail());
    }

    private void registerWithEmail() {
        // Get values from input fields
        String authString = email.getText().toString().trim();
        String pass = password.getText().toString().trim();
        String confirmPass = confirmPassword.getText().toString().trim();


        // Validate input
        if (authString.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show();
            return;
        }
        if (!pass.matches(passwordRegex)) {
            Toast.makeText(this, "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character", Toast.LENGTH_LONG).show();
            return;
        }
        if(authString.matches(emailRegex)) {
             auth.createUserWithEmailAndPassword(authString, pass)
                     .addOnCompleteListener(this, task -> {
                         if (task.isSuccessful()) {
                             Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
                             // Navigate to MainActivity
                             startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                             finish();
                         } else {
                             // Registration failed
                             Toast.makeText(RegisterActivity.this, "Registration failed! Please try again later", Toast.LENGTH_LONG).show();
                         }
                     });
        } else{
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_LONG).show();
        }
    }

}
