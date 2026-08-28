# Rekap Bimbingan Bab IV — Implementasi & Pengujian

> Bahan belajar sebelum bimbingan. Disusun berurutan mengikuti alur Bab IV,
> supaya kamu bisa menjelaskan dari hulu (implementasi) ke hilir (evaluasi).
>
> **Cara pakai:** baca Bagian 1–3 untuk paham alurnya, lalu hafalkan
> Bagian 6 (tanya-jawab) dan Bagian 7 (contekan angka). Bagian 4–5 dibaca
> kalau pembimbing minta demo langsung.

---

## Bagian 0 — Peta Bab IV dalam satu tarikan napas

Kalau pembimbing bertanya "Bab 4 kamu isinya apa?", jawab dengan lima blok ini:

| Subbab | Isi | Menjawab pertanyaan |
|---|---|---|
| 4.1 Penerapan FDD | 5 tahap FDD, dari model keseluruhan sampai *Build by Feature* | Bagaimana saya membangunnya? |
| 4.2 Rangkuman Implementasi Fitur | Rekap 6 fitur + kebutuhan fungsional terkait | Apa saja yang jadi? |
| 4.3 *Black-Box Testing* | 45 skenario, menguji **fungsionalitas** | Apakah fiturnya benar? |
| 4.4 *Concurrency Testing* | 9 skenario, menguji **ketahanan saat bersamaan** | Apakah tahan saat rebutan data? |
| 4.5 Evaluasi Akhir | TSR, *Offline Availability*, keandalan sinkronisasi | Seberapa berhasil secara angka? |

**Kalimat pembuka yang aman:**
> "Bab IV saya bagi jadi tiga bagian besar: implementasi mengikuti lima tahap FDD,
> lalu pengujian dua lapis — *Black-Box* untuk fungsionalitas dan *Concurrency*
> untuk ketahanan sinkronisasi — dan ditutup evaluasi kuantitatif."

---

## Bagian 1 — Menjelaskan Implementasi (Subbab 4.1)

### 1.1 Kenapa FDD, bukan Waterfall/Scrum?

Jawaban singkat: FDD berbasis **fitur**, dan aplikasi ini memang tersusun dari
6 fitur yang saling menumpuk (F1 fondasi → F6 puncak). FDD juga cocok untuk
pengembang tunggal karena tidak menuntut ritual tim seperti Scrum.

### 1.2 Lima tahap FDD dan wujud nyatanya di skripsi

| Tahap FDD | Wujud di Bab IV |
|---|---|
| *Develop an Overall Model* | Arsitektur MVVM + Clean Architecture, diagram lapisan |
| *Build a Feature List* | Tabel 6 fitur (F1–F6) |
| *Plan by Feature* | Urutan implementasi F1→F6 beserta kelas/modulnya |
| *Design by Feature* | ERD, skema Room, diagram alur sinkronisasi |
| *Build by Feature* | Kode nyata + tangkapan layar aplikasi berjalan |

### 1.3 Urutan 6 fitur — jelaskan sebagai "menumpuk", bukan "berjajar"

Ini poin yang sering ditanya. Urutannya **bukan acak**:

1. **F1 Autentikasi & Sesi Luring** — fondasi. Tanpa sesi tersimpan, fitur lain
   tidak tahu "mandor ini siapa" dan "proyeknya mana".
2. **F2 Katalog & Dashboard Luring** — data dasar. Mutasi butuh barang dulu.
3. **F3 Pencatatan Mutasi** — inti bisnis. Butuh F1 (siapa) + F2 (barang apa).
4. **F4 Transfer Antar Proyek** — lanjutan mutasi, melibatkan 2 proyek.
5. **F5 Riwayat & Status Sinkronisasi** — hanya membaca hasil F3 dan F4.
6. **F6 Sinkronisasi Asinkron** — puncak, mengangkut semua data F2–F4 ke awan.

> **Kalimat kunci:** "Setiap fitur hanya bisa dibangun kalau fitur di bawahnya
> sudah ada, jadi urutannya mengikuti ketergantungan data, bukan urutan menu."

### 1.4 Empat konsep arsitektur yang WAJIB kamu kuasai

Kalau cuma sempat menghafal empat hal, hafalkan ini:

**a. *Offline-First***
Semua operasi tulis masuk ke Room (basis data lokal di HP) **lebih dulu**.
Firestore (awan) menyusul belakangan. Jadi aplikasi tetap 100% berfungsi tanpa
sinyal — relevan karena lokasi proyek konstruksi sering tanpa jaringan.

