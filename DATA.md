# Data — Depor CS HUB

## Sumber kebenaran

Data operasional disimpan di PostgreSQL Supabase, bukan hardcoded di React. Konstanta label, enum, token CSS, serta fixture pengujian boleh ada di kode; identitas akun, assignment nyata dan isi keuangan bukan fixture public.

Dokumen ini menjelaskan schema dan migration pada backend. Migrasi dashboard_coverage diterapkan dan diverifikasi baca pada development tanggal 19 Oktober 2026; tidak mengubah record operasional.

## Entitas

| Tabel | Field utama | Fungsi |
| --- | --- | --- |
| `auth.users` | id dan kredensial yang dikelola Supabase | Identitas Auth; tidak diduplikasi ke tabel password public |
| `profiles` | id, name, role, active, avatar_path, created_at | UUID sama dengan Auth; nama, role lama, aktivasi |
| `programs` | id, name, kind, description, pic, pic_id, status, progress, progress_notes | Satu Proker atau UKOR |
| `program_assignees` | program_id, profile_id | Banyak PJ per kegiatan; primary key gabungan |
| `program_notes` | id, program_id, body, created_by, created_at | Pembaruan manual append-only oleh BPH/PJ |
| `program_milestones` | id, program_id, title, due_date, status, created_by, created_at, updated_at | Agenda/tahapan editable BPH/PJ |
| `program_updates` | id, program_id, actor_id, status, progress, notes, created_at | Riwayat perubahan progres yang dibuat trigger |
| `tasks` | id, title, description, program_id, assignee_id, status, priority, due_date | Tugas kecil pelaksanaan Proker/UKOR; null hanya legacy |
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

Dashboard memakai summary.recordedPrograms dan programStatusCounts untuk hitungan global. Assignment Staff membawa status/progress sendiri. programFocus tetap pratinjau maksimal 30 kegiatan; UI memberi coverage dan akses daftar lengkap saat terpotong. Null progres tidak dihitung sebagai tercatat; 0 dihitung. Task dan assignment diagregasi terpisah untuk menghindari penggandaan hitungan. Tugas tanpa assignee tidak dikreditkan ke individu. Cutoff tenggat dan milestone memakai Asia/Jakarta; update memakai jendela 30 hari. Hanya transaksi approved masuk angka realisasi keuangan.

## Migrasi dan data awal

Urutan migration pada repository backend:

1. `20261008154150_core.sql`
2. `20261008154204_events_profiles.sql`
3. `20261009173515_program_workspace.sql`
4. `20261019042816_dashboard_coverage.sql`
5. `20261019074232_program_hub_access.sql`
6. `20261019100116_profile_avatars.sql`
7. `20261020025834_team_avatar_read.sql`
8. `20261020032751_performance_reads.sql`

Jangan mengulang migration yang sudah tercatat. `supabase/seed.sql` adalah demo lama dan dapat menggandakan data bila diulang; bukan sumber dataset departemen. Harness dan fixture SQL hanya untuk database disposable. Dokumen ini tidak membawa password, UUID roster nyata, link bukti keuangan privat, atau salinan database.


## Privasi ruang kerja

programs, program_assignees, program_updates, program_notes dan program_milestones hanya terlihat bagi BPH aktif atau PJ dengan UUID pada program_assignees. Helper private.can_access_program memakai auth.uid dan role aktif di schema private, security definer dengan search_path kosong; fungsi ini memutus siklus RLS program/assignment. RPC publik tetap security invoker dan memakai JWT caller.

Tugas terkait serta finance/event terkait memakai batas program yang sama. Tugas tanpa program privat untuk creator/assignee, kecuali BPH. Menghapus assignment PJ mencabut akses terkait meski pernah membuat catatan/tugas. Program tanpa assignment hanya terlihat BPH. Tidak ada backfill otomatis dari teks PIC.

save_program mewajibkan 1–30 akun aktif pada assignment baru/edit lengkap, memvalidasi semuanya dan menghasilkan label PIC dari nama akun. PIC text hanya kompatibilitas tampilan (maksimum 120 karakter); UUID tetap otoritatif. Assignment baru tugas terkait memerlukan akun aktif yang BPH atau PJ program; data relasi historis tidak dibersihkan/mengganti dirinya sendiri.

Milestone: title 1–160 nonblank, due_date nullable, status Planned/Done. Note: body 1–2000 nonblank. Keduanya memakai FK program cascade; created_by FK profile restrict dan default auth.uid. Update milestone dibatasi kolom title/due_date/status. Notes tidak dapat diedit/dihapus client. Audit progres tetap hanya trigger; catatan manual tidak dihitung sebagai pembaruan progres dalam performa Staff.

