# Checklist Rilis Datasheet SmartPlug

Tujuan checklist ini adalah mencegah informasi yang dibutuhkan pembaca, terutama kontrak API dan batas fungsi, terlewat dari datasheet rilis berikutnya.

## 1. Tentukan pembaca dan status produk

- [ ] Satu dokumen hanya untuk satu pembaca utama: pengguna/integrator atau engineering internal.
- [ ] Status produk disebutkan dengan jelas: evaluation, pre-production, atau production.
- [ ] Tidak ada rating, akurasi, sertifikasi, atau kemampuan yang belum dibuktikan.
- [ ] Batas keselamatan ditulis sebagai larangan yang dapat dipahami pembaca, tanpa memberi instruksi pemasangan berbahaya.

## 2. Informasi minimum untuk pengguna/integrator

- [ ] Fungsi yang tersedia saat ini dan peran aplikasi dijelaskan.
- [ ] Fungsi yang tidak tersedia atau tidak boleh diasumsikan dicantumkan.
- [ ] Prasyarat penggunaan, koneksi, dan perilaku saat data tidak tersedia dijelaskan.
- [ ] Dokumen tidak membebani pembaca dengan GPIO, net PCB, MPN, atau catatan source internal kecuali pembacanya engineering.

## 3. Kontrak API - wajib bila perangkat menawarkan API

- [ ] Versi API, transport, base URL/port, dan kebutuhan internet dicantumkan.
- [ ] Setiap endpoint: method, path, HTTP status, dan kegunaannya dicantumkan.
- [ ] Content type, cache, autentikasi, dan batas keamanan transport dijelaskan.
- [ ] Respons awal atau contoh respons untuk kondisi penting tersedia.
- [ ] Field yang dipakai aplikasi mempunyai arti, satuan, kondisi valid, dan batas stale yang jelas.
- [ ] Endpoint atau aksi yang tidak tersedia dijelaskan, termasuk perilaku error/fallback.
- [ ] Tidak ada klaim kontrol fisik apabila firmware atau hardware belum membuktikannya.

## 4. Pemeriksaan bukti

- [ ] Isi dibandingkan dengan source firmware dan hasil build yang sama revisinya.
- [ ] Klaim runtime, pengukuran, skala koneksi, dan akurasi hanya muncul bila ada bukti pengujian perangkat nyata.
- [ ] Versi dokumen, versi firmware, dan status pengujian konsisten.
- [ ] Referensi internal dipisahkan dari dokumen pengguna.

## 5. QA rilis

- [ ] PDF tervalidasi jumlah halamannya dan diekstrak untuk memastikan teks API hadir.
- [ ] Seluruh halaman dirender dan diperiksa: tidak ada teks terpotong, tumpang tindih, tabel pecah, atau karakter rusak.
- [ ] Hash SHA-256 PDF dicatat setelah QA.
- [ ] Tautan indeks datasheet menunjuk hanya ke revisi rilis terkini.
