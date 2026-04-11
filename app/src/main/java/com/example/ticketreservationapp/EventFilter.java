package com.example.ticketreservationapp;

import java.util.Calendar;

public record EventFilter(Type type, String text, Calendar day) {

    public enum Type {NONE, LOCATION_PREFIX, CATEGORY, SINGLE_DATE}

    public static EventFilter none() {
        return new EventFilter(Type.NONE, null, null);
    }

    public static EventFilter locationPrefix(String prefix) {
        return new EventFilter(Type.LOCATION_PREFIX, prefix, null);
    }

    public static EventFilter category(String category) {
        return new EventFilter(Type.CATEGORY, category, null);
    }

    public static EventFilter singleDate(Calendar day) {
        return new EventFilter(Type.SINGLE_DATE, null, day);
    }
}