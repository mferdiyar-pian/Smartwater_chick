// ============================================================
// main.cpp — Smart Water Chick — Firmware ESP32
// Platform: PlatformIO + Arduino Framework
// Fitur: BLE Provisioning + Firebase + Mode Offline
// ============================================================
// Logika Mode:
//   1. BELUM ADA WiFi tersimpan → BLE Mode (setup pertama)
//   2. Ada WiFi tersimpan TAPI gagal/mati → OFFLINE Mode
//      - Sensor & Jadwal pompa tetap berjalan
//      - Terus mencoba reconnect WiFi di latar belakang
//      - Begitu WiFi kembali → otomatis connect Firebase
//   3. WiFi + Firebase OK → Mode Normal (kirim data ke cloud)
// ============================================================

#include <Arduino.h>
#include <WiFi.h>
#include <Preferences.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Firebase_ESP_Client.h>
#include <DHT.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "config.h"
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

// ──────────────────────────────────────────
// UUID BLE — harus sama dengan BleWifiSetupActivity.java
// ──────────────────────────────────────────
#define SERVICE_UUID     "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR_SSID_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR_PASS_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a9"
#define CHAR_STATUS_UUID "beb5483e-36e1-4688-b7f5-ea07361b26aa"

// Interval retry koneksi WiFi saat offline (ms)
#define WIFI_RETRY_INTERVAL_MS 30000

// ──────────────────────────────────────────
// OBJEK GLOBAL
// ──────────────────────────────────────────
LiquidCrystal_I2C lcd(0x27, 16, 2); // Alamat I2C: 0x27 (umum) atau 0x3F

FirebaseData fbdo;
FirebaseData fbdoCommand;
FirebaseAuth firebaseAuth;
FirebaseConfig firebaseConfig;

DHT dht(DHT_PIN, DHT_TYPE);
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", TIMEZONE_OFFSET, 60000);
Preferences preferences;

// BLE
BLEServer* pServer = nullptr;
BLECharacteristic* pStatusChar = nullptr;
bool bleDeviceConnected = false;
bool wifiCredentialsReceived = false;
String pendingSsid = "";
String pendingPass = "";

// Status sistem
bool isBleMode = false;      // Mode BLE (setup pertama, belum ada WiFi tersimpan)
bool isOnline = false;       // WiFi terhubung
bool firebaseReady = false;  // Firebase terhubung

bool isAutoMode = false;
bool schedule07 = false, schedule15 = false, schedule22 = false;

unsigned long lastSendTime = 0;
unsigned long lastWifiRetryTime = 0;

// Estimasi waktu offline menggunakan millis()
unsigned long lastEpochSync = 0;    // Epoch time saat NTP terakhir berhasil
unsigned long millisAtLastSync = 0; // millis() saat NTP terakhir berhasil

// WiFi tersimpan
String savedSsid = "";
String savedPass = "";

// ──────────────────────────────────────────
// DEKLARASI FUNGSI
// ──────────────────────────────────────────
void startBleProvisioning();
bool connectToWifi(String ssid, String pass, bool silent = false);
void connectFirebase();
void readAndSendSensorData();
void runOfflineTasks();
float readPH();
float readWaterLevel();
float levelToLiter(float heightCm);
void controlPump(bool fillOn, bool drainOn);
void handleCommand(String command);
void listenFirebaseStream();
void checkSchedule();
void notifyBleStatus(String status);
unsigned long getEstimatedEpoch();
String epochToDate(unsigned long epoch);
String epochToTime(unsigned long epoch);
int getHourFromEpoch(unsigned long epoch);
int getMinuteFromEpoch(unsigned long epoch);

