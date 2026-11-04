package com.example.clandestinely.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.clandestinely.data.model.Schema;
import com.example.clandestinely.data.model.TimeUtil;
import com.example.clandestinely.data.model.TipCacheEntry;
import com.example.clandestinely.data.model.TipCacheKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * tips_cache (M3 Smart Tips, FR3.1–3.3). Shared by all users; holds no personal data.
 *
 * How the Tips screen uses it (schema section 5.4):
 * 1. key = TipCacheKey.build(states, TipCacheKey.timeSlotAt(now), TipCacheKey.sensitivitiesOf(health))
 * 2. getFreshTip(key) -> if the result isn't null, show it
 * 3. otherwise ask Gemini (Firebase AI Logic), show the answer, then saveTip(...)
 * 4. if Gemini fails or the phone is offline -> use the rule-based tip library in the app
 */
public class TipCacheRepository {

    private final CollectionReference tips;

    public TipCacheRepository() {
        this(FirestoreDb.get());
    }

    public TipCacheRepository(FirebaseFirestore db) {
        this.tips = db.collection(Schema.TIPS_CACHE);
    }

    /**
     * The saved tip for {@code key} if it exists and hasn't expired, otherwise null.
     * Also null (never a failure) if Firestore can't be reached, so the caller can
     * move straight on to Gemini or the rule library.
     */
    public Task<TipCacheEntry> getFreshTip(@NonNull String key) {
        return tips.document(key).get().continueWith(task -> {
            if (!task.isSuccessful()) return null;
            DocumentSnapshot snapshot = task.getResult();
            TipCacheEntry entry = snapshot.toObject(TipCacheEntry.class);
            if (entry == null || entry.getExpiresAt() == null) return null;
            return TimeUtil.toMillis(entry.getExpiresAt()) > System.currentTimeMillis() ? entry : null;
        });
    }

    /**
     * Saves a tip for 1 hour. The Task's result is the tip's key.
     *
     * @param source Schema.SOURCE_AI or Schema.SOURCE_RULE
     * @param model  e.g. "gemini (Firebase AI Logic)"; null for rule tips
     */
    public Task<String> saveTip(@NonNull Collection<String> states, @NonNull String timeSlot,
                                @NonNull Collection<String> sensitivities, @NonNull String text,
                                @NonNull String source, @Nullable String model) {
        if (!Schema.SOURCE_AI.equals(source) && !Schema.SOURCE_RULE.equals(source)) {
            return Tasks.forException(new IllegalArgumentException("Unknown source: " + source));
        }
        String cleanText = text.trim();
        if (cleanText.length() > 300) cleanText = cleanText.substring(0, 300);
        if (cleanText.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Tip text is empty"));
        }

        final String key;
        try {
            key = TipCacheKey.build(states, timeSlot, sensitivities);
        } catch (IllegalArgumentException e) {
            return Tasks.forException(e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("states", TipCacheKey.normalisedStates(states));
        data.put("timeSlot", timeSlot);
        data.put("sensitivities", TipCacheKey.normalisedSensitivities(sensitivities));
        data.put("text", cleanText);
        data.put("source", source);
        data.put("model", model);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("expiresAt", TimeUtil.fromMillis(System.currentTimeMillis() + TipCacheKey.CACHE_LIFETIME_MS));

        return tips.document(key).set(data).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return key;
        });
    }
}
