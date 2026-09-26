# SmartPlug — Bank Pertanyaan Kesiapan Pre-Production dan Production

## Status dan batas bukti

Dokumen ini adalah daftar keputusan dan bukti yang harus dikumpulkan. Adanya source desain EasyEDA atau firmware yang dapat dikompilasi tidak dengan sendirinya membuktikan bahwa unit fisik aman, sudah diproduksi, sudah diuji pada listrik PLN, sudah lolos EMC/RF, atau sudah tersertifikasi.

Semua butir di bawah berstatus TBD sampai memiliki jawaban, PIC, tanggal, serta bukti yang dapat ditelusuri. Jawaban “ya”, “aman”, atau “sudah” tanpa bukti belum menutup butir.

## Skema pencatatan jawaban

Gunakan satu record untuk setiap ID:

| Field | Isi minimum |
|---|---|
| ID | ID stabil dari dokumen ini |
| Jawaban/keputusan | Keputusan yang tegas, termasuk angka dan satuan bila relevan |
| Status | Open, In progress, Blocked, Verified, N/A |
| Alasan N/A | Wajib diisi bila tidak berlaku |
| Bukti | Nomor requirement, drawing, commit, laporan uji, sertifikat, foto, atau raw log |
| Konfigurasi | HW revision, BOM revision, FW version, fixture, dan serial unit |
| PIC | Satu orang penanggung jawab |
| Reviewer | Pihak independen yang memverifikasi |
| Target | Tanggal penutupan |
| Gate | Concept, EVT, DVT, PVT, MP, atau post-market |

## Gate blocker teratas

Butir G0 wajib ditutup sebelum desain disebut siap DVT. Keputusan yang mengubah rating, plug/socket, isolasi, relay, radio, atau standar tujuan harus ditutup sebelum schematic/layout/tooling dibekukan.

## 1. Gate 0 — keputusan yang dapat mengubah desain (35)

- [ ] **G0-001** Di negara mana produk akan dipasarkan pada release pertama dan release berikutnya?
- [ ] **G0-002** Apakah target hanya Indonesia atau sejak awal harus memenuhi kebutuhan ekspor tertentu?
- [ ] **G0-003** Apakah produk diklasifikasikan sebagai consumer, commercial, industrial ringan, OEM, atau kombinasi?
- [ ] **G0-004** Siapa pengguna yang diizinkan: orang awam, installer terlatih, teknisi, atau semuanya?
- [ ] **G0-005** Apakah produk hanya untuk indoor kering atau juga lingkungan lembap, semi-outdoor, dan outdoor?
- [ ] **G0-006** Berapa tegangan nominal, rentang tegangan, frekuensi, dan toleransi input yang akan dinyatakan?
- [ ] **G0-007** Berapa arus dan daya kontinu maksimum yang akan dicetak pada label produk?
- [ ] **G0-008** Pada suhu lingkungan maksimum berapa rating arus kontinu tersebut masih berlaku 24/7?
- [ ] **G0-009** Kategori beban apa saja yang secara eksplisit diizinkan?
- [ ] **G0-010** Kategori beban apa saja yang secara eksplisit dilarang di label atau manual?
- [ ] **G0-011** Berapa inrush maksimum, durasi inrush, dan frekuensi switching yang harus ditahan?
- [ ] **G0-012** Sistem plug dan socket negara mana yang dipakai, termasuk versi grounded atau ungrounded?
- [ ] **G0-013** Apakah orientasi plug dapat terbalik sehingga line dan neutral tidak boleh diasumsikan tetap?
- [ ] **G0-014** Apakah protective earth wajib diteruskan dan bagaimana integritasnya dipertahankan?
- [ ] **G0-015** Apakah switching satu pole cukup atau dua pole diwajibkan oleh hazard analysis/standard tujuan?
- [ ] **G0-016** Apa safe state fisik relay selama power-up, reset, brownout, crash, dan firmware update?
- [ ] **G0-017** Apa kebijakan pemulihan relay setelah listrik padam: tetap OFF, restore, atau bergantung use case?
- [ ] **G0-018** Apakah perangkat harus menjalankan fungsi inti tanpa internet dan tanpa cloud?
- [ ] **G0-019** Apakah local control wajib tersedia saat Wi-Fi, router, DNS, NTP, atau cloud gagal?
- [ ] **G0-020** Radio apa yang dipilih: Wi-Fi, BLE, Thread, Zigbee, Matter, atau kombinasi?
- [ ] **G0-021** Apakah radio menggunakan modul tersertifikasi atau desain chip-down?
- [ ] **G0-022** Apakah produk mengukur tegangan, arus, daya, faktor daya, frekuensi, dan energi?
- [ ] **G0-023** Apakah hasil ukur hanya informatif, diklaim akurat, atau digunakan untuk billing?
- [ ] **G0-024** Apakah produk mengklaim overload, overtemperature, surge, short-circuit, atau proteksi lain?
- [ ] **G0-025** Berapa design life dalam tahun, jam aktif, dan jumlah operasi relay?
- [ ] **G0-026** Berapa masa minimum dukungan firmware, keamanan, aplikasi, cloud, dan spare unit?
- [ ] **G0-027** Berapa target volume untuk EVT, DVT, PVT, pilot, dan produksi bulanan?
- [ ] **G0-028** Berapa target COGS, harga jual, margin, serta anggaran lab dan sertifikasi?
- [ ] **G0-029** Siapa compliance owner yang berwenang menetapkan standard dan edisi yang dipakai?
- [ ] **G0-030** Siapa safety owner yang berwenang menghentikan build atau release?
- [ ] **G0-031** Siapa security owner yang berwenang menahan firmware, app, atau cloud release?
- [ ] **G0-032** Apa definisi terukur untuk status prototype-ready?
- [ ] **G0-033** Apa definisi terukur untuk status EVT-ready dan EVT-exit?
- [ ] **G0-034** Apa definisi terukur untuk status pre-production/DVT-ready dan PVT-ready?
- [ ] **G0-035** Apa bukti minimum yang wajib tersedia sebelum status production/MP release diberikan?

## 2. Produk, pasar, dan model bisnis (40)

- [ ] **PRD-001** Masalah pengguna apa yang diselesaikan SmartPlug dan bagaimana keberhasilannya diukur?
- [ ] **PRD-002** Apa tiga use case utama yang wajib bekerja sempurna pada release pertama?
- [ ] **PRD-003** Use case apa yang sengaja tidak didukung agar scope dan risiko terkendali?
- [ ] **PRD-004** Apakah produk untuk rumah, kantor, hotel, toko, fasilitas publik, atau integrator?
- [ ] **PRD-005** Apakah penggunaan tanpa pengawasan dalam jangka panjang diperbolehkan?
- [ ] **PRD-006** Apakah beban safety-critical seperti alat medis, alarm, pompa, heater, atau kulkas diperbolehkan?
- [ ] **PRD-007** Apakah produk boleh mengendalikan beban yang dapat bergerak, panas, atau menimbulkan kebakaran?
- [ ] **PRD-008** Apakah produk ditujukan bagi penyewa, pemilik rumah, pengelola gedung, atau semua segmen?
- [ ] **PRD-009** Apakah ada SKU berbeda untuk pasar, plug, warna, rating, radio, atau fungsi metering?
- [ ] **PRD-010** Apa perbedaan feature dan batas kompatibilitas antar-SKU?
- [ ] **PRD-011** Apakah satu firmware digunakan untuk semua SKU atau image terpisah?
- [ ] **PRD-012** Bagaimana aplikasi mencegah pengguna memilih model/SKU yang salah saat onboarding?
- [ ] **PRD-013** Apakah produk dijual dengan merek sendiri, white-label, atau keduanya?
- [ ] **PRD-014** Siapa legal manufacturer, importer, distributor, dan pihak pemegang sertifikat?
- [ ] **PRD-015** Siapa pemilik source, tooling, cloud account, app-store account, domain, dan signing key?
- [ ] **PRD-016** Berapa target harga jual per pasar dan kanal distribusi?
- [ ] **PRD-017** Berapa target gross margin setelah cloud, support, warranty, retur, dan sertifikasi?
- [ ] **PRD-018** Berapa biaya cloud maksimum per active device per bulan?
- [ ] **PRD-019** Apakah subscription diperlukan dan fungsi apa yang tetap gratis?
- [ ] **PRD-020** Apa konsekuensi bagi pengguna jika subscription dihentikan?
- [ ] **PRD-021** Apakah fungsi dasar tetap tersedia jika perusahaan atau cloud berhenti beroperasi?
- [ ] **PRD-022** Apa janji availability, latency, dan durability data yang akan dipasarkan?
- [ ] **PRD-023** Apakah klaim “energy saving”, “safe”, “surge protected”, atau “accurate” dapat dibuktikan?
- [ ] **PRD-024** Apakah nama, merek, desain, dan protokol bebas dari konflik hak kekayaan intelektual?
- [ ] **PRD-025** Apakah logo Wi-Fi, Bluetooth, Matter, atau asosiasi lain memerlukan listing/licensing?
- [ ] **PRD-026** Apakah product liability insurance diperlukan pada pasar tujuan?
- [ ] **PRD-027** Berapa target tanggal design freeze, certification sample, pilot, dan mass production?
- [ ] **PRD-028** Apakah jadwal memberi waktu untuk redesign setelah pre-compliance failure?
- [ ] **PRD-029** Apa risiko bisnis terbesar jika relay, radio module, metering IC, atau enclosure terlambat?
- [ ] **PRD-030** Apa kriteria make-or-buy untuk firmware, app, cloud, fixture, dan enclosure?
- [ ] **PRD-031** Apakah vendor eksternal wajib menyerahkan source, build instruction, test, dan SBOM?
- [ ] **PRD-032** Bagaimana acceptance terhadap deliverable vendor didefinisikan secara kontraktual?
- [ ] **PRD-033** Apakah ada pelanggan jangkar dengan requirement tambahan yang dapat mengubah desain?
- [ ] **PRD-034** Apakah roadmap berikutnya memerlukan sensor, protocol, atau rating yang perlu disiapkan sekarang?
- [ ] **PRD-035** Apa batas backward compatibility antara generasi hardware, firmware, app, dan cloud?
- [ ] **PRD-036** Apakah data penggunaan akan menjadi bagian model bisnis dan apakah pengguna mengetahuinya?
- [ ] **PRD-037** Apa target tingkat retur, DOA, warranty claim, dan support ticket per seribu unit?
- [ ] **PRD-038** Berapa lama replacement unit dan spare tooling harus tersedia?
- [ ] **PRD-039** Apakah strategi akhir masa jual, akhir dukungan, dan penghentian cloud sudah dibiayai?
- [ ] **PRD-040** Siapa yang menyetujui perubahan requirement setelah design freeze dan bagaimana dampaknya dinilai?

## 3. Pengguna, fungsi, HMI, dan foreseeable misuse (35)

