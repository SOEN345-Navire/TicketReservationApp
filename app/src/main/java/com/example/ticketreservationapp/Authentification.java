package com.example.ticketreservationapp;

import com.google.firebase.auth.FirebaseAuth;

public class Authentification {

    private static FirebaseAuth auth;

    public static final String emailRegex = "\\A[a-z0-9!#$%&'*+/=?^_\"{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_\"{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\z";
    public static final String phoneRegex = "^\\d{10}$";
    public static final String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

}
