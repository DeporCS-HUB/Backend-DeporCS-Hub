# Product Requirements — Depor CS HUB

Tanggal dokumentasi: 19 Oktober 2026. Baseline kode: frontend `cae6348`, backend `445813b`. Status fitur di bawah berasal dari kode tersebut; bukan audit ulang isi database atau uptime hari ini.

## Tujuan

Menyediakan satu tempat untuk memantau Proker dan UKOR, membagi tanggung jawab, memperbarui progres, serta mencatat tugas, keuangan, inventaris, dan kegiatan. Dashboard harus membantu melihat kondisi pekerjaan dalam beberapa detik.

Masalah utama yang ditangani: informasi tersebar, progres sulit dibandingkan, tanggung jawab tidak jelas, dan teks panjang membuat dashboard melelahkan untuk dibaca.

## Pengguna dan akses

| Pengguna | Kebutuhan utama |
| --- | --- |
| BPH | Membagi PJ, mengelola data kegiatan, meninjau tugas dan kontribusi Staff |
| Staff | Melihat pembagian kerja, mengedit progres kegiatan yang menjadi tanggung jawabnya, menjalankan tugas |
| Pengunjung belum login | Hanya akses layar login; data departemen tidak terbuka |

Tidak ada peran produk Member terpisah. Nilai `member` dalam database tetap dipakai sebagai role lama untuk Staff.

## Cakupan dan acceptance criteria

| ID | Kebutuhan | Acceptance criteria | Baseline |
| --- | --- | --- | --- |
| FR-01 | Login dan sesi | Profile aktif diverifikasi; token akses di memori; refresh memakai cookie HttpOnly; sesi invalid kembali ke login | Ada |
| FR-02 | KPI dan chart | Empat KPI; chart persentase Proker/UKOR, metrik Staff, dan distribusi status; data kosong berbeda dari 0 | Ada |
| FR-03 | Kelola kegiatan | BPH dapat CRUD, memilih beberapa PJ, jenis Proker/UKOR, agenda, tanggal, dan anggaran opsional | Ada |
| FR-04 | Update progres | BPH atau PJ Staff terkait dapat mengubah status, persentase, dan catatan; Staff lain ditolak | Ada |
| FR-05 | Papan tugas | Status kanban, prioritas, assignee, tenggat opsional; izin creator/assignee/BPH dijaga | Ada |
| FR-06 | Keuangan | Catat income/expense; hanya transaksi approved masuk realisasi | Ada |
| FR-07 | Inventaris dan kegiatan | Data inventaris serta jadwal/venue/izin dicatat manual oleh BPH | Ada |
| FR-08 | Tim dan profile | Direktori tim read-only; pengguna dapat mengubah nama sendiri, bukan role/aktivasi | Ada |
| FR-09 | Detail dan responsivitas | Deskripsi tidak ditumpuk di daftar; dialog dibatasi tinggi, bisa digulir, Escape menutup, fokus kembali ke pemicu | Ada |
| FR-10 | Penyesuaian referensi visual | Susunan dashboard/tabel mengikuti prinsip referensi terlampir, dengan identitas Depor dan data sebenarnya | Ada pada iterasi 19 Oktober 2026 |

## KPI yang dipakai

- **Kegiatan selesai:** jumlah status Completed dibanding total kegiatan dari ringkasan server.
- **Progres tercatat:** kegiatan dengan persentase bukan null dibanding seluruh kegiatan yang terlihat oleh pengguna, bukan hanya pratinjau chart.
- **Tugas selesai:** Done dibanding Done + seluruh tugas terbuka.
- **Lewat tenggat:** tugas bukan Done dengan tanggal tenggat sebelum tanggal saat ini di Asia/Jakarta.

Grafik Staff menyediakan status kegiatan yang ditugaskan, tugas selesai dibanding tugas ditetapkan, dan update progres dalam 30 hari. Jumlah update bukan ukuran mutu kerja. Tanpa tugas berarti belum ada dasar penilaian, bukan performa buruk.

## Alur utama