// ──────────────────────────────────────────
// BLE CALLBACKS
// ──────────────────────────────────────────
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* p)    {
        bleDeviceConnected = true;
        Serial.println("[BLE] Aplikasi terhubung!");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("App Terhubung! ");
        lcd.setCursor(0, 1); lcd.print("Kirim Info WiFi");
    }
    void onDisconnect(BLEServer* p) {
        bleDeviceConnected = false;
        BLEDevice::startAdvertising();
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Mode Setup BLE ");
        lcd.setCursor(0, 1); lcd.print("Buka App di HP ");
    }
};

class SsidCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pChar) {
        pendingSsid = String(pChar->getValue().c_str());
        Serial.println("[BLE] SSID: " + pendingSsid);
    }
};

class PassCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pChar) {
        pendingPass = String(pChar->getValue().c_str());
        wifiCredentialsReceived = true;
        Serial.println("[BLE] Password diterima.");
    }
};

// ──────────────────────────────────────────
// SETUP
// ──────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    Serial.println("\n===== Smart Water Chick ESP32 =====");

    // ── Inisialisasi LCD I2C ──
    Wire.begin();
    lcd.init();
    lcd.backlight();
    lcd.setCursor(0, 0); lcd.print("Smart Water     ");
    lcd.setCursor(0, 1); lcd.print("Chick Start...  ");
    delay(1500);

    pinMode(RELAY_ISI_PIN, OUTPUT);
    pinMode(RELAY_BUANG_PIN, OUTPUT);
    pinMode(TRIG_PIN, OUTPUT);
    pinMode(ECHO_PIN, INPUT);
    digitalWrite(RELAY_ISI_PIN, HIGH);
    digitalWrite(RELAY_BUANG_PIN, HIGH);
    dht.begin();

    // Baca WiFi tersimpan
    preferences.begin("wifi-creds", false);
    savedSsid = preferences.getString("ssid", "");
    savedPass = preferences.getString("pass", "");
    preferences.end();

    if (savedSsid.length() == 0) {
        // ── SKENARIO 1: Belum ada WiFi → BLE Mode ──
        Serial.println("Belum ada WiFi tersimpan → Mode BLE (Setup Pertama)");
        isBleMode = true;
        startBleProvisioning();
    } else {
        // ── SKENARIO 2 & 3: Ada WiFi tersimpan → Coba connect ──
        Serial.println("WiFi tersimpan: " + savedSsid + " → Mencoba connect...");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Koneksi WiFi... ");
        lcd.setCursor(0, 1); lcd.print(savedSsid.substring(0, 16));
        bool ok = connectToWifi(savedSsid, savedPass);

        if (ok) {
            // WiFi berhasil → Init NTP & Firebase
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("WiFi Terhubung! ");
            lcd.setCursor(0, 1); lcd.print(WiFi.localIP().toString());
            delay(1000);
            
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Sinkron Waktu...");
            Serial.println("Memulai NTP...");
            timeClient.begin();
            timeClient.setTimeOffset(TIMEZONE_OFFSET);
            timeClient.update();
            lastEpochSync = timeClient.getEpochTime();
            millisAtLastSync = millis();
            
            Serial.println("Memulai Firebase...");
            connectFirebase();
            isOnline = true;
        } else {
            // WiFi gagal → OFFLINE Mode (bukan BLE Mode!)
            Serial.println("");
            Serial.println("═══════════════════════════════════════");
            Serial.println("  ⚠️  MODE OFFLINE AKTIF");
            Serial.println("  Sensor & Jadwal tetap berjalan.");
            Serial.println("  Akan retry WiFi setiap 30 detik...");
            Serial.println("═══════════════════════════════════════");
            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("[OFFLINE] MODE  ");
            lcd.setCursor(0, 1); lcd.print("Retry WiFi 30s  ");
            delay(1500);
            isOnline = false;
            isBleMode = false;
        }
    }
}

