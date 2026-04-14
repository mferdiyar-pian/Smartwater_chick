package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

        // Start loading animation
        startLoadingAnimation();
    }

    private void startLoadingAnimation() {
        // Animate progress from 0 to 100 over 2.5 seconds
        final int totalDuration = 2500; // 2.5 seconds
        final int interval = 25; // update every 25ms
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
                    // Loading complete, go to LoginActivity
                    navigateToLogin();
                }
            }
        };

        handler.post(progressRunnable);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish(); // Close splash activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null);
    }
}
