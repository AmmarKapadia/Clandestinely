package com.example.clandestinely.data.model;

import com.google.firebase.Timestamp;

import java.util.Date;

/** Converts between Firestore Timestamps and milliseconds. */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static Timestamp fromMillis(long millis) {
        return new Timestamp(new Date(millis));
    }

    public static long toMillis(Timestamp timestamp) {
        return timestamp.toDate().getTime();
    }
}
