package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AlertDialog;
import android.widget.ListView;

import org.robolectric.Shadows;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityTest {

    private void initFirebase() {
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

    public static class TestMainActivity extends MainActivity {
        static FirebaseAuth AUTH;
        static FirebaseFirestore DB;
        static CollectionReference EVENTS_REF;
        static CollectionReference RESERVATIONS_REF;

        @Override protected FirebaseAuth provideAuth() { return AUTH; }
        @Override protected FirebaseFirestore provideDb() { return DB; }
        @Override protected CollectionReference provideEventsRef(FirebaseFirestore ignored) { return EVENTS_REF; }
        @Override protected CollectionReference provideReservationsRef(FirebaseFirestore ignored) { return RESERVATIONS_REF; }
    }
    @Before
    public void setUp() {
        initFirebase();

        TestMainActivity.AUTH = mock(FirebaseAuth.class);
        TestMainActivity.DB = mock(FirebaseFirestore.class);
        TestMainActivity.EVENTS_REF = mock(CollectionReference.class);
        TestMainActivity.RESERVATIONS_REF = mock(CollectionReference.class);
    }

    @After
    public void tearDown() {
        resetTestDeps();
    }

    @Test
    public void privateUiMethods_toggleTabsAndEmptyState() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();
        }

        assertNotNull(activity.findViewById(R.id.rvEvents));
        assertNotNull(activity.findViewById(R.id.rvReservations));
        assertNotNull(activity.findViewById(R.id.tabsReservations));

        invokePrivate(activity, "showEventsTab");

        RecyclerView rvEvents = getPrivateField(activity, "rvEvents", RecyclerView.class);
        RecyclerView rvReservations = getPrivateField(activity, "rvReservations", RecyclerView.class);
        TabLayout tabs = getPrivateField(activity, "reservationsTabs", TabLayout.class);
        MaterialToolbar toolbar = getPrivateField(activity, "toolbar", MaterialToolbar.class);

        assertEquals(View.VISIBLE, rvEvents.getVisibility());
        assertEquals(View.GONE, tabs.getVisibility());
        assertEquals(View.GONE, rvReservations.getVisibility());
        assertEquals("Book Events", toolbar.getTitle().toString());

        invokePrivate(activity, "showReservationsTab");
        assertEquals(View.GONE, rvEvents.getVisibility());
        assertEquals(View.VISIBLE, tabs.getVisibility());
        assertEquals(View.VISIBLE, rvReservations.getVisibility());
        assertEquals("My Reservations", toolbar.getTitle().toString());

        invokePrivate(activity, "updateReservationsEmptyState", new Class<?>[]{boolean.class}, true);
    }

    @Test
    public void reserveTicket_withoutUser_showsLoginToast() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();
        }

        FirebaseAuth auth = mock(FirebaseAuth.class);
        when(auth.getCurrentUser()).thenReturn(null);
        setPrivateField(activity, "auth", auth);

        Event event = new Event();
        event.setId("event-1");

        ShadowToast.reset();
        invokePrivate(activity, "reserveTicket", new Class<?>[]{Event.class, int.class}, event, 1);

        assertEquals("Please log in to book tickets.", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void onStartAndOnStop_withAdapters_callsListenerLifecycle() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            FirebaseAuth auth = mock(FirebaseAuth.class);
            when(auth.getCurrentUser()).thenReturn(null);
            EventAdapter eventAdapter = mock(EventAdapter.class);
            ReservationAdapter reservationAdapter = mock(ReservationAdapter.class);

            setPrivateField(activity, "auth", auth);
            setPrivateField(activity, "eventAdapter", eventAdapter);
            setPrivateField(activity, "reservationAdapter", reservationAdapter);

            controller.start();
            verify(eventAdapter).startListening();
            verify(reservationAdapter).startListening();
            verify(auth).addAuthStateListener(activity.authStateListener);

            controller.stop();
            verify(eventAdapter).stopListening();
            verify(reservationAdapter).stopListening();
            verify(auth).removeAuthStateListener(activity.authStateListener);
        }
    }

    @Test
    public void checkUserStatus_whenUserNull_redirectsToLogin() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();
        }

        invokePrivate(activity, "checkUserStatus", new Class<?>[]{FirebaseUser.class}, new Object[]{null});

        assert(activity.isFinishing());
    }

    private void invokePrivate(MainActivity activity, String methodName, Class<?>[] argTypes, Object... args) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(methodName, argTypes);
        method.setAccessible(true);
        method.invoke(activity, args);
    }

    private void invokePrivate(MainActivity activity, String methodName) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(activity);
    }

    private void setPrivateField(MainActivity activity, String fieldName, Object value) throws Exception {
        Field field = MainActivity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(activity, value);
    }

    private <T> T getPrivateField(MainActivity activity, String fieldName, Class<T> type) throws Exception {
        Field field = MainActivity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(activity));
    }
    private TextInputEditText findFirstTextInputEditText(View root) {
        if (root instanceof TextInputEditText) return (TextInputEditText) root;

        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextInputEditText found = findFirstTextInputEditText(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    public void cancelReservation_whenRunTransactionFails_showsFailedToast() throws Exception {
        DocumentReference reservationDoc = mock(DocumentReference.class);
        when(TestMainActivity.RESERVATIONS_REF.document("res-1")).thenReturn(reservationDoc);

        when(TestMainActivity.DB.runTransaction(any(Transaction.Function.class))).thenReturn(Tasks.forException(new RuntimeException("boom")));

        try (ActivityController<TestMainActivity> controller = Robolectric.buildActivity(TestMainActivity.class).create()) {
            TestMainActivity activity = controller.get();

            Reservation r = new Reservation();
            r.setId("res-1");

            ShadowToast.reset();
            invokePrivate(activity, "cancelReservation", new Class<?>[]{Reservation.class}, r);
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertEquals(activity.getString(R.string.cancel_reservation_failed), ShadowToast.getTextOfLatestToast());
        }
    }

    @Test
    public void cancelReservation_whenRunTransactionSucceeds_showsSuccessToast() throws Exception {
        DocumentReference reservationDoc = mock(DocumentReference.class);
        when(TestMainActivity.RESERVATIONS_REF.document("res-1")).thenReturn(reservationDoc);

        when(TestMainActivity.DB.runTransaction(any(Transaction.Function.class))).thenReturn(Tasks.forResult(2));

        try (ActivityController<TestMainActivity> controller = Robolectric.buildActivity(TestMainActivity.class).create()) {
            TestMainActivity activity = controller.get();

            Reservation r = new Reservation();
            r.setId("res-1");

            ShadowToast.reset();
            invokePrivate(activity, "cancelReservation", new Class<?>[]{Reservation.class}, r);
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            String expected = activity.getString(R.string.reservation_cancelled_success, 2);
            assertEquals(expected, ShadowToast.getTextOfLatestToast());
        }
    }

    @Test
    public void reserveTicket_whenTransactionSucceeds_showsConfirmedToast() throws Exception {
        when(TestMainActivity.AUTH.getCurrentUser()).thenReturn(null);

        DocumentReference newReservationDoc = mock(DocumentReference.class);
        when(TestMainActivity.RESERVATIONS_REF.document()).thenReturn(newReservationDoc);

        when(TestMainActivity.DB.runTransaction(any(Transaction.Function.class))).thenReturn(Tasks.forResult(null));

        try (ActivityController<TestMainActivity> controller = Robolectric.buildActivity(TestMainActivity.class).create()) {
            TestMainActivity activity = controller.get();

            FirebaseUser user = mock(FirebaseUser.class);
            when(user.getUid()).thenReturn("user-1");
            when(TestMainActivity.AUTH.getCurrentUser()).thenReturn(user);

            Event event = new Event();
            event.setId("event-1");

            ShadowToast.reset();
            invokePrivate(activity, "reserveTicket", new Class<?>[]{Event.class, int.class}, event, 2);
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertEquals("2 ticket(s) booked! Reservation confirmed.", ShadowToast.getTextOfLatestToast());
        }
    }

    @Test
    public void reserveTicket_whenNotEnoughTickets_showsNotEnoughToast() throws Exception {
        when(TestMainActivity.AUTH.getCurrentUser()).thenReturn(null);

        DocumentReference newReservationDoc = mock(DocumentReference.class);
        when(TestMainActivity.RESERVATIONS_REF.document()).thenReturn(newReservationDoc);

        when(TestMainActivity.DB.runTransaction(any(Transaction.Function.class))).thenReturn(Tasks.forException(new IllegalStateException("Not enough tickets available for selected quantity")));

        try (ActivityController<TestMainActivity> controller = Robolectric.buildActivity(TestMainActivity.class).create()) {
            TestMainActivity activity = controller.get();

            FirebaseUser user = mock(FirebaseUser.class);
            when(user.getUid()).thenReturn("user-1");
            when(TestMainActivity.AUTH.getCurrentUser()).thenReturn(user);

            Event event = new Event();
            event.setId("event-1");

            ShadowToast.reset();
            invokePrivate(activity, "reserveTicket", new Class<?>[]{Event.class, int.class}, event, 2);
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertEquals("Not enough tickets available for selected quantity", ShadowToast.getTextOfLatestToast());
        }
    }
    private void resetTestDeps() {
        TestMainActivity.AUTH = null;
        TestMainActivity.DB = null;
        TestMainActivity.EVENTS_REF = null;
        TestMainActivity.RESERVATIONS_REF = null;
    }
    @Test
    public void showFilterDialog_clickEachOption_executesBranches() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            invokePrivate(activity, "showFilterDialog");
            AlertDialog dialog0 = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog0);
            clickDialogListItem(dialog0, 0);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(ShadowDialog.getLatestDialog());

            invokePrivate(activity, "showFilterDialog");
            AlertDialog dialog1 = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog1);
            clickDialogListItem(dialog1, 1);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(ShadowDialog.getLatestDialog());

            invokePrivate(activity, "showFilterDialog");
            AlertDialog dialog2 = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog2);
            clickDialogListItem(dialog2, 2);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(ShadowDialog.getLatestDialog());
        }
    }
    private void clickDialogListItem(AlertDialog dialog, int index) {
        ListView lv = dialog.getListView();
        assertNotNull(lv);

        lv.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        lv.layout(0, 0, 1000, 1000);

        View row = lv.getAdapter().getView(index, null, lv);
        lv.performItemClick(row, index, lv.getAdapter().getItemId(index));
    }
    @Test
    public void showLocationInputDialog_clickSearch_runsPositiveHandler() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            invokePrivate(activity, "showLocationInputDialog");

            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);

            TextInputEditText input = findFirstTextInputEditText(dialog.getWindow().getDecorView());
            assertNotNull(input);
            input.setText(" New York ");

            assertNotNull(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }
    @Test
    public void showCategoryInputDialog_clickApply_runsPositiveHandler() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            invokePrivate(activity, "showCategoryInputDialog");

            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);

            TextInputEditText input = findFirstTextInputEditText(dialog.getWindow().getDecorView());
            assertNotNull(input);
            input.setText(" movies ");

            assertNotNull(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }
    @Test
    public void showDatePickerDialog_triggersOnDateSetListener_viaReflection() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            invokePrivate(activity, "showDatePickerDialog");

            android.app.Dialog latest = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertNotNull(latest);

            android.app.DatePickerDialog dp = (android.app.DatePickerDialog) latest;

            java.lang.reflect.Field f = android.app.DatePickerDialog.class.getDeclaredField("mDateSetListener");
            f.setAccessible(true);
            android.app.DatePickerDialog.OnDateSetListener listener = (android.app.DatePickerDialog.OnDateSetListener) f.get(dp);

            assertNotNull(listener);

            listener.onDateSet(dp.getDatePicker(), 2026, 0, 15);
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        }
    }
    @Test
    public void applyFilter_none_and_category_executes() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            invokePrivate(activity, "applyFilter", new Class<?>[]{EventFilter.class}, EventFilter.none());
            invokePrivate(activity, "applyFilter", new Class<?>[]{EventFilter.class}, EventFilter.category("movies"));

            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }
    @Test
    public void confirmCancelReservation_clickPositive_executesCancelReservation() throws Exception {
        when(TestMainActivity.AUTH.getCurrentUser()).thenReturn(null);

        when(TestMainActivity.DB.runTransaction(any(Transaction.Function.class)))
                .thenReturn(Tasks.forException(new RuntimeException("boom")));

        DocumentReference reservationDoc = mock(DocumentReference.class);
        when(TestMainActivity.RESERVATIONS_REF.document("res-1")).thenReturn(reservationDoc);

        try (ActivityController<TestMainActivity> controller = Robolectric.buildActivity(TestMainActivity.class).create()) {

            TestMainActivity activity = controller.get();

            Reservation r = new Reservation();
            r.setId("res-1");

            invokePrivate(activity, "confirmCancelReservation", new Class<?>[]{Reservation.class}, r);

            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);

            ShadowToast.reset();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertEquals(activity.getString(R.string.cancel_reservation_failed), ShadowToast.getTextOfLatestToast());
        }
    }
    @Test
    public void showEventsTab_whenFilterAppliedAndEmpty_showsNoMatchingResultsText() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            setPrivateField(activity, "filterApplied", true);

            setPrivateField(activity, "eventAdapter", null);

            invokePrivate(activity, "showEventsTab");

            RecyclerView rvEvents = getPrivateField(activity, "rvEvents", RecyclerView.class);
            assertEquals(View.VISIBLE, rvEvents.getVisibility());

            View tvNoMatching = activity.findViewById(R.id.tvNoMatchingResultsFromFilter);
            assertNotNull(tvNoMatching);

            assertEquals(View.VISIBLE, tvNoMatching.getVisibility());
        }
    }
    @Test
    public void showReservationsTab_whenNoAdapter_showsEmptyReservationsText() throws Exception {
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();

            setPrivateField(activity, "reservationAdapter", null);

            FirebaseAuth auth = mock(FirebaseAuth.class);
            when(auth.getCurrentUser()).thenReturn(null);
            setPrivateField(activity, "auth", auth);

            invokePrivate(activity, "showReservationsTab");

            RecyclerView rvEvents = getPrivateField(activity, "rvEvents", RecyclerView.class);
            RecyclerView rvReservations = getPrivateField(activity, "rvReservations", RecyclerView.class);
            TabLayout tabs = getPrivateField(activity, "reservationsTabs", TabLayout.class);

            assertEquals(View.GONE, rvEvents.getVisibility());
            assertEquals(View.VISIBLE, tabs.getVisibility());
            assertEquals(View.VISIBLE, rvReservations.getVisibility());

            View tvEmpty = activity.findViewById(R.id.tvEmptyReservations);
            assertNotNull(tvEmpty);

            assertEquals(View.VISIBLE, tvEmpty.getVisibility());
        }
    }
}
