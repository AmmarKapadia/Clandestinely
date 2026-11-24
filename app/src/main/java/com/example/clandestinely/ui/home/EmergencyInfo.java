package com.example.clandestinely.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.clandestinely.R;

public class EmergencyInfo {

    //M7: Emergency Info Screen (Static, Offline, Tap To Call)
    public static final String PHONE_UM_SECURITY = "0379677777";
    public static final String PHONE_EMERGENCY_999 = "999";
    public static final String PHONE_AMBULANCE = "0379492898";
    public static final String PHONE_CLINIC = "0379673212";
    public static final String PHONE_BOMBA = "0322824444";
    public static final String PHONE_JPPHB = "0379673999";

    public interface CallCallback {
        void onCallRequested(String phoneNumber);
    }

    private EmergencyInfo() {}

    /** Each tap-to-call view and the number it dials. */
    private static final int[] CALL_VIEW_IDS = {
            R.id.btnCallSecurity, R.id.btnCall999, R.id.cardAmbulance,
            R.id.cardClinic, R.id.cardBomba, R.id.cardJpphb};
    private static final String[] CALL_NUMBERS = {
            PHONE_UM_SECURITY, PHONE_EMERGENCY_999, PHONE_AMBULANCE,
            PHONE_CLINIC, PHONE_BOMBA, PHONE_JPPHB};

    /** Each SOP header and the body it expands or collapses. */
    private static final int[][] SOP_HEADER_BODY_IDS = {
            {R.id.sopHeaderLightning, R.id.sopBodyLightning},
            {R.id.sopHeaderTrees, R.id.sopBodyTrees},
            {R.id.sopHeaderFlood, R.id.sopBodyFlood}};

    public static void setupEmergencyFeatures(Fragment fragment, View rootView, CallCallback callCallback) {
        if (fragment == null || rootView == null) return;

        for (int i = 0; i < CALL_VIEW_IDS.length; i++) {
            View view = rootView.findViewById(CALL_VIEW_IDS[i]);
            String number = CALL_NUMBERS[i];
            if (view != null) {
                view.setOnClickListener(v -> callCallback.onCallRequested(number));
            }
        }

        setupSopToggles(rootView);
    }

    public static void makePhoneCall(Fragment fragment, ActivityResultLauncher<String> permissionLauncher, String phoneNumber) {
        if (fragment == null || !fragment.isAdded()) return;
        Context context = fragment.requireContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CALL_PHONE);
        } else {
            startCallIntent(fragment, phoneNumber);
        }
    }

    public static void startCallIntent(Fragment fragment, String phoneNumber) {
        if (fragment == null || !fragment.isAdded() || phoneNumber == null) return;
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            fragment.startActivity(callIntent);
        } catch (Exception e) {
            startDialIntent(fragment, phoneNumber);
        }
    }

    public static void startDialIntent(Fragment fragment, String phoneNumber) {
        if (fragment == null || !fragment.isAdded() || phoneNumber == null) return;
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        fragment.startActivity(dialIntent);
    }

    private static void setupSopToggles(View rootView) {
        for (int[] ids : SOP_HEADER_BODY_IDS) {
            View header = rootView.findViewById(ids[0]);
            View body = rootView.findViewById(ids[1]);
            if (header != null && body != null) {
                header.setOnClickListener(v -> toggleVisibility(body));
            }
        }
    }

    private static void toggleVisibility(View view) {
        view.setVisibility(view.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
}
