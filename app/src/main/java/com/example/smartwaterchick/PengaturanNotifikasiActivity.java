package com.example.smartwaterchick;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class PengaturanNotifikasiActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan_notifikasi);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("SmartWaterNotifPrefs", Context.MODE_PRIVATE);
        
        SwitchCompat switchPush = findViewById(R.id.switchPush);
        SwitchCompat switchCritical = findViewById(R.id.switchCritical);
        SwitchCompat switchWaterLevel = findViewById(R.id.switchWaterLevel);
        SwitchCompat switchWifiStatus = findViewById(R.id.switchWifiStatus);
        SwitchCompat switchRelayStatus = findViewById(R.id.switchRelayStatus);
        SwitchCompat switchSound = findViewById(R.id.switchSound);
        SwitchCompat switchVibrate = findViewById(R.id.switchVibrate);

        // Load saved states
        switchPush.setChecked(prefs.getBoolean("notif_push", true));
        switchCritical.setChecked(prefs.getBoolean("notif_critical", true));
        switchWaterLevel.setChecked(prefs.getBoolean("notif_water_level", true));
        switchWifiStatus.setChecked(prefs.getBoolean("notif_wifi", true));
        switchRelayStatus.setChecked(prefs.getBoolean("notif_relay", true));
        switchSound.setChecked(prefs.getBoolean("notif_sound", true));
        switchVibrate.setChecked(prefs.getBoolean("notif_vibrate", true));

        // Save switch states
        SharedPreferences.Editor editor = prefs.edit();

        switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_push", isChecked).apply();
            String status = isChecked ? "diaktifkan" : "dinonaktifkan";
            Toast.makeText(this, "Notifikasi Push " + status, Toast.LENGTH_SHORT).show();
        });

        switchCritical.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_critical", isChecked).apply();
        });

        switchWaterLevel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_water_level", isChecked).apply();
        });

        switchWifiStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_wifi", isChecked).apply();
        });

        switchRelayStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_relay", isChecked).apply();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_sound", isChecked).apply();
        });

        switchVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notif_vibrate", isChecked).apply();
        });
    }
}

