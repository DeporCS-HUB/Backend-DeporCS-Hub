# API — Depor CS HUB

Kontrak ini mengikuti controller/DTO backend dan iterasi 19 Oktober 2026. Ini API aplikasi yang sudah ada, meskipun tidak mengambil konten publik dari API pihak ketiga.

## Base URL, format, dan sesi

- Local: `http://localhost:8080/api`
- Target hosted yang dipakai proyek: `https://depor-cs-hub-api.vercel.app/api`
- Content-Type: `application/json`.
- Endpoint data memerlukan `Authorization: Bearer <access-token>` dari login.
- Cookie refresh tidak mengautentikasi endpoint data. Browser memakai credentials include untuk request Auth.
- POST Auth memerlukan header Origin yang persis sesuai konfigurasi, termasuk dari client command line.

Response berhasil membungkus hasil dalam `data`. List menambahkan `meta`:

```json
{"data":[],"meta":{"page":0,"size":50,"hasMore":false}}
```

Error:

```json
{"error":{"code":"VALIDATION_ERROR","message":"Periksa format dan kelengkapan input."}}
```

## Autentikasi

| Method | Path | Input | Hasil |
| --- | --- | --- | --- |
| POST | /auth/login | email, password | data.accessToken, expiresIn, user; Set-Cookie refresh |
| POST | /auth/refresh | Cookie depor_refresh, body tidak diperlukan | Token/sesi baru atau error |
| GET | /auth/session | Bearer | Profile pengguna dengan departmentRole |
| POST | /auth/logout | Bearer dan Origin diizinkan | data.loggedOut=true; cookie dibersihkan |
| GET | /health | Tanpa bearer | Status JVM; bukan bukti koneksi DB/Auth |

Login/refresh dapat diakses tanpa bearer, tetapi Origin tetap divalidasi. Tidak ada endpoint register, invite, reset password lewat email, atau administrasi role. POST /auth/password tersedia untuk mengganti password sendiri dengan verifikasi password saat ini. Gunakan pengelolaan Auth yang terpisah bila diperlukan.

## Endpoint data

Semua path berikut relatif terhadap `/api`.

| Resource | List | Create | Edit lengkap | Delete |
| --- | --- | --- | --- | --- |
| programs | GET /programs | POST /programs | PUT /programs/{id} | DELETE /programs/{id} |
| tasks | GET /tasks | POST /tasks | PUT /tasks/{id} | DELETE /tasks/{id} |
| finances | GET /finances | POST /finances | PUT /finances/{id} | DELETE /finances/{id} |
| inventory | GET /inventory | POST /inventory | PUT /inventory/{id} | DELETE /inventory/{id} |
| events | GET /events | POST /events | PUT /events/{id} | DELETE /events/{id} |
| profiles | GET /profiles | — | PUT /profiles/me | — |

Tambahan: `GET /dashboard` dan `PUT /programs/{id}/progress`.

GET /programs/{id} tersedia. Belum ada GET tunggal resource lain, PATCH aplikasi, export, atau upload. Backend memakai PATCH ke PostgREST secara internal; itu bukan method API publik Java.

List menerima page 0–100000, default 0; size 1–100, default 50. Urutan events: start_date lalu id; resource lain: created_at desc lalu id. Backend mengambil satu row tambahan untuk `meta.hasMore`, lalu mengembalikan paling banyak size row. Tidak ada total count list. Parameter yang tidak didukung atau enum tidak valid menghasilkan 400.

`q` mencari substring literal tanpa membedakan huruf besar/kecil, maksimum 120 karakter setelah trim, tanpa control character. `%`, `_`, `*`, koma, tanda kutip, dan karakter regex diperlakukan sebagai teks. Semua filter diterapkan sebelum pagination, tetap memakai JWT/RLS pengguna. Sort dan operator database tidak boleh dipasok client.

| Resource | Kolom pencarian q | Filter tambahan |
| --- | --- | --- |
| programs | name, pic, description, progress_notes | status, kind |
| tasks | title, description | status, priority, program_id, program_scope, assignee_id, scope |
| events | name, venue, description | status, permit_status |
| inventory | name, category, location | status |
| finances | description, category | status, type |
| profiles | name | departmentRole=bph atau staff |

Contoh: `GET /programs?q=latihan&kind=UKOR&page=0&size=10`. Nilai enum mengikuti DATA.md. departmentRole bph dipetakan ke role lama staff/admin; staff ke member. Tidak ada perubahan hak akses.

