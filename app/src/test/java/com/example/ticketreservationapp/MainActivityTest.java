package com.example.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
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

    @Test
    public void privateUiMethods_toggleTabsAndEmptyState() throws Exception {
        initFirebase();
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
        initFirebase();
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
        initFirebase();
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
        initFirebase();
        MainActivity activity;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create()) {
            activity = controller.get();
        }

        invokePrivate(activity, "checkUserStatus", new Class<?>[]{FirebaseUser.class}, new Object[]{null});

        assertEquals(true, activity.isFinishing());
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
}