- [ ] **UX-001** Bagaimana pengguna mengetahui keadaan relay yang diminta dan keadaan fisik yang sebenarnya?
- [ ] **UX-002** Apa arti setiap warna, intensitas, dan pola kedip LED?
- [ ] **UX-003** Apakah indikator terlihat pada terang, gelap, sudut miring, dan saat produk tertutup adaptor?
- [ ] **UX-004** Apakah pengguna dapat meredupkan atau mematikan LED tanpa menyembunyikan fault kritis?
- [ ] **UX-005** Apakah pola kedip menghindari frekuensi yang berisiko bagi pengguna sensitif?
- [ ] **UX-006** Apakah bunyi relay dapat diterima di kamar tidur dan lingkungan hening?
- [ ] **UX-007** Apa fungsi short press, long press, multi-press, dan kombinasi tombol?
- [ ] **UX-008** Bagaimana debounce, tombol macet, dan tekan tidak sengaja ditangani?
- [ ] **UX-009** Apakah factory reset memerlukan tindakan fisik yang sulit terpicu tidak sengaja?
- [ ] **UX-010** Apa yang dihapus, dipertahankan, dan ditampilkan setelah factory reset?
- [ ] **UX-011** Apakah tombol lokal selalu dapat mematikan beban meskipun app/cloud bermasalah?
- [ ] **UX-012** Kapan tombol lokal boleh dikunci dan bagaimana emergency override dilakukan?
- [ ] **UX-013** Bagaimana konflik perintah tombol, app, schedule, automation, dan cloud diselesaikan?
- [ ] **UX-014** Apakah sumber dan waktu setiap perubahan relay terlihat oleh owner?
- [ ] **UX-015** Apakah pengguna mendapat peringatan sebelum remote ON pada beban berisiko?
- [ ] **UX-016** Bagaimana aplikasi membedakan command accepted, command executed, dan physical state confirmed?
- [ ] **UX-017** Apakah timer dan schedule berjalan tanpa internet?
- [ ] **UX-018** Apa perilaku schedule ketika waktu perangkat belum valid?
- [ ] **UX-019** Apa perilaku schedule ketika zona waktu atau daylight-saving berubah?
- [ ] **UX-020** Apa perilaku dua schedule yang bertabrakan pada detik yang sama?
- [ ] **UX-021** Apakah automation loop dapat menyebabkan relay chatter dan bagaimana dicegah?
- [ ] **UX-022** Apakah ada minimum ON/OFF duration yang dijelaskan kepada pengguna?
- [ ] **UX-023** Bagaimana pengguna mengetahui overload, overtemperature, sensor fault, dan network fault?
- [ ] **UX-024** Fault mana yang boleh di-reset pengguna dan fault mana yang memerlukan service?
- [ ] **UX-025** Apakah produk aman digunakan oleh anak, lansia, dan pengguna dengan keterbatasan penglihatan?
- [ ] **UX-026** Apakah aplikasi memenuhi accessibility, font scaling, contrast, screen reader, dan localization?
- [ ] **UX-027** Apakah manual Bahasa Indonesia menjelaskan instalasi, rating, warning, dan prohibited load?
- [ ] **UX-028** Apakah warning berada di titik keputusan, bukan hanya di halaman legal yang tersembunyi?
- [ ] **UX-029** Apakah produk boleh ditumpuk dengan adaptor, extension, power strip, atau travel adapter?
- [ ] **UX-030** Apakah produk boleh digunakan pada outlet longgar, rusak, berkarat, atau tanpa earth?
- [ ] **UX-031** Apakah penggunaan dekat air, dapur, kamar mandi, tirai, dan bahan mudah terbakar dijelaskan?
- [ ] **UX-032** Apakah kabel beban dapat menarik produk keluar atau menambah torque pada wall socket?
- [ ] **UX-033** Apakah loss of cloud/account membuat pengguna kehilangan akses mematikan perangkat?
- [ ] **UX-034** Bagaimana ownership transfer memastikan owner lama kehilangan kontrol dan data?
- [ ] **UX-035** Apakah support dapat membantu pengguna tanpa meminta password, key, atau akses berlebihan?

## 4. Rating listrik dan profil beban (55)

- [ ] **ELEC-001** Berapa tegangan input steady-state minimum, nominal, dan maksimum?
- [ ] **ELEC-002** Berapa frekuensi input minimum, nominal, dan maksimum?
- [ ] **ELEC-003** Berapa overvoltage sementara yang harus ditoleransi tanpa unsafe failure?
- [ ] **ELEC-004** Berapa undervoltage dan brownout profile yang harus ditoleransi?
- [ ] **ELEC-005** Berapa arus kontinu maksimum pada ambient maksimum dan orientasi terburuk?
- [ ] **ELEC-006** Berapa derating arus terhadap suhu, ketinggian, enclosure, dan duty cycle?
- [ ] **ELEC-007** Berapa daya kontinu maksimum untuk beban resistif?
- [ ] **ELEC-008** Berapa rating untuk motor atau beban induktif?
- [ ] **ELEC-009** Berapa rating untuk LED driver atau beban kapasitif?
- [ ] **ELEC-010** Berapa rating untuk transformer, SMPS, dan perangkat dengan active PFC?
- [ ] **ELEC-011** Apakah heater, iron, kettle, dan cooking appliance diperbolehkan tanpa pengawasan?
- [ ] **ELEC-012** Apakah compressor, refrigerator, pump, dan air conditioner diperbolehkan?
- [ ] **ELEC-013** Berapa peak inrush maksimum yang dapat ditahan kontak relay?
- [ ] **ELEC-014** Berapa durasi, waveform, dan repetition rate inrush yang dipakai untuk validasi?
- [ ] **ELEC-015** Apakah rating relay vendor mencakup kategori beban dan jumlah cycle yang diklaim?
- [ ] **ELEC-016** Berapa self-consumption saat relay OFF, ON, idle radio, dan transmit maksimum?
- [ ] **ELEC-017** Berapa target standby power dan metode pengukurannya?
- [ ] **ELEC-018** Berapa voltage drop maksimum pada jalur daya pada rated load?
- [ ] **ELEC-019** Berapa contact resistance maksimum saat baru dan setelah aging?
- [ ] **ELEC-020** Bagaimana resistansi sambungan pin, receptacle, relay, shunt, PCB, dan solder dianggarkan?
- [ ] **ELEC-021** Berapa allowable temperature rise setiap bagian current path?
- [ ] **ELEC-022** Apakah current path memerlukan busbar/stamped conductor alih-alih copper trace saja?
- [ ] **ELEC-023** Apakah relay latching atau non-latching dan apa konsekuensi power-loss-nya?
- [ ] **ELEC-024** Apa state kontak relay ketika coil tidak diberi daya?
- [ ] **ELEC-025** Apakah perlu zero-cross switching dan untuk kategori beban apa?
- [ ] **ELEC-026** Apakah zero-cross switching meningkatkan risiko pada transformer atau load tertentu?
- [ ] **ELEC-027** Berapa waktu minimum ON dan OFF untuk melindungi relay dan beban?
- [ ] **ELEC-028** Berapa maksimum operasi relay per menit/jam/hari?
- [ ] **ELEC-029** Bagaimana perintah ON/OFF cepat atau berulang dibatasi?
- [ ] **ELEC-030** Apakah snubber, flyback, atau clamp relay mengubah release time secara signifikan?
- [ ] **ELEC-031** Dapatkah firmware mengetahui coil diperintah tanpa menganggap kontak telah berpindah?
- [ ] **ELEC-032** Adakah feedback independen untuk posisi atau tegangan setelah kontak?
- [ ] **ELEC-033** Dapatkah welded contact terdeteksi dan apa tindakan sesudahnya?
- [ ] **ELEC-034** Dapatkah failure-to-close terdeteksi dan apa yang ditampilkan kepada pengguna?
- [ ] **ELEC-035** Apakah produk mengklaim overload protection atau hanya monitoring?
- [ ] **ELEC-036** Jika overload protection ada, berapa threshold, tolerance, delay, dan hysteresis?
- [ ] **ELEC-037** Apakah overload threshold mempertimbangkan ambient dan thermal history?
- [ ] **ELEC-038** Apakah overload trip latching, auto-retry, atau membutuhkan physical reset?
- [ ] **ELEC-039** Berapa maksimum retry agar tidak menyebabkan fire/relay damage?
- [ ] **ELEC-040** Apakah proteksi overload tetap bekerja jika MCU, sensor, atau firmware gagal?
- [ ] **ELEC-041** Apakah produk mengklaim short-circuit protection?
- [ ] **ELEC-042** Bagaimana koordinasi fuse internal dengan MCB/RCBO instalasi pengguna?
- [ ] **ELEC-043** Berapa prospective short-circuit current yang menjadi design assumption?
- [ ] **ELEC-044** Berapa breaking capacity fuse atau protective element yang dipilih?
- [ ] **ELEC-045** Apakah fuse berada pada conductor yang tepat bila plug tidak terpolarisasi?
- [ ] **ELEC-046** Apakah earth conductor selalu kontinu dan tidak pernah diswitch atau difuse secara salah?
- [ ] **ELEC-047** Apakah backfeed dari UPS, inverter, generator, atau connected equipment mungkin terjadi?
- [ ] **ELEC-048** Bagaimana perangkat bereaksi terhadap phase reversal atau line-neutral reversal?
- [ ] **ELEC-049** Bagaimana perangkat bereaksi terhadap neutral putus atau earth fault yang masuk akal?
- [ ] **ELEC-050** Apakah harmonic dan power factor beban memengaruhi pemanasan atau metering?
- [ ] **ELEC-051** Bagaimana repeated outage dan rapid power cycling memengaruhi relay state?
- [ ] **ELEC-052** Apakah mass reconnect setelah blackout memerlukan random delay/jitter?
- [ ] **ELEC-053** Apakah reconnection delay berbeda untuk compressor atau beban sensitif?
- [ ] **ELEC-054** Apakah perangkat aman bila relay contact bounce terjadi pada peak mains?
- [ ] **ELEC-055** Apakah seluruh rating label diturunkan dari pengujian worst-case, bukan hanya rating komponen?

## 5. Keselamatan listrik, kebakaran, dan thermal (70)

