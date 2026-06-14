#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
// config.h — File Konfigurasi Smart Water Chick ESP32
// WAJIB diisi sesuai dengan data WiFi dan Firebase Anda
// ============================================================

// ──────────────────────────────────────────
// IMPORT KREDENSIAL RAHASIA (SSID, Password, API Key, dll)
// ──────────────────────────────────────────
#include "secrets.h"

// ──────────────────────────────────────────
// KONFIGURASI PIN HARDWARE
// ──────────────────────────────────────────

// Sensor pH Analog
#define PH_PIN    34      // GPIO34 (ADC1_CH6 — hanya baca, tidak bisa jadi output)

// Sensor Ultrasonik HC-SR04 (Level Air)
#define TRIG_PIN  5       // GPIO5
#define ECHO_PIN  18      // GPIO18

// Relay Pompa Isi Air (aktif LOW = relay ON saat pin LOW)
#define RELAY_ISI_PIN    4    // Diubah menjadi D4 agar terhubung ke fisik pompa
#define RELAY_BUANG_PIN  2    // Diubah menjadi D2

// Relay Kran Air Minum (IN3)
#define RELAY_MINUM_PIN  14   // GPIO14

// ──────────────────────────────────────────
// KONFIGURASI TOPLES (Wadah Air Ayam)
// ──────────────────────────────────────────
// Bentuk: Silinder tegak
// Diameter        : 10 cm  → Jari-jari (r) = 5 cm
// Tinggi internal : 18 cm  (dari dasar ke bibir atas)
// Volume maksimal : π × r² × h = 3.14159 × 25 × 18 = 1413.7 cm³ ≈ 1413 ml = 1.413 L
// ──────────────────────────────────────────

// Dimensi fisik toples
#define TANK_RADIUS_CM     5.0f    // Jari-jari toples (cm)
#define TANK_HEIGHT_CM     18.0f   // Tinggi internal toples (cm) — dari dasar ke bibir
#define TANK_VOLUME_LITER  1.413f  // Volume maksimal (liter) — diisi penuh

// ── Kalibrasi Sensor Ultrasonik ──────────────────────
// Sensor HC-SR04 dipasang di ATAS toples, menghadap ke bawah.
// CARA KALIBRASI:
//   1. Kosongkan toples (0%) → baca jarak di Serial Monitor → isi SENSOR_DIST_EMPTY_CM
//   2. Isi toples penuh (100%) → baca jarak → isi SENSOR_DIST_FULL_CM
//
// CONTOH (perkiraan):
//   Jika sensor dipasang 1 cm di atas bibir toples:
//   - Toples KOSONG : jarak ≈ 1 + 18 = 19 cm  → SENSOR_DIST_EMPTY_CM = 19.0
//   - Toples PENUH  : jarak ≈ 1 cm             → SENSOR_DIST_FULL_CM  =  1.0
// ──────────────────────────────────────────────────────
#define SENSOR_DIST_EMPTY_CM  19.0f   // Jarak sensor ke dasar toples saat KOSONG (cm)
#define SENSOR_DIST_FULL_CM    1.0f   // Jarak sensor ke permukaan air saat PENUH (cm)

// Toleransi pembacaan (filter noise sensor ultrasonik, ±0.3 cm)
#define SENSOR_NOISE_FILTER_CM  0.3f

// ──────────────────────────────────────────
// KONFIGURASI WAKTU
// ──────────────────────────────────────────
#define SEND_INTERVAL_MS  30000   // Kirim data sensor setiap 30 detik
#define TIMEZONE_OFFSET   28800   // WIB = UTC+8 = 8*3600 = 28800 detik

// ──────────────────────────────────────────
// KALIBRASI SENSOR pH — 2-POINT CALIBRATION
// CARA KALIBRASI:
//   1. Celupkan probe ke larutan buffer pH 4.0
//      Lihat Serial Monitor → catat voltase → isi PH4_VOLTAGE
//   2. Celupkan probe ke larutan buffer pH 7.0
//      Lihat Serial Monitor → catat voltase → isi PH7_VOLTAGE
//   3. Upload ulang firmware
// NILAI DEFAULT di bawah adalah estimasi umum modul pH-4502C
// dengan ESP32 (ADC 3.3V). Wajib dikalibrasi untuk akurasi!
// ──────────────────────────────────────────
#define PH4_VOLTAGE   3.00f   // Voltase terukur saat probe di larutan pH 4.0
#define PH7_VOLTAGE   2.50f   // Voltase terukur saat probe di larutan pH 7.0

#endif // CONFIG_H