# Revisi Aplikasi Mobile — Tahap 3 (Kesiapan Produksi)

> Dokumen instruksi untuk Claude Code. Dibuat setelah audit lanjutan atas repositori
> `TA-Inventaris` per commit `00d960f`, digabung dengan temuan uji manual transfer
> proyek A ke proyek B.
>
> **Baca `CLAUDE.md` lebih dulu** untuk konteks arsitektur.

---

## Konteks: apa yang sudah dikerjakan sebelumnya

Dua putaran perbaikan sudah selesai dan sudah diverifikasi benar. **Jangan diubah**
kecuali disebut secara eksplisit di dokumen ini.

### Putaran 1 (commit `dbac046`)

| Perbaikan | Ringkasan |
|---|---|
| Sinkronisasi dua arah | Ditambahkan `pullMutasi()` dan `pullTransfer()`. Sebelumnya jalur mutasi dan transfer bersifat *push-only*, sehingga transfer tidak pernah sampai ke perangkat penerima dan riwayat hilang saat pasang ulang. |
| Kursor delta terpisah | `last_sync_timestamp`, `last_sync_mutasi`, `last_sync_transfer` agar tiap koleksi tidak saling mendahului. |
| Penjaga LWW | Fungsi `bolehTimpa()` melindungi baris berstatus PENDING/IN_TRANSIT dari tertimpa versi server. |
| Hapus katalog | Penjaga `hasRiwayatMutasi()` dan penangkapan `SQLiteConstraintException`, mencegah tutup paksa akibat FK RESTRICT. |
| Filter jenis mutasi | Melengkapi F-MA-018. |

Pengujian: 53 → 72.

### Putaran 2 (commit `00d960f`)

| Perbaikan | Ringkasan |
|---|---|
| Transfer atomik | `konfirmasiTerimaAtomik()` dan `batalkanAtomik()` sebagai `@Transaction` di DAO, mencegah stok bertambah dua kali. |
| Penjaga logout | Peringatan bila ada data PENDING, dan upaya sinkronisasi lebih dulu sebelum sesi dihapus. |
| Penyaringan sisi peladen | `whereEqualTo("id_proyek", ...)` pada pull, plus `firestore.indexes.json` berisi empat composite index. |
| Aturan keamanan | Otorisasi per peran dan per proyek, termasuk penguncian field identitas transfer saat konfirmasi. |

Pengujian: 72 → 79, ditambah 22 pengujian aturan keamanan berbasis Firebase Local
Emulator Suite (`firestore-tests/rules.test.js`).

---

## Cara mengerjakan dokumen ini

Ada **9 pekerjaan** dalam **3 kelompok**. Kerjakan berurutan, dan **lapor setelah tiap
kelompok selesai** sebelum lanjut ke kelompok berikutnya.

Beberapa butir ditandai **[ANALISIS DULU]**. Untuk butir tersebut, sampaikan analisis
dan usulan solusi terlebih dahulu, lalu **tunggu persetujuan** sebelum mengubah kode.

---

# Kelompok A — Bug dari uji manual

## A1. Spam klik tombol kirim transfer menghasilkan duplikat

**Gejala terverifikasi.** Saat menguji transfer dari proyek A ke proyek B, menekan
tombol konfirmasi berulang dengan cepat membuat transfer terkirim lebih dari sekali dan
stok proyek asal berkurang berlipat.

Akar masalahnya ada tiga. Perbaiki ketiganya.

### (a) Penjaga dipasang terlambat

Di `TransferViewModel.createTransfer()`, baris `_uiState.value = Loading` berada **di
dalam** `viewModelScope.launch`. Akibatnya ada jeda antara ketukan dan saat Compose
merender ulang dengan tombol nonaktif, dan ketukan kedua lolos di jeda itu.

Pola yang benar sudah ada di `ProfileViewModel.logout()`: penjaga boolean diperiksa
**secara sinkron di awal fungsi, sebelum `launch`**.

```kotlin
if (_sedangProses.value) return
```

Terapkan ke `createTransfer()`, `confirmTransfer()`, dan `cancelTransfer()`.

