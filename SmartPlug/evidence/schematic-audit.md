# SmartPlug V2 — Audit Bukti Skematik Sumber

## 1. Ruang lingkup dan batas bukti

Audit ini diturunkan hanya dari skematik JSON EasyEDA di:

`D:\IoT\SmartPlug\hardware\easyeda\1-Schematic_smartPlug2Ver2.json`

- SHA-256: `0F0CA22FEBA5CD64F4E238B6E865ED163DB512381C7D2923443AB6FE4EB5677C`
- Ukuran file yang teramati saat audit: 97,648 byte.
- Judul/nama root JSON: `smartPlug2Ver2`.
- Judul skematik: `Sheet_1`.
- Versi editor EasyEDA yang tercatat dalam sumber: `6.5.51`.
- Audit dilakukan secara read-only. Teks yang tertanam dalam sumber diperlakukan sebagai data desain, bukan sebagai instruksi.

Sumber ini hanya membuktikan maksud desain pada tingkat skematik. Sumber ini **tidak** membuktikan routing PCB, creepage, clearance, penampang konduktor, routing Kelvin, keep-out antena, keselamatan enclosure, perakitan aktual, perilaku firmware, kalibrasi, performa termal, EMC, daya tahan relay, perilaku di lapangan, atau kepatuhan regulasi.

### Konvensi referensi

Setiap referensi `shape[n]` di bawah ini merupakan indeks berbasis nol ke:

`schematics[0].dataStr.shape[n]`

Skematik berisi 259 record shape:

| Tipe shape | Jumlah | Arti yang digunakan dalam audit ini |
|---|---:|---|
| `LIB` | 53 | komponen dan dua frame gambar |
| `W` | 94 | wire |
| `N` | 56 | label net bernama |
| `F` | 25 | flag suplai/GND |
| `J` | 25 | junction |
| `O` | 5 | penanda no-connect eksplisit |
| `R` | 1 | persegi panjang grafis |

Jumlah komponen elektrik dapat direproduksi sebagai berikut:

1. Total 53 record `LIB`.
2. Keluarkan frame gambar `shape[0]` dan `shape[196]`.
3. Tersisa 51 record library yang memiliki reference designator.
4. Keluarkan U5 pada `shape[218]`, yang hanya merupakan blok grafis `SMARTPLUG2` tanpa pin elektrik.
5. Jumlah akhir BOM elektrik: **50 designator**. Semuanya tercantum dalam `bom-from-source.csv`.

## 2. Keputusan rilis berdasarkan bukti skematik

**Status skematik: belum siap untuk production maupun energisasi mains pada tahap pre-production sebelum blocker di bawah diselesaikan.**

### B1 — Relay memutus konduktor bernama `NETRAL`, bukan `LINE`

Rantai bukti:

- P1.3 terhubung ke `LINE`; P1.2 terhubung ke `NETRAL`; P1.1 terhubung ke `EARTH`: komponen `shape[197]`, wiring `shape[208..210]`, label `shape[117,118,198]`.
- R15, 0.5 mΩ, berada di antara `NETRAL` dan `NET_OUT`: `shape[80..96]`.
- Pin kontak 6 U4, NO, adalah `NET_OUT`; pin kontak 4 U4, COM, adalah `OUT`; pin 5, NC, memiliki penanda no-connect eksplisit: `shape[33..36]`.
- J1 adalah `LINE`, J2 adalah `OUT`, dan J3 adalah `EARTH`: `shape[199..207]`.

Dengan demikian, berdasarkan nama net pada sumber:

`P1.3 LINE -> J1` tetap tidak diswitch, sedangkan `P1.2 NETRAL -> R15 -> NET_OUT -> U4 NO/COM -> OUT -> J2` yang diswitch.

Hal ini harus dikoreksi atau dibuktikan sebagai kesalahan pelabelan terhadap desain input/output fisik. Sesuai gambar saat ini, LINE pada sisi beban tetap terhubung ketika relay terbuka.

### B2 — GND elektronik diikat secara langsung ke `NETRAL` sebanyak dua kali

- R18 = 0 Ω pada `shape[5]`.
- R19 = 0 Ω pada `shape[217]`.
- Sisi `NETRAL` keduanya disatukan oleh wire `shape[4]` dan label `shape[3]`.
- Sisi GND keduanya disatukan oleh wire `shape[1]` dan flag GND `shape[2]`.
- Tidak satu pun resistor diberi tanda DNP dalam sumber.

