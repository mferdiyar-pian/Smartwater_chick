// ============================================================
// main.cpp — Smart Water Chick — Firmware ESP32
// Platform: PlatformIO + Arduino Framework
// Sensor: pH Analog (GPIO34) + Ultrasonik HC-SR04 (GPIO5/18)
// ============================================================
// Logika Mode:
//   1. WiFi + Firebase OK → Mode Normal (kirim data ke cloud)
//   2. WiFi gagal/mati   → OFFLINE Mode
//      - Sensor & Jadwal pompa tetap berjalan
//      - Terus mencoba reconnect WiFi setiap 30 detik
//      - Begitu WiFi kembali → otomatis connect Firebase
// ============================================================

#include <Arduino.h>
#include <WiFi.h>
#include <Preferences.h>
#include <Firebase_ESP_Client.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "config.h"
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

// Interval retry koneksi WiFi saat offline (ms)
#define WIFI_RETRY_INTERVAL_MS 30000

// ──────────────────────────────────────────
// OBJEK GLOBAL
// ──────────────────────────────────────────
LiquidCrystal_I2C lcd(0x27, 16, 2);

FirebaseData fbdo;
FirebaseData fbdoCommand;
FirebaseAuth firebaseAuth;
FirebaseConfig firebaseConfig;

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", TIMEZONE_OFFSET, 60000);
Preferences preferences;

bool isOnline      = false;
bool firebaseReady = false;
bool isAutoMode    = false;
bool schedule07 = false, schedule15 = false, schedule22 = false;
bool isPhConnected = false;
bool isUltrasonicConnected = false;

unsigned long lastSendTime       = 0;
unsigned long lastWifiRetryTime  = 0;
unsigned long lastEpochSync      = 0;
unsigned long millisAtLastSync   = 0;
unsigned long lastPollTime       = 0;

const unsigned long POLL_INTERVAL_MS = 2000;

// ──────────────────────────────────────────
// DEKLARASI FUNGSI
// ──────────────────────────────────────────
bool  connectToWifi(String ssid, String pass, bool silent = false);
void  connectFirebase();
void  readAndSendSensorData();
void  runOfflineTasks();
float readPH();
float readWaterLevel();
float levelToLiter(float heightCm);
void  controlPump(bool fillOn, bool drainOn);
void  pollFirebase();
void  checkSchedule();
unsigned long getEstimatedEpoch();
String epochToDate(unsigned long epoch);
String epochToTime(unsigned long epoch);
int   getHourFromEpoch(unsigned long epoch);
int   getMinuteFromEpoch(unsigned long epoch);

// ──────────────────────────────────────────
// SETUP
// ──────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    Serial.println("\n===== Smart Water Chick ESP32 =====");
    Serial.println("Sensor aktif: pH (GPIO34) + Ultrasonik (GPIO5/18)");

    // ── Inisialisasi LCD I2C ──
    Wire.begin();
    lcd.init();
    lcd.backlight();
    lcd.setCursor(0, 0); lcd.print("Smart Water     ");
    lcd.setCursor(0, 1); lcd.print("Chick Start...  ");
    delay(1500);

    // ── Inisialisasi Pin ──
    pinMode(RELAY_ISI_PIN,  OUTPUT);
    pinMode(RELAY_BUANG_PIN, OUTPUT);
    pinMode(RELAY_MINUM_PIN, OUTPUT);
    pinMode(TRIG_PIN, OUTPUT);
    pinMode(ECHO_PIN, INPUT);
    digitalWrite(RELAY_ISI_PIN,  HIGH);  // Relay OFF (aktif LOW)
    digitalWrite(RELAY_BUANG_PIN, HIGH);
    digitalWrite(RELAY_MINUM_PIN, HIGH);

    // ── Coba connect WiFi ──
    Serial.println("Mencoba connect ke WiFi...");
    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Koneksi WiFi... ");
    lcd.setCursor(0, 1); lcd.print(String(WIFI_SSID).substring(0, 16));
    bool ok = connectToWifi(WIFI_SSID, WIFI_PASSWORD);

    if (ok) {
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("WiFi Terhubung! ");
        lcd.setCursor(0, 1); lcd.print(WiFi.localIP().toString());
        delay(1000);

        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Sinkron Waktu...");
        timeClient.begin();
        timeClient.setTimeOffset(TIMEZONE_OFFSET);
        timeClient.update();
        lastEpochSync    = timeClient.getEpochTime();
        millisAtLastSync = millis();

        connectFirebase();
        isOnline = true;

        // Ganti tampilan LCD setelah Firebase siap
        if (firebaseReady) {
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Kontrol Siap!   ");
            lcd.setCursor(0, 1); lcd.print("Poll tiap 2 det ");
        }
    } else {
        Serial.println("═══════════════════════════════════════");
        Serial.println("  ⚠️  MODE OFFLINE AKTIF");
        Serial.println("  Sensor tetap berjalan.");
        Serial.println("  Retry WiFi tiap 30 detik...");
        Serial.println("═══════════════════════════════════════");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("[OFFLINE] MODE  ");
        lcd.setCursor(0, 1); lcd.print("Retry WiFi 30s  ");
        delay(1500);
        isOnline = false;
    }
}

