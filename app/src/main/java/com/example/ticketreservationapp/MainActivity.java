package com.example.ticketreservationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;

    FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            checkUserStatus(user);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initAuth();

    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(authStateListener);
    }


    private void checkUserStatus(FirebaseUser user) {
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LogInActivity.class));
            finish();
            return;
        }

        //Check if user is verified
        if (!user.isEmailVerified() && user.getEmail() != null && Objects.requireNonNull(user.getEmail()).matches(Authentification.emailRegex)) {
            startActivity(new Intent(MainActivity.this, ConfirmEmailActivity.class));
           finish();
        }
        Authentification.isAdmin(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    boolean isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                    if (isAdmin) {
                        startActivity(new Intent(MainActivity.this, AdminActivity.class));
                        finish();
                    }
                } else {
                    Authentification.setAdmin(user.getUid(), false);
                }
            }
        });
    }

    private void initAuth() {
        auth = FirebaseAuth.getInstance();
        Button logout = findViewById(R.id.logout);
        logout.setOnClickListener(v -> auth.signOut());
    }


}
