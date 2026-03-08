package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        View btnSignIn = findViewById(R.id.btnSignIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        View btnGoogle = findViewById(R.id.btnGoogle);
        View btnFingerprint = findViewById(R.id.btnFingerprint);

        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility); // Reset to visible icon
                isPasswordVisible = false;
            } else {
                // Show password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                // You might want a different icon for 'hide'
                isPasswordVisible = true;
            }
            // Move cursor to end
            etPassword.setSelection(etPassword.length());
        });

        // Sign In button - Dipermudah untuk testing
        btnSignIn.setOnClickListener(v -> {
            // Langsung pindah ke Dashboard untuk mempermudah testing
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password diklik", Toast.LENGTH_SHORT).show();
        });

        // Sign Up
        tvSignUp.setOnClickListener(v -> {
            Toast.makeText(this, "Sign Up diklik", Toast.LENGTH_SHORT).show();
        });

        // Google Login
        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Login dengan Google", Toast.LENGTH_SHORT).show();
            // Bypass login for testing
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });

        // Fingerprint Login
        btnFingerprint.setOnClickListener(v -> {
            Toast.makeText(this, "Login dengan Fingerprint", Toast.LENGTH_SHORT).show();
            // Bypass login for testing
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }
}
