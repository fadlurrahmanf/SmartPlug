# Kontrak REST API SmartPlug — v1

Image `esp07_product` menjalankan REST API HTTP lokal untuk aplikasi. Access
Point tidak menyajikan halaman dashboard maupun form commissioning; aplikasi
menggunakan endpoint JSON untuk provisioning, QC, monitoring, dan kontrol.

## Provisioning awal

Pada boot pertama (atau factory reset), perangkat kembali ke access point setup
untuk dipilih oleh aplikasi Android.

| Parameter | Perilaku |
|---|---|
| SSID | `SmartPlug-Setup` |
| Password AP | `SmartPlug123` |
| Akun admin | Username `admin`; password awal `SmartPlug123`. |
| IP AP | `192.168.4.1`, HTTP port 80. |

Password admin tidak disimpan plaintext: EEPROM menyimpan digest HMAC-SHA-256
dengan salt per perangkat.

Factory reset menghapus Wi-Fi existing, session, mode integrasi, pengaturan
broker, dan kredensial yang diubah pengguna, lalu memulihkan konfigurasi awal.

## Identitas perangkat

Firmware membentuk `device_id` otomatis dari **STA MAC Wi-Fi**, bukan AP MAC
dan bukan daftar nomor yang harus diinput operator. Formatnya:

```text
device_id = SP- + STA_MAC tanpa tanda titik dua
contoh    = SP-84F3EB123456
```

`device_id` tersedia pada `/api/v1/status` dan dipakai konsisten oleh REST,
MQTT, server, serta histori aplikasi.

## Kebijakan akses

Telemetry berikut sengaja **read-only dan public dalam LAN/AP** agar aplikasi
lokal dapat membaca pengukuran tanpa login:

| Endpoint | Isi |
|---|---|
| `GET /api/v1` | Discovery API. |
| `GET /api/v1/capabilities` | Kemampuan firmware dan batasnya. |
| `GET /api/v1/status` | Status Wi-Fi, relay commanded state, dan firmware. |
| `GET /api/v1/measurements/allparameters` | Seluruh parameter sampel terbaru. |
| `GET /api/v1/measurements/voltage` | Tegangan terbaru dalam V. |
| `GET /api/v1/measurements/current` | Arus terbaru dalam A. |
| `GET /api/v1/measurements/active-power` | Daya aktif terbaru dalam W. |
| `GET /api/v1/measurements/apparent-power` | Daya semu terbaru dalam VA. |
| `GET /api/v1/measurements/power-factor` | Power factor terbaru dalam PF. |
| `GET /api/v1/measurements/energy` | Energi kumulatif terbaru dalam Wh. |
| `GET /api/v1/health` | Kesehatan komunikasi meter. |

Mutasi administrasi browser memerlukan session dan CSRF token. Aplikasi Android
boleh memakai owner bearer token hasil pairing untuk relay, timer, reset energi,
dan factory reset tiga-tahap; tidak ada lagi `GET /relay/on` atau `GET /relay/off`.

## Login, session, dan API mutasi

1. `POST /api/v1/auth/login` dengan form URL-encoded `username` dan `password`.
2. Jika berhasil, respons memberi `csrf_token` dan browser menerima cookie
   `sp_session` dengan atribut `HttpOnly`, `SameSite=Strict`, dan masa hidup
   15 menit.
3. Setiap `POST` mutasi mengirim header `X-CSRF-Token: <token>` serta cookie.
4. `POST /api/v1/auth/logout` menghapus session.

| Endpoint | Input | Hasil |
|---|---|---|
| `POST /api/v1/relay` | `state=on` atau `state=off` | Antrekan pulse relay; minimum satu detik antar-perintah. |
| `POST /api/v1/settings/wifi` | `ssid`, `password` | Simpan Wi-Fi existing dan mulai koneksi STA. |
| `GET /api/v1/settings/mqtt` | session | Baca mode integrasi dan metadata broker tanpa password broker. |
| `POST /api/v1/settings/mqtt` | `mode=rest` atau `mode=mqtt`, serta parameter broker pada mode MQTT | Pilih jalur integrasi dan simpan pengaturan broker. |
| `POST /api/v1/settings/access` | `ap_ssid`, `ap_password`, opsional `admin_password` | Ganti akses AP/admin dan reboot. Password admin baru minimal 12 karakter. |
| `POST /api/v1/settings/wifi/reset` | — | Factory reset lalu reboot. |
| `POST /api/v1/energy/reset` | `confirm_1`, `confirm_2`, `confirm_3` semuanya `RESET_ENERGY` | Reset counter energi perangkat setelah konfirmasi tiga tahap. |
| `GET /api/v1/timer` | owner token atau session | Status timer termasuk `active`, `running`, dan `remaining_ms`. |
| `POST /api/v1/timer` | `days`, `hours`, `minutes`, `seconds`, atau `action=reset` | Terapkan timer; bila relay OFF, timer baru mulai saat relay ON. Mode langsung tidak bertahan setelah reboot. |
| `POST /api/v1/factory-reset` | owner token + tiga `FACTORY_RESET` | Hapus konfigurasi dan reboot; khusus aplikasi Android. |
| `GET /api/v1/audit` | session | Audit ring-buffer: login, perubahan setting, relay, dan reset. |

