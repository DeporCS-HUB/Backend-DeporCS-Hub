# Data — Depor CS HUB

## Sumber kebenaran

Data operasional disimpan di PostgreSQL Supabase, bukan hardcoded di React. Konstanta label, enum, token CSS, serta fixture pengujian boleh ada di kode; identitas akun, assignment nyata dan isi keuangan bukan fixture public.

Dokumen ini menjelaskan schema dari tiga migration pada backend. Tidak ada query atau perubahan database hosted yang dijalankan untuk pembuatan paket dokumentasi ini.

## Entitas

| Tabel | Field utama | Fungsi |
| --- | --- | --- |
| `auth.users` | id dan kredensial yang dikelola Supabase | Identitas Auth; tidak diduplikasi ke tabel password public |
| `profiles` | id, name, role, active, created_at | UUID sama dengan Auth; nama, role lama, aktivasi |
| `programs` | id, name, kind, description, pic, pic_id, status, progress, progress_notes | Satu Proker atau UKOR |
| `program_assignees` | program_id, profile_id | Banyak PJ per kegiatan; primary key gabungan |
| `program_updates` | id, program_id, actor_id, status, progress, notes, created_at | Riwayat perubahan progres yang dibuat trigger |
| `tasks` | id, title, description, program_id, assignee_id, status, priority, due_date | Tugas yang dapat berdiri sendiri atau terhubung kegiatan |
| `finances` | id, description, program_id, type, amount, category, transaction_date, status | Catatan pemasukan/pengeluaran |
| `inventory` | id, name, category, quantity, status, condition, emoji, location | Jumlah aset per record; emoji lama tidak ditampilkan |
| `events` | id, name, description, program_id, venue, start_date, end_date, status, permit_status | Jadwal dan pencatatan izin venue manual |

`programs` juga mempunyai start_date, end_date, budget, next_milestone, milestone_date, created_by, created_at, updated_at. Tasks/finances/inventory/events mempunyai created_by dan timestamps. `profiles` tidak mempunyai updated_at dalam baseline ini. `program_assignees` tidak mempunyai field role atau audit buatan sendiri.

## Hubungan dan penghapusan

| Hubungan | Perilaku |
| --- | --- |
| profiles.id → auth.users.id | Auth dihapus: profile ikut terhapus |
| program_assignees → programs/profiles | Record induk dihapus: assignment ikut terhapus |
| programs.pic_id → profiles | PIC lama dihapus: pic_id menjadi null |
| program_updates.program_id → programs | Kegiatan dihapus: riwayat terkait ikut terhapus |
| program_updates.actor_id → profiles | Actor dihapus: actor_id null; riwayat tetap ada selama kegiatan ada |
| tasks/finances/events.program_id → programs | Referensi membatasi penghapusan kegiatan |
| tasks.assignee_id → profiles | Assignee dihapus: hubungan menjadi null |
| created_by → profiles | FK default restrict/no-action; penghapusan profile dapat terhalang record yang dibuat |

Audit bersifat append-only untuk client, bukan jaminan penyimpanan selamanya setelah induknya dihapus. Gunakan kebijakan retensi baru bila membutuhkan arsip permanen.

## Role dan otorisasi

| Label produk | `departmentRole` dari backend | `profiles.role` lama |
| --- | --- | --- |
| BPH | bph | staff atau admin |
| Staff | staff | member |

`active=false` menolak akses operasional. Akun baru menerima profile inactive/member sampai aktivasi terpercaya. Metadata yang dapat diedit pengguna tidak memberikan hak akses. Role/active tidak dapat diedit melalui form profile.

## Jenis, status, dan batas

| Field | Nilai / batas |
| --- | --- |
| programs.kind | Proker, UKOR |
| programs.status | Planning, Ongoing, Active, Completed, Cancelled, Unspecified |
| programs.progress | integer 0–100 atau null |
| tasks.status | Backlog, To Do, In Progress, Review, Done |
| tasks.priority | Low, Medium, High |
| finances.type / status | income atau expense; pending, approved, rejected |
| inventory.status | Available, Borrowed, Maintenance, Lost |
| inventory.condition | Baik, Perawatan, Rusak |
| events.status | Planning, Confirmed, Completed, Cancelled |
| events.permit_status | Not required, Pending, Approved, Rejected |
| Deskripsi dan catatan progres | Maksimum 2.000 karakter |
| Nominal | Numeric(15,2); transaksi > 0, budget ≥ 0 atau null |
| Quantity | Integer 0–1.000.000 |
| Assignment input | Maksimum 30 UUID aktif per program |

Nama program/task/event/inventaris maksimum 160 karakter; nama profile dan PIC text maksimum 120. API juga menolak properti yang tidak dikenal.

## Null dan integritas

- Progress null = belum dilaporkan; 0 = sudah dilaporkan dan nilainya nol.
- Program start/end/budget dan tanggal milestone boleh kosong. Jika kedua tanggal periode diisi, akhir tidak boleh sebelum awal.
- Event wajib memiliki start/end yang valid; tasks.due_date opsional.
- Tidak mengetahui tanggal bukan alasan mengisi tanggal hari ini. Jangan memakai tanggal bukti pengeluaran sebagai deadline tugas.
- `assignee_ids` pada response API diturunkan dari `program_assignees`; field tersebut bukan kolom array pada programs.
- `pic` adalah label manusia; izin PJ memakai UUID assignment. `pic_id` dipertahankan untuk kompatibilitas, bukan satu-satunya sumber multiple PJ.
- Riwayat hanya menghitung perubahan nyata oleh pengguna terautentikasi. Import operator tidak diklaim sebagai kontribusi Staff.

UI mengisi 100% ketika menyimpan status Completed. Ini perilaku form saat ini, bukan constraint database yang memaksa semua writer melakukan hal yang sama. Bila perlu invariant global, buat perubahan terpisah dan uji seluruh writer.

## Agregasi

Dashboard membedakan total ringkasan server dari daftar chart maksimal 30 kegiatan. Task dan assignment diagregasi terpisah untuk menghindari penggandaan hitungan. Tugas tanpa assignee tidak dikreditkan ke individu. Cutoff tenggat dan milestone memakai Asia/Jakarta; update memakai jendela 30 hari. Hanya transaksi approved masuk angka realisasi keuangan.

## Migrasi dan data awal

Urutan migration pada repository backend:

1. `20261008154150_core.sql`
2. `20261008154204_events_profiles.sql`
3. `20261009173515_program_workspace.sql`

Jangan mengulang migration yang sudah tercatat. `supabase/seed.sql` adalah demo lama dan dapat menggandakan data bila diulang; bukan sumber dataset departemen. Harness dan fixture SQL hanya untuk database disposable. Dokumen ini tidak membawa password, UUID roster nyata, link bukti keuangan privat, atau salinan database.
