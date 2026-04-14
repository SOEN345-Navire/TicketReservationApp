package com.example.ticketreservationapp;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.espresso.intent.Intents;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

/**
 * E2E tests for registration flow.
 */
@RunWith(AndroidJUnit4.class)
public class RegistrationE2ETest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    @Before
    public void setUp() {
        Intents.init();

        intending(allOf(
                hasComponent(ConfirmEmailActivity.class.getName())
        )).respondWith(new android.app.Instrumentation.ActivityResult(
                android.app.Activity.RESULT_OK,
                null
        ));

        intending(allOf(
                hasComponent(LogInActivity.class.getName())
        )).respondWith(new android.app.Instrumentation.ActivityResult(
                android.app.Activity.RESULT_OK,
                null
        ));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private static String generateRandomEmail() {
        return "testuser" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
        public void completeRegistration_opensConfirmEmail() {
        String email = generateRandomEmail();
        String password = "ValidPass123!";

        onView(withId(R.id.email_edittext))
                .perform(typeText(email), closeSoftKeyboard())
                .check(matches(withText(email)));

        onView(withId(R.id.password_edittext))
                .perform(typeText(password), closeSoftKeyboard());

        onView(withId(R.id.confirmPassword_edittext))
                .perform(typeText(password), closeSoftKeyboard());

                onView(withId(R.id.registerMail_button)).perform(click());

        waitForIntent(ConfirmEmailActivity.class.getName());

    }

    @Test
        public void passwordMismatch_staysOnRegistrationPage() {
        String email = generateRandomEmail();

        onView(withId(R.id.email_edittext))
                .perform(typeText(email), closeSoftKeyboard())
                .check(matches(withText(email)));

        onView(withId(R.id.password_edittext))
                .perform(typeText("Password123!"), closeSoftKeyboard());

        onView(withId(R.id.confirmPassword_edittext))
                .perform(typeText("DifferentPass123!"), closeSoftKeyboard());

        onView(withId(R.id.registerMail_button)).perform(click());

        onView(withId(R.id.registerMail_button)).check(matches(isDisplayed()));
    }

        private void waitForIntent(String componentName) {
                long timeoutMs = 20000L;
                long pollMs = 250L;
                long end = SystemClock.elapsedRealtime() + timeoutMs;
                AssertionError lastError = null;

                while (SystemClock.elapsedRealtime() < end) {
                        try {
                                intended(hasComponent(componentName));
                                return;
                        } catch (AssertionError error) {
                                lastError = error;
                                SystemClock.sleep(pollMs);
                        }
                }

                if (lastError != null) {
                        throw lastError;
                }
                fail("Expected intent was not launched: " + componentName);
        }
}
