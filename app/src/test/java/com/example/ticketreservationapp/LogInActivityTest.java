package com.example.ticketreservationapp;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LogInActivityTest {

    @Test
    public void onCreateTest(){
        LogInActivity activity;
        try (ActivityController<LogInActivity> controller = Robolectric.buildActivity(LogInActivity.class)) {
            activity = controller.get();
        }
        FirebaseAuth auth = mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)){
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            activity.onCreate(null);
            assert(activity.auth.equals(auth));
            assert(activity.email.equals(activity.findViewById(R.id.email_edittext)));
            assert(activity.password.equals(activity.findViewById(R.id.password_edittext)));
            assert(activity.phone.equals(activity.findViewById(R.id.phone_edittext)));
            assert(activity.registerLink.equals(activity.findViewById(R.id.register_link)));
            assert(activity.logMail.equals(activity.findViewById(R.id.loginMail_button)));
            assert(activity.logPhone.equals(activity.findViewById(R.id.loginPhone_button)));

            activity.registerLink.performClick();
            assert(activity.isFinishing());

            activity.logMail.performClick();
            assert(ShadowToast.getTextOfLatestToast().equals("Please enter credentials"));

            activity.logPhone.performClick();
            assert(ShadowToast.getTextOfLatestToast().equals("Please enter a phone number"));
        }
    }

    @Test
    public void logInWithEmailTest(){
        HashMap<String, String> map = new HashMap<>();
        map.put("email", "");
        map.put("", "password");

        LogInActivity activity;
        try (ActivityController<LogInActivity> controller = Robolectric.buildActivity(LogInActivity.class)) {
            activity = controller.get();
        }

        for (String mail : map.keySet()) {
            activity.logInWithEmail(mail, map.get(mail));
            String toastText = ShadowToast.getTextOfLatestToast();
            assert("Please enter credentials".equals(toastText));
        }

        activity.auth = mock(FirebaseAuth.class);
        Task<AuthResult> task = mock(Task.class);
        when(task.isSuccessful()).thenReturn(true);
        when(activity.auth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(task);
        activity.logInWithEmail("test@gmail.com", "Password1234!");
        verify(activity.auth).signInWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void handleLoginResultTest() {
        LogInActivity activity;
        try (ActivityController<LogInActivity> controller = Robolectric.buildActivity(LogInActivity.class)) {
            activity = controller.get();
        }
        //Test successful login
        Task<AuthResult> task = mock(Task.class);
        when(task.isSuccessful()).thenReturn(true);
        activity.handleLoginResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Login successful!"));

        //Test failed login
        when(task.isSuccessful()).thenReturn(false);
        activity.handleLoginResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Login failed! Please try again later"));
    }

    @Test
    public void logInWithPhoneTest(){
        HashMap<String, String> map = new HashMap<>();
        map.put("", "Please enter a phone number");
        map.put("123456789", "Please enter a valid phone number");
        map.put("12345678901", "Please enter a valid phone number");

        LogInActivity activity;
        try (ActivityController<LogInActivity> controller = Robolectric.buildActivity(LogInActivity.class)) {
            activity = controller.get();
        }

        for (String phone : map.keySet()) {
            ShadowToast.reset();
            activity.logInWithPhone(phone);
            String toastText = ShadowToast.getTextOfLatestToast();
            assert(map.get(phone).equals(toastText));
        }
        activity.logInWithPhone("1234567890");
        assert(Authentification.phone.equals("+11234567890"));
    }
}
