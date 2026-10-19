# Depor CS HUB API

Aplikasi internal Departemen Olahraga BEM Fasilkom UI untuk memantau Proker/UKOR, membagi tugas, memperbarui progres, dan mencatat keuangan, inventaris serta kegiatan.

Dokumentasi lintas repository ini disusun 19 Oktober 2026 dari frontend `cae63480492c8c1ba4831142114e01c6461d129e` dan backend `445813be49409da21bc21ef88a12abb54bad3829`. Referensi `main` kedua repository diperiksa sesuai commit tersebut. Tidak ada audit ulang isi database, billing, atau login hosted dalam pekerjaan dokumentasi ini.

## Dokumentasi proyek

| Dokumen | Isi |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Aturan kerja, batas biaya/data, verifikasi dan definition of done |
| [PRD.md](PRD.md) | Tujuan, pengguna, kebutuhan dan acceptance criteria |
| [DESIGN.md](DESIGN.md) | Referensi dashboard, token visual, komponen dan UX |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Dua repository, Auth, API, cache dan deployment |
| [DATA.md](DATA.md) | Schema, hubungan, role, enum, null dan audit |
| [API.md](API.md) | Endpoint, payload, pagination, izin dan error |
| [TASKS.md](TASKS.md) | Baseline dan backlog prioritas |
| [CHANGELOG.md](CHANGELOG.md) | Riwayat perubahan dengan bukti commit/PR |
| README.md | Cara membaca paket dan menjalankan proyek |

Kesembilan dokumen tersedia pada root repository ini. Repository ini memuat komponen API; penjelasan komponen pasangan disertakan agar kontrak proyek konsisten. Aturan AGENTS sebelumnya tetap dipertahankan dan riwayat implementasi tersedia di [CODEX_HANDOFF.md](CODEX_HANDOFF.md).

## Repository dan target aplikasi

- Frontend: https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub
- Backend: https://github.com/DeporCS-HUB/Backend-DeporCS-Hub
- URL frontend yang dipakai proyek: https://depor-cs-hub-web.vercel.app/
- URL API yang dipakai proyek: https://depor-cs-hub-api.vercel.app/api

Data operasional berasal dari backend/Supabase. Hardcoded hanya sesuai untuk konstanta UI dan fixture tes. DATA.md dan API.md tetap diperlukan walaupun tidak mengonsumsi konten publik dari API eksternal.

## Fitur baseline

Login dan sesi; KPI/chart dashboard; Proker/UKOR multiple PJ; update progres oleh PJ/BPH; kanban tugas; catatan keuangan; inventaris; events; direktori tim; edit nama profile sendiri. Deskripsi panjang dibaca melalui Detail. Pencarian/filter berjalan di server untuk seluruh hasil; list dipaginasi 10 program, 12 aset, atau 20 record lainnya per halaman.

Belum tersedia: upload LPJ/lampiran, undangan akun, role administration UI, absensi, notifikasi, atau pengiriman izin venue. Lihat [PRD.md](PRD.md) untuk batas implementasi.

## Menjalankan frontend

Prasyarat: Node 22+ dan npm; ikuti lockfile repository. Jalankan perintah berikut dari direktori clone `Frontend-DeporCS-Hub`.

```sh
npm ci
npm run dev
```

Frontend lokal berjalan pada `http://localhost:3000`. Vite meneruskan `/api` ke `http://localhost:8080`. `API_PROXY_TARGET` dapat mengganti target lokal. `VITE_API_URL` opsional saat lokal; untuk deployment terpisah, nilainya adalah URL API HTTPS berakhiran `/api`.

Tidak diperlukan Supabase key, database credential, atau secret di frontend. Semua `VITE_*` terlihat di browser.

## Menjalankan backend

Prasyarat: Java 21, Maven 3.9+, dan environment development yang dikonfigurasi secara aman. Jalankan perintah berikut dari direktori clone `Backend-DeporCS-Hub`.

```sh
mvn verify
mvn spring-boot:run
```

| Environment | Penggunaan |
| --- | --- |
| SUPABASE_URL | URL project development yang diizinkan |
| SUPABASE_ANON_KEY | Public API key untuk adapter; request data tetap memakai JWT pengguna |
| APP_ALLOWED_ORIGINS | Origin frontend persis; lokal http://localhost:3000 |
| APP_COOKIE_SECURE | false hanya untuk HTTP lokal; true untuk HTTPS |
| APP_COOKIE_SAME_SITE | Lax lokal; None + secure untuk model domain terpisah yang dipakai |
| PORT | Default 8080; sesuaikan port routing hosting |
| SUPABASE_CONNECT_TIMEOUT_MS | Default 5000 |
| SUPABASE_READ_TIMEOUT_MS | Default 10000 |

Spring tidak memuat file .env otomatis. Gunakan environment shell/IDE/hosting tanpa memasukkan secret ke Git atau argumen command. Runtime normal tidak membutuhkan service-role key maupun JWT_SECRET buatan aplikasi.

Database development yang diizinkan: `dajpnhkutkhgxwkjzvpg`. Gunakan migration yang sudah tercatat dengan benar; jangan reset/seed hosted sebagai langkah startup rutin. Akun memerlukan Auth valid dan profile aktif; tidak ada akun/password bawaan dalam paket.

## Pemeriksaan

Frontend:

```sh
npm run lint
npm test
npm run build
npx playwright install chromium
npm run test:browser
```

Backend: `mvn verify`. SQL harness hanya pada database disposable. Jalankan browser/integrasi hosted secara terpisah dengan akun/environment aman bila memang termasuk scope.

Checkpoint terakhir pada 10 Oktober mencatat 22 unit test + 17 browser flow mock frontend dan 56 tes Java. Validasi khusus dokumentasi memeriksa tautan, kontrak, contoh JSON dan sumber kode. CI aplikasi yang berjalan otomatis dicatat pada PR terkait. Data hosted tidak dimodifikasi.

## Pengembangan berikutnya

Mulai dari [TASKS.md](TASKS.md). Referensi dashboard baru adalah arahan desain lanjutan, bukan klaim bahwa seluruh elemen gambar sudah menjadi fitur. Pertahankan layanan Free/$0 yang ada dan jangan menambah sumber daya berbayar.

## Panduan implementasi lanjutan

- [Checkpoint pengembangan](CODEX_HANDOFF.md)
- [deployment](docs/deployment.md)
- [live development validation](docs/live-development-validation.md)
- [program workspace](docs/program-workspace.md)
- [role adjustment](docs/role-adjustment.md)
- [vercel deployment](docs/vercel-deployment.md)


### Workspace program & tugas

Buka **Program → Buka program** untuk Ringkasan, Tugas, Timeline dan Agenda. Akses BPH/PJ dibatasi sampai RLS. BPH memilih akun PJ melalui Kelola program & PJ; Staff hanya membaca program yang ditangani. Tugas mendukung papan/daftar, filter, pagination per kolom, drag/drop atau select status, ringkasan global dan beban per akun BPH. Tombol muat ulang mengambil data terbaru; belum ada push Realtime. Kontrak dan izin ada di API.md/DATA.md.
