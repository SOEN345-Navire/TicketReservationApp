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

    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options) {
        super(options);
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

    class EventHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvCategory, tvLocation, tvDateTime, tvReservedPlaces, tvMaxPlaces;
        private final MaterialButton btnEdit, btnCancel;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        public EventHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvReservedPlaces = itemView.findViewById(R.id.tvReservedPlaces);
            tvMaxPlaces = itemView.findViewById(R.id.tvMaxPlaces);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(Event model) {
            tvName.setText(model.getName());
            tvLocation.setText(model.getLocation());
            tvReservedPlaces.setText("Reserved: " + model.getReservedPlaces());
            tvMaxPlaces.setText("Limit: " + model.getMaxPlaces());
            updateCategoryUI(model.getCategory());

            if (model.getDate() != null) {
                tvDateTime.setText(dateFormat.format(model.getDate().toDate()));
            }

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
        }

        public void updateCategoryUI(String categoryName) {
            String category = categoryName.toLowerCase().trim();
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