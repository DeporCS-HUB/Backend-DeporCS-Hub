# Depor CS HUB — kelanjutan di Codex

Checkpoint: 8 Oktober 2026. Tujuan pemindahan adalah melanjutkan implementasi yang sudah ada, termasuk menyiapkan Supabase development gratis. Tidak ada transfer otomatis riwayat chat atau sesi login melalui file ini.

## Repository dan branch

| Bagian | Repository | Branch | Draft PR |
| --- | --- | --- | --- |
| API Java | https://github.com/DeporCS-HUB/Backend-DeporCS-Hub | `codex/supabase-main-flows` | https://github.com/DeporCS-HUB/Backend-DeporCS-Hub/pull/1 |
| React UI | https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub | `codex/supabase-main-flows` | https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/1 |

Kedua PR masih draft, belum merge dan belum deploy. Main backend masih implementasi Node; pilih branch di atas untuk mendapatkan Java. Commit implementasi sebelum penambahan handoff ini: backend `f4e4becbf06bd70c396bfe7d793741df14c0ea37`, frontend `376b6d8ac2c5bd42be8e4ecefe2302e9d94baa6c`. Gunakan head branch terkini, bukan checkout permanen pada commit checkpoint.

## Batas dari pengguna

- Gunakan Supabase Free saja dengan biaya tambahan **$0**. Jangan upgrade plan, membeli compute/add-on, atau menggunakan branching berbayar.
- Pengguna sudah mengizinkan project development baru bernama **DeporCS HUB Dev** dalam organisasi **DeporCS Hub**, ID `jvdgogmbphotxdzrvbvc`, asalkan biaya aktual $0 terverifikasi.
- Pengguna mengizinkan dashboard Supabase di browser sebagai fallback jika connector tidak cukup. Prefer connector bila tersedia; periksa ketersediaannya lagi di sesi baru.
- **Jangan ubah production/main** `dajpnhkutkhgxwkjzvpg`, termasuk migrasi, seed, Auth, roles, settings, reset, atau penghapusan.
- Kredensial hanya melalui environment yang aman. Jangan menyalin rahasia dari riwayat chat ke file. Frontend tidak memerlukan key Supabase.

## Supabase yang sudah diperiksa

Inspection terakhir hanya menemukan satu project: **DeporCS HUB**, ref `dajpnhkutkhgxwkjzvpg`, organisasi di atas, region `ap-northeast-2`, status `ACTIVE_HEALTHY`, PostgreSQL 17.6. Plan organisasi adalah Free. Project ini tetap dianggap main/production meskipun terlihat kosong.

Saat inspection: tidak ada app tables/functions/policies/grants dalam public schema, riwayat migrasi kosong, `auth.users` berjumlah 0, tidak ada branches, dan security advisor mengembalikan lints kosong. Ini checkpoint hasil read-only, bukan jaminan kondisi saat ini atau sertifikasi keamanan.

**Development belum dibuat atau ditemukan. Tidak ada migrasi, seed, Auth provisioning, atau perubahan hosted yang dilakukan.**

Connector Supabase tersedia untuk inspection, tetapi `get_cost` gagal: `MCP tool get_cost was not returned by tools/list`. Karena provisioning membutuhkan konfirmasi biaya, pembuatan project tidak dilakukan. Jangan mengarang `confirm_cost_id`; gunakan alur biaya sah atau dashboard yang menunjukkan Free/$0 dan kuota yang masih tersedia.

Dashboard sebelumnya meminta login. Alur Supabase → GitHub → Google membingungkan pengguna, kemudian autentikasi dibatalkan. Jangan menganggap sesi browser, akun, koneksi plugin, atau login berhasil ikut berpindah ke Codex. Jangan mengulangi autentikasi GitHub/Google secara otomatis.

## Implementasi yang sudah selesai

- Backend **Java 21 / Spring Boot 3.5.7** menggantikan Node pada branch kerja. Supabase menyediakan PostgreSQL, Auth, dan PostgREST.
- Login/refresh/session/logout: verifikasi access token melalui `/auth/v1/user`; refresh cookie HttpOnly; access token frontend hanya dalam memori; origin terpercaya untuk mutasi auth.
- Roles berasal dari `public.profiles`; akun baru inactive sampai operator terpercaya mengaktifkan. Permintaan data memakai JWT pengguna dan public/anon key, dengan RLS; tidak memakai service-role bypass.
- CRUD programs, tasks, finances, inventory, events; dashboard RPC; validasi DTO/relasi; format respons konsisten. Member dibatasi berdasarkan role/ownership, staff/admin mengelola resource terkait.
- React 19 + Vite: layar utama tersambung API, protected routes, loading/empty/error, pagination, session refresh tersinkron, dan perubahan hanya tampil setelah server mengonfirmasi.
- Events mencatat jadwal, venue, relasi program, dan status izin secara manual. Tidak mengirim permohonan izin eksternal.
- Settings menyimpan nama profil sendiri melalui `PUT /api/profiles/me`; Team berupa direktori profil read-only.

