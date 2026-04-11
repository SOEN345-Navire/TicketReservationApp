package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import androidx.appcompat.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Button;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AdminActivityTest {
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
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

    private Event createEvent() {
        Event event = new Event("Concert", new Timestamp(new Date()), "MTL", "CONCERT", 2, 10);
        event.setId("event-id");
        return event;
    }

    @Test
    public void deleteEvent_removesDocumentFromFirestore() {
        Event event = createEvent();
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            CollectionReference mockRef = mock(CollectionReference.class);
            DocumentReference mockDocRef = mock(DocumentReference.class);
            when(mockRef.document(event.getId())).thenReturn(mockDocRef);
            when(mockDocRef.delete()).thenReturn(Tasks.forResult(null));
            activity.setEventsRef(mockRef);

            activity.deleteEvent(event);
            AlertDialog confirmDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
            confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            ShadowLooper.idleMainLooper();

            verify(mockRef).document(event.getId());
            verify(mockDocRef, times(1)).delete();
        }
    }

    @Test
    public void editEvent_updatesExistingFirestoreDocument() {
        Event event = createEvent();
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            CollectionReference mockRef = mock(CollectionReference.class);
            DocumentReference mockDocRef = mock(DocumentReference.class);
            when(mockRef.document(event.getId())).thenReturn(mockDocRef);
            when(mockDocRef.set(anyMap())).thenReturn(Tasks.forResult(null));
            activity.setEventsRef(mockRef);

            activity.editEvent(event);
            Dialog dialog = ShadowDialog.getLatestDialog();
            dialog.<TextInputEditText>findViewById(R.id.etName).setText("Rock Concert");
            dialog.findViewById(R.id.btnSaveEvent).performClick();
            ShadowLooper.idleMainLooper();

            verify(mockDocRef).set(mapCaptor.capture());
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            assertEquals("Rock Concert", capturedEvent.get("name"));
            assertEquals("MTL", capturedEvent.get("location")); // check other fields weren't modified
        }
    }

    @Test
    public void addEvent_addsNewFirestoreDocument() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            CollectionReference mockRef = mock(CollectionReference.class);
            DocumentReference mockDocRef = mock(DocumentReference.class);
            when(mockRef.add(anyMap())).thenReturn(Tasks.forResult(mockDocRef));
            activity.setEventsRef(mockRef);

            activity.findViewById(R.id.btnAdd).performClick();
            Dialog dialog = ShadowDialog.getLatestDialog();
            dialog.<TextInputEditText>findViewById(R.id.etName).setText("Rock Concert");
            dialog.<TextInputEditText>findViewById(R.id.etLocation).setText("MTL");
            dialog.<TextInputEditText>findViewById(R.id.etCategory).setText("CONCERT");
            dialog.<TextInputEditText>findViewById(R.id.etMaxPlaces).setText("100");
            dialog.findViewById(R.id.btnSaveEvent).performClick();
            ShadowLooper.idleMainLooper();

            verify(mockRef).add(mapCaptor.capture());
            Map<String, Object> capturedEvent = mapCaptor.getValue();

            assertEquals("Rock Concert", capturedEvent.get("name"));
            assertEquals("MTL", capturedEvent.get("location"));
            assertEquals("CONCERT", capturedEvent.get("category"));
            assertEquals(100, ((Number) Objects.requireNonNull(capturedEvent.get("maxPlaces"))).intValue());
            assertNotNull(capturedEvent.get("date"));
        }
    }

    @Test
    public void saveEvent_withPartialEmptyFields() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();
            activity.findViewById(R.id.btnAdd).performClick();
            Dialog dialog = ShadowDialog.getLatestDialog();

            TextInputEditText etName = dialog.findViewById(R.id.etName);
            TextInputEditText etLocation = dialog.findViewById(R.id.etLocation);
            TextInputEditText etCategory = dialog.findViewById(R.id.etCategory);
            TextInputEditText etMax = dialog.findViewById(R.id.etMaxPlaces);
            Button btnSave = dialog.findViewById(R.id.btnSaveEvent);

            String[][] eventFields = {
                    {"", "Montreal", "Concert", "100"}, // Name empty
                    {"Jazz", "", "Concert", "100"}, // Location empty
                    {"Jazz", "Montreal", "", "100"}, // Category empty
                    {"Jazz", "Montreal", "Concert", ""} // Max places empty
            };

            for (String[] fields : eventFields) {
                etName.setText(fields[0]);
                etLocation.setText(fields[1]);
                etCategory.setText(fields[2]);
                etMax.setText(fields[3]);

                btnSave.performClick();

                assertEquals("Please fill in all fields", ShadowToast.getTextOfLatestToast());
                ShadowToast.reset(); // Reset toast before next iteration
            }
        }
    }

    @Test
    public void whenLoggedOut_redirectsToLogin() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.getCurrentUser()).thenReturn(null);
            activity.authStateListener.onAuthStateChanged(mockAuth);

            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(LogInActivity.class.getName(), Objects.requireNonNull(intent.getComponent()).getClassName());
        }
    }

    @Test
    public void whenEmailNotVerified_redirectsToConfirmEmail() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            FirebaseUser mockUser = mock(FirebaseUser.class);
            when(mockUser.getUid()).thenReturn("uid-test");
            when(mockUser.isEmailVerified()).thenReturn(false);
            when(mockUser.getEmail()).thenReturn("test@gmail.com");

            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.getCurrentUser()).thenReturn(mockUser);

            activity.authStateListener.onAuthStateChanged(mockAuth);

            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(ConfirmEmailActivity.class.getName(), Objects.requireNonNull(intent.getComponent()).getClassName());
        }
    }

    @Test
    public void whenNotAdmin_redirectsToMainActivity() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            FirebaseUser mockUser = mock(FirebaseUser.class);
            when(mockUser.getUid()).thenReturn("regular-user-id");
            when(mockUser.isEmailVerified()).thenReturn(true);

            DocumentSnapshot mockDocument = mock(DocumentSnapshot.class);
            when(mockDocument.exists()).thenReturn(true);
            when(mockDocument.getBoolean("isAdmin")).thenReturn(false);

            Task<DocumentSnapshot> completedTask = Tasks.forResult(mockDocument);

            try (MockedStatic<Authentification> mockedAuth = mockStatic(Authentification.class)) {
                mockedAuth.when(() -> Authentification.isAdmin("regular-user-id")).thenReturn(completedTask);

                FirebaseAuth mockAuth = mock(FirebaseAuth.class);
                when(mockAuth.getCurrentUser()).thenReturn(mockUser);

                activity.authStateListener.onAuthStateChanged(mockAuth);

                ShadowLooper.idleMainLooper();
            }

            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(), Objects.requireNonNull(intent.getComponent()).getClassName());
        }
    }

    @Test
    public void whenIsAdmin_staysInAdminActivity() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            FirebaseUser mockUser = mock(FirebaseUser.class);
            when(mockUser.getUid()).thenReturn("admin-id");
            when(mockUser.isEmailVerified()).thenReturn(true);

            DocumentSnapshot mockDocument = mock(DocumentSnapshot.class);
            when(mockDocument.exists()).thenReturn(true);
            when(mockDocument.getBoolean("isAdmin")).thenReturn(true);

            Task<DocumentSnapshot> completedTask = Tasks.forResult(mockDocument);

            try (MockedStatic<Authentification> mockedAuth = mockStatic(Authentification.class)) {
                mockedAuth.when(() -> Authentification.isAdmin("admin-id")).thenReturn(completedTask);

                FirebaseAuth mockAuth = mock(FirebaseAuth.class);
                when(mockAuth.getCurrentUser()).thenReturn(mockUser);

                activity.authStateListener.onAuthStateChanged(mockAuth);

                ShadowLooper.idleMainLooper();

                assertFalse(activity.isFinishing());
            }
        }
    }

    @Test
    public void dateTimePicker_updatesBtnText() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();

            activity.findViewById(R.id.btnAdd).performClick();

            Dialog bottomSheet = ShadowDialog.getLatestDialog();
            MaterialButton btnPickDate = bottomSheet.findViewById(R.id.btnPickDate);
            btnPickDate.performClick();

            DatePickerDialog datePicker = (DatePickerDialog) ShadowDialog.getLatestDialog();
            datePicker.updateDate(2026, 3, 10);
            datePicker.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

            ShadowLooper.idleMainLooper();

            TimePickerDialog timePicker = (TimePickerDialog) ShadowDialog.getLatestDialog();
            timePicker.updateTime(14, 30);
            timePicker.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

            String btnText = btnPickDate.getText().toString();
            assertEquals("10/04/2026 14:30", btnText);
        }
    }

    @Test
    public void doesNotCrashOnStartStop() {
        try (ActivityController<AdminActivity> controller = Robolectric.buildActivity(AdminActivity.class).create()) {
            AdminActivity activity = controller.get();
            controller.start();
            controller.stop();
            assertFalse(activity.isDestroyed());
        }
    }
}