// ──────────────────────────────────────────
// LOOP UTAMA
// ──────────────────────────────────────────
void loop() {

    // ─── CABANG A: Mode BLE (setup pertama) ───
    if (isBleMode) {
        if (wifiCredentialsReceived) {
            wifiCredentialsReceived = false;
            notifyBleStatus("CONNECTING...");

            lcd.clear();
            lcd.setCursor(0, 0); lcd.print("Koneksi WiFi... ");
            lcd.setCursor(0, 1); lcd.print(pendingSsid.substring(0, 16));

            bool ok = connectToWifi(pendingSsid, pendingPass);
            if (ok) {
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("WiFi Terhubung! ");
                lcd.setCursor(0, 1); lcd.print(WiFi.localIP().toString());
                delay(1000);

                // Simpan ke flash
                preferences.begin("wifi-creds", false);
                preferences.putString("ssid", pendingSsid);
                preferences.putString("pass", pendingPass);
                preferences.end();
                savedSsid = pendingSsid;
                savedPass = pendingPass;

                notifyBleStatus("CONNECTED:" + WiFi.localIP().toString());
                
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("Setup Berhasil! ");
                lcd.setCursor(0, 1); lcd.print("Restarting...   ");
                delay(2000);

                // Restart ESP32 untuk membersihkan memori RAM dari proses BLE
                // Setelah nyala, otomatis masuk ke skenario koneksi WiFi normal
                ESP.restart();
            } else {
                notifyBleStatus("FAILED:WiFi tidak dapat terhubung");
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("WiFi GAGAL!     ");
                lcd.setCursor(0, 1); lcd.print("Cek SSID/Pass   ");
            }
        }
        delay(100);
        return;
    }

    // ─── CABANG B: Mode Offline — Coba reconnect WiFi ───
    if (!isOnline) {
        unsigned long now = millis();
        if (now - lastWifiRetryTime >= WIFI_RETRY_INTERVAL_MS || lastWifiRetryTime == 0) {
            lastWifiRetryTime = now;
            Serial.println("[WiFi] Mencoba reconnect ke: " + savedSsid);

            bool ok = connectToWifi(savedSsid, savedPass, true); // silent=true
            if (ok) {
                Serial.println("[WiFi] ✅ Kembali online!");
                isOnline = true;
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("Kembali Online! ");
                lcd.setCursor(0, 1); lcd.print(WiFi.localIP().toString());
                delay(1000);

                // Sinkronisasi waktu yang hilang selama offline
                timeClient.begin();
                timeClient.update();
                lastEpochSync = timeClient.getEpochTime();
                millisAtLastSync = millis();

                // Sambungkan kembali ke Firebase
                if (!firebaseReady) {
                    connectFirebase();
                }
            } else {
                Serial.println("[WiFi] ❌ Masih offline. Retry dalam 30 detik.");
                lcd.clear();
                lcd.setCursor(0, 0); lcd.print("[OFFLINE] MODE  ");
                lcd.setCursor(0, 1); lcd.print("Retry WiFi 30s  ");
            }
        }
    }

    // ─── CABANG C: Mode Normal (Online) — Kirim data & dengarkan perintah ───
    if (isOnline && firebaseReady) {
        if (Firebase.isTokenExpired()) Firebase.refreshToken(&firebaseConfig);
        listenFirebaseStream();

        // Cek apakah WiFi masih konek
        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("[WiFi] Koneksi terputus. Masuk mode offline...");
            isOnline = false;
            firebaseReady = false;
        }
    }

    // ─── SELALU JALAN (Online maupun Offline) ───
    unsigned long now = millis();
    if (now - lastSendTime >= SEND_INTERVAL_MS || lastSendTime == 0) {
        lastSendTime = now;

        if (isOnline) {
            timeClient.update();
            lastEpochSync = timeClient.getEpochTime();
            millisAtLastSync = millis();
        }

        if (isOnline && firebaseReady) {
            // Online: Baca sensor & kirim ke Firebase
            readAndSendSensorData();
        } else {
            // Offline: Baca sensor & tampilkan di Serial saja
            runOfflineTasks();
        }

        // Jadwal pompa tetap jalan di kedua mode
        checkSchedule();
    }

    delay(100);
}

