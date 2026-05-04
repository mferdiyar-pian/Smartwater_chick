# SmartWaterChick – ESP32 Firmware (PlatformIO)

## 📦 Struktur Folder
```
esp32_firmware/
├── platformio.ini       ← Konfigurasi PlatformIO
├── src/
│   └── main.cpp         ← Kode utama firmware
└── README.md            ← Panduan ini
```

## 🔌 Skema Wiring

| Komponen              | Pin ESP32       | Keterangan                 |
|-----------------------|-----------------|----------------------------|
| Sensor pH (analog)    | GPIO **34**     | ADC1_CH6 – input analog    |
| Relay pompa air       | GPIO **23**     | Active-LOW (IN relay)      |
| HC-SR04 TRIG          | GPIO **5**      | Output digital             |
| HC-SR04 ECHO          | GPIO **18**     | Input digital              |
| LCD I2C SDA           | GPIO **21**     | I2C Data                   |
| LCD I2C SCL           | GPIO **22**     | I2C Clock                  |
| VCC sensor/relay      | **3.3V / 5V**   | Sesuai modul               |
| GND                   | **GND**         | Common ground              |

> ⚠️ Sensor pH biasanya butuh 5V – gunakan level shifter jika modul outputnya 5V ke ADC ESP32 (maks 3.3V)!

## 🚀 Cara Upload ke ESP32 via PlatformIO

1. **Buka folder** `esp32_firmware/` di VS Code
   - File → Open Folder → pilih folder `esp32_firmware`
2. PlatformIO akan otomatis **mendeteksi** `platformio.ini`
3. Tunggu library **didownload otomatis**
4. Klik tombol **→ Upload** (✓ di toolbar bawah) atau tekan `Ctrl+Alt+U`
5. Buka **Serial Monitor** (`Ctrl+Alt+S`) untuk melihat log

## ⚙️ Kalibrasi pH

Edit bagian ini di `main.cpp` sesuai hasil kalibrasi sensor Anda:
```cpp
#define PH_VOLT_AT_7    2.5f    // Tegangan saat larutan pH 7
#define PH_VOLT_PER_PH  0.18f   // Ubah jika slope berbeda
```

Cara kalibrasi:
1. Celupkan probe ke larutan buffer **pH 7** → catat tegangan output
2. Celupkan ke larutan buffer **pH 4** → catat tegangan output  
3. `slope = (V_pH4 - V_pH7) / (4 - 7)` → masukkan ke `PH_VOLT_PER_PH`

## 🔥 Firebase Database – Struktur Node

```
/monitoring
  /ph           → float   (nilai pH saat ini)
  /jarak        → float   (jarak ultrasonik, cm)
  /tinggi_air   → float   (tinggi air di tangki, cm)
  /persen_air   → float   (persentase isi tangki 0–100)
  /relay        → bool    (status relay pompa)
  /kondisi_ph   → String  ("BAIK" / "RENDAH" / "TINGGI")

/kontrol
  /otomatis     → bool    (true = mode otomatis, false = manual)
  /isi_air      → bool    (perintah isi air – reset otomatis)
  /buang_air    → bool    (perintah matikan relay)

/config
  /ph_min         → float (default: 6.5)
  /ph_max         → float (default: 8.0)
  /tinggi_tangki  → float (default: 30.0 cm)
```

## 📱 Sinkronisasi dengan Android App

| Fitur Android         | Firebase Node             |
|-----------------------|---------------------------|
| Bar level tangki      | `/monitoring/persen_air`  |
| Tombol Isi Air        | `/kontrol/isi_air = true` |
| Tombol Buang Air      | `/kontrol/buang_air=true` |
| Switch Otomatis       | `/kontrol/otomatis`       |
| Tampil nilai pH       | `/monitoring/ph`          |
| Status relay/pompa    | `/monitoring/relay`       |
