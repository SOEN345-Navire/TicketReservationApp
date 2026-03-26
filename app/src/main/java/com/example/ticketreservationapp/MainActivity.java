package com.example.ticketreservationapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        auth = Authentification.getAuth();

        // Check if user is already logged in
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LogInActivity.class));
        }

    }
}
