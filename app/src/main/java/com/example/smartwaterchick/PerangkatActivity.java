package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PerangkatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perangkat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Back
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Notifikasi & Settings
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show());

        // Switch Siklus Penyaringan
        SwitchCompat switchSiklus = findViewById(R.id.switchSiklus);
        switchSiklus.setOnCheckedChangeListener((btn, c) ->
                Toast.makeText(this, "Siklus penyaringan " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show());

        // Switch Injeksi pH
        SwitchCompat switchInjeksi = findViewById(R.id.switchInjeksi);
        switchInjeksi.setOnCheckedChangeListener((btn, c) ->
                Toast.makeText(this, "Injeksi penetral " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show());

        // Lihat Semua log
        findViewById(R.id.tvLihatSemua).setOnClickListener(v ->
                Toast.makeText(this, "Membuka log sistem...", Toast.LENGTH_SHORT).show());

        // Setup konsumsi chart dengan data naik
        AnalisisChartView chartKonsumsi = findViewById(R.id.chartKonsumsi);
        float[] konsumsiData = {10f, 12f, 11f, 15f, 18f, 22f, 28f, 35f, 40f, 38f, 32f, 45f};
        chartKonsumsi.setData(konsumsiData, android.graphics.Color.parseColor("#1B5BCE"));

        // Bottom Navigation - tab Devices aktif
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_devices);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_devices) {
                startActivity(new Intent(this, PerangkatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}