package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Date;

public class EmailConfirmationsTest {

    @Test
    public void testConfirmReservation() {

        Timestamp date = mock(Timestamp.class);
        when(date.toDate()).thenReturn(new Date());
        Reservation reservation = new Reservation(
                "reservation-id",
                "user-id",
                "event-id",
                "Event Name",
                "Event Location",
                "Event Category",
                "Status",
                date,
                1
        );

        //When user is null, return false
        assertEquals(false, emailConfirmations.confirmReservation(null, reservation, "Subject", "Body"));

        //Mock Firebase
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseUser user = mock(FirebaseUser.class);

        //when user email is null, and phone number is null, return false
        assertEquals(false, emailConfirmations.confirmReservation(user, reservation, "Subject", "Body"));

        //When user email is null, and phone number is not null, return true
        when(user.getPhoneNumber()).thenReturn("1234567890");
        CollectionReference messagesCollection = mock(CollectionReference.class);
        when(db.collection("messages")).thenReturn(messagesCollection);
        //Mock static call to get instance to return the mocked instance
        try (MockedStatic<FirebaseFirestore> mockedDB = mockStatic(FirebaseFirestore.class)) {
            mockedDB.when(FirebaseFirestore::getInstance).thenReturn(db);
            assertEquals(true, emailConfirmations.confirmReservation(user, reservation, "Subject", "Body"));
        }


        //When user email is not null, return true
        when(user.getEmail()).thenReturn("test@gmail.com");

        CollectionReference mailCollection = mock(CollectionReference.class);
        when(db.collection("mail")).thenReturn(mailCollection);

        //Mock static call to get instance to return the mocked instance
        try (MockedStatic<FirebaseFirestore> mockedDB = mockStatic(FirebaseFirestore.class)) {
            mockedDB.when(FirebaseFirestore::getInstance).thenReturn(db);
            assertEquals(true, emailConfirmations.confirmReservation(user, reservation, "Subject", "Body"));
        }

    }
}
