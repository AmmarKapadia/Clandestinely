package com.example.clandestinely.data.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Builds tips_cache document IDs (schema section 5.2), e.g. "haze_sensitive+rain_afternoon_asthma".
 * Same logic as firebase/schema.js.
 */
public final class TipCacheKey {

    public static final long CACHE_LIFETIME_MS = HazardExpiry.HOUR_MS;

    public static final String MORNING = "morning";
    public static final String AFTERNOON = "afternoon";
    public static final String EVENING = "evening";
    public static final String NIGHT = "night";

    private TipCacheKey() {
    }

    /** morning 06–12, afternoon 12–18, evening 18–22, night 22–06 (Malaysia time). */
    public static String timeSlotAt(long millis) {
        Calendar calendar = Calendar.getInstance(HazardExpiry.MALAYSIA);
        calendar.setTimeInMillis(millis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 18) return AFTERNOON;
        if (hour >= 18 && hour < 22) return EVENING;
        return NIGHT;
    }

    /** The user's health flags as sensitivity names, sorted A–Z. */
    public static List<String> sensitivitiesOf(Health health) {
        TreeSet<String> result = new TreeSet<>();
        if (health.getAsthma()) result.add(Schema.SENSITIVITY_ASTHMA);
        if (health.getHeatSensitive()) result.add(Schema.SENSITIVITY_HEAT);
        if (health.getMobilityLimited()) result.add(Schema.SENSITIVITY_MOBILITY);
        if (health.getUvSensitive()) result.add(Schema.SENSITIVITY_UV);
        return new ArrayList<>(result);
    }

    /** Active states sorted A–Z; "normal" only when nothing else is active. */
    public static List<String> normalisedStates(Collection<String> states) {
        TreeSet<String> active = new TreeSet<>();
        for (String state : states) {
            if (!Schema.WEATHER_STATES.contains(state)) {
                throw new IllegalArgumentException("Unknown weather state: " + state);
            }
            if (!Schema.STATE_NORMAL.equals(state)) active.add(state);
        }
        if (active.isEmpty()) active.add(Schema.STATE_NORMAL);
        return new ArrayList<>(active);
    }

    /** Sensitivities sorted A–Z, without duplicates. */
    public static List<String> normalisedSensitivities(Collection<String> sensitivities) {
        TreeSet<String> result = new TreeSet<>();
        for (String s : sensitivities) {
            if (!Schema.SENSITIVITIES.contains(s)) {
                throw new IllegalArgumentException("Unknown sensitivity: " + s);
            }
            result.add(s);
        }
        return new ArrayList<>(result);
    }

    public static String build(Collection<String> states, String timeSlot, Collection<String> sensitivities) {
        if (!MORNING.equals(timeSlot) && !AFTERNOON.equals(timeSlot)
                && !EVENING.equals(timeSlot) && !NIGHT.equals(timeSlot)) {
            throw new IllegalArgumentException("Unknown time slot: " + timeSlot);
        }
        List<String> sens = normalisedSensitivities(sensitivities);
        String sensitivityPart = sens.isEmpty() ? "none" : join(sens);
        return join(normalisedStates(states)) + "_" + timeSlot + "_" + sensitivityPart;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append('+');
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
