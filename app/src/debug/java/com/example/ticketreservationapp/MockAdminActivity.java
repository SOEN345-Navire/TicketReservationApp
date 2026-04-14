package com.example.ticketreservationapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MockAdminActivity extends AppCompatActivity {

    private static final String FULL_EVENT_MESSAGE = "Event is full, no tickets are available";
    private static final String AVAILABLE_EVENT_MESSAGE = "Tickets available";

    private final List<Event> events = new ArrayList<>();
    private AdminEventAdapter adapter;
    private int nextEventId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        RecyclerView recyclerView = findViewById(R.id.rvEvents);
        ExtendedFloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        Button logout = findViewById(R.id.logout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEventAdapter(events, this::confirmCancelEvent, this::confirmDeleteEvent);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        btnAdd.setOnClickListener(v -> showAddEventDialog());
        logout.setOnClickListener(v -> finish());
    }

    private void showAddEventDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.add_event, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etLocation = view.findViewById(R.id.etLocation);
        TextInputEditText etCategory = view.findViewById(R.id.etCategory);
        TextInputEditText etMax = view.findViewById(R.id.etMaxPlaces);
        MaterialButton btnPickDate = view.findViewById(R.id.btnPickDate);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveEvent);

        tvTitle.setText("Add New Event");
        btnSave.setText("Create Event");

        Calendar calendar = Calendar.getInstance();
        updateDateButtonText(btnPickDate, calendar);

        btnPickDate.setOnClickListener(v -> showDateTimePicker(calendar, btnPickDate));

        btnSave.setOnClickListener(v -> {
            String name = text(etName);
            String location = text(etLocation);
            String category = text(etCategory);
            String maxPlacesValue = text(etMax);

            if (name.isEmpty() || location.isEmpty() || category.isEmpty() || maxPlacesValue.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int maxPlaces;
            try {
                maxPlaces = Integer.parseInt(maxPlacesValue);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Max places must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (maxPlaces <= 0) {
                Toast.makeText(this, "Max places must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            Event event = new Event(
                    name,
                    new Timestamp(calendar.getTime()),
                    location,
                    category,
                    0,
                    maxPlaces,
                    "active"
            );
            event.setId("mock-event-" + nextEventId++);

            events.add(event);
            adapter.notifyItemInserted(events.size() - 1);

            Toast.makeText(this, "Event Added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void showDateTimePicker(Calendar calendar, MaterialButton button) {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                updateDateButtonText(button, calendar);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtonText(MaterialButton button, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        button.setText(sdf.format(calendar.getTime()));
    }

    private void confirmCancelEvent(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Event")
                .setMessage("Are you sure you want to cancel this event?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    event.setStatus("cancelled");
                    int index = indexOfEvent(event.getId());
                    if (index >= 0) {
                        adapter.notifyItemChanged(index);
                    }
                    Toast.makeText(this, "Event Cancelled!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmDeleteEvent(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    int index = indexOfEvent(event.getId());
                    if (index >= 0) {
                        events.remove(index);
                        adapter.notifyItemRemoved(index);
                    }
                    Toast.makeText(this, "Event Deleted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private int indexOfEvent(String id) {
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (id != null && id.equals(event.getId())) {
                return i;
            }
        }
        return -1;
    }

    private static class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.EventHolder> {

        interface CancelListener {
            void onCancel(Event event);
        }

        interface DeleteListener {
            void onDelete(Event event);
        }

        private final List<Event> events;
        private final CancelListener cancelListener;
        private final DeleteListener deleteListener;

        AdminEventAdapter(List<Event> events, CancelListener cancelListener, DeleteListener deleteListener) {
            this.events = events;
            this.cancelListener = cancelListener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new EventHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull EventHolder holder, int position) {
            holder.bind(events.get(position));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        private class EventHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvCategory;
            private final TextView tvLocation;
            private final TextView tvDateTime;
            private final TextView tvReservedPlaces;
            private final TextView tvMaxPlaces;
            private final TextView tvAvailabilityStatus;
            private final TextView tvStatus;
            private final MaterialButton btnEdit;
            private final MaterialButton btnCancel;
            private final MaterialButton btnReserve;
            private final View layoutQuantitySelector;
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

            EventHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvReservedPlaces = itemView.findViewById(R.id.tvReservedPlaces);
                tvMaxPlaces = itemView.findViewById(R.id.tvMaxPlaces);
                tvAvailabilityStatus = itemView.findViewById(R.id.tvAvailabilityStatus);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnCancel = itemView.findViewById(R.id.btnCancel);
                btnReserve = itemView.findViewById(R.id.btnReserve);
                layoutQuantitySelector = itemView.findViewById(R.id.layoutQuantitySelector);
            }

            void bind(Event event) {
                int reservedPlaces = event.getReservedPlaces();
                int maxPlaces = event.getMaxPlaces();
                boolean isFull = reservedPlaces >= maxPlaces;
                boolean isCancelled = "cancelled".equalsIgnoreCase(event.getStatus());

                tvName.setText(event.getName());
                tvCategory.setText(event.getCategory());
                tvLocation.setText(event.getLocation());
                tvDateTime.setText(dateFormat.format(event.getDate().toDate()));
                tvReservedPlaces.setText("Reserved: " + reservedPlaces);
                tvMaxPlaces.setText("Limit: " + maxPlaces);

                btnReserve.setVisibility(View.GONE);
                layoutQuantitySelector.setVisibility(View.GONE);

                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("Cancel");
                btnCancel.setEnabled(!isCancelled);
                btnCancel.setAlpha(isCancelled ? 0.5f : 1f);

                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setText("Delete");

                if (isCancelled) {
                    tvStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("CANCELLED");
                    tvStatus.getBackground().setTint(Color.parseColor("#FFCDD2"));
                    tvStatus.setTextColor(Color.parseColor("#C62828"));
                    tvAvailabilityStatus.setText("Event Cancelled");
                    tvAvailabilityStatus.setTextColor(Color.parseColor("#C62828"));
                } else {
                    tvStatus.setVisibility(View.GONE);
                    tvAvailabilityStatus.setText(isFull ? FULL_EVENT_MESSAGE : AVAILABLE_EVENT_MESSAGE);
                    tvAvailabilityStatus.setTextColor(Color.parseColor(isFull ? "#C62828" : "#2E7D32"));
                }

                btnCancel.setOnClickListener(v -> {
                    if (!isCancelled) {
                        cancelListener.onCancel(event);
                    }
                });

                btnEdit.setOnClickListener(v -> deleteListener.onDelete(event));
            }
        }
    }
}
