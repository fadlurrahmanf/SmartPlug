# SmartPlug

## Datasheet board

Dokumen sumber untuk memahami board dan wiring terdapat pada
[`datasheet.md`](datasheet.md), [`design.md`](design.md), serta audit sumber
di folder [`evidence/`](evidence/). Artefak render/PDF dibuat terpisah dari
source repository ini.

Paket rekayasa yang dirakit dari `SmartPlugV2.zip` pada 2026-08-25.

## Mulai dari sini

- [`design.md`](design.md) — baseline desain berbasis source, arsitektur,
  bahaya, gate rilis, dan rencana pengembangan.
- [`datasheet.md`](datasheet.md) — datasheet rekayasa terkendali. Dokumen
  ini sengaja belum menjadi datasheet penjualan/produksi selama rating kritis
  belum terverifikasi.
- [`production-readiness-questions.md`](production-readiness-questions.md) —
  bank pertanyaan praproduksi/produksi yang terstruktur.
- [`firmware/`](firmware/) — firmware bring-up aman ESP-07 yang dapat
  dikompilasi. Aktuasi relay dinonaktifkan secara default.
- [`hardware/easyeda/`](hardware/easyeda/) — source EasyEDA yang tidak
  dimodifikasi dari arsip yang diberikan.
- [`evidence/`](evidence/) — manifest source, audit schematic, audit PCB,
  dan BOM yang diturunkan dari source.

## Disposisi saat ini

**BELUM SIAP UNTUK PROTOTIPE BERTEGANGAN, PRAPRODUKSI, ATAU RILIS PRODUKSI.**

Schematic/layout yang diberikan menunjukkan jalur beban dengan neutral yang
disakelar, GND elektronik terhubung ke neutral mains, tidak terlihat adanya
fuse/thermal-fuse, antarmuka daya yang generik/tanpa rating terbukti, jalur PCB
protective earth yang belum terkualifikasi, serta beberapa blocker
clearance/jalur arus. Ini adalah fakta desain yang harus ditutup, bukan bukti
bahwa unit hasil fabrikasi aman atau tidak aman dalam setiap kondisi.

Tidak ada board yang di-flash, diberi daya, dihubungkan ke mains, di-probe, atau
diuji fungsinya sebagai bagian dari paket ini.
