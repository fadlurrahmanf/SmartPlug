# Audit PCB SmartPlug V2

**Status:** audit statis source PCB; **belum merupakan bukti Gerber/fabrikasi, unit terakit, keselamatan listrik, EMC, RF, atau uji beban.**  
**Sumber:** `D:\IoT\SmartPlug\hardware\easyeda\1-PCB_PCB_smartPlug2Ver2.json`  
**Format sumber:** EasyEDA, `docType=3`, editor `6.5.51`  
**Tanggal audit:** 2026-08-25  
**Metode:** parsing objek layout, pad, net, outline, layer, dan aturan DRC secara read-only.

## Kesimpulan eksekutif

PCB ini **belum layak dinyatakan pre-production atau production-ready**. Blocker terpenting yang terlihat langsung dari layout adalah:

1. Relay satu kutub memutus jalur bernama `NETRAL`, sementara `LINE` diteruskan langsung ke keluaran. Kondisi OFF masih dapat meninggalkan beban/outlet terhubung ke line hidup.
2. `GND` terhubung langsung ke `NETRAL` melalui dua resistor 0 ohm, R18 dan R19. ESP-07, tombol, serta konektor servis U6 karena itu merupakan rangkaian mains-referenced, bukan SELV.
3. Tidak ditemukan fuse, fusible resistor, atau thermal fuse pada layout. MOV R10 berada antara line-neutral tanpa bukti koordinasi proteksi.
4. Jalur protective earth hanya berupa satu trace PCB 2,54 mm dari P1 ke J3, dengan endpoint ber-footprint header generik.
5. Jalur arus utama hanya 2,54 mm dan sebagian sengaja dibuka soldermask-nya. Copper weight, ketebalan plating, current rating, solder build-up, dan kenaikan temperatur tidak ditentukan.
6. Track `NET_OUT` berada sekitar 0,265 mm dari routed board edge; bukaan soldermask-nya mencapai sekitar 0,214 mm dari edge.
7. Aturan clearance default hanya 0,1524 mm. Jarak distinct-net terkecil yang teramati dari geometri sekitar 0,155 mm.
8. Identitas/rating komponen kritis P1, U1, U4, serta J1-J3 belum terkunci secara cukup untuk produksi.
9. Tidak ada bukti DRC mains-specific, antenna keepout, test point khusus, fiducial, revision/traceability marking, atau paket fabrication notes.

## 1. Unit, outline, dan dimensi mekanik

### 1.1 Konversi unit

Koordinat EasyEDA pada file ini konsisten dengan:

- 1 unit = 10 mil = **0,254 mm**.
- Pitch geometris P1 sebesar 19,685 unit menjadi 5,000 mm; angka ini tidak
  sama dengan label metadata footprint `P5.08`. Pitch/footprint produksi dan
  MPN konektor persis harus ditutup sebagai mismatch, bukan diasumsikan cocok.
- Lebar track 10 unit menjadi 2,540 mm.
- BBox 177,2 x 299,8 unit menjadi **45,009 x 76,149 mm**.

BBox mencakup stroke/objek terluar. Centerline outer BoardOutline membentang dari x=3933,386 hingga x=4110,551 dan y=3242,394 hingga y=3541,606, sehingga ukuran nominal outline sekitar **45,0 x 76,0 mm**.

### 1.2 Bentuk board

Outline bukan persegi panjang sederhana:

- sisi kiri mempunyai dua arc, di sudut atas dan bawah;
- sisi kanan mempunyai inset/step sedalam sekitar 16,5 mm;
- outer outline dan empat NPTH disimpan di dalam pseudo-component `U5 / SMARTPLUG2`, bukan sebagai seluruhnya objek mekanik top-level.

Menyimpan outline board sebagai bagian footprint U5 berisiko mengotori BOM dan membuat export CAM, panelisasi, atau revisi mekanik lebih rapuh. Outer outline dan NPTH sebaiknya dipindahkan ke objek board/mechanical native dan U5 dikeluarkan dari BOM/placement.

### 1.3 Lubang dan internal cutout

