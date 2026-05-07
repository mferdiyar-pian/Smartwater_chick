package com.example.smartwaterchick;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class KontrolActivity extends AppCompatActivity {

    private DatabaseReference dbKontrol;
    private DatabaseReference dbJadwal;

    private RecyclerView rvJadwal;
    private JadwalAdapter jadwalAdapter;
    private final List<JadwalItem> jadwalList = new ArrayList<>();

    private SwitchCompat switchOtomatis;
    private boolean isOtomatisAktif = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inisialisasi Firebase
        dbKontrol = FirebaseDatabase.getInstance().getReference("kontrol");
        dbJadwal = FirebaseDatabase.getInstance().getReference("jadwal");

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

        // ── Cek pH ──
        findViewById(R.id.btnCekPh).setOnClickListener(v -> {
            dbKontrol.getParent().child("monitoring").limitToLast(1)
                    .get().addOnSuccessListener(snapshot -> {
                        for (var entry : snapshot.getChildren()) {
                            Object phVal = entry.child("ph").getValue();
                            String ph = phVal != null ? String.valueOf(phVal) : "N/A";
                            Toast.makeText(this, "pH terkini: " + ph, Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Gagal membaca pH dari Firebase", Toast.LENGTH_SHORT).show());
        });

        // ── Isi Air Manual ──
        findViewById(R.id.btnIsiAir).setOnClickListener(v ->
                dbKontrol.child("perintah").setValue("isi_air")
                        .addOnSuccessListener(u -> Toast.makeText(this, "Perintah isi air dikirim!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim perintah", Toast.LENGTH_SHORT).show()));

        // ── Buang Air Manual ──
        findViewById(R.id.btnBuangAir).setOnClickListener(v ->
                dbKontrol.child("perintah").setValue("buang_air")
                        .addOnSuccessListener(u -> Toast.makeText(this, "Perintah buang air dikirim!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengirim perintah", Toast.LENGTH_SHORT).show()));

        // ── Switch Otomatis ──
        switchOtomatis = findViewById(R.id.switchOtomatis);
        switchOtomatis.setOnCheckedChangeListener((btn, checked) -> {
            isOtomatisAktif = checked;
            dbKontrol.child("otomatis").setValue(checked);
            Toast.makeText(this, checked ? "Mode otomatis aktif" : "Mode otomatis nonaktif", Toast.LENGTH_SHORT).show();

            // Jika dimatikan, matikan semua jadwal sekaligus
            if (!checked) {
                matikanSemuaJadwal();
            }
        });

        // ── RecyclerView Jadwal ──
        rvJadwal = findViewById(R.id.rvJadwal);
        rvJadwal.setLayoutManager(new LinearLayoutManager(this));
        jadwalAdapter = new JadwalAdapter(jadwalList, new JadwalAdapter.OnJadwalActionListener() {
            @Override
            public void onToggle(String id, boolean aktif) {
                dbJadwal.child(id).child("aktif").setValue(aktif);
            }

            @Override
            public void onEdit(JadwalItem item) {
                tampilkanDialogJadwal(item);
            }

            @Override
            public void onHapus(String id) {
                new AlertDialog.Builder(KontrolActivity.this)
                        .setTitle("Hapus Jadwal")
                        .setMessage("Yakin ingin menghapus jadwal ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbJadwal.child(id).removeValue();
                            Toast.makeText(KontrolActivity.this, "Jadwal dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
        });
        rvJadwal.setAdapter(jadwalAdapter);

        // ── Listener realtime dari Firebase untuk daftar jadwal ──
        dbJadwal.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jadwalList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    Integer jam = child.child("jam").getValue(Integer.class);
                    Integer menit = child.child("menit").getValue(Integer.class);
                    Boolean aktif = child.child("aktif").getValue(Boolean.class);

                    if (jam != null && menit != null && aktif != null) {
                        jadwalList.add(new JadwalItem(id, jam, menit, aktif));
                    }
                }
                // Urutkan berdasarkan jam dan menit
                Collections.sort(jadwalList, (a, b) -> {
                    if (a.jam != b.jam) return Integer.compare(a.jam, b.jam);
                    return Integer.compare(a.menit, b.menit);
                });
                jadwalAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(KontrolActivity.this, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Tombol Tambah Jadwal ──
        MaterialButton btnTambahJadwal = findViewById(R.id.btnTambahJadwal);
        btnTambahJadwal.setOnClickListener(v -> tampilkanDialogJadwal(null));

        // ── Bottom Navigation ──
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

    // ──────────────────────────────────────────
    // Dialog Tambah / Edit Jadwal
    // item == null → Tambah baru
    // item != null → Edit yang sudah ada
    // ──────────────────────────────────────────
    private void tampilkanDialogJadwal(JadwalItem item) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_tambah_jadwal, null);

        TextView tvJudul = dialogView.findViewById(R.id.tvDialogJudulJadwal);
        TimePicker timePicker = dialogView.findViewById(R.id.timePickerJadwal);
        timePicker.setIs24HourView(true);

        if (item != null) {
            // Mode edit → isi dengan nilai lama
            tvJudul.setText("Edit Jadwal");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.setHour(item.jam);
                timePicker.setMinute(item.menit);
            } else {
                timePicker.setCurrentHour(item.jam);
                timePicker.setCurrentMinute(item.menit);
            }
        } else {
            tvJudul.setText("Tambah Jadwal");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Tombol Batal
        dialogView.findViewById(R.id.btnBatalJadwal).setOnClickListener(v -> dialog.dismiss());

        // Tombol Simpan
        dialogView.findViewById(R.id.btnSimpanJadwal).setOnClickListener(v -> {
            int jam, menit;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                jam = timePicker.getHour();
                menit = timePicker.getMinute();
            } else {
                jam = timePicker.getCurrentHour();
                menit = timePicker.getCurrentMinute();
            }

            if (item != null) {
                // Update jadwal yang ada
                dbJadwal.child(item.id).child("jam").setValue(jam);
                dbJadwal.child(item.id).child("menit").setValue(menit);
                Toast.makeText(this, "Jadwal diperbarui", Toast.LENGTH_SHORT).show();
            } else {
                // Buat jadwal baru dengan ID unik dari Firebase
                String newId = dbJadwal.push().getKey();
                if (newId != null) {
                    DatabaseReference newRef = dbJadwal.child(newId);
                    newRef.child("jam").setValue(jam);
                    newRef.child("menit").setValue(menit);
                    newRef.child("aktif").setValue(true);
                    Toast.makeText(this, "Jadwal ditambahkan", Toast.LENGTH_SHORT).show();
                }
            }
            dialog.dismiss();
        });
    }

    // ──────────────────────────────────────────
    // Matikan semua jadwal saat switch Otomatis dimatikan
    // ──────────────────────────────────────────
    private void matikanSemuaJadwal() {
        for (JadwalItem j : jadwalList) {
            dbJadwal.child(j.id).child("aktif").setValue(false);
        }
        // Update UI lokal langsung tanpa menunggu Firebase
        for (JadwalItem j : jadwalList) {
            j.aktif = false;
        }
        jadwalAdapter.notifyDataSetChanged();
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