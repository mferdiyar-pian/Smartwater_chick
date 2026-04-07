package com.example.smartwaterchick;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class AnalisisActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private AnalisisChartView chartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new DatabaseHelper(this);
        chartView = findViewById(R.id.chartPh);

        // ambil data dari database
        ArrayList<Float> suhuList = db.getSuhuData();

        float[] suhuArray = new float[suhuList.size()];
        for (int i = 0; i < suhuList.size(); i++) {
            suhuArray[i] = suhuList.get(i);
        }

        chartView.setData(suhuArray, Color.parseColor("#1B5BCE"));

        // tombol back
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show());

        findViewById(R.id.ivSettings).setOnClickListener(v ->
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnLaporanLengkap).setOnClickListener(v ->
                Toast.makeText(this, "Membuka laporan lengkap...", Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analytics);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;

            } else if (id == R.id.nav_analytics) {
                return true;

            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
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