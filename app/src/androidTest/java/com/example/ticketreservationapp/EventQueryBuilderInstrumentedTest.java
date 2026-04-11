package com.example.ticketreservationapp;

import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

@RunWith(AndroidJUnit4.class)
public class EventQueryBuilderInstrumentedTest{

    private CollectionReference eventsRef;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext()
        );

        eventsRef = FirebaseFirestore.getInstance().collection("events");
        assertNotNull(eventsRef);
    }

    @Test
    public void build_none_returnsQuery() {
        Query q = EventQueryBuilder.build(eventsRef, EventFilter.none());
        assertNotNull(q);
    }

    @Test
    public void build_locationPrefix_returnsQuery() {
        Query q = EventQueryBuilder.build(eventsRef, EventFilter.locationPrefix("Cir"));
        assertNotNull(q);
    }

    @Test
    public void build_category_returnsQuery() {
        Query q = EventQueryBuilder.build(eventsRef, EventFilter.category("SPORTS"));
        assertNotNull(q);
    }

    @Test
    public void build_singleDate_returnsQuery() {
        Calendar c = Calendar.getInstance();
        Query q = EventQueryBuilder.build(eventsRef, EventFilter.singleDate(c));
        assertNotNull(q);
    }
}