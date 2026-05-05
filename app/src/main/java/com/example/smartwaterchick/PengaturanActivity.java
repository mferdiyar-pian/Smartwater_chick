package com.example.smartwaterchick;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class PengaturanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Load profile
        SharedPreferences prefs = getSharedPreferences("SmartWaterProfile", Context.MODE_PRIVATE);
        TextView tvNama = findViewById(R.id.tvNama);
        TextView tvRole = findViewById(R.id.tvRole);
        
        tvNama.setText(prefs.getString("nama", "Paimin"));
        tvRole.setText(prefs.getString("role", "CEO Peternakan Ayam"));

        // Edit profil
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profil, null);
            EditText etNama = dialogView.findViewById(R.id.etNama);
            EditText etRole = dialogView.findViewById(R.id.etRole);
            Button btnBatal = dialogView.findViewById(R.id.btnBatal);
            Button btnSimpan = dialogView.findViewById(R.id.btnSimpan);

            etNama.setText(tvNama.getText().toString());
            etRole.setText(tvRole.getText().toString());

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();
                    
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            btnBatal.setOnClickListener(v1 -> dialog.dismiss());
            btnSimpan.setOnClickListener(v12 -> {
                String newNama = etNama.getText().toString().trim();
                String newRole = etRole.getText().toString().trim();

                if (newNama.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nama", newNama);
                editor.putString("role", newRole);
                editor.apply();

                tvNama.setText(newNama);
                tvRole.setText(newRole);

                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });

        // ── MENU UTAMA ──
        findViewById(R.id.menuNotifikasi).setOnClickListener(v -> {
            startActivity(new Intent(this, PengaturanNotifikasiActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Setup WiFi ESP32 via BLE
        findViewById(R.id.menuManajemen).setOnClickListener(v -> {
            startActivity(new Intent(this, BleWifiSetupActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

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
                            // Sign out dari Firebase — sesi dihapus
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        })
                        .setNegativeButton("Batal", null)
                        .show());

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
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
                startActivity(new Intent(this, PerangkatActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }
}