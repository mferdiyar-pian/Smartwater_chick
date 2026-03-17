package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PengaturanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Edit profil
        findViewById(R.id.btnEdit).setOnClickListener(v ->
                Toast.makeText(this, "Edit profil...", Toast.LENGTH_SHORT).show());

        // ── MENU UTAMA ──
        findViewById(R.id.menuNotifikasi).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan Notifikasi", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuManajemen).setOnClickListener(v ->
                Toast.makeText(this, "Manajemen Perangkat", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuKeamanan).setOnClickListener(v ->
                Toast.makeText(this, "Keamanan", Toast.LENGTH_SHORT).show());

        // ── MENU UMUM ──
        findViewById(R.id.menuBackup).setOnClickListener(v ->
                Toast.makeText(this, "Backup Data...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuBahasa).setOnClickListener(v -> {
            String[] bahasa = {"Indonesia", "English"};
            new AlertDialog.Builder(this)
                    .setTitle("Pilih Bahasa")
                    .setItems(bahasa, (dialog, which) ->
                            Toast.makeText(this, "Bahasa: " + bahasa[which], Toast.LENGTH_SHORT).show())
                    .show();
        });

        findViewById(R.id.menuBantuan).setOnClickListener(v ->
                Toast.makeText(this, "Pusat Bantuan", Toast.LENGTH_SHORT).show());

        // ── KELUAR SESI ──
        findViewById(R.id.btnKeluarSesi).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Keluar Sesi")
                        .setMessage("Apakah Anda yakin ingin keluar?")
                        .setPositiveButton("Keluar", (dialog, which) -> {
                            // Navigasi ke LoginActivity
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Batal", null)
                        .show());

        // Bottom Navigation - tab Pengaturan aktif
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
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