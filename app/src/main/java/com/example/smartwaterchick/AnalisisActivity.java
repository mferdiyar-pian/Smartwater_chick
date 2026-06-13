package com.example.smartwaterchick;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AnalisisActivity extends BaseActivity {

    private AnalisisChartView chartView;
    private BarChartView barChart;
    private Spinner spinnerFilter;
    private Spinner spinnerFilterBar;
    private DatabaseReference dbRef;

    private ValueEventListener phListener;
    private Query phQuery;
    private ValueEventListener barListener;
    private Query barQuery;

    // View untuk Informasi Terkini
    private View rowEfisiensi;
    private View rowBoros;
    private View rowPhStabil;
    private View rowPhBuruk;
    private android.widget.TextView tvEfisiensiJudul;
    private android.widget.TextView tvEfisiensiDesc;
    private android.widget.TextView tvBorosJudul;
    private android.widget.TextView tvBorosDesc;
    private android.widget.TextView tvPhStabilJudul;
    private android.widget.TextView tvPhStabilDesc;
    private android.widget.TextView tvPhBurukJudul;
    private android.widget.TextView tvPhBurukDesc;
    private android.widget.TextView tvInfoKosong;

    // Dideklarasikan sebagai field, diinisialisasi di onCreate()
    private ActivityResultLauncher<Intent> saveLauncher;

    private List<String> dailyVolData   = new ArrayList<>();
    private List<String> weeklyVolData  = new ArrayList<>();
    private List<String> monthlyVolData = new ArrayList<>();
    private List<String> dailyPhData    = new ArrayList<>();
    private List<String> weeklyPhData   = new ArrayList<>();
    private List<String> monthlyPhData  = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Harus di dalam onCreate(), setelah super.onCreate()
        saveLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    writeExcelToUri(result.getData().getData());
                }
            }
        );

        dbRef = FirebaseDatabase.getInstance().getReference();
        chartView        = findViewById(R.id.chartPh);
        barChart         = findViewById(R.id.barChart);
        spinnerFilter    = findViewById(R.id.spinnerFilter);
        spinnerFilterBar = findViewById(R.id.spinnerFilterBar);

        // Inisialisasi View Informasi Terkini
        rowEfisiensi     = findViewById(R.id.rowEfisiensi);
        rowBoros         = findViewById(R.id.rowBoros);
        rowPhStabil      = findViewById(R.id.rowPhStabil);
        rowPhBuruk       = findViewById(R.id.rowPhBuruk);

        tvEfisiensiJudul = findViewById(R.id.tvEfisiensiJudul);
        tvEfisiensiDesc  = findViewById(R.id.tvEfisiensiDesc);
        tvBorosJudul     = findViewById(R.id.tvBorosJudul);
        tvBorosDesc      = findViewById(R.id.tvBorosDesc);
        tvPhStabilJudul  = findViewById(R.id.tvPhStabilJudul);
        tvPhStabilDesc   = findViewById(R.id.tvPhStabilDesc);
        tvPhBurukJudul   = findViewById(R.id.tvPhBurukJudul);
        tvPhBurukDesc    = findViewById(R.id.tvPhBurukDesc);
        tvInfoKosong     = findViewById(R.id.tvInfoKosong);

        String[] filter = {"1 Hari", "1 Minggu", "1 Bulan"};
        spinnerFilter.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, filter));
        spinnerFilter.setSelection(2);
        listenPhChart(30, "monthly");

        String[] barOpts = {"1 Hari", "1 Minggu", "1 Bulan"};
        spinnerFilterBar.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, barOpts));
        spinnerFilterBar.setSelection(2);
        listenBarChart("monthly", 30);

        spinnerFilterBar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                switch (pos) {
                    case 0: listenBarChart("daily",   1);  break;
                    case 1: listenBarChart("weekly",  7);  break;
                    case 2: listenBarChart("monthly", 30); break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                switch (pos) {
                    case 0: listenPhChart(1,  "daily");   break;
                    case 1: listenPhChart(7,  "weekly");  break;
                    case 2: listenPhChart(30, "monthly"); break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v -> {
            Toast.makeText(this, "Menyiapkan laporan...", Toast.LENGTH_SHORT).show();
            // Jika real-time listener sudah mengisi data, langsung export.
            // Jika belum (misalnya koneksi lambat), fetch dulu dari Firebase.
            if (hasAnyData()) {
                openSaveDialog();
            } else {
                fetchAllDataThenExport();
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analytics);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out); finish(); return true;
            } else if (id == R.id.nav_analytics) { return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out); return true;
            } else if (id == R.id.nav_devices) {
                startActivity(new Intent(this, PerangkatActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out); finish(); return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out); finish(); return true;
            }
            return false;
        });
    }

    // ── Cek apakah sudah ada data dari real-time listener ──────────────────
    private boolean hasAnyData() {
        return !monthlyVolData.isEmpty() || !monthlyPhData.isEmpty()
            || !weeklyVolData.isEmpty()  || !weeklyPhData.isEmpty()
            || !dailyVolData.isEmpty()   || !dailyPhData.isEmpty();
    }

    // ── Fetch dari Firebase (fallback jika listener belum mengisi data) ────
    private void fetchAllDataThenExport() {
        dbRef.child("volume_air").child("daily").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                // Ambil dan urutkan semua entry harian dari Firebase
                java.util.TreeMap<String, String> sorted = new java.util.TreeMap<>();
                for (DataSnapshot ds : snap.getChildren()) {
                    Object liter = ds.child("liter").getValue();
                    if (ds.getKey() != null && liter != null) {
                        sorted.put(ds.getKey(), liter.toString());
                    }
                }
                java.util.List<String> keys = new ArrayList<>(sorted.keySet());

                // Slice untuk masing-masing periode
                if (dailyVolData.isEmpty()) {
                    dailyVolData = new ArrayList<>();
                    if (!keys.isEmpty()) dailyVolData.add(sorted.get(keys.get(keys.size() - 1)));
                }
                if (weeklyVolData.isEmpty()) {
                    int s = Math.max(0, keys.size() - 7);
                    weeklyVolData = new ArrayList<>();
                    for (int i = s; i < keys.size(); i++) weeklyVolData.add(sorted.get(keys.get(i)));
                }
                if (monthlyVolData.isEmpty()) {
                    int s = Math.max(0, keys.size() - 30);
                    monthlyVolData = new ArrayList<>();
                    for (int i = s; i < keys.size(); i++) monthlyVolData.add(sorted.get(keys.get(i)));
                }

                dbRef.child("monitoring").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot phSnap) {
                        if (monthlyPhData.isEmpty()) {
                            List<String> all = new ArrayList<>();
                            for (DataSnapshot ds : phSnap.getChildren()) {
                                Object ph = ds.child("ph").getValue();
                                if (ph != null) all.add(ph.toString());
                            }
                            monthlyPhData = new ArrayList<>(all);
                            int sz = all.size();
                            weeklyPhData = sz >= 7
                                ? new ArrayList<>(all.subList(sz - 7, sz))
                                : new ArrayList<>(all);
                            dailyPhData = sz >= 1
                                ? new ArrayList<>(all.subList(sz - 1, sz))
                                : new ArrayList<>(all);
                        }
                        openSaveDialog();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Toast.makeText(AnalisisActivity.this,
                            "Gagal ambil pH: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AnalisisActivity.this,
                    "Gagal ambil volume: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> extractLiters(DataSnapshot snap) {
        List<String> list = new ArrayList<>();
        if (snap.exists()) {
            for (DataSnapshot ds : snap.getChildren()) {
                Object liter = ds.child("liter").getValue();
                if (liter != null) list.add(liter.toString());
            }
        }
        return list;
    }

    // ── Buka dialog simpan file ────────────────────────────────────────────
    private void openSaveDialog() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // XLSX — lebih kompatibel, tidak memerlukan library tambahan
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_TITLE, "Laporan_SmartWaterChick.xlsx");
            saveLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal buka dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Tulis XLSX ke URI yang dipilih user (background thread) ───────────
    private void writeExcelToUri(Uri uri) {
        // Snapshot data agar thread-safe
        final List<String> dv = new ArrayList<>(dailyVolData);
        final List<String> wv = new ArrayList<>(weeklyVolData);
        final List<String> mv = new ArrayList<>(monthlyVolData);
        final List<String> dp = new ArrayList<>(dailyPhData);
        final List<String> wp = new ArrayList<>(weeklyPhData);
        final List<String> mp = new ArrayList<>(monthlyPhData);

        new Thread(() -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Tidak bisa membuka file output");
                new XlsxWriter(dv, wv, mv, dp, wp, mp).write(out);
                out.flush();
                runOnUiThread(() ->
                    Toast.makeText(AnalisisActivity.this,
                        "Laporan berhasil disimpan!", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() ->
                    Toast.makeText(AnalisisActivity.this,
                        "Gagal: " + msg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // =====================================================================
    // LISTENER REAL-TIME pH
    // =====================================================================
    private void listenPhChart(int limit, String period) {
        if (phListener != null && phQuery != null) phQuery.removeEventListener(phListener);
        phQuery = dbRef.child("monitoring").limitToLast(limit);
        phListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Float> fl = new ArrayList<>();
                List<String> sl    = new ArrayList<>();
                for (DataSnapshot entry : snapshot.getChildren()) {
                    Object v = entry.child("ph").getValue();
                    if (v != null) {
                        float ph = v instanceof Double ? ((Double)v).floatValue()
                                 : v instanceof Long   ? ((Long)v).floatValue() : 0f;
                        fl.add(ph); sl.add(String.valueOf(ph));
                    }
                }
                // Update list untuk export XLS
                if ("daily".equals(period))       dailyPhData   = sl;
                else if ("weekly".equals(period)) weeklyPhData  = sl;
                else                              monthlyPhData = sl;

                updateInformasiTerkini();

                if (fl.isEmpty()) {
                    chartView.setData(new float[]{6.8f},
                        android.graphics.Color.parseColor("#1565C0"));
                    return;
                }
                float[] arr = new float[fl.size()];
                for (int i = 0; i < fl.size(); i++) arr[i] = fl.get(i);
                chartView.setData(arr, android.graphics.Color.parseColor("#1565C0"));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AnalisisActivity.this,
                    "Gagal memuat pH: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        phQuery.addValueEventListener(phListener);
    }

    // =====================================================================
    // LISTENER REAL-TIME Volume Air — membaca dari /volume_air/daily
    // dan menyusun data harian/mingguan/bulanan sendiri
    // =====================================================================
    private android.widget.TextView tvTotalDigunakan;

    private void listenBarChart(String tipe, int limit) {
        if (barListener != null && barQuery != null) barQuery.removeEventListener(barListener);

        // Ambil referensi tvTotalDigunakan jika belum
        if (tvTotalDigunakan == null) {
            tvTotalDigunakan = findViewById(R.id.tvTotalDigunakan);
        }

        // Selalu baca dari /volume_air/daily — sumber data aktual ESP32
        // Ambil 30 hari terakhir (cukup untuk semua mode filter)
        barQuery = dbRef.child("volume_air").child("daily").limitToLast(30);
        barListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Kumpulkan semua entry harian, diurutkan berdasarkan kunci (tanggal)
                java.util.TreeMap<String, Float> allDaily = new java.util.TreeMap<>();
                for (DataSnapshot entry : snapshot.getChildren()) {
                    String key = entry.getKey(); // format: "YYYY-MM-DD"
                    Object liter = entry.child("liter").getValue();
                    if (key != null && liter != null) {
                        float val = liter instanceof Double ? ((Double) liter).floatValue()
                                : liter instanceof Long   ? ((Long) liter).floatValue()
                                : liter instanceof Float  ? (Float) liter : 0f;
                        allDaily.put(key, val);
                    }
                }

                ArrayList<Float>  fl  = new ArrayList<>();
                ArrayList<String> lbs = new ArrayList<>();
                List<String>      sl  = new ArrayList<>();

                java.util.List<String> sortedKeys = new ArrayList<>(allDaily.keySet());
                // sortedKeys sudah terurut ascending (lama → baru) karena TreeMap

                if (tipe.equals("daily")) {
                    // Tampilkan hanya hari ini (entry terbaru)
                    if (!sortedKeys.isEmpty()) {
                        String key = sortedKeys.get(sortedKeys.size() - 1);
                        float val  = allDaily.get(key);
                        fl.add(val);
                        sl.add(String.valueOf(val));
                        // Format label: ambil hanya tanggal (bagian terakhir setelah tanda "-" ke-2)
                        lbs.add(shortDate(key));
                    }

                } else if (tipe.equals("weekly")) {
                    // Tampilkan 7 hari terakhir
                    int start = Math.max(0, sortedKeys.size() - 7);
                    for (int i = start; i < sortedKeys.size(); i++) {
                        String key = sortedKeys.get(i);
                        float val  = allDaily.get(key);
                        fl.add(val);
                        sl.add(String.valueOf(val));
                        lbs.add(shortDate(key));
                    }

                } else { // monthly — tampilkan hingga 30 hari terakhir
                    int start = Math.max(0, sortedKeys.size() - 30);
                    for (int i = start; i < sortedKeys.size(); i++) {
                        String key = sortedKeys.get(i);
                        float val  = allDaily.get(key);
                        fl.add(val);
                        sl.add(String.valueOf(val));
                        lbs.add(shortDate(key));
                    }
                }

                // Update list untuk export XLS
                if ("daily".equals(tipe))       dailyVolData   = sl;
                else if ("weekly".equals(tipe)) weeklyVolData  = sl;
                else                            monthlyVolData = sl;

                updateInformasiTerkini();

                // Update ringkasan "Total Digunakan"
                if (tvTotalDigunakan != null && !fl.isEmpty()) {
                    float total = 0;
                    for (float v : fl) total += v;
                    if (tipe.equals("daily")) {
                        tvTotalDigunakan.setText(String.format(java.util.Locale.getDefault(), "%.2f L", total));
                    } else {
                        tvTotalDigunakan.setText(String.format(java.util.Locale.getDefault(), "%.2f L", total));
                    }
                }

                // Hitung skala Y secara dinamis
                float maxVal = 0.001f;
                for (float v : fl) if (v > maxVal) maxVal = v;
                // Bulatkan ke atas ke nilai "cantik"
                float scale = computeNiceMax(maxVal);

                barChart.loadDataFromDatabase(fl, lbs, tipe, scale);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AnalisisActivity.this,
                    "Gagal memuat volume: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        barQuery.addValueEventListener(barListener);
    }

    /** Ambil dua digit terakhir tanggal (DD) dari key "YYYY-MM-DD" sebagai label ringkas */
    private String shortDate(String key) {
        if (key == null) return "-";
        String[] parts = key.split("-");
        if (parts.length == 3) {
            // Buang leading zero: "08" → "8"
            try { return String.valueOf(Integer.parseInt(parts[2])); } catch (Exception ignored) {}
            return parts[2];
        }
        return key;
    }

    /** Hitung skala Y maksimum yang "bulat" di atas nilai tertinggi data */
    private float computeNiceMax(float max) {
        if (max <= 0) return 2.0f;
        // Langkah yang digunakan: 0.5 L, 1 L, 2 L, 5 L, 10 L, 20 L, 50 L, dst.
        float[] steps = {0.5f, 1f, 2f, 5f, 10f, 20f, 50f, 100f, 200f, 500f, 1000f};
        float target = max * 1.25f; // 25% ruang di atas nilai tertinggi
        for (float s : steps) {
            float ceiling = (float) Math.ceil(target / s) * s;
            if (ceiling >= target) return ceiling;
        }
        return (float) Math.ceil(max * 1.3f);
    }

    private void updateInformasiTerkini() {
        // Cek data volume air
        List<String> activeVolData = null;
        if (!dailyVolData.isEmpty()) {
            activeVolData = dailyVolData;
        } else if (!weeklyVolData.isEmpty()) {
            activeVolData = weeklyVolData;
        } else if (!monthlyVolData.isEmpty()) {
            activeVolData = monthlyVolData;
        }

        // Cek data pH
        List<String> activePhData = null;
        if (!dailyPhData.isEmpty()) {
            activePhData = dailyPhData;
        } else if (!weeklyPhData.isEmpty()) {
            activePhData = weeklyPhData;
        } else if (!monthlyPhData.isEmpty()) {
            activePhData = monthlyPhData;
        }

        boolean hasData = false;

        // Evaluasi Volume Air
        if (activeVolData != null && !activeVolData.isEmpty()) {
            hasData = true;
            try {
                float sum = 0;
                float latestVol = 0;
                int count = 0;
                for (String s : activeVolData) {
                    try {
                        float v = Float.parseFloat(s);
                        sum += v;
                        latestVol = v;
                        count++;
                    } catch (NumberFormatException ignored) {}
                }
                float avg = count > 0 ? sum / count : 0;

                // Tentukan boros vs efisien
                // Jika latestVol di atas rata-rata (atau di atas 110L), maka boros. Jika di bawah, efisien.
                if (latestVol > avg || latestVol > 110f) {
                    rowBoros.setVisibility(View.VISIBLE);
                    rowEfisiensi.setVisibility(View.GONE);
                    tvBorosJudul.setText("Penggunaan Air Boros");
                    tvBorosDesc.setText("Konsumsi air meningkat di angka " + String.format("%.1f", latestVol) + " Liter (di atas rata-rata " + String.format("%.1f", avg) + " L). Harap pantau jika terjadi kebocoran pipa atau pemborosan air.");
                } else {
                    rowEfisiensi.setVisibility(View.VISIBLE);
                    rowBoros.setVisibility(View.GONE);
                    tvEfisiensiJudul.setText("Efisiensi Air Meningkat");
                    tvEfisiensiDesc.setText("Konsumsi air terpantau hemat di angka " + String.format("%.1f", latestVol) + " Liter (di bawah rata-rata " + String.format("%.1f", avg) + " L). Otomatisasi tangki berjalan optimal sesuai jadwal.");
                }
            } catch (Exception e) {
                rowEfisiensi.setVisibility(View.GONE);
                rowBoros.setVisibility(View.GONE);
            }
        } else {
            rowEfisiensi.setVisibility(View.GONE);
            rowBoros.setVisibility(View.GONE);
        }

        // Evaluasi pH Air
        if (activePhData != null && !activePhData.isEmpty()) {
            hasData = true;
            try {
                float latestPh = 0;
                for (String s : activePhData) {
                    try {
                        latestPh = Float.parseFloat(s);
                    } catch (NumberFormatException ignored) {}
                }

                // Tentukan stabil vs tidak stabil
                if (latestPh >= 6.5f && latestPh <= 7.5f) {
                    rowPhStabil.setVisibility(View.VISIBLE);
                    rowPhBuruk.setVisibility(View.GONE);
                    tvPhStabilJudul.setText("pH Air Stabil");
                    tvPhStabilDesc.setText("Tingkat pH air terpantau aman di angka " + String.format("%.1f", latestPh) + ". Kualitas air sangat ideal untuk kesehatan pencernaan dan sanitasi ayam.");
                } else {
                    rowPhBuruk.setVisibility(View.VISIBLE);
                    rowPhStabil.setVisibility(View.GONE);
                    tvPhBurukJudul.setText("pH Air Tidak Stabil / Buruk");
                    tvPhBurukDesc.setText("Tingkat pH terdeteksi buruk di angka " + String.format("%.1f", latestPh) + " (di luar batas ideal 6.5 - 7.5). Mohon segera lakukan pembersihan tangki atau filtrasi air.");
                }
            } catch (Exception e) {
                rowPhStabil.setVisibility(View.GONE);
                rowPhBuruk.setVisibility(View.GONE);
            }
        } else {
            rowPhStabil.setVisibility(View.GONE);
            rowPhBuruk.setVisibility(View.GONE);
        }

        if (hasData) {
            tvInfoKosong.setVisibility(View.GONE);
        } else {
            tvInfoKosong.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (phListener  != null && phQuery  != null) phQuery.removeEventListener(phListener);
        if (barListener != null && barQuery != null) barQuery.removeEventListener(barListener);
    }
}