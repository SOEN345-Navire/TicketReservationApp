package com.example.ticketreservationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;



public class ConfirmPhoneActivity extends AppCompatActivity {

    FirebaseAuth auth;

    String verificationId;

    PhoneAuthProvider.ForceResendingToken token;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_phone);

        auth = FirebaseAuth.getInstance();

        PhoneAuthOptions options = getOptions(Authentification.phone);

        PhoneAuthProvider.verifyPhoneNumber(options);

        EditText code = findViewById(R.id.code_edittext);
        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> confirmPhone(code.getText().toString().trim()));
    }

    void confirmPhone(String code) {
        if(code.isEmpty()){
            Toast.makeText(ConfirmPhoneActivity.this, "Please enter the code", Toast.LENGTH_LONG).show();
            return;
        }
        if(code.length() != 6){
            Toast.makeText(ConfirmPhoneActivity.this, "Please enter a valid code", Toast.LENGTH_LONG).show();
            return;
        }
        if(verificationId == null){
            Toast.makeText(ConfirmPhoneActivity.this, "Please wait for the code to be sent", Toast.LENGTH_LONG).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);

    }

    PhoneAuthOptions getOptions(String phoneString) {
        return new PhoneAuthOptions.Builder(auth)
                .setPhoneNumber(phoneString)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(getCallbacks())
                .build();
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks getCallbacks() {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(ConfirmPhoneActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                startActivity(new Intent(ConfirmPhoneActivity.this, LogInActivity.class));
                finish();
            }
            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Toast.makeText(ConfirmPhoneActivity.this, "Verify your SMS Inbox", Toast.LENGTH_LONG).show();
                ConfirmPhoneActivity.this.verificationId = verificationId;
                ConfirmPhoneActivity.this.token = token;
            }
        };
    }
    void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(this, this::handleLoginResult);
    }

    void handleLoginResult(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            Toast.makeText(ConfirmPhoneActivity.this, "Login successful", Toast.LENGTH_LONG).show();
            startActivity(new Intent(ConfirmPhoneActivity.this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(ConfirmPhoneActivity.this, "Login failed", Toast.LENGTH_LONG).show();
            startActivity(new Intent(ConfirmPhoneActivity.this, LogInActivity.class));
            finish();
        }
    }
}