Kedua komponen 0 Ω tersebut merupakan ikatan neutral-to-GND yang paralel. Akibatnya, ESP-07, BL0940, VCC, switch konfigurasi, dan konektor programming direferensikan ke mains dan tidak boleh dinyatakan sebagai SELV.

### B3 — Konektor programming direferensikan ke mains

U6 adalah konektor enam pin `ZX-MX1.25-6PWZ` pada `shape[195]`. Pemetaan berdasarkan sumber adalah:

| Pin U6 | Net | Koneksi downstream | Bukti |
|---:|---|---|---|
| 1 | VCC | Domain 3.3 V ESP-07 | `shape[121,195]` |
| 2 | GND | GND elektronik yang direferensikan ke netral | `shape[122,195]` ditambah B2 |
| 3 | RXD | ESP RXD0 | `shape[123,159,195]` |
| 4 | TXD | ESP TXD0 | `shape[124,158,195]` |
| 5 | RST | Reset ESP | `shape[125,142,182,195]` |
| 6 | IO0 | ESP GPIO0/boot strap | `shape[126,162,185,195]` |

Proses produksi harus melarang programming non-isolated dan koneksi mains secara bersamaan, atau menyediakan fixture galvanically isolated yang telah dikualifikasi beserta interlock yang dipaksakan.

### B4 — Tidak ada fuse pada satu-satunya sheet skematik

Satu-satunya komponen surge line-to-neutral yang eksplisit adalah R10, source value `10D471K-C8760`, MPN `10D471K`, pada `shape[115]`; komponen ini dipasang melintang antara `LINE` dan `NETRAL` melalui `shape[119,120]` dan label `shape[117,118]`.

Tidak ditemukan fuse, thermal fuse, fusible resistor, inrush limiter, pemutus over-temperature berbasis hardware, atau pemutus overload independen dari hardware di antara 50 komponen elektrik. Sumber tidak membuktikan apakah U1 memiliki proteksi internal.

### B5 — Konektor mains/beban belum terkualifikasi untuk production dalam sumber

- P1 adalah `CONN-TH_3P-P5.08` generik; manufacturer, MPN, dan identifier LCSC kosong: `shape[197]`.
- J1/J2/J3 adalah `HDR-M-2.54_1X1` generik; manufacturer dan MPN kosong. Hanya LCSC C81276 yang tercatat: `shape[199..201]`.

Rated voltage, rated current, kenaikan temperatur, flammability, retensi, perlindungan sentuh, maupun kelayakan protective-earth tidak dapat diklaim dari record tersebut.

### B6 — Identitas AC/DC dan relay yang kritis belum dikunci

- U1 adalah simbol custom `acDcPsTegak` / `ACDCPSTEGAK` dengan field manufacturer, MPN, supplier, dan rating kosong: `shape[211]`.
- U4 memiliki source value `HFE20-1/5-1HST-L2`, tetapi field manufacturer, MPN, dan supplier kosong: `shape[36]`.

Rating produksi, isolasi, approval, pinout, energi coil, daya tahan kontak, dan approved alternate masih TBD.

### B7 — Tidak ada feedback status relay

U4 direpresentasikan sebagai dual-coil latching relay: SET pin 1, coil COM pin 2, RST pin 3, contact COM pin 4, NC pin 5, dan NO pin 6 pada `shape[36]`. Tidak ada kontak bantu atau net sensing yang melaporkan posisi kontak aktual. Oleh karena itu, reset, brownout, kegagalan pulse coil, kontak yang welded, atau status yang tertahan secara mekanis tidak dapat dibedakan oleh hardware pada skematik.

## 3. Pemetaan jalur daya eksternal

### Konektor input P1

| Pin P1 | Net sumber | Bukti |
|---:|---|---|
| 1 | EARTH | `shape[197,198,210]` |
| 2 | NETRAL | `shape[117,197,209]` |
| 3 | LINE | `shape[118,197,208]` |

### Pemetaan output/header

| Designator | Net sumber | Bukti |
|---|---|---|
| J1 | LINE | `shape[199,204,207]` |
| J2 | OUT | `shape[200,203,205]` |
| J3 | EARTH | `shape[201,202,206]` |

Sumber tidak menetapkan apakah header tersebut merupakan placeholder, test point, anchor flying-lead, atau terminal beban final.

