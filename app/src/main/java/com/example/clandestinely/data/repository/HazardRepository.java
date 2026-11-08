package com.example.clandestinely.data.repository;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.clandestinely.data.model.HazardExpiry;
import com.example.clandestinely.data.model.HazardReport;
import com.example.clandestinely.data.model.Schema;
import com.example.clandestinely.data.model.TimeUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** hazard_reports (M5 Hazard Reporting, FR5.1–5.3) and hazard photos in Storage. */
public class HazardRepository {

    /** Thrown when a report can't be confirmed. getMessage() is safe to show to the user. */
    public static class ConfirmException extends RuntimeException {
        public ConfirmException(String message) {
            super(message);
        }
    }

    /** Receives the live list of active reports. */
    public interface ReportsListener {
        void onReports(@NonNull List<HazardReport> reports);

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;
    private final CollectionReference reports;

    public HazardRepository() {
        this(FirestoreDb.get());
    }

    public HazardRepository(FirebaseFirestore db) {
        this.db = db;
        this.reports = db.collection(Schema.HAZARD_REPORTS);
    }

    /**
     * FR5.1: saves a new report. The Task's result is the new report's ID.
     *
     * @param nearestEdgeId the closest path segment within 30 m of {@code location} from
     *                      campus_graph.json, or null if no path is close enough (schema 4.2).
     * @param photo         a JPEG picked or taken by the user, or null. Photos need Firebase
     *                      Storage, which needs the Blaze plan; pass null until that's set up.
     */
    public Task<String> submitReport(@NonNull String reporterUid, @NonNull String category,
                                     @NonNull GeoPoint location, @Nullable String nearestEdgeId,
                                     @Nullable String description, @Nullable Uri photo) {
        if (!Schema.HAZARD_CATEGORIES.contains(category)) {
            return Tasks.forException(new IllegalArgumentException("Unknown category: " + category));
        }
        DocumentReference ref = reports.document(); // new ID, used for the photo name too

        Task<String> photoTask;
        if (photo == null) {
            photoTask = Tasks.forResult(null);
        } else {
            String path = Schema.HAZARD_PHOTOS_FOLDER + "/" + ref.getId() + ".jpg";
            StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(path);
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
            photoTask = photoRef.putFile(photo, metadata).continueWith(upload -> {
                if (!upload.isSuccessful()) throw upload.getException();
                return path;
            });
        }

        return photoTask.continueWithTask(uploaded -> {
            if (!uploaded.isSuccessful()) throw uploaded.getException();
            Map<String, Object> data = new HashMap<>();
            data.put("category", category);
            data.put("description", cleanDescription(description));
            data.put("photoPath", uploaded.getResult());
            data.put("location", location);
            data.put("nearestEdgeId", nearestEdgeId);
            data.put("reportedBy", reporterUid);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("expiresAt", TimeUtil.fromMillis(HazardExpiry.initialExpiry(System.currentTimeMillis())));
            data.put("confirmCount", 0);
            return ref.set(data);
        }).continueWith(saved -> {
            if (!saved.isSuccessful()) throw saved.getException();
            return ref.getId();
        });
    }

    /**
     * FR5.2: starts listening to reports that haven't expired, soonest-to-expire first.
     * Call {@code remove()} on the result when the screen closes (e.g. in onStop).
     *
     * The list updates whenever a report is added, confirmed or deleted. A report that simply
     * runs out of time stays in the last list, so re-check {@link HazardReport#isActiveAt}
     * about once a minute (or use {@link HazardReport#blockedEdgeIds}, which filters by time).
     */
    public ListenerRegistration listenToActiveReports(@NonNull ReportsListener listener) {
        return reports
                .whereGreaterThan("expiresAt", Timestamp.now())
                .orderBy("expiresAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    long now = System.currentTimeMillis();
                    List<HazardReport> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            HazardReport report = doc.toObject(HazardReport.class);
                            if (report != null && report.isActiveAt(now)) list.add(report);
                        }
                    }
                    listener.onReports(list);
                });
    }

    /**
     * FR5.3: confirms someone else's report once, adding 1 hour (capped at 10 hours and midnight).
     * If it fails, use {@link #friendlyMessage} on the exception to show the reason.
     */
    public Task<Void> confirmReport(@NonNull String reportId, @NonNull String uid) {
        DocumentReference reportRef = reports.document(reportId);
        DocumentReference confirmationRef = reportRef.collection(Schema.CONFIRMATIONS).document(uid);

        return db.runTransaction(tx -> {
            DocumentSnapshot report = tx.get(reportRef);
            boolean alreadyConfirmed = tx.get(confirmationRef).exists();

            if (!report.exists()) throw new ConfirmException("This report no longer exists.");
            Timestamp createdAt = report.getTimestamp("createdAt");
            Timestamp expiresAt = report.getTimestamp("expiresAt");
            if (createdAt == null || expiresAt == null) {
                throw new ConfirmException("This report is still being saved. Try again in a moment.");
            }
            if (uid.equals(report.getString("reportedBy"))) {
                throw new ConfirmException("You can't confirm your own report.");
            }
            if (TimeUtil.toMillis(expiresAt) <= System.currentTimeMillis()) {
                throw new ConfirmException("This report has already expired.");
            }
            if (alreadyConfirmed) throw new ConfirmException("You've already confirmed this report.");

            long newExpiry = HazardExpiry.extendedExpiry(TimeUtil.toMillis(createdAt), TimeUtil.toMillis(expiresAt));
            Map<String, Object> changes = new HashMap<>();
            changes.put("confirmCount", FieldValue.increment(1));
            changes.put("expiresAt", TimeUtil.fromMillis(newExpiry));
            tx.update(reportRef, changes);

            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("confirmedAt", FieldValue.serverTimestamp());
            tx.set(confirmationRef, confirmation);
            return null;
        });
    }

    /** Only the person who made the report can delete it. */
    public Task<Void> deleteReport(@NonNull String reportId) {
        return reports.document(reportId).delete();
    }

    /** Download URL for a report photo, for showing it with an image loader such as Glide. */
    public Task<Uri> photoUrl(@NonNull String photoPath) {
        return FirebaseStorage.getInstance().getReference().child(photoPath).getDownloadUrl();
    }

    /** A message that's safe to show the user for a failed confirmReport(). */
    public static String friendlyMessage(@Nullable Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConfirmException) return t.getMessage();
            t = t.getCause();
        }
        return "Couldn't confirm the report. Check your connection and try again.";
    }

    @Nullable
    private static String cleanDescription(@Nullable String description) {
        if (description == null) return null;
        String trimmed = description.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
