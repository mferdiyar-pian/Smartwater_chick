package com.example.smartwaterchick;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class LoginActivity extends AppCompatActivity {

    private android.widget.EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    // CAPTCHA checkbox
    private CardView cardCaptcha;
    private FrameLayout flCaptchaBox;
    private ImageView ivCaptchaCheck;
    private ProgressBar pbCaptcha;
    private TextView tvCaptchaLabel, tvCaptchaStatus;

    private boolean isCaptchaVerified = false;
    private boolean isCaptchaLoading  = false;

    // Deteksi bot: hitung berapa kali diklik dalam waktu singkat
    private int tapCount = 0;
    private long firstTapTime = 0;
    private static final int BOT_TAP_THRESHOLD = 4;    // lebih dari 4 klik
    private static final long BOT_TAP_WINDOW_MS = 2000; // dalam 2 detik = curiga bot

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        // Init view umum
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        ivTogglePassword  = findViewById(R.id.ivTogglePassword);

        // Init CAPTCHA checkbox
        cardCaptcha      = findViewById(R.id.cardCaptcha);
        flCaptchaBox     = findViewById(R.id.flCaptchaBox);
        ivCaptchaCheck   = findViewById(R.id.ivCaptchaCheck);
        pbCaptcha        = findViewById(R.id.pbCaptcha);
        tvCaptchaLabel   = findViewById(R.id.tvCaptchaLabel);
        tvCaptchaStatus  = findViewById(R.id.tvCaptchaStatus);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp         = findViewById(R.id.tvSignUp);
        View     btnSignIn        = findViewById(R.id.btnSignIn);
        View     btnGoogle        = findViewById(R.id.btnGoogle);

        // Toggle password visibility
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

        // CAPTCHA checkbox – klik sekali untuk verifikasi
        cardCaptcha.setOnClickListener(v -> handleCaptchaTap());

        // LOGIN BUTTON
        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isCaptchaVerified) {
                Toast.makeText(this, "Mohon selesaikan verifikasi keamanan terlebih dahulu", Toast.LENGTH_SHORT).show();
                // Goyangkan card captcha agar user sadar
                shakeCaptchaCard();
                return;
            }

            boolean isValid = db.checkLogin(email, password);
            if (isValid) {
                Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            } else {
                Toast.makeText(this, "Login Gagal - Email/Password salah", Toast.LENGTH_SHORT).show();
                // Reset captcha supaya harus verifikasi ulang
                resetCaptcha();
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

        // Google Login
        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this,
                        "Login dengan Google belum tersedia. Fitur ini akan aktif setelah integrasi Firebase.",
                        Toast.LENGTH_LONG).show()
        );
    }

    // ─── CAPTCHA LOGIC ───────────────────────────────────────────────────────────

    private void handleCaptchaTap() {
        if (isCaptchaLoading || isCaptchaVerified) return;

        long now = System.currentTimeMillis();

        // Mulai hitung window jika ini klik pertama
        if (tapCount == 0) {
            firstTapTime = now;
        }
        tapCount++;

        // Cek apakah dalam window 2 detik sudah terlalu banyak klik (bot)
        if ((now - firstTapTime) < BOT_TAP_WINDOW_MS && tapCount > BOT_TAP_THRESHOLD) {
            // Kemungkinan bot – reset dan beri peringatan
            tapCount = 0;
            tvCaptchaStatus.setText("Aktivitas mencurigakan. Coba lagi.");
            tvCaptchaStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
            new Handler().postDelayed(() -> {
                tvCaptchaStatus.setText("Ketuk untuk verifikasi");
                tvCaptchaStatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
            }, 2000);
            return;
        }

        // Jika window sudah lewat, reset counter
        if ((now - firstTapTime) >= BOT_TAP_WINDOW_MS) {
            tapCount = 1;
            firstTapTime = now;
        }

        // Mulai proses verifikasi
        startCaptchaVerification();
    }

    private void startCaptchaVerification() {
        isCaptchaLoading = true;

        // Tampilkan loading spinner
        ivCaptchaCheck.setVisibility(View.GONE);
        pbCaptcha.setVisibility(View.VISIBLE);
        tvCaptchaStatus.setText("Memverifikasi...");
        tvCaptchaStatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

        // Simulasi verifikasi 1.2 detik lalu tampilkan centang
        new Handler().postDelayed(() -> {
            pbCaptcha.setVisibility(View.GONE);
            showCaptchaSuccess();
        }, 1200);
    }

    private void showCaptchaSuccess() {
        isCaptchaVerified = true;
        isCaptchaLoading  = false;

        // Tampilkan centang dengan animasi pop
        ivCaptchaCheck.setVisibility(View.VISIBLE);
        ivCaptchaCheck.setScaleX(0f);
        ivCaptchaCheck.setScaleY(0f);

        AnimatorSet pop = new AnimatorSet();
        pop.playTogether(
                ObjectAnimator.ofFloat(ivCaptchaCheck, "scaleX", 0f, 1.2f, 1f),
                ObjectAnimator.ofFloat(ivCaptchaCheck, "scaleY", 0f, 1.2f, 1f)
        );
        pop.setDuration(350);
        pop.setInterpolator(new OvershootInterpolator());
        pop.start();

        // Update teks & border checkbox menjadi hijau
        tvCaptchaLabel.setText("Verifikasi berhasil");
        tvCaptchaStatus.setText("✓ Anda terdeteksi sebagai manusia");
        tvCaptchaStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));

        // Beri outline hijau pada checkbox box
        flCaptchaBox.setBackground(getDrawable(R.drawable.bg_captcha_checkbox_checked));
        // Beri border hijau tipis pada card
        cardCaptcha.setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
    }

    private void resetCaptcha() {
        isCaptchaVerified = false;
        isCaptchaLoading  = false;
        tapCount = 0;

        ivCaptchaCheck.setVisibility(View.GONE);
        pbCaptcha.setVisibility(View.GONE);
        flCaptchaBox.setBackground(getDrawable(R.drawable.bg_captcha_checkbox));
        tvCaptchaLabel.setText("Saya bukan robot");
        tvCaptchaStatus.setText("Ketuk untuk verifikasi");
        tvCaptchaStatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
    }

    private void shakeCaptchaCard() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                cardCaptcha, "translationX",
                0f, -16f, 16f, -12f, 12f, -8f, 8f, 0f
        );
        shake.setDuration(400);
        shake.start();
    }
}