// ──────────────────────────────────────────
// LOOP UTAMA
// ──────────────────────────────────────────
void loop() {

    // ─── Mode Offline — Coba reconnect WiFi ───
    if (!isOnline) {
        unsigned long now = millis();
        if (now - lastWifiRetryTime >= WIFI_RETRY_INTERVAL_MS || lastWifiRetryTime == 0) {
            lastWifiRetryTime = now;
            bool ok = connectToWifi(WIFI_SSID, WIFI_PASSWORD, true);
            if (ok) {
                Serial.println("[WiFi] ✅ Kembali online!");
                isOnline = true;
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("Kembali Online! ");
                lcd.setCursor(0, 1); lcd.print(WiFi.localIP().toString());
                delay(1000);

                timeClient.begin();
                timeClient.update();
                lastEpochSync    = timeClient.getEpochTime();
                millisAtLastSync = millis();

                if (!firebaseReady) connectFirebase();
            } else {
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("[OFFLINE] MODE  ");
                lcd.setCursor(0, 1); lcd.print("Retry WiFi 30s  ");
            }
        }
    }

    // ─── Mode Normal (Online) ───
    if (isOnline && firebaseReady) {
        if (Firebase.isTokenExpired()) Firebase.refreshToken(&firebaseConfig);

        unsigned long now = millis();
        if (now - lastPollTime >= POLL_INTERVAL_MS || lastPollTime == 0) {
            lastPollTime = now;
            pollFirebase();
        }

        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("[WiFi] Koneksi terputus. Masuk mode offline...");
            isOnline      = false;
            firebaseReady = false;
        }
    }

    // ─── Kirim data sensor (Online & Offline) ───
    unsigned long now = millis();
    if (now - lastSendTime >= SEND_INTERVAL_MS || lastSendTime == 0) {
        lastSendTime = now;

        if (isOnline) {
            timeClient.update();
            lastEpochSync    = timeClient.getEpochTime();
            millisAtLastSync = millis();
        }

        if (isOnline && firebaseReady) {
            readAndSendSensorData();
        } else {
            runOfflineTasks();
        }

        checkSchedule();
    }

    delay(100);
}

// ──────────────────────────────────────────
// FUNGSI: Baca & kirim data sensor ke Firebase
// ──────────────────────────────────────────
void readAndSendSensorData() {
    float ph       = readPH();
    float levelCm  = readWaterLevel();
    float liter    = levelToLiter(levelCm);
    float persen   = constrain((liter / TANK_VOLUME_LITER) * 100.0f, 0, 100);
    float ml       = liter * 1000.0f;

    // ── Update LCD ──
    lcd.clear();
    lcd.setCursor(0, 0); lcd.printf("pH:%.2f         ", ph);
    lcd.setCursor(0, 1); lcd.printf("Air:%.2fL %d%%   ", liter, (int)persen);

    String tanggal = epochToDate(timeClient.getEpochTime());
    String waktu   = epochToTime(timeClient.getEpochTime());
    String key     = tanggal + "_" + waktu;
    key.replace(":", "-");

    Serial.printf("\n[ONLINE] %s %s | pH:%.2f | Air:%.0fml / %.3fL (%.1f%%)\n",
                  tanggal.c_str(), waktu.c_str(), ph, ml, liter, persen);

    // ── Kirim ke Firebase ──
    String base = "/monitoring/" + key;
    Firebase.RTDB.setFloat(&fbdo,  base + "/ph",      ph);
    Firebase.RTDB.setString(&fbdo, base + "/tanggal", tanggal);
    Firebase.RTDB.setString(&fbdo, base + "/waktu",   waktu);

    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/kapasitas_liter",  liter);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/kapasitas_persen", persen);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/ph_terkini",       ph);

    // Kirim status hardware tambahan
    Firebase.RTDB.setInt(&fbdo, "/kontrol_status/last_seen", (int)timeClient.getEpochTime());
    Firebase.RTDB.setInt(&fbdo, "/kontrol_status/rssi", WiFi.status() == WL_CONNECTED ? (int)WiFi.RSSI() : -100);
    Firebase.RTDB.setBool(&fbdo, "/kontrol_status/sensor_ph_connected", isPhConnected);
    Firebase.RTDB.setBool(&fbdo, "/kontrol_status/sensor_ultrasonic_connected", isUltrasonicConnected);

    Firebase.RTDB.setFloat(&fbdo,  "/volume_air/daily/" + tanggal + "/liter", liter);
    Firebase.RTDB.setString(&fbdo, "/volume_air/daily/" + tanggal + "/label", "Hari Ini");

    Serial.println("  ✅ Data dikirim ke Firebase.");
}

