package com.example.smartwaterchick;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    public static final String PREF_DARK_MODE = "dark_mode_enabled";
    public static final String PREFS_NAME = "SmartWaterProfile";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Terapkan mode gelap sebelum layout di-inflate
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.attachBaseContext(newBase);
    }
}
