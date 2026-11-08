package com.example.clandestinely.data.repository;

import com.example.clandestinely.data.model.Schema;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * The one place the app gets Firestore from.
 * Our database is named "default", so never use FirebaseFirestore.getInstance() with no
 * argument: that looks for "(default)" and finds nothing.
 */
public final class FirestoreDb {

    private FirestoreDb() {
    }

    public static FirebaseFirestore get() {
        return FirebaseFirestore.getInstance(Schema.DATABASE_ID);
    }
}
