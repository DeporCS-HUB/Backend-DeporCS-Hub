# Design — Depor CS HUB

## Arah visual

Referensi: `image.png` yang dilampirkan pengguna pada 19 Oktober 2026. Gambar memperlihatkan dashboard gelap dengan sidebar ringkas, kumpulan KPI, chart batang, chart donut, serta tabel berisi toolbar. Gambar adalah referensi visual, bukan screenshot implementasi Depor dan bukan sumber angka operasional.

Untuk aplikasi ini, istilah hero berarti bagian atas dashboard: judul, sapaan pendek, konteks tanggal/role, lalu KPI. Tidak diperlukan hero landing page besar.

## Penerjemahan referensi

| Elemen pada gambar | Penerapan untuk Depor |
| --- | --- |
| Background hitam dan kartu charcoal | Pertahankan kontras antarlapisan dan border tipis |
| Sidebar ikon yang ramping | Navigasi label + ikon di desktop; label tetap dapat diakses saat collapse |
| KPI berukuran besar | Empat angka utama dengan label pendek; KPI pertama putih dengan teks gelap; denominator memakai total global |
| Chart batang dominan | Bandingkan progres kegiatan, bukan memajang paragraf catatan |
| Chart donut kategori | Donut SVG native dengan total global, angka selesai di tengah dan legend berangka |
| Toolbar dan tabel rapat | Filter dekat data; kolom hanya informasi utama; deskripsi lewat Detail |
| Avatar, notifikasi, pencarian global | Tidak dianggap fitur tersedia jika belum diimplementasikan |

Warna biru, oranye, dan hijau pada contoh menunjukkan hierarki visual. Warna identitas global Depor tetap mengikuti palet yang sudah ditetapkan. Referensi komposisi tidak mengganti seluruh palet aplikasi secara diam-diam.

## Token warna utama

```css
:root {
  --black: #010102;
  --background: #09090B;
  --surface: #171717;
  --surface-light: #312F30;
  --navy: #1E2630;
  --dark-red: #5F3031;
  --dusty-red: #705657;
  --gray: #43474D;
  --light-gray: #B4B2B5;
  --white: #F5F6F7;
}
```

Background: `--background`. Panel: `--surface`. Border: `--surface-light`. Navigasi aktif: `--navy`. Aksi utama: merah gelap/dusty red. Teks primer: putih; teks sekunder: light gray. Hindari gradient dekoratif, emoji stiker, glow, dan warna acak per kartu.

## Tata letak

**Dashboard desktop:** header ringkas; empat KPI dalam satu baris; dua panel chart sejajar; distribusi status di bawah. Chart kegiatan dibatasi 320 px dan Staff 360 px dengan scroll internal berfokus keyboard. Jangan menambahkan kembali tabel tanggung jawab panjang pada dashboard.

**Dashboard mobile:** KPI menjadi dua kolom; chart bertumpuk satu kolom. Label, nilai, selector, dan legend tetap terbaca pada lebar 375 px. Gunakan min-width: 0 untuk grid dan pemotongan label panjang dengan nama lengkap tetap tersedia secara aksesibel.

**Program:** nama/jenis, PJ, progres, tanggal agenda, tindakan. Deskripsi, catatan panjang, anggaran dan rincian agenda muncul dalam Detail. Pada mobile, baris menjadi kartu ringkas dengan tombol terlihat tanpa scroll horizontal.

**Tugas:** kartu menampilkan judul, prioritas, tenggat/status, serta Detail. Deskripsi sumber yang panjang tidak dirender penuh pada kartu.

## Tipografi dan ukuran

Gunakan system sans-serif agar tidak menambah request font. Ukuran baseline: judul halaman sekitar 28–36 px; angka KPI 26–30 px; isi 13–14 px; label chart 11–12 px. Gunakan skala jarak konsisten 4, 8, 12, 16, 24, 32 px. Radius panel kecil sekitar 6 px; bayangan tidak menjadi elemen utama.

Angka memakai tabular numerals. Paragraf hanya muncul di tempat yang memang membutuhkan bacaan: Detail, form bantuan yang relevan, dan error. Hindari mengulang label Belum diisi dua kali pada satu bidang.

## Aturan chart

