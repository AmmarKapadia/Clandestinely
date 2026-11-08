package com.example.clandestinely.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

/** tips_cache/{cacheKey} — a saved tip shared by all users (FR3.1). */
public class TipCacheEntry {

    private String key = "";
    private List<String> states = new ArrayList<>();
    private String timeSlot = "";
    private List<String> sensitivities = new ArrayList<>();
    private String text = "";
    private String source = Schema.SOURCE_RULE;
    private String model;            // e.g. "gemini (Firebase AI Logic)"; null for rule tips
    private Timestamp createdAt;
    private Timestamp expiresAt;

    /** Firestore needs an empty constructor. */
    public TipCacheEntry() {
    }

    @DocumentId
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getStates() {
        return states;
    }

    public void setStates(List<String> states) {
        this.states = states;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public List<String> getSensitivities() {
        return sensitivities;
    }

    public void setSensitivities(List<String> sensitivities) {
        this.sensitivities = sensitivities;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }
}
