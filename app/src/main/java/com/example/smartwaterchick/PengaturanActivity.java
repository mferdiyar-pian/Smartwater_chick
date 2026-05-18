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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PengaturanActivity extends BaseActivity {

    private static final String PREF_PHOTO_PATH = "foto_profil_path";
    private ImageView ivAvatar;
    private SharedPreferences prefs;

    // Launcher untuk membuka galeri
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        savePhotoToInternal(selectedUri);
                    }
                }
            });

    // Launcher untuk minta izin akses galeri
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    bukaGaleri();
                } else {
                    Toast.makeText(this, "Izin akses galeri diperlukan", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        prefs = getSharedPreferences("SmartWaterProfile", Context.MODE_PRIVATE);

        // Load nama & role
        TextView tvNama = findViewById(R.id.tvNama);
        TextView tvRole = findViewById(R.id.tvRole);
        tvNama.setText(prefs.getString("nama", "Paimin"));
        tvRole.setText(prefs.getString("role", "CEO Peternakan Ayam"));

        // Load foto profil jika ada
        ivAvatar = findViewById(R.id.ivAvatar);
        loadSavedPhoto();

        // Setup toggle Dark Mode
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        boolean isDark = prefs.getBoolean(BaseActivity.PREF_DARK_MODE, false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(BaseActivity.PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            // Recreate semua activity agar tema terapply
            recreate();
        });

        // Tap avatar → tampilkan pilihan (Pilih Foto / Hapus Foto)
        findViewById(R.id.frameAvatar).setOnClickListener(v -> tampilkanDialogFoto());

        // Edit profil (nama & role)
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

                prefs.edit()
                        .putString("nama", newNama)
                        .putString("role", newRole)
                        .apply();

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
            Toast.makeText(this, "Manajemen Perangkat dinonaktifkan karena perangkat Anda tidak memerlukan koneksi Bluetooth.", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.menuKeamanan).setOnClickListener(v ->
                Toast.makeText(this, "Keamanan", Toast.LENGTH_SHORT).show());

        // ── MENU UMUM ──
        findViewById(R.id.menuBackup).setOnClickListener(v ->
                Toast.makeText(this, "Backup Data...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuBantuan).setOnClickListener(v ->
                Toast.makeText(this, "Pusat Bantuan", Toast.LENGTH_SHORT).show());

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
                        mintaIzinGaleri();
                    } else {
                        hapusFotoProfil();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    /** Minta izin galeri sesuai versi Android */
    private void mintaIzinGaleri() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            bukaGaleri();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    /** Buka Intent picker galeri */
    private void bukaGaleri() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
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
}