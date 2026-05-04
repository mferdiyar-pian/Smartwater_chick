/**
 * ============================================================
 *  SmartWaterChick – Firmware ESP32
 *  Platform : PlatformIO (VS Code)
 *  Framework: Arduino
 * ============================================================
 *
 *  SENSOR & KOMPONEN:
 *    - Sensor pH analog (ADC)          → GPIO 34 (A0 ESP32)
 *    - Sensor ultrasonik HC-SR04       → TRIG GPIO 5 / ECHO GPIO 18
 *    - Relay pompa air                 → GPIO 23 (Active-LOW)
 *    - LCD I2C 16×2                    → SDA GPIO 21 / SCL GPIO 22
 *    - (Opsional) Sensor suhu DS18B20  → GPIO 4
 *
 *  FIREBASE REALTIME DATABASE – struktur node:
 *    /monitoring
 *      /ph          : float  – nilai pH air
 *      /jarak       : float  – jarak ultrasonik (cm)
 *      /tinggi_air  : float  – tinggi air estimasi (cm)
 *      /persen_air  : float  – persentase air (0–100 %)
 *      /relay       : bool   – status relay saat ini
 *      /kondisi_ph  : String – "BAIK" / "RENDAH" / "TINGGI"
 *    /kontrol
 *      /otomatis    : bool   – mode otomatis aktif?
 *      /isi_air     : bool   – perintah manual: nyalakan relay
 *      /buang_air   : bool   – perintah manual: matikan relay
 *    /config
 *      /ph_min      : float  – batas bawah pH (default 6.5)
 *      /ph_max      : float  – batas atas  pH (default 8.0)
 *      /tinggi_tangki : float – tinggi fisik tangki (cm, default 30)
 * ============================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <LiquidCrystal_I2C.h>

// ============================================================
//  KONFIGURASI – Ganti sesuai jaringan & Firebase Anda
// ============================================================
#define WIFI_SSID       "Awikwok"
#define WIFI_PASSWORD   "12334566"
#define API_KEY         "AIzaSyBX_GuuTxQi5uHxPJ7i-pnaki95eAFWTMs"
#define DATABASE_URL    "https://smartwater-chick-77b9d-default-rtdb.firebaseio.com"

// ============================================================
//  PIN DEFINITIONS
// ============================================================
#define PH_PIN          34          // ADC1_CH6 – sensor pH analog
#define RELAY_PIN       23          // Relay pompa (Active-LOW)
#define TRIG_PIN        5           // HC-SR04 Trigger
#define ECHO_PIN        18          // HC-SR04 Echo

// ============================================================
//  KALIBRASI SENSOR pH
//  Ukur tegangan output saat probe dicelup larutan pH 4 & pH 7,
//  kemudian hitung slope & offset sesuai hasilnya.
// ============================================================
#define PH_ADC_VREF     3.3f        // Tegangan referensi ADC ESP32
#define PH_ADC_RES      4095.0f     // Resolusi 12-bit ESP32
#define PH_VOLT_AT_7    2.5f        // Tegangan saat pH = 7
#define PH_VOLT_PER_PH  0.18f       // ΔVolt per satuan pH (slope)

// ============================================================
//  PARAMETER TANGKI
// ============================================================
#define TINGGI_TANGKI_DEFAULT  30.0f   // cm – sesuaikan dengan tangki fisik
#define BATAS_PENUH_CM         3.0f    // Jarak sensor ke air saat tangki penuh
#define PH_MIN_DEFAULT         6.5f
#define PH_MAX_DEFAULT         8.0f

// ============================================================
//  INTERVAL WAKTU (milidetik)
// ============================================================
#define INTERVAL_SENSOR         2000UL   // Baca sensor setiap 2 detik
#define INTERVAL_FIREBASE       3000UL   // Kirim ke Firebase setiap 3 detik
#define INTERVAL_LCD            1000UL   // Refresh LCD setiap 1 detik

// ============================================================
//  OBJEK GLOBAL
// ============================================================
LiquidCrystal_I2C lcd(0x27, 16, 2);

FirebaseData   fbdo;
FirebaseData   fbdo_stream;   // stream terpisah agar tidak konflik
FirebaseAuth   auth;
FirebaseConfig config;

// Nilai sensor (volatile-friendly untuk multi-core)
float  g_pH        = 7.0f;
float  g_jarak     = 0.0f;
float  g_tinggiAir = 0.0f;
float  g_persenAir = 0.0f;
bool   g_relayOn   = false;

// Konfigurasi (dibaca dari Firebase)
float  g_phMin         = PH_MIN_DEFAULT;
float  g_phMax         = PH_MAX_DEFAULT;
float  g_tinggiTangki  = TINGGI_TANGKI_DEFAULT;

// Mode kontrol
bool   g_modeOtomatis  = true;

// Timestamp terakhir
unsigned long g_lastSensor   = 0;
unsigned long g_lastFirebase = 0;
unsigned long g_lastLCD      = 0;

// ============================================================
//  PROTOTYPES
// ============================================================
void     connectWiFi();
void     initFirebase();
void     bacaKonfigFirebase();
void     bacaSensor();
float    bacaPH();
float    bacaJarak();
void     kontrolRelay();
void     kirimKeFirebase();
void     updateLCD();
void     setRelay(bool nyala);
String   kondisiPH(float ph);

// ============================================================
//  SETUP
// ============================================================
void setup() {
    Serial.begin(115200);
    delay(200);

    Serial.println(F("\n========================================"));
    Serial.println(F("  SmartWaterChick – ESP32 Firmware v2.0"));
    Serial.println(F("========================================"));

    // Inisialisasi pin
    pinMode(RELAY_PIN, OUTPUT);
    setRelay(false);              // Relay mati dulu

    pinMode(TRIG_PIN, OUTPUT);
    pinMode(ECHO_PIN, INPUT);
    digitalWrite(TRIG_PIN, LOW);

    // ADC pH – pakai 12-bit
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);  // Range 0–3.3 V

    // Inisialisasi LCD
    lcd.init();
    lcd.backlight();
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(F("SmartWaterChick"));
    lcd.setCursor(0, 1);
    lcd.print(F("  Starting...   "));

    // Koneksi WiFi
    connectWiFi();

    // Inisialisasi Firebase
    initFirebase();

    // Baca konfigurasi awal dari Firebase
    bacaKonfigFirebase();

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(F("  SISTEM SIAP   "));
    delay(1500);
}

// ============================================================
//  LOOP UTAMA
// ============================================================
void loop() {
    unsigned long now = millis();

    // ── 1. Baca sensor setiap INTERVAL_SENSOR ──────────────
    if (now - g_lastSensor >= INTERVAL_SENSOR) {
        g_lastSensor = now;
        bacaSensor();
        kontrolRelay();

        // Tampilkan di Serial Monitor
        Serial.printf("[Sensor] pH=%.2f | Jarak=%.1f cm | Tinggi=%.1f cm | Air=%.0f%%\n",
                      g_pH, g_jarak, g_tinggiAir, g_persenAir);
        Serial.printf("[Status] Relay=%s | Mode=%s | Kondisi pH: %s\n",
                      g_relayOn ? "ON" : "OFF",
                      g_modeOtomatis ? "OTOMATIS" : "MANUAL",
                      kondisiPH(g_pH).c_str());
    }

    // ── 2. Kirim & ambil data Firebase setiap INTERVAL_FIREBASE
    if (Firebase.ready() && (now - g_lastFirebase >= INTERVAL_FIREBASE)) {
        g_lastFirebase = now;
        kirimKeFirebase();
        bacaKonfigFirebase();    // Refresh konfigurasi & perintah kontrol
    }

    // ── 3. Refresh tampilan LCD setiap INTERVAL_LCD ─────────
    if (now - g_lastLCD >= INTERVAL_LCD) {
        g_lastLCD = now;
        updateLCD();
    }

    // Yield agar watchdog tidak trip
    yield();
}

// ============================================================
//  FUNGSI: Koneksi WiFi dengan retry
// ============================================================
void connectWiFi() {
    Serial.printf("Menghubungkan ke WiFi: %s", WIFI_SSID);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    uint8_t retry = 0;
    while (WiFi.status() != WL_CONNECTED && retry < 30) {
        delay(500);
        Serial.print(".");
        retry++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.printf("\nWiFi terhubung! IP: %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println(F("\nGAGAL terhubung WiFi – lanjut offline"));
    }

    lcd.setCursor(0, 1);
    lcd.print(WiFi.status() == WL_CONNECTED ? "WiFi Terhubung! " : "WiFi GAGAL!     ");
    delay(1000);
}

// ============================================================
//  FUNGSI: Inisialisasi Firebase
// ============================================================
void initFirebase() {
    config.api_key      = API_KEY;
    config.database_url = DATABASE_URL;

    // Anonymous sign-in (tanpa akun – cocok untuk development)
    auth.user.email    = "";
    auth.user.password = "";

    config.token_status_callback = tokenStatusCallback;

    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    fbdo.setResponseSize(4096);

    Serial.println(F("Firebase: Menginisialisasi..."));
}

// ============================================================
//  FUNGSI: Baca konfigurasi & perintah kontrol dari Firebase
// ============================================================
void bacaKonfigFirebase() {
    if (!Firebase.ready()) return;

    // Mode otomatis
    if (Firebase.RTDB.getBool(&fbdo, "/kontrol/otomatis"))
        g_modeOtomatis = fbdo.boolData();

    // Ambil konfigurasi pH & tinggi tangki
    if (Firebase.RTDB.getFloat(&fbdo, "/config/ph_min"))
        g_phMin = fbdo.floatData();

    if (Firebase.RTDB.getFloat(&fbdo, "/config/ph_max"))
        g_phMax = fbdo.floatData();

    if (Firebase.RTDB.getFloat(&fbdo, "/config/tinggi_tangki"))
        g_tinggiTangki = fbdo.floatData();

    // Perintah manual dari aplikasi
    if (!g_modeOtomatis) {
        bool isiAir   = false;
        bool buangAir = false;

        if (Firebase.RTDB.getBool(&fbdo, "/kontrol/isi_air"))
            isiAir = fbdo.boolData();

        if (Firebase.RTDB.getBool(&fbdo, "/kontrol/buang_air"))
            buangAir = fbdo.boolData();

        if (isiAir) {
            setRelay(true);
            Serial.println(F("[Kontrol Manual] Relay ON – Isi Air"));
            // Reset flag di Firebase setelah dieksekusi
            Firebase.RTDB.setBool(&fbdo, "/kontrol/isi_air", false);
        }

        if (buangAir) {
            setRelay(false);
            Serial.println(F("[Kontrol Manual] Relay OFF – Buang Air"));
            Firebase.RTDB.setBool(&fbdo, "/kontrol/buang_air", false);
        }
    }
}

// ============================================================
//  FUNGSI: Baca semua sensor
// ============================================================
void bacaSensor() {
    g_pH    = bacaPH();
    g_jarak = bacaJarak();

    // Hitung tinggi air (jarak sensor ke permukaan air)
    // Asumsi: sensor dipasang di atas tangki → makin kecil jarak = makin penuh
    g_tinggiAir = g_tinggiTangki - g_jarak;
    if (g_tinggiAir < 0) g_tinggiAir = 0;

    // Hitung persentase
    float range = g_tinggiTangki - BATAS_PENUH_CM;
    g_persenAir = (range > 0) ? ((g_tinggiAir / range) * 100.0f) : 0.0f;
    if (g_persenAir > 100.0f) g_persenAir = 100.0f;
    if (g_persenAir < 0.0f)   g_persenAir = 0.0f;
}

// ============================================================
//  FUNGSI: Baca nilai pH dari ADC
// ============================================================
float bacaPH() {
    // Rata-rata 20 sampel untuk mengurangi noise
    long jumlah = 0;
    for (uint8_t i = 0; i < 20; i++) {
        jumlah += analogRead(PH_PIN);
        delay(5);
    }
    float rata = jumlah / 20.0f;

    // Konversi ADC → tegangan (0–3.3 V untuk ESP32)
    float tegangan = (rata / PH_ADC_RES) * PH_ADC_VREF;

    // Konversi tegangan → pH
    float ph = 7.0f + ((PH_VOLT_AT_7 - tegangan) / PH_VOLT_PER_PH);

    // Clamp ke rentang wajar
    if (ph < 0.0f)  ph = 0.0f;
    if (ph > 14.0f) ph = 14.0f;

    return ph;
}

// ============================================================
//  FUNGSI: Baca jarak ultrasonik HC-SR04 (tanpa library)
// ============================================================
float bacaJarak() {
    // Kirim pulsa trigger 10 µs
    digitalWrite(TRIG_PIN, LOW);
    delayMicroseconds(2);
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);

    // Ukur durasi echo (timeout 30 ms → ~5 m maksimum)
    long durasi = pulseIn(ECHO_PIN, HIGH, 30000UL);
    if (durasi == 0) return g_jarak; // Kembalikan nilai terakhir jika timeout

    float jarak = (durasi * 0.034f) / 2.0f;
    return jarak;
}

// ============================================================
//  FUNGSI: Kontrol relay berdasarkan kondisi pH & ketinggian air
// ============================================================
void kontrolRelay() {
    if (!g_modeOtomatis) return; // Mode manual – relay dikontrol dari Firebase

    bool phOk     = (g_pH >= g_phMin && g_pH <= g_phMax);
    bool airKosong = (g_persenAir < 20.0f);  // Tangki hampir kosong (<20%)
    bool airPenuh  = (g_persenAir >= 95.0f); // Tangki hampir penuh (≥95%)

    if (phOk && airKosong) {
        // pH bagus & air kurang → nyalakan relay untuk isi
        setRelay(true);
        Serial.println(F("[Otomatis] pH OK & air rendah → Relay ON"));
    } else if (!phOk) {
        // pH di luar batas → matikan relay (jangan kasih air buruk)
        setRelay(false);
        Serial.printf("[Otomatis] pH %.2f di luar batas (%.1f–%.1f) → Relay OFF\n",
                      g_pH, g_phMin, g_phMax);
    } else if (airPenuh) {
        // Tangki penuh → matikan relay
        setRelay(false);
        Serial.println(F("[Otomatis] Tangki penuh → Relay OFF"));
    }
    // Jika pH OK dan level sedang (20–95%) → biarkan status relay tidak berubah
}

// ============================================================
//  FUNGSI: Nyalakan / matikan relay
// ============================================================
void setRelay(bool nyala) {
    g_relayOn = nyala;
    // Relay Active-LOW: LOW = ON, HIGH = OFF
    digitalWrite(RELAY_PIN, nyala ? LOW : HIGH);
}

// ============================================================
//  FUNGSI: Kirim data monitoring ke Firebase
// ============================================================
void kirimKeFirebase() {
    if (!Firebase.ready()) {
        Serial.println(F("[Firebase] Tidak siap, skip kirim data"));
        return;
    }

    FirebaseJson json;
    json.set("ph",          g_pH);
    json.set("jarak",       g_jarak);
    json.set("tinggi_air",  g_tinggiAir);
    json.set("persen_air",  g_persenAir);
    json.set("relay",       g_relayOn);
    json.set("kondisi_ph",  kondisiPH(g_pH));

    if (Firebase.RTDB.updateNode(&fbdo, "/monitoring", &json)) {
        Serial.println(F("[Firebase] Data monitoring terkirim ✓"));
    } else {
        Serial.printf("[Firebase] Gagal: %s\n", fbdo.errorReason().c_str());
    }

    // Sync status relay ke Firebase
    Firebase.RTDB.setBool(&fbdo, "/monitoring/relay", g_relayOn);
}

// ============================================================
//  FUNGSI: Update tampilan LCD
//  Baris 0: pH + kondisi
//  Baris 1: Tinggi air + persentase
// ============================================================
void updateLCD() {
    char buf0[17];
    char buf1[17];

    // Baris 0: "pH:7.23 BAIK    "
    String kondisi = kondisiPH(g_pH);
    snprintf(buf0, sizeof(buf0), "pH:%-5.2f %-6s", g_pH, kondisi.c_str());

    // Baris 1: "Air:15.2cm  78% "
    snprintf(buf1, sizeof(buf1), "Air:%-4.1fcm %3.0f%%", g_tinggiAir, g_persenAir);

    lcd.setCursor(0, 0);
    lcd.print(buf0);
    lcd.setCursor(0, 1);
    lcd.print(buf1);
}

// ============================================================
//  FUNGSI: Kembalikan label kondisi pH
// ============================================================
String kondisiPH(float ph) {
    if (ph < g_phMin)  return "RENDAH";
    if (ph > g_phMax)  return "TINGGI";
    return "BAIK";
}