### (b) Duplikat tidak tertolak di lapisan bawah — **[ANALISIS DULU]**

Tiap panggilan `createTransfer()` membangkitkan `UuidGenerator.generateId()` baru,
sehingga dua ketukan menghasilkan dua baris dengan id berbeda dan
`decrementStokLokalJikaCukup()` berjalan dua kali. Tidak ada tabrakan kunci primer yang
bisa menyelamatkan.

Bandingkan dengan `konfirmasiTerimaAtomik()` yang sudah aman karena menolak transfer
yang statusnya bukan `IN_TRANSIT`.

Sampaikan analisis dan usulan solusi sebelum mengubah kode. Salah satu arah yang bisa
dipertimbangkan: membangkitkan id transfer **sekali saja** saat dialog konfirmasi dibuka
(bukan tiap ketukan), digabung dengan operasi atomik di DAO yang menolak id yang sudah
ada.

### (c) Penyebab sesungguhnya ada di UX

Setelah `TransferUiState.Success`, dialog **tidak ditutup** dan **tidak ada umpan balik
apa pun**. Pengguna mengira operasinya gagal, lalu menekan lagi. Inilah pemicu
sebenarnya dari spam klik.

Yang perlu dikerjakan:

- Tutup dialog saat `Success`.
- Tampilkan notifikasi keberhasilan (snackbar) berisi ringkasan: nama barang, jumlah,
  proyek tujuan.
- Kosongkan formulir setelah berhasil.
- Lakukan hal yang sama untuk konfirmasi penerimaan dan pembatalan.

### Pengujian yang perlu ditambahkan

Dua panggilan `createTransfer` beruntun untuk barang dan jumlah yang sama hanya boleh
menghasilkan **satu** baris transfer dan **satu** kali pengurangan stok.

---

## A2. Penerima terlambat tahu ada aset masuk, dan belum ada notifikasi sama sekali

**Gejala terverifikasi.** Saat uji manual, penerima baru melihat transfer masuk setelah
jeda cukup lama.

**Penyebabnya struktural, bukan bug.** `pullTransfer()` memakai `.get()` sekali jalan di
dalam `SyncWorker`, dan sinkronisasi periodik disetel 15 menit, yang merupakan batas
minimum WorkManager di Android dan tidak bisa dipersingkat. Aplikasi juga belum punya
notifikasi sama sekali: tidak ada izin `POST_NOTIFICATIONS` di manifes, tidak ada
notification channel, dan tidak ada FCM di dependensi.

### Batasan yang wajib dipatuhi

**Cloud Function dan FCM DILARANG.** Batasan masalah penelitian menyatakan arsitektur
memakai BaaS tanpa peladen mandiri. Menambahkan Cloud Function berarti mengubah batasan
penelitian, dan itu di luar lingkup pekerjaan ini.

### Yang perlu dikerjakan

1. **Notifikasi lokal dari `SyncWorker`.** Ketika `pullLatestTransfer()` menemukan
   transfer `IN_TRANSIT` **baru** yang menuju proyek aktif, munculkan notifikasi sistem
   yang membuka halaman Transfer Masuk saat diketuk. Pastikan tidak memberi notifikasi
   ulang untuk transfer yang sudah pernah diberitahukan.

2. **Izin dan channel.** Tambahkan `POST_NOTIFICATIONS` (Android 13+) beserta permintaan
   izin runtime-nya, dan buat notification channel di `KonstruksiApp`.

3. **Persingkat jeda yang dirasakan.** Picu sinkronisasi sekali jalan saat aplikasi
   kembali ke latar depan dan saat halaman Transfer Masuk dibuka.

4. **Snapshot listener untuk transfer — [ANALISIS DULU].** Khusus koleksi transfer,
   pakai `addSnapshotListener` selagi aplikasi di latar depan agar pembaruan nyaris
   seketika. Batasi hanya saat layar terkait aktif supaya tidak memboroskan kuota baca
   dan baterai. Sampaikan analisis biaya bacanya sebelum mengimplementasikan.

---

# Kelompok B — Skala dan ketahanan

## B1. Antrean sinkronisasi tanpa batas berpotensi kehabisan waktu

