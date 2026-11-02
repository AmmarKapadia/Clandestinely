package com.example.clandestinely.data.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Names and allowed values from docs/firestore_schema.md.
 * Use these constants instead of typing the strings, so a typo can't break a query.
 */
public final class Schema {

    private Schema() {
    }

    /**
     * Our Firestore database is named "default" (not the usual "(default)"),
     * so the app must ask for it by name. Always use FirestoreDb.get().
     */
    public static final String DATABASE_ID = "default";

    // ---------- collections ----------
    public static final String USERS = "users";
    public static final String HAZARD_REPORTS = "hazard_reports";
    public static final String CONFIRMATIONS = "confirmations";
    public static final String TIPS_CACHE = "tips_cache";
    public static final String HAZARD_PHOTOS_FOLDER = "hazard_photos";

    // ---------- hazard categories ----------
    public static final String CATEGORY_FLOOD = "flood";
    public static final String CATEGORY_FALLEN_TREE = "fallen_tree";
    public static final String CATEGORY_BLOCKED_DRAIN = "blocked_drain";
    public static final String CATEGORY_OTHER = "other";
    public static final List<String> HAZARD_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            CATEGORY_FLOOD, CATEGORY_FALLEN_TREE, CATEGORY_BLOCKED_DRAIN, CATEGORY_OTHER));

    // ---------- weather states (weather_rules.md, plus UV) ----------
    public static final String STATE_NORMAL = "normal";
    public static final String STATE_RAIN = "rain";
    public static final String STATE_THUNDERSTORM = "thunderstorm";
    public static final String STATE_HEAT = "heat";
    public static final String STATE_HAZE_SENSITIVE = "haze_sensitive";
    public static final String STATE_HAZE_UNHEALTHY = "haze_unhealthy";
    public static final String STATE_UV = "uv";
    public static final List<String> WEATHER_STATES = Collections.unmodifiableList(Arrays.asList(
            STATE_NORMAL, STATE_RAIN, STATE_THUNDERSTORM, STATE_HEAT,
            STATE_HAZE_SENSITIVE, STATE_HAZE_UNHEALTHY, STATE_UV));

    // ---------- sensitivity names used in tip keys ----------
    public static final String SENSITIVITY_ASTHMA = "asthma";
    public static final String SENSITIVITY_HEAT = "heat";
    public static final String SENSITIVITY_MOBILITY = "mobility";
    public static final String SENSITIVITY_UV = "uv";
    public static final List<String> SENSITIVITIES = Collections.unmodifiableList(Arrays.asList(
            SENSITIVITY_ASTHMA, SENSITIVITY_HEAT, SENSITIVITY_MOBILITY, SENSITIVITY_UV));

    // ---------- tip sources ----------
    public static final String SOURCE_AI = "ai";
    public static final String SOURCE_RULE = "rule";
}
