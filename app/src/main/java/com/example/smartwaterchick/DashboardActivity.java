package com.example.smartwaterchick;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Notification icon
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show()
        );

        // Settings icon
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show()
        );

        // Ganti air button
        findViewById(R.id.btnGantiAir).setOnClickListener(v ->
                Toast.makeText(this, "Mengganti air...", Toast.LENGTH_SHORT).show()
        );

        // Abaikan button
        findViewById(R.id.btnAbaikan).setOnClickListener(v ->
                findViewById(R.id.btnAbaikan).setVisibility(android.view.View.GONE)
        );

        // Lihat detail
        findViewById(R.id.tvLihatDetail).setOnClickListener(v ->
                Toast.makeText(this, "Lihat riwayat lengkap", Toast.LENGTH_SHORT).show()
        );

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_beranda);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
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