Index baru meliputi task status/due_date dan program/tanggal serta creator pada agenda/catatan. task_summary menghitung seluruh hasil filter di database; daftar tetap paginated. Tidak ada pengisian persentase otomatis dari jumlah tugas.


## Foto profil dalam bucket privat

profiles.avatar_path nullable dibatasi ke `<profile UUID>/<object UUID>.jpg`. Update hanya diri sendiri melalui policy profile aktif yang sudah ada. Kolom role/active tetap tidak dapat ditulis client. Directory hanya mengembalikan field yang dipilih API; identity Auth/session dapat memuat pointer sendiri.

Bucket depor-avatars: private, maksimal 524288 byte per objek, MIME image/jpeg. Empat policy storage.objects mencakup SELECT/INSERT/UPDATE/DELETE untuk authenticated dengan profile aktif dan nama folder UUID miliknya. Policy SELECT tambahan 20 Oktober membolehkan pembaca aktif melihat hanya avatar_path terkini profile aktif. Policy INSERT/UPDATE/DELETE tetap hanya owner; BPH tidak mendapat bypass mutasi. Bytes diunggah/dihapus melalui Storage API dengan JWT pengguna; migration hanya membuat bucket/config/policies. storage.objects tidak dijadikan tempat menyimpan metadata file rekaan pada hosted.

Tidak ada backfill foto atau perubahan password/akun saat migration. Fixture schema Storage pada tests/bootstrap.sql hanya stand-in lokal untuk policy, bukan Storage service. Signed URL berumur 900 detik dan dapat tetap berlaku sampai kedaluwarsa; bukan URL publik permanen. File versi lama dibersihkan setelah update pointer terkonfirmasi; request terputus dapat meninggalkan objek privat yatim untuk pemeriksaan operator.

Status UI memakai Planning, On Going dan Finished; enum API/database lama dipertahankan. Active tetap satu kelompok dengan Ongoing. Tidak ada UPDATE massal status/audit/progres.


Migrasi `team_avatar_read` menambah hanya policy SELECT `depor_avatar_department_read`. Predicate `(select private.app_role()) is not null` memverifikasi pembaca; subquery profile aktif membatasi nama objek pada pointer terkini. Bucket tetap private. Tidak ada backfill akun, assignment, path foto atau password; fixture metadata Storage hanya lokal disposable.


### RPC pembacaan ringkas

Migration performance_reads hanya membuat workspace_directory(), task_board(jsonb, integer[]) dan dashboard_overview(), semuanya STABLE SECURITY INVOKER, search_path kosong, EXECUTE hanya authenticated. Tidak mengubah tabel, akun, assignment, policy, password atau business rows. Board mempunyai lima slice maksimum 12 row, pagination global terfilter dan summary parity dengan task_summary. Dashboard compact menjaga total, null progress, distribusi status, serta kontribusi Staff; fungsi dashboard_stats lama dipertahankan untuk Keuangan.


## Parent tugas wajib — 20 Oktober 2026

Tugas baru memerlukan tasks.program_id yang menunjuk satu Proker/UKOR. Trigger invoker private.require_task_program menolak INSERT dengan program null serta UPDATE yang melepas relasi non-null menjadi null (23514). FK RESTRICT tetap melindungi program dengan tugas. Kolom masih nullable untuk mempertahankan record legacy tanpa backfill rekaan; perubahan status parsial legacy tetap diizinkan, sedangkan full update API mewajibkan induk.

RLS program/tugas dan guard akun PJ yang sudah ada tidak diperluas. workspace_directory menambahkan kind pada opsi program. task_summary dan task_board menambahkan unlinked (jumlah program_id null sesuai filter/RLS), serta filter program_scope linked/unlinked. Status, riwayat, akun, password dan data bisnis tidak diubah oleh migration. Dashboard tetap menghitung semua tugas yang berhak dilihat, termasuk legacy sampai ditautkan.


## Program goals & agenda — 21 October 2026

program_goals stores explicit targets and nullable measured actuals with period/date/evidence; program access RLS applies. Imported program_notes retain source/occurred_on separately from created_at. Milestones store local WIB times and tentative/confirmed certainty. Private operational imports remain outside Git. See [data rules](docs/program-goals-schedule.md).


## Finance records and reimbursement — 22 October 2026

Finance supports record_kind (transaction, funding_request, reimbursement_request), reimbursement_status (unknown, not_requested, pending, paid, not_required), notes (2000 chars), and source_ref (300 chars). Approval status remains pending/approved/rejected and is independent of reimbursement. Unknown transaction dates stay null. Only a transaction with a known date may be approved and counted by existing dashboard totals. A payment to a merchant is not evidence that the department reimbursed the purchaser. Requests should be reconciled with their underlying transaction before conversion to avoid duplicate expenses. No source data or bank details belong in this public repository.