**b. *Store-and-Forward* + status sinkronisasi**
Data baru diberi status `PENDING` (menunggu kirim). Saat jaringan tersedia,
*WorkManager* mengubahnya jadi `IN_TRANSIT` (sedang dikirim), lalu `SYNCED`
(sudah tersimpan di awan). Kalau gagal, kembali ke `PENDING` untuk dicoba lagi.

```
[Simpan lokal] → PENDING → IN_TRANSIT → SYNCED
                    ↑           |
                    └───gagal───┘
```

**c. *Delta Sync* (bukan *full refresh*)**
Aplikasi hanya menarik data yang berubah sejak penanda waktu terakhir
(`last_sync_timestamp`), bukan mengunduh ulang seluruh katalog. Hemat kuota,
penting untuk pekerja lapangan.

**d. *Last-Write-Wins* (LWW)**
Kalau dua perangkat mengubah data yang sama saat luring, yang menang adalah
yang penanda waktunya paling baru. Ini strategi resolusi konflik yang dipilih
karena sederhana dan dapat diprediksi.

### 1.5 Kalau ditanya "kenapa pakai X?"

| Teknologi | Alasan singkat |
|---|---|
| **Room** | Abstraksi SQLite, mendukung transaksi atomik (`@Transaction`) — kunci untuk mencegah stok minus |
| **WorkManager** | Satu-satunya cara resmi Android menjalankan tugas latar yang **selamat** dari aplikasi ditutup dan mati listrik |
| **Firestore** | *Backend* tanpa server sendiri, hemat biaya untuk skripsi, sudah punya penanda waktu server |
| **Jetpack Compose** | UI deklaratif, otomatis ikut berubah saat data berubah (cocok untuk status sinkronisasi yang hidup) |
| **Hilt** | Menyuntik dependensi, membuat kelas bisa diuji karena bisa diganti tiruan (*fake/mock*) saat pengujian |

---

## Bagian 2 — Menjelaskan Pengujian (Subbab 4.3 & 4.4)

### 2.1 Kenapa dua jenis pengujian? (pertanyaan favorit penguji)

> "*Black-Box* menjawab **'apakah fiturnya benar?'** — satu pengguna, satu aksi,
> satu hasil. Sedangkan *Concurrency* menjawab **'apakah tetap benar kalau
> banyak hal terjadi bersamaan?'** Karena fokus penelitian saya ada di
> sinkronisasi asinkron, pengujian kedua inilah yang paling relevan dengan
> rumusan masalah."

Analogi kalau diminta menyederhanakan:
> "*Black-Box* seperti menguji satu orang menarik uang di ATM. *Concurrency*
> seperti menguji sepuluh orang menarik dari rekening yang sama di saat yang
> sama — logika yang lolos uji pertama belum tentu lolos uji kedua."

### 2.2 Kenapa TIDAK pakai SUS (kuesioner *usability*)?

> "Fokus penelitian saya arsitektur sinkronisasi, bukan pengalaman pengguna.
> Metrik yang dipakai murni teknis: *Transaction Success Rate* dan
> *Offline Availability*, supaya selaras dengan rumusan masalah."

### 2.3 Empat metode pengujian yang saya pakai

Ini yang membedakan skripsimu dari sekadar "klik-klik manual". Hafalkan tabel ini:

| Metode | Jalan di mana? | Untuk apa | Kenapa dipilih |
|---|---|---|---|
| **Unit test (JVM)** | Komputer, tanpa HP | Logika bisnis di *use case* & *ViewModel* | Paling cepat (detik), bisa diulang kapan saja |
| **Compose UI test (Robolectric)** | Komputer, Android tiruan | Logika yang ada di lapisan tampilan | Menjangkau yang tak bisa diuji unit test biasa |
| ***Instrumented test*** | Emulator/HP asli | Room sungguhan + operasi paralel | Konkurensi harus diuji di basis data nyata |
| **ADB *scripting*** | Emulator asli | Kondisi sistem (mode pesawat, rotasi, *force-stop*) | Bukti objektif, bukan tangkapan layar manual |
| **Firebase Emulator** | Komputer | Aturan keamanan + penolakan akun | Aman, tidak menyentuh data produksi |

