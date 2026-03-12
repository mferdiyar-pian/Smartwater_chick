package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

        // Cek pH
        findViewById(R.id.btnCekUlang).setOnClickListener(v ->
                Toast.makeText(this, "Mengecek pH...", Toast.LENGTH_SHORT).show());

        // Laporan Lengkap
        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v ->
                Toast.makeText(this, "Membuka laporan lengkap...", Toast.LENGTH_SHORT).show());

        // Progress bar kapasitas 65%
        View viewProgress = findViewById(R.id.viewKapasitasProgress);
        viewProgress.post(() -> {
            int totalWidth = ((View) viewProgress.getParent()).getWidth();
            ViewGroup.LayoutParams params = viewProgress.getLayoutParams();
            params.width = (int) (totalWidth * 0.65f);
            viewProgress.setLayoutParams(params);
        });

        // Isi Air & Buang Air
        findViewById(R.id.btnIsiAir).setOnClickListener(v ->
                Toast.makeText(this, "Mengisi air tanki...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnBuangAir).setOnClickListener(v ->
                Toast.makeText(this, "Membuang air tanki...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnIsiAirAyam).setOnClickListener(v ->
                Toast.makeText(this, "Mengisi tempat minum ayam...", Toast.LENGTH_SHORT).show());

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