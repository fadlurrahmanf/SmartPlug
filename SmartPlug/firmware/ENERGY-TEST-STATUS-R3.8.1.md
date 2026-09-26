# Verifikasi perubahan energi R3.8.1

## Hasil

| Pemeriksaan | Hasil |
|---|---|
| Eksekusi native C++ protokol, metering, smoothing, dan anomali | 25/25 lulus |
| Build image runtime REST/MQTT `esp07_product` | Lulus |
| Build profil REST engineering `esp07_rest` | Lulus |
| Upload atau pengujian unit fisik dengan alat acuan | Tidak dilakukan |

Regresi dijalankan pada header C++ produksi, bukan penulisan ulang algoritma.
Sebelum perbaikan, 10 dari 23 test gagal pada algoritma lama. Sesudah perbaikan
dan dua tambahan edge case, seluruh 25 test lulus. Konfigurasi host diperbaiki
agar tidak membangun driver UART Arduino; harness Unity memiliki setUp/tearDown.
Compiler MinGW yang sudah tersedia dipakai tanpa instalasi compiler baru.

Cakupan energi: CF sebagai sumber tunggal; polling dengan interval berbeda;
counter tetap; rollover CF 24-bit; reset counter; lonjakan maju tidak masuk akal;
rollover millis; sampel bertimestamp sama; gap data; pemulihan total; perubahan
koefisien; kalibrasi invalid; restore negatif/NaN/infinity; akumulasi kecil pada
total besar; counter tetap tidak ikut smoothing.

## Build

| Target | RAM terpakai / batas | Flash program terpakai / batas | Ukuran berkas BIN |
|---|---|---|---|
| `esp07_product` | 35.708 / 81.920 byte | 394.403 / 434.160 byte | 398.560 byte |
| `esp07_rest` | 34.536 / 81.920 byte | 381.343 / 434.160 byte | 385.488 byte |

Layout flash tetap 512 KB. Batas image program dan ukuran berkas BIN berbeda
karena pengemasan bootloader/image. ServerSmartPlug tidak diubah.

SHA-256 `esp07_product/firmware.bin`:

`999D6BEB6056BFA11AA3906BC5F6FA80485E799B96D90FCA4300E71FEB38A1CB`

SHA-256 `esp07_rest/firmware.bin`:

`27624EAE66523E87ABAC773EAC53A04401925DCD7779546F879BF35CFCCE7E04`

Build masih menampilkan warning yang tidak terkait perubahan energi:
fungsi `mqttReached` tidak digunakan dan, saat dependensi dibangun ulang,
perbandingan signed/unsigned dalam PubSubClient.

## Perintah reproduksi di workstation ini

```powershell
$env:PATH = 'C:\Users\MSI\.platformio\packages\toolchain-gccmingw32\bin;' + $env:PATH
& 'C:\Users\MSI\.platformio\penv\Scripts\pio.exe' test -d D:\IoT\SmartPlug\firmware -e native_protocol_tests
& 'C:\Users\MSI\.platformio\penv\Scripts\pio.exe' run -d D:\IoT\SmartPlug\firmware -e esp07_product -e esp07_rest
```

## Batas hasil

Lulus test/build tidak mengukur akurasi board. Pengujian dengan meter referensi
masih diperlukan. R3.8.1 tidak memperbaiki format penyimpanan lama, batas
persistensi/sinkronisasi 1.000 kWh, ketahanan power-loss, workflow kalibrasi QC,
provisioning QR atau state-machine relay. Nilai tersimpan yang mungkin sudah
berlebih dari algoritma lama dipertahankan; tidak ada reset energi otomatis.

Kebijakan counter dan batas identifikasi reset dijelaskan pada
`ENERGY-R3.8.1.md`.
