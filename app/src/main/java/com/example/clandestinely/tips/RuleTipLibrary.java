package com.example.clandestinely.tips;

import com.example.clandestinely.data.model.HazardExpiry;
import com.example.clandestinely.data.model.Schema;
import com.example.clandestinely.data.model.TipCacheKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The rule-based tip library (FR3.3): picks tips for the current weather, time of day and the
 * user's sensitivities. Used when Gemini is unavailable or the phone is offline.
 *
 * <pre>
 * RuleTipLibrary library = RuleTipLoader.get(context);
 * List&lt;RuleTip&gt; tips = library.select(states, sensitivities, TipCacheKey.timeSlotAt(now));
 * String text = RuleTipLibrary.combine(tips, language);
 * </pre>
 *
 * How tips are picked:
 * 1. Active weather states are handled most dangerous first:
 *    thunderstorm, unhealthy haze, rain, heat, haze for sensitive groups, UV, normal.
 * 2. For each state: the user's sensitivity tips first (e.g. asthma during haze),
 *    then the most important tip for everyone.
 * 3. Any space left is filled with more tips for everyone, most dangerous state first.
 * Tips with the same priority take turns each hour, so users don't see the same one all day.
 */
public final class RuleTipLibrary {

    /** Most dangerous first. */
    public static final List<String> SEVERITY_ORDER = Collections.unmodifiableList(Arrays.asList(
            Schema.STATE_THUNDERSTORM,
            Schema.STATE_HAZE_UNHEALTHY,
            Schema.STATE_RAIN,
            Schema.STATE_HEAT,
            Schema.STATE_HAZE_SENSITIVE,
            Schema.STATE_UV,
            Schema.STATE_NORMAL));

    public static final int DEFAULT_MAX_TIPS = 3;

    private final List<RuleTip> tips;

    public RuleTipLibrary(List<RuleTip> tips) {
        this.tips = Collections.unmodifiableList(new ArrayList<>(tips));
    }

    public List<RuleTip> getAll() {
        return tips;
    }

    /** Up to 3 tips, rotating each hour. */
    public List<RuleTip> select(Collection<String> states, Collection<String> sensitivities, String timeSlot) {
        long hour = System.currentTimeMillis() / HazardExpiry.HOUR_MS;
        return select(states, sensitivities, timeSlot, DEFAULT_MAX_TIPS, hour);
    }

    /**
     * @param states        active weather states (Schema.STATE_*); empty means normal
     * @param sensitivities the user's sensitivities (TipCacheKey.sensitivitiesOf(health))
     * @param timeSlot      TipCacheKey.timeSlotAt(now)
     * @param maxTips       how many tips to return at most
     * @param rotation      any number that changes over time (e.g. the current hour); tips with
     *                      equal priority take turns based on it
     */
    public List<RuleTip> select(Collection<String> states, Collection<String> sensitivities,
                                String timeSlot, int maxTips, long rotation) {
        List<String> active = new ArrayList<>(TipCacheKey.normalisedStates(states));
        Collections.sort(active, (a, b) -> Integer.compare(SEVERITY_ORDER.indexOf(a), SEVERITY_ORDER.indexOf(b)));
        Set<String> userSensitivities = new HashSet<>(TipCacheKey.normalisedSensitivities(sensitivities));

        LinkedHashSet<RuleTip> picked = new LinkedHashSet<>();

        // Pass 1: for each state, the user's sensitivity tips, then the top tip for everyone.
        for (String state : active) {
            List<RuleTip> matching = ordered(matching(state, timeSlot), rotation);
            Set<String> sensitivitiesDone = new HashSet<>();
            for (RuleTip tip : matching) {
                String s = tip.getSensitivity();
                if (s != null && userSensitivities.contains(s) && sensitivitiesDone.add(s)) {
                    picked.add(tip);
                }
            }
            for (RuleTip tip : matching) {
                if (tip.isForEveryone()) {
                    picked.add(tip);
                    break;
                }
            }
        }

        // Pass 2: fill up with more tips for everyone, most dangerous state first.
        for (String state : active) {
            for (RuleTip tip : ordered(matching(state, timeSlot), rotation)) {
                if (picked.size() >= maxTips) break;
                if (tip.isForEveryone()) picked.add(tip);
            }
        }

        List<RuleTip> result = new ArrayList<>(picked);
        return result.size() > maxTips ? result.subList(0, maxTips) : result;
    }

    /** Joins tips into one paragraph, e.g. for the Home card or for saving to tips_cache. */
    public static String combine(List<RuleTip> tips, String language) {
        StringBuilder sb = new StringBuilder();
        for (RuleTip tip : tips) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(tip.text(language));
        }
        return sb.toString();
    }

    private List<RuleTip> matching(String state, String timeSlot) {
        List<RuleTip> result = new ArrayList<>();
        for (RuleTip tip : tips) {
            if (tip.getState().equals(state) && tip.appliesAt(timeSlot)) result.add(tip);
        }
        return result;
    }

    /** Sorted by priority; tips with the same priority are rotated by {@code rotation}. */
    private static List<RuleTip> ordered(List<RuleTip> list, long rotation) {
        List<RuleTip> sorted = new ArrayList<>(list);
        Collections.sort(sorted, (a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        List<RuleTip> result = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            int j = i;
            while (j < sorted.size() && sorted.get(j).getPriority() == sorted.get(i).getPriority()) j++;
            List<RuleTip> group = new ArrayList<>(sorted.subList(i, j));
            int size = group.size();
            int shift = (int) (((rotation % size) + size) % size);
            Collections.rotate(group, -shift);
            result.addAll(group);
            i = j;
        }
        return result;
    }
}