### Pemetaan simbol AC/DC U1

| Pin U1 | Nama pin simbol | Net yang terhubung | Bukti |
|---:|---|---|---|
| 1 | `Line` | `NETRAL` | `shape[7,211,212]` |
| 2 | `Net` | `LINE` | `shape[6,211,213]` |
| 3 | `V+` | `+5V` | `shape[97,211,214]` |
| 4 | `GND` | GND | `shape[211,215,216]` |

Nama pin simbol `Line` dan `Net` bertentangan dengan nama net yang terhubung. Hal ini mungkin tidak berpengaruh secara fungsi untuk input AC non-polar, tetapi tidak dapat diterima sebagai bukti pinout produksi sampai diselesaikan terhadap datasheet dan footprint modul yang tepat.

## 4. Rail daya

### Rail +5V

Rail `+5V` bersumber dari U1.3 dan memasok:

- U7 VIN pin 3: `shape[18,22,23]`.
- Pin common dual-coil U4 pin 2: `shape[24,31,32,36]`.
- Cathode D1/D2: `shape[24..32]`.
- C8 100 µF dan C11 100 nF: `shape[228..233]`.

### Rail VCC

U7 teridentifikasi dalam sumber sebagai `AMS1117-3.3`, sehingga VCC dimaksudkan bernilai 3.3 V:

- U7 pin 1 = GND; pin 3 = +5V; pin 2 dan 4 = VCC: `shape[17..23]`.
- C9 100 µF dan C10 100 nF terhubung antara VCC dan GND: `shape[11..16]`.
- VCC memasok VDD BL0940, VCC ESP-07, pull-up boot/reset, dan cabang LED status.

Sumber tidak memuat load budget, perhitungan termal regulator, margin dropout minimum, bukti ESR/stabilitas kapasitor, atau verifikasi DC-bias MLCC.

## 5. Pemetaan pin final ESP-07 berdasarkan sumber

U3 adalah ESP-07, Ai-Thinker, source MPN `ESP-07`, LCSC C82894 pada `shape[133]`.

| Pin modul | Nama simbol | Net | Fungsi yang terhubung | Bukti sumber yang tepat |
|---:|---|---|---|---|
| 1 | `REST` | RST | R5 10 kΩ pull-up ke VCC; C2 100 nF ke GND; U6.5 | `shape[133,142,143,147,179,180,182,183,195]` |
| 2 | ADC | AIN | Hanya stub bernama; tidak ditemukan pin komponen lain | `shape[133,148]` |
| 3 | CH_PD | node lokal tanpa nama | R6 10 kΩ pull-up ke VCC | `shape[133,144,145,147]` |
| 4 | GPIO16 | IO16 | Hanya stub bernama; tidak ditemukan pin komponen lain | `shape[133,168]` |
| 5 | GPIO14 | IO14 | BL0940 pin 8 ZX | `shape[53,66,133,167]` |
| 6 | GPIO12 | IO12 | BL0940 pin 9 CF | `shape[54,65,133,165]` |
| 7 | GPIO13 | RXD_EM | BL0940 pin 13 TX/SDO | `shape[58,63,133,166]` |
| 8 | VCC | VCC | Rail 3.3 V | `shape[133,137,138,139]` |
| 9 | GND | GND | GND yang direferensikan ke netral | `shape[133,149]` ditambah B2 |
| 10 | GPIO15 | TXD_EM | BL0940 pin 12 RX/SDI; R9 pulldown 10 kΩ | `shape[57,62,133,164,171,184]` |
| 11 | GPIO2 | IO2 | Cathode LED1; VCC -> R12 2 kΩ -> anode LED | `shape[133,163,173..178]` |
| 12 | GPIO0 | IO0 | R7 pull-up 10 kΩ; SW1 ke GND; U6.6 | `shape[126,133,162,169,170,185..195]` |
| 13 | GPIO4 | IO4 | R23 100 Ω -> gate Q4 -> driver RESET relay | `shape[39,133,161,220,222,224,227]` |
| 14 | GPIO5 | IO5 | R22 100 Ω -> gate Q3 -> driver SET relay | `shape[40,133,160,219,221,223,225,226]` |
| 15 | RXD0 | RXD | U6.3 | `shape[123,133,159,195]` |
| 16 | TXD0 | TXD | U6.4 | `shape[124,133,158,195]` |

