package com.example.smartwaterchick;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

public class NotificationSystem {

    // ─────────────────────────────────────────────────────────────
    // Model
    // ─────────────────────────────────────────────────────────────
    public static class NotifItem {
        public final String  title;
        public final String  subtitle;
        public final boolean isCritical;
        public final String  time;

        public NotifItem(String title, String subtitle, boolean isCritical) {
            this.title      = title;
            this.subtitle   = subtitle;
            this.isCritical = isCritical;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Makassar"));
            this.time = sdf.format(new Date());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────
    private static NotificationSystem instance;
    public static synchronized NotificationSystem getInstance() {
        if (instance == null) instance = new NotificationSystem();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    // State Firebase
    // ─────────────────────────────────────────────────────────────
    private final DatabaseReference dbRef;
    private final List<ImageView>   registeredIcons = new ArrayList<>();

    private boolean isOnline     = true;
    private float   ph           = 7.0f;
    private float   waterPercent = 100f;
    private boolean relayIsi     = false;
    private boolean relayBuang   = false;
    private boolean relayMinum   = false;

    private NotificationSystem() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        startListening();
    }

    // ─────────────────────────────────────────────────────────────
    // Firebase listeners
    // ─────────────────────────────────────────────────────────────
    private void startListening() {
        dbRef.child("kontrol_status").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) return;
                Object phRaw        = s.child("ph_terkini").getValue();
                Object persenRaw    = s.child("kapasitas_persen").getValue();
                Object wifiOnlineRaw = s.child("wifi_online").getValue();
                Object lastSeen     = s.child("last_seen").getValue();

                // Utamakan field wifi_online (boolean langsung dari ESP32 heartbeat 5 detik)
                if (wifiOnlineRaw instanceof Boolean) {
                    isOnline = (Boolean) wifiOnlineRaw;
                } else if (lastSeen != null) {
                    long ls = toLong(lastSeen);
                    if (ls > 9999999999L) ls = ls / 1000;
                    long currentEpoch = System.currentTimeMillis() / 1000;
                    long diffUtc  = Math.abs(currentEpoch - ls);
                    long diffWita = Math.abs((currentEpoch + 28800) - ls);
                    isOnline = (diffUtc < 45) || (diffWita < 45);
                } else {
                    isOnline = phRaw != null;
                }
                if (phRaw     != null) ph          = toFloat(phRaw);
                if (persenRaw != null) waterPercent = toFloat(persenRaw);
                updateRegisteredIcons();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        dbRef.child("kontrol").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) return;
                relayIsi   = getBool(s, "relay_isi");
                relayBuang = getBool(s, "relay_buang");
                relayMinum = getBool(s, "relay_minum");
                updateRegisteredIcons();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Build alert list
    // ─────────────────────────────────────────────────────────────
    public synchronized List<NotifItem> getFilteredAlerts(Context context) {
        List<NotifItem> list = new ArrayList<>();
        SharedPreferences prefs =
                SecurePrefsHelper.getPrefs(context, "SmartWaterNotifPrefs");
        if (!prefs.getBoolean("notif_push", true)) return list;

        if (prefs.getBoolean("notif_wifi", true) && !isOnline)
            list.add(new NotifItem(
                    "Alat terputus dari WiFi",
                    "Perangkat tidak merespons > 90 detik",
                    true));

        if (prefs.getBoolean("notif_critical", true)) {
            if (ph < 6.5f)
                list.add(new NotifItem(
                        String.format(Locale.US, "pH terlalu asam (%.2f)", ph),
                        "Batas aman 6.5–7.5 · Segera cek air",
                        true));
            else if (ph > 7.5f)
                list.add(new NotifItem(
                        String.format(Locale.US, "pH terlalu basa (%.2f)", ph),
                        "Batas aman 6.5–7.5 · Segera cek air",
                        true));
        }

        if (prefs.getBoolean("notif_water_level", true) && waterPercent < 20f)
            list.add(new NotifItem(
                    String.format(Locale.US, "Air hampir habis (%.0f%%)", waterPercent),
                    "Aktifkan pompa pengisian segera",
                    true));

        if (prefs.getBoolean("notif_relay", true)) {
            if (relayIsi)
                list.add(new NotifItem("Pompa air aktif",
                        "Relay isi sedang menyala", false));
            if (relayBuang)
                list.add(new NotifItem("Valve buang aktif",
                        "Relay buang sedang menyala", false));
            if (relayMinum)
                list.add(new NotifItem("Valve minum aktif",
                        "Kran air minum sedang terbuka", false));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // Icon management
    // ─────────────────────────────────────────────────────────────
    public synchronized void registerNotificationIcon(ImageView iv) {
        if (!registeredIcons.contains(iv)) registeredIcons.add(iv);
        updateIconState(iv);
    }

    public synchronized void unregisterNotificationIcon(ImageView iv) {
        registeredIcons.remove(iv);
    }

    private void updateRegisteredIcons() {
        for (ImageView iv : registeredIcons) updateIconState(iv);
    }

    private void updateIconState(ImageView iv) {
        if (iv == null) return;
        iv.post(() -> {
            List<NotifItem> alerts = getFilteredAlerts(iv.getContext());
            iv.setImageResource(alerts.isEmpty()
                    ? R.drawable.ic_notification
                    : R.drawable.ic_notification_badge);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Tampilkan popup notifikasi (selalu di bawah ikon bell)
    // ─────────────────────────────────────────────────────────────
    public void showNotificationMenu(Context context, View anchor) {
        List<NotifItem> alerts = getFilteredAlerts(context);
        openPopup(context, anchor, alerts, false);
    }

    /**
     * @param showAll  false = tampilkan maks 3 + tombol "Lihat selengkapnya"
     *                 true  = tampilkan semua dalam ScrollView (masih popup yg sama)
     */
    private void openPopup(Context context, View anchor,
                           List<NotifItem> alerts, boolean showAll) {
        float d         = context.getResources().getDisplayMetrics().density;
        int   panelW    = (int)(300 * d);
        int   screenH   = context.getResources().getDisplayMetrics().heightPixels;
        // max tinggi popup ≈ 60% layar (agar tidak keluar bawah)
        int   maxHeight = (int)(screenH * 0.60f);

        // ── Bungkus isi dalam ScrollView agar bisa di-scroll saat semua tampil ──
        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        // Header
        listLayout.addView(buildHeader(context, alerts, d));
        listLayout.addView(makeDivider(context, d));

        if (alerts.isEmpty()) {
            listLayout.addView(buildEmptyRow(context, d));
        } else {
            List<NotifItem> displayed = showAll
                    ? alerts
                    : alerts.subList(0, Math.min(3, alerts.size()));

            for (int i = 0; i < displayed.size(); i++) {
                listLayout.addView(buildRow(context, displayed.get(i), d));
                if (i < displayed.size() - 1) {
                    listLayout.addView(makeDivider(context, d));
                }
            }

            // Tombol "Lihat selengkapnya" hanya saat preview dan ada lebih dari 3
            if (!showAll && alerts.size() > 3) {
                listLayout.addView(makeDivider(context, d));

                TextView tvMore = new TextView(context);
                tvMore.setText("Lihat selengkapnya (" + (alerts.size() - 3) + " lainnya) ↓");
                tvMore.setTextSize(12f);
                tvMore.setTextColor(Color.parseColor("#1B5BCE"));
                tvMore.setTypeface(null, Typeface.BOLD);
                tvMore.setGravity(Gravity.CENTER);
                tvMore.setPadding(0, (int)(10*d), 0, (int)(10*d));
                // Klik → tutup popup ini, buka ulang dengan showAll=true di posisi SAMA
                tvMore.setOnClickListener(v -> {
                    Object tag = listLayout.getTag();
                    if (tag instanceof PopupWindow) ((PopupWindow) tag).dismiss();
                    openPopup(context, anchor, alerts, true);
                });
                listLayout.addView(tvMore);
            }

            // Tombol "Sembunyikan" saat tampil semua
            if (showAll) {
                listLayout.addView(makeDivider(context, d));

                TextView tvCollapse = new TextView(context);
                tvCollapse.setText("↑ Sembunyikan");
                tvCollapse.setTextSize(12f);
                tvCollapse.setTextColor(Color.parseColor("#6B7280"));
                tvCollapse.setTypeface(null, Typeface.BOLD);
                tvCollapse.setGravity(Gravity.CENTER);
                tvCollapse.setPadding(0, (int)(10*d), 0, (int)(10*d));
                tvCollapse.setOnClickListener(v -> {
                    Object tag = listLayout.getTag();
                    if (tag instanceof PopupWindow) ((PopupWindow) tag).dismiss();
                    openPopup(context, anchor, alerts, false);
                });
                listLayout.addView(tvCollapse);
            }
        }

        // Bungkus dalam ScrollView agar tidak keluar layar
        MaxHeightScrollView scrollView = new MaxHeightScrollView(context, maxHeight);
        scrollView.setBackgroundResource(R.drawable.bg_notification_panel);
        scrollView.addView(listLayout);

        // ── PopupWindow ────────────────────────────────────────────
        PopupWindow popup = new PopupWindow(
                scrollView,
                panelW,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(10 * d);
        popup.setOutsideTouchable(true);

        // Simpan referensi popup di tag agar tombol bisa dismiss
        listLayout.setTag(popup);

        popup.showAsDropDown(anchor, 0, (int)(4 * d));
    }

    // ─────────────────────────────────────────────────────────────
    // Komponen UI
    // ─────────────────────────────────────────────────────────────

    private View buildHeader(Context ctx, List<NotifItem> alerts, float d) {
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding((int)(14*d), (int)(10*d), (int)(14*d), (int)(10*d));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("Notifikasi");
        tvTitle.setTextSize(13f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(tvTitle);

        if (!alerts.isEmpty()) {
            TextView tvBadge = new TextView(ctx);
            tvBadge.setText(String.valueOf(alerts.size()));
            tvBadge.setTextSize(10f);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setGravity(Gravity.CENTER);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_red);
            int sz = (int)(18 * d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
            lp.setMarginEnd((int)(2 * d));
            tvBadge.setLayoutParams(lp);
            header.addView(tvBadge);
        }

        return header;
    }

    /** Satu baris notifikasi compact */
    private View buildRow(Context ctx, NotifItem item, float d) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int h = (int)(14 * d);
        int v = (int)(9  * d);
        row.setPadding(h, v, h, v);

        // dot warna
        View dot = new View(ctx);
        int dotSz = (int)(7 * d);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSz, dotSz);
        dotLp.setMarginEnd((int)(10 * d));
        dot.setLayoutParams(dotLp);
        dot.setBackgroundResource(item.isCritical
                ? R.drawable.bg_dot_red
                : R.drawable.bg_dot_blue);
        row.addView(dot);

        // kolom teks
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(item.title);
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText(item.subtitle);
        tvSub.setTextSize(11f);
        tvSub.setTextColor(Color.parseColor("#6B7280"));
        tvSub.setMaxLines(1);
        tvSub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(tvSub);

        row.addView(col);

        // waktu
        TextView tvTime = new TextView(ctx);
        tvTime.setText(item.time);
        tvTime.setTextSize(10f);
        tvTime.setTextColor(Color.parseColor("#9CA3AF"));
        tvTime.setPadding((int)(8 * d), 0, 0, 0);
        row.addView(tvTime);

        return row;
    }

    /** State kosong */
    private View buildEmptyRow(Context ctx, float d) {
        LinearLayout empty = new LinearLayout(ctx);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding((int)(16*d), (int)(20*d), (int)(16*d), (int)(20*d));

        ImageView ic = new ImageView(ctx);
        int sz = (int)(30 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = (int)(8 * d);
        ic.setLayoutParams(lp);
        ic.setImageResource(R.drawable.ic_notification);
        ic.setColorFilter(Color.parseColor("#D1D5DB"));
        empty.addView(ic);

        TextView tv = new TextView(ctx);
        tv.setText("Tidak ada notifikasi");
        tv.setTextSize(12.5f);
        tv.setTextColor(Color.parseColor("#9CA3AF"));
        tv.setGravity(Gravity.CENTER);
        empty.addView(tv);

        return empty;
    }

    /** Divider tipis */
    private View makeDivider(Context ctx, float d) {
        View v = new View(ctx);
        v.setBackgroundColor(Color.parseColor("#F3F4F6"));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d)));
        return v;
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers Firebase
    // ─────────────────────────────────────────────────────────────
    private float toFloat(Object val) {
        if (val instanceof Double)  return ((Double) val).floatValue();
        if (val instanceof Long)    return ((Long) val).floatValue();
        if (val instanceof Float)   return (Float) val;
        if (val instanceof Integer) return ((Integer) val).floatValue();
        return 0f;
    }

    private long toLong(Object val) {
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Double)  return ((Double) val).longValue();
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Float)   return ((Float) val).longValue();
        return 0L;
    }

    private boolean getBool(DataSnapshot snapshot, String key) {
        Object val = snapshot.child(key).getValue();
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }

    private static class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        public MaxHeightScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int customHeightSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, customHeightSpec);
        }
    }
}
