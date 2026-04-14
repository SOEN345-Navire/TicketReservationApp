package com.example.ticketreservationapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MockMainActivity extends AppCompatActivity {

    private static final String FULL_EVENT_MESSAGE = "Event is full, no tickets are available";
    private static final String AVAILABLE_EVENT_MESSAGE = "Tickets available";
    private static final String EVENT_CANCELLED_MESSAGE = "Event Cancelled";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String MOCK_USER_ID = "mock-user";
    private static final List<Event> SEEDED_EVENTS = createSeededEvents();
    private static boolean sessionInitialized = false;
    private static List<Event> sessionEvents;
    private static List<Reservation> sessionReservations;

    private MaterialToolbar toolbar;
    private RecyclerView rvEvents;
    private RecyclerView rvReservations;
    private TextView tvEmptyReservations;
    private TextView tvNoMatchingResultsFromFilter;
    private TabLayout tabsReservations;
    private SeededEventAdapter eventAdapter;
    private SeededReservationAdapter reservationAdapter;
    private List<Event> allEvents;
    private EventFilter activeFilter = EventFilter.none();
    private List<Reservation> allReservations;
    private final List<Reservation> visibleReservations = new ArrayList<>();
    private int selectedReservationsTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.mainToolbar);
        toolbar.setTitle("Book Events");

        rvEvents = findViewById(R.id.rvEvents);
        rvReservations = findViewById(R.id.rvReservations);
        tabsReservations = findViewById(R.id.tabsReservations);
        tvEmptyReservations = findViewById(R.id.tvEmptyReservations);
        tvNoMatchingResultsFromFilter = findViewById(R.id.tvNoMatchingResultsFromFilter);
        MaterialButton logout = findViewById(R.id.logout);
        MaterialButton btnFilter = findViewById(R.id.btnFilter);
        MaterialButton btnClearFilters = findViewById(R.id.btnClearFilters);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        rvReservations.setVisibility(View.GONE);
        tabsReservations.setVisibility(View.GONE);
        tvEmptyReservations.setVisibility(View.GONE);
        tvNoMatchingResultsFromFilter.setVisibility(View.GONE);

        logout.setOnClickListener(v -> { });
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnClearFilters.setOnClickListener(v -> applyFilter(EventFilter.none()));

        bottomNav.setOnItemSelectedListener(item -> {
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
        bottomNav.setSelectedItemId(R.id.nav_events);

        initSessionStateIfNeeded();
        allEvents = sessionEvents;
        allReservations = sessionReservations;

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new SeededEventAdapter(new ArrayList<>(allEvents), this::bookEvent);
        rvEvents.setAdapter(eventAdapter);
        rvEvents.setHasFixedSize(true);

        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        reservationAdapter = new SeededReservationAdapter(visibleReservations, this::cancelReservation);
        rvReservations.setAdapter(reservationAdapter);
        rvReservations.setHasFixedSize(true);

        initReservationsTabs();

        showEventsTab();
    }

    private void initSessionStateIfNeeded() {
        if (sessionInitialized) {
            return;
        }

        allEvents = copyEvents(SEEDED_EVENTS);
        allReservations = new ArrayList<>();
        seedReservations();

        sessionEvents = allEvents;
        sessionReservations = allReservations;
        sessionInitialized = true;
    }

    public static List<Event> getSeededEvents() {
        return Collections.unmodifiableList(copyEvents(SEEDED_EVENTS));
    }

    private static List<Event> copyEvents(List<Event> source) {
        List<Event> copy = new ArrayList<>();
        for (Event event : source) {
            Event eventCopy = new Event(
                    event.getName(),
                    event.getDate(),
                    event.getLocation(),
                    event.getCategory(),
                    event.getReservedPlaces(),
                    event.getMaxPlaces(),
                    event.getStatus()
            );
            eventCopy.setId(event.getId());
            copy.add(eventCopy);
        }
        return copy;
    }

    private void bookEvent(Event event, int ticketCount) {
        Reservation reservation = new Reservation(
                "reservation-" + (allReservations.size() + 1),
                MOCK_USER_ID,
                event.getId(),
                event.getName(),
                event.getLocation(),
                event.getCategory(),
                STATUS_CONFIRMED,
                event.getDate(),
                ticketCount
        );

        allReservations.add(0, reservation);
        refreshVisibleReservations();
        updateReservationsEmptyState();
    }

    private void cancelReservation(Reservation reservation) {
        reservation.setStatus(STATUS_CANCELLED);
        refreshVisibleReservations();
        updateReservationsEmptyState();
    }

    private void showEventsTab() {
        rvEvents.setVisibility(View.VISIBLE);
        rvReservations.setVisibility(View.GONE);
        tabsReservations.setVisibility(View.GONE);
        tvEmptyReservations.setVisibility(View.GONE);
        toolbar.setTitle("Book Events");
        refreshEventsEmptyUi();
    }

    private void showReservationsTab() {
        rvEvents.setVisibility(View.GONE);
        rvReservations.setVisibility(View.VISIBLE);
        tabsReservations.setVisibility(View.VISIBLE);
        tvNoMatchingResultsFromFilter.setVisibility(View.GONE);
        toolbar.setTitle(getString(R.string.my_reservations));

        TabLayout.Tab selectedTab = tabsReservations.getTabAt(selectedReservationsTab);
        if (selectedTab != null) {
            selectedTab.select();
        }

        refreshVisibleReservations();
        updateReservationsEmptyState();
    }

    private void updateReservationsEmptyState() {
        boolean isReservationsVisible = rvReservations.getVisibility() == View.VISIBLE;
        boolean isEmpty = visibleReservations.isEmpty();
        tvEmptyReservations.setVisibility(isReservationsVisible && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void initReservationsTabs() {
        if (tabsReservations.getTabCount() == 0) {
            tabsReservations.addTab(tabsReservations.newTab().setText(R.string.reservations_tab_active));
            tabsReservations.addTab(tabsReservations.newTab().setText(R.string.reservations_tab_past));
        }

        tabsReservations.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedReservationsTab = tab.getPosition();
                refreshVisibleReservations();
                updateReservationsEmptyState();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void refreshVisibleReservations() {
        visibleReservations.clear();

        String expectedStatus = selectedReservationsTab == 0 ? STATUS_CONFIRMED : STATUS_CANCELLED;
        for (Reservation reservation : allReservations) {
            if (expectedStatus.equalsIgnoreCase(reservation.getStatus())) {
                visibleReservations.add(reservation);
            }
        }

        if (reservationAdapter != null) {
            reservationAdapter.notifyDataSetChanged();
        }
    }

    private void seedReservations() {
        allReservations.clear();

        Event activeEvent = allEvents.get(0);
        Event cancelledEvent = allEvents.get(1);

        allReservations.add(new Reservation(
                "reservation-seeded-1",
                MOCK_USER_ID,
                activeEvent.getId(),
                activeEvent.getName(),
                activeEvent.getLocation(),
                activeEvent.getCategory(),
                STATUS_CONFIRMED,
                activeEvent.getDate(),
                1
        ));

        allReservations.add(new Reservation(
                "reservation-seeded-2",
                MOCK_USER_ID,
                cancelledEvent.getId(),
                cancelledEvent.getName(),
                cancelledEvent.getLocation(),
                cancelledEvent.getCategory(),
                STATUS_CANCELLED,
                cancelledEvent.getDate(),
                2
        ));

        refreshVisibleReservations();
    }

    private void refreshEventsEmptyUi() {
        boolean isEventsVisible = rvEvents.getVisibility() == View.VISIBLE;
        boolean isEmpty = eventAdapter == null || eventAdapter.getItemCount() == 0;
        boolean filterApplied = activeFilter != null && activeFilter.type() != EventFilter.Type.NONE;
        tvNoMatchingResultsFromFilter.setVisibility(isEventsVisible && filterApplied && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showFilterDialog() {
        String[] options = new String[] {
                "Search by Location",
                "Filter by Category",
                "Filter by Date"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter Events")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showLocationFilterDialog();
                    } else if (which == 1) {
                        showCategoryFilterDialog();
                    } else if (which == 2) {
                        showDateFilterDialog();
                    }
                })
                .show();
    }

    private void showLocationFilterDialog() {
        final TextInputEditText input = new TextInputEditText(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Search by Location")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString().trim();
                    applyFilter(EventFilter.locationPrefix(text));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCategoryFilterDialog() {
        final TextInputEditText input = new TextInputEditText(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter by Category")
                .setView(input)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString().trim();
                    applyFilter(EventFilter.category(text));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDateFilterDialog() {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    applyFilter(EventFilter.singleDate(selected));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void applyFilter(EventFilter filter) {
        activeFilter = filter == null ? EventFilter.none() : filter;
        List<Event> filtered = filterEvents(allEvents, activeFilter);
        eventAdapter.replaceEvents(filtered);
        refreshEventsEmptyUi();
    }

    private static List<Event> filterEvents(List<Event> source, EventFilter filter) {
        if (filter == null || filter.type() == EventFilter.Type.NONE) {
            return new ArrayList<>(source);
        }

        List<Event> filtered = new ArrayList<>();
        for (Event event : source) {
            if (matchesFilter(event, filter)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private static boolean matchesFilter(Event event, EventFilter filter) {
        if (filter.type() == EventFilter.Type.LOCATION_PREFIX) {
            String text = filter.text() == null ? "" : filter.text().trim().toLowerCase(Locale.getDefault());
            return event.getLocation() != null && event.getLocation().toLowerCase(Locale.getDefault()).startsWith(text);
        }

        if (filter.type() == EventFilter.Type.CATEGORY) {
            String text = filter.text() == null ? "" : filter.text().trim().toLowerCase(Locale.getDefault());
            return event.getCategory() != null && event.getCategory().toLowerCase(Locale.getDefault()).equals(text);
        }

        if (filter.type() == EventFilter.Type.SINGLE_DATE) {
            if (filter.day() == null || event.getDate() == null) {
                return false;
            }

            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(event.getDate().toDate());
            Calendar selected = filter.day();

            return eventDay.get(Calendar.YEAR) == selected.get(Calendar.YEAR)
                    && eventDay.get(Calendar.MONTH) == selected.get(Calendar.MONTH)
                    && eventDay.get(Calendar.DAY_OF_MONTH) == selected.get(Calendar.DAY_OF_MONTH);
        }

        return true;
    }

    private static List<Event> createSeededEvents() {
        List<Event> events = new ArrayList<>();
        events.add(createEvent(
                "event-1",
                "Jazz Night",
                toTimestamp(2026, Calendar.OCTOBER, 20, 19, 30),
                "Montreal Arena",
                "concert",
                40,
                100,
                "active"
        ));
        events.add(createEvent(
                "event-2",
                "City Marathon",
                toTimestamp(2026, Calendar.OCTOBER, 21, 8, 0),
                "Old Port",
                "sports",
                50,
                50,
                "active"
        ));
        events.add(createEvent(
                "event-3",
                "Indie Film Premiere",
                toTimestamp(2026, Calendar.OCTOBER, 22, 20, 0),
                "Cinema Banque",
                "movies",
                10,
                80,
                "cancelled"
        ));
        events.add(createEvent(
                "event-4",
                "Mountain Sunrise Hike",
                toTimestamp(2026, Calendar.OCTOBER, 23, 6, 30),
                "Mont-Tremblant",
                "travel",
                12,
                30,
                "active"
        ));
        return events;
    }

    private static Event createEvent(String id, String name, Timestamp date, String location, String category,
                                     int reservedPlaces, int maxPlaces, String status) {
        Event event = new Event(name, date, location, category, reservedPlaces, maxPlaces, status);
        event.setId(id);
        return event;
    }

    private static Timestamp toTimestamp(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    private static class SeededEventAdapter extends RecyclerView.Adapter<SeededEventAdapter.SeededEventHolder> {
        private interface ReserveListener {
            void onReserve(Event event, int quantity);
        }

        private final List<Event> events;
        private final ReserveListener reserveListener;

        SeededEventAdapter(List<Event> events, ReserveListener reserveListener) {
            this.events = events;
            this.reserveListener = reserveListener;
        }

        @NonNull
        @Override
        public SeededEventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new SeededEventHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull SeededEventHolder holder, int position) {
            holder.bind(events.get(position));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        void replaceEvents(List<Event> updatedEvents) {
            events.clear();
            events.addAll(updatedEvents);
            notifyDataSetChanged();
        }

        private class SeededEventHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvCategory;
            private final TextView tvLocation;
            private final TextView tvDateTime;
            private final TextView tvReservedPlaces;
            private final TextView tvMaxPlaces;
            private final TextView tvAvailabilityStatus;
            private final TextView tvStatus;
            private final TextView tvTicketQuantity;
            private final MaterialButton btnEdit;
            private final MaterialButton btnCancel;
            private final MaterialButton btnReserve;
            private final MaterialButton btnIncreaseTickets;
            private final MaterialButton btnDecreaseTickets;
            private final View layoutQuantitySelector;
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

            SeededEventHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvReservedPlaces = itemView.findViewById(R.id.tvReservedPlaces);
                tvMaxPlaces = itemView.findViewById(R.id.tvMaxPlaces);
                tvAvailabilityStatus = itemView.findViewById(R.id.tvAvailabilityStatus);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvTicketQuantity = itemView.findViewById(R.id.tvTicketQuantity);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnCancel = itemView.findViewById(R.id.btnCancel);
                btnReserve = itemView.findViewById(R.id.btnReserve);
                btnIncreaseTickets = itemView.findViewById(R.id.btnIncreaseTickets);
                btnDecreaseTickets = itemView.findViewById(R.id.btnDecreaseTickets);
                layoutQuantitySelector = itemView.findViewById(R.id.layoutQuantitySelector);
            }

            void bind(Event event) {
                int reservedPlaces = event.getReservedPlaces();
                int maxPlaces = event.getMaxPlaces();
                int availableTickets = maxPlaces - reservedPlaces;
                boolean isFull = availableTickets == 0;
                boolean isCancelled = "cancelled".equalsIgnoreCase(event.getStatus());

                tvName.setText(event.getName());
                tvCategory.setText(event.getCategory());
                tvLocation.setText(event.getLocation());
                tvDateTime.setText(dateFormat.format(event.getDate().toDate()));
                tvReservedPlaces.setText("Reserved: " + reservedPlaces);
                tvMaxPlaces.setText("Limit: " + maxPlaces);

                btnEdit.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                btnReserve.setVisibility(View.VISIBLE);
                layoutQuantitySelector.setVisibility(View.VISIBLE);

                boolean canReserve = !isFull && !isCancelled;
                final int[] selectedQuantity = {canReserve ? 1 : 0};
                tvTicketQuantity.setText(String.valueOf(selectedQuantity[0]));
                btnReserve.setEnabled(canReserve);
                btnReserve.setAlpha(canReserve ? 1f : 0.5f);
                btnDecreaseTickets.setEnabled(selectedQuantity[0] > 1);
                btnIncreaseTickets.setEnabled(canReserve && selectedQuantity[0] < availableTickets);

                btnDecreaseTickets.setOnClickListener(v -> {
                    if (selectedQuantity[0] <= 1) return;
                    selectedQuantity[0]--;
                    tvTicketQuantity.setText(String.valueOf(selectedQuantity[0]));
                    btnDecreaseTickets.setEnabled(selectedQuantity[0] > 1);
                    btnIncreaseTickets.setEnabled(selectedQuantity[0] < availableTickets);
                });

                btnIncreaseTickets.setOnClickListener(v -> {
                    if (selectedQuantity[0] >= availableTickets) return;
                    selectedQuantity[0]++;
                    tvTicketQuantity.setText(String.valueOf(selectedQuantity[0]));
                    btnDecreaseTickets.setEnabled(selectedQuantity[0] > 1);
                    btnIncreaseTickets.setEnabled(selectedQuantity[0] < availableTickets);
                });

                btnReserve.setOnClickListener(v -> {
                    if (!canReserve) return;

                    int nextReserved = Math.min(event.getMaxPlaces(), event.getReservedPlaces() + selectedQuantity[0]);
                    event.setReservedPlaces(nextReserved);

                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    }

                    reserveListener.onReserve(event, selectedQuantity[0]);
                    Toast.makeText(v.getContext(), selectedQuantity[0] + " ticket(s) booked!", Toast.LENGTH_SHORT).show();
                });

                if (isCancelled) {
                    tvStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("CANCELLED");
                    tvStatus.getBackground().setTint(Color.parseColor("#FFCDD2"));
                    tvStatus.setTextColor(Color.parseColor("#C62828"));
                    tvAvailabilityStatus.setText(EVENT_CANCELLED_MESSAGE);
                    tvAvailabilityStatus.setTextColor(Color.parseColor("#C62828"));
                } else {
                    tvStatus.setVisibility(View.GONE);
                    tvAvailabilityStatus.setText(isFull ? FULL_EVENT_MESSAGE : AVAILABLE_EVENT_MESSAGE);
                    tvAvailabilityStatus.setTextColor(Color.parseColor(isFull ? "#C62828" : "#2E7D32"));
                }
            }
        }
    }

    private static class SeededReservationAdapter extends RecyclerView.Adapter<SeededReservationAdapter.ReservationHolder> {
        private interface ReservationActionListener {
            void onCancel(Reservation reservation);
        }

        private final List<Reservation> reservations;
        private final ReservationActionListener actionListener;

        SeededReservationAdapter(List<Reservation> reservations, ReservationActionListener actionListener) {
            this.reservations = reservations;
            this.actionListener = actionListener;
        }

        @NonNull
        @Override
        public ReservationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
            return new ReservationHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ReservationHolder holder, int position) {
            holder.bind(reservations.get(position));
        }

        @Override
        public int getItemCount() {
            return reservations.size();
        }

        private class ReservationHolder extends RecyclerView.ViewHolder {
            private final TextView tvEventName;
            private final TextView tvCategory;
            private final TextView tvLocation;
            private final TextView tvEventDate;
            private final TextView tvTicketCount;
            private final TextView tvStatus;
            private final MaterialButton btnCancelReservation;
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

            ReservationHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tvReservationEventName);
                tvCategory = itemView.findViewById(R.id.tvReservationCategory);
                tvLocation = itemView.findViewById(R.id.tvReservationLocation);
                tvEventDate = itemView.findViewById(R.id.tvReservationEventDate);
                tvTicketCount = itemView.findViewById(R.id.tvReservationTicketCount);
                tvStatus = itemView.findViewById(R.id.tvReservationStatus);
                btnCancelReservation = itemView.findViewById(R.id.btnCancelReservation);
            }

            void bind(Reservation reservation) {
                tvEventName.setText(reservation.getEventName());
                tvCategory.setText(reservation.getEventCategory());
                tvLocation.setText(reservation.getEventLocation());
                tvEventDate.setText(dateFormat.format(reservation.getEventDate().toDate()));
                tvTicketCount.setText(itemView.getContext().getString(R.string.tickets_count, reservation.getTicketCount()));

                boolean isCancelled = STATUS_CANCELLED.equalsIgnoreCase(reservation.getStatus());
                if (isCancelled) {
                    tvStatus.setText(itemView.getContext().getString(R.string.status_cancelled));
                    tvStatus.setTextColor(0xFFC62828);
                    btnCancelReservation.setEnabled(false);
                    btnCancelReservation.setText(itemView.getContext().getString(R.string.reservation_cancelled));
                    btnCancelReservation.setAlpha(0.6f);
                } else {
                    tvStatus.setText(itemView.getContext().getString(R.string.status_confirmed));
                    tvStatus.setTextColor(0xFF2E7D32);
                    btnCancelReservation.setEnabled(true);
                    btnCancelReservation.setText(itemView.getContext().getString(R.string.cancel_reservation));
                    btnCancelReservation.setAlpha(1f);
                }

                btnCancelReservation.setVisibility(View.VISIBLE);
                btnCancelReservation.setOnClickListener(v -> {
                    if (!isCancelled) {
                        actionListener.onCancel(reservation);
                    }
                });
            }
        }
    }
}