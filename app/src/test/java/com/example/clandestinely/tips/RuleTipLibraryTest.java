package com.example.clandestinely.tips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** Tests the tip-picking rules with a small made-up library. */
public class RuleTipLibraryTest {

    private static final List<String> NONE = Collections.emptyList();

    private static RuleTip tip(String id, String state, String sensitivity, int priority, String... slots) {
        return new RuleTip(id, state, sensitivity, Arrays.asList(slots), priority, id + " en", id + " ms", "team");
    }

    private final RuleTipLibrary library = new RuleTipLibrary(Arrays.asList(
            tip("normal-1", "normal", null, 1),
            tip("normal-night", "normal", null, 2, "night"),
            tip("rain-1", "rain", null, 1),
            tip("rain-2", "rain", null, 2),
            tip("rain-3", "rain", null, 2),
            tip("rain-peak", "rain", null, 2, "morning", "evening"),
            tip("rain-mobility", "rain", "mobility", 1),
            tip("thunder-1", "thunderstorm", null, 1),
            tip("thunder-2", "thunderstorm", null, 2),
            tip("haze-1", "haze_sensitive", null, 1),
            tip("haze-asthma", "haze_sensitive", "asthma", 1),
            tip("heat-1", "heat", null, 1)));

    private static List<String> ids(List<RuleTip> tips) {
        List<String> ids = new ArrayList<>();
        for (RuleTip t : tips) ids.add(t.getId());
        return ids;
    }

    @Test
    public void mostDangerousStateComesFirst() {
        List<RuleTip> tips = library.select(Arrays.asList("rain", "thunderstorm"), NONE, "afternoon", 3, 0);
        assertEquals("thunder-1", tips.get(0).getId());
        assertEquals("rain-1", tips.get(1).getId());
    }

    @Test
    public void sensitivityTipComesBeforeGeneralTipForThatState() {
        List<RuleTip> tips = library.select(Arrays.asList("haze_sensitive"), Arrays.asList("asthma"), "afternoon", 3, 0);
        assertEquals(Arrays.asList("haze-asthma", "haze-1"), ids(tips));
    }

    @Test
    public void sensitivityTipsAreOnlyShownToThatUser() {
        List<RuleTip> tips = library.select(Arrays.asList("haze_sensitive"), NONE, "afternoon", 3, 0);
        assertFalse(ids(tips).contains("haze-asthma"));
    }

    @Test
    public void neverMoreThanMaxTips() {
        List<RuleTip> tips = library.select(
                Arrays.asList("thunderstorm", "rain", "heat", "haze_sensitive"),
                Arrays.asList("asthma", "mobility"), "afternoon", 3, 0);
        assertEquals(3, tips.size());
    }

    @Test
    public void fillsUpWithMoreTipsForTheSameState() {
        List<RuleTip> tips = library.select(Arrays.asList("rain"), NONE, "afternoon", 3, 0);
        assertEquals(3, tips.size());
        assertEquals("rain-1", tips.get(0).getId());
    }

    @Test
    public void timeSlotTipsOnlyAppearAtThoseTimes() {
        assertFalse(ids(library.select(Arrays.asList("rain"), NONE, "afternoon", 10, 0)).contains("rain-peak"));
        assertTrue(ids(library.select(Arrays.asList("rain"), NONE, "morning", 10, 0)).contains("rain-peak"));
    }

    @Test
    public void emptyStatesMeansNormal() {
        List<RuleTip> tips = library.select(NONE, NONE, "afternoon", 3, 0);
        assertEquals("normal-1", tips.get(0).getId());
    }

    @Test
    public void equalPriorityTipsTakeTurns() {
        List<String> hour0 = ids(library.select(Arrays.asList("rain"), NONE, "afternoon", 2, 0));
        List<String> hour1 = ids(library.select(Arrays.asList("rain"), NONE, "afternoon", 2, 1));
        assertEquals("rain-1", hour0.get(0));
        assertEquals("rain-1", hour1.get(0));
        assertFalse(hour0.get(1).equals(hour1.get(1)));
    }

    @Test
    public void noDuplicateTips() {
        List<String> tips = ids(library.select(Arrays.asList("rain"), Arrays.asList("mobility"), "morning", 10, 3));
        assertEquals(tips.size(), new java.util.HashSet<>(tips).size());
    }

    @Test
    public void languageFallsBackToEnglish() {
        RuleTip t = new RuleTip("x", "rain", null, null, 1, "Hello", "", "team");
        assertEquals("Hello", t.text("ms"));
        assertEquals("Hello", t.text("en"));
    }

    @Test
    public void combineJoinsTexts() {
        List<RuleTip> tips = library.select(Arrays.asList("thunderstorm"), NONE, "afternoon", 2, 0);
        assertEquals("thunder-1 ms thunder-2 ms", RuleTipLibrary.combine(tips, "ms"));
    }
}
