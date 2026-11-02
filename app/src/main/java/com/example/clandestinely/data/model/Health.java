package com.example.clandestinely.data.model;

/** users/{uid}.health — the user's health sensitivities (FR6.2). */
public class Health {

    private boolean asthma;
    private boolean heatSensitive;
    private boolean uvSensitive;
    private boolean mobilityLimited;

    /** Firestore needs an empty constructor. */
    public Health() {
    }

    public Health(boolean asthma, boolean heatSensitive, boolean uvSensitive, boolean mobilityLimited) {
        this.asthma = asthma;
        this.heatSensitive = heatSensitive;
        this.uvSensitive = uvSensitive;
        this.mobilityLimited = mobilityLimited;
    }

    public boolean getAsthma() {
        return asthma;
    }

    public void setAsthma(boolean asthma) {
        this.asthma = asthma;
    }

    public boolean getHeatSensitive() {
        return heatSensitive;
    }

    public void setHeatSensitive(boolean heatSensitive) {
        this.heatSensitive = heatSensitive;
    }

    public boolean getUvSensitive() {
        return uvSensitive;
    }

    public void setUvSensitive(boolean uvSensitive) {
        this.uvSensitive = uvSensitive;
    }

    public boolean getMobilityLimited() {
        return mobilityLimited;
    }

    public void setMobilityLimited(boolean mobilityLimited) {
        this.mobilityLimited = mobilityLimited;
    }
}
