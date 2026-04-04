package com.example.ticketreservationapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConfirmPhoneTest {

    @Test
    public void confirmPhoneTest(){
        HashMap<String, String> map = new HashMap<>();
        map.put("", "Please enter the code");
        map.put("12345", "Please enter a valid code");
        map.put("1234567", "Please enter a valid code");
        map.put("123456", "Please wait for the code to be sent");

        ConfirmPhoneActivity activity;
        try (ActivityController<ConfirmPhoneActivity> controller = Robolectric.buildActivity(ConfirmPhoneActivity.class)) {
            activity = controller.get();
        }

        for (String code : map.keySet()) {
            ShadowToast.reset();
            activity.confirmPhone(code);
            String toastText = ShadowToast.getTextOfLatestToast();
            assert(map.get(code).equals(toastText));
        }
    }


}