// ──────────────────────────────────────────
// FUNGSI: Jalankan tugas sensor saat offline
// Membaca sensor & menampilkan di Serial Monitor
// ──────────────────────────────────────────
void runOfflineTasks() {
    float suhu = dht.readTemperature();
    float kelembaban = dht.readHumidity();
    if (isnan(suhu) || isnan(kelembaban)) { suhu = 0; kelembaban = 0; }
    float ph = readPH();
    float levelCm = readWaterLevel();
    float liter = levelToLiter(levelCm);
    float persen = constrain((liter / TANK_VOLUME_LITER) * 100.0f, 0, 100);

    // Update Layar LCD
    lcd.clear();
    lcd.setCursor(0, 0); lcd.printf("S:%.1fC pH:%.1f   ", suhu, ph);
    lcd.setCursor(0, 1); lcd.printf("Air:%.1fL [OFL]  ", liter);

    unsigned long estEpoch = getEstimatedEpoch();

    Serial.println("\n[OFFLINE] ── Data Sensor ──────────────────");
    if (estEpoch > 0) {
        Serial.printf("  Waktu Estimasi : %s %s\n", epochToDate(estEpoch).c_str(), epochToTime(estEpoch).c_str());
    } else {
        Serial.println("  Waktu          : Tidak diketahui (belum pernah online)");
    }
    Serial.printf("  Suhu           : %.1f °C\n", suhu);
    Serial.printf("  Kelembaban     : %.1f %%\n", kelembaban);
    Serial.printf("  pH Air         : %.2f\n", ph);
    Serial.printf("  Volume Air     : %.1f L (%.0f%%)\n", liter, persen);
    Serial.println("  [Data TIDAK dikirim — offline]");
    Serial.println("────────────────────────────────────────────");
}

