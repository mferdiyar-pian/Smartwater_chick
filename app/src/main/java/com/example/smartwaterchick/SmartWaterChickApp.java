package com.example.smartwaterchick;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Application class — dijalankan SATU KALI saat app pertama kali dibuka.
 * Letakkan konfigurasi global di sini, bukan di Activity.
 */
public class SmartWaterChickApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Firebase persistence harus dikonfigurasi SEKALI sebelum
        // Firebase Database digunakan. Jangan taruh di Activity!
        // false = tidak simpan cache di SQLite (data selalu real-time dari server)
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);
    }
}