- [ ] **SAFE-001** Apa klasifikasi perlindungan terhadap kejut listrik untuk produk final?
- [ ] **SAFE-002** Apa overvoltage category yang dipakai dalam perhitungan desain?
- [ ] **SAFE-003** Apa pollution degree yang dipakai untuk lingkungan tujuan?
- [ ] **SAFE-004** Apa material group/CTI PCB yang dipakai untuk perhitungan creepage?
- [ ] **SAFE-005** Berapa working voltage aktual pada setiap isolation barrier?
- [ ] **SAFE-006** Berapa transient voltage dan impulse withstand yang diwajibkan?
- [ ] **SAFE-007** Berapa creepage minimum pada setiap pasangan hazardous-to-accessible dan hazardous-to-SELV?
- [ ] **SAFE-008** Berapa clearance minimum pada setiap isolation barrier?
- [ ] **SAFE-009** Apakah nilai creepage/clearance memperhitungkan altitude maksimum?
- [ ] **SAFE-010** Apakah manufacturing tolerance, board warp, component tilt, dan enclosure tolerance dihitung?
- [ ] **SAFE-011** Apakah slot PCB mempunyai width, length, plating, dan routing keepout yang terkendali?
- [ ] **SAFE-012** Apakah solder mask tidak dianggap sebagai insulasi tanpa dasar standard yang sah?
- [ ] **SAFE-013** Apakah silkscreen, adhesive, flux, coating, atau contamination dapat menjembatani barrier?
- [ ] **SAFE-014** Apakah screw, spring, pin, heatsink, light pipe, dan enclosure rib menjaga jarak aman?
- [ ] **SAFE-015** Apakah semua test point dan debug pad diklasifikasikan hazardous atau isolated?
- [ ] **SAFE-016** Apakah tombol, LED, antenna, UART, USB, dan fixture interface aman disentuh?
- [ ] **SAFE-017** Apakah power supply isolated atau non-isolated dan apa implikasinya terhadap seluruh low-voltage domain?
- [ ] **SAFE-018** Jika non-isolated, apakah semua bagian internal diperlakukan sebagai live pada dokumentasi dan fixture?
- [ ] **SAFE-019** Apakah accessible metal dapat menjadi live pada normal use atau single fault?
- [ ] **SAFE-020** Apakah protective earth mempunyai jalur mekanis/elektris yang tidak bergantung pada trace tipis?
- [ ] **SAFE-021** Berapa ground-bond current dan resistance acceptance untuk jalur PE?
- [ ] **SAFE-022** Apakah earth contact make-first/break-last sesuai sistem plug/socket yang dipilih?
- [ ] **SAFE-023** Jika plug non-polarized, apakah risiko single-pole switching telah dianalisis untuk kedua orientasi?
- [ ] **SAFE-024** Apakah keadaan OFF dilarang dipasarkan sebagai electrical isolation bila tidak memenuhi syarat isolator?
- [ ] **SAFE-025** Apakah manual menjelaskan bahwa perangkat tidak menggantikan disconnect/MCB bila benar demikian?
- [ ] **SAFE-026** Apakah relay menyediakan contact gap, dielectric strength, dan endurance yang diwajibkan?
- [ ] **SAFE-027** Apakah relay memiliki approval untuk working voltage, load category, ambient, dan pollution degree?
- [ ] **SAFE-028** Apakah fuse memiliki approval, breaking capacity, I²t, voltage rating, dan derating yang benar?
- [ ] **SAFE-029** Apakah resistor pada jalur mains bertipe fusible/flameproof bila kegagalannya kritis?
- [ ] **SAFE-030** Apakah X/Y capacitor memiliki safety class dan approval yang tepat?
- [ ] **SAFE-031** Apakah MOV/GDT/TVS/snubber dipilih dari surge environment yang dinyatakan?
- [ ] **SAFE-032** Bagaimana MOV gagal setelah aging atau repeated surge?
- [ ] **SAFE-033** Apakah MOV dilindungi thermal cutoff atau containment yang sesuai?
- [ ] **SAFE-034** Apakah protective component tetap aman saat open, short, drift, atau salah populasi?
- [ ] **SAFE-035** Apakah PSU failure mode mencakup output overvoltage, transformer failure, dan control-loop failure?
- [ ] **SAFE-036** Apakah accessible leakage/touch current dihitung dan diuji pada normal dan single fault?
- [ ] **SAFE-037** Apakah insulation resistance diuji setelah humidity/conditioning yang relevan?
- [ ] **SAFE-038** Apakah dielectric strength/hipot level dan test sequence telah ditentukan oleh standard tujuan?
- [ ] **SAFE-039** Apakah discharge time capacitor setelah unplug memenuhi batas aman?
- [ ] **SAFE-040** Apakah stored energy pada capacitor/inductor aman saat enclosure rusak atau servis?
- [ ] **SAFE-041** Di mana hotspot terburuk pada rated load dan worst-case orientation?
- [ ] **SAFE-042** Berapa suhu maksimum pin, socket contact, relay, terminal, shunt, fuse, PSU, dan enclosure?
- [ ] **SAFE-043** Apakah thermal test mencakup high line, low line, max radio activity, dan aged contact resistance?
- [ ] **SAFE-044** Apakah thermal test mencakup outlet tetangga tertutup dan sirkulasi udara buruk?
- [ ] **SAFE-045** Apakah thermal margin memperhitungkan tolerance komponen dan sensor?
- [ ] **SAFE-046** Apakah solder joint high-current tahan creep, fatigue, dan thermal cycling?
- [ ] **SAFE-047** Apakah plastic dekat hotspot mempunyai RTI dan flammability yang memadai?
- [ ] **SAFE-048** Apakah PCB laminate mempunyai flame rating, Tg, CTI, dan lot traceability?
- [ ] **SAFE-049** Apakah enclosure resin exact grade, warna, filler, dan recycled-content dikunci?
- [ ] **SAFE-050** Apakah pergantian warna atau supplier resin memerlukan requalification?
- [ ] **SAFE-051** Apakah enclosure menahan api, molten metal, dan partikel panas agar tidak menyebar?
- [ ] **SAFE-052** Apakah ventilation opening tidak memungkinkan probe, cairan, debu, atau serangga mencapai hazardous part?
- [ ] **SAFE-053** Apakah sensor temperatur berada di lokasi yang benar-benar mewakili risiko utama?
- [ ] **SAFE-054** Berapa accuracy, drift, response time, dan tolerance stack sensor temperatur?
- [ ] **SAFE-055** Apakah open, short, stuck, stale, dan impossible reading sensor dapat dideteksi?
- [ ] **SAFE-056** Apa threshold warning, derating, trip, latch, dan recovery temperatur?
- [ ] **SAFE-057** Apakah pengguna/cloud dilarang mengubah safety threshold tanpa controlled firmware release?
- [ ] **SAFE-058** Apakah thermal cutoff primer independen dari MCU dan firmware?
- [ ] **SAFE-059** Apakah single-fault analysis mencakup short/open setiap safety-critical component?
- [ ] **SAFE-060** Apakah relay drive stuck-high, stuck-low, leakage, dan false pulse telah dianalisis?
- [ ] **SAFE-061** Apakah MCU hang, RAM corrupt, stack overflow, dan task starvation tetap tidak menyebabkan unsafe ON?
- [ ] **SAFE-062** Apakah hardware pull memastikan relay de-energized selama reset dan GPIO high-impedance?
- [ ] **SAFE-063** Apakah brownout detector bereaksi sebelum logic/GPIO menjadi tak terprediksi?
- [ ] **SAFE-064** Apakah flash corruption, invalid config, atau calibration loss menghasilkan safe state?
- [ ] **SAFE-065** Apakah firmware update, rollback, dan recovery tidak menyebabkan unintended energization?
- [ ] **SAFE-066** Apakah EMI-induced reset atau GPIO glitch tidak menghasilkan unsafe switching?
- [ ] **SAFE-067** Apakah watchdog mengawasi safety task, bukan hanya membuat sistem reboot berulang?
- [ ] **SAFE-068** Apakah foreseeable misuse seperti overload, outlet longgar, adaptor bertumpuk, dan tirai masuk risk analysis?
- [ ] **SAFE-069** Apakah seluruh hazard mempunyai risk control, verification method, dan residual-risk acceptance?
- [ ] **SAFE-070** Apakah safety reviewer independen telah menyetujui design evidence sebelum DVT build?

## 6. Plug, socket, enclosure, dan mekanik (45)

- [ ] **MECH-001** Sistem plug/socket dan dimensional standard apa yang menjadi design basis?
- [ ] **MECH-002** Apakah dimensi pin, spacing, chamfer, radius, dan plating dikendalikan drawing bertoleransi?
- [ ] **MECH-003** Apakah socket geometry menerima hanya plug yang memang dibolehkan?
- [ ] **MECH-004** Apakah universal socket dihindari atau dibenarkan dengan safety evaluation khusus?
- [ ] **MECH-005** Berapa insertion force minimum dan maksimum saat baru?
- [ ] **MECH-006** Berapa withdrawal/retention force saat baru dan setelah endurance?
- [ ] **MECH-007** Berapa mating cycle target dan kondisi load selama cycle test?
- [ ] **MECH-008** Apakah contact material, spring temper, plating material, dan thickness dikunci?
- [ ] **MECH-009** Bagaimana oxidation, fretting, humidity, dan contamination memengaruhi contact resistance?
- [ ] **MECH-010** Apakah shutter diperlukan dan tahan single-pin insertion/child misuse?
- [ ] **MECH-011** Apakah earth contact engagement dan continuity konsisten pada tolerance extremes?
- [ ] **MECH-012** Apakah berat produk memberi bending moment aman pada wall socket?
- [ ] **MECH-013** Apakah produk tetap tertahan ketika kabel beban ditarik dari berbagai arah?
- [ ] **MECH-014** Apakah produk menutup outlet/switch tetangga pada konfigurasi umum?
- [ ] **MECH-015** Apakah dua SmartPlug dapat dipasang berdampingan tanpa overheating atau interference?
- [ ] **MECH-016** Apakah tombol dapat tertekan oleh dinding, adaptor, kabel, atau kemasan?
- [ ] **MECH-017** Apakah LED/light pipe tetap memiliki isolation spacing pada tolerance terburuk?
- [ ] **MECH-018** Apakah enclosure memakai screw, snap-fit, adhesive, heat-stake, atau ultrasonic weld?
- [ ] **MECH-019** Apakah metode penutupan memberi tamper resistance dan konsistensi produksi yang dibutuhkan?
- [ ] **MECH-020** Jika memakai screw, berapa torque, thread engagement, dan locking method?
- [ ] **MECH-021** Jika ultrasonic weld, berapa process window energy, pressure, amplitude, dan collapse?
- [ ] **MECH-022** Bagaimana weld quality diperiksa tanpa destructive test pada semua unit?
- [ ] **MECH-023** Apakah enclosure yang dibuka untuk rework boleh ditutup kembali dan dijual?
- [ ] **MECH-024** Apakah PCB, busbar, relay, dan contact carrier tertahan terhadap shock dan vibration?
- [ ] **MECH-025** Apakah tolerance stack-up dapat menekan PCB atau solder joint saat enclosure ditutup?
- [ ] **MECH-026** Apakah board warp atau component tilt dapat mengurangi clearance ke enclosure/metal?
- [ ] **MECH-027** Berapa drop height, orientation, surface, dan jumlah drop yang disyaratkan?
- [ ] **MECH-028** Berapa impact, compression, torsion, dan plug-pin bending requirement?
- [ ] **MECH-029** Apakah unit tetap electrically safe setelah drop meskipun masih berfungsi?
- [ ] **MECH-030** Apakah crack internal atau displaced part dapat dideteksi pada EOL/visual inspection?
- [ ] **MECH-031** Apa ingress-protection target dan untuk kondisi plug terpasang/tidak terpasang yang mana?
- [ ] **MECH-032** Apakah gasket, membrane, button seal, dan weld mempertahankan IP setelah aging?
- [ ] **MECH-033** Apakah condensate mempunyai drainage path yang tidak menjembatani live parts?
- [ ] **MECH-034** Apakah enclosure tahan humidity, household cleaner, oil, sweat, dan UV yang relevan?
- [ ] **MECH-035** Apakah plastic aging memengaruhi retention, shutter, flammability, dan impact strength?
- [ ] **MECH-036** Apakah pin terlindung dari deformasi selama packing, drop carton, dan transportasi?
- [ ] **MECH-037** Apakah label adhesive tahan panas, lembap, abrasi, cleaner, dan waktu pakai?
- [ ] **MECH-038** Apakah rating, model, serial, warning, dan regulatory marks terlihat saat diperlukan?
- [ ] **MECH-039** Apakah QR/serial tetap terbaca sepanjang design life?
- [ ] **MECH-040** Apakah enclosure mencegah user menyentuh hazardous parts dengan standard test probe?
- [ ] **MECH-041** Apakah flame/heat vent path menjauh dari wall, curtain, dan user?
- [ ] **MECH-042** Apakah acoustic click, buzzing, dan coil vibration memenuhi target kualitas?
- [ ] **MECH-043** Apakah cosmetic limit sample ditetapkan untuk gap, sink, weld line, warna, dan scratch?
- [ ] **MECH-044** Apakah tooling cavity dapat ditelusuri ke serial/lot untuk analisis defect?
- [ ] **MECH-045** Apakah drawing mekanik final mengikat exact material, finish, tolerance, dan revision tooling?

## 7. PCB, hardware detail, DFM, dan maintainability (45)

- [ ] **PCB-001** Apa stack-up, layer count, copper weight, finish, Tg, CTI, dan flame rating PCB?
- [ ] **PCB-002** Apakah fabricator dan exact material system dikunci atau memiliki approved equivalents?
- [ ] **PCB-003** Apakah current-carrying trace dihitung terhadap copper tolerance dan temperature rise?
- [ ] **PCB-004** Apakah neck-down, thermal relief, pad, via, jumper, dan solder menjadi bottleneck arus?
- [ ] **PCB-005** Jika via membawa arus, berapa jumlah, drill, plating minimum, dan failure margin?
- [ ] **PCB-006** Apakah jalur arus tinggi menggunakan busbar/stamped metal bila PCB saja tidak cukup?
- [ ] **PCB-007** Apakah creepage diukur pada Gerber final untuk semua layer dan assembly condition?
- [ ] **PCB-008** Apakah routing/copper keepout dijaga pada kedua sisi isolation slot?
- [ ] **PCB-009** Apakah milling slot minimum width/length dan tolerance masuk fab note?
- [ ] **PCB-010** Apakah V-score, mouse-bite, tab, rail, dan depaneling tidak merusak insulation distance?
- [ ] **PCB-011** Apakah tooling hole, fiducial, test coupon, dan board edge aman terhadap mains copper?
- [ ] **PCB-012** Apakah solder mask expansion dan silkscreen tidak mengurangi safety spacing?
- [ ] **PCB-013** Apakah component courtyard mencegah part miring atau alternate package mendekati barrier?
- [ ] **PCB-014** Apakah polarized/safety-critical part mempunyai keying dan unmistakable assembly marking?
- [ ] **PCB-015** Apakah exact relay footprint cocok dengan manufacturer drawing, bukan generic footprint?
- [ ] **PCB-016** Apakah alternate relay footprint, pinout, insulation, coil, contact, dan thermal benar-benar equivalent?
- [ ] **PCB-017** Apakah fuse holder/clip/contact mampu membawa arus dan bertahan terhadap thermal cycling?
- [ ] **PCB-018** Apakah shunt/current-sense memakai Kelvin routing dan controlled thermal environment?
- [ ] **PCB-019** Apakah divider, ADC, reference, dan metrology ground terlindung dari switching noise?
- [ ] **PCB-020** Apakah relay coil, SMPS, antenna, crystal, dan metrology mempunyai return-path yang benar?
- [ ] **PCB-021** Apakah decoupling ditempatkan sesuai reference design pada setiap IC?
- [ ] **PCB-022** Apakah bulk capacitance cukup untuk radio TX burst dan relay actuation pada low line?
- [ ] **PCB-023** Apakah brownout/ripple diuji pada component tolerance dan capacitor aging?
- [ ] **PCB-024** Apakah crystal/clock layout memenuhi vendor constraint dan EMC target?
- [ ] **PCB-025** Apakah antenna keepout mencakup semua layer, enclosure, busbar, relay, socket, dan wiring?
- [ ] **PCB-026** Apakah antenna matching network menyediakan tuning option dan controlled components?
- [ ] **PCB-027** Apakah RF test connector/pad tidak merusak production RF atau creepage?
- [ ] **PCB-028** Apakah surge/high-dVdt current loop dibuat kecil dan jauh dari logic/metrology?
- [ ] **PCB-029** Apakah snubber/MOV/fuse placement meminimalkan fault-energy propagation?
- [ ] **PCB-030** Apakah thermal vias/heatspreading tidak membentuk unintended hazardous coupling?
- [ ] **PCB-031** Apakah test point coverage dapat mengukur power rails, programming, sensor, relay, dan radio?
- [ ] **PCB-032** Apakah hazardous test points diberi spacing, guard, dan fixture-specific protection?
- [ ] **PCB-033** Apakah debug connector dihapus, depopulated, atau dikunci pada mass production?
- [ ] **PCB-034** Apakah conformal coating diperlukan berdasarkan humidity/contamination analysis?
- [ ] **PCB-035** Jika coating dipakai, apakah material, thickness, keepout, cure, inspection, dan rework dikontrol?
- [ ] **PCB-036** Apakah flux residue/cleaning process menjaga insulation resistance dan relay compatibility?
- [ ] **PCB-037** Apakah stencil aperture dan reflow profile memadai untuk thermal-mass/high-current joints?
- [ ] **PCB-038** Apakah AOI/X-ray dapat memeriksa seluruh solder joint kritis?
- [ ] **PCB-039** Apakah hand-solder/rework pada mains/current path memiliki process limit dan operator qualification?
- [ ] **PCB-040** Apakah BOM menyebut exact manufacturer part number, approval, tolerance, dan lifecycle status?
- [ ] **PCB-041** Apakah safety-critical components ditandai no-substitution dalam BOM/ERP?
- [ ] **PCB-042** Apakah schematic, PCB, BOM, fab, assembly, pick-place, dan firmware pinmap satu revision?
- [ ] **PCB-043** Apakah ECO dapat ditelusuri ke requirement, risk, test impact, dan production lot?
- [ ] **PCB-044** Apakah desain memungkinkan diagnosis aman dengan power rendah sebelum menggunakan mains?
- [ ] **PCB-045** Apakah independent layout review telah menutup electrical, safety, RF, thermal, DFM, dan DFT findings?

