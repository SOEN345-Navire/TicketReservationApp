package com.example.ticketreservationapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String FULL_EVENT_MESSAGE = "Event is full, no tickets are available";
    private static final String NOT_ENOUGH_TICKETS_MESSAGE = "Not enough tickets available for selected quantity";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_CANCELLED = "cancelled";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference eventsRef = db.collection("events");
    private final CollectionReference reservationsRef = db.collection("reservations");
    private EventAdapter eventAdapter;
    private ReservationAdapter reservationAdapter;
    private RecyclerView rvEvents;
    private RecyclerView rvReservations;
    private TextView tvEmptyReservations;
    private MaterialToolbar toolbar;
    private TabLayout reservationsTabs;
    private int selectedReservationsFilterTab = 0;
    private TextView tvNoMatchingResultsFromFilter;
    private boolean filterApplied = false;
    private final EventAdapter.EmptyStateListener eventsEmptyListener = empty -> refreshEventsEmptyUi();
    FirebaseAuth auth;

    FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            checkUserStatus(user);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        rvEvents = findViewById(R.id.rvEvents);
        rvReservations = findViewById(R.id.rvReservations);
        tvEmptyReservations = findViewById(R.id.tvEmptyReservations);
        toolbar = findViewById(R.id.mainToolbar);
        reservationsTabs = findViewById(R.id.tabsReservations);

        tvNoMatchingResultsFromFilter = findViewById(R.id.tvNoMatchingResultsFromFilter);

        initAuth();
        initEventsRecyclerView();
        initReservationsRecyclerView();

        Button btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> showFilterDialog());
        Button btnClearFilters = findViewById(R.id.btnClearFilters);
        btnClearFilters.setOnClickListener(v -> applyFilter(EventFilter.none()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reservationAdapter == null && auth.getCurrentUser() != null) {
            initReservationsRecyclerView();
        }
        if (eventAdapter != null) eventAdapter.startListening();
        if (reservationAdapter != null) reservationAdapter.startListening();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventAdapter != null) eventAdapter.stopListening();
        if (reservationAdapter != null) reservationAdapter.stopListening();
        auth.removeAuthStateListener(authStateListener);
    }


    private void checkUserStatus(FirebaseUser user) {
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LogInActivity.class));
            finish();
            return;
        }

        if (!user.isEmailVerified() && user.getEmail() != null && user.getEmail().matches(Authentification.emailRegex)) {
            startActivity(new Intent(MainActivity.this, ConfirmEmailActivity.class));
           finish();
        }
        Authentification.isAdmin(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    boolean isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                    if (isAdmin) {
                        startActivity(new Intent(MainActivity.this, AdminActivity.class));
                        finish();
                    }
                } else {
                    Authentification.setAdmin(user.getUid(), false);
                }
            }
        });
    }

    private void initAuth() {
        auth = FirebaseAuth.getInstance();
        Button logout = findViewById(R.id.logout);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNav);

        logout.setOnClickListener(v -> auth.signOut());

        bottomNavigation.setSelectedItemId(R.id.nav_events);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_reservations) {
                showReservationsTab();
                return true;
            }
            if (item.getItemId() == R.id.nav_events) {
                showEventsTab();
                return true;
            }
            return false;
        });

        showEventsTab();
    }

    private void initEventsRecyclerView() {
        Query query = eventsRef.orderBy("date", Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<Event> options = new FirestoreRecyclerOptions.Builder<Event>()
                .setQuery(query, Event.class)
                .build();

        eventAdapter = new EventAdapter(options, this::reserveTicket, eventsEmptyListener);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
        rvEvents.setHasFixedSize(true);
    }

    private void initReservationsRecyclerView() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (reservationsTabs.getTabCount() == 0) {
            reservationsTabs.addTab(reservationsTabs.newTab().setText(R.string.reservations_tab_active));
            reservationsTabs.addTab(reservationsTabs.newTab().setText(R.string.reservations_tab_past));
            reservationsTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    selectedReservationsFilterTab = tab.getPosition();
                    bindReservationsAdapter();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        rvReservations.setHasFixedSize(true);

        bindReservationsAdapter();
    }


    private void bindReservationsAdapter() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String filterStatus = selectedReservationsFilterTab == 0 ? STATUS_CONFIRMED : STATUS_CANCELLED;
        Query query = reservationsRef
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", filterStatus);

        FirestoreRecyclerOptions<Reservation> options = new FirestoreRecyclerOptions.Builder<Reservation>()
                .setQuery(query, Reservation.class)
                .build();

        if (reservationAdapter != null) {
            reservationAdapter.stopListening();
        }

        reservationAdapter = new ReservationAdapter(options, this::updateReservationsEmptyState, this::confirmCancelReservation);
        rvReservations.setAdapter(reservationAdapter);

        reservationAdapter.startListening();

        updateReservationsEmptyState(reservationAdapter.getItemCount() == 0);
    }
    private void refreshEventsEmptyUi() {
        if (tvNoMatchingResultsFromFilter == null) return;

        boolean isEventsVisible = rvEvents.getVisibility() == View.VISIBLE;
        boolean isEmpty = (eventAdapter == null) || eventAdapter.getItemCount() == 0;

        tvNoMatchingResultsFromFilter.setVisibility(isEventsVisible && filterApplied && isEmpty ? View.VISIBLE : View.GONE
        );
    }
    private void showEventsTab() {
        rvEvents.setVisibility(View.VISIBLE);
        reservationsTabs.setVisibility(View.GONE);
        rvReservations.setVisibility(View.GONE);
        tvEmptyReservations.setVisibility(View.GONE);
        toolbar.setTitle("Book Events");

        refreshEventsEmptyUi();
    }

    private void showReservationsTab() {
        rvEvents.setVisibility(View.GONE);
        reservationsTabs.setVisibility(View.VISIBLE);
        rvReservations.setVisibility(View.VISIBLE);
        toolbar.setTitle(getString(R.string.my_reservations));

        if (reservationAdapter == null) {
            initReservationsRecyclerView();
        }

        if (tvNoMatchingResultsFromFilter != null) {
            tvNoMatchingResultsFromFilter.setVisibility(View.GONE);
        }

        boolean isEmpty = reservationAdapter == null || reservationAdapter.getItemCount() == 0;
        updateReservationsEmptyState(isEmpty);
    }

    private void updateReservationsEmptyState(boolean isEmpty) {
        boolean isReservationsVisible = rvReservations.getVisibility() == View.VISIBLE;
        tvEmptyReservations.setVisibility(isReservationsVisible && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void confirmCancelReservation(Reservation reservation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancel_reservation)
                .setMessage(R.string.cancel_reservation_confirmation)
                .setPositiveButton(R.string.cancel_reservation, (dialog, which) -> cancelReservation(reservation))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void cancelReservation(Reservation reservation) {
        DocumentReference reservationRef = reservationsRef.document(reservation.getId());

        db.runTransaction(transaction -> {
            DocumentSnapshot reservationSnapshot = transaction.get(reservationRef);

            String status = reservationSnapshot.getString("status");
            if (!STATUS_CONFIRMED.equals(status)) {
                throw new IllegalStateException("Reservation already cancelled");
            }

            Reservation emailReservation = new Reservation(
                    reservation.getId(),
                    reservation.getUserId(),
                    reservation.getEventId(),
                    reservation.getEventName(),
                    reservation.getEventLocation(),
                    reservation.getEventCategory(),
                    STATUS_CANCELLED,
                    reservation.getEventDate(),
                    reservation.getTicketCount()
            );
            emailConfirmations.confirmReservation(auth.getCurrentUser(), emailReservation, "Reservation Cancelled", "A reservation has been cancelled: \n");

            String eventId = reservationSnapshot.getString("eventId");
            int ticketCount = Math.max(1, reservationSnapshot.getLong("ticketCount").intValue());

            DocumentReference eventRef = eventsRef.document(eventId);
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            int reservedPlaces = eventSnapshot.getLong("reservedPlaces").intValue();

            int updatedReservedPlaces = Math.max(0, reservedPlaces - ticketCount);
            transaction.update(eventRef, "reservedPlaces", updatedReservedPlaces);
            transaction.update(reservationRef, "status", STATUS_CANCELLED, "cancelledAt", FieldValue.serverTimestamp());

            return ticketCount;
        }).addOnSuccessListener(ticketCount -> Toast.makeText(
                this,
                getString(R.string.reservation_cancelled_success, ticketCount),
                Toast.LENGTH_LONG
        ).show()).addOnFailureListener(e -> {
            String message = e.getMessage();
            if ("Reservation already cancelled".equals(message)) {
                Toast.makeText(this, R.string.reservation_already_cancelled, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.cancel_reservation_failed, Toast.LENGTH_LONG).show();
        });
    }

    private void reserveTicket(Event event, int ticketCount) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to book tickets.", Toast.LENGTH_LONG).show();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference reservationRef = reservationsRef.document();

        db.runTransaction(transaction -> {
            DocumentReference eventRef = eventsRef.document(event.getId());
            DocumentSnapshot snapshot = transaction.get(eventRef);

            int reservedPlaces = snapshot.getLong("reservedPlaces").intValue();
            int maxPlaces = snapshot.getLong("maxPlaces").intValue();

            if (reservedPlaces + ticketCount > maxPlaces) {
                throw new IllegalStateException(ticketCount == 1 ? FULL_EVENT_MESSAGE : NOT_ENOUGH_TICKETS_MESSAGE);
            }

            Map<String, Object> reservation = new HashMap<>();
            reservation.put("userId", userId);
            reservation.put("eventId", event.getId());
            reservation.put("eventName", snapshot.getString("name"));
            reservation.put("eventLocation", snapshot.getString("location"));
            reservation.put("eventCategory", snapshot.getString("category"));
            reservation.put("eventDate", snapshot.getTimestamp("date"));
            reservation.put("ticketCount", ticketCount);
            reservation.put("status", STATUS_CONFIRMED);

            Reservation newReservation = new Reservation(
                    reservationRef.getId(),
                    userId,
                    event.getId(),
                    snapshot.getString("name"),
                    snapshot.getString("location"),
                    snapshot.getString("category"),
                    STATUS_CONFIRMED,
                    snapshot.getTimestamp("date"),
                    ticketCount
            );
            emailConfirmations.confirmReservation(currentUser, newReservation, "Reservation Confirmed", "A new reservation has been made: \n");

            transaction.update(eventRef, "reservedPlaces", reservedPlaces + ticketCount);
            transaction.set(reservationRef, reservation);
            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, ticketCount + " ticket(s) booked! Reservation confirmed.", Toast.LENGTH_SHORT).show();
        }
        ).addOnFailureListener(e -> {
            String message = FULL_EVENT_MESSAGE;
            if (NOT_ENOUGH_TICKETS_MESSAGE.equals(e.getMessage())) {
                message = NOT_ENOUGH_TICKETS_MESSAGE;
            } else if (!FULL_EVENT_MESSAGE.equals(e.getMessage())) {
                message = "Unable to book ticket right now. Please try again.";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void applyFilter(EventFilter filter) {
        filterApplied = (filter != null && filter.type() != EventFilter.Type.NONE);
        //android.util.Log.d("MainActivity", "applyFilter type=" + (filter == null ? "null" : filter.type()) + " filterApplied=" + filterApplied);
        Query q = EventQueryBuilder.build(eventsRef, filter);
        bindEventsAdapter(q);
    }

    private void bindEventsAdapter(Query query) {
        FirestoreRecyclerOptions<Event> options =
                new FirestoreRecyclerOptions.Builder<Event>()
                        .setQuery(query, Event.class)
                        .build();

        if (eventAdapter != null) {
            eventAdapter.stopListening();
        }

        eventAdapter = new EventAdapter(options, this::reserveTicket, eventsEmptyListener);
        rvEvents.setAdapter(eventAdapter);
        eventAdapter.startListening();

        refreshEventsEmptyUi();
    }

    private void showFilterDialog() {
        String[] options = new String[] {
                "Search by Location",
                "Filter by Category",
                "Filter by Date",
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter Events")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showLocationInputDialog();
                            break;
                        case 1:
                            showCategoryInputDialog();
                            break;
                        case 2:
                            showDatePickerDialog();
                            break;
                    }
                })
                .show();
    }
    private void showLocationInputDialog() {
        final com.google.android.material.textfield.TextInputEditText input =
                new com.google.android.material.textfield.TextInputEditText(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Search by Location")
                .setView(input)
                .setPositiveButton("Search", (d, w) -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    text = text.trim();
                    applyFilter(EventFilter.locationPrefix(text));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCategoryInputDialog() {
        final com.google.android.material.textfield.TextInputEditText input =
                new com.google.android.material.textfield.TextInputEditText(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter by Category")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    text = text.trim();
                    applyFilter(EventFilter.category(text));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();

        new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    applyFilter(EventFilter.singleDate(selected));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}
