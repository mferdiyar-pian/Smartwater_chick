package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class KontrolActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Back button
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Notifikasi & Settings
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show());

        // Progress bar tangki 65%
        setProgressBar(R.id.viewProgressTangki, 0.65f);
        setProgressBar(R.id.viewProgressKontrol, 0.65f);

        // Cek pH
        findViewById(R.id.btnCekPh).setOnClickListener(v ->
                Toast.makeText(this, "Mengecek pH...", Toast.LENGTH_SHORT).show());

        // Isi Air
        findViewById(R.id.btnIsiAir).setOnClickListener(v ->
                Toast.makeText(this, "Mengisi air tangki...", Toast.LENGTH_SHORT).show());

        // Buang Air
        findViewById(R.id.btnBuangAir).setOnClickListener(v ->
                Toast.makeText(this, "Membuang air tangki...", Toast.LENGTH_SHORT).show());

        // Switch Otomatis
        SwitchCompat switchOtomatis = findViewById(R.id.switchOtomatis);
        switchOtomatis.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, checked ? "Otomatis aktif" : "Otomatis nonaktif", Toast.LENGTH_SHORT).show());

        // Switch Jadwal
        SwitchCompat sw1 = findViewById(R.id.switchJadwal1);
        SwitchCompat sw2 = findViewById(R.id.switchJadwal2);
        SwitchCompat sw3 = findViewById(R.id.switchJadwal3);

        sw1.setOnCheckedChangeListener((btn, c) ->
                Toast.makeText(this, "Jadwal 07.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show());
        sw2.setOnCheckedChangeListener((btn, c) ->
                Toast.makeText(this, "Jadwal 15.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show());
        sw3.setOnCheckedChangeListener((btn, c) ->
                Toast.makeText(this, "Jadwal 22.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show());

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_controls);
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

    private void setProgressBar(int viewId, float fraction) {
        View progressView = findViewById(viewId);
        progressView.post(() -> {
            int totalWidth = ((View) progressView.getParent()).getWidth();
            ViewGroup.LayoutParams params = progressView.getLayoutParams();
            params.width = (int) (totalWidth * fraction);
            progressView.setLayoutParams(params);
        });
    }
}