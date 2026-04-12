package com.example.ticketreservationapp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EventQueryBuilderSpecTest {

    @Test
    public void toSpec_null_returnsNone() {
        assertEquals(EventQuerySpec.Type.NONE, EventQueryBuilder.toSpec(null).type());
    }

    @Test
    public void toSpec_none_returnsNone() {
        assertEquals(EventQuerySpec.Type.NONE, EventQueryBuilder.toSpec(EventFilter.none()).type());
    }

    @Test
    public void toSpec_locationPrefix_nullText_becomesNone() {
        EventFilter f = new EventFilter(EventFilter.Type.LOCATION_PREFIX, null, null);
        assertEquals(EventQuerySpec.Type.NONE, EventQueryBuilder.toSpec(f).type());
    }

    @Test
    public void toSpec_locationPrefix_blank_becomesNone() {
        assertEquals(EventQuerySpec.Type.NONE,
                EventQueryBuilder.toSpec(EventFilter.locationPrefix("   ")).type());
    }

    @Test
    public void toSpec_locationPrefix_trimsAndLowercases() {
        EventQuerySpec spec = EventQueryBuilder.toSpec(EventFilter.locationPrefix("  Ray "));
        assertEquals(EventQuerySpec.Type.LOCATION_PREFIX, spec.type());
        assertEquals("ray", spec.normalizedText());
    }

    @Test
    public void toSpec_category_nullText_becomesNone() {
        EventFilter f = new EventFilter(EventFilter.Type.CATEGORY, null, null);
        assertEquals(EventQuerySpec.Type.NONE, EventQueryBuilder.toSpec(f).type());
    }

    @Test
    public void toSpec_category_blank_becomesNone() {
        assertEquals(EventQuerySpec.Type.NONE,
                EventQueryBuilder.toSpec(EventFilter.category("")).type());
    }

    @Test
    public void toSpec_category_trimsAndUppercases() {
        EventQuerySpec spec = EventQueryBuilder.toSpec(EventFilter.category(" sports    "));
        assertEquals(EventQuerySpec.Type.CATEGORY, spec.type());
        assertEquals("SPORTS", spec.normalizedText());
    }

    @Test
    public void toSpec_category_mixedCase_normalizesToUpper() {
        EventQuerySpec spec = EventQueryBuilder.toSpec(EventFilter.category("sPoRtS"));
        assertEquals("SPORTS", spec.normalizedText());
    }

    @Test
    public void toSpec_singleDate_null_becomesNone() {
        assertEquals(EventQuerySpec.Type.NONE,
                EventQueryBuilder.toSpec(EventFilter.singleDate(null)).type());
    }

    @Test
    public void toSpec_singleDate_createsFullDayRange() {
        TimeZone tz = TimeZone.getTimeZone("UTC");

        Calendar c = Calendar.getInstance(tz);
        c.set(2026, Calendar.APRIL, 7, 15, 30, 10);
        c.set(Calendar.MILLISECOND, 123);

        EventQuerySpec spec = EventQueryBuilder.toSpec(EventFilter.singleDate(c));
        assertEquals(EventQuerySpec.Type.SINGLE_DATE, spec.type());

        Date start = spec.startInclusive();
        Date end = spec.endInclusive();
        assertNotNull(start);
        assertNotNull(end);
        assertFalse(start.after(end));

        Calendar startCal = Calendar.getInstance(tz);
        startCal.setTime(start);
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, startCal.get(Calendar.MINUTE));
        assertEquals(0, startCal.get(Calendar.SECOND));
        assertEquals(0, startCal.get(Calendar.MILLISECOND));

        Calendar endCal = Calendar.getInstance(tz);
        endCal.setTime(end);
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, endCal.get(Calendar.MINUTE));
        assertEquals(59, endCal.get(Calendar.SECOND));
        assertEquals(999, endCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void build_none_callsBaseQuery_orderByDate() {
        CollectionReference eventsRef = mock(CollectionReference.class);
        Query base = mock(Query.class);

        when(eventsRef.orderBy("date", Query.Direction.ASCENDING)).thenReturn(base);

        Query result = EventQueryBuilder.build(eventsRef, EventFilter.none());

        assertSame(base, result);
        verify(eventsRef).orderBy("date", Query.Direction.ASCENDING);
    }

    @Test
    public void build_locationPrefix_chainsOrderByStartAtEndAt() {
        CollectionReference eventsRef = mock(CollectionReference.class);
        Query ordered = mock(Query.class);
        Query started = mock(Query.class);
        Query ended = mock(Query.class);

        when(eventsRef.orderBy("locationLower")).thenReturn(ordered);
        when(ordered.startAt("ny")).thenReturn(started);
        when(started.endAt("ny" + "\uf8ff")).thenReturn(ended);

        Query result = EventQueryBuilder.build(eventsRef, EventFilter.locationPrefix(" NY "));

        assertSame(ended, result);
        verify(eventsRef).orderBy("locationLower");
        verify(ordered).startAt("ny");
        verify(started).endAt("ny" + "\uf8ff");
    }

    @Test
    public void build_category_callsWhereEqualTo_thenOrderByDate() {
        CollectionReference eventsRef = mock(CollectionReference.class);
        Query afterWhere = mock(Query.class);
        Query afterOrder = mock(Query.class);

        when(eventsRef.whereEqualTo("category", "MOVIES")).thenReturn(afterWhere);
        when(afterWhere.orderBy("date", Query.Direction.ASCENDING)).thenReturn(afterOrder);

        Query result = EventQueryBuilder.build(eventsRef, EventFilter.category("movies"));

        assertSame(afterOrder, result);
        verify(eventsRef).whereEqualTo("category", "MOVIES");
        verify(afterWhere).orderBy("date", Query.Direction.ASCENDING);
    }

    @Test
    public void build_singleDate_chainsDateRangeQuery() {
        CollectionReference eventsRef = mock(CollectionReference.class);
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        Query q3 = mock(Query.class);

        when(eventsRef.whereGreaterThanOrEqualTo(eq("date"), any(Timestamp.class))).thenReturn(q1);
        when(q1.whereLessThanOrEqualTo(eq("date"), any(Timestamp.class))).thenReturn(q2);
        when(q2.orderBy("date", Query.Direction.ASCENDING)).thenReturn(q3);

        Calendar selected = Calendar.getInstance(Locale.CANADA);
        selected.set(2026, Calendar.APRIL, 11, 10, 0, 0);

        Query result = EventQueryBuilder.build(eventsRef, EventFilter.singleDate(selected));

        assertSame(q3, result);
        verify(eventsRef).whereGreaterThanOrEqualTo(eq("date"), any(Timestamp.class));
        verify(q1).whereLessThanOrEqualTo(eq("date"), any(Timestamp.class));
        verify(q2).orderBy("date", Query.Direction.ASCENDING);
    }
}