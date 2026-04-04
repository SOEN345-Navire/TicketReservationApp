package com.example.ticketreservationapp;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowToast;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class LogInActivityTest {

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
