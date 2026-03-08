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
                ivTogglePassword.setColorFilter(getResources().getColor(android.R.color.darker_gray));
                isPasswordVisible = false;
            } else {
                // Show password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setColorFilter(getResources().getColor(R.color.blue_primary));
                isPasswordVisible = true;
            }
            // Move cursor to end
            etPassword.setSelection(etPassword.length());
        });

        // Sign In button
        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
                etEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Format email tidak valid");
                etEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                etPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                etPassword.setError("Password minimal 6 karakter");
                etPassword.requestFocus();
                return;
            }

            // TODO: Ganti dengan autentikasi Firebase / API Anda
            performLogin(email, password);
        });

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Navigasi ke halaman ForgotPasswordActivity
            Toast.makeText(this, "Forgot Password diklik", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, ForgotPasswordActivity.class);
            // startActivity(intent);
        });

        // Sign Up
        tvSignUp.setOnClickListener(v -> {
            // TODO: Navigasi ke halaman RegisterActivity
            Toast.makeText(this, "Sign Up diklik", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, RegisterActivity.class);
            // startActivity(intent);
        });

        // Google Login
        btnGoogle.setOnClickListener(v -> {
            // TODO: Implementasi Google Sign-In
            Toast.makeText(this, "Login dengan Google", Toast.LENGTH_SHORT).show();
        });

        // Fingerprint Login
        btnFingerprint.setOnClickListener(v -> {
            // TODO: Implementasi BiometricPrompt
            Toast.makeText(this, "Login dengan Fingerprint", Toast.LENGTH_SHORT).show();
        });
    }

    private void performLogin(String email, String password) {
        // Contoh validasi lokal (ganti dengan Firebase Auth / API)
        // Contoh dummy:
        if (email.equals("admin@smartwaterchick.com") && password.equals("admin123")) {
            Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show();
            // Navigasi ke MainActivity / Dashboard
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show();
        }
    }
}