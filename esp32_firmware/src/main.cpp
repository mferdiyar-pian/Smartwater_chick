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

// Global sensor values (diupdate setiap 2 detik secara lokal)
float latestPh     = 7.0f;
float latestLiter  = 0.0f;
float latestPersen = 0.0f;

unsigned long lastSendTime        = 0;
unsigned long lastWifiRetryTime   = 0;
unsigned long lastEpochSync       = 0;
unsigned long millisAtLastSync    = 0;
unsigned long lastPollTime        = 0;
unsigned long lastHeartbeatTime   = 0;   // Timer heartbeat WiFi status
unsigned long lastSensorReadTime  = 0;   // Timer pembacaan sensor lokal

const unsigned long POLL_INTERVAL_MS      = 2000;  // Pembacaan sensor + polling Firebase
const unsigned long HEARTBEAT_INTERVAL_MS = 5000;  // Kirim status WiFi ke Firebase setiap 5 detik

// ──────────────────────────────────────────
// DEKLARASI FUNGSI
// ──────────────────────────────────────────
bool  connectToWifi(String ssid, String pass, bool silent = false);
void  connectFirebase();
void  readSensors();
void  updateLCD(float ph, float liter, float persen);
void  sendSensorData(float ph, float liter, float persen);
void  logOfflineData(float ph, float liter, float persen);
void  sendHeartbeat(bool wifiOk);  // Kirim status WiFi ke Firebase (cepat)
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
    } else {
        Serial.println("═══════════════════════════════════════");
        Serial.println("  ⚠️  MODE OFFLINE AKTIF");
        Serial.println("  Sensor tetap berjalan.");
        Serial.println("  Retry WiFi tiap 30 detik...");
        Serial.println("═══════════════════════════════════════");
        isOnline = false;
    }

    // Pembacaan awal
    readSensors();
    updateLCD(latestPh, latestLiter, latestPersen);
}

// ──────────────────────────────────────────
// LOOP UTAMA
// ──────────────────────────────────────────
void loop() {
    unsigned long now = millis();

    // ─── 1. Pembacaan Sensor & Update LCD (Real-Time setiap 2 detik) ───
    if (now - lastSensorReadTime >= POLL_INTERVAL_MS || lastSensorReadTime == 0) {
        lastSensorReadTime = now;
        readSensors();
        updateLCD(latestPh, latestLiter, latestPersen);
    }

    // ─── 2. Mode Offline — Coba reconnect WiFi di background ───
    if (!isOnline) {
        if (now - lastWifiRetryTime >= WIFI_RETRY_INTERVAL_MS || lastWifiRetryTime == 0) {
            lastWifiRetryTime = now;
            Serial.println("[WiFi] Mencoba menghubungkan kembali...");
            bool ok = connectToWifi(WIFI_SSID, WIFI_PASSWORD, true); // silent = true agar tidak mengganggu LCD
            if (ok) {
                Serial.println("[WiFi] ✅ Kembali online!");
                isOnline = true;

                timeClient.begin();
                timeClient.update();
                lastEpochSync    = timeClient.getEpochTime();
                millisAtLastSync = millis();

                if (!firebaseReady) connectFirebase();
            }
        }
    }

    // ─── 3. Mode Online & Firebase Ready ───
    if (isOnline && firebaseReady) {
        if (Firebase.isTokenExpired()) Firebase.refreshToken(&firebaseConfig);

        // Polling kontrol relay setiap 2 detik
        if (now - lastPollTime >= POLL_INTERVAL_MS || lastPollTime == 0) {
            lastPollTime = now;
            pollFirebase();
        }

        // Cek status koneksi WiFi
        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("[WiFi] Koneksi terputus. Masuk mode offline...");
            isOnline      = false;
            firebaseReady = false;
        }

        // Heartbeat WiFi status setiap 5 detik
        if (now - lastHeartbeatTime >= HEARTBEAT_INTERVAL_MS || lastHeartbeatTime == 0) {
            lastHeartbeatTime = now;
            sendHeartbeat(true);
        }
    }

    // ─── 4. Kirim Data Sensor (Setiap 30 detik) ───
    if (now - lastSendTime >= SEND_INTERVAL_MS || lastSendTime == 0) {
        lastSendTime = now;

        if (isOnline) {
            timeClient.update();
            lastEpochSync    = timeClient.getEpochTime();
            millisAtLastSync = millis();
        }

        if (isOnline && firebaseReady) {
            sendSensorData(latestPh, latestLiter, latestPersen);
        } else {
            logOfflineData(latestPh, latestLiter, latestPersen);
        }

        checkSchedule();
    }

    delay(50);
}