| Objek | Bukti koordinat/ukuran | Hasil metrik |
|---|---:|---:|
| NPTH 1 | diameter 13,7795 unit | 3,50 mm |
| NPTH 2 | diameter 13,7795 unit | 3,50 mm |
| NPTH 3 | diameter 5,9055 unit | 1,50 mm |
| NPTH 4 | diameter 5,9055 unit | 1,50 mm |
| Cutout irregular | bounding 19 x 55,5 unit | **4,826 x 14,097 mm** |
| Bagian sempit cutout irregular | 3,5 unit | **0,889 mm** |
| Cutout persegi panjang | 3 x 31 unit | **0,762 x 7,874 mm** |

Kedua internal cutout adalah contour BoardOutline tertutup. Fabricator harus mengonfirmasi bahwa contour tersebut diproses sebagai routed NPTH cutout, diameter router tersedia, toleransi slot dapat dicapai, dan sisa web board cukup kuat.

## 2. Layer, copper, pad, dan via

### 2.1 Ringkasan objek top-level

- 135 objek `TRACK` total;
- 51 objek `LIB`;
- 30 objek `VIA`;
- 3 free-text top-level;
- 1 copper area;
- 2 internal BoardOutline track top-level, di luar outer outline yang tertanam di U5.

### 2.2 Penggunaan layer

PCB menggunakan dua signal layer saja:

| Layer | Fungsi | Track routing |
|---|---|---:|
| 1 | Top copper | 101 |
| 2 | Bottom copper | 27 |
| 7 | Top soldermask opening | 3 |
| 8 | Bottom soldermask opening | 2 |
| 10 | Internal BoardOutline top-level | 2 |

Inner1 hingga Inner32 terdapat pada daftar layer bawaan editor, tetapi tidak digunakan oleh routing. Tidak ada bottom-side SMD pad.

Pad yang teramati:

- 117 pad SMD di Top copper;
- 22 pad multilayer/PTH;
- 0 pad SMD di Bottom copper.

Hanya ada **satu copper area**, yaitu `NETRAL` pada Top layer. Tidak ada GND plane kontinu.

### 2.3 Lebar track

| Layer | Lebar unit | Lebar mm | Jumlah |
|---|---:|---:|---:|
| Top | 1,0 | 0,254 | 3 |
| Top | 1,5 | 0,381 | 50 |
| Top | 2,0 | 0,508 | 4 |
| Top | 3,0 | 0,762 | 36 |
| Top | 4,0 | 1,016 | 3 |
| Top | 5,0 | 1,270 | 2 |
| Top | 10,0 | 2,540 | 3 |
| Bottom | 1,5 | 0,381 | 9 |
| Bottom | 2,0 | 0,508 | 4 |
| Bottom | 3,0 | 0,762 | 11 |
| Bottom | 10,0 | 2,540 | 3 |

Soldermask opening selebar 10,4 unit atau **2,642 mm** terdapat pada:

- dua segmen `LINE` di Bottom;
- jalur `OUT` di Top;
- jalur `NET_OUT` di Top, dengan geometri `NET_OUT` tercatat dua kali/duplikat.

`EARTH` tidak memiliki opening serupa. Pola ini mengindikasikan kemungkinan solder reinforcement pada jalur arus, tetapi file tidak menentukan tebal solder, proses selective solder, acceptance criteria, atau cross-section minimum hasil akhir.

### 2.4 Via

Terdapat 30 via. Record tipikal menggunakan diameter 2,441 unit dan hole-radius 0,6102 unit, setara kira-kira:

- pad via: 0,620 mm;
- finished drill: 0,310 mm;
- annular ring radial: sekitar 0,155 mm.

Tidak ada via pada net arus utama `LINE`, `NETRAL`, `NET_OUT`, `OUT`, atau `EARTH`; jalur arus utama tidak melakukan layer transition.

## 3. BOM dan footprint evidence

Terdapat 51 `LIB` record. Satu record adalah pseudo-component U5 untuk mekanik board, sehingga tersisa 50 designator elektrik/assembly bila U5 dikeluarkan.

