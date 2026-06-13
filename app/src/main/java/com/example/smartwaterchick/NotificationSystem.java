package com.example.smartwaterchick;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationSystem {
    private static NotificationSystem instance;

    public static synchronized NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    private final DatabaseReference dbRef;
    private final List<ImageView> registeredIcons = new ArrayList<>();

    // Cache values
    private boolean isOnline = true;
    private float ph = 7.0f;
    private float waterPercent = 100f;
    private boolean relayIsi = false;
    private boolean relayBuang = false;
    private boolean relayMinum = false;

    private NotificationSystem() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        startListening();
    }

    private void startListening() {
        // Listen to /kontrol_status
        dbRef.child("kontrol_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Object phRaw = snapshot.child("ph_terkini").getValue();
                Object persenRaw = snapshot.child("kapasitas_persen").getValue();
                Object lastSeenRaw = snapshot.child("last_seen").getValue();

                // Check online status
                if (lastSeenRaw != null) {
                    long lastSeen = toLong(lastSeenRaw);
                    long currentEpoch = System.currentTimeMillis() / 1000;
                    isOnline = Math.abs(currentEpoch - lastSeen) < 90;
                } else {
                    isOnline = phRaw != null;
                }

                if (phRaw != null) ph = toFloat(phRaw);
                if (persenRaw != null) waterPercent = toFloat(persenRaw);

                evaluateNotifications();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen to /kontrol
        dbRef.child("kontrol").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                relayIsi = getBool(snapshot, "relay_isi");
                relayBuang = getBool(snapshot, "relay_buang");
                relayMinum = getBool(snapshot, "relay_minum");

                evaluateNotifications();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private synchronized void evaluateNotifications() {
        updateRegisteredIcons();
    }

    public synchronized List<String> getFilteredAlerts(Context context) {
        List<String> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences("SmartWaterNotifPrefs", Context.MODE_PRIVATE);
        boolean showPush = prefs.getBoolean("notif_push", true);
        if (!showPush) {
            return list;
        }

        // 1. Wifi / Connection
        if (prefs.getBoolean("notif_wifi", true)) {
            if (!isOnline) {
                list.add("Peringatan: Alat SmartWater Terputus (Offline)");
            }
        }

        // 2. pH / Critical
        if (prefs.getBoolean("notif_critical", true)) {
            if (ph < 6.5f) {
                list.add(String.format(java.util.Locale.getDefault(), "Peringatan: pH Air Terlalu Asam (%.2f pH)", ph));
            } else if (ph > 7.5f) {
                list.add(String.format(java.util.Locale.getDefault(), "Peringatan: pH Air Terlalu Basa (%.2f pH)", ph));
            }
        }

        // 3. Water Level
        if (prefs.getBoolean("notif_water_level", true)) {
            if (waterPercent < 20f) {
                list.add(String.format(java.util.Locale.getDefault(), "Peringatan: Level Air Sangat Rendah (%.0f%%)", waterPercent));
            }
        }

        // 4. Relay Status
        if (prefs.getBoolean("notif_relay", true)) {
            if (relayIsi) {
                list.add("Info: Pompa Air (Relay Isi) sedang aktif");
            }
            if (relayBuang) {
                list.add("Info: Valve Buang Air sedang aktif");
            }
            if (relayMinum) {
                list.add("Info: Valve Kran Air Minum sedang aktif");
            }
        }

        return list;
    }

    public synchronized void registerNotificationIcon(ImageView imageView) {
        if (!registeredIcons.contains(imageView)) {
            registeredIcons.add(imageView);
        }
        updateIconState(imageView);
    }

    public synchronized void unregisterNotificationIcon(ImageView imageView) {
        registeredIcons.remove(imageView);
    }

    private void updateRegisteredIcons() {
        for (ImageView iv : registeredIcons) {
            updateIconState(iv);
        }
    }

    private void updateIconState(ImageView iv) {
        if (iv == null) return;
        iv.post(() -> {
            Context context = iv.getContext();
            List<String> alerts = getFilteredAlerts(context);
            if (!alerts.isEmpty()) {
                iv.setImageResource(R.drawable.ic_notification_badge);
            } else {
                iv.setImageResource(R.drawable.ic_notification);
            }
        });
    }

    public void showNotificationMenu(Context context, View anchor) {
        List<String> alerts = getFilteredAlerts(context);
        PopupMenu popup = new PopupMenu(context, anchor);
        
        if (alerts.isEmpty()) {
            popup.getMenu().add("Tidak ada pemberitahuan baru");
        } else {
            for (String alert : alerts) {
                popup.getMenu().add(alert);
            }
        }
        popup.show();
    }

    // Helpers
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
        return 0L;
    }

    private boolean getBool(DataSnapshot snapshot, String key) {
        Object val = snapshot.child(key).getValue();
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }
}