// ──────────────────────────────────────────
// FUNGSI: Baca Sensor
// ──────────────────────────────────────────
void readSensors() {
    latestPh       = readPH();
    float levelCm  = readWaterLevel();
    latestLiter    = levelToLiter(levelCm);
    latestPersen   = constrain((latestLiter / TANK_VOLUME_LITER) * 100.0f, 0, 100);
}

// ──────────────────────────────────────────
// FUNGSI: Perbarui LCD (Flicker-Free, Fixed Width 16 Chars)
// ──────────────────────────────────────────
void updateLCD(float ph, float liter, float persen) {
    char statusChar = ' ';
    if (!isOnline) {
        statusChar = 'O'; // Offline
    } else if (isAutoMode) {
        statusChar = 'A'; // Auto
    } else {
        statusChar = 'M'; // Manual
    }

    char isiChar = (digitalRead(RELAY_ISI_PIN) == LOW) ? 'I' : ' ';
    char buangChar = (digitalRead(RELAY_BUANG_PIN) == LOW) ? 'B' : ' ';
    char minumChar = (digitalRead(RELAY_MINUM_PIN) == LOW) ? 'M' : ' ';

    char line1[17];
    char line2[17];
    snprintf(line1, sizeof(line1), "pH:%-4.2f %c [%c%c%c] ", ph, statusChar, isiChar, buangChar, minumChar);
    snprintf(line2, sizeof(line2), "Air:%-4.2fL %3d%%  ", liter, (int)persen);

    lcd.setCursor(0, 0);
    lcd.print(line1);
    lcd.setCursor(0, 1);
    lcd.print(line2);
}

// ──────────────────────────────────────────
// FUNGSI: Kirim data sensor ke Firebase (Dioptimalkan jadi 3 updateNode)
// ──────────────────────────────────────────
void sendSensorData(float ph, float liter, float persen) {
    if (!Firebase.ready()) return;

    String tanggal = epochToDate(timeClient.getEpochTime());
    String waktu   = epochToTime(timeClient.getEpochTime());
    String key     = tanggal + "_" + waktu;
    key.replace(":", "-");

    float ml = liter * 1000.0f;
    Serial.printf("\n[ONLINE] %s %s | pH:%.2f | Air:%.0fml / %.3fL (%.1f%%)\n",
                  tanggal.c_str(), waktu.c_str(), ph, ml, liter, persen);

    // ── 1. Update node /monitoring/key ──
    FirebaseJson jsonMonitoring;
    jsonMonitoring.set("ph", ph);
    jsonMonitoring.set("tanggal", tanggal);
    jsonMonitoring.set("waktu", waktu);
    
    if (Firebase.RTDB.updateNode(&fbdo, "/monitoring/" + key, &jsonMonitoring)) {
        Serial.println("  ✅ Data monitoring terkirim.");
    } else {
        Serial.printf("  ❌ Gagal kirim monitoring: %s\n", fbdo.errorReason().c_str());
    }

    // ── 2. Update node /kontrol_status ──
    FirebaseJson jsonStatus;
    jsonStatus.set("kapasitas_liter", liter);
    jsonStatus.set("kapasitas_persen", persen);
    jsonStatus.set("ph_terkini", ph);
    jsonStatus.set("last_seen/.sv", "timestamp");
    jsonStatus.set("rssi", WiFi.status() == WL_CONNECTED ? (int)WiFi.RSSI() : -100);
    jsonStatus.set("sensor_ph_connected", isPhConnected);
    jsonStatus.set("sensor_ultrasonic_connected", isUltrasonicConnected);

    if (Firebase.RTDB.updateNode(&fbdo, "/kontrol_status", &jsonStatus)) {
        Serial.println("  ✅ Data status terkirim.");
    } else {
        Serial.printf("  ❌ Gagal kirim status: %s\n", fbdo.errorReason().c_str());
    }

    // ── 3. Update node /volume_air/daily/tanggal ──
    FirebaseJson jsonVolume;
    jsonVolume.set("liter", liter);
    jsonVolume.set("label", "Hari Ini");

    if (Firebase.RTDB.updateNode(&fbdo, "/volume_air/daily/" + tanggal, &jsonVolume)) {
        Serial.println("  ✅ Data volume harian terkirim.");
    } else {
        Serial.printf("  ❌ Gagal kirim volume harian: %s\n", fbdo.errorReason().c_str());
    }
}