// ──────────────────────────────────────────
// FUNGSI: Jalankan tugas sensor saat OFFLINE
// ──────────────────────────────────────────
void runOfflineTasks() {
    float ph      = readPH();
    float levelCm = readWaterLevel();
    float liter   = levelToLiter(levelCm);
    float persen  = constrain((liter / TANK_VOLUME_LITER) * 100.0f, 0, 100);
    float ml      = liter * 1000.0f;

    // ── Update LCD ──
    lcd.clear();
    lcd.setCursor(0, 0); lcd.printf("pH:%.2f [OFL]   ", ph);
    lcd.setCursor(0, 1); lcd.printf("Air:%.2fL %d%%   ", liter, (int)persen);

    unsigned long estEpoch = getEstimatedEpoch();
    Serial.println("\n[OFFLINE] ── Data Sensor ──────────────────");
    if (estEpoch > 0) {
        Serial.printf("  Waktu Estimasi : %s %s\n",
                      epochToDate(estEpoch).c_str(), epochToTime(estEpoch).c_str());
    } else {
        Serial.println("  Waktu          : Tidak diketahui (belum pernah online)");
    }
    Serial.printf("  pH Air         : %.2f\n", ph);
    Serial.printf("  Volume Air     : %.0f ml / %.3f L (%.1f%%)\n", ml, liter, persen);
    Serial.println("  [Data TIDAK dikirim — offline]");
    Serial.println("────────────────────────────────────────────");
}

// ──────────────────────────────────────────
// FUNGSI: Baca sensor pH (Sama persis dengan Arduino IDE)
// ──────────────────────────────────────────
float readPH() {
    int buffer_arr[10];
    
    // Ambil 10 sampel data
    for (int i = 0; i < 10; i++) {
        buffer_arr[i] = analogRead(PH_PIN);
        delay(30);
    }

    // Urutkan data dari kecil ke besar (Bubble Sort)
    for (int i = 0; i < 9; i++) {
        for (int j = i + 1; j < 10; j++) {
            if (buffer_arr[i] > buffer_arr[j]) {
                int temp = buffer_arr[i];
                buffer_arr[i] = buffer_arr[j];
                buffer_arr[j] = temp;
            }
        }
    }

    // Ambil rata-rata dari 6 data tengah (membuang 2 nilai tertinggi dan 2 terendah)
    unsigned long int avgval = 0;
    for (int i = 2; i < 8; i++) {
        avgval += buffer_arr[i];
    }
    float avgAdc = avgval / 6.0f;

    // Deteksi jika sensor terhubung (jika ADC di luar batas ekstrim)
    if (avgAdc <= 5.0f || avgAdc >= 4090.0f) {
        isPhConnected = false;
    } else {
        isPhConnected = true;
    }

    // Konversi ke voltase
    float volt = avgAdc * (3.3f / 4095.0f);

    // Rumus / Formula dari Arduino IDE
    float calibration_value = 21.34f + 1.5f; // = 22.84
    float ph = -5.70f * volt + calibration_value;

    // Debug ke Serial Monitor
    Serial.printf("[pH] ADC:%.0f  Volt:%.4fV  pH:%.2f  Connected:%s\n", avgAdc, volt, ph, isPhConnected ? "YES" : "NO");

    return constrain(ph, 0.0f, 14.0f);
}

