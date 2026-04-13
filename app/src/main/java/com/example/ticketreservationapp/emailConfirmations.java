package com.example.ticketreservationapp;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class emailConfirmations {

    public static Boolean confirmReservation(FirebaseUser user, Reservation reservation, String subject, String body){
        if (user == null) return false;
        String email = user.getEmail();
        if (email == null){
            String phoneNumber = user.getPhoneNumber();
            if (phoneNumber == null) return false;
            body += reservation.toString();
            sendSMS(phoneNumber, subject, body);
            return true;
        }
        body += reservation.toString();
        sendEmail(email, subject, body);
        return true;
    }

    private static void sendEmail(String to, String subject, String body){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Define the email data
        Map<String, Object> email = new HashMap<>();
        email.put("to", to); // Can be a string or array

        Map<String, Object> message = new HashMap<>();
        message.put("subject", subject);
        message.put("text", body);

        email.put("message", message);

        // Add the document to the 'mail' collection
        db.collection("mail").add(email);
    }

    private static void sendSMS(String to, String subject, String body){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Define the SMS data
        Map<String, Object> sms = new HashMap<>();
        sms.put("to", to); // Can be a string or array
        sms.put("body", subject + "\n" + body);

        // Add the document to the 'messages' collection
        db.collection("messages").add(sms);
    }

}
