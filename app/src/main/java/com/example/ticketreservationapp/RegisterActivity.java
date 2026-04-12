package com.example.ticketreservationapp;

import static com.example.ticketreservationapp.Authentification.emailRegex;
import static com.example.ticketreservationapp.Authentification.passwordRegex;
import static com.example.ticketreservationapp.R.*;

import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;


public class RegisterActivity extends AppCompatActivity {
    EditText email, confirmPassword, password;

    Button registerMail;
    FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_register);

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();


        email = findViewById(id.email_edittext);
        password = findViewById(id.password_edittext);
        confirmPassword = findViewById(id.confirmPassword_edittext);

        registerMail = findViewById(id.registerMail_button);
        registerMail.setOnClickListener(v -> registerWithEmail(email.getText().toString().trim().toLowerCase(),password.getText().toString().trim(),confirmPassword.getText().toString().trim()));
    }

    void registerWithEmail(String authString, String pass, String confirmPass) {

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

            // Register user with email and password
             auth.createUserWithEmailAndPassword(authString, pass)
                     .addOnCompleteListener(this, this::handleRegistrationResult);
        } else{
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_LONG).show();
        }
    }

    public void handleRegistrationResult(Task<AuthResult> task) {
        // Registration successful
        if (task.isSuccessful()) {
            // Navigate to MainActivity
            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RegisterActivity.this, ConfirmEmailActivity.class));
            finish();
        }
        // Registration failed
        //User is already registered
        else if (task.getException() instanceof FirebaseAuthUserCollisionException){
            Toast.makeText(RegisterActivity.this, "You're already register please log in", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RegisterActivity.this, LogInActivity.class));
            finish();
        } else {
            Toast.makeText(RegisterActivity.this, "Registration failed! Please try again later", Toast.LENGTH_LONG).show();
        }
    }
}