| Kelompok | Jumlah | Catatan |
|---|---:|---|
| C1-C11 | 11 | 0603/1206; 100 nF, 22 uF, 100 uF |
| D1-D2 | 2 | SM4007PL |
| J1-J3 | 3 | metadata footprint generik `HDR-M-2.54_1x1` |
| LED1 | 1 | LED0603 blue |
| P1 | 1 | `CONN-TH_3P-P5.08` |
| Q3-Q4 | 2 | AO3400A |
| R1-R23 | 23 | termasuk MOV R10 dan shunt R15 |
| SW1 | 1 | TSC017A03526A |
| U1-U7 | 7 | U5 adalah pseudo-component board |

Komponen kritis yang terlihat:

- R10: MOV `10D471K`;
- R15: shunt `0.5 mOhm`, package R2512, MPN `LR2512-22R0005F4`;
- U1: custom `ACDCPSTEGAK`;
- U2: BL0940;
- U3: ESP-07;
- U4: `HFE20-1/5-1HST-L2`;
- U6: `ZX-MX1.25-6PWZ`;
- U7: AMS1117-3.3.

### 3.1 Masalah identitas dan rating

- P1 tidak mempunyai manufacturer/MPN yang terkunci pada metadata PCB.
- U1 adalah footprint/module custom tanpa manufacturer, MPN, supplier part, atau datasheet yang dapat ditelusuri dari PCB.
- U4 mempunyai value menyerupai part number, tetapi metadata manufacturer/MPN/supplier kosong.
- J1-J3 menyatakan footprint header 2,54 mm generik. Metadata ini tidak boleh dianggap sebagai bukti rating mains, current, temperatur, creepage, atau retention.
- U5 harus dikeluarkan dari BOM/placement dan mekaniknya dinormalisasi.
- Tidak ada variant/DNP/alternate-part evidence pada PCB JSON.

### 3.2 Anomali footprint

U4 pad 5 adalah pad Top copper berukuran hanya 0,1 x 0,1 unit, yaitu sekitar **0,0254 x 0,0254 mm**, tanpa net. Ini merupakan artefak CAM/DFM yang tidak realistis untuk fabrikasi. Pad tersebut harus dihapus atau dipindahkan ke layer non-copper sebagai mechanical/NC marker.

R15 memakai footprint dua pad. Sense `IP1` dan `IN1` terhubung ke dua power pad yang sama, bukan ke terminal Kelvin khusus. Akurasi metering harus memasukkan resistansi pad/copper, gradien temperatur, TCR, self-heating, solder-joint aging, dan toleransi kalibrasi produksi.

## 4. Topologi mains dan safety

### 4.1 Jalur input-output

Net dan pad menunjukkan topologi berikut:

```text
P1 pin 3 LINE    -> J1 LINE secara langsung
                 -> U1 input LINE
                 -> R10 MOV dan divider tegangan

P1 pin 2 NETRAL -> R15 shunt -> NET_OUT
                 -> U4 relay contact -> OUT -> J2

P1 pin 1 EARTH  -> trace PCB -> J3
```

U4 mempunyai contact pad `NET_OUT` dan `OUT`; coil dikendalikan melalui `SET1`/`RST1`. Dengan nama net dan koneksi yang tersedia, relay satu kutub memutus neutral, bukan line.

**Blocker:** saat relay OFF, beban/outlet masih dapat terhubung ke `LINE`. Desain harus memutus line, atau memutus kedua kutub, kecuali pengujian wiring sistem membuktikan bahwa nama/terminal saat ini salah. Koreksi tidak boleh hanya berupa perubahan label; jalur fisik dan polaritas instalasi harus diverifikasi.

### 4.2 Logic bukan SELV

R18 dan R19 masing-masing bernilai 0 ohm dan menjembatani `NETRAL` ke `GND`. Selain itu:

- U2 BL0940 menggunakan GND/VCC yang sama;
- `TXD_EM` dan `RXD_EM` menuju ESP-07 tanpa digital isolator;
- U6 membawa GND, VCC, RXD, TXD, RST, dan IO0;
- SW1 mempunyai pad GND;
- ESP-07 dan regulator berada pada domain GND yang sama.

Akibatnya, seluruh logic harus diperlakukan sebagai **hazardous live / mains-referenced**. Sekalipun U1 adalah modul AC-DC terisolasi, dua link neutral-GND dan metering direct-coupled menghilangkan manfaat isolation bagi interface yang dapat disentuh.

Implikasi minimum:

- U6 tidak boleh dapat diakses atau disambungkan ke programmer grounded saat board bertegangan;
- aktuator SW1, light pipe, casing, screw, kabel servis, dan fixture harus mempunyai insulation system yang sesuai;
- proses programming/calibration harus de-energized atau memakai isolation yang tervalidasi;
- desain perlu memilih arsitektur jelas: seluruh logic tertutup sebagai live circuitry, atau domain SELV nyata dengan isolation barrier lengkap.

### 4.3 Proteksi surge dan overcurrent

R10 MOV berada langsung antara `LINE` dan `NETRAL`. Tidak ditemukan footprint/designator untuk:

- fuse;
- thermal fuse;
- fusible resistor;
- resettable protector yang terbukti sesuai;
- thermal disconnect MOV.

Proteksi internal U1, bila ada, belum diketahui dan tidak otomatis melindungi load path, relay, shunt, terminal, atau PE path. MOV dapat gagal short; koordinasi dengan upstream/internal fuse, let-through energy, surge class, end-of-life mode, dan separation dari plastik wajib dibuktikan.

### 4.4 Protective earth

`EARTH` diteruskan dari P1 ke J3 menggunakan satu Bottom trace selebar 2,54 mm. Endpoint J3 memakai metadata header generik. Tidak terlihat redundant mechanical bond atau PE hardware khusus.

Jalur ini belum dapat dianggap memenuhi protective-earth continuity karena tidak ada bukti:

- copper weight dan cross-section minimum;
- short-circuit/fault-current withstand;
- terminal torque dan pull-out;
- solder-joint reliability;
- corrosion/fretting;
- ground-bond resistance;
- creepage terhadap line/neutral;
- PE symbol dan assembly verification.

## 5. Jalur arus dan thermal

Jalur arus utama yang terlihat:

- `LINE`: Bottom, 2,54 mm, dengan soldermask opening;
- `NETRAL`: P1 ke copper area Top dan R15;
- `NET_OUT`: Top, 2,54 mm, dengan soldermask opening;
- `OUT`: Top, 2,54 mm, dengan soldermask opening;
- `EARTH`: Bottom, 2,54 mm, tanpa opening serupa.

P1 menggunakan pad copper sekitar 2,00 mm dengan hole sekitar 1,30 mm. J1-J3 menggunakan pad copper sekitar 3,81 mm dengan hole sekitar 1,905 mm. U4 memakai pad sekitar 1,80 mm dengan hole sekitar 1,20 mm.

Lebar trace saja tidak membuktikan rating 10 A atau 16 A. Penetapan rating memerlukan setidaknya:

- copper foil/plating thickness;
- finished conductor cross-section setelah etch tolerance;
- panjang jalur dan resistansi aktual;
- temperatur internal enclosure dan ventilasi;
- allowable temperature rise;
- solder reinforcement yang terkontrol;
- rating terminal, relay, shunt, dan solder joint;
- steady-state, overload, locked-load, dan abnormal-load test.

AMS1117-3.3 juga tidak mempunyai dedicated thermal pour/via yang jelas. Drop 5 V ke 3,3 V pada peak current ESP-07 perlu diuji terhadap dropout, brownout, junction temperature, dan ambient maksimum.

## 6. Clearance, creepage, dan board-edge margin

### 6.1 Aturan DRC tersimpan

Default rule pada JSON:

| Parameter | Unit | Metrik |
|---|---:|---:|
| Track width | 1,0 | 0,254 mm |
| Clearance | 0,6 | **0,1524 mm** |
| Via diameter | 2,4 | 0,610 mm |
| Via drill | 1,2 | 0,305 mm |

`isRealtime=true`, tetapi `isDrcOnRoutingOrPlaceVia=false`.

Perhitungan geometri primitive menemukan jarak same-layer terkecil antar-net berbeda sekitar **0,610 unit = 0,155 mm**, pada track `IP1` terhadap `IN1` di Top layer. Angka ini sangat dekat dengan default rule. Ini adalah observasi geometri, bukan laporan DRC tersertifikasi.

### 6.2 Jarak copper mains yang dapat dihitung langsung