Implikasi produksi:

- GPIO0 memiliki jaringan pull-up dan switch-to-GND yang terlihat dalam sumber.
- GPIO15 memiliki pulldown 10 kΩ tetapi digunakan bersama RX/SDI BL0940; loading saat boot harus dibuktikan.
- GPIO2 tidak memiliki pull-up yang hanya berupa resistor secara eksplisit. Satu-satunya jalur pull-up yang terlihat adalah VCC melalui R12 dan LED1; margin boot-strap harus diuji terhadap Vf LED, temperatur, toleransi komponen, dan ramp VCC.
- GPIO4/GPIO5 mengendalikan coil latching relay dan harus tetap tidak aktif selama reset, ROM boot, OTA, watchdog, dan brownout.

## 6. Pemetaan pin final BL0940 berdasarkan sumber

U2 adalah BL0940, field manufacturer `BL(上海贝岭)`, LCSC C691894, pada `shape[78]`.

| Pin U2 | Nama simbol | Koneksi sumber | Bukti |
|---:|---|---|---|
| 1 | VDD | VCC | `shape[50,59,77,78]` |
| 2 | VI | No-connect eksplisit melalui endpoint wire | `shape[51,76,78]` |
| 3 | IP1 | IP1; R13 523 Ω dari NET_OUT, C1 100 nF ke GND | `shape[71,84,86,88,89,93]` |
| 4 | IN1 | IN1; R14 523 Ω dari NETRAL, C4 100 nF ke GND | `shape[70,81,83,85,87,90,94]` |
| 5 | VP | VP; lima resistor 390 kΩ dari LINE, R1 523 Ω dan C3 100 nF ke GND | `shape[69,98..114]` |
| 6 | VN | VN, disatukan langsung ke GND | `shape[64,67,68,72,78]` |
| 7 | GND | GND | `shape[64,67,78]` |
| 8 | ZX | IO14 -> ESP GPIO14 | `shape[53,66,78,167]` |
| 9 | CF | IO12 -> ESP GPIO12 | `shape[54,65,78,165]` |
| 10 | SEL | No-connect eksplisit | `shape[55,60,78]` |
| 11 | SCLK | No-connect eksplisit | `shape[56,61,78]` |
| 12 | RX/SDI | TXD_EM -> ESP GPIO15; R9 pulldown | `shape[57,62,78,164,171,184]` |
| 13 | TX/SDO | RXD_EM -> ESP GPIO13 | `shape[58,63,78,166]` |
| 14 | VPP | No-connect eksplisit | `shape[52,78]` |

Topologi no-connect eksplisit harus diperiksa terhadap revisi datasheet BL0940 yang terkontrol sebelum rilis, khususnya VI, SEL, SCLK, VPP, dan koneksi langsung VN-to-GND.

## 7. Front-end metering

### Kanal arus

- R15 teridentifikasi dalam sumber sebagai `LR2512-22R0005F4`, 0.5 mΩ, R2512, LCSC C154666: `shape[82]`.
- R15.2 berada pada `NETRAL`; R15.1 berada pada `NET_OUT`: `shape[80..96]`.
- R13 523 Ω menghubungkan `NET_OUT` ke IP1: `shape[84,88,89,93]`.
- R14 523 Ω menghubungkan `NETRAL` ke IN1: `shape[81,83,87,90,94]`.
- C1 dan C4 masing-masing 100 nF dari net sense-input ke GND: `shape[85,86,91,92]`.

Perhitungan nominal hanya berdasarkan source value:

| Arus beban | Tegangan shunt | Disipasi shunt |
|---:|---:|---:|
| 10 A | 5 mV | 50 mW |
| 16 A | 8 mV | 128 mW |
| 20 A | 10 mV | 200 mW |

Nilai tersebut merupakan hasil perhitungan, bukan rating yang telah disetujui. Kapasitas arus PCB, pulse rating shunt, TCR, toleransi, routing Kelvin, pemanasan solder joint, dan kalibrasi belum terbukti.

### Kanal tegangan

- LINE masuk ke R11, kemudian R4, R3, R2, dan R8; semuanya bernilai 390 kΩ: `shape[98..104,111..114]`.
- VP dibebani oleh R1 523 Ω dan C3 100 nF ke GND: `shape[105..110]`.
- Resistansi nominal high-side adalah 1.95 MΩ.

Perilaku nominal hasil perhitungan:

