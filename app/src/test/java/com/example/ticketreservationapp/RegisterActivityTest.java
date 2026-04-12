package com.example.ticketreservationapp;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
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
        testCases.add(new Data("testgmail.com", "Password1234!", "Password1234!", "Please enter a valid email"));
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
        activity.auth = mock(FirebaseAuth.class);
        Task<AuthResult> task = mock(Task.class);
        when(task.isSuccessful()).thenReturn(true);
        when(activity.auth.createUserWithEmailAndPassword(anyString(), anyString())).thenReturn(task);
        activity.registerWithEmail("test@gmail.com", "Password1234!", "Password1234!");
        verify(activity.auth).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void handleRegistrationResultTest(){
        RegisterActivity activity;
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class)) {
            activity = controller.get();
        }

        //Test successful registration
        Task<AuthResult> task = mock(Task.class);
        when(task.isSuccessful()).thenReturn(true);
        activity.handleRegistrationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Registration successful!"));

        //Test failed registration with FirebaseAuthUserCollisionException
        when(task.isSuccessful()).thenReturn(false);
        FirebaseAuthUserCollisionException exception = new FirebaseAuthUserCollisionException("Error","Error");
        when(task.getException()).thenReturn(exception);
        activity.handleRegistrationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("You're already register please log in"));

        //Test failed registration with other exception
        when(task.isSuccessful()).thenReturn(false);
        when(task.getException()).thenReturn(new Exception());
        activity.handleRegistrationResult(task);
        assert(ShadowToast.getTextOfLatestToast().equals("Registration failed! Please try again later"));
    }

    @Test
    public void onCreateTest(){
        RegisterActivity activity;
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class)) {
            activity = controller.get();
        }
        FirebaseAuth auth = mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> mockedStatic = Mockito.mockStatic(FirebaseAuth.class)){
            mockedStatic.when(FirebaseAuth::getInstance).thenReturn(auth);
            activity.onCreate(null);
            assert(activity.auth.equals(FirebaseAuth.getInstance()));
            assert(activity.email.equals(activity.findViewById(R.id.email_edittext)));
            assert(activity.password.equals(activity.findViewById(R.id.password_edittext)));
            assert(activity.confirmPassword.equals(activity.findViewById(R.id.confirmPassword_edittext)));
            assert(activity.registerMail.equals(activity.findViewById(R.id.registerMail_button)));
            activity.registerMail.performClick();
            assert(ShadowToast.getTextOfLatestToast().equals("Please enter credentials"));
        }
    }
}
