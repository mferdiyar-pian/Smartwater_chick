package com.example.smartwaterchick;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class KontrolActivity extends BaseActivity {

    // ─── Firebase ───
    private DatabaseReference dbKontrol;
    private DatabaseReference dbJadwal;
    private DatabaseReference dbStatus;
    private ValueEventListener phListener;
    private ValueEventListener waterListener;

    // ─── Views pH ───
    private TextView  tvPhKontrol;       // Nilai pH di header card pH
    private TextView  tvPhBarLabelKontrol;
    private TextView  tvPhStatusKontrol;
    private View      ivPhIndicatorKontrol;

    // ─── Views Air ───
    private TankView  tankView;
    private TextView  tvKapasitasTangki;  // Card 1 "650L / 1000L"
    private View      viewProgressTangki;
    private TextView  tvWaterStatusText;
    private ImageView ivWaterStatusIcon;
    private LinearLayout llWaterBadge;

    // ─── Relay & mode ───
    private SwitchCompat switchIsiAir;
    private SwitchCompat switchBuangAir;
    private SwitchCompat switchOtomatis;
    private SwitchCompat switchKranMinum;
    private boolean isOtomatisAktif     = false;
    private boolean isRelayIsiOn        = false;
    private boolean isRelayBuangOn      = false;
    private boolean isUpdatingFromFirebase = false;

    // ─── Jadwal ───
    private RecyclerView   rvJadwal;
    private JadwalAdapter  jadwalAdapter;
    private final List<JadwalItem> jadwalList = new ArrayList<>();

    // ─── Otomatis Minum ───
    private final Handler  scheduleHandler  = new Handler(Looper.getMainLooper());
    private Runnable scheduleChecker;   // loop pengecekan tiap 10 detik
    private Runnable autoOffRunnable;   // matikan relay setelah 60 detik
    /** true = relay_minum sedang menyala karena jadwal otomatis */
    private boolean  isMinumAutoOn      = false;
    /** Jam+menit jadwal yang terakhir kali dipicu (cegah double-trigger) */
    private int      lastTriggeredJam   = -1;
    private int      lastTriggeredMenit = -1;

    // ─── Konstanta pH air ayam ───
    private static final float PH_SAFE_MIN = 6.5f;
    private static final float PH_SAFE_MAX = 7.5f;
    // Konstanta volume tangki (sesuai wadah yang dipakai)
    private static final float TANK_MAX_LITER = 1.4f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ─── Firebase ───
        dbKontrol = FirebaseDatabase.getInstance().getReference("kontrol");
        dbJadwal  = FirebaseDatabase.getInstance().getReference("jadwal");
        dbStatus  = FirebaseDatabase.getInstance().getReference("kontrol_status");

        // ─── Bind views pH ───
        tvPhKontrol          = findViewById(R.id.tvPhKontrol);
        tvPhBarLabelKontrol  = findViewById(R.id.tvPhBarLabelKontrol);
        tvPhStatusKontrol    = findViewById(R.id.tvPhStatusKontrol);
        ivPhIndicatorKontrol = findViewById(R.id.ivPhIndicatorKontrol);

        // ─── Bind views air ───
        tankView             = findViewById(R.id.tankView);
        tvKapasitasTangki    = findViewById(R.id.tvKapasitasTangki);
        viewProgressTangki   = findViewById(R.id.viewProgressTangki);
        tvWaterStatusText    = findViewById(R.id.tvWaterStatusText);
        ivWaterStatusIcon    = findViewById(R.id.ivWaterStatusIcon);
        llWaterBadge         = findViewById(R.id.llWaterBadge);

        // ─── Back button ───
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            Intent intent = new Intent(KontrolActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // ─── Notification ───
        ImageView ivNotification = findViewById(R.id.ivNotification);
        NotificationSystem.getInstance().registerNotificationIcon(ivNotification);
        ivNotification.setOnClickListener(v ->
                NotificationSystem.getInstance().showNotificationMenu(KontrolActivity.this, v));

        // ─── Settings ───
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(KontrolActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ─── Cek pH button ───
        findViewById(R.id.btnCekPh).setOnClickListener(v -> {
            Toast.makeText(this, "Memerintahkan alat untuk cek pH...", Toast.LENGTH_SHORT).show();
            dbKontrol.child("cek_sekarang").setValue(true);
        });

        // ─── Switch Isi Air ───
        switchIsiAir = findViewById(R.id.switchIsiAir);
        switchIsiAir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingFromFirebase) {
                dbKontrol.child("relay_isi").setValue(isChecked).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(KontrolActivity.this, "Gagal kirim: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(KontrolActivity.this,
                                isChecked ? "Menyalakan Pompa Isi..." : "Mematikan Pompa Isi...",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // ─── Switch Buang Air ───
        switchBuangAir = findViewById(R.id.switchBuangAir);
        switchBuangAir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingFromFirebase) {
                dbKontrol.child("relay_buang").setValue(isChecked).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(KontrolActivity.this, "Gagal kirim: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(KontrolActivity.this,
                                isChecked ? "Menyalakan Pompa Buang..." : "Mematikan Pompa Buang...",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // ─── Switch Otomatis ───
        switchOtomatis = findViewById(R.id.switchOtomatis);
        switchOtomatis.setOnClickListener(v -> {
            boolean checked = switchOtomatis.isChecked();
            isOtomatisAktif = checked;
            dbKontrol.child("otomatis").setValue(checked);
            Toast.makeText(this,
                    checked ? "Mode otomatis aktif" : "Mode otomatis nonaktif",
                    Toast.LENGTH_SHORT).show();
            if (!checked) matikanSemuaJadwal();
        });

        // ─── Switch Kran Minum ───
        switchKranMinum = findViewById(R.id.switchKranMinum);
        switchKranMinum.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingFromFirebase) {
                if (!isChecked && isMinumAutoOn) {
                    // User mematikan manual saat sedang otomatis → batalkan auto-off
                    cancelAutoOff();
                    isMinumAutoOn = false;
                }
                dbKontrol.child("relay_minum").setValue(isChecked).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(KontrolActivity.this, "Gagal kirim: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(KontrolActivity.this,
                                isChecked ? "Menyalakan Kran Minum..." : "Mematikan Kran Minum...",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // ─── Listener relay & mode dari Firebase ───
        dbKontrol.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isUpdatingFromFirebase = true;

                Boolean relayIsi = snapshot.child("relay_isi").getValue(Boolean.class);
                isRelayIsiOn = relayIsi != null && relayIsi;
                switchIsiAir.setChecked(isRelayIsiOn);

                Boolean relayBuang = snapshot.child("relay_buang").getValue(Boolean.class);
                isRelayBuangOn = relayBuang != null && relayBuang;
                switchBuangAir.setChecked(isRelayBuangOn);

                Boolean relayMinum = snapshot.child("relay_minum").getValue(Boolean.class);
                if (relayMinum != null) {
                    switchKranMinum.setChecked(relayMinum);
                }

                isUpdatingFromFirebase = false;

                Boolean otomatis = snapshot.child("otomatis").getValue(Boolean.class);
                if (otomatis != null) {
                    isOtomatisAktif = otomatis;
                    switchOtomatis.setChecked(otomatis);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ─── Jadwal RecyclerView ───
        rvJadwal = findViewById(R.id.rvJadwal);
        rvJadwal.setLayoutManager(new LinearLayoutManager(this));
        jadwalAdapter = new JadwalAdapter(jadwalList, new JadwalAdapter.OnJadwalActionListener() {
            @Override public void onToggle(String id, boolean aktif) {
                dbJadwal.child(id).child("aktif").setValue(aktif);
            }
            @Override public void onEdit(JadwalItem item) { tampilkanDialogJadwal(item); }
            @Override public void onHapus(String id) {
                new AlertDialog.Builder(KontrolActivity.this)
                        .setTitle("Hapus Jadwal")
                        .setMessage("Yakin ingin menghapus jadwal ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            dbJadwal.child(id).removeValue();
                            Toast.makeText(KontrolActivity.this, "Jadwal dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
        });
        rvJadwal.setAdapter(jadwalAdapter);

        dbJadwal.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jadwalList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id      = child.getKey();
                    Integer jam    = child.child("jam").getValue(Integer.class);
                    Integer menit  = child.child("menit").getValue(Integer.class);
                    Boolean aktif  = child.child("aktif").getValue(Boolean.class);
                    if (jam != null && menit != null && aktif != null) {
                        jadwalList.add(new JadwalItem(id, jam, menit, aktif));
                    }
                }
                Collections.sort(jadwalList, (a, b) -> {
                    if (a.jam != b.jam) return Integer.compare(a.jam, b.jam);
                    return Integer.compare(a.menit, b.menit);
                });
                jadwalAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(KontrolActivity.this, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show();
            }
        });

        // ─── Tombol Tambah Jadwal ───
        MaterialButton btnTambahJadwal = findViewById(R.id.btnTambahJadwal);
        btnTambahJadwal.setOnClickListener(v -> tampilkanDialogJadwal(null));

        // ─── Bottom Navigation ───
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_controls);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                Intent i = new Intent(this, DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); return true;
            } else if (id == R.id.nav_controls) {
                return true;
            } else if (id == R.id.nav_devices) {
                startActivity(new Intent(this, PerangkatActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); return true;
            }
            return false;
        });

        // ─── Mulai listener sensor real-time ───
        startPhListener();
        startWaterListener();

        // ─── Mulai pengecekan jadwal otomatis ───
        startScheduleChecker();
    }

    // =========================================================
    // LISTENER pH REAL-TIME
    // =========================================================
    private void startPhListener() {
        phListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                float ph = 7.0f;
                Object val = snapshot.getValue();
                if (val instanceof Double) ph = ((Double) val).floatValue();
                else if (val instanceof Long)  ph = ((Long) val).floatValue();
                else if (val instanceof Float) ph = (Float) val;
                updatePhUI(ph);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        dbStatus.child("ph_terkini").addValueEventListener(phListener);
    }

    // =========================================================
    // LISTENER VOLUME AIR REAL-TIME
    // =========================================================
    private void startWaterListener() {
        waterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                float persen = 0f;

                Object valP = snapshot.child("kapasitas_persen").getValue();
                if (valP instanceof Double) persen = ((Double) valP).floatValue();
                else if (valP instanceof Long) persen = ((Long) valP).floatValue();

                // Hitung liter dari persentase agar selalu sinkron
                float liter = (persen / 100f) * TANK_MAX_LITER;

                updateWaterUI(liter, persen);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        dbStatus.addValueEventListener(waterListener);
    }

    // =========================================================
    // UPDATE UI pH
    // =========================================================
    private void updatePhUI(float ph) {
        String phText  = String.format("%.2f pH", ph);
        String status;
        int    colorInt;
        int    bgColor;

        if (ph < PH_SAFE_MIN) {
            status   = "ASAM";
            colorInt = Color.parseColor("#E74C3C");
            bgColor  = Color.parseColor("#FDECEA");
        } else if (ph > PH_SAFE_MAX) {
            status   = "BASA";
            colorInt = Color.parseColor("#8E44AD");
            bgColor  = Color.parseColor("#F5EEF8");
        } else {
            status   = "AMAN";
            colorInt = Color.parseColor("#2ECC71");
            bgColor  = Color.parseColor("#EAFAF1");
        }

        // Nilai di header card pH
        if (tvPhKontrol != null) { tvPhKontrol.setText(phText); tvPhKontrol.setTextColor(colorInt); }

        // Label di bawah bar
        if (tvPhBarLabelKontrol != null) tvPhBarLabelKontrol.setText(phText);

        // Badge status
        if (tvPhStatusKontrol != null) {
            tvPhStatusKontrol.setText(status);
            tvPhStatusKontrol.setTextColor(colorInt);
            tvPhStatusKontrol.setBackgroundColor(bgColor);
        }

        // Posisi indikator pada bar pH (0–14 → 0–100%)
        if (ivPhIndicatorKontrol != null) {
            ivPhIndicatorKontrol.post(() -> {
                View parent = (View) ivPhIndicatorKontrol.getParent();
                if (parent == null) return;
                int parentWidth  = parent.getWidth();
                int indicatorW   = ivPhIndicatorKontrol.getWidth();
                float fraction   = Math.max(0f, Math.min(ph / 14.0f, 1.0f));
                int margin       = (int)(fraction * parentWidth) - indicatorW / 2;
                margin           = Math.max(0, Math.min(margin, parentWidth - indicatorW));

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ivPhIndicatorKontrol.getLayoutParams();
                lp.gravity    = Gravity.START | Gravity.CENTER_VERTICAL;
                lp.leftMargin = margin;
                ivPhIndicatorKontrol.setLayoutParams(lp);
                ivPhIndicatorKontrol.setBackgroundColor(colorInt);
            });
        }
    }

    // =========================================================
    // UPDATE UI VOLUME AIR
    // =========================================================
    private void updateWaterUI(float liter, float persen) {
        float fraction = Math.max(0f, Math.min(persen / 100f, 1f));

        // TankView
        if (tankView != null) tankView.setFillPercent(fraction);

        // Teks kapasitas (2 desimal karena max hanya 1.4L)
        String kapText = String.format("%.2fL / %.1fL", liter, TANK_MAX_LITER);
        if (tvKapasitasTangki  != null) tvKapasitasTangki.setText(kapText);

        // Progress bar
        updateProgressBar(viewProgressTangki, fraction);

        // Status badge air
        String waterStatus;
        int    waterColor;
        int    waterBg;
        if (persen >= 60f) {
            waterStatus = "Cukup";
            waterColor  = Color.parseColor("#2ECC71");
            waterBg     = Color.parseColor("#EAFAF1");
        } else if (persen >= 30f) {
            waterStatus = "Sedang";
            waterColor  = Color.parseColor("#F39C12");
            waterBg     = Color.parseColor("#FEF9E7");
        } else {
            waterStatus = "Rendah!";
            waterColor  = Color.parseColor("#E74C3C");
            waterBg     = Color.parseColor("#FDECEA");
        }

        if (tvWaterStatusText != null) {
            tvWaterStatusText.setText(waterStatus);
            tvWaterStatusText.setTextColor(waterColor);
        }
        if (ivWaterStatusIcon != null) {
            ivWaterStatusIcon.setColorFilter(waterColor);
        }
        if (llWaterBadge != null) {
            llWaterBadge.setBackgroundColor(waterBg);
        }
    }

    private void updateProgressBar(View progressView, float fraction) {
        if (progressView == null) return;
        progressView.post(() -> {
            View parent = (View) progressView.getParent();
            if (parent == null) return;
            int totalWidth = parent.getWidth();
            ViewGroup.LayoutParams params = progressView.getLayoutParams();
            params.width = (int)(totalWidth * fraction);
            progressView.setLayoutParams(params);
        });
    }

    // =========================================================
    // DIALOG JADWAL
    // =========================================================
    private void tampilkanDialogJadwal(JadwalItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_jadwal, null);
        TextView  tvJudul    = dialogView.findViewById(R.id.tvDialogJudulJadwal);
        TimePicker timePicker = dialogView.findViewById(R.id.timePickerJadwal);
        timePicker.setIs24HourView(true);

        if (item != null) {
            tvJudul.setText("Edit Jadwal");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.setHour(item.jam);
                timePicker.setMinute(item.menit);
            } else {
                timePicker.setCurrentHour(item.jam);
                timePicker.setCurrentMinute(item.menit);
            }
        } else {
            tvJudul.setText("Tambah Jadwal");
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        dialogView.findViewById(R.id.btnBatalJadwal).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSimpanJadwal).setOnClickListener(v -> {
            int jam, menit;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                jam   = timePicker.getHour();
                menit = timePicker.getMinute();
            } else {
                jam   = timePicker.getCurrentHour();
                menit = timePicker.getCurrentMinute();
            }

            if (item != null) {
                dbJadwal.child(item.id).child("jam").setValue(jam);
                dbJadwal.child(item.id).child("menit").setValue(menit);
                Toast.makeText(this, "Jadwal diperbarui", Toast.LENGTH_SHORT).show();
            } else {
                String newId = dbJadwal.push().getKey();
                if (newId != null) {
                    dbJadwal.child(newId).child("jam").setValue(jam);
                    dbJadwal.child(newId).child("menit").setValue(menit);
                    dbJadwal.child(newId).child("aktif").setValue(true);
                    Toast.makeText(this, "Jadwal ditambahkan", Toast.LENGTH_SHORT).show();
                }
            }
            dialog.dismiss();
        });
    }

    private void matikanSemuaJadwal() {
        for (JadwalItem j : jadwalList) {
            dbJadwal.child(j.id).child("aktif").setValue(false);
            j.aktif = false;
        }
        jadwalAdapter.notifyDataSetChanged();
    }

    // =========================================================
    // OTOMATIS PENGISIAN AIR MINUM
    // =========================================================

    /** Mulai loop pengecekan jadwal tiap 10 detik */
    private void startScheduleChecker() {
        scheduleChecker = new Runnable() {
            @Override public void run() {
                checkAndTriggerJadwal();
                scheduleHandler.postDelayed(this, 10_000); // cek setiap 10 detik
            }
        };
        scheduleHandler.post(scheduleChecker);
    }

    /** Hentikan loop pengecekan (dipanggil di onDestroy) */
    private void stopScheduleChecker() {
        if (scheduleChecker != null) scheduleHandler.removeCallbacks(scheduleChecker);
        cancelAutoOff();
    }

    /**
     * Cek apakah ada jadwal aktif yang jam & menitnya cocok dengan waktu HP sekarang.
     * Jika iya dan mode otomatis aktif, nyalakan relay kran minum selama 60 detik.
     */
    private void checkAndTriggerJadwal() {
        if (!isOtomatisAktif) return;
        if (isMinumAutoOn)    return; // sedang menyala, jangan trigger lagi

        Calendar now = Calendar.getInstance(); // waktu lokal HP
        int nowJam   = now.get(Calendar.HOUR_OF_DAY);
        int nowMenit = now.get(Calendar.MINUTE);

        for (JadwalItem j : jadwalList) {
            if (!j.aktif) continue;
            if (j.jam == nowJam && j.menit == nowMenit) {
                // Pastikan jadwal ini belum dipicu di menit yang sama
                if (lastTriggeredJam == nowJam && lastTriggeredMenit == nowMenit) continue;

                lastTriggeredJam   = nowJam;
                lastTriggeredMenit = nowMenit;
                nyalakanMinumOtomatis();
                return; // satu jadwal satu trigger per menit
            }
        }

        // Reset pencatat triggered jika menit sudah berganti
        if (lastTriggeredJam == nowJam && lastTriggeredMenit != nowMenit) {
            lastTriggeredJam   = -1;
            lastTriggeredMenit = -1;
        }
    }

    /** Nyalakan relay kran minum dan jadwalkan auto-off setelah 60 detik */
    private void nyalakanMinumOtomatis() {
        isMinumAutoOn = true;
        dbKontrol.child("relay_minum").setValue(true);
        Toast.makeText(this, "⏱ Jadwal: Kran minum menyala 60 detik", Toast.LENGTH_LONG).show();

        autoOffRunnable = () -> {
            matikanMinum();
            Toast.makeText(this, "✅ Kran minum otomatis dimatikan", Toast.LENGTH_SHORT).show();
        };
        scheduleHandler.postDelayed(autoOffRunnable, 60_000); // 60 detik
    }

    /** Matikan relay kran minum dan batalkan pending auto-off */
    private void matikanMinum() {
        isMinumAutoOn = false;
        cancelAutoOff();
        dbKontrol.child("relay_minum").setValue(false);
    }

    /** Batalkan runnable auto-off jika masih pending */
    private void cancelAutoOff() {
        if (autoOffRunnable != null) {
            scheduleHandler.removeCallbacks(autoOffRunnable);
            autoOffRunnable = null;
        }
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationSystem.getInstance().unregisterNotificationIcon(findViewById(R.id.ivNotification));
        if (phListener    != null) dbStatus.child("ph_terkini").removeEventListener(phListener);
        if (waterListener != null) dbStatus.removeEventListener(waterListener);
        stopScheduleChecker(); // hentikan loop jadwal & batalkan auto-off
    }
}
