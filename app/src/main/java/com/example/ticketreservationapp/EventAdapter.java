package com.example.ticketreservationapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EventAdapter extends FirestoreRecyclerAdapter<Event, EventAdapter.EventHolder> {

    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull EventHolder holder, int position, @NonNull Event model) {
        String docId = getSnapshots().getSnapshot(position).getId();
        model.setId(docId);
        holder.tvName.setText(model.getName());
        holder.tvCategory.setText(model.getCategory());
        holder.tvLocation.setText(model.getLocation());
        holder.tvReservedPlaces.setText("Reserved: " + model.getReservedPlaces());
        holder.tvMaxPlaces.setText("Limit: " + model.getMaxPlaces());

        if (model.getDate() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault());
            String dateString = sdf.format(model.getDate().toDate());
            holder.tvDateTime.setText(dateString);
        }

        String category = model.getCategory().toLowerCase().trim();
        int color;
        int textColor = android.graphics.Color.WHITE;

        switch (category) {
            case "concert":
                color = android.graphics.Color.parseColor("#f29bc2");
                break;
            case "movies":
                color = android.graphics.Color.parseColor("#a176d6");
                break;
            case "travel":
                color = android.graphics.Color.parseColor("#6bb0ed");
                break;
            case "sports":
                color = android.graphics.Color.parseColor("#7cbd6f");
                break;
            default:
                color = android.graphics.Color.parseColor("#9C27B0");
                break;
        }

        holder.tvCategory.getBackground().setTint(color);
        holder.tvCategory.setTextColor(textColor);
        holder.tvCategory.setText(model.getCategory());

        holder.btnEdit.setOnClickListener(v -> {
            if (v.getContext() instanceof AdminActivity) {
                ((AdminActivity) v.getContext()).editEvent(model);
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("Cancel Event")
                    .setMessage("Are you sure you want to delete this event?")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> {

                        String id = model.getId();
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(id)
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Event Removed!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @NonNull
    @Override
    public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_event, parent, false);
        return new EventHolder(v);
    }

    class EventHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvLocation, tvDateTime, tvReservedPlaces, tvMaxPlaces;
        com.google.android.material.button.MaterialButton btnEdit, btnCancel;

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
    }
}