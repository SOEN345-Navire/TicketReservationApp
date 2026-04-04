package com.example.ticketreservationapp;


import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Authentification {

    public static final String emailRegex = "\\A[a-z0-9!#$%&'*+/=?^_\"{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_\"{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\z";
    public static final String phoneRegex = "^\\d{10}$";
    public static final String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    public static String phone;

    public static Task<DocumentSnapshot> isAdmin(String uid){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(uid);
        return docRef.get();
    }

    public static void setAdmin(String uid, boolean isAdmin) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> isAdminObj = new HashMap<>();
        isAdminObj.put("isAdmin", isAdmin);
        db.collection("users").document(uid).set(isAdminObj);
    }


}
