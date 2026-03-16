package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AnalisisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Tombol Back
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Notifikasi & Settings
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show());

        // Laporan Lengkap
        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v ->
                Toast.makeText(this, "Membuka laporan lengkap...", Toast.LENGTH_SHORT).show());

        // Bottom Navigation - tab Analytics aktif
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analytics);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                return true;
            } else if (id == R.id.nav_controls) {
                Toast.makeText(this, "Controls", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_devices) {
                Toast.makeText(this, "Devices", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}