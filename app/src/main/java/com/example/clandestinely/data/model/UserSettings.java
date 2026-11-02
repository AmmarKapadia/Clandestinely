package com.example.clandestinely.data.model;

/** users/{uid}.settings — language and text size (FR6.3). */
public class UserSettings {

    private String language = "en";     // "en" or "ms"
    private String textSize = "normal"; // "normal" or "large"

    public UserSettings() {
    }

    public UserSettings(String language, String textSize) {
        this.language = language;
        this.textSize = textSize;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTextSize() {
        return textSize;
    }

    public void setTextSize(String textSize) {
        this.textSize = textSize;
    }
}