// ──────────────────────────────────────────
// FUNGSI: Estimasi waktu saat offline
// Menggunakan epoch terakhir + selisih millis()
// ──────────────────────────────────────────
unsigned long getEstimatedEpoch() {
    if (lastEpochSync == 0) return 0; // Belum pernah sync NTP
    unsigned long elapsedSec = (millis() - millisAtLastSync) / 1000;
    return lastEpochSync + elapsedSec;
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

// ──────────────────────────────────────────
// FUNGSI: Cek & jalankan jadwal pompa
// Bekerja baik online maupun offline
// ──────────────────────────────────────────
void checkSchedule() {
    if (!isAutoMode) return;

    unsigned long epoch = isOnline ? timeClient.getEpochTime() : getEstimatedEpoch();
    if (epoch == 0) return; // Belum pernah sync waktu, skip

    int jam = getHourFromEpoch(epoch);
    int menit = getMinuteFromEpoch(epoch);
    if (menit > 1) return; // Hanya jalankan di menit ke-0 dan ke-1

    if ((schedule07 && jam == 7) || (schedule15 && jam == 15) || (schedule22 && jam == 22)) {
        Serial.printf("[JADWAL] Jam %02d:00 — Isi air otomatis (online=%s)\n",
                      jam, isOnline ? "YA" : "TIDAK");
        controlPump(true, false);
        delay(10000);
        controlPump(false, false);
    }
}

// ──────────────────────────────────────────
// FUNGSI: Start BLE Provisioning
// ──────────────────────────────────────────
void startBleProvisioning() {
    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Inisialisasi BLE");
    lcd.setCursor(0, 1); lcd.print("Mohon tunggu... ");

    BLEDevice::init("SmartWaterChick");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService* pService = pServer->createService(SERVICE_UUID);

    BLECharacteristic* pSsidChar = pService->createCharacteristic(
        CHAR_SSID_UUID, BLECharacteristic::PROPERTY_WRITE);
    pSsidChar->setCallbacks(new SsidCallback());

    BLECharacteristic* pPassChar = pService->createCharacteristic(
        CHAR_PASS_UUID, BLECharacteristic::PROPERTY_WRITE);
    pPassChar->setCallbacks(new PassCallback());

    pStatusChar = pService->createCharacteristic(
        CHAR_STATUS_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    pStatusChar->addDescriptor(new BLE2902());
    pStatusChar->setValue("WAITING");

    pService->start();
    BLEAdvertising* pAdv = BLEDevice::getAdvertising();
    pAdv->addServiceUUID(SERVICE_UUID);
    pAdv->setScanResponse(true);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] Advertising aktif. Menunggu setup dari aplikasi...");

    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Mode Setup BLE  ");
    lcd.setCursor(0, 1); lcd.print("Buka App di HP  ");
}

void notifyBleStatus(String status) {
    if (pStatusChar && bleDeviceConnected) {
        pStatusChar->setValue(status.c_str());
        pStatusChar->notify();
    }
}

// ──────────────────────────────────────────
// FUNGSI: Koneksi WiFi
// silent=true → tidak print progress dots
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
    firebaseConfig.api_key = FIREBASE_API_KEY;
    firebaseConfig.database_url = FIREBASE_DATABASE_URL;

    // Aktifkan koneksi tanpa akun (Anonim/Public)
    firebaseConfig.signer.test_mode = true;

    // Mencegah Crash (Out of Memory) dengan mengecilkan buffer SSL Firebase
    fbdo.setBSSLBufferSize(2048, 1024);
    fbdoCommand.setBSSLBufferSize(2048, 1024);

    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Init Firebase...");
    Serial.println("Memanggil Firebase.begin()...");

    Firebase.begin(&firebaseConfig, &firebaseAuth);
    // HAPUS Firebase.reconnectWiFi(true) karena sering bentrok (kita sudah handle WiFi reconnect secara manual di loop)

    lcd.clear();
    lcd.setCursor(0, 0); lcd.print("Koneksi Cloud...");
    lcd.setCursor(0, 1); lcd.print("Firebase...     ");

    Serial.print("Menghubungkan ke Firebase");
    unsigned long waitStart = millis();
    while (!Firebase.ready() && millis() - waitStart < 15000) {
        delay(500); Serial.print(".");
    }

    if (Firebase.ready()) {
        Serial.println("\n✅ Firebase Terhubung!");
        firebaseReady = true;
        Firebase.RTDB.beginStream(&fbdoCommand, "/kontrol");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Firebase OK!    ");
        lcd.setCursor(0, 1); lcd.print("Sistem Siap     ");
        delay(1000);
    } else {
        Serial.println("\n⚠️ Firebase gagal. Akan retry saat WiFi stabil.");
        lcd.clear();
        lcd.setCursor(0, 0); lcd.print("Firebase GAGAL! ");
        lcd.setCursor(0, 1); lcd.print("Mode Online-Lmtd");
        delay(1500);
    }
}

