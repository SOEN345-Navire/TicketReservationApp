package com.example.ticketreservationapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventAdapter extends FirestoreRecyclerAdapter<Event, EventAdapter.EventHolder> {

    private static final String FULL_EVENT_MESSAGE = "Event is full, no tickets are available";
    private static final String AVAILABLE_EVENT_MESSAGE = "Tickets available";
    private final boolean isAdminMode;
    private final ReserveListener reserveListener;
    private final EmptyStateListener emptyStateListener;

    @FunctionalInterface
    public interface ReserveListener {
        void onReserve(Event event, int quantity);
    }
    public interface EmptyStateListener {
        void onEmptyStateChanged(boolean isEmpty);
    }
    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options, ReserveListener reserveListener, EmptyStateListener emptyStateListener) {
        super(options);
        this.isAdminMode = false;
        this.reserveListener = reserveListener;
        this.emptyStateListener = emptyStateListener;
    }
    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options, ReserveListener reserveListener) {
        this(options, reserveListener, null);
    }
    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options) {
        super(options);
        this.isAdminMode = true;
        this.reserveListener = null;
        this.emptyStateListener = null;
    }

    @Override
    protected void onBindViewHolder(@NonNull EventHolder holder, int position, @NonNull Event model) {
        String docId = getSnapshots().getSnapshot(position).getId();
        model.setId(docId);
        holder.bind(model);
    }

    @NonNull
    @Override
    public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_event, parent, false);
        return new EventHolder(v);
    }
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (emptyStateListener != null) {
            emptyStateListener.onEmptyStateChanged(getItemCount() == 0);
        }
    }
    class EventHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvCategory, tvLocation, tvDateTime, tvReservedPlaces, tvMaxPlaces, tvAvailabilityStatus;
        private final TextView tvTicketQuantity;
        private final MaterialButton btnEdit, btnCancel, btnReserve;
        private final MaterialButton btnIncreaseTickets, btnDecreaseTickets;
        private final View layoutQuantitySelector;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

        public EventHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvReservedPlaces = itemView.findViewById(R.id.tvReservedPlaces);
            tvMaxPlaces = itemView.findViewById(R.id.tvMaxPlaces);
            tvAvailabilityStatus = itemView.findViewById(R.id.tvAvailabilityStatus);
            tvTicketQuantity = itemView.findViewById(R.id.tvTicketQuantity);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReserve = itemView.findViewById(R.id.btnReserve);
            btnIncreaseTickets = itemView.findViewById(R.id.btnIncreaseTickets);
            btnDecreaseTickets = itemView.findViewById(R.id.btnDecreaseTickets);
            layoutQuantitySelector = itemView.findViewById(R.id.layoutQuantitySelector);
        }

        public void bind(Event model) {
            int reservedPlaces = model.getReservedPlaces();
            int maxPlaces = model.getMaxPlaces();
            int availableTickets = maxPlaces - reservedPlaces;
            boolean isFull = availableTickets == 0;

            tvName.setText(model.getName());
            tvLocation.setText(model.getLocation());
            tvReservedPlaces.setText("Reserved: " + reservedPlaces);
            tvMaxPlaces.setText("Limit: " + maxPlaces);
            tvAvailabilityStatus.setText(isFull ? FULL_EVENT_MESSAGE : AVAILABLE_EVENT_MESSAGE);
            tvAvailabilityStatus.setTextColor(Color.parseColor(isFull ? "#C62828" : "#2E7D32"));
            updateCategoryUI(model.getCategory());

            tvDateTime.setText(dateFormat.format(model.getDate().toDate()));

            if (isAdminMode) {
                btnEdit.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                btnReserve.setVisibility(View.GONE);
                layoutQuantitySelector.setVisibility(View.GONE);

                btnEdit.setOnClickListener(v -> {
                    if (v.getContext() instanceof AdminActivity) {
                        ((AdminActivity) v.getContext()).editEvent(model);
                    }
                });

                btnCancel.setOnClickListener(v -> {
                    if (v.getContext() instanceof AdminActivity) {
                        ((AdminActivity) v.getContext()).deleteEvent(model);
                    }
                });
                return;
            }

            btnEdit.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            btnReserve.setVisibility(View.VISIBLE);
            layoutQuantitySelector.setVisibility(View.VISIBLE);
            btnReserve.setEnabled(!isFull);
            btnReserve.setAlpha(isFull ? 0.5f : 1f);

            final int[] selectedQuantity = {isFull ? 0 : 1};
            tvTicketQuantity.setText(String.valueOf(selectedQuantity[0]));
            btnDecreaseTickets.setEnabled(selectedQuantity[0] > 1);
            btnIncreaseTickets.setEnabled(!isFull && selectedQuantity[0] < availableTickets);

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
                reserveListener.onReserve(model, selectedQuantity[0]);
            });
        }

        public void updateCategoryUI(String categoryName) {
            String category = categoryName.toLowerCase(Locale.getDefault()).trim();
            int color;

            switch (category) {
                case "concert":
                    color = Color.parseColor("#f29bc2");
                    break;
                case "movies":
                    color = Color.parseColor("#a176d6");
                    break;
                case "travel":
                    color = Color.parseColor("#6bb0ed");
                    break;
                case "sports":
                    color = Color.parseColor("#7cbd6f");
                    break;
                default:
                    color = Color.parseColor("#9C27B0");
                    break;
            }

            tvCategory.getBackground().setTint(color);
            tvCategory.setTextColor(Color.WHITE);
            tvCategory.setText(categoryName);
        }
    }
}