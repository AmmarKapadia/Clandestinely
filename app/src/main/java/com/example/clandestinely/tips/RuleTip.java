package com.example.clandestinely.tips;

import java.util.Collections;
import java.util.List;

/**
 * One tip from the rule-based tip library (assets/rule_tips_v1.json), FR3.3.
 */
public final class RuleTip {

    private final String id;
    private final String state;          // weather state, e.g. "rain" (see Schema.STATE_*)
    private final String sensitivity;    // "asthma", "heat", "mobility", "uv", or null for everyone
    private final List<String> timeSlots; // empty = any time of day
    private final int priority;          // 1 = most important for this state
    private final String en;
    private final String ms;
    private final String source;         // key into the "sources" list in the JSON file

    public RuleTip(String id, String state, String sensitivity, List<String> timeSlots,
                   int priority, String en, String ms, String source) {
        this.id = id;
        this.state = state;
        this.sensitivity = sensitivity;
        this.timeSlots = timeSlots == null ? Collections.<String>emptyList() : Collections.unmodifiableList(timeSlots);
        this.priority = priority;
        this.en = en;
        this.ms = ms;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    /** Null means the tip is for everyone. */
    public String getSensitivity() {
        return sensitivity;
    }

    public boolean isForEveryone() {
        return sensitivity == null;
    }

    public List<String> getTimeSlots() {
        return timeSlots;
    }

    public int getPriority() {
        return priority;
    }

    public String getSource() {
        return source;
    }

    /** True if this tip can be shown in the given time slot. */
    public boolean appliesAt(String timeSlot) {
        return timeSlots.isEmpty() || timeSlots.contains(timeSlot);
    }

    /** The tip text in the app's language: "ms" for Bahasa Melayu, anything else for English. */
    public String text(String language) {
        if ("ms".equals(language) && ms != null && !ms.isEmpty()) return ms;
        return en;
    }
}
