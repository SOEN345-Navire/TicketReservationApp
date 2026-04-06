package com.example.ticketreservationapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Locale;

public final class EventQueryBuilder {
    private EventQueryBuilder() {}

    private static Query baseQuery(CollectionReference eventsRef) {
        return eventsRef.orderBy("date", Query.Direction.ASCENDING);
    }

    public static Query build(CollectionReference eventsRef, EventFilter filter) {
        if (filter == null || filter.type() == EventFilter.Type.NONE) {
            return baseQuery(eventsRef);
        }

        switch (filter.type()) {
            case LOCATION_PREFIX: {
                final String raw = filter.text();
                final String prefix = (raw == null ? "" : raw.trim())
                        .toLowerCase(Locale.getDefault());

                if (prefix.isEmpty()) return baseQuery(eventsRef);

                return eventsRef
                        .orderBy("locationLower")
                        .startAt(prefix)
                        .endAt(prefix + "\uf8ff");
            }

            case CATEGORY: {
                final String raw = filter.text();
                final String cat = (raw == null ? "" : raw.trim())
                        .toUpperCase(Locale.getDefault());

                if (cat.isEmpty()) return baseQuery(eventsRef);

                return eventsRef
                        .whereEqualTo("category", cat)
                        .orderBy("date", Query.Direction.ASCENDING);
            }

            case SINGLE_DATE: {
                Calendar selectedDay = filter.day();
                if (selectedDay == null) return baseQuery(eventsRef);

                Calendar start = (Calendar) selectedDay.clone();
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                Calendar end = (Calendar) selectedDay.clone();
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);

                Timestamp startTs = new Timestamp(start.getTime());
                Timestamp endTs = new Timestamp(end.getTime());

                return eventsRef
                        .whereGreaterThanOrEqualTo("date", startTs)
                        .whereLessThanOrEqualTo("date", endTs)
                        .orderBy("date", Query.Direction.ASCENDING);
            }

            default:
                return baseQuery(eventsRef);
        }
    }
}