// ──────────────────────────────────────────
// FUNGSI: Kirim heartbeat status WiFi (Dioptimalkan jadi 1 updateNode)
// ──────────────────────────────────────────
void sendHeartbeat(bool wifiOk) {
    if (!Firebase.ready()) return;

    FirebaseJson jsonHB;
    int rssiVal = (wifiOk && WiFi.status() == WL_CONNECTED) ? (int)WiFi.RSSI() : -100;
    jsonHB.set("wifi_online", wifiOk);
    jsonHB.set("rssi", rssiVal);
    jsonHB.set("last_seen/.sv", "timestamp");

    if (Firebase.RTDB.updateNode(&fbdo, "/kontrol_status", &jsonHB)) {
        Serial.printf("[HB] wifi_online=%s rssi=%d\n", wifiOk ? "true" : "false", rssiVal);
    } else {
        Serial.printf("[HB ERR] Gagal kirim heartbeat: %s\n", fbdo.errorReason().c_str());
    }
}

// ──────────────────────────────────────────
// FUNGSI: Logging data sensor saat OFFLINE
// ──────────────────────────────────────────
void logOfflineData(float ph, float liter, float persen) {
    float ml = liter * 1000.0f;
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

    // Menggunakan kalibrasi 2-titik dari config.h
    // Slope = (pH7 - pH4) / (Volt7 - Volt4)
    float slope = (7.0f - 4.0f) / (PH7_VOLTAGE - PH4_VOLTAGE);
    
    // Rumus linear: pH = Slope * (Volt_ukur - Volt7) + 7.0
    float ph = slope * (volt - PH7_VOLTAGE) + 7.0f;

    // Debug ke Serial Monitor
    Serial.printf("[pH] ADC:%.0f  Volt:%.4fV  pH:%.2f  Connected:%s\n", avgAdc, volt, ph, isPhConnected ? "YES" : "NO");

    return constrain(ph, 0.0f, 14.0f);
}

// ──────────────────────────────────────────
// FUNGSI: Baca level air (HC-SR04)
// ──────────────────────────────────────────
float readWaterLevel() {
    const int SAMPLES = 5;
    float readings[SAMPLES];
    int validCount = 0;

    for (int i = 0; i < SAMPLES; i++) {
        digitalWrite(TRIG_PIN, LOW);  delayMicroseconds(2);
        digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
        digitalWrite(TRIG_PIN, LOW);

        long dur = pulseIn(ECHO_PIN, HIGH, 30000);
        if (dur > 0) {
            readings[validCount++] = (dur * 0.034f) / 2.0f;  // cm
        }
        delay(20);
    }

    if (validCount == 0) {
        Serial.println("[ULTRASONIC] ⚠️ Tidak ada echo! Cek kabel/sensor.");
        isUltrasonicConnected = false;
        return 0.0f;
    }

    isUltrasonicConnected = true;

    for (int i = 0; i < validCount - 1; i++) {
        for (int j = i + 1; j < validCount; j++) {
            if (readings[i] > readings[j]) {
                float tmp = readings[i]; readings[i] = readings[j]; readings[j] = tmp;
            }
        }
    }
    float distCm = readings[validCount / 2];

    float distRange  = SENSOR_DIST_EMPTY_CM - SENSOR_DIST_FULL_CM;
    float waterRatio = (SENSOR_DIST_EMPTY_CM - distCm) / distRange;
    float waterCm    = waterRatio * TANK_HEIGHT_CM;
    float waterCm_c  = constrain(waterCm, 0.0f, TANK_HEIGHT_CM);

    Serial.printf("[ULTRASONIC] Jarak:%.2fcm | TinggiAir:%.2fcm (%.1f%%)\n",
                  distCm, waterCm_c, (waterCm_c / TANK_HEIGHT_CM) * 100.0f);

    return waterCm_c;
}

