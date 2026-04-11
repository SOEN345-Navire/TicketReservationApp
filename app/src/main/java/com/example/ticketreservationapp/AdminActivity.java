package com.example.ticketreservationapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference eventsRef = db.collection("events");
    private EventAdapter adapter;
    private RecyclerView recyclerView;

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
        setContentView(R.layout.activity_admin);

        recyclerView = findViewById(R.id.rvEvents);
        ExtendedFloatingActionButton btnAdd = findViewById(R.id.btnAdd);

        initRecyclerView();

        initAuth();

        btnAdd.setOnClickListener(v -> addEvent());
    }

    private void checkUserStatus(FirebaseUser user) {
        if (user == null) {
            startActivity(new Intent(AdminActivity.this, LogInActivity.class));
            finish();
            return;
        }
        //Check if user is verified
        if (!user.isEmailVerified() && user.getEmail() != null && Objects.requireNonNull(user.getEmail()).matches(Authentification.emailRegex)) {
            startActivity(new Intent(AdminActivity.this, ConfirmEmailActivity.class));
            finish();
        }

        Authentification.isAdmin(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    boolean isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                    if (!isAdmin) {
                        startActivity(new Intent(AdminActivity.this, MainActivity.class));
                        finish();
                    }
                }
            }
        });
    }

    private void initAuth() {
        auth = FirebaseAuth.getInstance();
        Button logout = findViewById(R.id.logout);
        logout.setOnClickListener(v -> auth.signOut());
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
        showEventDialog(null);
    }

    public void deleteEvent(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    eventsRef.document(event.getId()).delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Event Removed!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void editEvent(Event event) {
        showEventDialog(event);
    }

    // method to handle both adding and creating events
    // if event is null, form is blank (Add mode). If not null, form is pre-filled (Edit mode).
    private void showEventDialog(Event event) {
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

        final Calendar calendar = Calendar.getInstance();
        boolean isEdit = (event != null);

        if (isEdit) { // Pre-fill fields if we're editing
            tvTitle.setText("Edit Event");
            btnSave.setText("Update Event");
            etName.setText(event.getName());
            etLocation.setText(event.getLocation());
            etCategory.setText(event.getCategory());
            etMax.setText(String.valueOf(event.getMaxPlaces()));
            if (event.getDate() != null) {
                calendar.setTime(event.getDate().toDate());
                updateDateButtonText(btnPickDate, calendar);
            }
        }

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

            // normalize for filtering
            String locationSmallCaps = location.toLowerCase(Locale.getDefault());
            String categoryBigCaps = category.toUpperCase(Locale.getDefault());

            Map<String, Object> data = new java.util.HashMap<>();
            data.put("name", name);
            data.put("location", location);
            data.put("locationLower", locationSmallCaps);
            data.put("category", categoryBigCaps);
            data.put("maxPlaces", maxPlaces);
            data.put("date", date);


            if (isEdit) { // Update existing document
                data.put("reservedPlaces", event.getReservedPlaces());
                eventsRef.document(event.getId()).set(data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event Updated!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            } else { // Create brand new document
                data.put("reservedPlaces", 0);
                eventsRef.add(data).addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Event Added!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
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
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
        auth.removeAuthStateListener(authStateListener);
    }
    protected void setEventsRef(CollectionReference ref) {
        this.eventsRef = ref;
    }
}
