# API — Depor CS HUB

Kontrak ini berasal dari controller/DTO backend pada commit `445813b`. Ini API aplikasi yang sudah ada, meskipun tidak mengambil konten publik dari API pihak ketiga.

## Base URL, format, dan sesi

- Local: `http://localhost:8080/api`
- Target hosted yang dipakai proyek: `https://depor-cs-hub-api.vercel.app/api`
- Content-Type: `application/json`.
- Endpoint data memerlukan `Authorization: Bearer <access-token>` dari login.
- Cookie refresh tidak mengautentikasi endpoint data. Browser memakai credentials include untuk request Auth.
- POST Auth memerlukan header Origin yang persis sesuai konfigurasi, termasuk dari client command line.

Response berhasil membungkus hasil dalam `data`. List menambahkan `meta`:

```json
{"data":[],"meta":{"page":0,"size":50}}
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

Login/refresh dapat diakses tanpa bearer, tetapi Origin tetap divalidasi. Tidak ada endpoint register, invite, reset password, atau administrasi role dalam controller ini. Gunakan pengelolaan Auth yang terpisah bila diperlukan.

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

Tidak ada GET /resource/{id}, PATCH aplikasi, search global, export, atau upload pada baseline controller ini. Backend memakai PATCH ke PostgREST secara internal; itu bukan method API publik Java.

List menerima page 0–100000, default 0; size 1–100, default 50. Frontend biasanya meminta size 100. Urutan events: start_date lalu id; resource lain: created_at desc lalu id. API belum menerima query pencarian/status/sort khusus. Filter UI berlaku pada halaman yang sudah dimuat.

Create menghasilkan 201. Edit/delete berhasil menghasilkan 200. Delete mengembalikan `{"data":{"deleted":true}}`. Operasi yang tidak menemukan record/hasil write kosong tidak boleh dianggap sukses.

## Izin

| Operasi | BPH | Staff aktif |
| --- | --- | --- |
| Membaca data departemen | Ya | Ya |
| CRUD program lengkap, finance, inventory, events | Ya | Tidak |
| Update progres program | Ya | Hanya PJ yang terhubung UUID |
| Membuat tugas | Dapat memilih assignee | Hanya untuk diri sendiri; tanpa assignee API mengisinya ke diri sendiri |
| Edit/hapus tugas | Semua | Creator atau assignee; aturan reassign tetap dibatasi |
| Edit nama profile | Diri sendiri | Diri sendiri |
| Mengubah role/active | Tidak lewat API ini | Tidak |

RLS dan validasi profile aktif berlaku di database. Tombol tersembunyi tidak menggantikan pemeriksaan izin.

## Payload utama

### Program: POST dan PUT lengkap

Field wajib: name, pic, status. Field lain: description, pic_id, start_date, end_date, progress, budget, kind, progress_notes, next_milestone, milestone_date, assignee_ids. Lihat batas/enum di [DATA.md](DATA.md).

```json
{
  "name":"Kegiatan contoh",
  "kind":"Proker",
  "description":"Contoh dokumentasi, bukan data operasional.",
  "pic":"PJ kegiatan",
  "pic_id":null,
  "assignee_ids":[],
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

Array PJ yang tidak kosong harus berisi UUID profile aktif yang benar-benar ada. Contoh di atas sengaja tidak menunjuk akun nyata. `[]` menghapus assignment pada edit; tidak mengirim/null assignment mempertahankan assignment yang ada, kecuali perubahan pic_id lama yang memicu kompatibilitas. Frontend mengirim daftar lengkap. Gunakan body lengkap untuk PUT, bukan update parsial yang diasumsikan seperti PATCH.

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
| tasks | title, status, priority | description, program_id, assignee_id, due_date |
| finances | description, type, amount, category, transaction_date, status | program_id |
| inventory | name, category, quantity, status, condition | emoji, location |
| events | name, venue, start_date, end_date, status, permit_status | description, program_id |
| profiles/me | name | Tidak ada field role/active |

Tanggal berupa YYYY-MM-DD, amount/budget berupa angka JSON, dan ID berupa UUID. created_by/actor/timestamps dikelola server/database. Jangan mengambil input role atau ownership dari client.

## Dashboard

`GET /dashboard` memanggil `dashboard_stats()` dengan JWT pengguna. Kelompok field:

| Key | Isi |
| --- | --- |
| summary | Total/aktif/selesai program, tugas, keuangan dan aset |
| overview | proker, ukor, openTasks, overdueTasks |
| programFocus | id/name/kind/PIC/status/progress/notes/milestone; maksimum 30 |
| staffPerformance | id/name/programs, assignedTasks, completedTasks, reviewTasks, overdueTasks, updatesLast30Days, lastUpdate |
| monthly, inventory | Agregat keuangan bulanan dan status aset |
| upcomingMilestones, attentionTasks, upcomingPrograms | Ringkasan pendukung yang tetap tersedia, meski tidak semuanya dipasang di dashboard UI terbaru |

UI tidak boleh menyamakan jumlah pada programFocus dengan total seluruh database jika dataset melewati batas. Array programs di Staff berisi identitas kegiatan; statusnya saat ini dipetakan UI dari programFocus. Ini perlu perhatian saat daftar melebihi batas tersebut.

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