Create menghasilkan 201. Edit/delete berhasil menghasilkan 200. Delete mengembalikan `{"data":{"deleted":true}}`. Operasi yang tidak menemukan record/hasil write kosong tidak boleh dianggap sukses.

## Izin

| Operasi | BPH | Staff aktif |
| --- | --- | --- |
| Membaca program dan data terkait | Semua | Hanya program dengan UUID PJ; termasuk tugas, finance/event terkait, agenda, catatan dan audit |
| Membaca tugas legacy tanpa program | Semua | Creator/assignee saja |
| Agenda dan catatan program | Kelola agenda; tambah catatan | Sama, hanya program yang ditangani |
| CRUD program lengkap, finance, inventory, events | Ya | Tidak |
| Update progres program | Ya | Hanya PJ yang terhubung UUID |
| Membuat tugas | Dapat memilih assignee | Hanya untuk diri sendiri; tanpa assignee API mengisinya ke diri sendiri |
| Edit/hapus tugas | Semua | Creator atau assignee; aturan reassign tetap dibatasi |
| Edit nama profile | Diri sendiri | Diri sendiri |
| Mengubah role/active | Tidak lewat API ini | Tidak |

RLS dan validasi profile aktif berlaku di database. Tombol tersembunyi tidak menggantikan pemeriksaan izin.

## Payload utama

### Program: POST dan PUT lengkap

Field wajib: name, status, dan akun PJ aktif. Buat/edit lengkap menggunakan assignee_ids berisi 1–30 UUID; pic_id tetap fallback kompatibilitas. pic opsional dan labelnya selalu diturunkan database dari nama akun. Field lain: description, pic_id, start_date, end_date, progress, budget, kind, progress_notes, next_milestone, milestone_date, assignee_ids. Lihat batas/enum di [DATA.md](DATA.md).

```json
{
  "name":"Kegiatan contoh",
  "kind":"Proker",
  "description":"Contoh dokumentasi, bukan data operasional.",
  "assignee_ids":["00000000-0000-0000-0000-000000000001"],
  "start_date":null,
  "end_date":null,
  "status":"Planning",
  "progress":null,
  "budget":null,
  "progress_notes":"",
  "next_milestone":"",
  "milestone_date":null
}
```

UUID contoh adalah placeholder, bukan akun hosted. Ganti dengan id dari GET /profiles; directory hanya mengembalikan profile aktif. Array kosong ditolak. Omission/null pada edit mempertahankan assignment yang sudah ada; program tanpa PJ harus diberi akun terlebih dahulu. Frontend mengirim daftar lengkap. Label pic bebas tidak memberikan akses. Gunakan body lengkap untuk PUT, bukan update parsial yang diasumsikan seperti PATCH.

Simpan memakai RPC save_program secara atomik. Mengosongkan catatan/agenda dari form mengirim string kosong; null/missing pada field kompatibilitas tertentu dapat mempertahankan nilai lama.

### Progres: PUT /programs/{id}/progress

```json
{
  "status":"Ongoing",
  "progress":35,
  "progress_notes":"Catatan progres contoh."
}
```

Hanya tiga field ini diterima. Progress dan catatan dapat null. Anggaran, nama, PJ, tanggal, actor, atau created_by tidak boleh ditambahkan. Endpoint bukan full program update. Respons adalah record hasil write; jangan mengasumsikan seluruh metadata join assignee selalu dikembalikan.

### Field resource lain

| Resource | Wajib | Opsional |
| --- | --- | --- |
| tasks | title, program_id, status, priority | description, assignee_id, due_date |
| finances | description, type, amount, category, transaction_date, status | program_id |
| inventory | name, category, quantity, status, condition | emoji, location |
| events | name, venue, start_date, end_date, status, permit_status | description, program_id |
| profiles/me | name | Tidak ada field role/active |

Tanggal berupa YYYY-MM-DD, amount/budget berupa angka JSON, dan ID berupa UUID. created_by/actor/timestamps dikelola server/database. Jangan mengambil input role atau ownership dari client.

## Dashboard

`GET /dashboard` memanggil `dashboard_stats()` dengan JWT pengguna. Kelompok field:

| Key | Isi |
| --- | --- |
| summary | Total/aktif/selesai program, recordedPrograms (progress bukan null), tugas, keuangan dan aset |
| programStatusCounts | Semua kegiatan, dikelompokkan kind/status/count; tidak dibatasi pratinjau |
| overview | proker, ukor, openTasks, overdueTasks |
| programFocus | id/name/kind/PIC/status/progress/notes/milestone; maksimum 30 |
| staffPerformance | id/name/programs, assignedTasks, completedTasks, reviewTasks, overdueTasks, updatesLast30Days, lastUpdate |
| monthly, inventory | Agregat keuangan bulanan dan status aset |
| upcomingMilestones, attentionTasks, upcomingPrograms | Ringkasan pendukung yang tetap tersedia, meski tidak semuanya dipasang di dashboard UI terbaru |

KPI memakai total global dan recordedPrograms; distribusi memakai programStatusCounts. Setiap item programs pada Staff memuat id/name/kind/status/progress langsung dari seluruh assignment, termasuk kegiatan di luar programFocus. Pratinjau maksimum 30 diberi label coverage dan link daftar lengkap; tidak dipakai sebagai sumber hitungan global.

## Kode status dan error handling

| HTTP | Arti |
| --- | --- |
| 400 | JSON/field/UUID/pagination/validasi input tidak sesuai |
| 401 | Belum login, token/sesi tidak valid |
| 403 | Origin tidak dipercaya, profile/role/ownership tidak berhak |
| 404 | Resource tidak ada, hasil write kosong, atau endpoint tidak tersedia |
| 405 | Method endpoint tidak didukung |
| 409 | Konflik data, termasuk FK yang mencegah penghapusan |
| 429 | Batas percobaan/request upstream tercapai; coba lagi kemudian |
| 500 | Error aplikasi tak terduga dengan pesan tersanitasi |
| 502/503 | Ketergantungan upstream tidak tersedia/response tidak valid, sesuai mapping adapter |

Frontend hanya mencoba refresh pada 401, bukan pada 403. Setelah gagal simpan, pertahankan form dan data sebelumnya; setelah berhasil, invalidasi daftar dan ringkasan. Jangan menampilkan error upstream mentah, token, atau password dalam log/UI.


## Ruang kerja program dan pelacakan tugas — 19 Oktober 2026

| Method | Path | Kontrak |
| --- | --- | --- |
| GET | /programs/{id} | Record program dengan assignee_ids; 404 untuk tidak ada/tidak dapat dibaca |
| GET | /programs/{id}/timeline?page=0&size=20 | data.items dan data.hasMore; gabungan audit progres dan catatan, terbaru dahulu |
| POST | /programs/{id}/notes | body, teks nonblank maksimum 2000; author/program/waktu dikelola server |
| GET | /programs/{id}/milestones?page=0&size=20 | data array, meta.hasMore; due_date asc null terakhir; filter status opsional Planned/Done |
| POST | /programs/{id}/milestones | title (1–160), status Planned/Done, due_date opsional |
| PUT | /programs/{id}/milestones/{milestoneId} | title/status/due_date lengkap; tidak dapat memindahkan program atau mengganti creator |
| DELETE | /programs/{id}/milestones/{milestoneId} | Agenda harus milik program pada URL |
| GET | /tasks/summary | Filter sama dengan /tasks; total/done/overdue/unassigned/statuses/workload tanpa pagination |
| PUT | /tasks/{id}/status | status, expected_updated_at opsional (ISO Instant); hanya status dipatch |

Timeline memakai id gabungan, kind=progress/note, actor_id, body, created_at, status/progress (null untuk catatan). Catatan append-only untuk client, bukan edit/hapus audit. Pagination 0–100000, size 1–100. Tanggal agenda yang belum diketahui tetap null. Endpoint anak memeriksa akses program sebelum query; RLS memeriksa lagi setiap row.

Filter tugas tambahan: program_id dan assignee_id harus UUID canonical; scope kosong/all, mine, overdue atau unassigned. mine memakai identitas terverifikasi; overdue berarti status bukan Done dan due_date sebelum hari ini Asia/Jakarta. Pencarian/filter dilakukan sebelum pagination. Summary memakai filter yang sama dan RLS; workload hanya untuk BPH, berisi assignee_id,total,done,open,overdue. Null assignee adalah kelompok belum ada PJ. Tidak mengarang skor atau menghitung total dari kartu yang sedang dimuat.