1. Persentase memakai sumbu 0–100 yang tetap. Bar 0% sah dan berbeda dari data null.
2. Null memakai garis/track putus-putus serta tanda em dash. Jangan mengisi bar penuh untuk membuat data kosong terlihat aktif.
3. Status Selesai/Berjalan/Rencana/Belum diisi/Batal memakai label dan legend; pola arsiran membantu membedakan unknown.
4. Metrik Staff dapat diganti tanpa memuat seluruh halaman. Gunakan judul/denominator sesuai metrik.
5. Jangan memakai rata-rata yang menutup jumlah kegiatan belum dilaporkan. Tampilkan coverage bila kelak menambah agregat baru.
6. Jangan menampilkan persentase perubahan bulanan atau tren panah seperti contoh bila data pembanding tidak tersedia.

## Dialog, form, dan aksesibilitas

Detail memiliki judul, tombol Tutup, tinggi maksimum sekitar 80dvh (85dvh pada mobile), dan konten yang dapat digulir. Fokus masuk ke dialog, tetap di dalam dialog, Escape menutup, lalu fokus kembali ke pemicu. Area scroll dapat menerima fokus keyboard. Teks/URL panjang dapat membungkus tanpa membuat dialog melebar.

Gunakan label pada input, fokus keyboard terlihat, urutan heading jelas, dan angka/teks alternatif untuk chart. Targetkan kontras WCAG AA saat mengubah warna. Tombol ikon memiliki accessible name. Error tidak menghapus input; tombol simpan dinonaktifkan selama write.

## Pemeriksaan desain

Periksa desktop 1440 px, tablet 768 px dan mobile 375 px dengan data panjang, tidak ada data, null, 0, 100, dan error. Pastikan isi Detail tidak memperpanjang halaman di belakangnya. Penyesuaian visual berikutnya harus mempertahankan fungsi dan izin yang sudah berjalan.


## Halaman program dan papan tugas — 19 Oktober 2026

Program memiliki header ringkas dengan jenis/status/akses dan aksi. Empat tab: Ringkasan, Tugas, Timeline, Agenda. Ringkasan dua kolom desktop: progres dan hitungan tugas di kiri, akun PJ dan agenda terdekat di kanan. Agenda Planned baru mendapat prioritas pada ringkasan; agenda lama pada profil tetap diberi label sumber. Catatan/description maksimal tiga baris dengan akses dialog lengkap. Mobile menumpuk panel.

Papan tugas memakai toolbar pencarian, refresh, Papan/Daftar dan create; filter akun/program/prioritas/tugas saya; KPI actionable dengan label sesuai filter. Ringkasan beban per akun berada dalam disclosure. Setiap kolom mempunyai count global dan pagination sendiri; empty/loading/error per kolom tidak menghilangkan filter. Status tidak hanya warna: label, angka, dan select tersedia. Kartu menampilkan prioritas, judul terbatasi, program, foto profil akun (fallback inisial), nama PJ, tenggat, status dan aksi; tidak menggunakan foto/stiker generatif.

Drag/drop desktop adalah tambahan, select status tetap tersedia untuk keyboard/mobile. Deadline terlambat merah, Done hijau, berjalan biru, Review kuning. Board horizontal scroll di dalam kontainer, bukan overflow viewport; pada 375 px satu kartu/kolom tetap terbaca. Muat ulang menyediakan read terbaru, tanpa mengklaim Realtime.

Kolom papan mempunyai tinggi terbatasi dan area kartu scroll internal. Dua belas kartu di satu status tidak memanjangkan seluruh halaman. Header hitungan dan kontrol pagination tetap di luar area scroll.


## Pengaturan akun dan status

Status program utama tampil Planning / On Going / Finished. Warna semantik tetap kuning/biru/hijau; nilai legacy tidak diberi progres rekaan. Dropdown baru menawarkan tiga status, nilai Unspecified/Cancelled lama hanya placeholder yang dipertahankan saat edit record lama.

Settings dua kolom desktop: foto di kiri, nama/password di kanan; mobile ditumpuk tanpa overflow viewport. Pilih file tidak otomatis menyimpan: tampilkan crop preview dengan Simpan foto/Batalkan. Avatar memakai inisial sampai sumber terkonfirmasi tersedia. Password memiliki autocomplete current/new-password, konfirmasi dan toggle tampil/sembunyi; tidak memperlihatkan isi password pada pesan sukses/error. Tombol simpan disabled selama request, error tetap menyertakan pilihan/input, sukses mengosongkan field password.


### Foto identitas anggota — 20 Oktober 2026

Foto bundar 29 px pada assignment dan 38 px di Tim; object-fit cover, lazy loading, tidak draggable agar tidak mengganggu drag kartu. Nama tetap terbaca di sampingnya; multi-PJ ditumpuk ringkas dan nama panjang dibungkus di daftar program. Alt menyebut nama akun; foto rusak/kosong menampilkan inisial tanpa layout shift. Tidak ada kontrol edit foto orang lain.
