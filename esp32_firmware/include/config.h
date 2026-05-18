#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
// config.h — File Konfigurasi Smart Water Chick ESP32
// WAJIB diisi sesuai dengan data WiFi dan Firebase Anda
// ============================================================

// ──────────────────────────────────────────
// KONFIGURASI WIFI
// ──────────────────────────────────────────
#define WIFI_SSID       "POLITEKNIK NEGERI TANAH LAUT"
#define WIFI_PASSWORD   "Sejak@2009#"

// ──────────────────────────────────────────
// KONFIGURASI FIREBASE
// Ambil dari: Firebase Console → Project Settings → General
// ──────────────────────────────────────────
#define FIREBASE_API_KEY      "AIzaSyBX_GuuTxQi5uHxPJ7i-pnaki95eAFWTMs"
#define FIREBASE_DATABASE_URL "smartwater-chick-77b9d-default-rtdb.firebaseio.com"

// (Konfigurasi Email & Password dihapus. ESP32 akan masuk tanpa akun / Test Mode)

// ──────────────────────────────────────────
// KONFIGURASI PIN HARDWARE
// ──────────────────────────────────────────

// Sensor DHT22 (Suhu & Kelembaban)
#define DHT_PIN   15      // GPIO15
#define DHT_TYPE  DHT22   // Tipe sensor: DHT22 (lebih akurat) atau DHT11

// Sensor pH Analog
#define PH_PIN    34      // GPIO34 (ADC1_CH6 — hanya baca, tidak bisa jadi output)

// Sensor Ultrasonik HC-SR04 (Level Air)
#define TRIG_PIN  5       // GPIO5
#define ECHO_PIN  18      // GPIO18

// Relay Pompa Isi Air (aktif LOW = relay ON saat pin LOW)
#define RELAY_ISI_PIN    4    // Diubah menjadi D4 agar terhubung ke fisik pompa
#define RELAY_BUANG_PIN  2    // Diubah menjadi D2

// ──────────────────────────────────────────
// KONFIGURASI TANGKI
// ──────────────────────────────────────────
#define TANK_HEIGHT_CM     50.0f   // Tinggi tangki penuh (cm)
#define TANK_VOLUME_LITER  200.0f  // Volume tangki penuh (liter)

// Jarak sensor ultrasonik ke permukaan air saat tangki PENUH (cm)
// Ukur saat tangki penuh, biasanya 2-5 cm dari sensor ke air
#define SENSOR_TO_WATER_FULL_CM  2.0f

// ──────────────────────────────────────────
// KONFIGURASI WAKTU
// ──────────────────────────────────────────
#define SEND_INTERVAL_MS  30000   // Kirim data sensor setiap 30 detik
#define TIMEZONE_OFFSET   28800   // WIB = UTC+8 = 8*3600 = 28800 detik

// ──────────────────────────────────────────
// KALIBRASI SENSOR pH
// Lakukan kalibrasi dengan larutan buffer pH 4 dan pH 7
// Ukur voltase dan masukkan di sini
// ──────────────────────────────────────────
#define PH_NEUTRAL_VOLTAGE  2.5f    // Voltase saat pH = 7.0 (buffer netral)
#define PH_SLOPE            -5.5f   // Slope: perubahan voltase per unit pH

#endif // CONFIG_H