# Gate validasi HIL firmware SmartPlug R0.2.0

Dokumen ini adalah prosedur verifikasi sebelum image SmartPlug dinyatakan teruji
pada hardware. Dokumen ini **bukan** izin untuk menyalakan mains atau
mengaktifkan relay.

## Safety gate sebelum flash atau uji

- Topologi board saat ini mengikat GND elektronik ke domain netral/mains.
- Pemrograman hanya saat mains lepas, atau menggunakan programmer terisolasi
  yang sudah diverifikasi dengan prosedur keselamatan yang disetujui.
- Relay fisik wajib tetap terkunci: verifikasi build flag
  `SMARTPLUG_ALLOW_RELAY_ACTUATION=0`.
- Jangan memasang beban atau melakukan uji 220 VAC sebelum review wiring,
  proteksi, enclosure, fixture, dan PIC keselamatan selesai.

Jika salah satu butir tidak terpenuhi, hentikan uji dan catat sebagai blocked.

## Bukti firmware yang harus direkam

| Gate | Cara verifikasi | Kriteria lulus |
|---|---|---|
| Identitas image | Catat SHA-256 BIN dan log build | Hash sama dengan manifest R0.2.0. |
| Boot | Flash dalam kondisi aman, lalu simpan log serial | Banner menampilkan `SmartPlug local API compiled in: true` dan `Relay actuation compiled in: false`. |
| SoftAP | Hubungkan Android/laptop ke SSID setup | SSID terlihat dan alamat device `192.168.4.1` dapat diakses. |
| REST root | `GET /` | HTTP 200, JSON valid, daftar endpoint benar. |
| Capability | `GET /api/v1/capabilities` | `voltage_anomaly_reporting:true`, `local_relay_control:false`, `micro_sd_csv:false`, `cloud_required:false`. |
| Measurement sebelum kalibrasi | `GET /api/v1/measurements/latest` | `calibration_state:not_calibrated`, `electrical:null`; raw code tidak ditampilkan sebagai satuan listrik. |
| Health | `GET /api/v1/health` | `standby_detection.mode:detect_only`, `voltage_anomaly.mode:report_only`, dan `relay_cut_enabled:false`. |
| Endpoint tidak dikenal | `GET /api/v1/relay` | HTTP 404 dan tidak ada pulsa relay. |
| Durasi respons | Poll seluruh endpoint 100 kali melalui LAN AP | P95 kurang dari atau sama dengan 3 detik; simpan hasil mentah. |

## Gate kalibrasi metering yang masih terpisah

Koefisien pada `include/SmartPlugConfig.h` tetap nol sampai proses ini memiliki
alat acuan, serial number, metode, dan hasil tercatat:

1. Verifikasi tegangan dan frekuensi sumber dalam kondisi uji yang disetujui.
2. Uji sedikitnya titik beban rendah, menengah, dan tinggi dengan alat acuan
   yang teridentifikasi.
3. Catat raw BL0940, nilai acuan, koefisien, error V/A/W/PF/Wh, serta tanggal.
4. Masukkan koefisien hanya bila hasil review menyetujui target akurasi.
5. Ulangi API dan endurance test setelah kalibrasi.

Tanpa bukti ini, unit listrik dan klaim akurasi tetap tidak tersedia.

## Kriteria hold/reject

- API mengeluarkan nilai Volt/Ampere/Watt saat status `not_calibrated`.
- Endpoint apa pun dapat mengubah state relay atau relay memberi pulsa.
- SoftAP/API reboot, hang, atau respons JSON tidak valid dalam pengujian.
- Data metering stale diintegrasikan sebagai energi melewati gap 5 detik.
- Ada indikasi masalah keselamatan hardware, pemrograman, atau isolasi.
