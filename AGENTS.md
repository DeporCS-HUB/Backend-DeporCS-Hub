# Depor CS HUB

Baca `CODEX_HANDOFF.md` dan `README.md` sebelum melanjutkan proyek ini.

- Pada 9 Oktober 2026 pengguna meminta merge hingga deployment. Merge PR #1 backend/frontend dan deployment Free/$0 sudah diizinkan; gunakan database development di bawah dan verifikasi billing hosting sebelum provisioning.
- Supabase harus memakai fitur Free dengan biaya tambahan $0. Verifikasi biaya aktual sebelum provisioning; jangan mengaktifkan layanan berbayar.
- Pada 8 Oktober 2026 pengguna secara eksplisit menetapkan project `dajpnhkutkhgxwkjzvpg` sebagai development, mengizinkan migrasi `core` dan `events_profiles`, serta mencabut larangan perubahan production untuk ref ini. Semua testing hosted harus tetap memakai ref development tersebut; jangan mengubah project production lain.
- Semua migrasi, seed, akun uji, dan perubahan data hosted hanya boleh dilakukan pada development yang identitasnya sudah diverifikasi.
- Jangan menyimpan kredensial dalam repository, dokumen, log, atau frontend. Gunakan environment backend.
- Jangan jalankan `supabase/tests/bootstrap.sql` pada hosted Supabase; file ini hanya harness database lokal sementara.
- Pertahankan perubahan pengguna dan laporkan batas validasi secara akurat: tes mock/lokal tidak membuktikan integrasi hosted.

- Target hosting terkini adalah Vercel, dipilih pengguna pada 9 Oktober 2026. Kedua repo public dan PR #1 sudah merged. Verifikasi akun Hobby/$0 dan penggunaan aktual sebelum deploy; file Render adalah alternatif yang belum dipakai. Jangan provision dua provider.

## Panduan kerja terintegrasi — 19 Oktober 2026

Repository ini berisi komponen API; repository pasangan: `DeporCS-HUB/Frontend-DeporCS-Hub`. Ketentuan sebelumnya di atas tetap berlaku.

## Konteks dan urutan baca

Depor CS HUB adalah aplikasi operasional Departemen Olahraga BEM Fasilkom UI. Sistem memiliki frontend React/Vite, backend Java/Spring Boot, serta Supabase untuk Auth dan PostgreSQL. Aplikasi bukan prototipe dengan data hardcoded.

Baca [README.md](README.md), [PRD.md](PRD.md), [ARCHITECTURE.md](ARCHITECTURE.md), lalu dokumen sesuai pekerjaan. Dokumen pada root menjelaskan proyek lintas frontend/backend. `CODEX_HANDOFF.md` menyimpan checkpoint dan riwayat; bagian bertanggal lama tidak menggantikan keputusan terbaru. Baca aturan setempat di repository pasangan sebelum mengeditnya.

## Batas kerja

- Ikuti permintaan terbaru pengguna dan otorisasi yang sudah diberikan. Lanjutkan pekerjaan rutin tanpa meminta izin yang sama berulang kali.
- Pertahankan perubahan pengguna. Periksa `git status` dan commit dasar sebelum mengedit.
- Gunakan layanan Free/$0 yang sudah tersedia. Jangan mengaktifkan upgrade, add-on, branching berbayar, atau memasang metode pembayaran.
- Development yang diizinkan: project Supabase `dajpnhkutkhgxwkjzvpg`. Jangan memodifikasi project lain. Verifikasi identitas target sebelum tindakan hosted.
- Integrasi dokumentasi ini tidak mengubah kode aplikasi, konfigurasi, migrasi, seed, akun, atau data hosted. Pertahankan pipeline yang sudah ada; jangan menambah layanan hosting.
- Jangan menaruh password, token, private key, service-role key, data bank, atau data operasional privat dalam Git, contoh request, log, maupun frontend.
- Repository bersifat public. Isi data departemen melalui database yang diizinkan; gunakan data fiktif untuk fixture.

## Aturan implementasi

1. Otorisasi bersumber dari profile aktif yang diverifikasi backend. Jangan percaya role dari input atau metadata pengguna.
2. Label produk BPH/Staff berbeda dari nilai role lama. Pertahankan mapping pada [DATA.md](DATA.md).
3. Semua request data memakai JWT pengguna. Jangan mengatasi error izin dengan bypass RLS atau service-role key.
4. `null` berarti belum diketahui. Jangan mengubah progres, tanggal, atau anggaran kosong menjadi angka/tanggal rekaan.
5. Jangan membuat skor performa, tren, absensi, aktivitas audit, atau transaksi yang tidak memiliki data sumber.
6. Teks panjang masuk dialog Detail. Dashboard mengutamakan KPI dan chart; hindari daftar paragraf serta kartu dekoratif.
7. Pertahankan cache yang terisolasi per akun, invalidasi setelah write berhasil, dan route/chart splitting.
8. Jangan mengubah data tersimpan hanya untuk membuat chart terlihat terisi.

## Verifikasi sesuai perubahan

| Perubahan | Pemeriksaan |
| --- | --- |
| Dokumentasi | Tautan internal, istilah role, kontrak API, contoh JSON, dan status implementasi konsisten |
| Frontend | `npm run lint`, `npm test`, `npm run build`; browser test untuk alur yang terdampak |
| Backend | Java 21; `mvn verify`; uji izin dan validasi request yang berubah |
| Schema/RLS | Migrasi terurut dan rollback test pada database disposable; verifikasi target hosted terpisah bila termasuk scope |
| UI | Desktop dan 375 px; keyboard, loading/error/empty, teks panjang, serta overflow |

Jangan menjalankan `supabase/tests/bootstrap.sql` atau fixture suite pada database hosted berisi akun/data nyata. Jumlah tes historis bukan bukti bahwa tes sudah dijalankan ulang.

## Selesai berarti

Perubahan memenuhi acceptance criteria, pemeriksaan relevan selesai, dokumentasi terkait diperbarui, dan keterbatasan pengujian dijelaskan. Laporan membedakan hasil lokal/mock, SQL dengan simulated JWT, dan integrasi login/browser hosted yang nyata. Untuk release yang sudah diotorisasi, verifikasi repo, commit, target, CI, dan deployment yang benar.