1. Pengguna login dan backend memverifikasi profile aktif.
2. Dashboard memuat ringkasan, lalu pengguna memilih Proker/UKOR atau metrik Staff.
3. Pengguna membuka Program, mencari kegiatan, lalu memilih Detail atau Update progres.
4. Simpan berhasil memperbarui data dan menginvalidasi cache daftar/dashboard.
5. Simpan gagal menampilkan error dan mempertahankan nilai server yang sudah terkonfirmasi.

## Kriteria UX

Nama, status, angka, dan tindakan utama terlihat tanpa membuka paragraf. Header dashboard ringkas; tidak perlu hero pemasaran besar. Pada ponsel, menu dapat dibuka, tombol dapat dijangkau, dan halaman tidak melebar keluar viewport. Warna chart mempunyai label/angka sehingga informasi tidak bergantung pada warna.

## Batas dan non-goals saat ini

Belum tersedia: undangan akun, pengelolaan role lewat UI, absensi, unggahan LPJ/lampiran, pengiriman notifikasi, izin venue eksternal, atau preferensi pengguna. Permintaan menyediakan LPJ tidak berarti fitur upload sudah ada. Pencarian tiap resource sudah memakai server sebelum pagination; pencarian lintas-resource, export, foto profil sendiri tersedia; avatar direktori orang lain dan notifikasi belum tersedia.

## Ukuran keberhasilan

Uji pengguna dapat menemukan progres sebuah kegiatan, mengidentifikasi data belum diisi, dan menyimpan progres dengan akun yang berhak. Tidak ada target conversion, pertumbuhan, atau tren numerik rekaan. Pengukuran waktu penggunaan dan kepuasan belum dikumpulkan.


## Iterasi workspace program dan pelacakan BPH — 19 Oktober 2026

- Setiap Proker/UKOR memiliki URL /programs/:id dengan ringkasan, tugas terkait, timeline pembaruan dan agenda editable.
- Hanya BPH aktif dan akun PJ yang terhubung UUID dapat membaca program dan record terkait; perubahan PJ mencabut akses. BPH mengelola profil/PJ. Tugas mandiri privat bagi owner/assignee, BPH dapat memantau semuanya.
- Pemilihan PJ/assignee berasal dari akun aktif; teks nama bebas tidak digunakan untuk otorisasi. Assignment tugas tidak memberikan akses program.
- BPH memantau total/status/terlambat/belum ada PJ serta beban per akun dari seluruh hasil filter. Papan/daftar, drag/drop dan kontrol status keyboard, filter program/PJ/prioritas/pencarian/tugas saya, serta pagination per kolom harus tersedia.
- Timeline menggabungkan audit progres nyata dan catatan manual. Agenda mendukung create/edit/complete/reopen/delete dengan tanggal kosong tetap null. Tidak menambahkan data sejarah, lampiran atau komentar tugas palsu.
- UI tetap navy dengan brand biru/merah; status memakai warna semantik. Desktop/375 px tidak overflow halaman; board boleh scroll horizontal dalam wadah. Deskripsi panjang tidak memperpanjang halaman.
- Tidak menambah provider, layanan berbayar atau reset akun/data. LPJ dan informasi belum tersedia tetap menunggu sumber pengguna.


## Iterasi status dan akun sendiri — 19 Oktober 2026

- Pilihan status program Planning / On Going / Finished, konsisten pada form/filter/badge/chart. Active lama termasuk On Going. Nilai belum diketahui dan audit historis tidak ditulis ulang.
- Pengguna aktif dapat memilih, mempratinjau, menyimpan, mengganti dan menghapus foto sendiri; JPG/PNG/WebP maksimal 5 MiB pada browser, diproses menjadi foto persegi. Header dan Settings memakai foto terkonfirmasi, dengan inisial saat kosong/gagal dimuat.
- Pengguna dapat mengganti password sendiri dengan password saat ini, password baru dan konfirmasi. Minimal delapan karakter, tidak sama dengan sebelumnya; policy Auth tetap berlaku. Kesalahan tidak menghapus input/sesi. Sukses membersihkan input dan mempertahankan login melalui session segar.
- Foto privat, akun lain/BPH tidak dapat mengubah atau mengambil foto caller lewat endpoint ini. Tidak ada reset password massal atau edit role/active. Tidak menambah layanan berbayar.
