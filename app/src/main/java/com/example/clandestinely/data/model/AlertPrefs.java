package com.example.clandestinely.data.model;

/** users/{uid}.alertPrefs — which alerts to send and quiet hours (FR2.1–2.3). */
public class AlertPrefs {

    private boolean rain = true;     // also covers thunderstorms
    private boolean heat = true;
    private boolean haze = true;
    private int rainLeadMinutes = 60;
    private String quietStart;       // "HH:mm", e.g. "23:00", or null
    private String quietEnd;         // "HH:mm", e.g. "07:00", or null

    public AlertPrefs() {
    }

    public boolean getRain() {
        return rain;
    }

    public void setRain(boolean rain) {
        this.rain = rain;
    }

    public boolean getHeat() {
        return heat;
    }

    public void setHeat(boolean heat) {
        this.heat = heat;
    }

    public boolean getHaze() {
        return haze;
    }

    public void setHaze(boolean haze) {
        this.haze = haze;
    }

    public int getRainLeadMinutes() {
        return rainLeadMinutes;
    }

    public void setRainLeadMinutes(int rainLeadMinutes) {
        this.rainLeadMinutes = rainLeadMinutes;
    }

    public String getQuietStart() {
        return quietStart;
    }

    public void setQuietStart(String quietStart) {
        this.quietStart = quietStart;
    }

    public String getQuietEnd() {
        return quietEnd;
    }

    public void setQuietEnd(String quietEnd) {
        this.quietEnd = quietEnd;
    }
}
