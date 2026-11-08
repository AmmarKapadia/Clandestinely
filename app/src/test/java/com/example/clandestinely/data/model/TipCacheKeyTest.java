package com.example.clandestinely.data.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TipCacheKeyTest {

    private static final List<String> NONE = Collections.emptyList();

    private static String slotAt(int hour, int minute) {
        Calendar c = Calendar.getInstance(HazardExpiry.MALAYSIA);
        c.clear();
        c.set(2026, Calendar.OCTOBER, 29, hour, minute, 0);
        return TipCacheKey.timeSlotAt(c.getTimeInMillis());
    }

    @Test
    public void keysMatchSchemaExamples() {
        assertEquals("rain_afternoon_none",
                TipCacheKey.build(Arrays.asList("rain"), "afternoon", NONE));
        assertEquals("haze_sensitive+rain_afternoon_asthma",
                TipCacheKey.build(Arrays.asList("rain", "haze_sensitive"), "afternoon", Arrays.asList("asthma")));
        assertEquals("heat_morning_mobility+uv",
                TipCacheKey.build(Arrays.asList("heat"), "morning", Arrays.asList("uv", "mobility")));
    }

    @Test
    public void normalOnlyWhenNothingElseIsActive() {
        assertEquals("rain_evening_none",
                TipCacheKey.build(Arrays.asList("normal", "rain"), "evening", NONE));
        assertEquals("normal_evening_none",
                TipCacheKey.build(NONE, "evening", NONE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownState() {
        TipCacheKey.build(Arrays.asList("snow"), "morning", NONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownTimeSlot() {
        TipCacheKey.build(Arrays.asList("rain"), "noon", NONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownSensitivity() {
        TipCacheKey.build(Arrays.asList("rain"), "morning", Arrays.asList("diabetes"));
    }

    @Test
    public void timeSlotBoundaries() {
        assertEquals("night", slotAt(5, 59));
        assertEquals("morning", slotAt(6, 0));
        assertEquals("morning", slotAt(11, 59));
        assertEquals("afternoon", slotAt(12, 0));
        assertEquals("evening", slotAt(18, 0));
        assertEquals("night", slotAt(22, 0));
    }

    @Test
    public void healthFlagsBecomeSortedSensitivities() {
        assertEquals(Arrays.asList("asthma", "uv"),
                TipCacheKey.sensitivitiesOf(new Health(true, false, true, false)));
        assertEquals(NONE, TipCacheKey.sensitivitiesOf(new Health()));
    }
}
