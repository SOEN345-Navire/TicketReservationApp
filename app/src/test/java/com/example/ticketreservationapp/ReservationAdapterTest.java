package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReservationAdapterTest {

    @SuppressWarnings("unchecked")
    private FirestoreRecyclerOptions<Reservation> mockOptions() {
        ObservableSnapshotArray<Reservation> snapshots = mock(ObservableSnapshotArray.class);
        return mockOptions(snapshots);
    }

    private FirestoreRecyclerOptions<Reservation> mockOptions(ObservableSnapshotArray<Reservation> snapshots) {
        return new FirestoreRecyclerOptions.Builder<Reservation>()
                .setSnapshotArray(snapshots)
                .build();
    }

    private View inflateReservationItemView() {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context themedContext = new ContextThemeWrapper(appContext, R.style.Theme_TicketReservationApp);
        return LayoutInflater.from(themedContext)
                .inflate(R.layout.item_reservation, new FrameLayout(themedContext), false);
    }

    private Reservation createReservation(String status, int ticketCount) {
        Reservation reservation = new Reservation();
        reservation.setId("res-1");
        reservation.setEventName("Tech Expo");
        reservation.setEventCategory("conference");
        reservation.setEventLocation("Laval");
        reservation.setEventDate(new Timestamp(new Date()));
        reservation.setStatus(status);
        reservation.setTicketCount(ticketCount);
        return reservation;
    }

    @Test
    public void bind_confirmed_enablesCancelAndInvokesCallback() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ReservationAdapter adapter = new ReservationAdapter(mockOptions(), empty -> {}, reservation -> callbackCount.incrementAndGet());
        View itemView = inflateReservationItemView();
        ReservationAdapter.ReservationHolder holder = adapter.new ReservationHolder(itemView);

        holder.bind(createReservation("confirmed", 2));

        TextView status = itemView.findViewById(R.id.tvReservationStatus);
        TextView tickets = itemView.findViewById(R.id.tvReservationTicketCount);
        MaterialButton cancel = itemView.findViewById(R.id.btnCancelReservation);

        assertEquals("Status: Confirmed", status.getText().toString());
        assertEquals("Tickets: 2", tickets.getText().toString());
        assertTrue(cancel.isEnabled());
        assertEquals("Cancel Reservation", cancel.getText().toString());

        cancel.performClick();
        assertEquals(1, callbackCount.get());
    }

    @Test
    public void bind_cancelled_disablesCancelAndDoesNotInvokeCallback() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ReservationAdapter adapter = new ReservationAdapter(mockOptions(), empty -> {}, reservation -> callbackCount.incrementAndGet());
        View itemView = inflateReservationItemView();
        ReservationAdapter.ReservationHolder holder = adapter.new ReservationHolder(itemView);

        holder.bind(createReservation("cancelled", 1));

        TextView status = itemView.findViewById(R.id.tvReservationStatus);
        MaterialButton cancel = itemView.findViewById(R.id.btnCancelReservation);

        assertEquals("Status: Cancelled", status.getText().toString());
        assertFalse(cancel.isEnabled());
        assertEquals("Cancelled", cancel.getText().toString());

        cancel.performClick();
        assertEquals(0, callbackCount.get());
    }

    @Test
    public void onDataChanged_notifiesEmptyStateForNoItems() {
        AtomicBoolean isEmpty = new AtomicBoolean(false);
        ReservationAdapter adapter = new ReservationAdapter(mockOptions(), isEmpty::set, reservation -> {});

        adapter.onDataChanged();

        assertTrue(isEmpty.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onCreateAndOnBindViewHolder_setsDocumentIdFromSnapshot() {
        ObservableSnapshotArray<Reservation> snapshots = mock(ObservableSnapshotArray.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshots.getSnapshot(0)).thenReturn(snapshot);
        when(snapshot.getId()).thenReturn("reservation-doc-9");

        ReservationAdapter adapter = new ReservationAdapter(mockOptions(snapshots), empty -> {}, reservation -> {});
        Context appContext = ApplicationProvider.getApplicationContext();
        Context themedContext = new ContextThemeWrapper(appContext, R.style.Theme_TicketReservationApp);
        FrameLayout parent = new FrameLayout(themedContext);
        ReservationAdapter.ReservationHolder holder = adapter.onCreateViewHolder(parent, 0);

        Reservation reservation = createReservation("confirmed", 1);
        adapter.onBindViewHolder(holder, 0, reservation);

        assertEquals("reservation-doc-9", reservation.getId());
    }

    @Test
    public void onDataChanged_withNullEmptyStateListener_doesNotCrash() {
        ReservationAdapter adapter = new ReservationAdapter(mockOptions(), null, reservation -> {});
        adapter.onDataChanged();
    }
}