Belum tersedia: undangan akun, attendance, administrasi role melalui UI, workflow izin eksternal, preferensi bahasa/theme/notifikasi, uploads, dan penanganan konflik edit bersamaan. Jangan mengklaim fitur ini selesai.

## Migrasi dan validasi

Pada repository backend, urutan migrasi:

1. `supabase/migrations/202610080001_core.sql`
2. `supabase/migrations/202610080002_events_profiles.sql`

`supabase/seed.sql` hanya untuk development, opsional, dan belum dijalankan. Review schema/grants/RLS sebelum menerapkan migrasi; periksa nama profil yang hanya whitespace sebelum constraint kedua. Akun Auth harus dibuat melalui provisioning development yang terpercaya; role/active tidak dapat dinaikkan oleh metadata pengguna.

**`supabase/tests/bootstrap.sql` memalsukan Auth schema/roles untuk harness PostgreSQL lokal. Jangan pernah menjalankannya pada hosted Supabase.** Suite `rls.sql` dan `rls_events_profiles.sql` juga khusus database disposable untuk pengujian.

Bukti implementasi sebelumnya:

- Backend `mvn verify`: 37 tes (31 mocked API/security, 6 adapter dengan HTTP Supabase simulasi). Kedua migrasi dan suite RLS lulus di PGlite lokal serta PostgreSQL 17 CI.
- Backend CI: https://github.com/DeporCS-HUB/Backend-DeporCS-Hub/actions/runs/37760404413
- Frontend build/lint, 10 unit tests, dan 7 Playwright browser tests lulus di GitHub CI dengan mocked API.
- Frontend CI: https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/actions/runs/37760417400
- Chromium lokal saat itu terhalang pembatasan Unix socket runtime; CI browser berhasil.
- **Login hosted, refresh hosted, CRUD database nyata, dan persistensi lintas reload belum divalidasi.**

## Buka source di Codex

Jika repository belum tersedia lokal, jalankan dari folder induk baru yang kosong dengan Git terpasang dan akses GitHub yang sesuai:

```sh
git clone --branch codex/supabase-main-flows https://github.com/DeporCS-HUB/Backend-DeporCS-Hub.git
git clone --branch codex/supabase-main-flows https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub.git
```

Jika folder sudah ada, periksa branch/status dan simpan perubahan lokal sebelum checkout/pull; jangan menimpa atau reset perubahan. Tambahkan kedua folder ke project lokal Codex dan jadikan backend primary. Alternatif CLI: mulai Codex dari folder induk kedua repo agar keduanya bisa dikerjakan. Baca juga handoff pada kedua repo.

Prerequisites: Java 21, Maven 3.9+, Node 22+. Backend: `mvn verify` dan `mvn spring-boot:run`. Frontend: `npm ci`, `npm run build`, `npm run lint`, `npm test`, `npm run dev`. Browser test: `npx playwright install chromium` kemudian `npm run test:browser` pada runtime yang mendukungnya.

Konfigurasi backend: `SUPABASE_URL` dan `SUPABASE_ANON_KEY` dari development; `APP_ALLOWED_ORIGINS=http://localhost:3000`, `APP_COOKIE_SECURE=false` untuk HTTP lokal. Spring tidak otomatis memuat file `.env`; gunakan environment proses. Backend port 8080, frontend port 3000. UI default memakai proxy `/api`; semua nilai `VITE_*` bersifat publik. Lihat README tiap repo untuk kontrak dan konfigurasi lengkap.

## Langkah berikutnya

1. Periksa checkout/head branch dan akses Supabase terbaru secara read-only. Identifikasi organisasi, plan, kuota, project, dan biaya aktual.
2. Bila belum ada development, buat **DeporCS HUB Dev** dalam organisasi yang diizinkan hanya setelah $0 terverifikasi. Hentikan provisioning jika gratis tidak dapat dipastikan. Jangan gunakan main sebagai pengganti.
3. Verifikasi ref development hasil provisioning dan catat pemisahannya dari ref main. Review schema, lalu terapkan kedua migrasi hanya ke development.
4. Siapkan akun uji member dan staff, aktivasi lewat operator terpercaya; pasang environment backend tanpa mengekspos key. Seed demo opsional pada development saja.
5. Validasi alur nyata: login → dashboard → CRUD sesuai role → reload → refresh → logout; periksa penolakan privilege escalation dan akses inactive.
6. Perbaiki temuan pada branch kerja, update draft PR, dan laporkan bukti. Merge/deploy memerlukan instruksi tersendiri.

Instruksi awal untuk chat Codex: **Baca AGENTS.md, CODEX_HANDOFF.md, dan README.md pada kedua repository. Lanjutkan dari checkpoint terakhir dengan Supabase development Free/$0, jangan ubah production `dajpnhkutkhgxwkjzvpg`, dan verifikasi biaya sebelum provisioning.**