## 8. Metering, sensor, dan kalibrasi (35)

- [ ] **MET-001** Parameter apa yang diukur: Vrms, Irms, active power, apparent power, PF, frequency, dan energy?
- [ ] **MET-002** Berapa range, resolution, update rate, dan latency setiap parameter?
- [ ] **MET-003** Berapa accuracy claim setiap parameter pada seluruh range?
- [ ] **MET-004** Apakah accuracy dinyatakan pada unity dan non-unity power factor?
- [ ] **MET-005** Apakah waveform non-sinusoidal dan nonlinear load masuk specification?
- [ ] **MET-006** Berapa minimum current/power yang masih menghasilkan data valid?
- [ ] **MET-007** Bagaimana no-load noise, offset, dan phantom energy dicegah?
- [ ] **MET-008** Bagaimana ADC clipping, sensor saturation, dan out-of-range dideteksi?
- [ ] **MET-009** Bagaimana phase error antara kanal voltage/current dikalibrasi?
- [ ] **MET-010** Bagaimana suhu memengaruhi shunt, divider, ADC, reference, oscillator, dan algorithm?
- [ ] **MET-011** Apakah calibration per-unit, per-lot, atau hanya nominal design coefficient?
- [ ] **MET-012** Berapa titik voltage, current, PF, frequency, dan temperature yang dikalibrasi?
- [ ] **MET-013** Apakah titik kalibrasi mewakili low, typical, high, dan claimed operating range?
- [ ] **MET-014** Apa uncertainty budget reference instrument, fixture, source, contact, dan algorithm?
- [ ] **MET-015** Apakah reference instrument memiliki kalibrasi tertelusur dan belum kedaluwarsa?
- [ ] **MET-016** Apakah measurement-system analysis/GR&R dilakukan pada stasiun kalibrasi?
- [ ] **MET-017** Berapa calibration takt time dan pengaruhnya pada kapasitas line?
- [ ] **MET-018** Di mana coefficient kalibrasi disimpan dan format/version-nya apa?
- [ ] **MET-019** Apakah coefficient redundant, atomic, ber-CRC/MAC, dan tahan power loss?
- [ ] **MET-020** Apakah unauthorized local/cloud recalibration dicegah?
- [ ] **MET-021** Apa perilaku jika calibration data hilang, corrupt, kosong, atau salah model?
- [ ] **MET-022** Apakah data invalid dilarang ditampilkan sebagai angka valid?
- [ ] **MET-023** Bagaimana accumulated energy bertahan terhadap reset dan flash wear?
- [ ] **MET-024** Berapa interval persist energy dan maksimum data yang hilang saat power cut?
- [ ] **MET-025** Apakah counter rollover/overflow diuji hingga melampaui design life?
- [ ] **MET-026** Apakah timebase error memengaruhi energy accumulation dan schedule?
- [ ] **MET-027** Apakah metering IC checksum/status/fault dibaca dan ditindaklanjuti?
- [ ] **MET-028** Apakah current/voltage plausibility dibandingkan dengan relay state bila hardware memungkinkan?
- [ ] **MET-029** Apakah UI membedakan measured, estimated, unavailable, stale, dan fault?
- [ ] **MET-030** Berapa long-term drift allowance dan bagaimana dibuktikan?
- [ ] **MET-031** Apakah recalibration lapangan diperlukan dan siapa yang berwenang?
- [ ] **MET-032** Apakah claim billing/legal metrology secara eksplisit dilarang bila belum tersertifikasi?
- [ ] **MET-033** Apakah meter reference design vendor telah divalidasi pada layout dan component aktual?
- [ ] **MET-034** Apakah calibration result, coefficient, fixture, instrument, operator, dan serial tersimpan di MES?
- [ ] **MET-035** Apakah unit gagal kalibrasi dikarantina tanpa menghapus hasil gagal melalui retest?

## 9. RF, antenna, provisioning, dan konektivitas lokal (45)

- [ ] **RF-001** Protokol radio final apa dan apa alasan teknis, regulasi, biaya, serta lifecycle-nya?
- [ ] **RF-002** Apakah radio memakai certified module atau chip-down design?
- [ ] **RF-003** Jika memakai modul, kondisi host/antenna/enclosure apa yang dapat membatalkan approval?
- [ ] **RF-004** Apakah exact module hardware/firmware revision dikunci dalam BOM?
- [ ] **RF-005** Antenna type, gain, polarization, pattern, feed, dan matching apa yang dipakai?
- [ ] **RF-006** Apakah antenna sama dengan konfigurasi yang akan disertifikasi?
- [ ] **RF-007** Apakah country code, channel mask, bandwidth, dan transmit power dikunci per pasar?
- [ ] **RF-008** Berapa conducted power dan EIRP maksimum pada setiap mode?
- [ ] **RF-009** Apakah production RF calibration diperlukan dan bagaimana coefficient dilindungi?
- [ ] **RF-010** Apa test limits untuk frequency error, power, spectral mask, harmonics, dan spurious emissions?
- [ ] **RF-011** Bagaimana antenna performance berubah ketika relay OFF versus ON?
- [ ] **RF-012** Bagaimana antenna performance berubah pada no-load versus rated load?
- [ ] **RF-013** Bagaimana dinding, metal socket, busbar, enclosure, tangan, dan kabel beban memengaruhi RF?
- [ ] **RF-014** Apa target sensitivity, packet error rate, throughput, latency, TRP, dan TIS?
- [ ] **RF-015** Apa target jarak di free space, rumah padat, dan lingkungan RF congested?
- [ ] **RF-016** Apakah target diuji pada router vendor/chipset/firmware yang beragam?
- [ ] **RF-017** Apakah 2.4 GHz legacy rates/modes yang relevan didukung?
- [ ] **RF-018** Apakah WPA2, WPA3, transition mode, hidden SSID, unicode, dan credential panjang diuji?
- [ ] **RF-019** Apakah enterprise Wi-Fi, captive portal, mesh, repeater, dan band steering didukung atau dilarang?
- [ ] **RF-020** Apa yang terjadi saat SSID/password/router diganti?
- [ ] **RF-021** Bagaimana provisioning dilakukan jika ponsel tidak memiliki internet?
- [ ] **RF-022** Apakah provisioning memerlukan proof of possession unit fisik?
- [ ] **RF-023** Apakah commissioning advertisement mengungkap identifier/data berlebihan?
- [ ] **RF-024** Apakah provisioning mode memiliki timeout, retry limit, dan visible indication?
- [ ] **RF-025** Apakah attacker di sekitar dapat mengambil alih unit yang belum dipair?
- [ ] **RF-026** Apakah BLE/radio commissioning dimatikan atau dibatasi setelah onboarding?
- [ ] **RF-027** Bagaimana coexistence Wi-Fi/BLE/Thread/Zigbee diuji?
- [ ] **RF-028** Apakah simultaneous radio activity mengganggu metering, relay, atau watchdog?
- [ ] **RF-029** Bagaimana perangkat pulih dari AP reboot, DHCP lease change, IP conflict, dan roaming?
- [ ] **RF-030** Bagaimana perangkat menangani DNS failure, NTP failure, gateway loss, dan captive redirect?
- [ ] **RF-031** Apakah reconnect memakai exponential backoff dan jitter?
- [ ] **RF-032** Apakah retry storm dapat membebani router, device, atau cloud?
- [ ] **RF-033** Apakah network task dapat menyebabkan starvation atau memory exhaustion?
- [ ] **RF-034** Apakah malformed packet dan RF jamming tetap tidak mengganggu safety supervisor?
- [ ] **RF-035** Apakah local-control discovery dan command diautentikasi?
- [ ] **RF-036** Apakah device tetap bisa dimatikan secara lokal ketika radio macet?
- [ ] **RF-037** Apakah MAC address/OUI, Bluetooth identifiers, dan protocol IDs dialokasikan legal dan unik?
- [ ] **RF-038** Apakah Matter/CSA certification diperlukan untuk feature yang diklaim?
- [ ] **RF-039** Apakah Wi-Fi Alliance/Bluetooth SIG listing diperlukan untuk logo/interoperability claim?
- [ ] **RF-040** Apakah RF test firmware terpisah dari production firmware dan ditandatangani berbeda?
- [ ] **RF-041** Apakah RF test mode dapat diaktifkan pada unit customer?
- [ ] **RF-042** Bagaimana unit gagal RF test dikarantina dan identifier-nya direvoke?
- [ ] **RF-043** Apakah antenna tuning dilakukan pada production-intent enclosure, contact, dan PCB?
- [ ] **RF-044** Apakah regulatory test memakai worst-case channel, bandwidth, power, traffic, dan peripheral state?
- [ ] **RF-045** Apakah perubahan antenna, enclosure, module, layout, atau firmware RF memicu certification impact review?

## 10. Arsitektur, runtime, dan kualitas firmware (70)

