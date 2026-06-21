package com.example.smartwaterchick;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SmartWaterProfile";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Selalu gunakan mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.attachBaseContext(newBase);
    }

    /**
     * Validasi dan sanitasi intent yang masuk.
     * Dipanggil secara otomatis saat Activity menerima intent baru.
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        // Validasi: pastikan intent berasal dari package yang sama atau package yang diizinkan
        String pkg = intent.getPackage();
        if (pkg != null && !pkg.equals(getPackageName())) {
            // Intent dari package asing — abaikan extras berbahaya
            setIntent(new Intent(intent.getAction()));
            return;
        }
        setIntent(intent);
    }

    /**
     * Helper: ambil intent yang sudah divalidasi (tidak null-safe crash).
     */
    @NonNull
    protected Intent getValidatedIntent() {
        Intent i = getIntent();
        return (i != null) ? i : new Intent();
    }
}