// ──────────────────────────────────────────
// FUNGSI: Baca level air (HC-SR04) — Multi-sample + Median Filter
// Sensor dipasang DI ATAS toples menghadap ke bawah.
// Return : tinggi air dari dasar toples (cm), 0.0 s/d TANK_HEIGHT_CM
// ──────────────────────────────────────────
float readWaterLevel() {
    // Ambil 5 sampel jarak untuk median filter
    const int SAMPLES = 5;
    float readings[SAMPLES];
    int validCount = 0;

    for (int i = 0; i < SAMPLES; i++) {
        digitalWrite(TRIG_PIN, LOW);  delayMicroseconds(2);
        digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
        digitalWrite(TRIG_PIN, LOW);

        // Timeout 30ms ≈ maks 5 meter, cukup untuk toples 18 cm
        long dur = pulseIn(ECHO_PIN, HIGH, 30000);
        if (dur > 0) {
            readings[validCount++] = (dur * 0.034f) / 2.0f;  // cm
        }
        delay(20);  // Jeda antar-ping agar echo tidak bertabrakan
    }

    // Jika semua sampel gagal (tidak ada echo) → anggap kosong
    if (validCount == 0) {
        Serial.println("[ULTRASONIC] ⚠️ Tidak ada echo! Cek kabel/sensor.");
        isUltrasonicConnected = false;
        return 0.0f;
    }

    isUltrasonicConnected = true;

    // Urutkan (bubble sort sederhana) lalu ambil nilai TENGAH (median)
    for (int i = 0; i < validCount - 1; i++) {
        for (int j = i + 1; j < validCount; j++) {
            if (readings[i] > readings[j]) {
                float tmp = readings[i]; readings[i] = readings[j]; readings[j] = tmp;
            }
        }
    }
    float distCm = readings[validCount / 2];  // nilai median

    // Hitung tinggi air berdasarkan kalibrasi dua titik:
    //   distCm == SENSOR_DIST_EMPTY_CM  → air = 0 cm   (toples kosong)
    //   distCm == SENSOR_DIST_FULL_CM   → air = 18 cm  (toples penuh)
    // Rumus: tinggiAir = (distCm_kosong - distCm_baca) / (distCm_kosong - distCm_penuh) * tinggi_total
    float distRange  = SENSOR_DIST_EMPTY_CM - SENSOR_DIST_FULL_CM;  // cm rentang jarak
    float waterRatio = (SENSOR_DIST_EMPTY_CM - distCm) / distRange;  // 0.0 – 1.0
    float waterCm    = waterRatio * TANK_HEIGHT_CM;
    float waterCm_c  = constrain(waterCm, 0.0f, TANK_HEIGHT_CM);

    Serial.printf("[ULTRASONIC] Jarak:%.2fcm | TinggiAir:%.2fcm (%.1f%%)\n",
                  distCm, waterCm_c, (waterCm_c / TANK_HEIGHT_CM) * 100.0f);

    return waterCm_c;
}

// ──────────────────────────────────────────
// FUNGSI: Konversi tinggi air (cm) → Volume (liter) — Rumus Silinder Akurat
// V = π × r² × h  (cm³ = mL) → bagi 1000 = liter
// ──────────────────────────────────────────
float levelToLiter(float h) {
    // V (cm³) = π × r² × h
    // Dengan r = TANK_RADIUS_CM = 5 cm, maka πr² = 78.5398 cm²
    const float PI_R2 = 3.14159265f * TANK_RADIUS_CM * TANK_RADIUS_CM;
    float volumeCm3   = PI_R2 * h;          // cm³ = mL
    float volumeLiter = volumeCm3 / 1000.0f; // konversi ke liter
    return constrain(volumeLiter, 0.0f, TANK_VOLUME_LITER);
}

// ──────────────────────────────────────────
// FUNGSI: Kontrol Pompa
// ──────────────────────────────────────────
void controlPump(bool fillOn, bool drainOn) {
    digitalWrite(RELAY_ISI_PIN,   fillOn  ? LOW : HIGH);
    digitalWrite(RELAY_BUANG_PIN, drainOn ? LOW : HIGH);
}