- [ ] **FW-001** MCU/module final apa dan apakah pin/resource map telah diverifikasi terhadap hardware?
- [ ] **FW-002** Berapa flash, RAM, NVS, stack, peripheral, dan security capability yang tersedia?
- [ ] **FW-003** SDK/RTOS versi apa dan sampai kapan vendor mendukungnya?
- [ ] **FW-004** Apakah compiler, linker, SDK, packages, dan scripts dipin exact version?
- [ ] **FW-005** Apakah build dapat direproduksi dari clean environment?
- [ ] **FW-006** Apakah artifact mengikat commit, dependency lock, toolchain, build flags, signer, dan timestamp?
- [ ] **FW-007** Apakah HAL/BSP memisahkan pinmap dan hardware revision dari product logic?
- [ ] **FW-008** Apakah unknown/unsupported hardware revision berhenti pada safe state?
- [ ] **FW-009** Apakah hanya satu Safety Supervisor/Actuator Arbiter yang boleh menulis relay GPIO?
- [ ] **FW-010** Apakah network, cloud, schedule, UI, dan factory service dilarang menulis relay langsung?
- [ ] **FW-011** Apakah semua relay request melewati authorization, interlock, fault, dan rate-limit policy?
- [ ] **FW-012** Apakah product state machine terdokumentasi dengan state dan transition eksplisit?
- [ ] **FW-013** Apa state untuk boot-safe, commissioning, OFF, turning-on, ON, turning-off, fault, update, dan recovery?
- [ ] **FW-014** Apakah illegal transition ditolak dan dicatat?
- [ ] **FW-015** Bagaimana perintah lokal, cloud, schedule, automation, dan safety diprioritaskan?
- [ ] **FW-016** Apakah safety trip selalu mengalahkan perintah ON dari sumber mana pun?
- [ ] **FW-017** Apakah command memiliki ID, source, authorization, sequence/time, TTL, dan desired action?
- [ ] **FW-018** Apakah duplicate command diproses idempotent?
- [ ] **FW-019** Apakah reordered, delayed, atau stale command tidak dapat menyalakan beban?
- [ ] **FW-020** Apakah reported state membedakan requested, commanded, inferred, dan physically confirmed state?
- [ ] **FW-021** Apa boot sequence lengkap sebelum relay boleh aktif?
- [ ] **FW-022** Apakah relay de-energized sebelum clock, watchdog, storage, dan critical self-test sehat?
- [ ] **FW-023** Apa restore-state decision setelah cold boot, warm reset, watchdog, brownout, dan OTA?
- [ ] **FW-024** Apakah reset reason tersimpan secara tahan power loss?
- [ ] **FW-025** Bagaimana brownout dibedakan dari normal reset dan external reset?
- [ ] **FW-026** Apakah repeated reset/boot loop masuk recovery atau fault-safe mode?
- [ ] **FW-027** Berapa retry boot maksimum sebelum perangkat berhenti melakukan tindakan berisiko?
- [ ] **FW-028** Apa hardware watchdog window dan task-health criteria?
- [ ] **FW-029** Apakah satu task yang hidup dapat menyembunyikan safety task yang mati?
- [ ] **FW-030** Apakah watchdog feed bergantung pada health vote seluruh task kritis?
- [ ] **FW-031** Apakah stack overflow, heap corruption, allocation failure, dan memory leak terdeteksi?
- [ ] **FW-032** Apakah safety path menghindari dynamic allocation setelah initialization?
- [ ] **FW-033** Apakah ISR bounded, non-blocking, dan tidak menjalankan network/storage operation?
- [ ] **FW-034** Apakah task priority dan worst-case execution time dianalisis?
- [ ] **FW-035** Apakah priority inversion dan deadlock diuji?
- [ ] **FW-036** Apakah queue overflow mempunyai deterministic drop/reject/fault policy?
- [ ] **FW-037** Apakah event storm tidak menghabiskan RAM atau CPU safety path?
- [ ] **FW-038** Apakah sensor sample memiliki timestamp, validity, range, dan freshness?
- [ ] **FW-039** Bagaimana outlier, stuck-at, open, short, stale, dan impossible value ditangani?
- [ ] **FW-040** Apakah filtering latency kompatibel dengan protection response time?
- [ ] **FW-041** Apakah threshold memakai units eksplisit, tolerance, hysteresis, debounce, dan persistence?
- [ ] **FW-042** Apakah integer overflow, underflow, divide-by-zero, NaN, rounding, dan unit conversion diuji?
- [ ] **FW-043** Apakah fault dibedakan menjadi transient, recoverable, latched, permanent, dan diagnostic-only?
- [ ] **FW-044** Fault mana yang memerlukan physical action sebelum relay dapat ON lagi?
- [ ] **FW-045** Fault mana yang tidak boleh di-clear dari cloud atau app?
- [ ] **FW-046** Apakah local OFF tetap berfungsi ketika cloud/network/storage task gagal?
- [ ] **FW-047** Bagaimana button debounce, long-press, stuck-button, bounce, dan EMI pulse ditangani?
- [ ] **FW-048** Apakah factory reset tahan accidental trigger selama normal use dan power cycling?
- [ ] **FW-049** Apakah reset menghapus owner, token, schedule, Wi-Fi, key tertentu, log, dan energy sesuai policy?
- [ ] **FW-050** Apakah UTC, monotonic clock, timezone, dan RTC dipisahkan jelas?
- [ ] **FW-051** Apa perilaku schedule jika wall clock belum sinkron?
- [ ] **FW-052** Apa perilaku timer/schedule jika waktu melompat maju atau mundur?
- [ ] **FW-053** Apakah timer wraparound dan long-duration timeout diuji?
- [ ] **FW-054** Apakah config storage transactional, redundant, versioned, dan wear-leveled?
- [ ] **FW-055** Apakah config mempunyai CRC/MAC dan safe defaults?
- [ ] **FW-056** Apakah migration config antar-versi bersifat atomic dan rollback-safe?
- [ ] **FW-057** Apa perilaku bila satu copy atau seluruh copy config corrupt?
- [ ] **FW-058** Apakah calibration dan identity tidak tertimpa oleh factory/user config update?
- [ ] **FW-059** Apakah log bounded, rate-limited, wear-aware, dan mempunyai severity?
- [ ] **FW-060** Apakah log menyensor password, token, key, SSID, dan data pribadi?
- [ ] **FW-061** Apakah crash dump berguna tetapi tidak membocorkan secrets?
- [ ] **FW-062** Apakah debug, development, factory, staging, dan production build dibedakan secara kuat?
- [ ] **FW-063** Apakah production build dapat dipastikan tidak memuat test credentials atau unsafe commands?
- [ ] **FW-064** Apakah feature flags versioned, authorized, audited, dan tidak dapat mengubah safety limit?
- [ ] **FW-065** Apakah setiap release menghasilkan firmware SBOM dan license inventory?
- [ ] **FW-066** Apakah coding standard, review checklist, static analysis, dan warning-as-error diterapkan?
- [ ] **FW-067** Apakah unit, integration, HIL, regression, fuzz, dan long-soak test menjadi release gate?
- [ ] **FW-068** Siapa yang menyetujui firmware release dan siapa yang memegang veto safety/security?
- [ ] **FW-069** Apakah emergency rollback/release procedure diuji, bukan hanya didokumentasikan?
- [ ] **FW-070** Apakah release notes mengikat known issues, compatibility, test evidence, SBOM, dan artifact hash?

## 11. Secure boot, OTA, device identity, dan cybersecurity (45)

- [ ] **SEC-001** Apakah threat model mencakup device, radio, LAN, mobile app, cloud, factory, dan supply chain?
- [ ] **SEC-002** Aset apa yang dilindungi: keselamatan, relay control, keys, account, firmware, dan usage data?
- [ ] **SEC-003** Siapa attacker yang diasumsikan dan akses fisik/network apa yang mereka miliki?
- [ ] **SEC-004** Apa dampak maksimum unauthorized ON, OFF, schedule, update, atau data access?
- [ ] **SEC-005** Apakah secure boot didukung hardware dan diwajibkan pada production unit?
- [ ] **SEC-006** Apakah semua bootloader, application, radio blob, dan recovery image diautentikasi?
- [ ] **SEC-007** Apakah production image wajib signed dan unsigned image selalu ditolak?
- [ ] **SEC-008** Di mana firmware-signing key disimpan dan siapa yang dapat menggunakannya?
- [ ] **SEC-009** Apakah key ceremony, dual control, audit, backup, rotation, revocation, dan recovery terdokumentasi?
- [ ] **SEC-010** Apakah bootloader dapat diperbarui dan bagaimana brick/rollback risk dikendalikan?
- [ ] **SEC-011** Apakah OTA memakai A/B partition atau recovery image yang power-fail safe?
- [ ] **SEC-012** Apa health criteria dan observation window sebelum image baru dikonfirmasi?
- [ ] **SEC-013** Apakah power cut diuji pada setiap tahap download, verify, write, switch, boot, dan confirm?
- [ ] **SEC-014** Apakah manifest mengikat model, HW revision, region, version, size, hash, signer, dan dependencies?
- [ ] **SEC-015** Apakah wrong-model, wrong-region, corrupt, truncated, replayed, dan expired image ditolak?
- [ ] **SEC-016** Apakah anti-rollback diperlukan dan bagaimana emergency rollback tetap aman?
- [ ] **SEC-017** Apakah update tidak dapat menyebabkan relay berubah tanpa actuator/safety policy?
- [ ] **SEC-018** Apakah phased rollout, canary, pause, kill switch, dan rollback tersedia?
- [ ] **SEC-019** Apakah satu unit mempunyai device identity dan credential unik?
- [ ] **SEC-020** Apakah universal/default password dan shared private key dilarang?
- [ ] **SEC-021** Bagaimana key/credential dibuat dengan entropy yang memadai?
- [ ] **SEC-022** Bagaimana key diinjeksikan tanpa tampil di log, MES screen, atau operator file?
- [ ] **SEC-023** Apakah private key non-exportable atau dilindungi eFuse/secure element sesuai threat model?
- [ ] **SEC-024** Apakah flash encryption diperlukan dan apakah key-nya per-unit?
- [ ] **SEC-025** Apakah JTAG, SWD, ROM downloader, UART shell, dan test commands dikunci pada MP?
- [ ] **SEC-026** Apakah authorized recovery/forensics masih mungkin tanpa universal backdoor?
- [ ] **SEC-027** Apakah device-to-cloud menggunakan authenticated encryption dan mutual authentication bila diperlukan?
- [ ] **SEC-028** Bagaimana certificate validation bekerja sebelum wall clock valid?
- [ ] **SEC-029** Bagaimana certificate/key rotation dan expiry ditangani pada unit yang lama offline?
- [ ] **SEC-030** Bagaimana compromised device, owner, app token, dan certificate direvoke secara granular?
- [ ] **SEC-031** Apakah local API, discovery, commissioning, dan diagnostic interface authenticated/authorized?
- [ ] **SEC-032** Apakah pairing memakai proof of possession dan mencegah replay/takeover?
- [ ] **SEC-033** Apakah ownership transfer mencabut semua akses owner lama?
- [ ] **SEC-034** Apakah command dilindungi dari replay, reordering, tampering, dan resource exhaustion?
- [ ] **SEC-035** Apakah rate limiting tersedia pada login, pairing, command, reset, API, dan update?
- [ ] **SEC-036** Apakah malformed packet, parser fuzz, dan denial-of-service tidak menonaktifkan safety function?
- [ ] **SEC-037** Apakah least privilege dan isolation diterapkan antar-service/task yang relevan?
- [ ] **SEC-038** Apakah production, staging, development, dan factory secrets benar-benar terpisah?
- [ ] **SEC-039** Apakah SAST, SCA, secret scan, fuzzing, dependency monitoring, dan pentest menjadi gate?
- [ ] **SEC-040** Apakah SBOM mencakup bootloader, firmware, SDK, binary blob, app, cloud, dan build tools?
- [ ] **SEC-041** Apakah vulnerability disclosure/CVD policy dan security contact dipublikasikan?
- [ ] **SEC-042** Apa SLA triage, mitigation, patch, notification, dan CVE handling sepanjang support period?
- [ ] **SEC-043** Apakah incident-response tabletop dan emergency-signing/rollout pernah diuji?
- [ ] **SEC-044** Apakah security update support period dinyatakan transparan sebelum pembelian?
- [ ] **SEC-045** Apakah independent security assessment menutup semua critical/high finding sebelum MP?

## 12. Cloud, aplikasi, API, account, dan privasi (50)