// ──────────────────────────────────────────
// FUNGSI: Konversi tinggi air (cm) → Volume (liter)
// ──────────────────────────────────────────
float levelToLiter(float h) {
    const float PI_R2 = 3.14159265f * TANK_RADIUS_CM * TANK_RADIUS_CM;
    float volumeCm3   = PI_R2 * h;
    float volumeLiter = volumeCm3 / 1000.0f;
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
// FUNGSI: Polling Firebase (Dioptimalkan jadi 1 request JSON)
// ──────────────────────────────────────────
void pollFirebase() {
    if (!Firebase.ready()) return;

    if (Firebase.RTDB.getJSON(&fbdo, "/kontrol")) {
        if (fbdo.dataType() == "json") {
            FirebaseJson &json = fbdo.jsonObject();
            FirebaseJsonData result;

            // 1. Relay Isi
            if (json.get(result, "relay_isi") && result.success) {
                bool state = result.boolValue;
                digitalWrite(RELAY_ISI_PIN, state ? LOW : HIGH);
                static bool lastRelayIsi = false;
                if (state != lastRelayIsi) {
                    lastRelayIsi = state;
                    Serial.printf("[FIREBASE] relay_isi: %s\n", state ? "ON" : "OFF");
                }
            }

            // 2. Relay Buang
            if (json.get(result, "relay_buang") && result.success) {
                bool state = result.boolValue;
                digitalWrite(RELAY_BUANG_PIN, state ? LOW : HIGH);
                static bool lastRelayBuang = false;
                if (state != lastRelayBuang) {
                    lastRelayBuang = state;
                    Serial.printf("[FIREBASE] relay_buang: %s\n", state ? "ON" : "OFF");
                }
            }

            // 3. Relay Minum
            if (json.get(result, "relay_minum") && result.success) {
                bool state = result.boolValue;
                digitalWrite(RELAY_MINUM_PIN, state ? LOW : HIGH);
                static bool lastRelayMinum = false;
                if (state != lastRelayMinum) {
                    lastRelayMinum = state;
                    Serial.printf("[FIREBASE] relay_minum: %s\n", state ? "ON" : "OFF");
                }
            }

            // 4. Mode Otomatis
            if (json.get(result, "otomatis") && result.success) {
                isAutoMode = result.boolValue;
            }

            // 5. Cek Sekarang (Manual Trigger)
            if (json.get(result, "cek_sekarang") && result.success) {
                bool cekSekarang = result.boolValue;
                if (cekSekarang) {
                    Serial.println("[FIREBASE] Perintah Cek Manual diterima!");
                    
                    // Baca ulang sensor secara langsung & update tampilan
                    readSensors();
                    updateLCD(latestPh, latestLiter, latestPersen);
                    
                    // Kirim data langsung ke Firebase
                    sendSensorData(latestPh, latestLiter, latestPersen);
                    
                    // Reset flag cek_sekarang di Firebase
                    Firebase.RTDB.setBool(&fbdo, "/kontrol/cek_sekarang", false);
                }
            }
        }
    } else {
        Serial.printf("[POLL ERR] Gagal membaca /kontrol: %s\n", fbdo.errorReason().c_str());
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
// ──────────────────────────────────────────
void connectFirebase() {
    firebaseConfig.api_key      = FIREBASE_API_KEY;
    firebaseConfig.database_url = FIREBASE_DATABASE_URL;

    firebaseAuth.user.email    = FIREBASE_USER_EMAIL;
    firebaseAuth.user.password = FIREBASE_USER_PASSWORD;

    // Buffer SSL dioptimalkan untuk hemat memori & stabil
    fbdo.setBSSLBufferSize(4096, 1024);

    Firebase.reconnectWiFi(true);

    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Init Firebase...");
    lcd.setCursor(0, 1); lcd.print("Mohon tunggu... ");
    Serial.println("Memanggil Firebase.begin()...");

    Firebase.begin(&firebaseConfig, &firebaseAuth);

    Serial.print("Menghubungkan ke Firebase");
    unsigned long waitStart = millis();
    int dotCount = 0;
    while (!Firebase.ready() && millis() - waitStart < 10000) {
        delay(300);
        Serial.print(".");

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
    time_t rawtime = (time_t)epoch;
    struct tm* t = gmtime(&rawtime);
    if (t == NULL) return "1970-01-01";
    char buf[12];
    sprintf(buf, "%04d-%02d-%02d", t->tm_year + 1900, t->tm_mon + 1, t->tm_mday);
    return String(buf);
}

String epochToTime(unsigned long epoch) {
    time_t rawtime = (time_t)epoch;
    struct tm* t = gmtime(&rawtime);
    if (t == NULL) return "00:00:00";
    char buf[10];
    sprintf(buf, "%02d:%02d:%02d", t->tm_hour, t->tm_min, t->tm_sec);
    return String(buf);
}