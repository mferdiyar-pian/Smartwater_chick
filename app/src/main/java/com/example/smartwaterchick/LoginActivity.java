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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // init database
        db = new DatabaseHelper(this);

        // init view
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        View btnSignIn = findViewById(R.id.btnSignIn);
        View btnGoogle = findViewById(R.id.btnGoogle);
        View btnFingerprint = findViewById(R.id.btnFingerprint);

        // toggle password
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.length());
        });

        // LOGIN BUTTON (pakai database)
        btnSignIn.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // validasi kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            // cek database
            boolean isValid = db.checkLogin(email, password);

            if (isValid) {
                Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();

            } else {
                Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show();
            }
        });

        // Forgot Password
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Fitur belum tersedia", Toast.LENGTH_SHORT).show()
        );

        // Sign Up
        tvSignUp.setOnClickListener(v ->
                Toast.makeText(this, "Arahkan ke halaman register", Toast.LENGTH_SHORT).show()
        );

        // Google Login (sementara dummy)
        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Login Google (dummy)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Fingerprint (sementara dummy)
        btnFingerprint.setOnClickListener(v -> {
            Toast.makeText(this, "Fingerprint (dummy)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }
}