| LINE RMS | Arus divider | VP RMS | Disipasi per resistor 390 kΩ |
|---:|---:|---:|---:|
| 230 V | 117.917 µA | 61.671 mV | 5.423 mW |
| 240 V | 123.044 µA | 64.352 mV | 5.905 mW |
| 265 V | 135.861 µA | 71.055 mV | 7.199 mW |

Persyaratan working-voltage, surge, pulse, toleransi, voltage coefficient, failure-mode, spacing, dan kontaminasi untuk kelima resistor 0603 masih TBD.

## 8. Topologi relay dan driver coil

U4 pada `shape[36]` direpresentasikan sebagai dual-coil latching relay enam pin:

| Pin U4 | Nama simbol | Net |
|---:|---|---|
| 1 | SET | SET1 |
| 2 | COM, common coil | +5V |
| 3 | RST | RST1 |
| 4 | COM, kontak | OUT |
| 5 | NC | No-connect eksplisit `shape[34]` |
| 6 | NO | NET_OUT |

Driver SET:

`ESP GPIO5/IO5 -> R22 100 Ω -> gate Q3`, dengan R20 100 kΩ dari gate ke GND; drain Q3 -> R16 10 Ω -> SET1; cathode D1 -> +5V dan anode -> SET1. Bukti: `shape[24,26,28,30,38,40,42,44,48,219,221,223,225,226]`.

Driver RESET:

`ESP GPIO4/IO4 -> R23 100 Ω -> gate Q4`, dengan R21 100 kΩ dari gate ke GND; drain Q4 -> R17 10 Ω -> RST1; cathode D2 -> +5V dan anode -> RST1. Bukti: `shape[25,27,29,37,39,41,43,45,46,220,222,224,227]`.

Sumber menunjukkan flyback diode, resistor seri gate, dan gate pulldown. Sumber tidak membuktikan rating coil, pulse width, interlock coil simultan, contact suppression, posisi relay, deteksi contact welding, atau kondisi power-up yang aman.

## 9. Arsitektur proteksi dan keselamatan yang terlihat dalam sumber

Terlihat:

- R10/MOV 10D471K melintang antara LINE dan NETRAL: `shape[115..120]`.
- Jalur berlabel protective-earth dari P1.1 ke J3: `shape[198,201,202,206,210]`.
- Flyback diode coil relay D1/D2: `shape[24..32]`.
- Gate pulldown R20/R21 dan resistor seri gate R22/R23.

Tidak ada pada sheet skematik ini:

- fuse yang dapat diganti atau non-resettable;
- thermal disconnect MOV;
- sensor/pemutus over-temperature independen;
- trip overcurrent independen;
- contact snubber pada sisi line;
- pemutus dua pole;
- feedback kontak relay;
- interface servis/programming terisolasi;
- proteksi ESD pada konektor servis;
- rating keselamatan konektor;
- anotasi barrier isolasi eksplisit.

Ketidakhadiran pada skematik tidak membuktikan ketidakhadiran pada enclosure, harness, modul daya, atau PCB yang akan dibuat kemudian. Namun, hal tersebut berarti fitur-fitur itu tidak dapat diklaim berdasarkan sumber ini.

## 10. Kontradiksi sumber dan data yang belum terselesaikan

