package com.example.smartwaterchick;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ManajemenDataActivity extends BaseActivity {

    private DatabaseReference dbRef;
    private SharedPreferences sharedPrefs;

    private SwitchCompat switchAutoDelete;
    private TextView tvAutoDeleteStatus;
    private LinearLayout layoutPeriodeAuto;
    private RadioGroup rgPeriodeAuto;
    private RadioButton rb1Hari, rb1Minggu, rb1Bulan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_data);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbRef = FirebaseDatabase.getInstance().getReference();
        sharedPrefs = SecurePrefsHelper.getPrefs(this, "DataManagementPrefs");

        // Bind Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            Intent intent = new Intent(this, PengaturanActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Bind Manual Deletion buttons
        findViewById(R.id.btnHapus1Hari).setOnClickListener(v -> showConfirmDeleteDialog(1, "1 Hari Terakhir"));
        findViewById(R.id.btnHapus1Minggu).setOnClickListener(v -> showConfirmDeleteDialog(7, "1 Minggu Terakhir"));
        findViewById(R.id.btnHapus1Bulan).setOnClickListener(v -> showConfirmDeleteDialog(30, "1 Bulan Terakhir"));

        // Bind Auto-Delete elements
        switchAutoDelete = findViewById(R.id.switchAutoDelete);
        tvAutoDeleteStatus = findViewById(R.id.tvAutoDeleteStatus);
        layoutPeriodeAuto = findViewById(R.id.layoutPeriodeAuto);
        rgPeriodeAuto = findViewById(R.id.rgPeriodeAuto);
        rb1Hari = findViewById(R.id.rb1Hari);
        rb1Minggu = findViewById(R.id.rb1Minggu);
        rb1Bulan = findViewById(R.id.rb1Bulan);

        // Load settings
        boolean isAutoEnabled = sharedPrefs.getBoolean("auto_delete_enabled", false);
        String savedPeriod = sharedPrefs.getString("auto_delete_period", "1 Minggu");

        switchAutoDelete.setChecked(isAutoEnabled);
        tvAutoDeleteStatus.setText(isAutoEnabled ? "Aktif" : "Nonaktif");
        layoutPeriodeAuto.setVisibility(isAutoEnabled ? View.VISIBLE : View.GONE);

        if ("1 Hari".equals(savedPeriod)) {
            rb1Hari.setChecked(true);
        } else if ("1 Minggu".equals(savedPeriod)) {
            rb1Minggu.setChecked(true);
        } else if ("1 Bulan".equals(savedPeriod)) {
            rb1Bulan.setChecked(true);
        }

        // Toggle listener
        switchAutoDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("auto_delete_enabled", isChecked).apply();
            tvAutoDeleteStatus.setText(isChecked ? "Aktif" : "Nonaktif");
            layoutPeriodeAuto.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                // Trigger auto-delete immediately when enabled
                triggerAutoDelete();
            }
        });

        // Radio group listener
        rgPeriodeAuto.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedPeriod = "1 Minggu";
            if (checkedId == R.id.rb1Hari) {
                selectedPeriod = "1 Hari";
            } else if (checkedId == R.id.rb1Minggu) {
                selectedPeriod = "1 Minggu";
            } else if (checkedId == R.id.rb1Bulan) {
                selectedPeriod = "1 Bulan";
            }
            sharedPrefs.edit().putString("auto_delete_period", selectedPeriod).apply();
            Toast.makeText(this, "Periode hapus otomatis diubah ke: " + selectedPeriod, Toast.LENGTH_SHORT).show();
            // Trigger check
            triggerAutoDelete();
        });
    }

    private void showConfirmDeleteDialog(int days, String label) {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus log sensor (/monitoring) yang lebih lama dari " + label + "?\n\nData grafik volume air harian tetap aman.")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Hapus", (dialog, which) -> {
                    Toast.makeText(this, "Menyinkronkan data sebelum menghapus...", Toast.LENGTH_SHORT).show();
                    aggregateAndSyncBeforeDelete(days, label, () -> deleteMonitoringOlderThan(days, label));
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void aggregateAndSyncBeforeDelete(int days, String label, Runnable next) {
        dbRef.child("monitoring").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    java.util.TreeMap<String, java.util.List<Float>> dailyPhMap = new java.util.TreeMap<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String t = ds.child("tanggal").getValue(String.class);
                        Object v = ds.child("ph").getValue();
                        if (t != null && v != null) {
                            float ph = v instanceof Double ? ((Double) v).floatValue()
                                     : v instanceof Long   ? ((Long) v).floatValue()
                                     : v instanceof Float  ? (Float) v : 0f;
                            if (!dailyPhMap.containsKey(t)) {
                                dailyPhMap.put(t, new ArrayList<>());
                            }
                            dailyPhMap.get(t).add(ph);
                        }
                    }
                    for (String t : dailyPhMap.keySet()) {
                        java.util.List<Float> list = dailyPhMap.get(t);
                        float sum = 0f;
                        for (float val : list) sum += val;
                        float avg = list.isEmpty() ? 0f : sum / list.size();
                        dbRef.child("volume_air").child("daily").child(t).child("ph").setValue(avg);
                    }
                }
                next.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                next.run();
            }
        });
    }

    private void deleteMonitoringOlderThan(int days, String label) {
        String thresholdDate = getDateMinusDays(days);
        dbRef.child("monitoring").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ManajemenDataActivity.this, "Tidak ada data monitoring.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Kumpulkan semua key yang akan dihapus terlebih dahulu
                // (tidak boleh remove saat iterasi DataSnapshot — bisa menyebabkan silent failure)
                ArrayList<String> keysToDelete = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String t = ds.child("tanggal").getValue(String.class);
                    // Jika tanggal null (data rusak) atau lebih kecil/sama dengan thresholdDate (sudah lewat batas hari)
                    if (ds.getKey() != null && (t == null || t.compareTo(thresholdDate) <= 0)) {
                        keysToDelete.add(ds.getKey());
                    }
                }
                // Hapus satu per satu via referensi langsung
                for (String key : keysToDelete) {
                    dbRef.child("monitoring").child(key).removeValue();
                }
                if (keysToDelete.isEmpty()) {
                    Toast.makeText(ManajemenDataActivity.this, "Tidak ada log lama yang perlu dihapus.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManajemenDataActivity.this, "Berhasil menghapus " + keysToDelete.size() + " log lama (" + label + ").", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManajemenDataActivity.this, "Gagal menghapus: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getDateMinusDays(int days) {
        java.util.TimeZone wita = java.util.TimeZone.getTimeZone("Asia/Makassar");
        java.util.Calendar cal = java.util.Calendar.getInstance(wita);
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days);
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    private void triggerAutoDelete() {
        performAutoDeleteIfEnabled(this, dbRef);
    }

    public static void performAutoDeleteIfEnabled(Context context, DatabaseReference dbRef) {
        SharedPreferences prefs = SecurePrefsHelper.getPrefs(context, "DataManagementPrefs");
        boolean enabled = prefs.getBoolean("auto_delete_enabled", false);
        if (!enabled) return;

        String period = prefs.getString("auto_delete_period", "1 Minggu");
        int days = 7;
        if ("1 Hari".equals(period)) {
            days = 1;
        } else if ("1 Bulan".equals(period)) {
            days = 30;
        }

        final int finalDays = days;
        java.util.TimeZone wita = java.util.TimeZone.getTimeZone("Asia/Makassar");
        java.util.Calendar cal = java.util.Calendar.getInstance(wita);
        cal.add(java.util.Calendar.DAY_OF_YEAR, -finalDays);
        String thresholdDate = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));

        // First sync/aggregate then delete older logs
        dbRef.child("monitoring").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    java.util.TreeMap<String, java.util.List<Float>> dailyPhMap = new java.util.TreeMap<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String t = ds.child("tanggal").getValue(String.class);
                        Object v = ds.child("ph").getValue();
                        if (t != null && v != null) {
                            float ph = v instanceof Double ? ((Double) v).floatValue()
                                     : v instanceof Long   ? ((Long) v).floatValue()
                                     : v instanceof Float  ? (Float) v : 0f;
                            if (!dailyPhMap.containsKey(t)) {
                                dailyPhMap.put(t, new ArrayList<>());
                            }
                            dailyPhMap.get(t).add(ph);
                        }
                    }
                    for (String t : dailyPhMap.keySet()) {
                        java.util.List<Float> list = dailyPhMap.get(t);
                        float sum = 0f;
                        for (float val : list) sum += val;
                        float avg = list.isEmpty() ? 0f : sum / list.size();
                        dbRef.child("volume_air").child("daily").child(t).child("ph").setValue(avg);
                    }

                    // Kumpulkan semua key yang akan dihapus dulu, baru hapus via referensi langsung
                    ArrayList<String> keysToDelete = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String t = ds.child("tanggal").getValue(String.class);
                        if (ds.getKey() != null && (t == null || t.compareTo(thresholdDate) <= 0)) {
                            keysToDelete.add(ds.getKey());
                        }
                    }
                    for (String key : keysToDelete) {
                        dbRef.child("monitoring").child(key).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
