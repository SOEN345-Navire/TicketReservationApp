package com.example.ticketreservationapp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

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
public class ConfirmPhoneTest {

    @Test
    public void onCreateTest() {
        ConfirmPhoneActivity activity;
        try (ActivityController<ConfirmPhoneActivity> controller = Robolectric.buildActivity(ConfirmPhoneActivity.class)) {
            activity = controller.get();
        }
        FirebaseAuth auth = mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> mockedAuth = mockStatic(FirebaseAuth.class)){
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            Authentification.phone = "1234567890";
            activity.onCreate(null);
            assert(activity.auth.equals(auth));
            assert(activity.verificationId == null);
            assert(activity.token == null);
            assert(activity.findViewById(R.id.code_edittext).equals(activity.findViewById(R.id.code_edittext)));
            assert(activity.findViewById(R.id.confirm_button).equals(activity.findViewById(R.id.confirm_button)));

            activity.findViewById(R.id.confirm_button).performClick();
            assert(ShadowToast.getTextOfLatestToast().equals("Please enter the code"));
        }
    }

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

        //Test invalid input
        for (String code : map.keySet()) {
            ShadowToast.reset();
            activity.confirmPhone(code);
            String toastText = ShadowToast.getTextOfLatestToast();
            assert(map.get(code).equals(toastText));
        }

        //Test valid input
        activity.verificationId = "1234567890";
        activity.auth = mock(FirebaseAuth.class);
        PhoneAuthCredential credential = mock(PhoneAuthCredential.class);
        Task<AuthResult> task = mock(Task.class);
        when(activity.auth.signInWithCredential(credential)).thenReturn(task);
        when(task.isSuccessful()).thenReturn(true);

        try(MockedStatic<PhoneAuthProvider> mockedAuth = mockStatic(PhoneAuthProvider.class)){
            mockedAuth.when(() -> PhoneAuthProvider.getCredential(activity.verificationId, "123456")).thenReturn(credential);
            activity.confirmPhone("123456");
            verify(activity.auth).signInWithCredential(credential);
        }

    }

    @Test
    public void getOptionsTest(){
        ConfirmPhoneActivity activity;
        try (ActivityController<ConfirmPhoneActivity> controller = Robolectric.buildActivity(ConfirmPhoneActivity.class)) {
            activity = controller.get();
        }
        activity.auth = mock(FirebaseAuth.class);
        PhoneAuthOptions options = activity.getOptions("1234567890");
        assert(options != null);
    }

    @Test
    public void callbacksTest(){
        ConfirmPhoneActivity activity;
        try (ActivityController<ConfirmPhoneActivity> controller = Robolectric.buildActivity(ConfirmPhoneActivity.class)) {
            activity = controller.get();
        }
        activity.auth = mock(FirebaseAuth.class);
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = activity.getCallbacks();

        //Test onCodeSent
        PhoneAuthProvider.ForceResendingToken token = mock(PhoneAuthProvider.ForceResendingToken.class);
        callbacks.onCodeSent("1234567890", token);
        assert(activity.verificationId.equals("1234567890"));
        assert(activity.token.equals(token));
        assert(ShadowToast.getTextOfLatestToast().equals("Verify your SMS Inbox"));

        //Test OnVerificationFailed
        FirebaseException exception = mock(FirebaseException.class);
        when(exception.getMessage()).thenReturn("Error");
        callbacks.onVerificationFailed(exception);
        assert(ShadowToast.getTextOfLatestToast().equals("Error"));

        //Test onVerificationCompleted
        PhoneAuthCredential credential = mock(PhoneAuthCredential.class);
        Task<AuthResult> task = mock(Task.class);
        when(activity.auth.signInWithCredential(credential)).thenReturn(task);
        callbacks.onVerificationCompleted(credential);
        verify(activity.auth).signInWithCredential(credential);
    }

    @Test
    public void handleLoginResultTest(){
        ConfirmPhoneActivity activity;
        try (ActivityController<ConfirmPhoneActivity> controller = Robolectric.buildActivity(ConfirmPhoneActivity.class)) {
            activity = controller.get();
        }

        //Test successful login
        Task<AuthResult> task = mock(Task.class);
        when(task.isSuccessful()).thenReturn(true);
        activity.handleLoginResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Login successful"));

        //Test failed login
        when(task.isSuccessful()).thenReturn(false);
        activity.handleLoginResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Login failed"));
    }


}
