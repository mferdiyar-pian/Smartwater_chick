package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class KontrolActivity extends AppCompatActivity {

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inisialisasi Firebase Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference("kontrol");

        // Back button
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            Intent intent = new Intent(KontrolActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Notifikasi
        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(KontrolActivity.this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        // Settings
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(KontrolActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Progress bar tangki 65%
        setProgressBar(R.id.viewProgressTangki, 0.65f);
        setProgressBar(R.id.viewProgressKontrol, 0.65f);

        // Cek pH — baca nilai pH terbaru dari Firebase
        findViewById(R.id.btnCekPh).setOnClickListener(v -> {
            dbRef.getParent().child("monitoring").limitToLast(1)
                    .get().addOnSuccessListener(snapshot -> {
                        for (var entry : snapshot.getChildren()) {
                            Object phVal = entry.child("ph").getValue();
                            String ph = phVal != null ? String.valueOf(phVal) : "N/A";
                            Toast.makeText(this, "pH terkini: " + ph, Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Gagal membaca pH dari Firebase", Toast.LENGTH_SHORT).show());
        });

        // Isi Air — kirim perintah ke Firebase
        findViewById(R.id.btnIsiAir).setOnClickListener(v -> {
            dbRef.child("perintah").setValue("isi_air")
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Perintah isi air dikirim!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim perintah", Toast.LENGTH_SHORT).show());
        });

        // Buang Air — kirim perintah ke Firebase
        findViewById(R.id.btnBuangAir).setOnClickListener(v -> {
            dbRef.child("perintah").setValue("buang_air")
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Perintah buang air dikirim!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim perintah", Toast.LENGTH_SHORT).show());
        });

        // Switch Otomatis — simpan ke Firebase
        SwitchCompat switchOtomatis = findViewById(R.id.switchOtomatis);
        switchOtomatis.setOnCheckedChangeListener((btn, checked) -> {
            dbRef.child("otomatis").setValue(checked);
            Toast.makeText(this, checked ? "Mode otomatis aktif" : "Mode otomatis nonaktif", Toast.LENGTH_SHORT).show();
        });

        // Switch Jadwal — simpan ke Firebase
        SwitchCompat sw1 = findViewById(R.id.switchJadwal1);
        SwitchCompat sw2 = findViewById(R.id.switchJadwal2);
        SwitchCompat sw3 = findViewById(R.id.switchJadwal3);

        sw1.setOnCheckedChangeListener((btn, c) -> {
            dbRef.child("jadwal_07").setValue(c);
            Toast.makeText(this, "Jadwal 07.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show();
        });
        sw2.setOnCheckedChangeListener((btn, c) -> {
            dbRef.child("jadwal_15").setValue(c);
            Toast.makeText(this, "Jadwal 15.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show();
        });
        sw3.setOnCheckedChangeListener((btn, c) -> {
            dbRef.child("jadwal_22").setValue(c);
            Toast.makeText(this, "Jadwal 22.00 " + (c ? "aktif" : "nonaktif"), Toast.LENGTH_SHORT).show();
        });

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_controls);
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
                return true;
            } else if (id == R.id.nav_devices) {
                startActivity(new Intent(this, PerangkatActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
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