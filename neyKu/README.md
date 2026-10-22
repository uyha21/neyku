# neyKu — Android Expense & Budget Tracker

Aplikasi Android pencatat keuangan offline, antarmuka Bahasa Indonesia, tema gelap dengan aksen hijau.

## Fitur yang tersedia
- Ringkasan saldo, pemasukan/pengeluaran bulan ini.
- Tambah/hapus transaksi dengan kategori, tanggal, dan dompet.
- Grafik batang pengeluaran 7 hari terakhir.
- Anggaran bulanan per kategori dan indikator pemakaian.
- Banyak dompet/rekening dengan saldo awal.
- Ekspor transaksi ke CSV melalui Android document picker.
- PIN 4–6 digit untuk layar pembuka.
- Penyimpanan lokal di SharedPreferences; tidak memerlukan akun atau internet.

## Buka dan build APK
1. Instal Android Studio versi stabil dan Android SDK Platform 35.
2. Ekstrak `neyKu-source.zip`.
3. Android Studio → **Open** → pilih folder `neyKu`.
4. Tunggu Gradle Sync selesai. Jika diminta, instal Android SDK Build-Tools yang direkomendasikan.
5. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. APK debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.
7. Pindahkan APK ke ponsel Android dan izinkan instalasi dari sumber tersebut bila diminta.

## Catatan
- Proyek menggunakan JDK 17 dan Android Gradle Plugin 8.7.3.
- Data tersimpan di perangkat. Menghapus data aplikasi atau meng-uninstall dapat menghapus data.
- CSV dapat dibuka di Excel/Google Sheets. Ini bukan file `.xlsx`.
- PIN pada versi awal adalah pengunci aplikasi sederhana, bukan penyimpanan kredensial tingkat tinggi. Jangan gunakan PIN yang sama dengan rekening bank.
- Ekspor CSV hanya mencakup daftar transaksi; saldo awal dompet dan definisi anggaran tidak diekspor.
- Proyek ini merupakan implementasi awal yang dapat dikembangkan lebih lanjut. Uji pada emulator/perangkat sebelum dipakai untuk pencatatan penting.


## Build langsung dari HP dengan GitHub Actions
Folder `.github/workflows/build.yml` berisi workflow build cloud. Dari browser HP:
1. Buat repository baru di GitHub dan unggah semua isi folder proyek (bukan file ZIP sebagai satu file).
2. Buka tab **Actions** pada repository dan aktifkan Actions jika diminta.
3. Pilih workflow **Build neyKu APK**, tekan **Run workflow**.
4. Tunggu hingga selesai. Buka hasil run yang hijau, lalu unduh artifact **neyKu-debug-apk**.
5. Ekstrak ZIP artifact dan ketuk `app-debug.apk` untuk memasangnya.

Build cloud bisa gagal jika ada error kode atau konfigurasi. Jika gagal, buka run → langkah **Build debug APK**, lalu salin pesan error agar bisa diperbaiki.
