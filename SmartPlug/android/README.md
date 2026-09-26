# SmartPlug Android App

Aplikasi Android native (Kotlin + Jetpack Compose) untuk onboarding, monitoring, kontrol relay,
dan riwayat energi perangkat SmartPlug, dibangun mengikuti kontrak di
[`docs/design.md`](../docs/design.md).

## Status penting — baca sebelum menganggap ini "selesai"

**Belum ada pemasangan atau pengujian terhadap perangkat SmartPlug fisik.** Ini bukan kelalaian —
per `design.md` bagian "Status implementasi firmware" (dan dikonfirmasi di
[`firmware/LOCAL-API.md`](../firmware/LOCAL-API.md)), firmware R3.9.x yang ada di repo ini **belum
mengimplementasikan** endpoint pairing (`/api/v1/pair/*`), Wi-Fi scan dari perangkat, hasil
`lan_ip` pairing, mDNS/NSD, atau autentikasi `owner_token`. Firmware saat ini memakai autentikasi
session-cookie + CSRF, bukan bearer token seperti yang didesain di `design.md`.

Karena itu:

- Aplikasi ini diimplementasikan **persis mengikuti kontrak target di `design.md`**, sesuai
  instruksi ("jangan mengarang endpoint... design.md yang berlaku").
- **Tidak ada backend nyata untuk diuji pairing end-to-end saat ini.** Unit test memakai
  `MockWebServer` untuk mensimulasikan endpoint pairing/REST/relay yang didesain, memverifikasi
  logika parsing, state machine, retry/backoff, dan penyelesaian state relay — bukan bukti bahwa
  perangkat fisik akan berperilaku identik.
- Alur `WifiNetworkSpecifier` (koneksi ke AP `SP-<unit_id>`) dan resolusi NSD/mDNS memerlukan
  radio Wi-Fi nyata dan tidak dapat diuji lewat unit test JVM; keduanya perlu instrumented test
  atau pengujian manual pada perangkat Android + SmartPlug fisik begitu firmware pairing tersedia.
- Jangan menganggap "APK berhasil dibangun" sebagai bukti pairing, monitoring, atau kontrol relay
  berfungsi pada perangkat sungguhan.

## Build

Prasyarat: JDK 17. Android SDK (platform 34, build-tools 34.0.0) sudah disediakan di
`android/.android-sdk/` untuk build ini (lihat `local.properties`); Android Studio dapat memakai
SDK-nya sendiri jika sudah terpasang.

```powershell
cd android
./gradlew assembleDebug
```

APK debug: `android/app/build/outputs/apk/debug/app-debug.apk` (application id
`com.smartplug.app.debug`).

Tidak ada keystore rilis yang disediakan untuk pekerjaan ini, sehingga hanya build **debug** yang
dihasilkan. `buildTypes.release` di `app/build.gradle.kts` sudah menyalakan minify/shrink tetapi
sengaja dibiarkan tanpa `signingConfig` — tambahkan `signingConfigs.release` begitu keystore rilis
tersedia untuk menghasilkan APK/AAB rilis yang ditandatangani.

### Menjalankan unit test

```powershell
./gradlew testDebugUnitTest
```

Cakupan: parsing DTO/error envelope (kedua bentuk — target `design.md` dan bentuk flat yang sudah
dipakai firmware), state machine pairing (`pollStatus` berhenti tepat di `connected`/`failed`),
jadwal retry/backoff pengukuran (2/4/8/15/30 detik) dan reconnect MQTT (5/10/20/40/60 detik), serta
penyelesaian state relay (queued → completed/timeout) untuk mode REST langsung.

## Menghubungkan aplikasi ke SmartPlug

Sesuai desain, alur di aplikasi adalah: **Tambah SmartPlug** → pilih SmartPlug dari daftar AP
sekitar → pilih Wi-Fi rumah → (opsional) aktifkan ServerSmartPlug bila terdeteksi → **Hubungkan**.
Lihat [`docs/design.md`](../docs/design.md) bagian "Alur sistem" untuk urutan lengkap.

**Ini tidak akan berhasil terhadap firmware SmartPlug yang ada di repo `firmware/` saat ini**,
karena endpoint `/api/v1/pair/*` belum diimplementasikan di sana. Menjalankan alur ini memerlukan
firmware yang sudah menambahkan pairing API sesuai daftar kerja di `design.md`
("Status implementasi firmware", poin 1-6).

## Struktur proyek

```
app/src/main/java/com/smartplug/app/
├── data/
│   ├── remote/        Retrofit interfaces (Pairing/Device/Server), DTO, parsing error terpadu
│   ├── local/          DataStore (preferensi), EncryptedSharedPreferences (token/password), Room
│   └── repository/     Implementasi repository — satu-satunya tempat yang tahu REST vs MQTT-server
├── domain/
│   ├── model/          Model domain (tidak bergantung pada bentuk JSON wire)
│   └── repository/     Interface repository (dipakai ViewModel, di-mock di unit test)
├── di/                  Modul Hilt (network, database, binding repository)
├── ui/
│   ├── theme/           Palet warna + tipografi light/dark
│   ├── navigation/      NavHost + daftar destinasi
│   ├── components/      Kartu, status pill, grafik energi, polling-while-visible, dsb.
│   └── screens/         Satu paket per halaman (Beranda, Perangkat, Tambah SmartPlug, Detail,
│                        Riwayat, Pengaturan), masing-masing dengan ViewModel Hilt-nya sendiri
└── util/                Kebijakan retry/backoff, SoundManager, haptic
```

## Ringkasan endpoint yang dipakai aplikasi

Lihat [`API-SUMMARY.md`](API-SUMMARY.md).
