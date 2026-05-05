package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvLoadingPercent;
    private int progress = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        tvLoadingPercent = findViewById(R.id.tvLoadingPercent);

        startLoadingAnimation();
    }

    private void startLoadingAnimation() {
        final int totalDuration = 2500;
        final int interval = 25;
        final int steps = totalDuration / interval;
        final int increment = 100 / steps;

        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress < 100) {
                    progress += increment;
                    if (progress > 100) progress = 100;

                    progressBar.setProgress(progress);
                    tvLoadingPercent.setText(progress + "%");

                    handler.postDelayed(this, interval);
                } else {
                    // Selesai loading — cek status login Firebase
                    checkLoginAndNavigate();
                }
            }
        };

        handler.post(progressRunnable);
    }

    private void checkLoginAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Pengguna sudah login sebelumnya → langsung ke Dashboard
            Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
            startActivity(intent);
        } else {
            // Belum login → arahkan ke halaman Login
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
