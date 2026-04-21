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

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etCaptcha;
    private ImageView ivTogglePassword, ivRefreshCaptcha, ivCaptchaStatus;
    private TextView tvCaptchaQuestion;
    private boolean isPasswordVisible = false;
    private boolean isCaptchaValid = false;

    private DatabaseHelper db;
    private Random random = new Random();
    private int currentCaptchaAnswer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // init database
        db = new DatabaseHelper(this);

        // init view
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCaptcha = findViewById(R.id.etCaptcha);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivRefreshCaptcha = findViewById(R.id.ivRefreshCaptcha);
        ivCaptchaStatus = findViewById(R.id.ivCaptchaStatus);
        tvCaptchaQuestion = findViewById(R.id.tvCaptchaQuestion);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        View btnSignIn = findViewById(R.id.btnSignIn);
        View btnGoogle = findViewById(R.id.btnGoogle);

        // Generate initial captcha
        generateNewCaptcha();

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

        // Refresh Captcha button
        ivRefreshCaptcha.setOnClickListener(v -> generateNewCaptcha());

        // LOGIN BUTTON (pakai database + captcha)
        btnSignIn.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String captchaInput = etCaptcha.getText().toString().trim();

            // validasi kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            // validasi captcha
            if (captchaInput.isEmpty()) {
                Toast.makeText(this, "Captcha tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int userAnswer = Integer.parseInt(captchaInput);
                if (userAnswer != currentCaptchaAnswer) {
                    Toast.makeText(this, "Captcha salah! Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                    ivCaptchaStatus.setImageResource(R.drawable.ic_close);
                    ivCaptchaStatus.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
                    ivCaptchaStatus.setVisibility(View.VISIBLE);
                    generateNewCaptcha();
                    return;
                }
                // Captcha benar
                ivCaptchaStatus.setImageResource(R.drawable.ic_check_circle);
                ivCaptchaStatus.setColorFilter(getResources().getColor(android.R.color.holo_green_light));
                ivCaptchaStatus.setVisibility(View.VISIBLE);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Captcha harus berupa angka", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Login Gagal - Email/Password salah", Toast.LENGTH_SHORT).show();
                generateNewCaptcha();
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

        // Google Login (belum tersambung Firebase)
        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this,
                "Login dengan Google belum tersedia. Fitur ini akan aktif setelah integrasi Firebase.",
                Toast.LENGTH_LONG).show();
        });
    }

    // Method untuk generate captcha baru
    private void generateNewCaptcha() {
        int num1 = random.nextInt(10) + 1; // 1-10
        int num2 = random.nextInt(10) + 1; // 1-10
        currentCaptchaAnswer = num1 + num2;

        tvCaptchaQuestion.setText(num1 + " + " + num2 + " = ?");
        etCaptcha.setText("");
        ivCaptchaStatus.setVisibility(View.INVISIBLE);
    }
}