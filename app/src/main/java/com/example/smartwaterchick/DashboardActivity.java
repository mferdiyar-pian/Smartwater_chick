package com.example.smartwaterchick;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Notification icon
        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(DashboardActivity.this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        // Settings icon
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Ganti air button
        findViewById(R.id.btnGantiAir).setOnClickListener(v ->
                Toast.makeText(this, "Mengganti air...", Toast.LENGTH_SHORT).show()
        );

        // Abaikan button
        findViewById(R.id.btnAbaikan).setOnClickListener(v ->
                findViewById(R.id.btnAbaikan).setVisibility(android.view.View.GONE)
        );

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_beranda);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
}

