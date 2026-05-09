package com.example.smartwaterchick;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BleWifiSetupActivity extends BaseActivity {

    // UUID harus sama persis dengan yang didefinisikan di ESP32
    private static final UUID SERVICE_UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHAR_SSID_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID CHAR_PASS_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");
    private static final UUID CHAR_STATUS_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa");

    private static final String TARGET_DEVICE_NAME = "SmartWaterChick";
    private static final int SCAN_PERIOD_MS = 10000;
    private static final int REQUEST_BLE_PERMISSIONS = 100;

    // UI
    private TextView tvBleStatus, tvDeviceList, tvDeviceName, tvLog;
    private Button btnScan, btnConnect, btnSendWifi;
    private LinearLayout layoutDeviceFound;
    private CardView cardWifiInput, cardLog;
    private ProgressBar progressBle;
    private EditText etSsid, etWifiPassword;
    private ImageView ivToggleWifiPass;

    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice foundDevice;
    private boolean isConnected = false;
    private boolean isPasswordVisible = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ──────────────────────────────────────────
    // SCAN CALLBACK — dipanggil setiap ada device BLE ditemukan
    // ──────────────────────────────────────────
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = null;
            if (ActivityCompat.checkSelfPermission(BleWifiSetupActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                name = result.getDevice().getName();
            }

            if (name != null && name.equals(TARGET_DEVICE_NAME)) {
                foundDevice = result.getDevice();
                stopScan();
                final String finalName = name; // Dibutuhkan oleh lambda runOnUiThread
                runOnUiThread(() -> {
                    tvDeviceList.setVisibility(View.GONE);
                    tvDeviceName.setText(finalName + " — " + result.getDevice().getAddress());
                    layoutDeviceFound.setVisibility(View.VISIBLE);
                    appendLog("✅ Perangkat ditemukan: " + finalName);
                    updateStatus("Perangkat ditemukan", "#2ECC71");
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                appendLog("❌ Scan gagal (kode: " + errorCode + ")");
                updateStatus("Scan gagal", "#E74C3C");
                progressBle.setVisibility(View.GONE);
                btnScan.setEnabled(true);
            });
        }
    };

    // ──────────────────────────────────────────
    // GATT CALLBACK — event koneksi & komunikasi BLE
    // ──────────────────────────────────────────
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                isConnected = true;
                runOnUiThread(() -> {
                    updateStatus("Terhubung ke ESP32 ✓", "#2ECC71");
                    appendLog("✅ BLE Terhubung! Menemukan layanan...");
                    progressBle.setVisibility(View.GONE);
                    cardWifiInput.setVisibility(View.VISIBLE);
                    btnConnect.setText("Terhubung ✓");
                    btnConnect.setEnabled(false);
                });
                if (ActivityCompat.checkSelfPermission(BleWifiSetupActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false;
                runOnUiThread(() -> {
                    updateStatus("Terputus dari ESP32", "#E74C3C");
                    appendLog("⚠️ Koneksi BLE terputus");
                    cardWifiInput.setVisibility(View.GONE);
                    btnConnect.setText("Hubungkan");
                    btnConnect.setEnabled(true);
                    progressBle.setVisibility(View.GONE);
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> appendLog("✅ Layanan BLE ditemukan. Siap kirim data WiFi."));
            } else {
                runOnUiThread(() -> appendLog("⚠️ Gagal menemukan layanan BLE (status: " + status + ")"));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (characteristic.getUuid().equals(CHAR_SSID_UUID)) {
                // SSID berhasil dikirim → sekarang kirim password
                runOnUiThread(() -> appendLog("✅ SSID terkirim. Mengirim password..."));
                sendPassword();
            } else if (characteristic.getUuid().equals(CHAR_PASS_UUID)) {
                runOnUiThread(() -> {
                    appendLog("✅ Password terkirim!");
                    appendLog("⏳ Menunggu ESP32 terhubung ke WiFi...");
                    updateStatus("Data WiFi dikirim ✓", "#1B5BCE");
                    btnSendWifi.setEnabled(true);
                    btnSendWifi.setText("ðŸ“¡  Kirim ke ESP32");
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Menerima notifikasi status dari ESP32
            if (characteristic.getUuid().equals(CHAR_STATUS_UUID)) {
                String status = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    appendLog("📡 Status ESP32: " + status);
                    if (status.startsWith("CONNECTED")) {
                        updateStatus("ESP32 Online ✓", "#2ECC71");
                        appendLog("🎉 ESP32 berhasil terhubung ke WiFi & Firebase!");
                    } else if (status.startsWith("FAILED")) {
                        updateStatus("WiFi Gagal!", "#E74C3C");
                        appendLog("❌ ESP32 gagal konek WiFi. Periksa nama/password.");
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_wifi_setup);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inisialisasi View
        tvBleStatus = findViewById(R.id.tvBleStatus);
        tvDeviceList = findViewById(R.id.tvDeviceList);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvLog = findViewById(R.id.tvLog);
        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        btnSendWifi = findViewById(R.id.btnSendWifi);
        layoutDeviceFound = findViewById(R.id.layoutDeviceFound);
        cardWifiInput = findViewById(R.id.cardWifiInput);
        cardLog = findViewById(R.id.cardLog);
        progressBle = findViewById(R.id.progressBle);
        etSsid = findViewById(R.id.etSsid);
        etWifiPassword = findViewById(R.id.etWifiPassword);
        ivToggleWifiPass = findViewById(R.id.ivToggleWifiPass);

        // Back button
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Toggle password visibility
        ivToggleWifiPass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etWifiPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isPasswordVisible = false;
            } else {
                etWifiPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible = true;
            }
            etWifiPassword.setSelection(etWifiPassword.length());
        });

        // Inisialisasi BluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Tombol Scan
        btnScan.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                startScan();
            }
        });

        // Tombol Connect
        btnConnect.setOnClickListener(v -> {
            if (foundDevice != null) {
                connectToDevice();
            }
        });

        // Tombol Kirim WiFi
        btnSendWifi.setOnClickListener(v -> {
            String ssid = etSsid.getText().toString().trim();
            String pass = etWifiPassword.getText().toString().trim();
            if (ssid.isEmpty()) {
                Toast.makeText(this, "Nama WiFi tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isConnected) {
                Toast.makeText(this, "Belum terhubung ke ESP32", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSendWifi.setEnabled(false);
            btnSendWifi.setText("Mengirim...");
            appendLog("📤 Mengirim SSID: " + ssid);
            sendSsid(ssid);
        });
    }

    // ──────────────────────────────────────────
    // SCAN BLE
    // ──────────────────────────────────────────
    private void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Aktifkan Bluetooth terlebih dahulu", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }

        foundDevice = null;
        layoutDeviceFound.setVisibility(View.GONE);
        tvDeviceList.setVisibility(View.VISIBLE);
        tvDeviceList.setText("Mencari SmartWaterChick...");
        cardLog.setVisibility(View.VISIBLE);
        tvLog.setText("");
        progressBle.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        updateStatus("Scanning...", "#F39C12");
        appendLog("Memulai scan BLE...");

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bleScanner.startScan(scanCallback);
        }

        // Otomatis berhenti scan setelah SCAN_PERIOD_MS
        handler.postDelayed(() -> {
            stopScan();
            if (foundDevice == null) {
                runOnUiThread(() -> {
                    tvDeviceList.setText("Perangkat tidak ditemukan. Coba lagi.");
                    updateStatus("Tidak ditemukan", "#E74C3C");
                    appendLog("❌ Scan selesai. SmartWaterChick tidak ditemukan.");
                    progressBle.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                });
            }
        }, SCAN_PERIOD_MS);
    }

    private void stopScan() {
        if (bleScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bleScanner.stopScan(scanCallback);
            }
        }
        runOnUiThread(() -> {
            progressBle.setVisibility(View.GONE);
            btnScan.setEnabled(true);
        });
    }

    // ──────────────────────────────────────────
    // CONNECT KE PERANGKAT BLE
    // ──────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (foundDevice == null) return;
        progressBle.setVisibility(View.VISIBLE);
        btnConnect.setEnabled(false);
        btnConnect.setText("Menghubungkan...");
        updateStatus("Menghubungkan...", "#F39C12");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            appendLog("🔗 Menghubungkan ke " + foundDevice.getName() + "...");
            bluetoothGatt = foundDevice.connectGatt(this, false, gattCallback);
        } else {
            appendLog("❌ Gagal menghubungkan: Izin Bluetooth tidak diberikan.");
        }
    }

    // ──────────────────────────────────────────
    // KIRIM SSID VIA BLE
    // ──────────────────────────────────────────
    private void sendSsid(String ssid) {
        if (bluetoothGatt == null) return;
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            appendLog("❌ Layanan BLE tidak ditemukan pada perangkat");
            return;
        }
        BluetoothGattCharacteristic charSsid = service.getCharacteristic(CHAR_SSID_UUID);
        if (charSsid == null) {
            appendLog("❌ Karakteristik SSID tidak ditemukan");
            return;
        }
        charSsid.setValue(ssid.getBytes(StandardCharsets.UTF_8));
        charSsid.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothGatt.writeCharacteristic(charSsid);
        }
    }

    // ──────────────────────────────────────────
    // KIRIM PASSWORD VIA BLE (dipanggil setelah SSID sukses)
    // ──────────────────────────────────────────
    private void sendPassword() {
        if (bluetoothGatt == null) return;
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) return;
        BluetoothGattCharacteristic charPass = service.getCharacteristic(CHAR_PASS_UUID);
        if (charPass == null) return;

        String password = etWifiPassword.getText().toString().trim();
        charPass.setValue(password.getBytes(StandardCharsets.UTF_8));
        charPass.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothGatt.writeCharacteristic(charPass);
        }
    }

    // ──────────────────────────────────────────
    // HELPER UI
    // ──────────────────────────────────────────
    private void updateStatus(String text, String colorHex) {
        tvBleStatus.setText(text);
        tvBleStatus.setTextColor(android.graphics.Color.parseColor(colorHex));
    }

    private void appendLog(String message) {
        cardLog.setVisibility(View.VISIBLE);
        String current = tvLog.getText().toString();
        tvLog.setText(current + (current.isEmpty() ? "" : "\n") + message);
    }

    // ──────────────────────────────────────────
    // CEK DAN MINTA PERMISSION BLE
    // ──────────────────────────────────────────
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, REQUEST_BLE_PERMISSIONS);
                return false;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLE_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startScan();
            } else {
                Toast.makeText(this, "Izin Bluetooth diperlukan untuk fitur ini", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
    }
}

