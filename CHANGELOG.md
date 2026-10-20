# Changelog — Depor CS HUB

Tanggal memakai YYYY-MM-DD. Entri berasal dari riwayat repository dan checkpoint percakapan yang tersedia. Tidak ada nomor versi aplikasi semantik yang direka.

## 2026-10-19 — Filter server dan KPI global

- Search literal dan filter tervalidasi untuk enam resource, pagination lookahead/hasMore, debounce dan reset halaman; ukuran halaman lebih pendek.
- Migrasi dashboard_coverage: recordedPrograms dan distribusi status global, status/progress pada assignment Staff; pratinjau 30 tidak lagi menentukan hitungan global.
- KPI utama putih, donut SVG, chart panjang dengan scroll keyboard; palet Depor dan route/chart splitting dipertahankan.
- Tautan runbook README menjadi relatif repository.
- Java 62 tes, 23 unit frontend, lint/build, empat suite SQL lokal dan 22 browser flow mock mencakup dataset besar serta viewport 375/768/1440.
- Hosted: migrasi sukses dan baca RPC dengan simulated JWT diverifikasi. Login/refresh/logout hosted dengan akun BPH/Staff belum diuji karena kredensial aman tidak tersedia. Tidak ada akun, seed, atau record operasional yang dibuat/diubah untuk pengujian.

## 2026-10-19 — Integrasi dokumentasi repository

### Ditambahkan

AGENTS.md, PRD.md, DESIGN.md, ARCHITECTURE.md, DATA.md, API.md, TASKS.md, CHANGELOG.md dan README.md pada root repository frontend dan backend. README disesuaikan dengan masing-masing repository; aturan AGENTS yang sudah berlaku dipertahankan. DESIGN.md memetakan referensi dashboard yang dilampirkan pengguna. API/data dijelaskan dari kode yang sudah ada, bukan sebagai proyek statis hardcoded.

### Batas perubahan

Perubahan hanya pada dokumentasi dan checkpoint. Tidak ada perubahan kode aplikasi, konfigurasi, migrasi, seed, akun, role, atau data hosted. CI/deployment yang berjalan otomatis berasal dari pipeline repository yang sudah ada; tidak ada layanan baru yang diprovision.

## 2026-10-10 — Dashboard KPI ringkas

- Empat KPI dan chart progres kegiatan, metrik Staff, serta distribusi status menggantikan panel naratif panjang.
- Program dan Tugas memindahkan deskripsi ke dialog Detail dengan scroll internal dan dukungan fokus keyboard.
- Data belum diisi dibedakan dari 0%; kontrol progres PJ dipertahankan.
- Frontend [PR #7](https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/7), merge `cae6348`.

Checkpoint saat itu mencatat 22 unit test dan 17 browser flow mock lulus. Ini bukti historis, bukan tes yang dijalankan lagi khusus untuk penyusunan dokumen.

## 2026-10-10 — Program workspace

- Proker/UKOR, multiple PJ, progres/tanggal/anggaran opsional, dan milestone.
- Endpoint progres khusus PJ/BPH, penyimpanan program/PJ atomik, serta audit perubahan nyata.
- Agregasi tanggung jawab, tugas dan update Staff; data departemen diisi di development tanpa masuk fixture public.
- Backend [PR #5](https://github.com/DeporCS-HUB/Backend-DeporCS-Hub/pull/5), merge `445813b`.
- Frontend [PR #6](https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/6), merge `ad89d00`.

Checkpoint mencatat 56 tes Java serta pemeriksaan SQL lokal dan rollback hosted dengan simulated JWT. Pemeriksaan ini berbeda dari login hosted dengan roster pengguna terkini.

## 2026-10-10 — Tema Depor

Palet charcoal/navy/muted red, heading ringkas dan label navigasi Indonesia. Frontend [PR #5](https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/5), merge `ca37d01`.

## 2026-10-09 — Optimasi loading

Route/chart splitting, penghapusan request font eksternal, form login yang terlihat saat pengecekan sesi, serta cache GET memori 30 detik dengan deduplikasi dan invalidasi. Backend mempertahankan pengecekan Auth/profile; cache preflight CORS satu jam. Frontend merge `17b639e`, backend merge `c2a55b9`.

## Riwayat fondasi

Migration core dan events_profiles bertanggal 2026-10-08 menyiapkan schema, RLS, Auth profile, events dan edit profile sendiri. Dokumen lama juga menyimpan bukti integrasi dengan akun uji yang kemudian diganti. Bukti historis tersebut tidak berarti akun uji lama masih aktif atau harus dibuat ulang.


## 2026-10-19 — Workspace program dan pelacakan tugas

- Halaman mandiri Proker/UKOR: Ringkasan, Tugas, Timeline, Agenda; teks panjang terbatasi dan detail lengkap dalam dialog.
- Akses program dan record terkait dibatasi ke BPH/PJ melalui RLS; tugas mandiri privat untuk creator/assignee dan BPH. Assignment menggunakan akun aktif.
- Agenda editable dan catatan manual append-only, digabung dengan audit progres pada timeline paginated.
- Papan/daftar, filter program/akun/prioritas/tugas saya, beban akun BPH, count seluruh hasil filter, pagination per kolom, drag/drop/status keyboard dan refresh.
- Status-only write dengan guard updated_at; perbaikan selected account pada opsi async dan reset cursor saat filter kembali ke nilai sebelumnya.
- Tidak mengubah akun, assignment tersimpan, progress historis atau menambah provider/dependency berbayar.


## 2026-10-19 — Status program dan pengaturan akun

- Pilihan status utama Planning / On Going / Finished; Active lama termasuk filter On Going, tanpa backfill audit/progres.
- Foto profil privat: crop preview, upload/ganti/hapus, avatar header, validasi/re-encode JPEG, pointer CAS dan cleanup objek versi lama.
- Ganti password sendiri dengan verifikasi password saat ini dan sesi baru; tidak mengandalkan toggle current-password project. Error tidak menyebabkan logout/refresh loop; sukses membersihkan input.
- Migration profile_avatars hanya menambah pointer/config bucket/RLS. Tidak mengganti akun/password/foto nyata, tidak menambah dependency/layanan berbayar.


## 20 Oktober 2026 — Foto tim dan assignment

- Foto akun ditampilkan bersama nama pada Tim Departemen, PJ program serta assignment papan/daftar/detail tugas dan ringkasan beban. Identitas berasal dari UUID akun.
- API batch signed URLs dan policy baca foto terkini anggota aktif; bucket tetap private dan mutasi foto tetap owner-only.
- Pembacaan direktori mengikuti invalidasi cache akun; fallback inisial dan refresh URL menghindari foto kosong menghalangi pekerjaan. Tidak menambah dependency aplikasi, reset akun atau layanan berbayar.
