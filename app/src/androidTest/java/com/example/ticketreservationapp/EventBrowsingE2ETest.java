package com.example.ticketreservationapp;

import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.Timestamp;

import org.hamcrest.Matcher;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * E2E test for event browsing flow.
 * Verifies the main events list opens with all events and expected item details.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventBrowsingE2ETest {

    private static final String FULL_EVENT_MESSAGE = "Event is full, no tickets are available";
    private static final String AVAILABLE_EVENT_MESSAGE = "Tickets available";
    private static final String EVENT_CANCELLED_MESSAGE = "Event Cancelled";
    private static final long OBSERVATION_DELAY_MS = 1000L;

    @Rule
    public ActivityScenarioRule<MockMainActivity> activityRule =
            new ActivityScenarioRule<>(MockMainActivity.class);

    @Test
    public void test01_showsAllEventsInEventsTab() {
        List<Event> seededEvents = MockMainActivity.getSeededEvents();

        onView(withId(R.id.rvEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).check(new RecyclerCountAssert(seededEvents.size()));
        waitStep();

        for (int position = 0; position < seededEvents.size(); position++) {
            Event event = seededEvents.get(position);

            onView(withId(R.id.rvEvents)).perform(RecyclerViewActions.scrollToPosition(position));
            waitStep();
            onView(withId(R.id.rvEvents)).check(new EventRowAssert(position, event));
            waitStep();
        }
    }

    @Test
    public void test04_booksOneTicket_updatesEventRow() {
        List<Event> seededEvents = MockMainActivity.getSeededEvents();

        int bookingPosition = 3;
        Event selectedEvent = seededEvents.get(bookingPosition);
        Event expectedBefore = copyEventWithReserved(selectedEvent, selectedEvent.getReservedPlaces());
        Event expectedAfter = copyEventWithReserved(selectedEvent, selectedEvent.getReservedPlaces() + 1);

        onView(withId(R.id.rvEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).perform(RecyclerViewActions.scrollToPosition(bookingPosition));
        waitStep();

        onView(withId(R.id.rvEvents)).check(new EventRowAssert(bookingPosition, expectedBefore));
        waitStep();

        onView(withId(R.id.rvEvents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(bookingPosition, clickChild(R.id.btnReserve))
        );
        waitStep();

        onView(withId(R.id.rvEvents)).check(new EventRowAssert(bookingPosition, expectedAfter));
    }

    @Test
    public void test05_booksTicket_showsReservationInTab() {
        List<Event> seededEvents = MockMainActivity.getSeededEvents();

        int bookingPosition = 3;
        Event bookedEvent = seededEvents.get(bookingPosition);

        onView(withId(R.id.rvEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).perform(RecyclerViewActions.scrollToPosition(bookingPosition));
        waitStep();

        onView(withId(R.id.rvEvents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(bookingPosition, clickChild(R.id.btnReserve))
        );
        waitStep();

        onView(withId(R.id.bottomNav)).perform(selectBottomTab(R.id.nav_reservations));
        waitStep();

        onView(withId(R.id.rvReservations)).check(matches(isDisplayed()));
        onView(withId(R.id.rvReservations)).check(new RecyclerCountAssert(2));
        onView(withId(R.id.rvReservations)).check(new ReservationRowAssert(0, bookedEvent, 1, "Status: Confirmed"));
    }

    @Test
    public void test02_reservations_cancelJazzMovesToPastCancelled() {
        List<Event> seededEvents = MockMainActivity.getSeededEvents();

        Event seededActiveEvent = seededEvents.get(0);

        onView(withId(R.id.bottomNav)).perform(selectBottomTab(R.id.nav_reservations));
        waitStep();

        onView(withId(R.id.rvReservations)).check(matches(isDisplayed()));
        onView(withId(R.id.rvReservations)).check(new RecyclerCountAssert(1));
        onView(withId(R.id.rvReservations)).check(new ReservationRowAssert(0, seededActiveEvent, 1, "Status: Confirmed"));

        onView(withId(R.id.rvReservations)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, clickChild(R.id.btnCancelReservation))
        );
        waitStep();

        onView(withId(R.id.rvReservations)).check(new RecyclerCountAssert(0));

        onView(withText("Past / Cancelled")).perform(click());
        waitStep();

        onView(withId(R.id.rvReservations)).check(new RecyclerCountAssert(2));
        onView(withId(R.id.rvReservations)).check(new ReservationRowAssert(0, seededActiveEvent, 1, "Status: Cancelled"));
    }

    @Test
    public void test03_jazzRemainsInPastCancelledInNextTest() {
        List<Event> seededEvents = MockMainActivity.getSeededEvents();
        Event jazzEvent = seededEvents.get(0);

        onView(withId(R.id.bottomNav)).perform(selectBottomTab(R.id.nav_reservations));
        waitStep();

        onView(withText("Past / Cancelled")).perform(click());
        waitStep();

        onView(withId(R.id.rvReservations)).check(matches(isDisplayed()));
        onView(withId(R.id.rvReservations)).check(new RecyclerCountAssert(2));
        onView(withId(R.id.rvReservations)).check(new ReservationRowAssert(0, jazzEvent, 1, "Status: Cancelled"));
    }

    private static void waitStep() {
        SystemClock.sleep(OBSERVATION_DELAY_MS);
    }

    private static ViewAction clickChild(@IdRes int childViewId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + childViewId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(childViewId);
                assertNotNull("Missing child view id: " + childViewId, child);
                child.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private static ViewAction selectBottomTab(@IdRes int itemId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Select bottom navigation item " + itemId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                BottomNavigationView bottomNavigationView = (BottomNavigationView) view;
                bottomNavigationView.setSelectedItemId(itemId);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private static Event copyEventWithReserved(Event source, int reservedPlaces) {
        Event clone = new Event(
                source.getName(),
                source.getDate(),
                source.getLocation(),
                source.getCategory(),
                reservedPlaces,
                source.getMaxPlaces(),
                source.getStatus()
        );
        clone.setId(source.getId());
        return clone;
    }

    private static String formatDate(Timestamp date) {
        return new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date.toDate());
    }

    private static String availabilityText(Event event) {
        if ("cancelled".equalsIgnoreCase(event.getStatus())) {
            return EVENT_CANCELLED_MESSAGE;
        }
        return event.getReservedPlaces() >= event.getMaxPlaces() ? FULL_EVENT_MESSAGE : AVAILABLE_EVENT_MESSAGE;
    }

    private static class EventRowAssert implements ViewAssertion {
        private final int position;
        private final Event expected;

        EventRowAssert(int position, Event expected) {
            this.position = position;
            this.expected = expected;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            assertNotNull("Missing row at position " + position, holder);

            View itemView = holder.itemView;
            assertText(itemView, R.id.tvEventName, expected.getName());
            assertText(itemView, R.id.tvCategory, expected.getCategory());
            assertText(itemView, R.id.tvLocation, expected.getLocation());
            assertText(itemView, R.id.tvDateTime, formatDate(expected.getDate()));
            assertText(itemView, R.id.tvReservedPlaces, "Reserved: " + expected.getReservedPlaces());
            assertText(itemView, R.id.tvMaxPlaces, "Limit: " + expected.getMaxPlaces());
            assertText(itemView, R.id.tvAvailabilityStatus, availabilityText(expected));

            TextView statusView = itemView.findViewById(R.id.tvStatus);
            assertNotNull("Missing status view", statusView);

            if ("cancelled".equalsIgnoreCase(expected.getStatus())) {
                assertEquals("Cancelled row should show status", View.VISIBLE, statusView.getVisibility());
                assertEquals("CANCELLED", statusView.getText().toString());
            } else {
                assertEquals("Active row should hide status", View.GONE, statusView.getVisibility());
            }
        }
    }

    private static class ReservationRowAssert implements ViewAssertion {
        private final int position;
        private final Event expectedEvent;
        private final int expectedTicketCount;
        private final String expectedStatus;

        ReservationRowAssert(int position, Event expectedEvent, int expectedTicketCount, String expectedStatus) {
            this.position = position;
            this.expectedEvent = expectedEvent;
            this.expectedTicketCount = expectedTicketCount;
            this.expectedStatus = expectedStatus;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            assertNotNull("Missing reservation row at position " + position, holder);

            View itemView = holder.itemView;
            assertText(itemView, R.id.tvReservationEventName, expectedEvent.getName());
            assertText(itemView, R.id.tvReservationCategory, expectedEvent.getCategory());
            assertText(itemView, R.id.tvReservationLocation, expectedEvent.getLocation());
            assertText(itemView, R.id.tvReservationEventDate, formatDate(expectedEvent.getDate()));
            assertText(itemView, R.id.tvReservationTicketCount, "Tickets: " + expectedTicketCount);
            assertText(itemView, R.id.tvReservationStatus, expectedStatus);
        }
    }

    private static void assertText(View itemView, @IdRes int viewId, String expectedText) {
        TextView textView = itemView.findViewById(viewId);
        assertNotNull("Missing view id: " + viewId, textView);
        assertEquals(expectedText, textView.getText().toString());
    }

    private static class RecyclerCountAssert implements ViewAssertion {
        private final int expectedCount;

        RecyclerCountAssert(int expectedCount) {
            this.expectedCount = expectedCount;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            assertNotNull("RecyclerView adapter should be set", adapter);
            assertEquals("Unexpected RecyclerView item count", expectedCount, adapter.getItemCount());
        }
    }
}
