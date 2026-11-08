package com.example.clandestinely.data.model;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

public class HazardExpiryTest {

    /** A time on 29 Oct 2026, Malaysia time. */
    private static long my(int hour, int minute) {
        Calendar c = Calendar.getInstance(HazardExpiry.MALAYSIA);
        c.clear();
        c.set(2026, Calendar.OCTOBER, 29, hour, minute, 0);
        return c.getTimeInMillis();
    }

    private static long my(int hour) {
        return my(hour, 0);
    }

    private static final long MIDNIGHT = my(0) + 24 * HazardExpiry.HOUR_MS; // 30 Oct 00:00

    @Test
    public void nextMidnightIsMalaysiaMidnight() {
        assertEquals(MIDNIGHT, HazardExpiry.nextMidnight(my(0, 1)));
        assertEquals(MIDNIGHT, HazardExpiry.nextMidnight(my(15)));
        assertEquals(MIDNIGHT, HazardExpiry.nextMidnight(my(23, 59)));
    }

    @Test
    public void newReportLastsTwoHours() {
        assertEquals(my(12), HazardExpiry.initialExpiry(my(10)));
    }

    @Test
    public void newReportAt11pmEndsAtMidnight() {
        assertEquals(MIDNIGHT, HazardExpiry.initialExpiry(my(23)));
    }

    @Test
    public void eachConfirmationAddsOneHour() {
        long created = my(8);
        long expires = HazardExpiry.initialExpiry(created);
        expires = HazardExpiry.extendedExpiry(created, expires);
        assertEquals(my(11), expires);
        expires = HazardExpiry.extendedExpiry(created, expires);
        assertEquals(my(12), expires);
    }

    @Test
    public void neverMoreThanTenHours() {
        long created = my(6);
        long expires = HazardExpiry.initialExpiry(created);
        for (int i = 0; i < 20; i++) expires = HazardExpiry.extendedExpiry(created, expires);
        assertEquals(my(16), expires);
    }

    @Test
    public void reportAt9pmNeverPassesMidnight() {
        long created = my(21);
        long expires = HazardExpiry.initialExpiry(created);
        for (int i = 0; i < 5; i++) expires = HazardExpiry.extendedExpiry(created, expires);
        assertEquals(MIDNIGHT, expires);
    }
}
