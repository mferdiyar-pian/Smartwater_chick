package com.example.smartwaterchick;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PengaturanActivity extends BaseActivity {

    private static final String PREF_PHOTO_PATH = "foto_profil_path";
    private ImageView ivAvatar;
    private SharedPreferences prefs;

    private DatabaseReference dbRef;
    private ValueEventListener statusListener;
    private View viewOnlineDot;
    private TextView tvOnlineStatus;
    private TextView tvStatusAir;
    private TextView tvStatusSistem;

    // Launcher untuk membuka galeri
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    savePhotoToInternal(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        prefs = SecurePrefsHelper.getPrefs(this, "SmartWaterProfile");

        // Load nama & email login
        TextView tvNama = findViewById(R.id.tvNama);
        TextView tvRole = findViewById(R.id.tvRole); // tvRole sekarang digunakan untuk menampilkan email login
        tvNama.setText(prefs.getString("nama", "Paimin"));
        
        String loginEmail = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loginEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        if (loginEmail == null || loginEmail.isEmpty()) {
            loginEmail = prefs.getString("email", "user@mail.com");
        }
        tvRole.setText(loginEmail);

        // Load foto profil jika ada
        ivAvatar = findViewById(R.id.ivAvatar);
        loadSavedPhoto();

        // Inisialisasi online status views & Firebase
        viewOnlineDot = findViewById(R.id.viewOnlineDot);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        tvStatusAir = findViewById(R.id.tvStatusAir);
        tvStatusSistem = findViewById(R.id.tvStatusSistem);
        dbRef = FirebaseDatabase.getInstance().getReference();
        startStatusListener();


        // Tap avatar → tampilkan pilihan (Pilih Foto / Hapus Foto)
        findViewById(R.id.frameAvatar).setOnClickListener(v -> tampilkanDialogFoto());

        // Edit profil (nama)
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profil, null);
            EditText etNama = dialogView.findViewById(R.id.etNama);
            EditText etRole = dialogView.findViewById(R.id.etRole); // Field email login
            Button btnBatal = dialogView.findViewById(R.id.btnBatal);
            Button btnSimpan = dialogView.findViewById(R.id.btnSimpan);

            etNama.setText(tvNama.getText().toString());
            
            // Tampilkan email login, nonaktifkan pengeditan
            String currentEmail = "";
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
            if (currentEmail == null || currentEmail.isEmpty()) {
                currentEmail = tvRole.getText().toString();
            }
            etRole.setText(currentEmail);
            etRole.setEnabled(false);
            etRole.setFocusable(false);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            btnBatal.setOnClickListener(v1 -> dialog.dismiss());
            btnSimpan.setOnClickListener(v12 -> {
                String newNama = etNama.getText().toString().trim();

                if (newNama.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }

                prefs.edit()
                        .putString("nama", newNama)
                        .apply();

                tvNama.setText(newNama);

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

        findViewById(R.id.menuKeamanan).setOnClickListener(v -> {
            startActivity(new Intent(this, KeamananActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ── MENU UMUM ──
        findViewById(R.id.menuManajemenData).setOnClickListener(v -> {
            startActivity(new Intent(this, ManajemenDataActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        findViewById(R.id.menuBantuan).setOnClickListener(v -> {
            startActivity(new Intent(this, PusatBantuanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ── KELUAR SESI ──
        findViewById(R.id.btnKeluarSesi).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Keluar Sesi")
                        .setMessage("Apakah Anda yakin ingin keluar?")
                        .setPositiveButton("Keluar", (dialog, which) -> {
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
                Intent i = new Intent(this, DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
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

    // ─── FOTO PROFIL ───────────────────────────────────────────────────────────

    /** Tampilkan dialog pilihan: Pilih Foto / Hapus Foto */
    private void tampilkanDialogFoto() {
        boolean adaFoto = prefs.getString(PREF_PHOTO_PATH, null) != null;
        String[] opsi = adaFoto
                ? new String[]{"Pilih Foto dari Galeri", "Hapus Foto Profil"}
                : new String[]{"Pilih Foto dari Galeri"};

        new AlertDialog.Builder(this)
                .setTitle("Foto Profil")
                .setItems(opsi, (dialog, which) -> {
                    if (which == 0) {
                        bukaGaleri();
                    } else {
                        hapusFotoProfil();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    /** Buka Intent picker galeri */
    private void bukaGaleri() {
        pickImageLauncher.launch("image/*");
    }

    /** Simpan foto dari URI ke penyimpanan internal aplikasi */
    private void savePhotoToInternal(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            File file = new File(getFilesDir(), "foto_profil.jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();

            // Simpan path & tampilkan
            prefs.edit().putString(PREF_PHOTO_PATH, file.getAbsolutePath()).apply();
            loadSavedPhoto();
            Toast.makeText(this, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Gagal menyimpan foto", Toast.LENGTH_SHORT).show();
        }
    }

    /** Load foto yang tersimpan ke ImageView */
    private void loadSavedPhoto() {
        String path = prefs.getString(PREF_PHOTO_PATH, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null) {
                    ivAvatar.setImageBitmap(bmp);
                    return;
                }
            }
            // File tidak ada lagi, bersihkan preferensi
            prefs.edit().remove(PREF_PHOTO_PATH).apply();
        }
        // Tampilkan default avatar
        ivAvatar.setImageResource(R.drawable.ic_avatar_default);
    }

    /** Hapus foto profil dan kembali ke avatar default */
    private void hapusFotoProfil() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Foto Profil")
                .setMessage("Foto profil akan dihapus. Lanjutkan?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    String path = prefs.getString(PREF_PHOTO_PATH, null);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) file.delete();
                        prefs.edit().remove(PREF_PHOTO_PATH).apply();
                    }
                    ivAvatar.setImageResource(R.drawable.ic_avatar_default);
                    Toast.makeText(this, "Foto profil dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void startStatusListener() {
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    setDeviceOffline();
                    return;
                }

                Object phRaw = snapshot.child("ph_terkini").getValue();
                Object literRaw = snapshot.child("kapasitas_liter").getValue();
                Object persenRaw = snapshot.child("kapasitas_persen").getValue();
                Object wifiOnlineRaw = snapshot.child("wifi_online").getValue();
                Object lastSeenRaw = snapshot.child("last_seen").getValue();
                Object sensorPhConnRaw = snapshot.child("sensor_ph_connected").getValue();
                Object sensorUltraConnRaw = snapshot.child("sensor_ultrasonic_connected").getValue();

                // ── Deteksi online/offline: utamakan field wifi_online (boolean langsung dari ESP32)
                boolean isOnline;
                if (wifiOnlineRaw instanceof Boolean) {
                    isOnline = (Boolean) wifiOnlineRaw;
                } else if (lastSeenRaw != null) {
                    long lastSeen = toLong(lastSeenRaw);
                    if (lastSeen > 9999999999L) lastSeen = lastSeen / 1000;
                    long currentEpoch = System.currentTimeMillis() / 1000;
                    long diffUtc  = Math.abs(currentEpoch - lastSeen);
                    long diffWita = Math.abs((currentEpoch + 28800) - lastSeen);
                    isOnline = (diffUtc < 45) || (diffWita < 45);
                } else {
                    isOnline = (phRaw != null) || (literRaw != null) || (persenRaw != null);
                }

                if (isOnline) {
                    setDeviceOnline();

                    // Parse data sensor
                    float ph = (phRaw != null) ? toFloat(phRaw) : 7.0f;
                    float waterPercent = (persenRaw != null) ? toFloat(persenRaw) : 0.0f;

                    // Parse status sensor
                    boolean sensorPhConnected = false;
                    if (sensorPhConnRaw instanceof Boolean) {
                        sensorPhConnected = (Boolean) sensorPhConnRaw;
                    } else {
                        sensorPhConnected = (phRaw != null);
                    }

                    boolean sensorUltraConnected = false;
                    if (sensorUltraConnRaw instanceof Boolean) {
                        sensorUltraConnected = (Boolean) sensorUltraConnRaw;
                    } else {
                        sensorUltraConnected = (persenRaw != null);
                    }

                    // 1. Status Pengisian Air
                    if (tvStatusAir != null) {
                        if (!sensorUltraConnected || waterPercent < 20.0f || waterPercent > 100.0f) {
                            tvStatusAir.setText("Tidak Normal");
                            tvStatusAir.setTextColor(Color.parseColor("#E74C3C")); // Merah
                        } else {
                            tvStatusAir.setText("Normal");
                            tvStatusAir.setTextColor(Color.parseColor("#27AE60")); // Hijau
                        }
                    }

                    // 2. Status Sistem
                    if (tvStatusSistem != null) {
                        if (!sensorPhConnected || !sensorUltraConnected || ph < 6.5f || ph > 7.5f || waterPercent < 20.0f) {
                            tvStatusSistem.setText("Tidak Optimal");
                            tvStatusSistem.setTextColor(Color.parseColor("#E74C3C")); // Merah
                        } else {
                            tvStatusSistem.setText("Optimal");
                            tvStatusSistem.setTextColor(Color.parseColor("#27AE60")); // Hijau (27AE60)
                        }
                    }

                } else {
                    setDeviceOffline();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDeviceOffline();
            }
        };
        dbRef.child("kontrol_status").addValueEventListener(statusListener);
    }

    private void setDeviceOnline() {
        if (tvOnlineStatus != null) {
            tvOnlineStatus.setText("ONLINE");
            tvOnlineStatus.setTextColor(Color.parseColor("#2ECC71"));
        }
        if (viewOnlineDot != null) {
            viewOnlineDot.setBackgroundResource(R.drawable.bg_dot_green);
        }
    }

    private void setDeviceOffline() {
        if (tvOnlineStatus != null) {
            tvOnlineStatus.setText("OFFLINE");
            tvOnlineStatus.setTextColor(Color.parseColor("#E74C3C"));
        }
        if (viewOnlineDot != null) {
            viewOnlineDot.setBackgroundResource(R.drawable.bg_dot_red);
        }
        if (tvStatusAir != null) {
            tvStatusAir.setText("Tidak Normal");
            tvStatusAir.setTextColor(Color.parseColor("#E74C3C"));
        }
        if (tvStatusSistem != null) {
            tvStatusSistem.setText("Tidak Optimal");
            tvStatusSistem.setTextColor(Color.parseColor("#E74C3C"));
        }
    }

    private float toFloat(Object val) {
        if (val instanceof Float) return (Float) val;
        if (val instanceof Double) return ((Double) val).floatValue();
        if (val instanceof Integer) return ((Integer) val).floatValue();
        if (val instanceof Long) return ((Long) val).floatValue();
        try {
            return Float.parseFloat(val.toString());
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private long toLong(Object val) {
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Double)  return ((Double) val).longValue();
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Float)   return ((Float) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null && dbRef != null) {
            dbRef.child("kontrol_status").removeEventListener(statusListener);
        }
    }
}