- [ ] **CLOUD-001** Apakah cloud benar-benar diperlukan untuk setiap fungsi dan fungsi mana yang local-first?
- [ ] **CLOUD-002** Fungsi apa yang tetap tersedia ketika internet/cloud berhenti sementara atau permanen?
- [ ] **CLOUD-003** Apa cloud provider, region, data residency, dan disaster-recovery region?
- [ ] **CLOUD-004** Apa availability, durability, RTO, RPO, dan command-latency target?
- [ ] **CLOUD-005** Bagaimana desired, accepted, executed, reported, dan physical state dibedakan?
- [ ] **CLOUD-006** Apakah message ordering, duplication, delay, dan loss ditangani eksplisit?
- [ ] **CLOUD-007** Bagaimana stale offline queue dicegah menyalakan beban setelah reconnect?
- [ ] **CLOUD-008** Apakah command mempunyai TTL, idempotency key, owner context, dan audit record?
- [ ] **CLOUD-009** Bagaimana conflict antara beberapa pengguna, automation, schedule, dan local button diselesaikan?
- [ ] **CLOUD-010** Apa behavior jika broker, database, cache, DNS, NTP, push, atau identity provider gagal?
- [ ] **CLOUD-011** Apakah multi-region failover dan failback diuji dengan real traffic pattern?
- [ ] **CLOUD-012** Apakah backup restore diuji dan tidak menghidupkan revoked token/owner?
- [ ] **CLOUD-013** Berapa kapasitas total device, active connection, message rate, dan automation puncak?
- [ ] **CLOUD-014** Apakah blackout mass-reconnect/load spike disimulasikan?
- [ ] **CLOUD-015** Apakah retry/backoff/jitter mencegah thundering herd?
- [ ] **CLOUD-016** Apakah tenant isolation dan authorization diuji untuk horizontal/vertical access control?
- [ ] **CLOUD-017** Apakah operator/support access memakai RBAC, MFA, approval, session log, dan review?
- [ ] **CLOUD-018** Apakah support dilarang menyalakan beban tanpa consent dan audit yang sesuai?
- [ ] **CLOUD-019** Apakah API versioned dan backward-compatible sepanjang firmware support window?
- [ ] **CLOUD-020** Bagaimana old firmware diamankan ketika API/protocol berevolusi?
- [ ] **CLOUD-021** Apakah mobile app menyimpan token/key di OS secure storage?
- [ ] **CLOUD-022** Apakah account login, recovery, MFA, session revocation, dan device approval aman?
- [ ] **CLOUD-023** Apakah password-reset/ownership-transfer flow tahan social engineering?
- [ ] **CLOUD-024** Apa role owner, admin, member, guest, installer, dan support?
- [ ] **CLOUD-025** Apakah invitation expiry, revocation, dan least privilege diterapkan?
- [ ] **CLOUD-026** Apakah onboarding diuji pada permission denied, Bluetooth/location off, dan OS terbaru?
- [ ] **CLOUD-027** Apakah app mendukung accessibility, localization, font scaling, screen reader, dan low-connectivity?
- [ ] **CLOUD-028** Apakah app menampilkan firmware version, support status, connectivity, dan actual fault?
- [ ] **CLOUD-029** Apakah automation mempunyai loop detection, rate limit, priority, dan safety interlock?
- [ ] **CLOUD-030** Apakah notification menyertakan device, action, source, actual time, dan result?
- [ ] **CLOUD-031** Apakah audit history perubahan owner, relay, schedule, update, dan security event tersedia?
- [ ] **CLOUD-032** Data apa yang dikumpulkan device, app, cloud, analytics, dan support tools?
- [ ] **CLOUD-033** Apakah energy-use timeline dapat mengungkap occupancy atau kebiasaan rumah?
- [ ] **CLOUD-034** Apa tujuan dan dasar pemrosesan setiap field data?
- [ ] **CLOUD-035** Apakah data yang tidak diperlukan dapat dihapus dari arsitektur sebelum launch?
- [ ] **CLOUD-036** Apakah consent granular dan dapat ditarik tanpa mematikan fungsi inti yang tak terkait?
- [ ] **CLOUD-037** Berapa retention setiap jenis telemetry, log, account, support, dan backup data?
- [ ] **CLOUD-038** Siapa controller, processor, subprocessor, dan penerima data lintas negara?
- [ ] **CLOUD-039** Bagaimana pengguna mengakses, mengekspor, mengoreksi, dan menghapus data?
- [ ] **CLOUD-040** Berapa SLA deletion pada primary store, replica, analytics, log, dan backup?
- [ ] **CLOUD-041** Apakah log menyimpan SSID, IP, device identifier, location, atau usage pattern?
- [ ] **CLOUD-042** Apakah analytics/advertising SDK benar-benar diperlukan dan disclosed?
- [ ] **CLOUD-043** Apakah data dijual, diprofilkan, atau dibagikan untuk tujuan di luar fungsi produk?
- [ ] **CLOUD-044** Apakah privacy notice tersedia dalam bahasa dan format yang mudah dipahami pengguna?
- [ ] **CLOUD-045** Bagaimana shared-household privacy dan akses antaranggota dikelola?
- [ ] **CLOUD-046** Apakah old owner berhenti menerima telemetry/notifikasi segera setelah transfer?
- [ ] **CLOUD-047** Apakah breach detection, containment, legal notification, dan customer communication diuji?
- [ ] **CLOUD-048** Apakah infrastructure-as-code, peer review, secret management, dan change approval diterapkan?
- [ ] **CLOUD-049** Apa exit plan jika cloud vendor, push provider, app store, atau identity provider berhenti?
- [ ] **CLOUD-050** Apakah data-protection/privacy impact assessment disetujui sebelum production release?

## 13. Manufaktur, DFM/DFT, kalibrasi line, dan traceability (55)

- [ ] **MFG-001** Siapa CM/EMS/factory dan standar sistem mutu apa yang mereka jalankan?
- [ ] **MFG-002** Apakah line mempunyai pengalaman serta kontrol untuk produk mains dan high-pot?
- [ ] **MFG-003** Apa target takt time, first-pass yield, retest rate, dan line capacity?
- [ ] **MFG-004** Apa CTQ/safety-critical characteristics dan bagaimana masing-masing dikontrol?
- [ ] **MFG-005** Komponen apa yang ditandai safety-critical dan no-substitution?
- [ ] **MFG-006** Apakah approved manufacturer/vendor list dikunci di PLM/ERP?
- [ ] **MFG-007** Apakah purchasing dilarang mengganti MPN berdasarkan nilai/footprint saja?
- [ ] **MFG-008** Bagaimana counterfeit, reclaimed, dan unauthorized components dicegah?
- [ ] **MFG-009** Apa incoming inspection untuk relay, fuse, MOV, contact, PCB, resin, dan radio module?
- [ ] **MFG-010** Apakah CoC, safety approval, date code, lot code, dan source disimpan?
- [ ] **MFG-011** Apakah setiap serial dapat ditelusuri ke critical component lot dan tooling cavity?
- [ ] **MFG-012** Apakah MSL, bake, humidity, shelf life, dan floor life dikontrol?
- [ ] **MFG-013** Apakah solder paste, stencil, printer, SPI, reflow, alloy, dan profile dikunci?
- [ ] **MFG-014** Apakah high-thermal-mass/current parts mencapai solder quality pada process window?
- [ ] **MFG-015** Apakah AOI dapat melihat polarity, wrong part, solder bridge, dan critical joints?
- [ ] **MFG-016** Apakah X-ray diperlukan untuk hidden or high-current joints?
- [ ] **MFG-017** Apakah hand solder/rework pada mains/current path dibatasi dan terdokumentasi?
- [ ] **MFG-018** Berapa maksimum rework cycle untuk PCB, contact, relay, dan enclosure?
- [ ] **MFG-019** Apakah cleaned/uncleaned process divalidasi terhadap insulation resistance dan relay?
- [ ] **MFG-020** Apakah ICT coverage dan escaped-fault analysis tersedia?
- [ ] **MFG-021** Apakah FCT menguji power rails, GPIO, sensor, metering, storage, radio, button, dan LED?
- [ ] **MFG-022** Apakah FCT menguji actual relay contact path, bukan hanya coil click?
- [ ] **MFG-023** Apakah relay ON/OFF voltage drop atau continuity diperiksa pada kedua state?
- [ ] **MFG-024** Apakah PE continuity/ground bond diuji sesuai control plan bila earth tersedia?
- [ ] **MFG-025** Apakah hipot/dielectric-strength testing scope dan sampling ditetapkan oleh standard/control plan?
- [ ] **MFG-026** Apakah insulation resistance dan leakage/touch current diuji sesuai kebutuhan?
- [ ] **MFG-027** Apakah safety-test fixture memiliki guard, interlock, emergency stop, dan automatic discharge?
- [ ] **MFG-028** Apakah operator mains/high-pot terlatih, berwenang, dan diaudit?
- [ ] **MFG-029** Apakah fixture melakukan self-test untuk probe, relay, cable, interlock, dan reference failure?
- [ ] **MFG-030** Apakah fixture calibration dan preventive-maintenance schedule terdokumentasi?
- [ ] **MFG-031** Apakah test software, limits, sequence, dan firmware version dikendalikan revision?
- [ ] **MFG-032** Apakah operator tidak dapat mengubah limit atau skip test tanpa authorization?
- [ ] **MFG-033** Apakah semua hasil awal, gagal, retest, override, dan abort disimpan?
- [ ] **MFG-034** Apakah rerun test tidak menghapus failure signature pertama?
- [ ] **MFG-035** Apakah fail unit otomatis dikarantina dan diblokir dari packing/shipment?
- [ ] **MFG-036** Apa alur MRB, root cause, rework, retest, concession, dan scrap?
- [ ] **MFG-037** Apakah concession mengikat quantity, serial range, owner, expiry, dan risk approval?
- [ ] **MFG-038** Apakah golden-pass, known-bad, dan boundary units tersedia untuk fixture verification?
- [ ] **MFG-039** Apakah measurement-system analysis/GR&R dilakukan untuk seluruh CTQ measurement?
- [ ] **MFG-040** Apakah process capability/SPC dipantau untuk contact resistance, calibration, weld, dan torque?
- [ ] **MFG-041** Apakah serial number unik, collision-proof, readable, dan immutable?
- [ ] **MFG-042** Apakah serial mengikat HW, BOM, FW, MAC, keys, calibration, test, lot, date, dan cavity?
- [ ] **MFG-043** Bagaimana MAC, device ID, certificates, dan keys dialokasikan tanpa duplikasi?
- [ ] **MFG-044** Apakah key injection memakai secure station/HSM dan operator tidak melihat secret?
- [ ] **MFG-045** Apakah factory/CM dapat membuat clone atau overbuild di luar authorized work order?
- [ ] **MFG-046** Apakah failed/unused identity dan credential dapat direvoke?
- [ ] **MFG-047** Apakah production firmware, test firmware, dan factory image dibedakan cryptographically?
- [ ] **MFG-048** Apakah factory mode dikunci sebelum unit meninggalkan test station?
- [ ] **MFG-049** Apakah EOL memverifikasi secure boot, debug lock, flash protection, dan final FW hash?
- [ ] **MFG-050** Apakah onboarding smoke test tidak mengikat unit ke account/operator pribadi?
- [ ] **MFG-051** Apakah enclosure weld/screw/torque/gap diperiksa dengan method yang capable?
- [ ] **MFG-052** Apakah final inspection mencakup pin, crack, contamination, label, QR, dan cosmetic limit?
- [ ] **MFG-053** Apakah packaging scan menjamin unit, serial, label, manual, dan carton configuration cocok?
- [ ] **MFG-054** Apakah retained sample diambil dari tiap lot/shift/cavity dengan retention period jelas?
- [ ] **MFG-055** Apakah line audit, supplier audit, layered process audit, dan yield review dijadwalkan?

## 14. Verification, reliability, quality, dan field validation (45)

