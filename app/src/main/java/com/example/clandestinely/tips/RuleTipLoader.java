package com.example.clandestinely.tips;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the rule-based tip library from app/src/main/assets/rule_tips_v1.json.
 * The file is inside the app, so it works offline (FR3.3, NFR2).
 */
public final class RuleTipLoader {

    public static final String ASSET_FILE = "rule_tips_v1.json";

    private static volatile RuleTipLibrary cached;

    private RuleTipLoader() {
    }

    /** The library, loaded once and then kept in memory. */
    public static RuleTipLibrary get(Context context) {
        RuleTipLibrary library = cached;
        if (library == null) {
            synchronized (RuleTipLoader.class) {
                library = cached;
                if (library == null) {
                    library = load(context.getApplicationContext());
                    cached = library;
                }
            }
        }
        return library;
    }

    private static RuleTipLibrary load(Context context) {
        try (InputStream in = context.getAssets().open(ASSET_FILE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            throw new IllegalStateException("Couldn't read " + ASSET_FILE + ": " + e.getMessage(), e);
        }
    }

    static RuleTipLibrary parse(String json) throws JSONException {
        JSONArray array = new JSONObject(json).getJSONArray("tips");
        List<RuleTip> tips = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            List<String> slots = new ArrayList<>();
            JSONArray slotArray = o.optJSONArray("timeSlots");
            if (slotArray != null) {
                for (int k = 0; k < slotArray.length(); k++) slots.add(slotArray.getString(k));
            }
            String sensitivity = o.isNull("sensitivity") ? null : o.getString("sensitivity");
            tips.add(new RuleTip(
                    o.getString("id"),
                    o.getString("state"),
                    sensitivity,
                    slots,
                    o.optInt("priority", 2),
                    o.getString("en"),
                    o.optString("ms", ""),
                    o.optString("source", "")));
        }
        return new RuleTipLibrary(tips);
    }
}