1. Nama net `NETRAL` salah eja di seluruh sumber; tidak ada net `NEUTRAL`.
2. Pin `Line` U1 terhubung ke `NETRAL`, sedangkan pin `Net` U1 terhubung ke `LINE`: `shape[6,7,211..213]`.
3. R18 dan R19 merupakan dua ikatan neutral-to-GND 0 Ω yang sama-sama dinyatakan terpasang; tidak ada opsi DNP yang tercatat: `shape[1..5,217]`.
4. SW1 menggunakan package `SW-TH_TSC017A04826A`, sedangkan source MPN-nya adalah `TSC017A03526A`: `shape[190]`.
5. Displayed value U4 menyerupai part number, tetapi field manufacturer/MPN/supplier kosong: `shape[36]`.
6. U1 memiliki package/simbol custom tanpa identitas terkontrol: `shape[211]`.
7. P1 tidak memiliki manufacturer, MPN, atau supplier part: `shape[197]`.
8. J1/J2/J3 tidak memiliki data manufacturer dan MPN: `shape[199..201]`.
9. U5 `smartPlug2` adalah grafis tanpa pin elektrik dan tidak boleh dianggap sebagai IC atau item BOM elektrik: `shape[218]`.
10. Dua frame gambar lengkap berada bersama-sama dalam sheet yang sama: `shape[0]` dan `shape[196]`.
11. Title block masih mencantumkan `Your Company`, revisi `1.0`, tanggal `2025-05-10`, dan drawn by `mamanbudiman`: di dalam `shape[0]` dan diduplikasi dalam `shape[196]`.
12. Pin reset ESP diberi nama `REST` dalam simbol: `shape[133]`.
13. R10 secara elektrik merupakan MOV berdasarkan value/MPN, tetapi menggunakan designator resistor `R10` dan kategori package `RES-TH`: `shape[115]`.
14. VN dan GND U2 secara eksplisit di-short oleh wire meskipun memiliki pin/nama terpisah: `shape[64,67,68,72,78]`.
15. Pull-up boot GPIO2 mengandalkan cabang LED, bukan resistor pull-up eksplisit: `shape[163,173..178]`.
16. ADC/AIN dan GPIO16/IO16 U3 tidak memiliki consumer berupa pin komponen lain pada sheet ini: `shape[133,148,168]`.
17. Displayed value kapasitor dan resistor tidak menyatakan toleransi, tegangan, daya, TCR, dielectric, rating temperatur, atau approved alternate.
18. Tidak ditemukan metadata DNP/varian populasi eksplisit untuk link kritis yang tampak opsional.

## 11. Identitas komponen yang firm dibandingkan dengan TBD

Komponen aktif/relevan-keselamatan yang teridentifikasi dalam sumber meliputi:

- U3 ESP-07, Ai-Thinker, LCSC C82894: `shape[133]`.
- U2 BL0940, LCSC C691894: `shape[78]`.
- U7 AMS1117-3.3, LCSC C6186: `shape[23]`.
- Q3/Q4 AO3400A, LCSC C20917: `shape[219,220]`.
- D1/D2 SM4007PL, LCSC C64898: `shape[25,26]`.
- R15 LR2512-22R0005F4, LCSC C154666: `shape[82]`.
- R10 10D471K, LCSC C8760: `shape[115]`.
- U6 ZX-MX1.25-6PWZ, LCSC C7430467: `shape[195]`.

Identitas yang masih kritis/TBD:

- Modul AC/DC U1.
- Manufacturer relay U4 dan MPN yang terkontrol.
- Konektor input P1.
- Implementasi terminal output/earth final J1/J2/J3.
- Klasifikasi U5 sebagai elemen yang murni mekanis/grafis.

Meskipun suatu MPN tersedia, kelayakannya belum terbukti sampai datasheet otoritatif, perhitungan derating, status lifecycle, kontrol supply-chain, dan approved alternate dilampirkan.

## 12. Klaim yang dilarang hanya berdasarkan bukti ini

Jangan menerbitkan hal-hal berikut sebagai klaim datasheet final hanya berdasarkan skematik ini:

- tegangan/frekuensi input nominal atau maksimum;
- arus beban kontinu atau peak maksimum;
- rating beban resistif, induktif, motor, heater, atau kapasitif;
- daya tahan mekanis/elektrik relay;
- akurasi, resolusi, drift, atau interval kalibrasi pengukuran;
- kelas isolasi, status SELV, kelas proteksi, creepage, atau clearance;
- koordinasi fuse, kategori surge, atau kategori overvoltage;
- temperatur operasi/penyimpanan;
- IP rating atau flammability enclosure;
- sertifikasi EMC, keselamatan, radio, atau metrologi;
- kontinuitas protective-earth;
- jangkauan Wi-Fi atau kepatuhan RF;
- kesiapan produksi.

## 13. Pertanyaan penutupan yang wajib untuk hardware/pre-production

