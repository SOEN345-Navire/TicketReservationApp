package com.example.ticketreservationapp;


public class Authentification {

    public static final String emailRegex = "\\A[a-z0-9!#$%&'*+/=?^_\"{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_\"{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\z";
    public static final String phoneRegex = "^\\d{10}$";
    public static final String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";


}
