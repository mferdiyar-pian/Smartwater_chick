package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;

/**
 * Helper untuk memvalidasi intent extras secara aman.
 * Mengatasi temuan "Unvalidated Intent Extras" pada security scanner.
 * Semua nilai yang diterima dari intent divalidasi sebelum digunakan.
 */
public final class IntentValidator {

    private IntentValidator() {}

    /**
     * Ambil String extra secara aman dengan validasi null dan panjang maksimum.
     * @param intent Intent sumber
     * @param key    Key extra yang dicari
     * @param defaultValue Nilai default jika null atau tidak valid
     * @param maxLength Panjang maksimum yang diizinkan (0 = tidak terbatas)
     * @return Nilai yang sudah divalidasi
     */
    public static String getSafeStringExtra(Intent intent, String key, String defaultValue, int maxLength) {
        if (intent == null || key == null) return defaultValue;
        String value = intent.getStringExtra(key);
        if (value == null) return defaultValue;
        // Trim whitespace dan batasi panjang
        value = value.trim();
        if (maxLength > 0 && value.length() > maxLength) {
            value = value.substring(0, maxLength);
        }
        return value;
    }

    /**
     * Ambil int extra secara aman dengan validasi range.
     */
    public static int getSafeIntExtra(Intent intent, String key, int defaultValue, int min, int max) {
        if (intent == null || key == null) return defaultValue;
        int value = intent.getIntExtra(key, defaultValue);
        if (value < min || value > max) return defaultValue;
        return value;
    }

    /**
     * Ambil boolean extra secara aman.
     */
    public static boolean getSafeBooleanExtra(Intent intent, String key, boolean defaultValue) {
        if (intent == null || key == null) return defaultValue;
        return intent.getBooleanExtra(key, defaultValue);
    }

    /**
     * Validasi Bundle: pastikan tidak null dan berisi key yang diharapkan.
     */
    public static boolean isBundleValid(Bundle bundle, String... requiredKeys) {
        if (bundle == null) return false;
        for (String key : requiredKeys) {
            if (!bundle.containsKey(key)) return false;
        }
        return true;
    }
}
