package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PerangkatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perangkat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Back
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Notifikasi button
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show());

        // Settings button
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show());

        // Lihat Semua log
        findViewById(R.id.tvLihatSemua).setOnClickListener(v ->
                Toast.makeText(this, "Membuka semua log...", Toast.LENGTH_SHORT).show());

        // Restart Device button
        Button btnRestart = findViewById(R.id.bnRestartDevice);
        btnRestart.setOnClickListener(v -> {
            Toast.makeText(this, "Memulai ulang perangkat...", Toast.LENGTH_LONG).show();
            // Di sini bisa ditambahkan logika restart perangkat
        });

        // Update Firmware button
        Button btnUpdate = findViewById(R.id.btnUpdateFirmware);
        btnUpdate.setOnClickListener(v -> {
            Toast.makeText(this, "Memeriksa update firmware...", Toast.LENGTH_SHORT).show();
            // Di sini bisa ditambahkan logika update firmware
        });

        // Bottom Navigation - tab Devices aktif
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_devices);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_devices) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }
}