> **Kalimat kunci:** "Saya memilih metode berdasarkan **di lapisan mana logika
> yang ingin dibuktikan berada**, bukan berdasarkan mana yang paling mudah."

### 2.4 Kenapa Robolectric, bukan Espresso?

Jawaban jujur dan aman (sudah tertulis di laporan):
> "Emulator yang tersedia di perangkat saya API 37 (*image* pratinjau) yang
> belum kompatibel dengan Espresso 3.6.1. Kode pengujiannya identik dan tetap
> bisa dijalankan di emulator versi stabil — hanya *runner*-nya yang berbeda."

---

## Bagian 3 — Isi Pengujian secara Rinci

### 3.1 *Black-Box Testing* — 45 skenario

**Struktur tabelnya:** Skenario | Data Uji | Hasil Diharapkan | Hasil Aktual Awal |
Status Awal | Perbaikan | Status Akhir

**Pengelompokan (untuk menjelaskan, jangan sebut satu-satu):**
- Skenario 1–24 → pengujian fungsional keenam fitur
- Skenario 25–45 → pengujian ketahanan (*robustness*)

**Hasil: 44 lulus, 1 tidak sesuai (skenario 36 Rotasi Layar).**

**Tiga skenario yang awalnya GAGAL** — ini justru nilai plus, tunjukkan dengan
percaya diri karena membuktikan pengujiannya jujur dan bukan formalitas:

| No. | Temuan awal | Tindakan |
|---|---|---|
| 36 Rotasi Layar | Isian formulir hilang saat layar diputar (pakai `remember`, bukan `rememberSaveable`) | **Dibiarkan**, dicatat sebagai keterbatasan |
| 38 Penyimpanan Penuh | Penyimpanan lokal tak punya penanganan galat sama sekali → berpotensi *force close* | **Diperbaiki** (dibungkus penanganan galat) |
| 45 Akun Dinonaktifkan | Aturan keamanan Firestore masih terbuka total tanpa syarat autentikasi | **Diperbaiki** (diperketat jadi wajib terautentikasi) |

> **Cara menjelaskan No. 36 kalau dicecar:** "Perbaikannya sebenarnya sepele,
> cukup ganti `remember` jadi `rememberSaveable`. Tapi saya sengaja tidak
> menerapkannya supaya cakupan penelitian tetap fokus di arsitektur
> sinkronisasi, dan saya catat terbuka sebagai keterbatasan."

> **Cara menjelaskan No. 45 (paling berbobot):** "Saat menguji, saya menemukan
> aturan keamanan Firestore masih dibuka total — sisa penyederhanaan tahap awal
> agar pengujian fungsional bisa jalan sebelum autentikasi terpasang. Artinya
> klaim di skenario itu belum benar-benar dijamin. Saya perketat jadi wajib
> terautentikasi, dan sebelum menerapkannya saya telusuri dulu bahwa aplikasi
> mobile dan situs admin sama-sama mengakses Firestore setelah login, jadi tidak
> ada fungsi yang putus."

### 3.2 *Concurrency Testing* — 9 skenario, target TSR 100%

| No. | Skenario | Diuji lewat |
|---|---|---|
| 1 | Luring → daring dengan antrean lokal | `TriggerSyncOrderTest` + `MutasiConcurrencyTest` |
| 2 | Dua pengguna *check-out* bersamaan, SKU sama | `MutasiConcurrencyTest` (*instrumented*) |
| 3 | Koneksi putus di tengah *Delta Sync* | `SyncPartialBatchTest` |
| 4 | Mutasi luring lampau + transaksi daring kini | `RiwayatViewModelTest` |
| 5 | *Stress test* 100 perangkat serentak | `SyncIdempotencyTest` |
| 6 | Satu akun di dua perangkat | `KatalogPushRaceTest` (*instrumented*) |
| 7 | Transfer + mutasi bersamaan | `TransferMutasiConcurrencyTest` (*instrumented*) |
| 8 | *Delta Sync* saat pengguna mengisi formulir | `PullDuringFormTest` (*instrumented*) |
| 9 | *Retry* setelah transmisi parsial gagal | `SyncIdempotencyTest` |

**Skenario 2 adalah bintangnya** — kuasai yang ini paling dalam:
> "Sepuluh operasi *check-out* ditembakkan paralel ke SKU berstok 10, masing-masing
> minta 2 unit. Karena penulisan dibungkus fungsi atomik beranotasi `@Transaction`
> di lapisan DAO, hasilnya deterministik: tepat 5 yang lolos, stok berakhir tepat
> nol, tidak pernah negatif, dan jumlah log mutasi persis sama dengan jumlah
> *check-out* yang berhasil."

