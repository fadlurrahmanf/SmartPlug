# Laporan build dan test

Tanggal: 2026-09-24. Lingkungan build: JDK 17 (Microsoft build), Android SDK cmdline-tools
(platform-tools, `platforms;android-34`, `build-tools;34.0.0`) diinstal lokal ke
`android/.android-sdk/`, Gradle 8.7 via wrapper.

## Build

```
./gradlew assembleDebug
```

**Hasil: BUILD SUCCESSFUL.** Output: `app/build/outputs/apk/debug/app-debug.apk` (~63,8 MB,
application id `com.smartplug.app.debug`, `versionName 0.1.0`).

Tidak ada build release yang dihasilkan — tidak ada keystore rilis yang tersedia untuk pekerjaan
ini (lihat `README.md`).

## Unit test

```
./gradlew testDebugUnitTest
```

**Hasil: 20/20 test lulus, 0 gagal.**

| Kelas test | Jumlah test | Cakupan |
|---|---:|---|
| `ApiResultParsingTest` | 5 | Parsing sukses + dua bentuk error body (`design.md` bersarang, firmware datar), body kosong, body rusak |
| `PairingRepositoryImplTest` | 4 | `fetchPairInfo`, `pollStatus` connecting→connected, →failed dengan `reason`, berhenti pada kegagalan transport |
| `RelayRepositoryImplTest` | 4 | Parsing command relay queued, penyelesaian state DIRECT (polling sampai cocok), timeout, mapping `relay_state` |
| `PollingTest` | 2 | Poller single-flight sekuensial, reset backoff setelah sukses |
| `RetryPolicyTest` | 5 | Jadwal backoff pengukuran (2/4/8/15/30 detik) dan reconnect (5/10/20/40/60 detik), cadence polling |

Semua test berjalan sebagai unit test JVM biasa (bukan instrumented test) memakai
`MockWebServer` untuk mensimulasikan endpoint SmartPlug/ServerSmartPlug — **tidak ada perangkat
fisik atau backend nyata yang terlibat**, karena firmware saat ini belum mengimplementasikan
endpoint pairing yang didesain (lihat `README.md`).

## Yang belum diverifikasi (jujur, bukan diklaim berhasil)

- Pairing nyata ke SmartPlug fisik melalui `WifiNetworkSpecifier` — perlu radio Wi-Fi asli,
  tidak bisa diuji lewat unit test JVM, dan firmware pairing API belum ada untuk diajak bicara.
- Discovery NSD/mDNS (`_smartplug._tcp`, `_srvrplug._tcp`) — perlu jaringan LAN nyata dengan
  perangkat yang mengumumkan service tersebut.
- Tampilan UI di perangkat/emulator sungguhan (rendering Compose, animasi, adaptasi tablet vs
  ponsel, light/dark) — belum dijalankan di emulator/perangkat karena sesi ini tidak memiliki
  AVD/emulator terpasang.
- Instrumented test (`androidTest`) — dependency sudah ditambahkan di `build.gradle.kts`
  (`androidx.test`, Espresso, Compose UI test) tetapi belum ada test instrumented yang ditulis,
  dan tidak ada emulator/perangkat di lingkungan ini untuk menjalankannya.

## Peringatan compiler yang tersisa (tidak menghalangi build)

- Beberapa API `NsdManager`/`WifiManager` (`.SSID`, `resolveService(..)`, `.host`) sudah deprecated
  di API level terbaru tetapi masih didukung penuh hingga API 34/35 dan tidak punya pengganti yang
  tersedia di `minSdk 29`.
- Beberapa ikon `Icons.Filled.ArrowBack` disarankan pindah ke varian `AutoMirrored` — kosmetik,
  tidak fungsional.
