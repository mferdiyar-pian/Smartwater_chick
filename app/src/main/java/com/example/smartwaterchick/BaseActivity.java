package com.example.smartwaterchick;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SmartWaterProfile";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Selalu gunakan mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.attachBaseContext(newBase);
    }
}
