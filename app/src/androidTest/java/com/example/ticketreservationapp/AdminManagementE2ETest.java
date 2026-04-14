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
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AdminManagementE2ETest {

    private static final long STEP_DELAY_MS = 700L;

    @Rule
    public ActivityScenarioRule<MockAdminActivity> activityRule =
            new ActivityScenarioRule<>(MockAdminActivity.class);

    @Test
    public void adminCanAddCancelAndDeleteEvent() {
        String eventName = "E2E Admin Event";

        onView(withText("Manage Events")).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).check(new RecyclerCountAssert(0));

        onView(withId(R.id.btnAdd)).perform(click());
        waitStep();

        onView(withId(R.id.etName)).perform(replaceText(eventName), closeSoftKeyboard());
        onView(withId(R.id.etLocation)).perform(replaceText("Montreal"), closeSoftKeyboard());
        onView(withId(R.id.etCategory)).perform(replaceText("sports"), closeSoftKeyboard());
        onView(withId(R.id.etMaxPlaces)).perform(replaceText("25"), closeSoftKeyboard());
        onView(withId(R.id.btnSaveEvent)).perform(click());
        waitStep();

        onView(withId(R.id.rvEvents)).check(new RecyclerCountAssert(1));

        onView(withId(R.id.rvEvents)).perform(
                RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(eventName)),
                        clickChild(R.id.btnCancel)
                )
        );
        waitStep();
        onView(withText("Yes, Cancel")).perform(click());
        waitStep();

        onView(withId(R.id.rvEvents)).check(new EventCancelledAssert(eventName));

        onView(withId(R.id.rvEvents)).perform(
                RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(eventName)),
                        clickChild(R.id.btnEdit)
                )
        );
        waitStep();
        onView(withText("Yes, Delete")).perform(click());
        waitStep();

        onView(withId(R.id.rvEvents)).check(new RecyclerCountAssert(0));
        onView(withText(eventName)).check(doesNotExist());
    }

    private static void waitStep() {
        SystemClock.sleep(STEP_DELAY_MS);
    }

    private static ViewAction clickChild(@IdRes int childId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click child view id " + childId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(childId);
                assertNotNull("Missing child view: " + childId, child);
                child.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
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
            assertEquals("Unexpected event count", expectedCount, adapter.getItemCount());
        }
    }

    private static class EventCancelledAssert implements ViewAssertion {
        private final String eventName;

        EventCancelledAssert(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ViewHolder holder = findHolderByEventName(recyclerView, eventName);
            assertNotNull("Event row not found: " + eventName, holder);

            TextView status = holder.itemView.findViewById(R.id.tvStatus);
            assertNotNull("Status view is missing", status);
            assertEquals(View.VISIBLE, status.getVisibility());
            assertEquals("CANCELLED", status.getText().toString());
        }

        private RecyclerView.ViewHolder findHolderByEventName(RecyclerView recyclerView, String name) {
            for (int position = 0; position < recyclerView.getChildCount(); position++) {
                View child = recyclerView.getChildAt(position);
                TextView eventNameText = child.findViewById(R.id.tvEventName);
                if (eventNameText != null && name.equals(eventNameText.getText().toString())) {
                    return recyclerView.getChildViewHolder(child);
                }
            }
            return null;
        }
    }
}
