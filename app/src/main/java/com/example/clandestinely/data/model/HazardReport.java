package com.example.clandestinely.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** hazard_reports/{reportId} — one reported hazard (FR5.1–5.3). */
public class HazardReport {

    private String id = "";
    private String category = Schema.CATEGORY_OTHER;
    private String description;
    private String photoPath;
    private GeoPoint location;
    private String nearestEdgeId;
    private String reportedBy = "";
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private int confirmCount;

    /** Firestore needs an empty constructor. */
    public HazardReport() {
    }

    /** True if the report should still show on the map at {@code nowMillis}. */
    @Exclude
    public boolean isActiveAt(long nowMillis) {
        return expiresAt != null && TimeUtil.toMillis(expiresAt) > nowMillis;
    }

    /** Path segments routing should avoid (FR4.3). */
    public static Set<String> blockedEdgeIds(List<HazardReport> reports, long nowMillis) {
        Set<String> edges = new HashSet<>();
        for (HazardReport report : reports) {
            if (report.isActiveAt(nowMillis) && report.getNearestEdgeId() != null) {
                edges.add(report.getNearestEdgeId());
            }
        }
        return edges;
    }

    @DocumentId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getNearestEdgeId() {
        return nearestEdgeId;
    }

    public void setNearestEdgeId(String nearestEdgeId) {
        this.nearestEdgeId = nearestEdgeId;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
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

    public int getConfirmCount() {
        return confirmCount;
    }

    public void setConfirmCount(int confirmCount) {
        this.confirmCount = confirmCount;
    }
}
