package com.example.clandestinely.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

/** users/{uid} — one document per signed-in user, including guests. */
public class UserProfile {

    private String uid = "";
    private String displayName = "";
    private String email;            // null for guests
    private boolean guest;
    private Timestamp createdAt;     // filled in by the server
    private Timestamp updatedAt;     // filled in by the server
    private List<String> usualLocations = new ArrayList<>();
    private Health health = new Health();
    private UserSettings settings = new UserSettings();
    private AlertPrefs alertPrefs = new AlertPrefs();
    private List<String> fcmTokens = new ArrayList<>();

    /** Firestore needs an empty constructor. */
    public UserProfile() {
    }

    /** The document ID (the user's Firebase uid). Not saved as a field. */
    @DocumentId
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Without @PropertyName, Firestore would save this as "guest" instead of "isGuest".
    @PropertyName("isGuest")
    public boolean isGuest() {
        return guest;
    }

    @PropertyName("isGuest")
    public void setGuest(boolean guest) {
        this.guest = guest;
    }

    @ServerTimestamp
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @ServerTimestamp
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getUsualLocations() {
        return usualLocations;
    }

    public void setUsualLocations(List<String> usualLocations) {
        this.usualLocations = usualLocations;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    public UserSettings getSettings() {
        return settings;
    }

    public void setSettings(UserSettings settings) {
        this.settings = settings;
    }

    public AlertPrefs getAlertPrefs() {
        return alertPrefs;
    }

    public void setAlertPrefs(AlertPrefs alertPrefs) {
        this.alertPrefs = alertPrefs;
    }

    public List<String> getFcmTokens() {
        return fcmTokens;
    }

    public void setFcmTokens(List<String> fcmTokens) {
        this.fcmTokens = fcmTokens;
    }
}