1. Apakah nama `LINE` dan `NETRAL` benar secara fisik pada P1 dan interface beban?
2. Apakah pemutusan neutral dilakukan dengan sengaja? Jika ya, persyaratan keselamatan produk mana yang mengizinkannya?
3. Apakah relay harus memutus LINE, atau kedua pole harus diputus?
4. Apakah polaritas plug/socket dijamin pada setiap instalasi target?
5. Apakah R18 dan R19 keduanya memang dimaksudkan untuk dipasang?
6. Mengapa diperlukan dua ikatan neutral-to-GND 0 Ω yang paralel?
7. Apakah arsitektur resmi memang sengaja non-isolated?
8. Bagaimana seluruh bagian yang dapat diakses pengguna dan personel servis dijauhkan dari domain yang direferensikan ke live?
9. Bagaimana isolasi actuator SW1 dikualifikasi?
10. Apakah programming dilarang setiap kali mains terhubung?
11. Isolasi galvanik dan interlock apa yang disediakan oleh fixture produksi?
12. Apa manufacturer, MPN, pinout, rentang input, rating output, rating isolasi, dan kumpulan approval U1 yang tepat?
13. Apakah U1 memiliki fuse atau perangkat proteksi termal yang terdokumentasi?
14. Tipe, rating, breaking capacity, dan penempatan fuse eksternal apa yang akan ditambahkan?
15. Bagaimana kegagalan MOV dikoordinasikan dengan fuse dan thermal disconnect?
16. Apa manufacturer/MPN U4 yang tepat dan revisi datasheet relay yang disetujui?
17. Berapa rating kontak U4 untuk beban resistif, induktif, kapasitif, motor, dan SMPS?
18. Berapa batas arus coil, pulse minimum, pulse maksimum, dan cooldown?
19. Bagaimana aktivasi SET/RESET secara bersamaan dicegah dalam hardware dan firmware?
20. Bagaimana status aktual latching relay dideteksi setelah brownout atau reset?
21. Bagaimana kegagalan welded-contact dideteksi atau dimitigasi?
22. Apakah RC snubber atau contact suppression lain diperlukan?
23. Apa part final P1/J1/J2/J3 beserta rating tegangan/arus/temperatur/flammability-nya?
24. Apakah J1/J2/J3 benar-benar terminal mains atau hanya placeholder gambar?
25. Apakah produk termasuk Class I atau Class II?
26. Jika Class I, bagaimana bonding PE, fault current, ground-bond resistance, retensi, dan korosi diverifikasi?
27. Berapa ketebalan tembaga PCB, lebar trace, struktur via, dan kenaikan temperatur terminal yang mendukung beban maksimum?
28. Berapa creepage/clearance, material group/CTI, pollution degree, dan overvoltage category yang berlaku?
29. Apakah isolation slot, barrier, coating, atau encapsulation diperlukan?
30. Apakah R15 di-routing secara Kelvin, dan berapa toleransi, TCR, pulse rating, serta batas termal solder joint-nya?
31. Berapa working-voltage dan surge rating setiap resistor divider 390 kΩ 0603?
32. Apakah datasheet BL0940 otoritatif menyetujui no-connect VI/SEL/SCLK/VPP dan topologi VN-to-GND yang ditampilkan?
33. Berapa target akurasi tegangan/arus/daya/energi dan test point yang berlaku?
34. Bagaimana kalibrasi offset, gain, phase, dan temperatur dilakukan serta ditelusuri per serial number?
35. Berapa kapasitansi aktual C7/C8/C9 pada kondisi DC bias dan temperatur?
36. Apakah AMS1117 stabil dengan teknologi kapasitor dan rentang ESR yang dipilih?
37. Berapa peak-current dan thermal budget 3.3 V selama transmisi Wi-Fi ESP?
38. Apakah GPIO2 tetap menjadi strap boot-high yang valid pada seluruh corner Vf LED, temperatur, toleransi, dan ramp-rate?
39. Dapatkah loading BL0940 mengganggu GPIO15 selama boot ESP?
40. Apakah GPIO4/GPIO5 dijamin tidak aktif selama reset, ROM boot, OTA, watchdog, dan brownout?
41. Proteksi over-temperature dan overload independen apa yang diperlukan?
42. Uji ESD, EFT, surge, conducted-emission, radiated-emission, immunity, dielectric, leakage, ground-bond, dan endurance mana yang menjadi release gate?
43. Fixture end-of-line seperti apa yang membuktikan wiring aman, operasi relay, kalibrasi metering, kontinuitas earth, dan isolasi tanpa mengekspos operator ke mains?
44. Proses controlled BOM, lifecycle, PCN, lot traceability, dan approved-alternate apa yang berlaku?
45. Paket bukti PCB, enclosure, firmware, kalibrasi, termal, EMC, keselamatan, dan RF mana yang wajib tersedia sebelum status diubah dari engineering sample menjadi pre-production?
