package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;

    // Komponen UI Captcha
    private CardView cardCaptcha;
    private FrameLayout flCaptchaBox;
    private ImageView ivCaptchaCheck;
    private ProgressBar pbCaptcha;
    private TextView tvCaptchaStatus;
    private boolean isCaptchaVerified = false;

    private Button btnSignIn;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Launcher untuk hasil Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    enableLoginButtons();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Konfigurasi Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Inisialisasi View Input
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        
        // Inisialisasi View Captcha Checkbox
        cardCaptcha = findViewById(R.id.cardCaptcha);
        flCaptchaBox = findViewById(R.id.flCaptchaBox);
        ivCaptchaCheck = findViewById(R.id.ivCaptchaCheck);
        pbCaptcha = findViewById(R.id.pbCaptcha);
        tvCaptchaStatus = findViewById(R.id.tvCaptchaStatus);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        View btnGoogle = findViewById(R.id.btnGoogle);



        // Logika simulasi reCAPTCHA checkbox
        cardCaptcha.setOnClickListener(v -> {
            if (!isCaptchaVerified) {
                verifyCaptcha();
            }
        });
        flCaptchaBox.setOnClickListener(v -> {
            if (!isCaptchaVerified) {
                verifyCaptcha();
            }
        });

        // Tombol Sign In (Email & Password via Firebase)
        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!isCaptchaVerified) {
                Toast.makeText(this, "Silakan verifikasi Captcha (Saya bukan robot)", Toast.LENGTH_SHORT).show();
                return;
            }

            disableLoginButtons();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        enableLoginButtons();
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
                            goToDashboard();
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Login gagal";
                            Toast.makeText(this, "Login Gagal: " + errorMsg, Toast.LENGTH_LONG).show();
                            resetCaptcha();
                        }
                    });
        });

        // Tombol Google Sign-In
        btnGoogle.setOnClickListener(v -> {
            disableLoginButtons();
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // Forgot Password via Firebase
        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email Anda terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Email reset password telah dikirim ke " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Gagal mengirim email reset", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void verifyCaptcha() {
        // Tampilkan loading di dalam checkbox
        ivCaptchaCheck.setVisibility(View.GONE);
        pbCaptcha.setVisibility(View.VISIBLE);
        tvCaptchaStatus.setText("Memverifikasi...");
        
        // Simulasi delay jaringan/verifikasi 1,5 detik
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pbCaptcha.setVisibility(View.GONE);
            ivCaptchaCheck.setVisibility(View.VISIBLE);
            tvCaptchaStatus.setText("Terverifikasi");
            isCaptchaVerified = true;
        }, 1500);
    }
    
    private void resetCaptcha() {
        isCaptchaVerified = false;
        ivCaptchaCheck.setVisibility(View.GONE);
        pbCaptcha.setVisibility(View.GONE);
        tvCaptchaStatus.setText("Ketuk untuk verifikasi");
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    enableLoginButtons();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Selamat datang, " + (user != null ? user.getDisplayName() : ""), Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    } else {
                        Toast.makeText(this, "Autentikasi Google gagal", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void disableLoginButtons() {
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Memproses...");
    }

    private void enableLoginButtons() {
        btnSignIn.setEnabled(true);
        btnSignIn.setText("Sign In");
    }
}