- [ ] **VER-001** Apakah setiap requirement mempunyai verification method: test, inspection, analysis, atau review?
- [ ] **VER-002** Apakah DVP&R mengikat requirement, risk, sample, setup, pass criteria, owner, dan evidence?
- [ ] **VER-003** Apakah pass/fail criteria ditetapkan sebelum test dimulai?
- [ ] **VER-004** Apakah test memakai production-intent PCB, BOM, enclosure, tooling, firmware, dan process?
- [ ] **VER-005** Apakah sample size diturunkan dari reliability/confidence target dan hazard severity?
- [ ] **VER-006** Apakah sampel berasal dari beberapa lot, build, line, shift, cavity, dan critical-component lot?
- [ ] **VER-007** Apa design life dalam tahun, powered hours, load hours, dan relay cycles?
- [ ] **VER-008** Apa reliability target, confidence level, allowable failure, dan statistical rationale?
- [ ] **VER-009** Apakah DFMEA/PFMEA/FTA failure modes mempunyai verification coverage?
- [ ] **VER-010** Apakah relay endurance diuji dengan resistive load pada rating yang diklaim?
- [ ] **VER-011** Apakah relay endurance diuji dengan inductive, capacitive, motor, LED, dan inrush loads yang diklaim?
- [ ] **VER-012** Apakah contact resistance, temperature rise, bounce, weld, dan failure-to-close dipantau selama endurance?
- [ ] **VER-013** Apakah rapid power cycling dan arbitrary power cut diuji pada seluruh operating state?
- [ ] **VER-014** Apakah brownout sweep, dips, interruptions, high/low line, dan repeated outage diuji?
- [ ] **VER-015** Apakah surge, EFT/burst, ESD, radiated RF, dan conducted RF immunity diuji?
- [ ] **VER-016** Apakah behavior criteria selama/setelah EMC disturbance didefinisikan untuk setiap function?
- [ ] **VER-017** Apakah EMC event dilarang menyebabkan unintended ON, corrupted calibration, atau credential loss?
- [ ] **VER-018** Apakah conducted/radiated emissions diuji pada relay OFF, ON, switching, radio TX, dan worst load?
- [ ] **VER-019** Apakah harmonic current dan voltage fluctuation/flicker applicability serta test ditetapkan?
- [ ] **VER-020** Apakah temperature-rise test dilakukan pada rated load, high ambient, dan worst orientation?
- [ ] **VER-021** Apakah low-temperature start, high-temperature operation, dan thermal cycling dilakukan?
- [ ] **VER-022** Apakah damp heat/humidity, condensation risk, dan insulation resistance diuji?
- [ ] **VER-023** Apakah storage high/low temperature dan transport profile diuji?
- [ ] **VER-024** Apakah altitude effect pada clearance, cooling, dan RF dinilai?
- [ ] **VER-025** Apakah drop, impact, compression, vibration, torsion, insertion, withdrawal, dan pin bending diuji?
- [ ] **VER-026** Apakah chemical, cleaner, UV, label abrasion, dan plastic-aging tests sesuai intended environment?
- [ ] **VER-027** Apakah long-soak firmware mencakup network churn, cloud outage, commands, metering, dan storage writes?
- [ ] **VER-028** Berapa durasi soak dan apa acceptance untuk reset, memory, CPU, latency, serta state consistency?
- [ ] **VER-029** Apakah flash/NVS endurance dihitung dan diuji melebihi lifetime write budget?
- [ ] **VER-030** Apakah timer, energy counter, sequence, timestamp, dan version rollover diuji?
- [ ] **VER-031** Apakah OTA diuji dengan power cut acak, corrupt image, wrong model, downgrade, dan full storage?
- [ ] **VER-032** Apakah protocol/config/parser fuzzing dilakukan dengan sanitizer pada host dan target test?
- [ ] **VER-033** Apakah race, queue full, task stall, low-memory, low-storage, dan watchdog faults diinjeksi?
- [ ] **VER-034** Apakah router/AP compatibility matrix mewakili produk populer dan kondisi buruk?
- [ ] **VER-035** Apakah congested 2.4 GHz, weak signal, packet loss, and reconnect storm diuji?
- [ ] **VER-036** Apakah cloud outage, region failover, mass reconnect, dan stale-command scenarios diuji end-to-end?
- [ ] **VER-037** Apakah factory reset, account recovery, owner transfer, data deletion, dan device revocation diuji end-to-end?
- [ ] **VER-038** Apakah destructive safety tests memakai sample terpisah dengan serial dan configuration traceability?
- [ ] **VER-039** Apakah setiap test menyimpan raw data, waveform, photo, environment, instrument, dan calibration status?
- [ ] **VER-040** Apakah failure pertama dipertahankan sebelum retest, teardown, atau corrective action?
- [ ] **VER-041** Apakah setiap failure mempunyai root cause, CAPA, regression test, dan affected-configuration analysis?
- [ ] **VER-042** Apakah independent accredited/competent lab melakukan pre-compliance dan formal compliance tests?
- [ ] **VER-043** Apakah certification samples identik dengan released hardware, antenna, BOM, firmware, label, dan manual?
- [ ] **VER-044** Apakah field pilot mewakili outlet, router, load, suhu, pengguna, dan lokasi yang beragam?
- [ ] **VER-045** Apakah pilot exit menuntut closure seluruh safety/security critical issue dan review field-return data?

## 15. Compliance, supply chain, dokumentasi, dan lifecycle (45)

- [ ] **GOV-001** Apakah compliance engineer/LSPro/lab telah menetapkan klasifikasi resmi produk final?
- [ ] **GOV-002** Apakah produk termasuk standardisasi ketenagalistrikan dan skema sertifikasi yang berlaku di Indonesia?
- [ ] **GOV-003** Apakah keseluruhan smart plug atau hanya bagian plug/socket berada dalam lingkup SNI wajib?
- [ ] **GOV-004** Apakah SNI IEC 60884-1:2014 menjadi basis yang diterima untuk konfigurasi final?
- [ ] **GOV-005** Apakah IEC 60884-3-2:2026 dipakai sebagai design basis dan apakah telah diadopsi/diterima pasar tujuan?
- [ ] **GOV-006** Standard, edisi, amendment, national deviation, dan transition date mana yang diwajibkan?
- [ ] **GOV-007** Apakah standard EMC yang benar ditetapkan berdasarkan product classification, bukan tebakan?
- [ ] **GOV-008** Apakah creepage, clearance, fire, relay, IP, dan metering standard candidates dipetakan clause-by-clause?
- [ ] **GOV-009** Apakah LSPro dan laboratorium yang dipilih memiliki scope serta akreditasi relevan?
- [ ] **GOV-010** Apakah SPPT-SNI, tanda SNI, tanda keselamatan, family grouping, atau surveillance berlaku?
- [ ] **GOV-011** Apakah radio final memerlukan sertifikasi alat/perangkat telekomunikasi Komdigi?
- [ ] **GOV-012** Apakah Wi-Fi diklasifikasikan RLAN dan BLE/Thread/Zigbee diklasifikasikan SRD sesuai konfigurasi?
- [ ] **GOV-013** Apakah band, channel, bandwidth, EIRP, antenna, dan placement memenuhi izin kelas terbaru?
- [ ] **GOV-014** Apakah label sertifikat, PLG ID, QR, warning, dan informasi pemegang sertifikat sudah dipetakan?
- [ ] **GOV-015** Apakah certified radio module mengurangi pengujian atau produk akhir tetap memerlukan sertifikasi?
- [ ] **GOV-016** Apakah aplikasi/cloud termasuk lingkup PSE privat atau kewajiban digital lain di Indonesia?
- [ ] **GOV-017** Apakah pengolahan data memenuhi UU Pelindungan Data Pribadi dan aturan turunannya yang berlaku?
- [ ] **GOV-018** Apakah kewajiban label/manual Bahasa Indonesia, perlindungan konsumen, garansi, dan recall dipenuhi?
- [ ] **GOV-019** Apakah OSS, import, customs/HS code, NPB, TKDN, dan izin usaha yang relevan dipetakan?
- [ ] **GOV-020** Jika ekspor EU, apakah RED, LVD/EMC, RoHS, WEEE, Ecodesign, dan CRA berlaku?
- [ ] **GOV-021** Jika ekspor UK, apakah UKCA dan PSTI consumer-connectable product requirements berlaku?
- [ ] **GOV-022** Jika ekspor AS/Kanada, apakah FCC/ISED serta NRTL/UL/CSA evaluation diperlukan?
- [ ] **GOV-023** Apakah legal review membedakan mandatory regulation, voluntary standard, dan customer requirement?
- [ ] **GOV-024** Apakah compliance matrix mencatat clause, evidence, owner, status, deviation, dan expiry?
- [ ] **GOV-025** Apakah perubahan HW/FW/antenna/material/supplier pascasertifikasi mempunyai impact-assessment process?
- [ ] **GOV-026** Apakah setiap critical component mempunyai approval valid untuk exact MPN, factory, dan rating?
- [ ] **GOV-027** Apakah critical components dibeli dari authorized source dengan anti-counterfeit controls?
- [ ] **GOV-028** Apakah lead time, MOQ, allocation, geographic risk, dan EOL status tiap long-lead part diketahui?
- [ ] **GOV-029** Apakah approved second source telah divalidasi sebelum kebutuhan substitusi muncul?
- [ ] **GOV-030** Apakah alternate part dinilai electrical, thermal, RF, safety, firmware, mechanical, dan certification equivalent?
- [ ] **GOV-031** Apakah supplier wajib memberi PCN untuk material, process, factory, firmware, tooling, dan sub-tier changes?
- [ ] **GOV-032** Apakah supplier lot dapat dipetakan ke affected serial range untuk targeted containment/recall?
- [ ] **GOV-033** Apakah firmware SDK, binary blobs, cloud APIs, dan licensed protocols didukung sepanjang design life?
- [ ] **GOV-034** Apakah source escrow, migration, atau exit plan tersedia untuk dependency yang kritis?
- [ ] **GOV-035** Apakah tooling ownership, custody, maintenance, backup data, dan disaster recovery jelas?
- [ ] **GOV-036** Apakah PRD, architecture, schematic, PCB, BOM, mechanical, firmware, dan test package punya controlled baseline?
- [ ] **GOV-037** Apakah hazard analysis, DFMEA, PFMEA, FTA, threat model, privacy assessment, dan SBOM tersedia?
- [ ] **GOV-038** Apakah user manual, label, carton, installation, support, RMA, recall, dan EOL documents tersedia?
- [ ] **GOV-039** Apakah release configuration index mengikat exact artifact, revision, certificate, report, dan label?
- [ ] **GOV-040** Apa warranty period, acceptable DOA/field-return rate, dan spare/replacement policy?
- [ ] **GOV-041** Apakah RMA/FRACAS menghubungkan serial, failure analysis, root cause, CAPA, dan affected population?
- [ ] **GOV-042** Siapa yang dapat menetapkan stop-ship, field warning, remote mitigation, dan recall?
- [ ] **GOV-043** Apakah end-of-sale, end-of-support, end-of-cloud, last-update, dan disclosure timeline dipublikasikan?
- [ ] **GOV-044** Apakah fungsi lokal yang aman tetap tersedia setelah cloud/security support berakhir?
- [ ] **GOV-045** Apakah post-market safety, cybersecurity, supplier, certificate, field failure, dan regulatory surveillance memiliki PIC?

## Ringkasan jumlah

Dokumen ini berisi tepat **715 pertanyaan** dengan ID stabil:

| Kelompok | Rentang ID | Jumlah |
|---|---:|---:|
| Gate 0 | G0-001–G0-035 | 35 |
| Produk/pasar | PRD-001–PRD-040 | 40 |
| Pengguna/HMI | UX-001–UX-035 | 35 |
| Listrik/beban | ELEC-001–ELEC-055 | 55 |
| Safety/thermal | SAFE-001–SAFE-070 | 70 |
| Mekanik | MECH-001–MECH-045 | 45 |
| PCB/hardware | PCB-001–PCB-045 | 45 |
| Metering | MET-001–MET-035 | 35 |
| RF | RF-001–RF-045 | 45 |
| Firmware | FW-001–FW-070 | 70 |
| Security/OTA | SEC-001–SEC-045 | 45 |
| Cloud/app/privacy | CLOUD-001–CLOUD-050 | 50 |
| Manufacturing | MFG-001–MFG-055 | 55 |
| Verification | VER-001–VER-045 | 45 |
| Governance/lifecycle | GOV-001–GOV-045 | 45 |
| **Total** |  | **715** |

# Arsitektur firmware konservatif

Arsitektur ini adalah baseline keselamatan, bukan keputusan bahwa hardware yang ada telah mendukung seluruh fungsi. Pin, polarity, sensor, feedback contact, MCU capability, dan isolation boundary wajib dibuktikan dari source desain serta unit fisik.

```text
ROM / immutable root of trust
└── Secure bootloader + recovery + signed A/B update
    └── BSP / HAL / verified hardware revision and pin map
        ├── Reset, brownout, clock, hardware watchdog
        ├── Relay driver
        ├── Metering and temperature drivers
        ├── Button and indicator drivers
        ├── Transactional NVS and protected key storage
        └── Radio driver
            ├── Safety Supervisor / Actuator Arbiter
            ├── Product State Machine
            ├── Metering and Fault Monitor
            ├── Local UI
            ├── Connectivity and Commissioning
            ├── Device Identity and Security Services
            ├── Local/Cloud Protocol Adapter
            ├── Schedule and Automation
            ├── Bounded Diagnostics and Logging
            └── Authenticated Factory Test Service
```

## Prinsip arsitektur