| Pasangan | Edge-to-edge |
|---|---:|
| U1 `LINE` - `NETRAL` pad Top | **2,445 mm** |
| P1 `LINE` - `NETRAL` PTH pad | **3,000 mm** |
| P1 `NETRAL` - `EARTH` PTH pad | **3,000 mm** |
| J1 `LINE` - J2 `OUT` PTH pad | **3,683 mm** |
| R10 MOV `LINE` - `NETRAL` PTH pad | sekitar **5,498 mm** |

Kecukupan nilai tersebut tidak dapat diputuskan tanpa product standard, working voltage, overvoltage category, pollution degree, CTI/material group, altitude, coating, enclosure, dan fault model. Tidak ada parameter itu pada PCB JSON.

### 6.3 Copper terhadap board edge

| Objek | Margin copper ke routed edge | Margin opening ke edge |
|---|---:|---:|
| `NET_OUT` trace di sisi kiri | **0,265 mm** | **0,214 mm** |
| J1/J2 PTH pad ke right inset edge | **1,039 mm** | tidak berlaku |
| `OUT` trace ke right inset edge | **1,099 mm** | **1,048 mm** |

Margin `NET_OUT` sekitar 0,265 mm merupakan blocker fabrikasi dan safety: routing tolerance, copper pull-back, chipping, contamination, handling, coating, dan surface creepage belum mempunyai margin realistis.

Dua internal slot tidak membentuk isolation barrier yang sah karena R18/R19 dan jalur metering tetap menghubungkan domain neutral ke logic. Seluruh logic harus dinilai sebagai live circuitry sampai arsitektur diubah.

## 7. Routing, EMI, RF, dan signal integrity

- Tidak ada GND plane kontinu.
- Jalur IO4/IO5 dan power melintasi area luas serta berpindah layer melalui via.
- Return path untuk switching relay dan digital/RF tidak dikontrol dengan plane yang jelas.
- Tidak ditemukan objek atau teks `keepout`/`antenna` untuk ESP-07.
- Orientasi antenna, copper clearance, metal enclosure, mains wiring, dan jarak terhadap plastik belum tervalidasi terhadap datasheet module.
- Tidak terlihat RC snubber/contact TVS pada contact relay; hanya MOV line-neutral yang tersedia.
- Lima resistor 390 kohm package 0603 membentuk divider line-voltage. Continuous working-voltage, pulse/surge rating, derating, dan single-fault behavior tiap resistor harus diverifikasi, bukan hanya resistance/power.
- Relay contact life terhadap inductive/capacitive load, inrush, arcing, conducted emission, dan radiated emission belum memiliki bukti.
- Tidak ada bukti pre-compliance EMC, ESD, EFT, surge, dip/interruption, harmonic, atau RF coexistence.

## 8. Manufacturability dan fabrication package

Hal positif yang terlihat adalah seluruh SMD berada di sisi Top dan tidak ada via pada jalur arus utama. Namun file PCB belum memuat bukti produksi berikut:

- stackup dan finished board thickness;
- copper weight/plating minimum;
- FR-4 Tg, CTI/material group, dan UL94 class;
- surface finish;
- soldermask material dan thickness;
- minimum soldermask dam/sliver;
- NPTH/PTH tolerance;
- route tolerance dan router-bit untuk dua narrow cutout;
- finished-hole tolerance;
- controlled solder buildup pada jalur terbuka;
- stencil/paste rules;
- panelization, tooling rail, breakaway/tab, dan mouse-bite location;
- global/panel fiducial;
- assembly drawing, polarity/orientation, torque, adhesive, atau staking;
- AOI/AXI/ICT coverage;
- cleanliness/ionic contamination limit;
- conformal coating requirement dan keepout;
- approved BOM/AVL, lifecycle, alternates, dan counterfeit control.

Nama net `NETRAL` juga salah eja. Walaupun konsisten di sebagian layout, typo ini meningkatkan risiko salah mapping pada review, automation, test fixture, dokumentasi, dan revisi berikutnya. Normalisasi nama net harus dilakukan bersama ERC/connectivity review.

## 9. DRC, unrouted, test point, dan marking

### 9.1 DRC dan unrouted

- Tidak ada track pada layer Ratlines/layer 9.
- Tidak ada objek `DRCError` tersimpan.
- Tidak ada bukti report DRC/ERC terbaru.
- Absennya ratline/error object pada JSON **bukan** bukti bahwa netlist complete atau layout lolos DRC.
- `isDrcOnRoutingOrPlaceVia=false`, sehingga routing tidak dipaksa memeriksa semua rule saat dibuat.

