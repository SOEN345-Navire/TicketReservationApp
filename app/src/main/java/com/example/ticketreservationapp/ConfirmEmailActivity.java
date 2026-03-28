package com.example.ticketreservationapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ConfirmEmailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_email);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(ConfirmEmailActivity.this, LogInActivity.class));
            finish();
            return;
        }
        user.sendEmailVerification();
        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> checkEmailVerification(user));

    }

    private void checkEmailVerification(FirebaseUser user) {
        Task<Void> reload = user.reload();
        reload.addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    startActivity(new Intent(ConfirmEmailActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Please verify your email", Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(this, "Failed to reload user", Toast.LENGTH_LONG).show();
            }
        });
    }

}
