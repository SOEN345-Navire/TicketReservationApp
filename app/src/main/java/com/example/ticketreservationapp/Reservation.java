package com.example.ticketreservationapp;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

public class Reservation {
    private String id;
    private String userId;
    private String eventId;
    private String eventName;
    private String eventLocation;
    private String eventCategory;
    private String status;
    private Timestamp eventDate;
    private int ticketCount;


    public Reservation() {
    }

    public Reservation(String id, String userId, String eventId, String eventName, String eventLocation, String eventCategory, String status, Timestamp eventDate, int ticketCount) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventLocation = eventLocation;
        this.eventCategory = eventCategory;
        this.status = status;
        this.eventDate = eventDate;
        this.ticketCount = ticketCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }


    @NonNull
    @Override
    public String toString(){
        return ("Reservation:\n" +
                "eventName: " + eventName + "\n" +
                "eventLocation: " + eventLocation + "\n" +
                "eventCategory: " + eventCategory + "\n" +
                "status: " + status + "\n" +
                "eventDate: " + eventDate.toDate() + "\n" +
                "ticketCount: " + ticketCount + "\n"
        );
    }
}
