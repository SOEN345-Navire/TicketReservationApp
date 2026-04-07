package com.example.ticketreservationapp;

import java.util.Date;

public record EventQuerySpec(
        Type type,
        String normalizedText,
        Date startInclusive,
        Date endInclusive
) {
    public enum Type {
        NONE,
        LOCATION_PREFIX,
        CATEGORY,
        SINGLE_DATE
    }

    public static EventQuerySpec none() {
        return new EventQuerySpec(Type.NONE, "", null, null);
    }

}