1. Fuse, insulation, creepage/clearance, thermal cutoff, fire containment, dan protection primer tidak boleh hanya bergantung pada firmware.
2. Hanya Safety Supervisor/Actuator Arbiter yang boleh mengubah relay output.
3. Network, cloud, schedule, app, local UI, dan factory service hanya mengajukan request; supervisor menentukan apakah transisi diizinkan.
4. Relay diberi hardware pull menuju keadaan de-energized selama reset/high-impedance. State fisik aman final tetap ditentukan hazard analysis.
5. Baseline konservatif adalah tidak mengaktifkan relay sebelum image, HW revision, storage, watchdog, dan self-test kritis valid.
6. Restore-last-state tidak boleh menjadi default implisit; keputusan harus eksplisit per use case, reset reason, dan fault state.
7. State machine minimal mencakup BOOT_SAFE, COMMISSIONING, OFF, TURNING_ON, ON, TURNING_OFF, FAULT_LATCHED, UPDATE_SAFE, dan RECOVERY.
8. Safety trip selalu mengalahkan remote/local ON; fault kritis tidak dapat di-clear sembarang command cloud.
9. Command membawa ID, source, authorization context, sequence/time, TTL, dan hasil eksekusi; command lama atau duplikat tidak boleh menyebabkan tindakan baru.
10. Sensor data selalu membawa timestamp, validity, freshness, dan diagnostic state.
11. Configuration storage memakai transaksi/redundansi, schema version, CRC/MAC, wear management, safe default, dan power-loss recovery.
12. Monotonic time dipakai untuk timeout; UTC/timezone dipakai untuk jadwal; schedule tidak berjalan dengan waktu invalid.
13. Watchdog memakai health vote task kritis sehingga satu task tidak dapat menyembunyikan task safety yang mati.
14. Safety path bersifat bounded dan tidak bergantung pada blocking network call, unbounded queue, atau allocation tak terkendali.
15. Factory mode membutuhkan kondisi fisik/authenticated fixture, memiliki timeout, diaudit, dan wajib terkunci sebelum shipping.
16. Production build mengunci debug/ROM download sesuai threat model dan tidak memuat test credential/unsafe command.
17. OTA wajib signed, terikat model/HW/region, power-fail safe, mempunyai health-confirmation, rollback/recovery, phased rollout, dan kill switch.
18. Per-unit identity/credential unik dibuat melalui proses pabrik yang aman dan dapat direvoke secara granular.
19. Log dibatasi ukuran/rate, tidak memuat secret, dan menyimpan reset/fault/update/security evidence yang dibutuhkan.
20. Setiap firmware release dapat direproduksi serta mengikat source commit, toolchain, SBOM, artifact hash, signer, dan test evidence.

# Test plan konservatif

## Phase 0 — requirement, compliance, dan risk closure

- Tutup seluruh Gate 0.
- Buat intended/prohibited use, load matrix, state machine, hazard analysis, DFMEA, PFMEA, FTA, threat model, privacy assessment, dan compliance matrix.
- Verifikasi pinmap, relay polarity, sensor path, isolation boundary, dan HW revision dari schematic/PCB sebelum driver dibuat.
- Tentukan pass criteria dan evidence format sebelum test.
- Dilarang menyebut production-ready pada fase ini.

## Phase 1 — host-side firmware verification tanpa mains

- Unit/property test state machine, legal/illegal transitions, priority, threshold, hysteresis, debounce, timer, overflow, dan conversion.
- Simulasikan power loss pada setiap operasi NVS, config migration, energy persist, dan OTA metadata.
- Fuzz parser, command, commissioning, config, manifest OTA, dan local/cloud protocol.
- Uji duplicate, reordered, delayed, expired, replayed, malformed, dan unauthorized command.
- Uji time jump, invalid clock, timezone, DST, schedule collision, timer wrap, dan long-duration counter.
- Uji queue full, task stall, memory/storage failure, corrupt config, stale sensor, dan watchdog health voting.
- Jalankan static analysis, compiler warnings-as-errors, sanitizer, SCA, secret scan, license check, dan SBOM generation.

## Phase 2 — target MCU dengan sumber aman, tanpa switching mains

- Validasi boot/reset/brownout/watchdog serta default GPIO menggunakan dummy/electrically safe fixture.
- Validasi relay driver logic tanpa menganggap bunyi coil sebagai contact proof.
- Emulasikan metering/temperature sensors dan inject open, short, stuck, stale, noise, dan out-of-range.
- Uji button, LED, provisioning, radio, transactional storage, reset reason, dan factory mode.
- Uji secure boot, debug lock, per-unit identity, key injection, certificate rotation, dan revocation pada sampel khusus.
- Uji A/B OTA/recovery dengan pemutusan daya acak dan image salah/corrupt.
- Lakukan long-soak serta monitoring RAM, CPU, stack, queue, NVS wear, dan latency.

## Phase 3 — isolated HIL dan engineering pre-compliance

- Fixture mengemulasikan sensor/contact feedback serta memverifikasi actuator arbitration dan seluruh state transition.
- Uji anti-chatter, conflicting commands, fault latch/recovery, stale command, cloud/AP/DNS/NTP outage, dan reconnect storm.
- Tune antenna pada production-intent PCB/enclosure/contact hardware.
- Lakukan preliminary conducted/radiated emissions scan sebelum layout/tooling freeze.
- Validasi factory fixture, limits, golden/known-bad units, data capture, dan failure quarantine.

## Phase 4 — EVT mains engineering

Hanya personel kompeten dengan fixture berpelindung, interlock, current limiting/isolation yang sesuai, emergency stop, prosedur discharge, dan alat ukur terkalibrasi.

- Sweep rated voltage/frequency, high/low line, brownout, dip, interruption, repeated outage, dan rapid cycling.
- Gunakan load matrix resistive, inductive, motor, transformer, LED/capacitive, SMPS, dan high-inrush hanya sesuai intended use.
- Ukur voltage drop, contact resistance, current path, hotspot, temperature rise, leakage, dan power consumption.
- Validasi relay bounce/chatter/endurance awal, weld/failure-to-close detection bila hardware mendukung.
- Validasi metering accuracy, PF/waveform behavior, no-load, saturation, calibration, dan thermal drift.
- Lakukan planned abnormal/single-fault dan surge/EFT/ESD pre-compliance tests dengan review keselamatan.
- Hasil EVT bukan sertifikat dan bukan bukti mass-production readiness.

## Phase 5 — DVT, qualification, dan formal compliance

- Gunakan unit production-intent dari beberapa lot/cavity dengan exact BOM, enclosure, antenna, dan release-candidate firmware.
- Jalankan formal electrical-safety, temperature-rise, abnormal-operation, mechanical, endurance, environmental, EMC, dan RF tests.
- Jalankan relay endurance untuk setiap load category yang diklaim sambil memantau contact resistance/thermal/failure mode.
- Jalankan full firmware/app/cloud regression, long soak, OTA power-cut campaign, fuzzing, dan independent security assessment.
- Formal certification memakai konfigurasi hardware, firmware mode, antenna, label, manual, dan factory yang sama dengan release.
- Tidak boleh ada unresolved safety-critical atau security-critical defect.

## Phase 6 — PVT dan production-line validation

- Gunakan production tooling, operator, supplier lots, fixture, MES, firmware signer, key injection, label, dan packaging aktual.
- Verifikasi seluruh 100-percent tests dan sampling tests yang ditetapkan control plan/standard.
- Lakukan GR&R serta process capability untuk CTQ dan audit false-pass/false-fail/retest behavior.
- Buktikan traceability dari serial ke BOM/FW/MAC/key/calibration/tests/lot/cavity dan reverse lookup saat containment.
- Ambil retained samples dan jalankan reliability audit samples dari PVT lot.
- PVT exit membutuhkan yield stabil, fixture capable, quarantine/MRB efektif, serta closure seluruh critical issue.

## Phase 7 — controlled field pilot dan MP monitoring

- Pilot mewakili tipe outlet, router, RF congestion, suhu, jenis beban, household, dan lokasi yang diizinkan.
- Pantau thermal, reboot, relay failures, connectivity, latency, OTA, pairing, metering, cloud cost, serta support burden.
- Jangan mengubah safety limits melalui remote experimentation.
- Setiap failure mengikat serial, HW/FW/BOM, load, environment, raw logs, teardown, root cause, dan CAPA.
- MP memerlukan ongoing yield/SPC, retained-sample audits, field FRACAS, vulnerability monitoring, certificate surveillance, dan supplier PCN review.

## Evidence minimum setiap test

- Requirement/risk/test ID dan pass criteria.
- Serial number, HW/BOM/FW/toolchain revision, serta exact configuration.
- Fixture, instrument, calibration status, operator, reviewer, dan tanggal.
- Environment, voltage, frequency, load, network, cloud/app version, dan preconditioning.
- Raw logs, waveforms, photos, thermal images, scripts, serta processed results.
- Initial result, failure signature, retest history, deviation, disposition, root cause, dan CAPA link.

# Gate summary

| Gate | Minimum exit evidence |
|---|---|
| Concept | Gate 0, intended use, rating, architecture, risk, dan compliance route disetujui |
| EVT exit | Risiko desain utama diuji; current path/thermal/safety architecture layak; tidak ada blocker desain terbuka |
| DVT/pre-production exit | Konfigurasi production-intent lulus DVP&R dengan hasil dan bukti safety/EMC/RF/reliability/security yang diwajibkan telah selesai; firmware RC dan supplier controls terkunci |
| PVT exit | Production line/fixture/MES/key injection/traceability capable; yield stabil; packaging/label/manual final |
| Production/MP release | Sertifikat wajib tersedia dan cocok dengan released configuration; no critical blockers; pilot/CAPA/support/recall siap |
| Post-market | FRACAS, vulnerability response, OTA, supplier PCN, certificate surveillance, warranty, EOL, dan recall berjalan |

# Referensi resmi untuk menentukan applicability

Daftar ini adalah kandidat domain yang harus dikonfirmasi oleh compliance owner, LSPro/laboratorium, dan penasihat hukum untuk konfigurasi serta pasar final. Mencantumkan referensi tidak berarti produk sudah patuh atau bahwa semua referensi otomatis berlaku.

## Indonesia

- BSN — SNI IEC 60884-1:2014, katalog berstatus berlaku: https://pesta.bsn.go.id/produk/detail/9916-sniiec60884-12014
- JDIH ESDM — Permen ESDM Nomor 7 Tahun 2021 tentang standardisasi ketenagalistrikan dan tanda SNI/keselamatan: https://jdih.esdm.go.id/dokumen/view?id=2190
- JDIH Komdigi — Permen Kominfo Nomor 3 Tahun 2024 tentang sertifikasi alat/perangkat telekomunikasi: https://jdih.komdigi.go.id/produk_hukum/view/id/892/t/peraturan%2Bmenteri%2Bkomunikasi%2Bdan%2Binformatika%2Bnomor%2B3%2Btahun%2B2024
- JDIH Komdigi — Permen Komdigi Nomor 2 Tahun 2025, perubahan aturan spektrum berdasarkan izin kelas: https://jdih.komdigi.go.id/produk_hukum/unduh/id/949/t/peraturan%2Bmenteri%2Bkomunikasi%2Bdan%2Bdigital%2Bnomor%2B2%2Btahun%2B2025
- JDIH Komdigi — Kepmen Komdigi Nomor 12 Tahun 2025 untuk RLAN: https://jdih.komdigi.go.id/produk_hukum/view/id/951/t/keputusan%2Bmenteri%2Bkomunikasi%2Bdan%2Bdigital%2Bnomor%2B12%2Btahun%2B2025
- JDIH/BPK — UU Nomor 27 Tahun 2022 tentang Pelindungan Data Pribadi: https://peraturan.bpk.go.id/Home/Download/224884/UU%20Nomor%2027%20Tahun%202022.pdf

## Standard/product-security candidates

- IEC — IEC 60884-1:2022, plugs and socket-outlets general requirements: https://webstore.iec.ch/en/publication/34175
- IEC — IEC 60884-3-2:2026, accessories incorporating electronic components; safety dan EMC: https://webstore.iec.ch/en/publication/71932
- ETSI — EN 303 645 V3.1.3, consumer IoT cybersecurity baseline: https://www.etsi.org/deliver/etsi_en/303600_303699/303645/03.01.03_60/en_303645v030103p.pdf
- NIST — IR 8425, Profile of the IoT Core Baseline for Consumer IoT Products: https://csrc.nist.gov/pubs/ir/8425/final

## Conditional export-market references

- EU — Radio Equipment Directive 2014/53/EU: https://eur-lex.europa.eu/eli/dir/2014/53/oj/eng
- EU — Cyber Resilience Act, Regulation (EU) 2024/2847: https://eur-lex.europa.eu/eli/reg/2024/2847/oj/eng
- UK Government — consumer connectable product security/PSTI guidance: https://www.gov.uk/guidance/regulations-consumer-connectable-product-security
- US eCFR — FCC 47 CFR Part 15: https://www.ecfr.gov/current/title-47/chapter-I/subchapter-A/part-15

# Pernyataan readiness

Tanpa unit production-intent, hasil thermal/load/endurance/abnormal-operation, laporan EMC/RF/safety lab, firmware release evidence, PVT line evidence, serta sertifikat yang diwajibkan, status yang tepat adalah desain atau pre-production dalam pengembangan—bukan production-ready.