Tugas terkait program hanya dapat dibaca BPH/PJ. Tugas legacy tanpa program hanya BPH/creator/assignee. Tugas baru wajib berinduk. Mutasi Staff tetap creator/assignee dan hanya dapat memilih diri sendiri. Assignment baru/reassign harus akun aktif; untuk tugas terkait, akun harus BPH atau PJ program tersebut. Assignment tugas tidak otomatis memberi akses program. Relasi lama tidak ditulis ulang; perubahan status yang tidak mengubah relasi tetap dapat dilakukan pemilik yang berhak.

Status-only update menghindari penimpaan deskripsi/assignment saat drag/drop. Jika expected_updated_at sudah berubah, hasilnya 409 dan UI meminta muat ulang. Ini bukan optimistic locking menyeluruh untuk edit lengkap/agenda. Write gagal mempertahankan status lama; tombol muat ulang adalah read/invalidation, bukan mutasi bisnis.

Semua agregat dashboard sekarang mencakup row yang boleh dibaca caller: BPH seluruh departemen, Staff program yang ditangani dan tugas legacy tanpa induk miliknya. Status program, PJ dan detail tidak bocor melalui dashboard/riwayat/related tables. Directory nama akun aktif tetap tersedia untuk pilihan assignment.


## Status program dan akun sendiri — 19 Oktober 2026

Pilihan utama UI: **Planning / On Going / Finished**. Nilai API/database tetap Planning/Ongoing/Completed. Active lama ditampilkan On Going; GET /programs?status=Ongoing mencakup Ongoing dan Active sebelum pagination. Pada edit eksplisit Active disimpan sebagai Ongoing. Status lama Unspecified/Cancelled tetap terbaca agar tidak mengarang tahap kegiatan atau mengubah audit; hanya dipertahankan sebagai nilai lama, bukan pilihan utama baru. Label tanggal selesai dan status tugas/event tidak ikut diganti.

| Method | Endpoint | Input / hasil |
| --- | --- | --- |
| GET | /profiles/me/avatar | data.available; bila ada: url signed 900 detik, expiresIn |
| PUT | /profiles/me/avatar | image: base64 JPEG (bukan data URL), maksimal 512 KiB dan 1024 × 1024; hasil profile sendiri dengan avatar_path |
| DELETE | /profiles/me/avatar | Hapus pointer foto sendiri dan coba hapus objek lama; avatar_path null |
| POST | /auth/password | currentPassword (1–1024), newPassword (8–128); bearer dan Origin tepercaya wajib |

Upload aplikasi menerima JPG/PNG/WebP sampai 5 MiB, memotong tengah ke persegi 512 px dan mengirim JPEG ke API. Backend memeriksa format/dimensi sebelum decode penuh, mengodekan ulang JPEG tanpa metadata, dan memakai nama file UUID di folder akun. File input/nama path/userId tidak diterima dari client. request nonimage, oversize, atau properti tambahan ditolak. Tidak ada URL publik permanen; direktori memakai batch signed URL terautentikasi.

Foto berada di bucket private depor-avatars. Endpoint /me menandatangani foto sendiri; endpoint batch direktori menandatangani foto terkini anggota aktif menggunakan JWT pembaca, tanpa service role. Signed URL adalah akses sementara 15 menit; jangan disimpan sebagai URL permanen atau dicetak ke log. Profil Auth/session menambah avatar_path nullable, bukan image bytes atau signed token. UI cache akun tetap terisolasi.

Upload memakai objek versi baru. Profile pointer diubah dengan kondisi nilai lama; konflik menghasilkan 409 dan foto sebelumnya tetap menjadi sumber terkonfirmasi. File lama dihapus setelah pointer berhasil berubah. Kegagalan cleanup tidak membatalkan write profile yang sudah commit. Network outcome yang ambigu dapat meninggalkan objek privat yatim; jangan menghapus metadata storage.objects langsung untuk membersihkannya.

Password diverifikasi dengan sign-in internal menggunakan email dari Auth user terverifikasi, bukan input email/UUID. ID hasil verifikasi harus sama. Kemudian update Auth memakai session segar serta current_password. Respons sukses sama seperti sesi login, ditambah passwordChanged=true: accessToken di memori dan refresh cookie HttpOnly diganti. Password tidak masuk profile, database public, cache, localStorage atau log. Verifikasi gagal menghasilkan 400 PASSWORD_MISMATCH, bukan 401 yang memicu loop refresh. Policy password Supabase tetap berlaku. Session verifikasi sementara dibersihkan bila update gagal. Fitur ini bukan reset/invite akun lain.


