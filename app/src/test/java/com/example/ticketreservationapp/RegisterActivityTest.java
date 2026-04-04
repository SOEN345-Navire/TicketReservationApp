package com.example.ticketreservationapp;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
public class RegisterActivityTest {

    private class Data{
        String email;
        String password;
        String confirmPassword;
        String toastText;
        Data(String email, String password, String confirmPassword, String toastText){
            this.email = email;
            this.password = password;
            this.confirmPassword = confirmPassword;
            this.toastText = toastText;
        }
    }

    @Test
    public void registerWithEmailTest() {
        ArrayList<Data> testCases = new ArrayList<>();
        testCases.add(new Data("", "Password1234!", "Password1234!", "Please enter credentials"));
        testCases.add(new Data("test@gmail.com", "Password1234!", "", "Please enter credentials"));
        testCases.add(new Data("test@gmail.com", "", "Password1234!", "Please enter credentials"));
        testCases.add(new Data("test@gmail.com", "Password1234!", "Password1234?", "Passwords do not match"));
        testCases.add(new Data("test@gmail.com", "Pa1!", "Pa1!", "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"));
        testCases.add(new Data("test@gmail.com", "password1234!", "password1234!", "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"));
        testCases.add(new Data("test@gmail.com", "PASSWORD1234!", "PASSWORD1234!", "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"));
        testCases.add(new Data("test@gmail.com", "Password!", "Password!", "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"));
        testCases.add(new Data("test@gmail.com", "Password1234", "Password1234", "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"));

        RegisterActivity activity;
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class)) {
            activity = controller.get();
        }
        for (Data testCase : testCases) {
            ShadowToast.reset();
            activity.registerWithEmail(testCase.email, testCase.password, testCase.confirmPassword);
            String toastText = ShadowToast.getTextOfLatestToast();
            assert(testCase.toastText.equals(toastText));
        }
    }
}
