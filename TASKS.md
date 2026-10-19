# Tasks — Depor CS HUB

Daftar ini memisahkan baseline yang terbukti pada kode dari pekerjaan berikutnya. Selesai pada baseline tidak berarti integrasi hosted telah diuji ulang pada 19 Oktober 2026.

## Baseline yang tersedia

- [x] Login, refresh terkoordinasi, logout, serta profile aktif — FR-01.
- [x] Empat KPI dan tiga chart ringkas; null dibedakan dari nol — FR-02.
- [x] Proker/UKOR dengan multiple PJ, tanggal/anggaran/progres opsional — FR-03.
- [x] Update progres khusus BPH/PJ dan audit perubahan nyata — FR-04.
- [x] Kanban tugas dengan ownership dan status — FR-05.
- [x] Catatan keuangan, inventaris, events dan profile — FR-06–08.
- [x] Dialog Detail dengan tinggi terbatas dan dukungan keyboard — FR-09.
- [x] Sembilan dokumen proyek terintegrasi pada root repository dengan kontrak yang sesuai kode.

## Iterasi 19 Oktober 2026

- [x] DATA-01: KPI global, distribusi status global, status Staff dari assignment lengkap; SQL rollback 35 program dan browser dataset >30.
- [x] UX-02: pencarian/filter server pada enam resource, debounce, reset halaman, metadata hasMore; kasus target di luar 100 record diuji.
- [x] UX-01: KPI putih dominan dan donut status, palet Depor, chart/deskripsi ringkas.
- [x] QA-02: browser mock 375/768/1440 px; overflow, fokus chart, navigasi, dialog dan form.
- [x] UX-03: pagination list lebih pendek, chart panjang dengan scroll internal dan link daftar lengkap.

Validasi: Java 62 tes; frontend lint/build dan 23 unit test; empat suite SQL disposable; browser mock 22 flow. Migrasi hosted dan baca RPC dengan simulated JWT berbeda dari login Auth nyata. QA-01 tetap tertunda: kredensial uji aman belum tersedia, tidak ada reset akun/data.

## Pekerjaan berikutnya

| ID | Prioritas | Pekerjaan | Kriteria selesai |
| --- | --- | --- | --- |
| QA-01 | P0 | Acceptance hosted dengan akun BPH/Staff yang sekarang | Login → progres/tugas → reload → refresh → logout; verifikasi izin dan persistence; hanya fixture aman milik tes yang dibersihkan |
| SEC-01 | P1 | Tinjau rekomendasi index/FK dan RLS init plan historis | Inspeksi ulang, migrasi terukur bila masih perlu, izin tidak berubah, tanpa fitur berbayar |
| DATA-02 | P1 | Lengkapi informasi yang belum diberikan | LPJ/tanggal/progres hanya diisi dari sumber pengguna; tidak mengasumsikan status selesai |
| FEAT-01 | P2 | Spesifikasi unggahan LPJ/lampiran | Batas ukuran/format, izin akses, retensi, storage Free dan UX disepakati sebelum implementasi |
| FEAT-02 | P2 | Spesifikasi notifikasi/undangan/role UI | Kebutuhan jelas, hak akses diuji, biaya tetap terkendali; belum dianggap fitur tersedia |

Prioritas P0 adalah langkah validasi, bukan klaim terjadi insiden. Pekerjaan baru tidak otomatis boleh menghapus data nyata atau mengaktifkan layanan berbayar.

## Definition of done per pekerjaan

1. Acceptance criteria jelas dan sesuai scope.
2. Implementasi, izin, empty/error/loading state dan data null ditangani.
3. Pemeriksaan yang relevan lulus; bukti mock/lokal/hosted dibedakan.
4. Dokumen kontrak/desain/changelog ikut diperbarui.
5. Bila release diotorisasi: commit dan target yang benar, CI lulus, status deployment terverifikasi.

## Cara memperbarui daftar

Pindahkan pekerjaan ke selesai hanya setelah ada bukti. Catat tanggal, commit/PR, pemeriksaan dan keterbatasannya. Status tidak diubah hanya karena tombol/komponen sudah dibuat atau mock test lulus.