Login salah dibatasi: lima kegagalan berurutan menghasilkan lockout 60 detik.
Mutasi API dibatasi, dan input SSID/password/state divalidasi sebelum disimpan
atau diteruskan ke relay.

## Storage dan pengukuran

Konfigurasi Wi-Fi, AP, hash admin, mode integrasi, dan pengaturan MQTT disimpan
dalam record EEPROM terversi dengan CRC32. Jika record korup atau format tidak
cocok, perangkat kembali ke provisioning awal aman.

BL0940 dipoll setiap 500 ms. Tegangan, arus, dan daya aktif memakai smoothing
hingga 10 sampel valid. Nilai electrical yang belum memiliki
koefisien valid dilaporkan sebagai `0`; field `calibration` membedakannya dari
pembacaan nol fisik. Persistensi `energy_wh` selalu aktif: counter dipulihkan
dari LittleFS saat boot dan ditulis ulang setiap 15 menit pada mode REST
langsung atau lima menit pada mode MQTT, sehingga nilainya kumulatif
antar-restart. Factory reset menghapus checkpoint energi ini.

## Format measurement REST

Mulai R3.8.1, energi bertambah hanya dari selisih counter energi BL0940; nilainya
dapat tetap di antara increment, terutama pada beban kecil. Tidak ada estimasi
`daya * waktu` yang ditambahkan. Reset/lonjakan counter dan jeda data lebih dari
lima detik memulai baseline baru tanpa menaikkan total dari interval yang tidak
pasti. Ketentuan lengkap terdapat pada `ENERGY-R3.8.1.md`.

`GET /api/v1/measurements/allparameters` mengembalikan JSON seperti berikut:

```json
{
  "captured_at_ms": 125000,
  "has_sample": true,
  "calibration": "calibrated",
  "electrical": {
    "voltage_v": 220.1,
    "current_a": 0.42,
    "active_power_w": 86.4,
    "apparent_power_va": 92.5,
    "power_factor": 0.934,
    "energy_wh": 1234.5
  }
}
```

Endpoint parameter tunggal memakai format yang konsisten, misalnya
`GET /api/v1/measurements/voltage`:

```json
{"captured_at_ms":125000,"has_sample":true,"parameter":"voltage","value":220.1,"unit":"V","calibration":"calibrated"}
```

## Batas keamanan yang tetap berlaku

Transport masih HTTP lokal tanpa TLS. Session/CSRF mengurangi penyalahgunaan dari
halaman atau klien lain di jaringan, tetapi tidak mengenkripsi password atau
telemetry terhadap pihak yang telah memiliki akses ke Wi-Fi. Relay API hanya
menyatakan command queued. Setelah boot, tiga sampel arus valid digunakan untuk
menentukan state awal: arus terdeteksi menetapkan `on`; tiga pembacaan tanpa
arus memicu pulse `off`, kemudian status menjadi `off` setelah pulse selesai.

## Status HTTP dan error

Semua error berbentuk JSON `{"error":"<error_code>"}`.

| HTTP | Contoh error | Tindakan aplikasi |
|---|---|---|
| 400 | `invalid_relay_state`, `invalid_mqtt_settings` | Perbaiki input; jangan retry otomatis. |
| 401 | `authentication_required`, `authentication_failed` | Login kembali. |
| 403 | `csrf_invalid`, `relay_actuation_disabled` | Perbarui session/CSRF atau nonaktifkan kontrol relay. |
| 404 | `not_found`, `measurement_not_found` | Gunakan endpoint yang diumumkan discovery. |
| 429 | `rate_limited`, `login_temporarily_locked` | Tunggu sebelum mencoba lagi. |
| 500 | `settings_write_failed`, `calibration_write_failed` | Simpan error dan minta pengguna mengulang. |