`getPendingSync()` di `MasterKatalogDao`, `LogMutasiDao`, dan `TransferProyekDao`
mengambil semua baris PENDING **tanpa `LIMIT`**, lalu mengunggahnya satu per satu secara
berurutan dalam satu `doWork()`.

WorkManager membatasi eksekusi sekitar sepuluh menit. Mandor yang luring dua minggu
dengan ratusan catatan berisiko kena batas waktu, gagal, lalu mengulang dari awal
terus-menerus.

Yang perlu dikerjakan:

- Pecah unggahan per kelompok (misalnya 50 dokumen per siklus).
- Bila masih ada sisa, kembalikan `Result.retry()` atau jadwalkan siklus lanjutan.
- Pertimbangkan Firestore `WriteBatch` (batas 500 operasi) untuk mengurangi bolak-balik
  jaringan.
- Tambahkan pengujian untuk skenario antrean besar.

---

## B2. Riwayat dimuat seluruhnya ke memori — **[ANALISIS DULU]**

`getAllMutasi()` dan `getAllTransfer()` adalah `SELECT *` tanpa `LIMIT` maupun paginasi,
lalu digabung di `RiwayatViewModel`. Setelah setahun pemakaian dengan puluhan ribu baris,
layar Riwayat akan lambat dan boros memori.

Terapkan paginasi. Pilih antara Paging 3 atau `LIMIT`/`OFFSET` sederhana bila Paging
dinilai berlebihan untuk lingkup ini, lalu jelaskan pilihanmu sebelum mengubah kode.

Pastikan penyaringan status sinkronisasi dan jenis mutasi tetap berfungsi setelah
paginasi diterapkan.

---

## B3. Riwayat masih lintas proyek — **[ANALISIS DULU]**

`getAllMutasi()` belum punya `WHERE id_proyek`, padahal katalog sudah difilter per
proyek. Mandor yang dipindahtugaskan akan melihat riwayat proyek lama bercampur dengan
proyek barunya.

Saring per proyek aktif, ikuti pola `KatalogRepositoryImpl.proyekAktif()` yang sudah ada.

Putuskan dan jelaskan lebih dulu perlakuan untuk entri transfer, yang memakai **nama**
proyek pada `proyek_asal`/`proyek_tujuan`, bukan `id_proyek`.

---

# Kelompok C — Keamanan dan kesiapan rilis

## C1. Login luring tidak memverifikasi kata sandi — **[ANALISIS DULU]**

`AuthRepositoryImpl.loginLuring()` hanya mencocokkan `username` dengan sesi tersimpan.
Parameter `password` **tidak diperiksa sama sekali**.

Saat ini sulit dieksploitasi karena `AppNavigation` melewati layar Login bila sesi ada,
sehingga layar Login praktis hanya muncul ketika sesi sudah kosong. Tapi ini lubang laten
yang langsung terbuka begitu logika layar awal berubah.

Arah perbaikan: simpan verifikator kata sandi saat login daring berhasil (hash ber-salt,
misalnya PBKDF2 atau Argon2 lewat `androidx.security`), lalu verifikasi saat login
luring.

> **Jangan pernah menyimpan kata sandi dalam bentuk terbaca.**

Sampaikan analisis lebih dulu, khususnya dua hal berikut:

- Apakah ini menyalahi prinsip *offline-first*?
- Bagaimana perilakunya bila mandor mengganti kata sandi saat sedang luring?

---

## C2. Aturan baca Firestore masih longgar

Operasi tulis sudah dikunci per peran dan per proyek, tapi `katalog/`, `mutasi/`, dan
`transfer/` masih memakai `allow read: if isSignedIn()`. Artinya setiap mandor masih bisa
**membaca** data seluruh proyek.

Perketat menjadi per proyek. Ini sekarang aman dilakukan karena semua kueri pull sudah
memakai `whereEqualTo` per proyek, dan Firestore mengizinkan aturan baca per dokumen pada
kueri daftar selama kuerinya sendiri sudah dibatasi.

> **PENTING:** pertahankan jalur `isAdmin()`, karena website admin membaca lintas proyek
> untuk halaman monitoring.

