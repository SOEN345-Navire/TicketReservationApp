package com.example.ticketreservationapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConfirmEmailTest {

    @Test
    public void onCreateNullUserTest(){
        ConfirmEmailActivity activity;
        try (ActivityController<ConfirmEmailActivity> controller = Robolectric.buildActivity(ConfirmEmailActivity.class)) {
            activity = controller.get();
        }
        FirebaseAuth auth = mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)){
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            activity.onCreate(null);
            assert(activity.isFinishing());
        }

    }

    @Test
    public void onCreateTest(){
        ConfirmEmailActivity activity;
        try (ActivityController<ConfirmEmailActivity> controller = Robolectric.buildActivity(ConfirmEmailActivity.class)) {
            activity = controller.get();
        }

        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);
        try(MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)){
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            activity.onCreate(null);
            assert(activity.confirmButton.equals(activity.findViewById(R.id.confirm_button)));

            Task<Void> reload = mock(Task.class);
            when(user.reload()).thenReturn(reload);
            Task<Void> task = mock(Task.class);
            activity.confirmButton.performClick();
            verify(reload).addOnCompleteListener(any(ConfirmEmailActivity.class), any(OnCompleteListener.class));
        }

    }

    @Test
    public void handleEmailVerificationResultTest() {
        ConfirmEmailActivity activity;
        try (ActivityController<ConfirmEmailActivity> controller = Robolectric.buildActivity(ConfirmEmailActivity.class)) {
            activity = controller.get();
        }
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);
        Task<Void> task = mock(Task.class);
        activity.auth = auth;

        //Test successful verification
        when(user.isEmailVerified()).thenReturn(true);
        when(task.isSuccessful()).thenReturn(true);
        activity.handleEmailVerificationResult(task);
        assert(activity.isFinishing());

        //Test failed verification
        when(user.isEmailVerified()).thenReturn(false);
        when(task.isSuccessful()).thenReturn(true);
        activity.handleEmailVerificationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Please verify your email"));

        //Test null user
        when(auth.getCurrentUser()).thenReturn(null);
        activity.handleEmailVerificationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Please verify your email"));


        //Test failed task
        when(task.isSuccessful()).thenReturn(false);
        activity.handleEmailVerificationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Failed to reload user"));

    }
}
