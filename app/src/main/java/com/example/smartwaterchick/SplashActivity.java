package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private View viewProgress;
    private TextView tvPercent;
    private int currentProgress = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Total durasi loading: 3 detik (3000ms), update tiap 30ms = 100 langkah
    private static final int TOTAL_DURATION_MS = 3000;
    private static final int INTERVAL_MS = 30;
    private static final int STEPS = TOTAL_DURATION_MS / INTERVAL_MS; // = 100

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sembunyikan ActionBar di splash
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);

        viewProgress = findViewById(R.id.viewProgress);
        tvPercent = findViewById(R.id.tvPercent);

        // Mulai animasi loading setelah layout selesai dirender
        viewProgress.post(this::startLoading);
    }

    private void startLoading() {
        int totalWidth = ((View) viewProgress.getParent()).getWidth();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentProgress <= STEPS) {
                    int percent = currentProgress; // 0–100
                    int fillWidth = (int) (totalWidth * (percent / 100f));

                    // Update lebar progress bar
                    ViewGroup.LayoutParams params = viewProgress.getLayoutParams();
                    params.width = fillWidth;
                    viewProgress.setLayoutParams(params);

                    // Update teks persentase
                    tvPercent.setText(percent + "%");

                    currentProgress++;
                    handler.postDelayed(this, INTERVAL_MS);
                } else {
                    // Loading selesai → pindah ke LoginActivity
                    goToLogin();
                }
            }
        };

        handler.post(runnable);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // tutup SplashActivity agar tidak bisa back ke sini
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // bersihkan handler
    }
}