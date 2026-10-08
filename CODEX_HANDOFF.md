# Depor CS HUB — checkpoint development

Checkpoint: 8 Oktober 2026. Baca juga AGENTS.md dan README.md.

## Project dan izin

- Project development: **DeporCS HUB**, ref `dajpnhkutkhgxwkjzvpg`, URL `https://dajpnhkutkhgxwkjzvpg.supabase.co`.
- Organisasi **DeporCS Hub**, ID `jvdgogmbphotxdzrvbvc`; plan **Free** terverifikasi melalui connector. Hanya fitur Free/$0; tidak ada upgrade, add-on, paid branching, atau provisioning baru.
- Ref ini awalnya diperlakukan sebagai production. Pada 8 Oktober 2026 pengguna secara eksplisit menetapkannya sebagai development, mengizinkan migrasi `core` dan `events_profiles`, dan mencabut larangan production untuk ref tersebut. Jangan memakai larangan lama sebagai status terkini; jangan mengubah project production lain.
- Branch kerja kedua repository: `codex/supabase-main-flows`. Draft PR #1 tetap belum merge; jangan deploy/merge tanpa instruksi baru.
- Jangan menaruh kredensial di Git/dokumen/log/frontend. Gunakan environment backend. Frontend tidak memerlukan key Supabase.

## Repository

| Bagian | Repository | Draft PR |
| --- | --- | --- |
| Java API | https://github.com/DeporCS-HUB/Backend-DeporCS-Hub | https://github.com/DeporCS-HUB/Backend-DeporCS-Hub/pull/1 |
| React UI | https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub | https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/1 |

## Schema hosted yang sudah diterapkan

Migrasi sukses pada ref development di atas:

1. `supabase/migrations/20261008154150_core.sql` — versi hosted `20261008154150`, nama `core`.
2. `supabase/migrations/20261008154204_events_profiles.sql` — versi hosted `20261008154204`, nama `events_profiles`.

Filename diselaraskan dengan versi migrasi yang dikembalikan connector agar migration history dan repository cocok. Jangan menjalankan ulang migrasi sebagai file versi lama.

Enam tabel: profiles, programs, tasks, finances, inventory, events; seluruhnya memiliki RLS. Trigger Auth membuat profil inactive/member; metadata tidak dapat menaikkan role/active. SECURITY DEFINER bootstrap dan lookup role berada di schema `private`; dashboard RPC adalah SECURITY INVOKER. Nama metadata yang hanya whitespace dinormalisasi sebelum constraint nonblank.

Tidak ada seed permanen atau akun uji permanen. Jangan pernah menjalankan `supabase/tests/bootstrap.sql` pada hosted Supabase; itu harness Auth lokal.

## Implementasi

Backend Java 21 / Spring Boot 3.5.7: Supabase Auth memverifikasi token lewat `/auth/v1/user`; refresh cookie HttpOnly, access token frontend hanya di memori, mutasi Auth membatasi Origin. Data API selalu memakai user JWT dan public key, bukan service-role bypass. CRUD programs/tasks/finances/inventory/events, dashboard RPC, profil nama sendiri, validasi DTO/FK, serta role/ownership sudah diimplementasikan.

React 19 + Vite: login/session/protected routes, CRUD layar utama, refresh tersinkron, loading/empty/error/pagination. Events mencatat izin secara manual. Team read-only. Belum tersedia undangan, attendance, role admin UI, attachment, notifikasi, bahasa/theme, pengiriman izin eksternal, atau penanganan konflik edit bersamaan.

## Validasi dan batas

- Kedua migrasi dan dua suite SQL RLS lulus di PGlite lokal.
- Kedua suite RLS juga lulus pada PostgreSQL hosted development. Fixtures Auth/profil/data dibuat dalam transaksi dan seluruhnya di-rollback; JWT claims disimulasikan sebagai role SQL, bukan login Supabase nyata. Suite mensyaratkan database development kosong sebelum fixture dimulai.
- Pengujian mencakup staff CRUD, member/ownership, role escalation, inactive accounts, anon access, own-profile updates, event permissions, tanggal/quantity, foreign keys, dan agregasi 1.002 task. Metadata whitespace/spoof role/active juga diperiksa.
- Script read-only readiness lulus 15 pemeriksaan HTTP/API nyata: Auth settings, anon denial pada enam tabel/RPC, backend health, authentication required, invalid login/token/refresh melalui Java adapter, penghapusan cookie refresh kosong, dan Origin rejection. Tidak ada akun/data dibuat oleh script. Login/CRUD/refresh positif tetap belum terbukti.
- Security advisor tidak menemukan lint keamanan. Performance advisor: 4 FK created_by belum memiliki covering index; 9 policy masih mengevaluasi auth.uid per baris; unused-index INFO pada database baru bukan alasan menghapus indeks. Temuan ini belum diubah oleh migrasi tambahan. Lihat docs/live-development-validation.md pada backend.
- Hosted Auth reachable, email signups aktif dan konfirmasi email tetap diwajibkan. Tidak ada service-role/admin key atau akun staff/member terverifikasi di runtime. Login sukses, authenticated PostgREST CRUD/persistensi, refresh sesi sukses, dan logout nyata belum terbukti.
- Validasi Java terkini: `mvn verify` lulus 41 tests (31 mocked API/security + 10 simulated Auth/REST HTTP adapter), termasuk pemetaan Auth 403 bad_jwt menjadi 401 tanpa mengubah 403 permission/data. Validasi implementasi sebelumnya: 37 Java tests (mock API / simulated Auth HTTP), 10 frontend unit tests, 7 browser tests menggunakan mocked API. Hasil itu bukan bukti integrasi Auth hosted positif.

## Melanjutkan

1. Buat dua akun **development** melalui Supabase Auth dashboard/admin resmi; verifikasi email. Entry password harus dilakukan pengguna sendiri. Jangan menonaktifkan email confirmation untuk melewati provisioning.
2. Identifikasi UUID akun yang memang untuk staff/member sebelum aktivasi. Operator SQL terpercaya dapat menetapkan staff + active atau member + active pada UUID yang ditentukan, tanpa mengganti akun lain. Jangan percaya role/active dari user_metadata.
3. Pasang SUPABASE_URL dan SUPABASE_ANON_KEY dalam environment backend; simpan password akun uji secara aman pada environment pengguna. APP_ALLOWED_ORIGINS=http://localhost:3000, APP_COOKIE_SECURE=false untuk HTTP lokal.
4. Jalankan Java backend dan React UI. Uji login → dashboard → CRUD sesuai role → reload → refresh → logout dengan akun yang terverifikasi; bedakan ini dari tes RLS SQL.
5. Read-only HTTP/backend readiness tersedia di backend: `python scripts/verify-hosted-readiness.py` setelah `mvn verify`; script hanya menerima ref development yang diizinkan dan tidak membuat akun/data. JAVA_HOME opsional. Script tidak membuktikan login/CRUD positif.
6. Tinjau performance advisor sebelum beban besar; buat migrasi baru melalui Supabase CLI, jangan mengedit migration history hosted. Jangan upgrade plan.
7. Perbarui draft PR dan hasil validasi. Merge/deploy tetap memerlukan instruksi terpisah.

Riwayat chat dan sesi browser lokal tidak ikut berpindah melalui dokumen ini. Login Chrome pengguna tidak otomatis mengautentikasi cloud browser. Connector Supabase bekerja; dashboard cloud login sebelumnya terhalang Google 502.
