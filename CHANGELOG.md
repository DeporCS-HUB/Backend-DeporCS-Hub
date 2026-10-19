# Changelog — Depor CS HUB

Tanggal memakai YYYY-MM-DD. Entri berasal dari riwayat repository dan checkpoint percakapan yang tersedia. Tidak ada nomor versi aplikasi semantik yang direka.

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