// ──────────────────────────────────────────
// FUNGSI: Polling Firebase (perintah relay & mode otomatis)
// FUNGSI: Polling Firebase (perintah relay & mode otomatis)
// ──────────────────────────────────────────
void pollFirebase() {
    // Catatan: tidak pakai Firebase.ready() agar relay tetap bisa dibaca
    // meski koneksi sesekali tidak "ready" secara internal
    if (!Firebase.ready()) {
        Serial.println("[POLL] Firebase belum siap, mencoba tetap baca...");
    }

    // Relay Isi Air
    {
        static bool lastRelayIsi = false;
        bool state = false;
        if (Firebase.RTDB.getBool(&fbdoCommand, "/kontrol/relay_isi")) {
            state = fbdoCommand.boolData();
        } else {
            Serial.printf("[POLL ERR] relay_isi: %s\n", fbdoCommand.errorReason().c_str());
            state = lastRelayIsi;  // pertahankan state terakhir jika gagal baca
        }
        digitalWrite(RELAY_ISI_PIN, state ? LOW : HIGH);
        if (state != lastRelayIsi) {
            lastRelayIsi = state;
            Serial.printf("[FIREBASE] relay_isi: %s\n", state ? "ON" : "OFF");
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Pompa Isi Air:  ");
            lcd.setCursor(0, 1); lcd.print(state ? ">>> ON <<<      " : ">>> OFF <<<     ");
        }
    }

    // Relay Buang Air
    {
        static bool lastRelayBuang = false;
        bool state = false;
        if (Firebase.RTDB.getBool(&fbdoCommand, "/kontrol/relay_buang")) {
            state = fbdoCommand.boolData();
        } else {
            Serial.printf("[POLL ERR] relay_buang: %s\n", fbdoCommand.errorReason().c_str());
            state = lastRelayBuang;
        }
        digitalWrite(RELAY_BUANG_PIN, state ? LOW : HIGH);
        if (state != lastRelayBuang) {
            lastRelayBuang = state;
            Serial.printf("[FIREBASE] relay_buang: %s\n", state ? "ON" : "OFF");
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Pompa Buang Air:");
            lcd.setCursor(0, 1); lcd.print(state ? ">>> ON <<<      " : ">>> OFF <<<     ");
        }
    }

    // Relay Kran Minum Ayam
    {
        static bool lastRelayMinum = false;
        bool state = false;
        if (Firebase.RTDB.getBool(&fbdoCommand, "/kontrol/relay_minum")) {
            state = fbdoCommand.boolData();
        } else {
            Serial.printf("[POLL ERR] relay_minum: %s\n", fbdoCommand.errorReason().c_str());
            state = lastRelayMinum;
        }
        digitalWrite(RELAY_MINUM_PIN, state ? LOW : HIGH);
        if (state != lastRelayMinum) {
            lastRelayMinum = state;
            Serial.printf("[FIREBASE] relay_minum: %s\n", state ? "ON" : "OFF");
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Kran Air Minum: ");
            lcd.setCursor(0, 1); lcd.print(state ? ">>> ON <<<      " : ">>> OFF <<<     ");
        }
    }

    // Mode Otomatis
    if (Firebase.RTDB.getBool(&fbdoCommand, "/kontrol/otomatis")) {
        isAutoMode = fbdoCommand.boolData();
    }

    // Perintah Cek Manual (Cek pH)
    if (Firebase.RTDB.getBool(&fbdoCommand, "/kontrol/cek_sekarang")) {
        bool cekSekarang = fbdoCommand.boolData();
        if (cekSekarang) {
            Serial.println("[FIREBASE] Perintah Cek Manual diterima!");
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Membaca Sensor..");
            lcd.setCursor(0, 1); lcd.print("pH + Air Level  ");
            readAndSendSensorData();
            Firebase.RTDB.setBool(&fbdoCommand, "/kontrol/cek_sekarang", false);
        }
    }
}

// ──────────────────────────────────────────
// FUNGSI: Cek & jalankan jadwal pompa
// ──────────────────────────────────────────
void checkSchedule() {
    if (!isAutoMode) return;
    unsigned long epoch = isOnline ? timeClient.getEpochTime() : getEstimatedEpoch();
    if (epoch == 0) return;

    int jam   = getHourFromEpoch(epoch);
    int menit = getMinuteFromEpoch(epoch);
    if (menit > 1) return;

    if ((schedule07 && jam == 7) || (schedule15 && jam == 15) || (schedule22 && jam == 22)) {
        Serial.printf("[JADWAL] Jam %02d:00 — Isi air otomatis\n", jam);
        controlPump(true, false);
        delay(10000);
        controlPump(false, false);
    }
}