**Konsep idempoten (skenario 5 & 9)** — sering ditanya:
> "Setiap dokumen ditulis ke ID yang sudah ditentukan dari UUID transaksinya.
> Jadi kalau pengiriman diulang karena gagal, dokumen yang sama tertimpa, bukan
> membuat duplikat. Inilah yang menjamin *Zero Data Loss* tanpa duplikasi."

---

## Bagian 4 — Cara Menjalankan Pengujian (untuk demo langsung)

### 4.1 Unit test JVM — 54 pengujian, ±15 detik

Paling aman untuk demo karena cepat dan tidak butuh emulator.

**Lewat Android Studio (disarankan saat bimbingan):**
1. Buka panel *Project* di kiri
2. Klik kanan folder `src/test`
3. Pilih **Run 'Tests in ...'**
4. Panel *Run* muncul dengan daftar hijau

**Lewat terminal:**
```powershell
.\gradlew testDebugUnitTest
```

Menjalankan satu kelas saja:
```powershell
.\gradlew testDebugUnitTest --tests "com.ta.konstruksi.sync.SyncWorkerTest"
```

### 4.2 *Instrumented test* — 7 pengujian, butuh emulator

Ini yang menguji Room sungguhan untuk konkurensi.

1. Nyalakan emulator dulu (*Device Manager* → ▶)
2. Klik kanan folder `src/androidTest` → **Run**

Atau terminal:
```powershell
.\gradlew connectedDebugAndroidTest
```

> **Catatan penting:** `KatalogPushRaceTest` mencetak ringkasan 25 percobaan ke
> **Logcat** (bukan Database Inspector). Alasannya: pengujian ini memakai Room
> *in-memory* yang hidup hanya beberapa milidetik, sehingga Database Inspector
> tidak sempat menyambung. Kalau ditanya kenapa tidak pakai Database Inspector,
> jawab persis ini.

### 4.3 Firebase Emulator — 6 pengujian, untuk skenario 45

```powershell
cd firestore-tests
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
npm install
npm run test:emulators
```

**Wajib kamu ingat:** di tengah keluaran akan muncul baris merah
`PERMISSION_DENIED`. Itu **bukan kegagalan** — itu justru bukti bahwa aturan
keamanan memang menolak akses tanpa autentikasi, sesuai yang diharapkan
pengujian `assertFails`. Kalau pembimbing menunjuk baris itu, kamu harus siap
menjelaskannya, jangan panik.

### 4.4 Rekap jumlah pengujian

| Jenis | Jumlah | Perintah |
|---|---|---|
| Unit test JVM | **54** | `gradlew testDebugUnitTest` |
| *Instrumented* | **7** | `gradlew connectedDebugAndroidTest` |
| Firebase Emulator | **6** | `npm run test:emulators` |
| **Total otomatis** | **67** | |

---

## Bagian 5 — Perkakas yang Dipakai (kalau ditanya satu per satu)

| Perkakas | Fungsi | Kalimat penjelas |
|---|---|---|
| **JUnit 4** | Kerangka pengujian dasar | "Penanda `@Test` dan penyedia siklus hidup pengujian" |
| **Truth** | Penulisan pernyataan hasil | "Membuat `assertThat(x).isEqualTo(y)` terbaca seperti kalimat" |
| **MockK** | Membuat objek tiruan | "Meniru Firestore supaya pengujian tidak menyentuh jaringan sungguhan" |
| **Turbine** | Menguji aliran data (*Flow*) | "Menangkap perubahan data reaktif satu per satu" |
| **Robolectric** | Android tiruan di komputer | "Merender komponen Compose tanpa emulator" |
| **work-testing** | Menguji *WorkManager* | "Mengatur jumlah percobaan untuk menguji keputusan *retry*" |
| **ADB** | Mengendalikan emulator dari terminal | "Mengaktifkan mode pesawat, rotasi, *force-stop* secara terprogram" |
| **Firebase Emulator Suite** | Firebase tiruan lokal | "Menguji aturan keamanan tanpa menyentuh proyek produksi" |
| **Mocha** | Kerangka pengujian JavaScript | "Untuk pengujian aturan keamanan yang berbasis Node, bukan Kotlin" |

