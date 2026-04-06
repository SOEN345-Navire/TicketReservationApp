package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

public class ReservationTest {

    @Test
    public void gettersAndSetters_storeAllReservationFields() {
        Reservation reservation = new Reservation();
        Timestamp date = new Timestamp(new Date());

        reservation.setId("res-42");
        reservation.setUserId("user-1");
        reservation.setEventId("event-7");
        reservation.setEventName("Music Fest");
        reservation.setEventLocation("Montreal");
        reservation.setEventCategory("concert");
        reservation.setStatus("confirmed");
        reservation.setEventDate(date);
        reservation.setTicketCount(3);

        assertEquals("res-42", reservation.getId());
        assertEquals("user-1", reservation.getUserId());
        assertEquals("event-7", reservation.getEventId());
        assertEquals("Music Fest", reservation.getEventName());
        assertEquals("Montreal", reservation.getEventLocation());
        assertEquals("concert", reservation.getEventCategory());
        assertEquals("confirmed", reservation.getStatus());
        assertSame(date, reservation.getEventDate());
        assertEquals(3, reservation.getTicketCount());
    }
}