// ──────────────────────────────────────────
// FUNGSI: Koneksi WiFi
// ──────────────────────────────────────────
bool connectToWifi(String ssid, String pass, bool silent) {
    if (!silent) Serial.print("Menghubungkan ke WiFi: " + ssid);
    WiFi.begin(ssid.c_str(), pass.c_str());

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        if (!silent) Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        if (!silent) Serial.println("\n✅ WiFi Terhubung! IP: " + WiFi.localIP().toString());
        return true;
    }

    WiFi.disconnect();
    if (!silent) Serial.println("\n❌ Gagal terhubung ke WiFi.");
    return false;
}

// ──────────────────────────────────────────
// FUNGSI: Koneksi Firebase
void connectFirebase() {
    firebaseConfig.api_key      = FIREBASE_API_KEY;
    firebaseConfig.database_url = FIREBASE_DATABASE_URL;

    // Autentikasi akun perangkat (device login)
    firebaseAuth.user.email    = FIREBASE_USER_EMAIL;
    firebaseAuth.user.password = FIREBASE_USER_PASSWORD;

    // ── Buffer SSL lebih besar → handshake lebih cepat ──────────────────────
    // Firebase ESP Client merekomendasikan 4096/1024 untuk koneksi stabil
    fbdo.setBSSLBufferSize(4096, 1024);
    fbdoCommand.setBSSLBufferSize(4096, 1024);

    // Nonaktifkan reconnect otomatis yang bisa memperlambat inisialisasi awal
    Firebase.reconnectWiFi(true);

    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Init Firebase...");
    lcd.setCursor(0, 1); lcd.print("Mohon tunggu... ");
    Serial.println("Memanggil Firebase.begin()...");

    Firebase.begin(&firebaseConfig, &firebaseAuth);

    // ── Tunggu Firebase ready (maks 10 detik, update LCD tiap 300ms) ────────
    Serial.print("Menghubungkan ke Firebase");
    unsigned long waitStart = millis();
    int dotCount = 0;
    while (!Firebase.ready() && millis() - waitStart < 10000) {
        delay(300);
        Serial.print(".");

        // Tampilkan animasi titik pada baris ke-2 LCD agar tidak terlihat hang
        String dots = "";
        for (int i = 0; i < (dotCount % 4); i++) dots += ".";
        lcd.setCursor(0, 1);
        lcd.print("Menghubungkan" + dots + "   ");
        dotCount++;
    }

    if (Firebase.ready()) {
        Serial.println("\n✅ Firebase Terhubung!");
        firebaseReady = true;
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Firebase OK!    ");
        lcd.setCursor(0, 1); lcd.print("Sistem Siap     ");
        delay(800);
    } else {
        Serial.println("\n⚠️ Firebase gagal. Mode Offline aktif, retry otomatis...");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Firebase GAGAL! ");
        lcd.setCursor(0, 1); lcd.print("Mode Offline    ");
        delay(1000);
    }
}

// ──────────────────────────────────────────
// FUNGSI WAKTU
// ──────────────────────────────────────────
unsigned long getEstimatedEpoch() {
    if (lastEpochSync == 0) return 0;
    return lastEpochSync + (millis() - millisAtLastSync) / 1000;
}

int getHourFromEpoch(unsigned long epoch) {
    struct tm* t = gmtime((time_t*)&epoch);
    return t->tm_hour;
}

int getMinuteFromEpoch(unsigned long epoch) {
    struct tm* t = gmtime((time_t*)&epoch);
    return t->tm_min;
}

String epochToDate(unsigned long epoch) {
    struct tm* t = gmtime((time_t*)&epoch);
    char buf[11];
    sprintf(buf, "%04d-%02d-%02d", t->tm_year + 1900, t->tm_mon + 1, t->tm_mday);
    return String(buf);
}

String epochToTime(unsigned long epoch) {
    struct tm* t = gmtime((time_t*)&epoch);
    char buf[9];
    sprintf(buf, "%02d:%02d:%02d", t->tm_hour, t->tm_min, t->tm_sec);
    return String(buf);
}