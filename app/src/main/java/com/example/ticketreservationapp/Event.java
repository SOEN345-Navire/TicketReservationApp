package com.example.ticketreservationapp;

import com.google.firebase.Timestamp;

public class Event {
    private String id;
    private String name;
    private Timestamp date;
    private String location;
    private String category;
    private int reservedPlaces;
    private int maxPlaces;
    private String status;

    public Event() {

    }

    public Event(String name, Timestamp date, String location, String category, int reservedPlaces, int maxPlaces, String status) {
        this.name = name;
        this.date = date;
        this.location = location;
        this.category = category;
        this.reservedPlaces = reservedPlaces;
        this.maxPlaces = maxPlaces;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getReservedPlaces() {
        return reservedPlaces;
    }

    public void setReservedPlaces(int reservedPlaces) {
        this.reservedPlaces = reservedPlaces;
    }

    public int getMaxPlaces() {
        return maxPlaces;
    }

    public void setMaxPlaces(int maxPlaces) {
        this.maxPlaces = maxPlaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