Sebelum release, DRC harus memakai net class terpisah untuk line, neutral, switched mains, PE, live logic, dan—bila dibuat—SELV. Rule harus mencakup copper-to-edge/cutout, creepage, clearance, NPTH, annular ring, soldermask sliver, courtyard, silkscreen-to-pad, dan isolation-slot constraints.

### 9.2 Test point dan programming

Tidak ditemukan designator `TP`, objek testpoint, atau label test point. U6 adalah konektor programming/service, bukan test point aman, karena GND-nya neutral-referenced. J1-J3 adalah node mains dan tidak boleh diperlakukan sebagai probe point biasa.

Kebutuhan yang belum dipenuhi:

- ICT points untuk power rails dan signal penting;
- safe fixture untuk de-energized programming;
- metering calibration points dan reference load procedure;
- relay-contact continuity test;
- PE continuity/ground-bond fixture;
- hipot/leakage test fixture;
- unique device identity provisioning dan readback;
- fixture poka-yoke dan interlock agar programmer grounded tidak tersambung ke live board.

### 9.3 Marking

Free-text top-level yang ditemukan hanya `L`, `N`, dan `E` pada BottomSilk. Selain itu terdapat `smartPlug2`/`U5` dan ordinary component ref/value dari footprint.

Tidak ditemukan bukti marking berikut:

- board part number dan revision;
- rated voltage/current/frequency;
- protective-earth symbol; yang ada hanya huruf `E`;
- hazardous-live/warning symbol;
- fuse rating;
- polarity atau lengkapnya pinout U6;
- certification/flammability mark;
- serial, lot, date code, atau QR traceability;
- manufacturing test status;
- antenna keepout/metal exclusion note.

Tidak ditemukan fiducial object/text. Empat NPTH tidak berlabel tooling dan tidak boleh diasumsikan sebagai tooling datum tanpa mechanical drawing.

## 10. Gate wajib sebelum pre-production

1. **Perbaiki topologi switching:** putus `LINE` atau gunakan double-pole switching; verifikasi terminal mapping secara fisik.
2. **Tentukan arsitektur insulation:** seluruh logic tertutup sebagai live circuitry, atau desain isolation barrier nyata sampai interface pengguna/servis.
3. **Tambahkan proteksi terkoordinasi:** fuse/thermal protection, MOV end-of-life handling, dan abnormal fault analysis.
4. **Redesain PE dan jalur arus:** gunakan komponen/routing yang mempunyai rating serta bukti temperature-rise dan fault-current.
5. **Perbesar copper pull-back:** terutama `NET_OUT` di sisi kiri dan output pads/traces di right inset.
6. **Kunci komponen kritis:** exact MPN, datasheet revision, rating, approvals, lifecycle, dan alternate rule untuk P1, U1, U4, J1-J3, MOV, shunt, regulator, serta ESP module.
7. **Buat rule DRC mains-specific** berdasarkan standard target, OVC, pollution degree, CTI, altitude, dan enclosure.
8. **Perbaiki footprint/mechanical data:** hapus dummy pad U4, pindahkan outline/holes dari U5, validasi narrow slots, tambahkan fiducial dan panel datum.
9. **Buat fabrication/assembly/test package** dengan stackup, material, toleransi, solder process, marking, AVL, programming, calibration, dan traceability.
10. **Lulus pengujian unit nyata:** ground-bond, hipot, leakage/touch current, temperature rise, overload, short/abnormal operation, surge, EFT, ESD, conducted/radiated EMC, relay endurance, metering accuracy, RF range/coexistence, dan environmental cycling.

## Disposisi

**Disposition: NOT READY FOR PRE-PRODUCTION.**

Source PCB cukup untuk menunjukkan intent dan prototype-level connectivity, tetapi belum menyediakan safety architecture, component control, fabrication definition, test access, traceability, maupun verification evidence yang diperlukan untuk level pre-production. Status hanya boleh dinaikkan setelah blocker topology/safety/edge-clearance diselesaikan dan evidence gate pada Bagian 10 terpenuhi.