## Foto direktori tim — 20 Oktober 2026

`GET /profiles` menambah `avatar_path` nullable pada field direktori yang sudah ada (tanpa email Auth). `GET /profiles/avatars?ids=<uuid>,<uuid>` menerima 1–100 UUID canonical; duplikat dideduplikasi. Memerlukan bearer dari akun aktif, BPH maupun Staff. API mencari pointer terkini pada profile aktif melalui PostgREST/RLS lalu melakukan satu request Storage batch sign. Input path/URL tidak diterima dari client. Akun tidak ditemukan/nonaktif/tanpa foto atau file hilang tidak mendapat entry.

```json
{
  "data": {
    "00000000-0000-0000-0000-000000000002": {
      "path": "00000000-0000-0000-0000-000000000002/00000000-0000-0000-0000-000000000003.jpg",
      "url": "https://<project>.supabase.co/storage/v1/object/sign/depor-avatars/<path>?token=<temporary>"
    }
  },
  "meta": { "expiresIn": 900 }
}
```

UUID invalid, list kosong/tidak diberikan atau >100 menghasilkan 400; tanpa autentikasi 401; akun nonaktif 403. Upstream gagal menghasilkan error (bukan sukses palsu). Foto per-file gagal diabaikan; UI memakai inisial. Signed URLs jangan dicetak ke log/disimpan permanen. Hanya foto terpilih yang dibagikan; URL yang sudah diterbitkan dapat berlaku hingga expiry. PUT/DELETE foto tetap di `/profiles/me/avatar`; tidak ada endpoint mutasi foto orang lain.


## Compact reads — 20 Oktober 2026

| GET endpoint | Response data | Izin |
| --- | --- | --- |
| `/dashboard/overview` | overview, enam field summary KPI, programFocus ≤30, programStatusCounts, staffPerformance chart | JWT + profile aktif; invoker/RLS |
| `/directory` | profiles aktif (id/name/role/active/avatar_path), programs (id/name/kind/assignee_ids) | Scope program BPH/PJ; tanpa description, budget, email Auth |
| `/tasks/board` | summary global sesuai filter, columns untuk lima status berisi items ≤12 dan hasMore | Izin task yang sama; workload hanya BPH |
| `/tasks/:id` | Satu record tugas penuh, termasuk description, atau 404 | RLS, tidak menambah izin edit |

Board menerima q, program_id, assignee_id, priority, status, scope seperti daftar lama; `pages=0,0,0,0,0` mengikuti Backlog/To Do/In Progress/Review/Done. Tepat lima integer 0–100000; parameter page/size ditolak 400. Setiap kolom memiliki page sendiri; mengganti filter mereset semua page. Summary menghitung seluruh hasil filter, bukan slice kartu. Description tidak dikirim pada board; Detail/Edit mengambil record lengkap dan Edit memaksa read baru agar tidak menimpa deskripsi kosong. Error read tidak membuka editor kosong.

Endpoint `/dashboard` tetap menyediakan kontrak finansial lama untuk Keuangan; `/dashboard/overview` tidak menghitung/mengirim finance, inventory, agenda atau teks panjang. Directory/board/overview masing-masing satu RPC dengan JWT caller, tanpa cache principal/data server, tanpa bypass RLS. Respons JSON ≥1024 byte dapat memakai gzip bila client mendukung; Cache-Control tetap no-store. Tidak ada auth/data dalam CDN publik.


## Tugas pelaksanaan Proker/UKOR — 20 Oktober 2026

POST /tasks dan PUT /tasks/{id} mewajibkan program_id berupa UUID selain title/status/priority; omission/null ditolak 400. Full edit record legacy sekaligus menautkan induknya. PUT /tasks/{id}/status tetap parsial sehingga status legacy tidak terblokir. Caller JWT/RLS dan aturan Staff self-assignment tidak berubah.

GET /tasks, /tasks/summary dan /tasks/board menerima program_scope kosong, linked atau unlinked, terpisah dari scope=unassigned (assignee kosong). Kedua summary menambahkan unlinked, hitungan record tanpa induk setelah semua filter; workload tetap hanya BPH. List/board memakai filter yang sama sebelum pagination. GET /directory menambahkan kind pada setiap program, tanpa request tambahan. RPC baru/yang diperbarui tetap SECURITY INVOKER dengan EXECUTE hanya authenticated.
