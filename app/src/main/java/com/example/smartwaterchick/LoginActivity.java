package com.example.smartwaterchick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private CardView cardCaptcha;
    private FrameLayout flCaptchaBox;
    private ImageView ivCaptchaCheck;
    private ProgressBar pbCaptcha;
    private TextView tvCaptchaStatus;
    private boolean isCaptchaVerified = false;
    private Button btnSignIn;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null && account.getIdToken() != null) {
                        firebaseAuthWithGoogle(account.getIdToken());
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In gagal", Toast.LENGTH_SHORT).show();
                    enableLoginButtons();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cardCaptcha = findViewById(R.id.cardCaptcha);
        flCaptchaBox = findViewById(R.id.flCaptchaBox);
        ivCaptchaCheck = findViewById(R.id.ivCaptchaCheck);
        pbCaptcha = findViewById(R.id.pbCaptcha);
        tvCaptchaStatus = findViewById(R.id.tvCaptchaStatus);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        View btnGoogle = findViewById(R.id.btnGoogle);

        View.OnClickListener captchaClick = v -> { if (!isCaptchaVerified) verifyCaptcha(); };
        cardCaptcha.setOnClickListener(captchaClick);
        flCaptchaBox.setOnClickListener(captchaClick);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isCaptchaVerified) {
                Toast.makeText(this, "Silakan verifikasi Captcha terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            disableLoginButtons();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        enableLoginButtons();
                        if (task.isSuccessful()) {
                            goToDashboard();
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Login gagal";
                            Toast.makeText(this, "Login Gagal: " + msg, Toast.LENGTH_LONG).show();
                            resetCaptcha();
                        }
                    });
        });

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                disableLoginButtons();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                Toast.makeText(this,
                        task.isSuccessful() ? "Email reset dikirim ke " + email : "Gagal mengirim email reset",
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    private void verifyCaptcha() {
        ivCaptchaCheck.setVisibility(View.GONE);
        pbCaptcha.setVisibility(View.VISIBLE);
        tvCaptchaStatus.setText("Memverifikasi...");
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
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
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
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void disableLoginButtons() { btnSignIn.setEnabled(false); btnSignIn.setText("Memproses..."); }
    private void enableLoginButtons() { btnSignIn.setEnabled(true); btnSignIn.setText("Sign In"); }
}
