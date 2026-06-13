package com.example.smartwaterchick;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PerangkatActivity extends BaseActivity {

    // ─── Firebase ───
    private DatabaseReference dbRef;
    private ValueEventListener statusListener;
    private ValueEventListener monitoringListener;
    private ValueEventListener relayListener;

    // ─── Views: Card Status ───
    private TextView tvDeviceStatus;
    private ImageView ivWifiIcon;
    private TextView tvWifiStatus;
    private ImageView ivSignalIcon;
    private TextView tvSignalStatus;

    // ─── Views: Monitoring Komponen ───
    private ImageView ivPhIcon, ivUltrasonicIcon, ivPumpIcon;
    private ImageView ivValveBuangIcon, ivValveMinumIcon;
    private TextView tvPhSensorStatus, tvPhValue;
    private TextView tvUltrasonicStatus, tvUltrasonicValue;
    private TextView tvPumpStatus, tvPumpSubtitle;
    private TextView tvValveBuangStatus, tvValveBuangSubtitle;
    private TextView tvValveMinumStatus, tvValveMinumSubtitle;

    // ─── Views: Log ───
    private LinearLayout logContainer;
    private TextView tvLogPlaceholder;

    // ─── Data log ───
    private final List<LogItem> allLogs = new ArrayList<>();

    // ─── Inner class data log ───
    private static class LogItem {
        String title;
        String detail;
        boolean isOk;   // true=hijau, false=oranye/merah

        LogItem(String title, String detail, boolean isOk) {
            this.title = title;
            this.detail = detail;
            this.isOk = isOk;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perangkat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ─── Bind views ───
        tvDeviceStatus     = findViewById(R.id.tvDeviceStatus);
        ivWifiIcon         = findViewById(R.id.ivWifiIcon);
        tvWifiStatus       = findViewById(R.id.tvWifiStatus);
        ivSignalIcon       = findViewById(R.id.ivSignalIcon);
        tvSignalStatus     = findViewById(R.id.tvSignalStatus);

        ivPhIcon           = findViewById(R.id.ivPhIcon);
        tvPhSensorStatus   = findViewById(R.id.tvPhSensorStatus);
        tvPhValue          = findViewById(R.id.tvPhValue);

        ivUltrasonicIcon   = findViewById(R.id.ivUltrasonicIcon);
        tvUltrasonicStatus = findViewById(R.id.tvUltrasonicStatus);
        tvUltrasonicValue  = findViewById(R.id.tvUltrasonicValue);

        ivPumpIcon         = findViewById(R.id.ivPumpIcon);
        tvPumpStatus       = findViewById(R.id.tvPumpStatus);
        tvPumpSubtitle     = findViewById(R.id.tvPumpSubtitle);

        ivValveBuangIcon     = findViewById(R.id.ivValveBuangIcon);
        tvValveBuangStatus   = findViewById(R.id.tvValveBuangStatus);
        tvValveBuangSubtitle = findViewById(R.id.tvValveBuangSubtitle);

        ivValveMinumIcon     = findViewById(R.id.ivValveMinumIcon);
        tvValveMinumStatus   = findViewById(R.id.tvValveMinumStatus);
        tvValveMinumSubtitle = findViewById(R.id.tvValveMinumSubtitle);

        logContainer       = findViewById(R.id.logContainer);
        tvLogPlaceholder   = findViewById(R.id.tvLogPlaceholder);

        // ─── Firebase ───
        dbRef = FirebaseDatabase.getInstance().getReference();

        // ─── Back ───
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            Intent intent = new Intent(PerangkatActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // ─── Notifikasi ───
        findViewById(R.id.ivNotification).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(PerangkatActivity.this, v);
            popup.getMenu().add("Peringatan: pH Air di Tangki 1 Rendah (5.5)");
            popup.getMenu().add("Info: Kapasitas Air berkurang.");
            popup.show();
        });

        // ─── Settings ───
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            startActivity(new Intent(PerangkatActivity.this, PengaturanActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ─── Lihat Semua Log ───
        findViewById(R.id.tvLihatSemua).setOnClickListener(v -> showAllLogsDialog());

        // ─── Bottom Navigation ───
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_devices);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_beranda) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalisisActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_controls) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_devices) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, PengaturanActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });

        // ─── Mulai listener Firebase ───
        startStatusListener();
        startMonitoringListener();
        startRelayListener();
    }

    // =========================================================
    // LISTENER: /kontrol_status — pH + kapasitas air
    // Menentukan: Online/Offline, WiFi, Sinyal, Status sensor
    // =========================================================
    private void startStatusListener() {
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    setDeviceOffline();
                    return;
                }

                // Ambil nilai sensor dan status hardware dari Firebase
                Object phRaw = snapshot.child("ph_terkini").getValue();
                Object literRaw = snapshot.child("kapasitas_liter").getValue();
                Object persenRaw = snapshot.child("kapasitas_persen").getValue();
                Object lastSeenRaw = snapshot.child("last_seen").getValue();
                Object rssiRaw = snapshot.child("rssi").getValue();
                Object sensorPhConnRaw = snapshot.child("sensor_ph_connected").getValue();
                Object sensorUltraConnRaw = snapshot.child("sensor_ultrasonic_connected").getValue();

                // Tentukan status online berdasarkan timestamp "last_seen"
                boolean isOnline = false;
                if (lastSeenRaw != null) {
                    long lastSeen = toLong(lastSeenRaw);
                    long currentEpoch = System.currentTimeMillis() / 1000;
                    // Toleransi 90 detik karena perangkat mengirim data setiap 30 detik
                    isOnline = Math.abs(currentEpoch - lastSeen) < 90;
                } else {
                    // Fallback jika field last_seen belum terisi
                    isOnline = (phRaw != null) || (literRaw != null) || (persenRaw != null);
                }

                if (isOnline) {
                    setDeviceOnline();

                    // Update Status WiFi
                    tvWifiStatus.setText("Terhubung");
                    tvWifiStatus.setTextColor(Color.parseColor("#2ECC71"));
                    ivWifiIcon.setColorFilter(Color.parseColor("#2ECC71"));

                    // Update Status Sinyal berdasarkan RSSI
                    int rssi = -100;
                    if (rssiRaw != null) {
                        rssi = toInt(rssiRaw);
                    }
                    if (rssi >= -60) {
                        tvSignalStatus.setText("Kuat");
                        tvSignalStatus.setTextColor(Color.parseColor("#2ECC71"));
                        ivSignalIcon.setColorFilter(Color.parseColor("#2ECC71"));
                    } else if (rssi >= -75) {
                        tvSignalStatus.setText("Sedang");
                        tvSignalStatus.setTextColor(Color.parseColor("#F1C40F"));
                        ivSignalIcon.setColorFilter(Color.parseColor("#F1C40F"));
                    } else if (rssi >= -90) {
                        tvSignalStatus.setText("Lemah");
                        tvSignalStatus.setTextColor(Color.parseColor("#E67E22"));
                        ivSignalIcon.setColorFilter(Color.parseColor("#E67E22"));
                    } else {
                        tvSignalStatus.setText("Tidak Ada");
                        tvSignalStatus.setTextColor(Color.parseColor("#E74C3C"));
                        ivSignalIcon.setColorFilter(Color.parseColor("#E74C3C"));
                    }

                    // Update status sensor pH
                    boolean phConnected = false;
                    if (sensorPhConnRaw != null) {
                        phConnected = (boolean) sensorPhConnRaw;
                    } else {
                        phConnected = (phRaw != null);
                    }

                    if (phConnected && phRaw != null) {
                        float ph = toFloat(phRaw);
                        tvPhValue.setText(String.format(Locale.US, "pH terkini: %.2f", ph));
                        setComponentConnected(ivPhIcon, tvPhSensorStatus, "#2ECC71", "Terhubung");
                    } else {
                        tvPhValue.setText("Sensor bermasalah / terputus");
                        setComponentDisconnected(ivPhIcon, tvPhSensorStatus);
                    }

                    // Update status sensor Ultrasonik (kapasitas air)
                    boolean ultraConnected = false;
                    if (sensorUltraConnRaw != null) {
                        ultraConnected = (boolean) sensorUltraConnRaw;
                    } else {
                        ultraConnected = (literRaw != null || persenRaw != null);
                    }

                    if (ultraConnected && (literRaw != null || persenRaw != null)) {
                        float persen = (persenRaw != null) ? toFloat(persenRaw) : 0f;
                        float liter  = (literRaw  != null) ? toFloat(literRaw)  : 0f;
                        tvUltrasonicValue.setText(
                                String.format(Locale.US, "Kapasitas: %.1fL (%.0f%%)", liter, persen));
                        setComponentConnected(ivUltrasonicIcon, tvUltrasonicStatus, "#1B5BCE", "Terhubung");
                    } else {
                        tvUltrasonicValue.setText("Sensor bermasalah / terputus");
                        setComponentDisconnected(ivUltrasonicIcon, tvUltrasonicStatus);
                    }

                } else {
                    setDeviceOffline();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDeviceOffline();
            }
        };
        dbRef.child("kontrol_status").addValueEventListener(statusListener);
    }

    // =========================================================
    // LISTENER: /monitoring — Ambil log terbaru dari Firebase
    // =========================================================
    private void startMonitoringListener() {
        monitoringListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Hapus hanya log sensor lama agar log relay/perangkat tidak hilang
                java.util.Iterator<LogItem> iterator = allLogs.iterator();
                while (iterator.hasNext()) {
                    LogItem item = iterator.next();
                    if (item.detail.startsWith("Data sensor")) {
                        iterator.remove();
                    }
                }

                if (snapshot.exists()) {
                    // Gunakan TreeMap terbalik agar log terbaru di atas
                    TreeMap<String, DataSnapshot> sorted = new TreeMap<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        sorted.put(child.getKey(), child);
                    }

                    // Buat log dari data monitoring
                    List<String> keys = new ArrayList<>(sorted.descendingKeySet());
                    for (String key : keys) {
                        DataSnapshot entry = sorted.get(key);
                        if (entry == null) continue;

                        Object phRaw  = entry.child("ph").getValue();
                        Object tanggal = entry.child("tanggal").getValue();
                        Object waktu   = entry.child("waktu").getValue();

                        if (phRaw == null) continue;

                        float ph = toFloat(phRaw);
                        String tgl = (tanggal != null) ? tanggal.toString() : "-";
                        String wkt = (waktu   != null) ? waktu.toString()   : "-";

                        String title;
                        boolean isOk;
                        if (ph >= 6.5f && ph <= 7.5f) {
                            title = String.format(Locale.US, "pH Normal (%.2f) — Air Aman", ph);
                            isOk = true;
                        } else if (ph < 6.5f) {
                            title = String.format(Locale.US, "pH Asam (%.2f) — Perlu Perhatian", ph);
                            isOk = false;
                        } else {
                            title = String.format(Locale.US, "pH Basa (%.2f) — Perlu Perhatian", ph);
                            isOk = false;
                        }

                        allLogs.add(new LogItem(title, "Data sensor • " + tgl + " " + wkt, isOk));
                    }
                }

                renderPreviewLogs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvLogPlaceholder.setVisibility(View.VISIBLE);
                tvLogPlaceholder.setText("Gagal memuat log: " + error.getMessage());
            }
        };
        dbRef.child("monitoring").addValueEventListener(monitoringListener);
    }

    // =========================================================
    // LISTENER: /kontrol — status relay pompa
    // =========================================================
    private void startRelayListener() {
        relayListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvPumpStatus.setText("Tidak diketahui");
                    tvPumpStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvPumpSubtitle.setText("Data relay tidak tersedia");
                    ivPumpIcon.setColorFilter(Color.parseColor("#9E9E9E"));

                    tvValveBuangStatus.setText("Tidak diketahui");
                    tvValveBuangStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvValveBuangSubtitle.setText("Data relay tidak tersedia");
                    ivValveBuangIcon.setColorFilter(Color.parseColor("#9E9E9E"));

                    tvValveMinumStatus.setText("Tidak diketahui");
                    tvValveMinumStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvValveMinumSubtitle.setText("Data relay tidak tersedia");
                    ivValveMinumIcon.setColorFilter(Color.parseColor("#9E9E9E"));
                    return;
                }

                boolean relayIsi   = getBool(snapshot, "relay_isi");
                boolean relayBuang = getBool(snapshot, "relay_buang");
                boolean relayMinum = getBool(snapshot, "relay_minum");
                boolean otomatis   = getBool(snapshot, "otomatis");

                boolean needRenderLogs = false;
                String now = getCurrentTimeWITA();

                // ─── Pompa Air (Relay Isi) ───
                if (relayIsi) {
                    tvPumpStatus.setText("Aktif");
                    tvPumpStatus.setTextColor(Color.parseColor("#2ECC71"));
                    tvPumpSubtitle.setText(otomatis ? "Mode Otomatis · Menyala" : "Mode Manual · Menyala");
                    ivPumpIcon.setColorFilter(Color.parseColor("#2ECC71"));

                    boolean alreadyLogged = false;
                    for (LogItem li : allLogs) {
                        if (li.title.equals("Pompa Air (Relay) dinyalakan")) {
                            alreadyLogged = true;
                            break;
                        }
                    }
                    if (!alreadyLogged) {
                        allLogs.add(0, new LogItem(
                                "Pompa Air (Relay) dinyalakan",
                                "Relay menyala • " + now,
                                true));
                        needRenderLogs = true;
                    }
                } else {
                    tvPumpStatus.setText("Standby");
                    tvPumpStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvPumpSubtitle.setText(otomatis ? "Mode Otomatis · Mati" : "Mode Manual · Mati");
                    ivPumpIcon.setColorFilter(Color.parseColor("#9E9E9E"));
                }

                // ─── Valve Buang Air (Relay Buang) ───
                if (relayBuang) {
                    tvValveBuangStatus.setText("Aktif");
                    tvValveBuangStatus.setTextColor(Color.parseColor("#2ECC71"));
                    tvValveBuangSubtitle.setText(otomatis ? "Mode Otomatis · Menyala" : "Mode Manual · Menyala");
                    ivValveBuangIcon.setColorFilter(Color.parseColor("#2ECC71"));

                    boolean alreadyLogged = false;
                    for (LogItem li : allLogs) {
                        if (li.title.equals("Valve Buang Air (Relay) dinyalakan")) {
                            alreadyLogged = true;
                            break;
                        }
                    }
                    if (!alreadyLogged) {
                        allLogs.add(0, new LogItem(
                                "Valve Buang Air (Relay) dinyalakan",
                                "Relay menyala • " + now,
                                true));
                        needRenderLogs = true;
                    }
                } else {
                    tvValveBuangStatus.setText("Standby");
                    tvValveBuangStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvValveBuangSubtitle.setText(otomatis ? "Mode Otomatis · Mati" : "Mode Manual · Mati");
                    ivValveBuangIcon.setColorFilter(Color.parseColor("#9E9E9E"));
                }

                // ─── Valve Kran Air Minum (Relay Minum) ───
                if (relayMinum) {
                    tvValveMinumStatus.setText("Aktif");
                    tvValveMinumStatus.setTextColor(Color.parseColor("#2ECC71"));
                    tvValveMinumSubtitle.setText(otomatis ? "Mode Otomatis · Menyala" : "Mode Manual · Menyala");
                    ivValveMinumIcon.setColorFilter(Color.parseColor("#2ECC71"));

                    boolean alreadyLogged = false;
                    for (LogItem li : allLogs) {
                        if (li.title.equals("Valve Kran Air Minum (Relay) dinyalakan")) {
                            alreadyLogged = true;
                            break;
                        }
                    }
                    if (!alreadyLogged) {
                        allLogs.add(0, new LogItem(
                                "Valve Kran Air Minum (Relay) dinyalakan",
                                "Relay menyala • " + now,
                                true));
                        needRenderLogs = true;
                    }
                } else {
                    tvValveMinumStatus.setText("Standby");
                    tvValveMinumStatus.setTextColor(Color.parseColor("#9E9E9E"));
                    tvValveMinumSubtitle.setText(otomatis ? "Mode Otomatis · Mati" : "Mode Manual · Mati");
                    ivValveMinumIcon.setColorFilter(Color.parseColor("#9E9E9E"));
                }

                if (needRenderLogs) {
                    renderPreviewLogs();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        dbRef.child("kontrol").addValueEventListener(relayListener);
    }

    // =========================================================
    // HELPER: Render 3 log terbaru di card
    // =========================================================
    private void renderPreviewLogs() {
        // Hapus semua view kecuali placeholder
        logContainer.removeAllViews();
        logContainer.addView(tvLogPlaceholder);

        if (allLogs.isEmpty()) {
            tvLogPlaceholder.setVisibility(View.VISIBLE);
            tvLogPlaceholder.setText("Belum ada log data sensor.");
            return;
        }

        tvLogPlaceholder.setVisibility(View.GONE);

        int count = Math.min(3, allLogs.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                // Divider
                View divider = new View(this);
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                dp.setMargins(0, 0, 0, dpToPx(14));
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
                logContainer.addView(divider);
            }
            logContainer.addView(buildLogItemView(allLogs.get(i), i < count - 1));
        }
    }

    // =========================================================
    // HELPER: Ambil waktu sekarang sesuai WITA (Kalimantan Selatan)
    // =========================================================
    private String getCurrentTimeWITA() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Makassar"));
        return sdf.format(new Date());
    }

    // =========================================================
    // HELPER: Build view satu item log
    // =========================================================
    private View buildLogItemView(LogItem item, boolean withBottomMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (withBottomMargin) rowParams.setMargins(0, 0, 0, dpToPx(14));
        row.setLayoutParams(rowParams);

        // Ikon status
        ImageView icon = new ImageView(this);
        int iconRes = item.isOk ? R.drawable.ic_check_circle : R.drawable.ic_notification;
        int iconColor = item.isOk ? Color.parseColor("#2ECC71") : Color.parseColor("#E67E22");
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(22), dpToPx(22));
        iconParams.setMargins(0, dpToPx(2), dpToPx(12), 0);
        icon.setLayoutParams(iconParams);
        row.addView(icon);

        // Teks
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(tcParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.title);
        tvTitle.setTextSize(13f);
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        textCol.addView(tvTitle);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(item.detail);
        tvDetail.setTextSize(11f);
        tvDetail.setTextColor(Color.parseColor("#757575"));
        textCol.addView(tvDetail);

        row.addView(textCol);
        return row;
    }

    // =========================================================
    // HELPER: Dialog Lihat Semua Log
    // =========================================================
    private void showAllLogsDialog() {
        if (allLogs.isEmpty()) {
            Toast.makeText(this, "Belum ada log tersedia.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Semua Log Perangkat");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        container.setPadding(pad, pad, pad, pad);

        for (int i = 0; i < allLogs.size(); i++) {
            if (i > 0) {
                View divider = new View(this);
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                dp.setMargins(0, 0, 0, dpToPx(12));
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                container.addView(divider);
            }
            container.addView(buildLogItemView(allLogs.get(i), i < allLogs.size() - 1));
        }

        scrollView.addView(container);
        builder.setView(scrollView);
        builder.setPositiveButton("Tutup", null);
        builder.show();
    }

    // =========================================================
    // HELPER: Set status perangkat Online
    // =========================================================
    private void setDeviceOnline() {
        // Badge status
        tvDeviceStatus.setText("Online");
        tvDeviceStatus.setTextColor(Color.parseColor("#2ECC71"));

        // Tambah log jika perlu
        addOnlineLogIfNeeded();
    }

    // =========================================================
    // HELPER: Set status perangkat Offline
    // =========================================================
    private void setDeviceOffline() {
        // Badge status
        tvDeviceStatus.setText("Offline");
        tvDeviceStatus.setTextColor(Color.parseColor("#E74C3C"));

        // WiFi tidak terhubung
        ivWifiIcon.setColorFilter(Color.parseColor("#E74C3C"));
        tvWifiStatus.setText("Offline");
        tvWifiStatus.setTextColor(Color.parseColor("#E74C3C"));

        // Sinyal tidak ada
        ivSignalIcon.setColorFilter(Color.parseColor("#E74C3C"));
        tvSignalStatus.setText("Tidak Ada");
        tvSignalStatus.setTextColor(Color.parseColor("#E74C3C"));

        // Semua sensor tidak terhubung
        setComponentDisconnected(ivPhIcon, tvPhSensorStatus);
        tvPhValue.setText("Tidak ada data");
        setComponentDisconnected(ivUltrasonicIcon, tvUltrasonicStatus);
        tvUltrasonicValue.setText("Tidak ada data");
        tvPumpStatus.setText("Tidak diketahui");
        tvPumpStatus.setTextColor(Color.parseColor("#9E9E9E"));
        tvPumpSubtitle.setText("Perangkat offline");
        ivPumpIcon.setColorFilter(Color.parseColor("#9E9E9E"));

        tvValveBuangStatus.setText("Tidak diketahui");
        tvValveBuangStatus.setTextColor(Color.parseColor("#9E9E9E"));
        tvValveBuangSubtitle.setText("Perangkat offline");
        ivValveBuangIcon.setColorFilter(Color.parseColor("#9E9E9E"));

        tvValveMinumStatus.setText("Tidak diketahui");
        tvValveMinumStatus.setTextColor(Color.parseColor("#9E9E9E"));
        tvValveMinumSubtitle.setText("Perangkat offline");
        ivValveMinumIcon.setColorFilter(Color.parseColor("#9E9E9E"));

        // Log offline
        addOfflineLogIfNeeded();
    }

    private void addOnlineLogIfNeeded() {
        for (LogItem li : allLogs) {
            if (li.title.equals("Perangkat Terhubung")) return;
        }
        String now = getCurrentTimeWITA();
        allLogs.add(0, new LogItem("Perangkat Terhubung",
                "Koneksi berhasil ke Firebase • " + now, true));
        renderPreviewLogs();
    }

    private void addOfflineLogIfNeeded() {
        for (LogItem li : allLogs) {
            if (li.title.equals("Perangkat Offline")) return;
        }
        String now = getCurrentTimeWITA();
        allLogs.add(0, new LogItem("Perangkat Offline",
                "Tidak ada data dari sensor • " + now, false));
        renderPreviewLogs();
    }

    // =========================================================
    // HELPER: Set komponen terhubung / tidak
    // =========================================================
    private void setComponentConnected(ImageView icon, TextView status,
                                       String colorHex, String label) {
        icon.setColorFilter(Color.parseColor(colorHex));
        status.setText(label);
        status.setTextColor(Color.parseColor(colorHex));
    }

    private void setComponentDisconnected(ImageView icon, TextView status) {
        icon.setColorFilter(Color.parseColor("#9E9E9E"));
        status.setText("Tidak Terhubung");
        status.setTextColor(Color.parseColor("#9E9E9E"));
    }

    // =========================================================
    // HELPER: Konversi Object Firebase → float
    // =========================================================
    private float toFloat(Object val) {
        if (val instanceof Double)  return ((Double) val).floatValue();
        if (val instanceof Long)    return ((Long)   val).floatValue();
        if (val instanceof Float)   return (Float) val;
        if (val instanceof Integer) return ((Integer) val).floatValue();
        return 0f;
    }

    private long toLong(Object val) {
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Double)  return ((Double) val).longValue();
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Float)   return ((Float) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private int toInt(Object val) {
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long)    return ((Long) val).intValue();
        if (val instanceof Double)  return ((Double) val).intValue();
        if (val instanceof Float)   return ((Float) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean getBool(DataSnapshot snapshot, String key) {
        Object val = snapshot.child(key).getValue();
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // =========================================================
    // LIFECYCLE — Lepas semua listener
    // =========================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null)
            dbRef.child("kontrol_status").removeEventListener(statusListener);
        if (monitoringListener != null)
            dbRef.child("monitoring").removeEventListener(monitoringListener);
        if (relayListener != null)
            dbRef.child("kontrol").removeEventListener(relayListener);
    }
}
