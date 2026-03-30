package com.example.ticketreservationapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference eventsRef = db.collection("events");
    private EventAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        recyclerView = findViewById(R.id.rvEvents);
        ExtendedFloatingActionButton btnAdd = findViewById(R.id.btnAdd);

        initRecyclerView();

        btnAdd.setOnClickListener(v -> addEvent());
    }

    private void initRecyclerView() {
        Query query = eventsRef.orderBy("date", Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<Event> options = new FirestoreRecyclerOptions.Builder<Event>()
                .setQuery(query, Event.class)
                .build();

        adapter = new EventAdapter(options);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void addEvent() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.add_event, null);
        dialog.setContentView(view);

        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etLocation = view.findViewById(R.id.etLocation);
        TextInputEditText etCategory = view.findViewById(R.id.etCategory);
        TextInputEditText etMax = view.findViewById(R.id.etMaxPlaces);
        MaterialButton btnPickDate = view.findViewById(R.id.btnPickDate);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveEvent);

        final Calendar calendar = Calendar.getInstance();

        btnPickDate.setOnClickListener(v -> showDateTimePicker(calendar, btnPickDate));

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String maxStr = etMax.getText().toString().trim();

            if (name.isEmpty() || maxStr.isEmpty() || location.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int maxPlaces = Integer.parseInt(maxStr);
            Timestamp date = new Timestamp(calendar.getTime());
            // Create a new Event object
            Event newEvent = new Event(name, date, location, category, 0, maxPlaces);
            // Push to Firestore
            eventsRef.add(newEvent).addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Event Added!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public void editEvent(Event event) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.add_event, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("Edit Event");

        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etLocation = view.findViewById(R.id.etLocation);
        TextInputEditText etCategory = view.findViewById(R.id.etCategory);
        TextInputEditText etMax = view.findViewById(R.id.etMaxPlaces);
        MaterialButton btnPickDate = view.findViewById(R.id.btnPickDate);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveEvent);
        btnSave.setText("Update Event");

        // Pre-fill existing data
        etName.setText(event.getName());
        etLocation.setText(event.getLocation());
        etCategory.setText(event.getCategory());
        etMax.setText(String.valueOf(event.getMaxPlaces()));

        final Calendar calendar = Calendar.getInstance();
        if (event.getDate() != null) {
            calendar.setTime(event.getDate().toDate());
            updateDateButtonText(btnPickDate, calendar);
        }

        btnPickDate.setOnClickListener(v -> showDateTimePicker(calendar, btnPickDate));

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String maxStr = etMax.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            if (name.isEmpty() || maxStr.isEmpty() || category.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the existing object fields
            event.setName(name);
            event.setLocation(etLocation.getText().toString().trim());
            event.setCategory(etCategory.getText().toString().trim());
            event.setMaxPlaces(Integer.parseInt(maxStr));
            event.setDate(new Timestamp(calendar.getTime()));

            // Update in Firestore using the document ID
            eventsRef.document(event.getId()).set(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event Updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    private void showDateTimePicker(Calendar calendar, MaterialButton btn) {
        new DatePickerDialog(this, (view1, year, month, day) -> {
            calendar.set(year, month, day);
            new TimePickerDialog(this, (view2, hour, min) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, min);
                updateDateButtonText(btn, calendar);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtonText(MaterialButton btn, Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        btn.setText(sdf.format(cal.getTime()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}