package com.example.ticketreservationapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReservationAdapter extends FirestoreRecyclerAdapter<Reservation, ReservationAdapter.ReservationHolder> {

    public interface EmptyStateListener {
        void onEmptyStateChanged(boolean isEmpty);
    }

    public interface ReservationActionListener {
        void onCancel(Reservation reservation);
    }

    private final EmptyStateListener emptyStateListener;
    private final ReservationActionListener actionListener;

    public ReservationAdapter(@NonNull FirestoreRecyclerOptions<Reservation> options,
                              EmptyStateListener emptyStateListener,
                              ReservationActionListener actionListener) {
        super(options);
        this.emptyStateListener = emptyStateListener;
        this.actionListener = actionListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ReservationHolder holder, int position, @NonNull Reservation model) {
        String docId = getSnapshots().getSnapshot(position).getId();
        model.setId(docId);
        holder.bind(model);
    }

    @NonNull
    @Override
    public ReservationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ReservationHolder(v);
    }

    @Override
    public void onDataChanged() {
        if (emptyStateListener != null) {
            emptyStateListener.onEmptyStateChanged(getItemCount() == 0);
        }
    }

    class ReservationHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventName;
        private final TextView tvCategory;
        private final TextView tvLocation;
        private final TextView tvEventDate;
        private final TextView tvTicketCount;
        private final TextView tvStatus;
        private final MaterialButton btnCancelReservation;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

        ReservationHolder(View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvReservationEventName);
            tvCategory = itemView.findViewById(R.id.tvReservationCategory);
            tvLocation = itemView.findViewById(R.id.tvReservationLocation);
            tvEventDate = itemView.findViewById(R.id.tvReservationEventDate);
            tvTicketCount = itemView.findViewById(R.id.tvReservationTicketCount);
            tvStatus = itemView.findViewById(R.id.tvReservationStatus);
            btnCancelReservation = itemView.findViewById(R.id.btnCancelReservation);
        }

        void bind(Reservation model) {
            tvEventName.setText(model.getEventName());
            tvCategory.setText(model.getEventCategory());
            tvLocation.setText(model.getEventLocation());
            tvEventDate.setText(dateFormat.format(model.getEventDate().toDate()));
            tvTicketCount.setText(itemView.getContext().getString(R.string.tickets_count, model.getTicketCount()));

            String status = model.getStatus();
            boolean isCancelled = "cancelled".equalsIgnoreCase(status);

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

            btnCancelReservation.setOnClickListener(v -> {
                if (!isCancelled) {
                    actionListener.onCancel(model);
                }
            });
        }
    }
}
