package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EventAdapterTest {

    public static class FakeAdminActivity extends AdminActivity {
        boolean editCalled;
        boolean cancelCalled;

        @Override
        protected void onCreate(Bundle savedInstanceState) {}

        @Override
        public void editEvent(Event event) {
            editCalled = true;
        }

        @Override
        public void cancelEvent(Event event) {
            cancelCalled = true;
        }
    }

    @SuppressWarnings("unchecked")
    private FirestoreRecyclerOptions<Event> mockOptions() {
        ObservableSnapshotArray<Event> snapshots = mock(ObservableSnapshotArray.class);
        return mockOptions(snapshots);
    }

    private void initFirebase() {
        Context context = RuntimeEnvironment.getApplication();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:testapp")
                    .setApiKey("test-api-key")
                    .setProjectId("test-project")
                    .build();
            FirebaseApp.initializeApp(context, options);
        }
    }

    private FirestoreRecyclerOptions<Event> mockOptions(ObservableSnapshotArray<Event> snapshots) {
        return new FirestoreRecyclerOptions.Builder<Event>()
                .setSnapshotArray(snapshots)
                .build();
    }

    private View inflateEventItemView() {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context themedContext = new ContextThemeWrapper(appContext, R.style.Theme_TicketReservationApp);
        return LayoutInflater.from(themedContext)
                .inflate(R.layout.item_event, new FrameLayout(themedContext), false);
    }

    private Event createEvent(int reserved, int max) {
        Event event = new Event();
        event.setId("event-1");
        event.setName("Jazz Night");
        event.setCategory("concert");
        event.setLocation("MTL");
        event.setDate(new Timestamp(new Date()));
        event.setReservedPlaces(reserved);
        event.setMaxPlaces(max);
        event.setStatus("active");
        return event;
    }

    @Test
    public void bind_fullEvent_disablesReserveAndShowsFullMessage() {
        EventAdapter adapter = new EventAdapter(mockOptions(), (event, quantity) -> {});
        View itemView = inflateEventItemView();
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);

        holder.bind(createEvent(5, 5));

        TextView availability = itemView.findViewById(R.id.tvAvailabilityStatus);
        TextView quantity = itemView.findViewById(R.id.tvTicketQuantity);
        MaterialButton reserve = itemView.findViewById(R.id.btnReserve);
        MaterialButton edit = itemView.findViewById(R.id.btnEdit);

        assertEquals("Event is full, no tickets are available", availability.getText().toString());
        assertEquals("0", quantity.getText().toString());
        assertFalse(reserve.isEnabled());
        assertEquals(View.GONE, edit.getVisibility());
    }

    @Test
    public void bind_userMode_updatesQuantityAndInvokesReserveCallback() {
        AtomicInteger selectedQuantity = new AtomicInteger(-1);
        AtomicReference<Event> selectedEvent = new AtomicReference<>();
        EventAdapter adapter = new EventAdapter(mockOptions(), (event, quantity) -> {
            selectedEvent.set(event);
            selectedQuantity.set(quantity);
        });
        View itemView = inflateEventItemView();
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);
        Event event = createEvent(2, 5);

        holder.bind(event);

        TextView quantity = itemView.findViewById(R.id.tvTicketQuantity);
        MaterialButton increase = itemView.findViewById(R.id.btnIncreaseTickets);
        MaterialButton decrease = itemView.findViewById(R.id.btnDecreaseTickets);
        MaterialButton reserve = itemView.findViewById(R.id.btnReserve);

        assertEquals("1", quantity.getText().toString());
        assertFalse(decrease.isEnabled());

        increase.performClick();
        increase.performClick();
        increase.performClick();
        assertEquals("3", quantity.getText().toString());

        decrease.performClick();
        assertEquals("2", quantity.getText().toString());

        reserve.performClick();
        assertSame(event, selectedEvent.get());
        assertEquals(2, selectedQuantity.get());
    }

    @Test
    public void bind_adminMode_showsAdminActionsAndHidesUserControls() {
        EventAdapter adapter = new EventAdapter(mockOptions());
        View itemView = inflateEventItemView();
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);

        holder.bind(createEvent(1, 3));

        assertEquals(View.VISIBLE, itemView.findViewById(R.id.btnEdit).getVisibility());
        assertEquals(View.VISIBLE, itemView.findViewById(R.id.btnCancel).getVisibility());
        assertEquals(View.GONE, itemView.findViewById(R.id.btnReserve).getVisibility());
        assertEquals(View.GONE, itemView.findViewById(R.id.layoutQuantitySelector).getVisibility());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onCreateAndOnBindViewHolder_setsDocumentIdFromSnapshot() {
        ObservableSnapshotArray<Event> snapshots = mock(ObservableSnapshotArray.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshots.getSnapshot(0)).thenReturn(snapshot);
        when(snapshot.getId()).thenReturn("event-doc-42");

        EventAdapter adapter = new EventAdapter(mockOptions(snapshots), (event, quantity) -> {});
        Context appContext = ApplicationProvider.getApplicationContext();
        Context themedContext = new ContextThemeWrapper(appContext, R.style.Theme_TicketReservationApp);
        FrameLayout parent = new FrameLayout(themedContext);
        EventAdapter.EventHolder holder = adapter.onCreateViewHolder(parent, 0);

        Event event = createEvent(1, 3);
        adapter.onBindViewHolder(holder, 0, event);

        assertEquals("event-doc-42", event.getId());
    }

    @Test
    public void updateCategoryUI_coversAllCategoryBranches() {
        EventAdapter adapter = new EventAdapter(mockOptions(), (event, quantity) -> {});
        View itemView = inflateEventItemView();
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);
        TextView categoryView = itemView.findViewById(R.id.tvCategory);

        holder.updateCategoryUI("movies");
        assertEquals("movies", categoryView.getText().toString());

        holder.updateCategoryUI("travel");
        assertEquals("travel", categoryView.getText().toString());

        holder.updateCategoryUI("sports");
        assertEquals("sports", categoryView.getText().toString());

        holder.updateCategoryUI("theatre");
        assertEquals("theatre", categoryView.getText().toString());
    }

    @Test
    public void bind_adminMode_clicksInvokeAdminActions() {
        initFirebase();
        EventAdapter adapter = new EventAdapter(mockOptions());
        ActivityController<FakeAdminActivity> controller = org.robolectric.Robolectric.buildActivity(FakeAdminActivity.class).create();
        FakeAdminActivity activity = controller.get();

        View itemView = LayoutInflater.from(activity)
                .inflate(R.layout.item_event, new FrameLayout(activity), false);
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);

        holder.bind(createEvent(1, 4));

        itemView.findViewById(R.id.btnEdit).performClick();
        itemView.findViewById(R.id.btnCancel).performClick();

        assertTrue(activity.editCalled);
        assertTrue(activity.cancelCalled);
    }
    @Test
    public void onDataChanged_notifiesEmptyStateListener_whenItsEmpty() {
        AtomicReference<Boolean> empty = new AtomicReference<>(null);

        EventAdapter adapter = new EventAdapter(
                mockOptions(),
                (event, quantity) -> {},
                empty::set
        );

        adapter.onDataChanged();

        assertEquals(Boolean.TRUE, empty.get());
    }

    @Test
    public void bind_cancelledEvent_showsCancelledStatusAndDisablesActions() {
        EventAdapter adapter = new EventAdapter(mockOptions(), (event, quantity) -> {});
        View itemView = inflateEventItemView();
        EventAdapter.EventHolder holder = adapter.new EventHolder(itemView);
        Event event = createEvent(0, 10);
        event.setStatus("cancelled");

        holder.bind(event);

        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView tvAvailability = itemView.findViewById(R.id.tvAvailabilityStatus);
        MaterialButton btnReserve = itemView.findViewById(R.id.btnReserve);

        assertEquals(View.VISIBLE, tvStatus.getVisibility());
        assertEquals("CANCELLED", tvStatus.getText().toString());
        assertEquals(Color.parseColor("#C62828"), tvStatus.getCurrentTextColor());

        assertEquals("Event Cancelled", tvAvailability.getText().toString());
        assertEquals(Color.parseColor("#C62828"), tvAvailability.getCurrentTextColor());

        assertFalse(btnReserve.isEnabled());
    }
}
