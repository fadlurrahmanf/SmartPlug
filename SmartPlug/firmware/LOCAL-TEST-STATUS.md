# Status verifikasi firmware SmartPlug R0.2.0

Catatan ini bersifat historis untuk R0.2.0. Hasil pengujian energi dan build
R3.8.1 tersedia pada [ENERGY-TEST-STATUS-R3.8.1.md](ENERGY-TEST-STATUS-R3.8.1.md).

Tanggal verifikasi: 2026-08-28  
Target: ESP-07 / ESP8266, PlatformIO `espressif8266@4.2.1`

## Bukti yang sudah ada

| Pemeriksaan | Hasil | Batas bukti |
|---|---|---|
| Kompilasi profil aman `esp07_safe` | Lulus | Relay dan radio tetap nonaktif pada profil ini. RAM 29.076 B (35,5%), flash 277.303 B (36,4%). |
| Kompilasi profil SmartPlug lokal `esp07_local` | Lulus | Image memuat SoftAP, REST read-only, dan status anomali tegangan report-only. RAM 29.696 B (36,2%), flash 314.447 B (41,3%). |
| Audit statis `cppcheck` pada `main.cpp`, `SmartPlugApi.cpp`, `SmartPlugMetering.h`, `Bl0940Protocol.h` | Lulus | Tidak menemukan defect pada pemeriksaan statis yang dijalankan; ini bukan uji eksekusi. |
| Cross-compile test protokol dan metering | Lulus | `pio test -e esp07_safe --without-uploading --without-testing` berhasil membangun runner dan 11 test cases dengan toolchain ESP8266. Tidak menjalankan test karena tidak ada perangkat/port uji. |
| Eksekusi unit test protokol dan metering | Belum dieksekusi | Runner tersedia di `test/test_protocol`; workstation belum memiliki executable host GCC/G++ dan belum ada ESP-07 test fixture. |

## Yang belum boleh diklaim

- Belum ada flash ke ESP-07, eksekusi 11 unit test pada perangkat, uji SoftAP/REST pada perangkat nyata, atau uji BL0940 dengan beban referensi.
- Nilai Volt, Ampere, Watt, PF, dan kWh sengaja berstatus **belum tersedia** sampai koefisien kalibrasi diisi dan divalidasi.
- Belum ada uji umur panjang, reboot/power-loss, keamanan Wi-Fi, maupun HIL (hardware-in-the-loop).
- Relay fisik dan auto-cut tetap tidak tersedia karena flag `SMARTPLUG_ALLOW_RELAY_ACTUATION=0`.

Jadi, source dan image telah **berhasil dibangun** dan pemeriksaan statis telah dilakukan. Pernyataan “tanpa bug” atau “siap produksi” baru sah setelah daftar uji perangkat nyata dan kalibrasi di atas selesai.
