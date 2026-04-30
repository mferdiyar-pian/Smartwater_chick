#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <LiquidCrystal_I2C.h>

// --- KONEKSI ---
#define WIFI_SSID "Awikwok"
#define WIFI_PASSWORD "12334566"
#define API_KEY "AIzaSyCQ53_8gQuDtfQz80hWbmGYpLXak4E-N4U"
#define DATABASE_URL "https://smartwater-chick-default-rtdb.firebaseio.com"

// --- PIN ---
#define RELAY_PIN 23
#define TRIG_PIN 5
#define ECHO_PIN 18

LiquidCrystal_I2C lcd(0x27, 16, 2);
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  digitalWrite(RELAY_PIN, HIGH); // Mati (Relai Active Low)

  lcd.init();
  lcd.backlight();
  lcd.print("Connecting WiFi");

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  Firebase.begin(&config, &auth);
  
  lcd.clear();
  lcd.print("SISTEM SIAP");
}

void loop() {
  // BACA JARAK MANUAL (TANPA LIBRARY)
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long durasi = pulseIn(ECHO_PIN, HIGH);
  float jarak = durasi * 0.034 / 2;

  if (Firebase.ready()) {
    Firebase.RTDB.setFloat(&fbdo, "/monitoring/jarak", jarak);
    
    bool otomatis = true;
    if (Firebase.RTDB.getBool(&fbdo, "/kontrol/otomatis")) otomatis = fbdo.boolData();

    if (otomatis) {
      if (jarak > 20) digitalWrite(RELAY_PIN, LOW); // Nyala
      else digitalWrite(RELAY_PIN, HIGH); // Mati
    } else {
      bool isi = false;
      if (Firebase.RTDB.getBool(&fbdo, "/kontrol/isi_air")) isi = fbdo.boolData();
      digitalWrite(RELAY_PIN, isi ? LOW : HIGH);
    }
  }

  lcd.setCursor(0, 0);
  lcd.print("Jarak: "); lcd.print(jarak); lcd.print("cm  ");
  delay(1000);
}
