package com.example.ticketreservationapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;


public class LogInActivity extends AppCompatActivity {

    EditText email;
    EditText password;
    EditText phone;

    Button logMail, logPhone;

    TextView registerLink;

    FirebaseAuth auth;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();


        email = findViewById(R.id.email_edittext);
        password = findViewById(R.id.password_edittext);
        phone = findViewById(R.id.phone_edittext);

        registerLink = findViewById(R.id.register_link);
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LogInActivity.this, RegisterActivity.class));
            finish();
        });


        logMail = findViewById(R.id.loginMail_button);
        logMail.setOnClickListener(v -> logInWithEmail(email.getText().toString().trim(), password.getText().toString().trim()));
        logPhone = findViewById(R.id.loginPhone_button);
        logPhone.setOnClickListener(v -> logInWithPhone(phone.getText().toString().trim()));
    }

    void logInWithEmail(String mailString, String pass) {

        if (mailString.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(mailString, pass).addOnCompleteListener(this, this::handleLoginResult);
    }

    void logInWithPhone(String phoneString) {

        if (phoneString.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_LONG).show();
            return;
        }

        if (!phoneString.matches(Authentification.phoneRegex)) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_LONG).show();
            return;
        }
        Authentification.phone = "+1" + phoneString;
        startActivity(new Intent(LogInActivity.this, ConfirmPhoneActivity.class));
        finish();
    }

    void handleLoginResult(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            Toast.makeText(LogInActivity.this, "Login successful!", Toast.LENGTH_LONG).show();
            // Navigate to MainActivity
            startActivity(new Intent(LogInActivity.this, MainActivity.class));
            finish();
        } else {
            // Login failed
            Toast.makeText(LogInActivity.this, "Login failed! Please try again later", Toast.LENGTH_LONG).show();

        }
    }

}
