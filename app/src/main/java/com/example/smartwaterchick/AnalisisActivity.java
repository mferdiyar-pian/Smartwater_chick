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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class AnalisisActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private AnalisisChartView chartView;
    private BarChartView barChart;
    private Spinner spinnerFilter;
    private Spinner spinnerFilterBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);
        chartView = findViewById(R.id.chartPh);
        barChart = findViewById(R.id.barChart);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        spinnerFilterBar = findViewById(R.id.spinnerFilterBar);

        // ======================
        // SETUP DROPDOWN
        // ======================
        String[] filter = {"1 Hari", "1 Minggu", "1 Bulan"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                filter
        );

        spinnerFilter.setAdapter(adapter);

        // ======================
        // DEFAULT LOAD DIAGRAM PH (1 BULAN)
        // ======================
        spinnerFilter.setSelection(2);  // default ke "1 Bulan"
        loadChartData(30);

        // ======================
        // SETUP DROPDOWN DIAGRAM BATANG
        // ======================
        String[] barFilterOptions = {"1 Hari", "1 Minggu", "1 Bulan"};
        ArrayAdapter<String> barAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                barFilterOptions
        );
        spinnerFilterBar.setAdapter(barAdapter);
        spinnerFilterBar.setSelection(2); // Default ke 1 Bulan (30 data)
        loadBarChartData("monthly", 30);

        // ======================
        // EVENT FILTER DIAGRAM BATANG
        // ======================
        spinnerFilterBar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 1 Hari = hanya hari ini
                        loadBarChartData("daily", 1);
                        break;
                    case 1: // 1 Minggu = 7 hari terakhir
                        loadBarChartData("weekly", 7);
                        break;
                    case 2: // 1 Bulan = 30 hari
                        loadBarChartData("monthly", 30);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ======================
        // EVENT FILTER
        // ======================
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    loadChartData(1);
                } else if (position == 1) {
                    loadChartData(7);
                } else {
                    loadChartData(30);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ======================
        // BACK BUTTON
        // ======================
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            Intent intent = new Intent(AnalisisActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(AnalisisActivity.this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(AnalisisActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v ->
                Toast.makeText(this, "Membuka laporan lengkap...", Toast.LENGTH_SHORT).show());

        // ======================
        // BOTTOM NAVIGATION
        // ======================
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
    // METHOD LOAD DATA KE CHART (DIAGRAM GARIS pH)
    // ======================
    private void loadChartData(int limit) {
        ArrayList<Float> dataList = db.getLimitedData("ph", limit);

        if (dataList == null || dataList.isEmpty()) {
            chartView.setData(new float[]{6.8f}, Color.parseColor("#1565C0"));
            return;
        }

        float[] dataArray = new float[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            dataArray[i] = dataList.get(i);
        }

        chartView.setData(dataArray, Color.parseColor("#1565C0"));
    }

    // ======================
    // METHOD LOAD DATA KE DIAGRAM BATANG
    // ======================
    private void loadBarChartData(String tipe, int limit) {
        ArrayList<Float> dataList = db.getVolumeData(tipe, limit);
        ArrayList<String> labelList = db.getVolumeLabels(tipe, limit);
        barChart.loadDataFromDatabase(dataList, labelList, tipe);
    }
}