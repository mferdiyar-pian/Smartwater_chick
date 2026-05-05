package com.example.smartwaterchick;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AnalisisActivity extends AppCompatActivity {

    private AnalisisChartView chartView;
    private BarChartView barChart;
    private Spinner spinnerFilter;
    private Spinner spinnerFilterBar;

    private DatabaseReference dbRef;

    // Simpan listener aktif agar bisa dilepas saat filter berubah atau Activity ditutup
    private ValueEventListener phListener;
    private Query phQuery;

    private ValueEventListener barListener;
    private Query barQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbRef = FirebaseDatabase.getInstance().getReference();

        chartView = findViewById(R.id.chartPh);
        barChart = findViewById(R.id.barChart);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        spinnerFilterBar = findViewById(R.id.spinnerFilterBar);

        // Setup Dropdown pH
        String[] filter = {"1 Hari", "1 Minggu", "1 Bulan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filter);
        spinnerFilter.setAdapter(adapter);
        spinnerFilter.setSelection(2);
        listenPhChart(30);

        // Setup Dropdown Volume Air
        String[] barFilterOptions = {"1 Hari", "1 Minggu", "1 Bulan"};
        ArrayAdapter<String> barAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, barFilterOptions);
        spinnerFilterBar.setAdapter(barAdapter);
        spinnerFilterBar.setSelection(2);
        listenBarChart("monthly", 30);

        // Event Filter Volume Air
        spinnerFilterBar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: listenBarChart("daily", 1); break;
                    case 1: listenBarChart("weekly", 7); break;
                    case 2: listenBarChart("monthly", 30); break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Event Filter pH
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) listenPhChart(1);
                else if (position == 1) listenPhChart(7);
                else listenPhChart(30);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Back Button
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            Intent intent = new Intent(AnalisisActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Notifikasi
        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(AnalisisActivity.this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        // Settings
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(AnalisisActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Laporan Lengkap
        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v ->
                Toast.makeText(this, "Membuka laporan lengkap...", Toast.LENGTH_SHORT).show());

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analytics);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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

    // ======================
    // LISTENER REAL-TIME GRAFIK pH
    // ======================
    private void listenPhChart(int limit) {
        if (phListener != null && phQuery != null) {
            phQuery.removeEventListener(phListener);
        }

        phQuery = dbRef.child("monitoring").limitToLast(limit);
        phListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Float> dataList = new ArrayList<>();
                for (DataSnapshot entry : snapshot.getChildren()) {
                    Object phValue = entry.child("ph").getValue();
                    if (phValue != null) {
                        float ph = 0f;
                        if (phValue instanceof Double) ph = ((Double) phValue).floatValue();
                        else if (phValue instanceof Long) ph = ((Long) phValue).floatValue();
                        dataList.add(ph);
                    }
                }
                if (dataList.isEmpty()) {
                    chartView.setData(new float[]{6.8f}, Color.parseColor("#1565C0"));
                    return;
                }
                float[] arr = new float[dataList.size()];
                for (int i = 0; i < dataList.size(); i++) arr[i] = dataList.get(i);
                chartView.setData(arr, Color.parseColor("#1565C0"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalisisActivity.this, "Gagal memuat data pH: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        phQuery.addValueEventListener(phListener);
    }

    // ======================
    // LISTENER REAL-TIME DIAGRAM BATANG VOLUME AIR
    // ======================
    private void listenBarChart(String tipe, int limit) {
        if (barListener != null && barQuery != null) {
            barQuery.removeEventListener(barListener);
        }

        barQuery = dbRef.child("volume_air").child(tipe).limitToLast(limit);
        barListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Float> dataList = new ArrayList<>();
                ArrayList<String> labelList = new ArrayList<>();
                for (DataSnapshot entry : snapshot.getChildren()) {
                    Object liter = entry.child("liter").getValue();
                    if (liter != null) {
                        float val = 0f;
                        if (liter instanceof Double) val = ((Double) liter).floatValue();
                        else if (liter instanceof Long) val = ((Long) liter).floatValue();
                        dataList.add(val);
                    }
                    if (entry.child("label").getValue() != null) {
                        labelList.add(String.valueOf(entry.child("label").getValue()));
                    } else {
                        labelList.add(entry.getKey());
                    }
                }
                barChart.loadDataFromDatabase(dataList, labelList, tipe);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalisisActivity.this, "Gagal memuat data volume: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        barQuery.addValueEventListener(barListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (phListener != null && phQuery != null) {
            phQuery.removeEventListener(phListener);
        }
        if (barListener != null && barQuery != null) {
            barQuery.removeEventListener(barListener);
        }
    }
}