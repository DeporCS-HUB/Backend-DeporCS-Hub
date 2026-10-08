# Depor CS HUB

Baca `CODEX_HANDOFF.md` dan `README.md` sebelum melanjutkan proyek ini.

- Lanjutkan branch `codex/supabase-main-flows`; jangan merge atau deploy tanpa instruksi baru.
- Supabase harus memakai fitur Free dengan biaya tambahan $0. Verifikasi biaya aktual sebelum provisioning; jangan mengaktifkan layanan berbayar.
- Project Supabase `dajpnhkutkhgxwkjzvpg` harus diperlakukan sebagai production/main dan tidak boleh diubah.
- Semua migrasi, seed, akun uji, dan perubahan data hosted hanya boleh dilakukan pada development yang identitasnya sudah diverifikasi.
- Jangan menyimpan kredensial dalam repository, dokumen, log, atau frontend. Gunakan environment backend.
- Jangan jalankan `supabase/tests/bootstrap.sql` pada hosted Supabase; file ini hanya harness database lokal sementara.
- Pertahankan perubahan pengguna dan laporkan batas validasi secara akurat: tes mock/lokal tidak membuktikan integrasi hosted.
