# Pending software menuju production

R3.9.0 menambahkan provisioning unik untuk factory build, penggantian password
admin awal, journal energi dua file, dan uji regresi host/browser. Build
kompatibilitas biasa masih mempunyai akses bootstrap bersama pada unit kosong.
Lihat RELEASE-R3.9.0.md untuk bukti terbaru; jangan menyamakan hasil host dengan
uji perangkat fisik.

Prioritas sebelum penjualan:

1. EEPROM konfigurasi masih satu sektor flash: CRC mendeteksi kerusakan tetapi
   belum menyediakan backup power-fail independen. Perlu rancangan partition
   dan uji putus daya saat commit; tidak dipaksakan dengan mengambil sektor
   kosong yang belum diverifikasi.
2. Karakterisasi noise/current saat boot dan uji coil aktual. Tiga sampel
   current valid tidak membuktikan kontak; jangan klaim proteksi listrik.
3. ServerSmartPlug masih R3.8.0: batas 1.000.000 Wh dan float energi, write SD,
   kredensial broker/ACL serta korelasi command ID perlu dimatangkan terpisah.
4. Test HTTP pada ESP asli untuk login, expiry, CSRF, lockout dan mutasi gagal;
   browser mock membuktikan alur UI, bukan implementasi server ESP.
5. Pemulihan konfigurasi bukan secure erase/pindah pemilik. Proses penghapusan
   data server dan perangkat perlu ditetapkan.

| Item | Status | Prasyarat / alasan |
|---|---|---|
| TLS/HTTPS lokal | Pending resource dan threat model | ESP-07/flash 512 KB tidak memiliki budget yang telah divalidasi untuk TLS dashboard. HTTP lokal harus tetap dinyatakan secara jujur. |
| Secure OTA + rollback | Pending hardware flash/boot design | Flash 512 KB tidak memuat image aplikasi aktif dan staging image OTA dengan aman. |
| Uji ketahanan persistensi energi | Pending hardware test | LittleFS menyimpan record energyWh ber-CRC setiap lima menit saat diaktifkan; masih perlu uji wear dan power-loss. |
| Uji ketahanan histori server | Pending hardware test | ServerSmartPlug sudah menyimpan agregat satu menit pada SD card; masih perlu uji kapasitas SD, power-loss, dan retensi jangka panjang. |
| RTC/NTP dan histori bertimestamp offline | Pending product time policy | Server mencoba sinkronisasi NTP saat terhubung jaringan; perlu sumber waktu dan recovery yang tervalidasi saat jaringan offline. |
| Audit log persisten | Pending wear-budget | Yang ada hanya ring-buffer RAM agar tidak menulis flash pada tiap event. |
| Multi-user/role access | Pending threat model | Implementasi sekarang satu session admin lokal; belum ada role operator/viewer. |
| Kunci provisioning pabrik | Tool dan factory build tersedia; QC label fisik pending | Password acak terpisah per unit, QR Wi-Fi, kartu admin; header harus diarsipkan aman dan cocok dengan STA MAC. |
| Test otomatis security/API | Host/browser tersedia; API target pending | 38 native test, uji relay dari class aktual, dan alur browser mock tersedia. Login lockout/expiry/CSRF target ESP tetap perlu diuji. |
| Feedback kontak relay | Pending hardware | Raw arus dapat menjadi sinyal beban, tetapi tidak dapat membuktikan posisi relay: relay ON dapat memiliki beban 0 A, dan arus dapat berasal dari wiring/fault lain. |
| Kontrol auto-cut | Pending safety hardware | Tidak dibuka sampai feedback, proteksi, dan test fixture relay disetujui. |
| MicroSD CSV | Pending hardware | Board ESP-07 saat ini tidak memiliki socket MicroSD/GPIO bersih. |
| Harmonik / ketidakseimbangan fase | Tidak didukung | BL0940 dan rangkaian ini bukan analyzer kualitas daya atau multi-fase. |
