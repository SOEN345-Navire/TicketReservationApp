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

    // for unit-tests
    static EventQuerySpec toSpec(EventFilter filter) {
        if (filter == null || filter.type() == EventFilter.Type.NONE) {
            return EventQuerySpec.none();
        }

        return switch (filter.type()) {
            case LOCATION_PREFIX -> {
                String raw = filter.text();
                String prefix = (raw == null ? "" : raw.trim()).toLowerCase(Locale.getDefault());
                yield prefix.isEmpty()
                        ? EventQuerySpec.none()
                        : new EventQuerySpec(EventQuerySpec.Type.LOCATION_PREFIX, prefix, null, null);
            }

            case CATEGORY -> {
                String raw = filter.text();
                String cat = (raw == null ? "" : raw.trim()).toUpperCase(Locale.getDefault());
                yield cat.isEmpty()
                        ? EventQuerySpec.none()
                        : new EventQuerySpec(EventQuerySpec.Type.CATEGORY, cat, null, null);
            }

            case SINGLE_DATE -> {
                Calendar selectedDay = filter.day();
                if (selectedDay == null) yield EventQuerySpec.none();

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

                yield new EventQuerySpec(
                        EventQuerySpec.Type.SINGLE_DATE,
                        "",
                        start.getTime(),
                        end.getTime()
                );
            }

            default -> EventQuerySpec.none();
        };
    }

    // data logic
    public static Query build(CollectionReference eventsRef, EventFilter filter) {
        EventQuerySpec spec = toSpec(filter);

        if (spec.type() == EventQuerySpec.Type.NONE) {
            return baseQuery(eventsRef);
        }

        switch (spec.type()) {
            case LOCATION_PREFIX: {
                String prefix = spec.normalizedText();
                return eventsRef
                        .orderBy("locationLower")
                        .startAt(prefix)
                        .endAt(prefix + "\uf8ff");
            }

            case CATEGORY:
                return eventsRef
                        .whereEqualTo("category", spec.normalizedText())
                        .orderBy("date", Query.Direction.ASCENDING);

            case SINGLE_DATE: {
                Timestamp startTs = new Timestamp(spec.startInclusive());
                Timestamp endTs = new Timestamp(spec.endInclusive());

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