---

## Bagian 6 — Latihan Tanya-Jawab

**T: Kenapa hasilnya bisa 100% lulus? Apa pengujiannya tidak terlalu mudah?**
> "Tidak semuanya lulus dari awal. Tiga skenario gagal di percobaan pertama —
> rotasi layar, penyimpanan penuh, dan penolakan akun. Dua saya perbaiki, satu
> saya catat sebagai keterbatasan. Kegagalan itu justru saya tampilkan di tabel
> pada kolom Status Awal supaya terlihat prosesnya."

**T: Bagaimana kamu yakin datanya tidak hilang saat luring?**
> "Karena setiap operasi tulis masuk ke Room lebih dulu dengan status `PENDING`,
> baru dikirim *WorkManager*. Ini saya buktikan di skenario ketahanan: aplikasi
> saya matikan paksa saat luring, antrean tetap utuh saat dibuka lagi, dan
> otomatis terkirim begitu jaringan pulih."

**T: Apa bedanya `local_timestamp` dan `server_timestamp`?**
> "`local_timestamp` dari jam perangkat, dipakai untuk resolusi konflik LWW.
> `server_timestamp` dari server Firestore, dipakai sebagai penanda *Delta Sync*.
> Dipisah supaya jam perangkat yang dimundurkan tidak merusak proses sinkronisasi
> — ini saya uji khusus di skenario 35."

**T: Kalau dua orang edit barang sama saat luring, siapa yang menang?**
> "Yang penanda waktunya paling baru (*Last-Write-Wins*). Tapi khusus log mutasi
> tidak pernah bentrok, karena setiap mutasi punya UUID sendiri — yang bisa
> bentrok hanya angka stoknya."

**T: Kenapa tidak ada tombol sinkronisasi manual?**
> "Sesuai kebutuhan fungsional F-MA-009, sinkronisasi sepenuhnya otomatis lewat
> tiga pemicu: saat jaringan tersedia, setelah setiap operasi tulis, dan berkala
> tiap 15 menit."

**T: Apa keterbatasan penelitianmu?** (siapkan, hampir pasti ditanya)
> 1. Rotasi layar belum mempertahankan isian formulir (skenario 36)
> 2. Token yang sudah terbit tetap sah sampai kedaluwarsa alami, jadi pencabutan
>    akun baru efektif pada percobaan masuk berikutnya
> 3. Pengujian 100 perangkat serentak masih simulasi, bukan 100 HP fisik
> 4. Situs admin di luar cakupan penelitian, hanya sistem pendukung

---

## Bagian 7 — Contekan Angka

| Hal | Angka |
|---|---|
| Fitur utama | 6 (F1–F6) |
| Skenario *Black-Box* | 45 (44 lulus, 1 tidak sesuai) |
| Skenario *Concurrency* | 9 (target TSR 100%) |
| Pengujian otomatis total | 67 |
| Kategori barang | 3 (alat, bahan, *scaffolding*) |
| Status sinkronisasi | 3 (`PENDING`, `IN_TRANSIT`, `SYNCED`) |
| Interval sinkronisasi berkala | 15 menit |
| Batas percobaan ulang | 3 kali, *exponential backoff* 30 detik |

---

## Bagian 8 — Sebelum Berangkat Bimbingan

- [ ] Compile LaTeX **2–3 kali** (nomor gambar butuh *pass* kedua)
- [ ] Jalankan `gradlew testDebugUnitTest`, pastikan hijau
- [ ] Nyalakan emulator lebih awal (butuh waktu *booting*)
- [ ] Siapkan aplikasi terpasang di emulator untuk demo luring
- [ ] Baca ulang Bagian 6, terutama pertanyaan keterbatasan

### Yang masih perlu diselesaikan

1. **Subbab 4.5 (Evaluasi Akhir) masih berisi penanda kosong** — tiga subbab
   evaluasi belum diisi perhitungan. Kalau bimbingan membahas sampai situ,
   sampaikan bahwa bagian ini sedang dikerjakan.
2. **Aturan keamanan Firestore belum diunggah ke produksi.** Yang sudah diperketat
   baru berkas lokalnya. Perlu `firebase deploy --only firestore:rules`, lalu
   pastikan aplikasi mobile dan situs admin masih berfungsi normal.
3. **Tabel 4.1 sedikit melebihi lebar halaman** (~9 mm), belum dirapikan.
