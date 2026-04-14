package com.example.ticketreservationapp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Provides mocked Firestore components for testing.
 * This prevents real Firestore initialization which causes protobuf conflicts in tests.
 */
public class MockFirestoreProvider {

    public static FirebaseFirestore getMockFirestore() {
        FirebaseFirestore mock = mock(FirebaseFirestore.class);
        
        // Mock events collection
        CollectionReference eventsRef = mock(CollectionReference.class);
        Query eventsQuery = mock(Query.class);
        when(eventsRef.orderBy("date", Query.Direction.ASCENDING)).thenReturn(eventsQuery);
        when(mock.collection("events")).thenReturn(eventsRef);
        
        // Mock reservations collection
        CollectionReference reservationsRef = mock(CollectionReference.class);
        Query reservationsQuery = mock(Query.class);
        when(reservationsRef.whereEqualTo("userId", "test-user-id"))
                .thenReturn(reservationsQuery);
        when(reservationsQuery.whereEqualTo("status", "confirmed")).thenReturn(reservationsQuery);
        when(reservationsQuery.whereEqualTo("status", "cancelled")).thenReturn(reservationsQuery);
        when(mock.collection("reservations")).thenReturn(reservationsRef);
        
        return mock;
    }

    public static CollectionReference getMockCollectionReference() {
        return mock(CollectionReference.class);
    }
}