Perluas `firestore-tests/rules.test.js` untuk membuktikan:

- Mandor proyek A ditolak membaca data proyek B.
- Seluruh operasi sah aplikasi mobile tetap berhasil.
- Seluruh operasi sah website admin tetap berhasil.

---

## C3. `allowBackup` masih true

`AndroidManifest.xml` memakai `android:allowBackup="true"`, sehingga basis data Room
berisi sesi dan seluruh data inventaris bisa ikut tersalin ke cadangan perangkat.

Matikan, atau batasi lewat aturan cadangan yang mengecualikan basis data.

---

## C4. Belum ada konfigurasi rilis

`build.gradle.kts` belum punya blok `buildTypes.release`: tidak ada `minifyEnabled`,
ProGuard, maupun `signingConfig`, dan `versionCode` masih 1. Aplikasi belum bisa dibuat
berkas rilis yang layak pasang.

Yang perlu dikerjakan:

- Tambahkan konfigurasi rilis dengan `minifyEnabled` dan `shrinkResources`.
- Tulis aturan ProGuard yang aman untuk Room, Hilt, dan Firestore.
- Siapkan `signingConfig` yang membaca kredensial dari `keystore.properties`.
- Pastikan berkas keystore dan kata sandinya masuk `.gitignore` dan tidak pernah
  di-commit.
- Pastikan `./gradlew assembleRelease` berhasil.

---

## C5. Belum ada pelaporan kerusakan

Dependensi Firebase hanya Auth dan Firestore. Kalau aplikasi tertutup paksa di HP mandor
di lapangan, tidak ada cara mengetahuinya.

Tambahkan Firebase Crashlytics. Pastikan tidak ada data pribadi maupun isi inventaris
yang ikut terkirim dalam laporan kerusakan.

---

# Aturan kerja

- Ikuti konvensi `CLAUDE.md`: tanpa XML, `Flow`/`StateFlow` bukan `LiveData`, DAO
  `suspend` atau `Flow`, migrasi Room eksplisit tanpa `fallbackToDestructiveMigration`.
- Jalankan `./gradlew testDebugUnitTest` setelah tiap kelompok, pastikan lolos.
- Untuk butir bertanda **[ANALISIS DULU]** (A1b, A2 no.4, B2, B3, C1), sampaikan analisis
  dan tunggu persetujuan sebelum mengubah kode.
- Kalau ada temuan yang dinilai lebih kritis dari urutan di atas, sampaikan lebih dulu
  sebelum mengerjakannya.

## Laporan akhir yang dibutuhkan

Setelah semua kelompok selesai, laporkan tiga hal berikut. Ketiganya akan dipakai untuk
memperbarui tabel skenario pengujian pada laporan Tugas Akhir.

1. Jumlah total pengujian sekarang (JVM, instrumentasi, dan aturan keamanan, dipisah).
2. Daftar pengujian baru yang ditambahkan, beserta perilaku yang dibuktikan tiap
   pengujian.
3. Daftar perilaku baru yang terlihat oleh pengguna, karena tiap perilaku baru
   kemungkinan memerlukan skenario Black-Box baru di laporan.

---

# Catatan untuk laporan Tugas Akhir (di luar lingkup Claude Code)

Bagian ini bukan pekerjaan koding, hanya pengingat.

- **Paragraf CT-01 di Bab IV sudah usang.** Masih menyatakan jalur mutasi dan transfer
  bersifat *push-only*, dan urutan pemanggilan yang disebutkan belum memuat
  `pullLatestMutasi()` serta `pullLatestTransfer()`.
- **Gambar hasil pengujian perlu diambil ulang.** Jumlah dan nama pengujian sudah
  berubah, sehingga `test_sync.png`, `test_katalog.png`, `test_mutasi.png`, dan lainnya
  tidak lagi mencerminkan isi berkas pengujian.
- **Skenario Black-Box perlu ditambah**, dari 45 menjadi sekitar 53, ditambah skenario
  baru dari pekerjaan A1 dan A2. Bila notifikasi jadi diimplementasikan, perlu kebutuhan
  fungsional baru (misalnya F-MA-025).