// ──────────────────────────────────────────
// FUNGSI: Baca & kirim data sensor
// ──────────────────────────────────────────
void readAndSendSensorData() {
    float suhu = dht.readTemperature();
    float kelembaban = dht.readHumidity();
    if (isnan(suhu) || isnan(kelembaban)) { suhu = 0; kelembaban = 0; }
    float ph = readPH();
    float levelCm = readWaterLevel();
    float liter = levelToLiter(levelCm);
    float persen = constrain((liter / TANK_VOLUME_LITER) * 100.0f, 0, 100);

    // ── Update Layar LCD ──
    lcd.clear();
    lcd.setCursor(0, 0); lcd.printf("S:%.1fC pH:%.1f   ", suhu, ph);
    lcd.setCursor(0, 1); lcd.printf("Air:%.1fL %d%%    ", liter, (int)persen);

    String tanggal = epochToDate(timeClient.getEpochTime());
    String waktu = epochToTime(timeClient.getEpochTime());
    String key = tanggal + "_" + waktu;
    key.replace(":", "-");

    Serial.printf("\n[ONLINE] %s %s | Suhu:%.1f pH:%.2f Air:%.0f%%\n",
                  tanggal.c_str(), waktu.c_str(), suhu, ph, persen);

    String base = "/monitoring/" + key;
    Firebase.RTDB.setFloat(&fbdo, base + "/suhu", suhu);
    Firebase.RTDB.setFloat(&fbdo, base + "/kelembaban", kelembaban);
    Firebase.RTDB.setFloat(&fbdo, base + "/ph", ph);
    Firebase.RTDB.setString(&fbdo, base + "/tanggal", tanggal);
    Firebase.RTDB.setString(&fbdo, base + "/waktu", waktu);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/kapasitas_liter", liter);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/kapasitas_persen", persen);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/ph_terkini", ph);
    Firebase.RTDB.setFloat(&fbdo, "/kontrol_status/suhu_terkini", suhu);
    Firebase.RTDB.setFloat(&fbdo, "/volume_air/daily/" + tanggal + "/liter", liter);
    Firebase.RTDB.setString(&fbdo, "/volume_air/daily/" + tanggal + "/label", "Hari Ini");

    Serial.println("  ✅ Data dikirim.");
}

float readPH() {
    long sum = 0;
    for (int i = 0; i < 10; i++) { sum += analogRead(PH_PIN); delay(10); }
    float voltage = (sum / 10.0f) * (3.3f / 4095.0f);
    float ph = 7.0f + ((PH_NEUTRAL_VOLTAGE - voltage) / PH_SLOPE) * (-1);
    return constrain(ph, 0.0f, 14.0f);
}

float readWaterLevel() {
    digitalWrite(TRIG_PIN, LOW); delayMicroseconds(2);
    digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);
    long dur = pulseIn(ECHO_PIN, HIGH, 30000);
    if (dur == 0) return 0;
    float dist = (dur * 0.034f) / 2.0f;
    return constrain(TANK_HEIGHT_CM - (dist - SENSOR_TO_WATER_FULL_CM), 0.0f, TANK_HEIGHT_CM);
}

float levelToLiter(float h) { return (h / TANK_HEIGHT_CM) * TANK_VOLUME_LITER; }

void controlPump(bool fillOn, bool drainOn) {
    digitalWrite(RELAY_ISI_PIN,   fillOn  ? LOW : HIGH);
    digitalWrite(RELAY_BUANG_PIN, drainOn ? LOW : HIGH);
}

void handleCommand(String cmd) {
    if (cmd == "isi_air") {
        controlPump(true, false); delay(5000); controlPump(false, false);
        Firebase.RTDB.setString(&fbdo, "/kontrol/perintah", "");
    } else if (cmd == "buang_air") {
        controlPump(false, true); delay(5000); controlPump(false, false);
        Firebase.RTDB.setString(&fbdo, "/kontrol/perintah", "");
    }
}

void listenFirebaseStream() {
    if (!Firebase.ready()) return;
    if (!Firebase.RTDB.readStream(&fbdoCommand)) {
        if (fbdoCommand.streamTimeout())
            Firebase.RTDB.beginStream(&fbdoCommand, "/kontrol");
        return;
    }
    if (!fbdoCommand.streamAvailable()) return;

    String path = fbdoCommand.dataPath();
    if (path == "/perintah" && fbdoCommand.stringData().length() > 0)
        handleCommand(fbdoCommand.stringData());
    else if (path == "/otomatis") isAutoMode = fbdoCommand.boolData();
    else if (path == "/jadwal_07") schedule07 = fbdoCommand.boolData();
    else if (path == "/jadwal_15") schedule15 = fbdoCommand.boolData();
    else if (path == "/jadwal_22") schedule22 = fbdoCommand.boolData();
}
