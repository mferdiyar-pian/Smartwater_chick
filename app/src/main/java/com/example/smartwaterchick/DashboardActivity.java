package com.example.smartwaterchick;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends BaseActivity {

    // ─── Views pH ───
    private TextView tvPhValue;
    private TextView tvPhBarLabel;
    private TextView tvPhStatus;
    private View     ivPhIndicator;

    // ─── Views lainnya ───
    private TextView tvWaterCapacity;
    private TextView tvAlertMessage;
    private View     cardAlert;
    private TextView tvIrrigationStatus;
    private android.widget.ImageView ivIrrigationIcon;

    // ─── Firebase ───
    private DatabaseReference dbRef;
    private ValueEventListener phListener;
    private ValueEventListener waterListener;
    private ValueEventListener relayListener;

    // ─── Batas pH aman untuk air minum ayam (umumnya 6.5 – 7.5) ───
    private static final float PH_SAFE_MIN  = 6.5f;
    private static final float PH_SAFE_MAX  = 7.5f;
    private static final float PH_ACID_MAX  = 6.5f;   // Di bawah ini = ASAM
    private static final float PH_BASA_MIN  = 7.5f;   // Di atas ini  = BASA

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ─── Bind views ───
        tvPhValue       = findViewById(R.id.tvPhValue);
        tvPhBarLabel    = findViewById(R.id.tvPhBarLabel);
        tvPhStatus      = findViewById(R.id.tvPhStatus);
        ivPhIndicator   = findViewById(R.id.ivPhIndicator);
        tvWaterCapacity    = findViewById(R.id.tvWaterCapacity);
        tvAlertMessage     = findViewById(R.id.tvAlertMessage);
        cardAlert          = findViewById(R.id.cardAlert);
        tvIrrigationStatus = findViewById(R.id.tvIrrigationStatus);
        ivIrrigationIcon   = findViewById(R.id.ivIrrigationIcon);

        // ─── Firebase (persistence dimatikan — data real-time, tidak perlu SQLite cache) ───
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);
        dbRef = FirebaseDatabase.getInstance().getReference();

        // ─── Setup Toolbar ───
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // ─── Notification icon ───
        ImageView ivNotification = findViewById(R.id.ivNotification);
        NotificationSystem.getInstance().registerNotificationIcon(ivNotification);
        ivNotification.setOnClickListener(v -> {
            NotificationSystem.getInstance().showNotificationMenu(DashboardActivity.this, v);
        });

        // ─── Settings icon ───
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ─── Ganti air button ───
        findViewById(R.id.btnGantiAir).setOnClickListener(v ->
                Toast.makeText(this, "Mengganti air...", Toast.LENGTH_SHORT).show()
        );

        // ─── Abaikan button ───
        findViewById(R.id.btnAbaikan).setOnClickListener(v ->
                findViewById(R.id.btnAbaikan).setVisibility(View.GONE)
        );

        // ─── Cek pH button ───
        findViewById(R.id.btnCekPh).setOnClickListener(v -> {
            Toast.makeText(this, "Memerintahkan alat untuk cek pH...", Toast.LENGTH_SHORT).show();
            dbRef.child("kontrol").child("cek_sekarang").setValue(true);
        });

        // ─── Bottom Navigation ───
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_beranda);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_devices) {
                startActivity(new Intent(this, PerangkatActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });

        // ─── Auto-delete check ───
        ManajemenDataActivity.performAutoDeleteIfEnabled(this, dbRef);

        // ─── Mulai listener Firebase real-time ───
        startPhListener();
        startWaterListener();
        startRelayListener();
    }

    // =========================================================
    // LISTENER pH REAL-TIME dari Firebase
    // Path: /kontrol_status/ph_terkini
    // =========================================================
    private void startPhListener() {
        phListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                float ph = 7.0f;
                Object val = snapshot.getValue();
                if (val instanceof Double) ph = ((Double) val).floatValue();
                else if (val instanceof Long)   ph = ((Long)   val).floatValue();
                else if (val instanceof Float)  ph = (Float)   val;

                updatePhUI(ph);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this,
                        "Gagal membaca pH: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        dbRef.child("kontrol_status").child("ph_terkini").addValueEventListener(phListener);
    }

    // =========================================================
    // LISTENER KAPASITAS AIR REAL-TIME dari Firebase
    // Path: /kontrol_status/kapasitas_persen
    // =========================================================
    private void startWaterListener() {
        waterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                float persen = 0f;
                Object val = snapshot.getValue();
                if (val instanceof Double) persen = ((Double) val).floatValue();
                else if (val instanceof Long)   persen = ((Long)   val).floatValue();

                if (tvWaterCapacity != null) {
                    tvWaterCapacity.setText(String.format("%.0f%%", persen));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        dbRef.child("kontrol_status").child("kapasitas_persen").addValueEventListener(waterListener);
    }

    // =========================================================
    // LISTENER STATUS RELAY REAL-TIME dari Firebase
    // Path: /kontrol — relay_isi, relay_buang, relay_minum
    // =========================================================
    private void startRelayListener() {
        relayListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean relayIsi   = getBool(snapshot, "relay_isi");
                boolean relayBuang = getBool(snapshot, "relay_buang");
                boolean relayMinum = getBool(snapshot, "relay_minum");

                updateIrrigationStatus(relayIsi, relayBuang, relayMinum);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        dbRef.child("kontrol").addValueEventListener(relayListener);
    }

    private boolean getBool(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        if (v instanceof Boolean) return (Boolean) v;
        return false;
    }

    // =========================================================
    // UPDATE UI STATUS PENGISIAN AIR
    // =========================================================
    private void updateIrrigationStatus(boolean isi, boolean buang, boolean minum) {
        if (tvIrrigationStatus == null) return;

        boolean anyActive = isi || buang || minum;

        if (anyActive) {
            // Tentukan label berdasarkan relay yang aktif
            StringBuilder label = new StringBuilder();
            if (isi)   label.append("Pengisian ");
            if (buang) label.append("Buang ");
            if (minum) label.append("Minum ");

            tvIrrigationStatus.setText("Aktif");
            tvIrrigationStatus.setTextColor(Color.parseColor("#2ECC71"));

            // Warna ikon hijau saat aktif
            if (ivIrrigationIcon != null) {
                ivIrrigationIcon.setColorFilter(Color.parseColor("#2ECC71"));
                ivIrrigationIcon.setBackgroundResource(R.drawable.bg_icon_green);
            }
        } else {
            tvIrrigationStatus.setText("Tidak Aktif");
            tvIrrigationStatus.setTextColor(Color.parseColor("#9E9E9E"));

            // Warna ikon abu saat tidak aktif
            if (ivIrrigationIcon != null) {
                ivIrrigationIcon.setColorFilter(Color.parseColor("#9E9E9E"));
                ivIrrigationIcon.setBackgroundResource(R.drawable.bg_icon_blue);
            }
        }
    }

    // =========================================================
    // UPDATE SEMUA UI INDIKATOR pH
    // =========================================================
    private void updatePhUI(float ph) {
        // ── 1. Teks nilai pH ──
        String phText = String.format("%.2f pH", ph);
        if (tvPhValue    != null) tvPhValue.setText(phText);
        if (tvPhBarLabel != null) tvPhBarLabel.setText(phText);

        // ── 2. Tentukan kategori & warna ──
        String statusText;
        int    statusColor;
        int    statusBg;
        String alertMsg;
        boolean bahaya = false;

        if (ph < PH_ACID_MAX) {
            // ASAM — berbahaya untuk ayam
            statusText  = "ASAM";
            statusColor = Color.parseColor("#E74C3C");
            statusBg    = Color.parseColor("#FDECEA");
            alertMsg    = String.format("Kadar pH %.2f terlalu ASAM! Segera ganti air agar ayam tidak terdampak.", ph);
            bahaya      = true;
        } else if (ph > PH_BASA_MIN) {
            // BASA — kurang ideal
            statusText  = "BASA";
            statusColor = Color.parseColor("#8E44AD");
            statusBg    = Color.parseColor("#F5EEF8");
            alertMsg    = String.format("Kadar pH %.2f terlalu BASA! Air perlu diseimbangkan.", ph);
            bahaya      = true;
        } else {
            // AMAN / NETRAL
            statusText  = "AMAN";
            statusColor = Color.parseColor("#2ECC71");
            statusBg    = Color.parseColor("#EAFAF1");
            alertMsg    = String.format("pH %.2f dalam batas aman (%.1f – %.1f). Air siap untuk ayam.", ph, PH_SAFE_MIN, PH_SAFE_MAX);
            bahaya      = false;
        }

        // ── 3. Update warna nilai pH ──
        if (tvPhValue != null) tvPhValue.setTextColor(statusColor);

        // ── 4. Update badge status ──
        if (tvPhStatus != null) {
            tvPhStatus.setText(statusText);
            tvPhStatus.setTextColor(statusColor);
            tvPhStatus.setBackgroundColor(statusBg);
        }

        // ── 5. Update pesan peringatan ──
        if (tvAlertMessage != null) tvAlertMessage.setText(alertMsg);

        // ── 6. Posisi indikator pada pH bar (pH 0–14 → 0–100%) ──
        if (ivPhIndicator != null) {
            ivPhIndicator.post(() -> {
                View parent = (View) ivPhIndicator.getParent();
                if (parent == null) return;

                int parentWidth   = parent.getWidth();
                int indicatorW    = ivPhIndicator.getWidth();
                float fraction    = Math.max(0f, Math.min(ph / 14.0f, 1.0f));
                int targetMargin  = (int)(fraction * parentWidth) - indicatorW / 2;
                targetMargin      = Math.max(0, Math.min(targetMargin, parentWidth - indicatorW));

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ivPhIndicator.getLayoutParams();
                lp.gravity      = Gravity.START | Gravity.CENTER_VERTICAL;
                lp.leftMargin   = targetMargin;
                ivPhIndicator.setLayoutParams(lp);
                ivPhIndicator.setBackgroundColor(statusColor);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationSystem.getInstance().unregisterNotificationIcon(findViewById(R.id.ivNotification));
        if (phListener != null)
            dbRef.child("kontrol_status").child("ph_terkini").removeEventListener(phListener);
        if (waterListener != null)
            dbRef.child("kontrol_status").child("kapasitas_persen").removeEventListener(waterListener);
        if (relayListener != null)
            dbRef.child("kontrol").removeEventListener(relayListener);
    }
}
