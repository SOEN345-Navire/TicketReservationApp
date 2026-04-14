package com.example.ticketreservationapp;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.espresso.intent.Intents;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * E2E tests for login flow.
 * Tests login form interactions and navigation to app home screen.
 */
@RunWith(AndroidJUnit4.class)
public class LoginE2ETest {

        private static final long PHONE_CODE_ENTRY_DELAY_MS = 4000L;

    @Rule
    public ActivityScenarioRule<LogInActivity> activityRule =
            new ActivityScenarioRule<>(LogInActivity.class);

    @Before
    public void setUp() {
        Intents.init();

        intending(allOf(
                hasComponent(MainActivity.class.getName())
        )).respondWith(new android.app.Instrumentation.ActivityResult(
                android.app.Activity.RESULT_OK,
                null
        ));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
        public void emailLogin_registersUserThenLogsIn() throws Exception {
                String email = "e2e-login-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
                String password = "ValidPass123!";

                ensureUserExists(email, password);

        onView(withId(R.id.email_edittext))
                .perform(typeText(email), closeSoftKeyboard())
                .check(matches(withText(email)));

        onView(withId(R.id.password_edittext))
                .perform(typeText(password), closeSoftKeyboard());

                onView(withId(R.id.loginMail_button)).perform(click());

        waitForIntent(MainActivity.class.getName());
    }

    @Test
    public void phoneLogin_withProvidedCode_logsIn() {
        String phone = "6505551212";
        String code = "123456";

        onView(withId(R.id.phone_edittext))
                .perform(typeText(phone), closeSoftKeyboard())
                .check(matches(withText(phone)));

        onView(withId(R.id.loginPhone_button)).perform(click());

        onView(withId(R.id.code_edittext)).check(matches(isDisplayed()));
        SystemClock.sleep(PHONE_CODE_ENTRY_DELAY_MS);

        onView(withId(R.id.code_edittext))
                .perform(replaceText(code), closeSoftKeyboard())
                .check(matches(withText(code)));

        onView(withId(R.id.confirm_button)).perform(click());

        waitForIntent(MainActivity.class.getName());
    }

        private void ensureUserExists(String email, String password) throws Exception {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Exception> error = new AtomicReference<>();

                auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                        if (!task.isSuccessful()) {
                                                Exception exception = task.getException();
                                                if (!(exception instanceof FirebaseAuthUserCollisionException)) {
                                                        error.set(exception);
                                                }
                                        }
                                        auth.signOut();
                                        latch.countDown();
                                });

                boolean completed = latch.await(30, TimeUnit.SECONDS);
                assertTrue("Timed out creating user for login E2E test", completed);

                if (error.get() != null) {
                        fail("Unable to create user for login E2E test: " + error.get().getMessage());
                }
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
