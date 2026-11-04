package com.example.clandestinely.tips;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.clandestinely.data.model.Schema;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Checks the real tip file (src/main/assets/rule_tips_v1.json).
 * Needs this line in app/build.gradle.kts dependencies:
 *     testImplementation("org.json:json:20240303")
 */
public class TipLibraryFileTest {

    private static RuleTipLibrary load() throws Exception {
        // Unit tests run from the app/ folder, so this is app/src/main/assets/rule_tips_v1.json
        try (InputStream in = new FileInputStream("src/main/assets/" + RuleTipLoader.ASSET_FILE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return RuleTipLoader.parse(new String(out.toByteArray(), "UTF-8"));
        }
    }

    @Test
    public void hasAtLeast20Tips() throws Exception {
        assertTrue(load().getAll().size() >= 20);
    }

    @Test
    public void everyWeatherStateHasATipForEveryoneAtAnyTime() throws Exception {
        RuleTipLibrary library = load();
        for (String state : Schema.WEATHER_STATES) {
            for (String slot : Arrays.asList("morning", "afternoon", "evening", "night")) {
                List<RuleTip> tips = library.select(Arrays.asList(state), Arrays.<String>asList(), slot, 3, 0);
                assertFalse("No tip for " + state + " at " + slot, tips.isEmpty());
            }
        }
    }

    @Test
    public void everySensitivityHasAtLeastOneTip() throws Exception {
        Set<String> covered = new HashSet<>();
        for (RuleTip tip : load().getAll()) {
            if (tip.getSensitivity() != null) covered.add(tip.getSensitivity());
        }
        assertTrue(covered.containsAll(Schema.SENSITIVITIES));
    }

    @Test
    public void tipsAreValid() throws Exception {
        Set<String> ids = new HashSet<>();
        for (RuleTip tip : load().getAll()) {
            assertTrue("Duplicate id " + tip.getId(), ids.add(tip.getId()));
            assertTrue("Unknown state in " + tip.getId(), Schema.WEATHER_STATES.contains(tip.getState()));
            if (tip.getSensitivity() != null) {
                assertTrue("Unknown sensitivity in " + tip.getId(), Schema.SENSITIVITIES.contains(tip.getSensitivity()));
            }
            assertNotNull(tip.text("en"));
            assertTrue("Tip too long for the Home card: " + tip.getId(), tip.text("en").length() <= 160);
            assertTrue("Missing BM text: " + tip.getId(), !tip.text("ms").equals(tip.text("en")));
        }
    }
}
