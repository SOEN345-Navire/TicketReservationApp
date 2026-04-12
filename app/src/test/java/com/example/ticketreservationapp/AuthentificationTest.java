package com.example.ticketreservationapp;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AuthentificationTest {

    @Test
    public void setAdminTest(){
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        CollectionReference collectionRef = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        when(db.collection("users")).thenReturn(collectionRef);
        when(collectionRef.document("uid")).thenReturn(docRef);
        try(MockedStatic<FirebaseFirestore> mockedStatic = mockStatic(FirebaseFirestore.class)){
            mockedStatic.when(FirebaseFirestore::getInstance).thenReturn(db);
            Authentification.setAdmin("uid", true);
            verify(docRef).set(Map.of("isAdmin", true));
            Authentification.setAdmin("uid", false);
            verify(docRef).set(Map.of("isAdmin", false));
        }
    }
}
