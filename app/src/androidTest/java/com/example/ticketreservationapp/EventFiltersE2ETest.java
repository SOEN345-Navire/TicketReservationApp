package com.example.ticketreservationapp;

import android.os.SystemClock;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class EventFiltersE2ETest {

    private static final long OBSERVATION_DELAY_MS = 700L;

    @Rule
    public ActivityScenarioRule<MockMainActivity> activityRule =
            new ActivityScenarioRule<>(MockMainActivity.class);

    @Test
    public void filtersByLocation() {
        assertCount(4);

        filterByLocation("Mont");

        assertCount(2);
        assertEventNameAt(0, "Jazz Night");
        assertEventNameAt(1, "Mountain Sunrise Hike");
    }

    @Test
    public void filtersByCategory() {
        assertCount(4);

        filterByCategory("sports");

        assertCount(1);
        assertEventNameAt(0, "City Marathon");
    }

    @Test
    public void filtersByDate() {
        assertCount(4);

        filterByDate(2026, 10, 22);

        assertCount(1);
        assertEventNameAt(0, "Indie Film Premiere");
    }

    @Test
    public void clearFilter_restoresAllEvents() {
        filterByCategory("sports");
        assertCount(1);

        onView(withId(R.id.btnClearFilters)).perform(click());
        waitStep();

        assertCount(4);
    }

    private void filterByLocation(String prefix) {
        chooseFilter("Search by Location");
        onView(isAssignableFrom(EditText.class)).perform(replaceText(prefix), closeSoftKeyboard());
        onView(withText("Search")).perform(click());
        waitStep();
    }

    private void filterByCategory(String category) {
        chooseFilter("Filter by Category");
        onView(isAssignableFrom(EditText.class)).perform(replaceText(category), closeSoftKeyboard());
        onView(withText("Apply")).perform(click());
        waitStep();
    }

    private void filterByDate(int year, int month, int day) {
        chooseFilter("Filter by Date");
        onView(isAssignableFrom(DatePicker.class)).perform(PickerActions.setDate(year, month, day));
        onView(withId(android.R.id.button1)).perform(click());
        waitStep();
    }

    private void chooseFilter(String optionText) {
        onView(withId(R.id.btnFilter)).perform(click());
        onView(withText(optionText)).perform(click());
        waitStep();
    }

    private void assertCount(int expectedCount) {
        onView(withId(R.id.rvEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).check(new RecyclerCountAssert(expectedCount));
    }

    private void assertEventNameAt(int position, String expectedName) {
        onView(withId(R.id.rvEvents)).perform(RecyclerViewActions.scrollToPosition(position));
        waitStep();
        onView(withId(R.id.rvEvents)).check(new EventNameAssert(position, expectedName));
    }

    private static void waitStep() {
        SystemClock.sleep(OBSERVATION_DELAY_MS);
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

    private static class EventNameAssert implements ViewAssertion {
        private final int position;
        private final String expectedName;

        EventNameAssert(int position, String expectedName) {
            this.position = position;
            this.expectedName = expectedName;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            assertNotNull("Missing row at position " + position, holder);

            TextView eventName = holder.itemView.findViewById(R.id.tvEventName);
            assertNotNull("Missing event name view", eventName);
            assertEquals(expectedName, eventName.getText().toString());
        }
    }
}
