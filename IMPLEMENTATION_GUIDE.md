# IMPLEMENTATION_GUIDE.md

Panduan implementasi detail per fitur — berdasarkan Plan by Feature (FDD) dari proposal TA.
Gunakan file ini sebagai checklist saat mengimplementasikan setiap fitur di Claude Code.

---

## Urutan Implementasi

```
F1 Auth → F2 Katalog → F3 Mutasi → F4 Transfer → F5 Riwayat → F6 Sync
```

Setiap fitur bisa berdiri sendiri kecuali F3 butuh F2, dan F6 butuh semua fitur lain selesai.

---

## Status Implementasi (per 6 Juli 2026)

| Fitur | Status | Catatan |
|---|---|---|
| **F1** Autentikasi & Sesi Offline | ✅ **Selesai** | Firebase Auth asli (login-only), offline-first, auto-login saat launch. Terverifikasi (login valid/salah, logout). |
| **F2** Katalog & Dashboard Offline | ✅ **Selesai** | CRUD + `stok_minimum` + peringatan Dashboard. Dashboard didesain ulang (lihat catatan deviasi). |
| **F3** Pencatatan Mutasi | ✅ **Selesai** | Validasi stok + aturan `kondisi_barang` (alat wajib) + update stok atomik (`@Transaction`). |
| **F4** Transfer Antar Proyek | ✅ **Selesai** | Alur IN_TRANSIT → SELESAI, stok dipindah via ViewModel. |
| **F5** Riwayat & Status Sinkronisasi | ✅ **Selesai** | Gabungan mutasi+transfer, filter, badge sinkron. |
| **F6** Sinkronisasi Asinkron | ✅ **Selesai** | Delta sync, state machine IN_TRANSIT, LWW, `server_timestamp`+`updated_by`. |

> **Deviasi desain yang disengaja (bukan bug):**
> - **Sinkronisasi otomatis** menggantikan tombol "Sync Sekarang/Manual" — dipicu setelah setiap operasi tulis (via `SyncScheduler`) dan periodik 15 menit (WorkManager). Tidak ada tombol sync manual di UI.
> - **Dashboard didesain ulang**: menampilkan "Mutasi Hari Ini", "Menunggu Sinkron", aksi cepat, dan "Stok Material Terendah" (berbasis `stok_minimum`) — bukan kartu "total SKU/total stok" atau "5 barang terakhir".
> - **Hapus katalog**: logika (`hapusKatalog`) tersedia di ViewModel/Repository namun belum dipasang gestur (swipe/long-press) di UI.
> - **Halaman Profil** (`ProfileScreen` + `ProfileViewModel`): diakses via **avatar inisial** di header Dashboard (menggantikan tombol "KELUAR"). Menampilkan identitas (nama, role, email, id_mandor), **Proyek Aktif** (nama_proyek + id_proyek), **Sinkronisasi** (waktu sinkron terakhir + jumlah menunggu), versi aplikasi, dan tombol **Keluar** (logout dipindah ke sini).

---

## Model Kepemilikan Proyek (Scoping per Proyek)

Setiap mandor **ditugaskan ke satu proyek**. Aplikasi mobile hanya menampilkan katalog & data
milik proyek si mandor — mandor tidak melihat aset proyek lain. Penugasan proyek (siapa di proyek
mana) **dikelola di luar aplikasi mobile ini**, yaitu pada **website admin yang akan dibangun
terpisah**. Aplikasi mobile hanya **membaca** penugasan tersebut, tidak mengubahnya.

### Alur provisioning
```
[Website Admin]  --tulis-->  Firestore  users/{uid}  { nama, role, id_proyek, nama_proyek }
                                   │
                                   ▼ (saat login, sekali ambil)
[App Mobile] AuthRepositoryImpl.buatSesiDari() → simpan ke UserSession (Room)
                                   │
                                   ▼ (dipakai offline seterusnya)
     Katalog difilter WHERE id_proyek = UserSession.id_proyek
     Mutasi baru diikat ke UserSession.id_proyek (bukan konstanta)
     Dashboard menampilkan UserSession.nama_proyek pada slot lokasi (ikon 📍)
```

### Implementasi (Selesai)
- [x] `UserSession` + `UserSessionEntity`: tambah `id_proyek` & `nama_proyek` (default `""`)
- [x] `MasterKatalog` + `MasterKatalogEntity`: tambah `id_proyek` (barang milik proyek mana)
- [x] Migrasi Room `v3 → v4` (`MIGRATION_3_4`): tambah kolom + **backfill** data lama ke
      `PROYEK-UTAMA` agar katalog/sesi yang sudah ada tidak hilang dari tampilan
- [x] `FirestoreDataSource.fetchUserProfile(uid)`: baca `users/{uid}` (nama, role, id_proyek, nama_proyek)
- [x] `AuthRepositoryImpl.login()`: isi proyek dari profil server; fallback bila dokumen belum ada
      (`Constants.DEFAULT_PROYEK` / `DEFAULT_NAMA_PROYEK`) — login tetap sukses walau luring/tanpa dokumen
- [x] `MasterKatalogDao.getAllKatalog(idProyek)` + `KatalogRepositoryImpl` menyaring ke proyek aktif
      dan **menstempel** `id_proyek` pada barang baru
- [x] `MutasiViewModel`: `id_proyek` diambil dari sesi (bukan `Constants.DEFAULT_PROYEK`)
- [x] `TransferViewModel` + `TransferScreen`: `proyek_asal` diambil dari sesi (`nama_proyek`), bukan
      placeholder statis "Proyek Aktif"; `proyek_tujuan` **dipilih dari registry proyek** (dropdown,
      bukan diketik) sehingga bebas typo dan konsisten dengan nama proyek penerima
- [x] **Registry proyek** (`ProyekEntity`/`ProyekDao`/`ProyekRepository` + koleksi Firestore `proyek`):
      migrasi Room `v4 → v5` (tabel `proyek`), `FirestoreDataSource.pullProyek()` (tarik penuh, read-only),
      `SyncRepository.pullLatestProyek()` dipanggil di `TriggerSyncUseCase`. Sumber kebenaran nama proyek
      = website admin; app hanya membaca & meng-cache
- [x] `DashboardScreen`: slot lokasi (📍) menampilkan `nama_proyek` (dulu keliru menampilkan `role`)
- [x] Sinkron: `pullKatalog(since, idProyek)` menyaring hasil ke proyek si mandor
      (penyaringan di sisi klien agar cukup indeks satu-field `server_timestamp`, tanpa indeks komposit)

### Catatan & batasan (jujur)
- **Riwayat (`RiwayatScreen`) belum di-scope per proyek** — hanya **katalog** & **transfer masuk** yang
  disaring. Karena satu perangkat = satu mandor = satu proyek, mutasi/transfer yang tercatat di
  perangkat itu memang milik proyek tersebut, jadi belum menjadi masalah pada skenario single-device.
- **Transfer sudah di-scope**: `proyek_asal` terikat ke proyek sesi (`nama_proyek`), dan
  `TransferMasukScreen` hanya menampilkan transfer IN_TRANSIT yang `proyek_tujuan`-nya cocok dengan
  proyek aktif. Karena `proyek_tujuan` kini **dipilih dari registry proyek** (bukan diketik), nama
  proyek konsisten di kedua sisi dan pencocokan andal. Provisioning `users/{uid}` (nama_proyek penerima)
  dan koleksi `proyek` sama-sama bersumber dari website admin.
- **Provisioning `users/{uid}` belum otomatis**: dokumen ditulis manual/oleh website (belum ada).
  Untuk akun uji `budi@skn.com` yang belum punya dokumen, proyek jatuh ke fallback `PROYEK-UTAMA`
  sehingga Dashboard menampilkan "Proyek belum ditetapkan".
- **Website admin adalah pekerjaan terpisah** (di luar cakupan TA aplikasi mobile ini).

---

## F1 — Autentikasi & Manajemen Sesi Offline

**Prioritas:** Wajib pertama — semua fitur lain bergantung pada sesi user.

### Checklist Implementasi
- [x] Setup Firebase project → `google-services.json` (tersedia di root repo) — provider Email/Password aktif
- [x] Implementasi `AuthRepositoryImpl`:
  - `login()`: panggil `FirebaseAuth.signInWithEmailAndPassword()`, simpan sesi ke `UserSessionDao`
  - `logout()`: panggil `FirebaseAuth.signOut()`, hapus sesi dari Room (`clearSession()`)
  - `getLocalSession()`: baca dari `UserSessionDao.getSession()`
  - `isSessionValid()`: cek sesi Room / `firebaseAuth.currentUser`
- [x] Implementasi `LoginScreen.kt`:
  - TextField email + TextField password (PasswordVisualTransformation)
  - Tombol "Masuk" → trigger `AuthViewModel.login()`
  - Loading indicator saat state = `AuthUiState.Loading`
  - Error snackbar saat state = `AuthUiState.Error`
  - Navigasi ke Dashboard saat state = `AuthUiState.Success`
- [x] Cek sesi tersimpan saat app launch: jika ada sesi valid di Room → skip LoginScreen (`AppNavigation` + `AuthViewModel.startupLoggedIn`)

### Kriteria Selesai
- Login dengan kredensial valid → masuk ke Dashboard ✓
- Login dengan kredensial salah → tampilkan pesan error ✓
- Tidak ada internet → sesi lokal dari Room masih bisa digunakan ✓
- Logout → sesi Room terhapus, kembali ke LoginScreen ✓

---

## F2 — Katalog Lokal & Dashboard Offline

**Prioritas:** Wajib sebelum F3 dan F4.

### Checklist Implementasi
- [x] Implementasi `KatalogRepositoryImpl`:
  - `getAllKatalog()`: return `Flow` dari `MasterKatalogDao.getAllKatalog()`
  - `getKatalogBySku()`: query single item dari Room
  - `upsertKatalog()`: insert/update ke Room dengan `sync_status = PENDING`
  - `deleteKatalog()`: hapus dari Room
  - `updateStokLokal()`: panggil `MasterKatalogDao.updateStok(skuId, delta)`
- [x] Implementasi `KatalogScreen.kt`:
  - `LazyColumn` daftar barang (collect dari `KatalogViewModel.katalogList`)
  - Setiap item tampilkan: nama barang, SKU, stok, satuan, badge sync status
  - Item dapat diklik → `DetailBarangScreen`; FAB "+" → `TambahKatalogScreen`
  - ⚠️ Swipe-to-delete/long-press: logika `hapusKatalog` ada, gestur UI belum dipasang
- [x] Implementasi `TambahKatalogScreen.kt`:
  - Form: kategori (3 kategori kanonik), nama barang, satuan, **stok_minimum** (threshold peringatan)
  - Tombol simpan → `KatalogViewModel.tambahKatalog()` → SKU di-generate otomatis via `UuidGenerator.generateSkuId()`
- [x] Implementasi `DashboardScreen.kt` (didesain ulang):
  - Kartu ringkasan: "Mutasi Hari Ini" + "Menunggu Sinkron" (jumlah PENDING)
  - ✅ Peringatan stok minimum: barang yang `stok_lokal <= stok_minimum` ("Stok Material Terendah")
  - Aksi cepat (Transfer, Katalog) + daftar item menunggu sinkron
  - ⚠️ Kartu "total SKU/total stok" & "5 barang terakhir" tidak dipakai; sinkronisasi otomatis (tanpa tombol "Sync Sekarang")

### Kriteria Selesai
- Tambah barang baru → muncul di daftar dengan SKU-XXXXXX ✓
- Stok terbaru tampil di dashboard tanpa koneksi internet ✓
- Badge "PENDING" muncul pada barang baru yang belum disync ✓
- Peringatan muncul saat `stok_lokal <= stok_minimum` ✓

---

## F3 — Pencatatan Mutasi Inventaris

**Bergantung pada:** F2 (katalog harus ada)

### Checklist Implementasi
- [x] Tambahkan enum `KondisiBarang` (`util/SyncStatus.kt`) dan konstanta `KategoriBarang` (`util/Constants.kt`):
  ```kotlin
  enum class KondisiBarang { BAIK, RUSAK_RINGAN, RUSAK_BERAT }

  object KategoriBarang {
      const val ALAT        = "alat"        // kondisi_barang WAJIB
      const val BAHAN       = "bahan"       // kondisi_barang = null
      const val SCAFFOLDING = "scaffolding" // kondisi_barang = null
  }
  ```
- [x] Update `LogMutasiEntity` dan domain model `LogMutasi` dengan atribut baru:
  - `id_mandor: String` — auto-fill dari sesi login aktif
  - `id_proyek: String` — proyek tempat mutasi terjadi
  - `kondisi_barang: KondisiBarang? = null` — **nullable**, hanya wajib jika `kategori = "alat"`
  - `lokasi_penggunaan: String` — area spesifik dalam proyek
  - `penanggungjawab: String` — nama mandor pemegang aset
- [x] Buat Room DB Migration `version 1 → 2` untuk menambahkan kolom baru (JANGAN fallbackToDestructiveMigration) — juga migrasi `2 → 3` (`stok_minimum` + `kondisi_barang` nullable)
- [x] Implementasi `MutasiRepositoryImpl`:
  - `recordMutasi()`: insert `LogMutasiEntity` ke Room dengan semua atribut baru
  - `getAllMutasi()`: return `Flow` dari DAO
  - `getPendingMutasi()`: query `sync_status = 'PENDING'`
  - `markSynced()`: update `sync_status = 'SYNCED'`
- [x] Implementasi `MutasiScreen.kt`:
  - Dropdown pilih barang (dari katalog Room) — saat barang dipilih, baca `kategori`-nya
  - Toggle Check-In / Check-Out
  - Input jumlah (validasi numerik > 0)
  - **Dropdown `kondisi_barang` (Baik / Rusak Ringan / Rusak Berat): tampil HANYA jika `kategori = "alat"`**
  - Input `lokasi_penggunaan` (TextField) — selalu tampil
  - Input `penanggungjawab` (TextField — bisa pre-fill nama dari sesi) — selalu tampil
  - Input catatan (opsional)
  - `id_mandor` dan `id_proyek` diambil otomatis dari `UserSession` aktif — tidak ditampilkan ke user
  - Tombol simpan → `MutasiViewModel.recordMutasi()`
  - Validasi client-side: CHECK_OUT tidak boleh melebihi stok saat ini
- [x] `RecordMutasiUseCase` — validasi kategori sebelum INSERT:
  ```kotlin
  if (katalog.kategori == KategoriBarang.ALAT && mutasi.kondisi_barang == null) {
      return Result.failure("Kondisi barang wajib diisi untuk kategori alat")
  }
  ```
- [x] `@Transaction` digunakan di DAO untuk update stok + insert mutasi agar atomic (`LogMutasiDao.recordMutasiAtomic`)

### Aturan Bisnis Kategori (KRITIS)
```
kategori = "alat"        → kondisi_barang WAJIB (dropdown tampil)
kategori = "bahan"       → kondisi_barang = null (dropdown DISEMBUNYIKAN)
kategori = "scaffolding" → kondisi_barang = null (dropdown DISEMBUNYIKAN)
```
Validasi ini ada di DUA tempat:
1. **UI layer** (`MutasiScreen`): sembunyikan/tampilkan dropdown secara reaktif berdasarkan item yang dipilih
2. **Domain layer** (`RecordMutasiUseCase`): validasi ulang sebelum INSERT sebagai safety net

### Logika Kritis
```
CHECK_OUT:
  1. Baca stok_lokal dan kategori dari Room (via sku_id)
  2. Validasi: stok_lokal >= jumlah
  3. Validasi: jika kategori = "alat", kondisi_barang != null
  4. Insert LogMutasi (sync_status = PENDING)
     → id_mandor auto-fill dari UserSession.id_mandor
     → kondisi_barang = null jika bahan/scaffolding
  5. Update stok_lokal -= jumlah (optimistic)

CHECK_IN:
  1. Validasi: jika kategori = "alat", kondisi_barang != null
  2. Insert LogMutasi (sync_status = PENDING)
  3. Update stok_lokal += jumlah
```

### Kriteria Selesai
- CHECK_OUT dengan jumlah > stok → error "Stok tidak mencukupi" ✓
- Pilih barang kategori "alat" → dropdown kondisi_barang muncul dan wajib diisi ✓
- Pilih barang kategori "bahan" atau "scaffolding" → dropdown kondisi_barang tidak muncul ✓
- Form tidak bisa disimpan jika `lokasi_penggunaan` atau `penanggungjawab` kosong ✓
- CHECK_IN berhasil → stok bertambah, transaksi tersimpan dengan PENDING ✓
- Tanpa internet → semua operasi tetap berjalan ✓

---

## F4 — Transfer Aset Antar Proyek

**Bergantung pada:** F2 (katalog harus ada)

> **Catatan gap proposal:** Proposal hanya mendokumentasikan 1 wireframe (`wireframe_transfer.png`)
> untuk sisi pengirim. Implementasi aktual memiliki **3 screen** — lihat checklist di bawah.
> Enum `status_transfer` di teks proposal menyebut `PENDING_TRANSFER` tapi nilai aktual adalah `IN_TRANSIT`.

### Alur F4 (2 Sisi)
```
[Proyek Asal — Pengirim]          [Proyek Tujuan — Penerima]
TransferScreen                     TransferMasukScreen
  → isi form (barang, tujuan,        → lihat daftar transfer IN_TRANSIT
    jumlah)                            yang masuk ke proyek ini
  → stok asal berkurang            TransferDetailScreen
  → status = IN_TRANSIT              → lihat detail transfer
                                     → tap "Konfirmasi Terima"
                                     → status = SELESAI
                                     → stok tujuan bertambah
```

### Checklist Implementasi
- [x] Implementasi `TransferRepositoryImpl`:
  - `createTransfer()`: insert `TransferProyekEntity` dengan `status_transfer = IN_TRANSIT`
  - `updateStatusTransfer()`: update status ke `SELESAI` atau `DIBATALKAN`
  - `getPendingTransfer()` + `markSynced()`
  - `getAllTransfer()`: return `Flow` semua transfer dari Room
- [x] Implementasi `TransferScreen.kt` — **sisi pengirim**:
  - Dropdown kategori → dropdown barang (difilter per kategori)
  - Tampil info stok proyek asal (**proyek asal = `nama_proyek` dari sesi**)
  - **Dropdown proyek tujuan dari registry** (`proyekList`, kecuali proyek sendiri) — dipilih, bukan diketik, agar bebas typo
  - Input jumlah (stepper, max = stok lokal)
  - Banner info IN_TRANSIT
  - Tombol "Kirim Aset" → `TransferViewModel.createTransfer()` → stok asal berkurang, status `IN_TRANSIT`
- [x] Implementasi `TransferMasukScreen.kt` — **daftar transfer masuk**:
  - Filter dari `transferList`: `status_transfer == IN_TRANSIT` **dan** `proyek_tujuan == proyek aktif` (dari sesi)
  - Setiap item tampilkan: nama barang, proyek asal, jumlah, waktu
  - Tap item → navigasi ke `TransferDetailScreen`
- [x] Implementasi `TransferDetailScreen.kt` — **konfirmasi penerimaan**:
  - Tampil detail lengkap transfer (barang, asal, tujuan, jumlah, status)
  - Tombol "Konfirmasi Terima" → `TransferViewModel.konfirmasiTerima(idTransfer)` → status `SELESAI`
  - Tombol "Batalkan" → status `DIBATALKAN`
  - UI reaktif terhadap `uiState` (loading/sukses/error)

### Kriteria Selesai
- Transfer dibuat → status `IN_TRANSIT`, stok proyek asal berkurang ✓
- Muncul di `TransferMasukScreen` sisi penerima ✓
- Konfirmasi penerimaan → status `SELESAI`, stok proyek tujuan bertambah ✓
- Batalkan transfer → status `DIBATALKAN` ✓
- Data tersimpan offline, `sync_status = PENDING` ✓

---

## F5 — Riwayat Transaksi & Status Sinkronisasi

### Checklist Implementasi
- [x] Implementasi `RiwayatScreen.kt`:
  - Tab atau filter: "Mutasi" | "Transfer" | "Semua"
  - `LazyColumn` semua transaksi dengan `SyncStatusBadge`
  - Filter berdasarkan: tanggal range, jenis (CHECK_IN/CHECK_OUT), status sync
  - Setiap item tampilkan: nama barang, jenis, jumlah, waktu, status
- [x] Implement `RiwayatViewModel`:
  - Collect `Flow` dari `MutasiRepository.getAllMutasi()` dan `TransferRepository.getAllTransfer()`
  - Combine dan sort berdasarkan `local_timestamp DESC`

### Kriteria Selesai
- Semua transaksi muncul dalam urutan terbaru ✓
- Badge warna sesuai: 🟡 PENDING | 🔵 IN_TRANSIT | 🟢 SYNCED ✓
- Filter berfungsi ✓

---

## F6 — Sinkronisasi Asinkron (Delta Sync)

**Bergantung pada:** Semua fitur F1–F5 harus selesai.

### Checklist Implementasi
- [x] Implementasi `FirestoreDataSource`:
  - `uploadMutasi/uploadTransfer/uploadKatalog(entity, updatedBy)`: tulis dokumen + `server_timestamp` (`FieldValue.serverTimestamp`) + `updated_by`
  - `pullKatalog(since)`: query Firestore `katalog/` dimana `server_timestamp > since` (kursor delta bebas selisih jam)
- [ ] Implementasi `SyncRepositoryImpl`:
  - `syncPendingMutasi()`:
    1. `logMutasiDao.getPending()` → ambil semua PENDING
    2. Update status ke IN_TRANSIT di Room
    3. Upload ke Firestore
    4. Jika sukses: `logMutasiDao.markSynced()` untuk setiap item
    5. Jika gagal: kembalikan ke PENDING (rollback)
  - `syncPendingTransfer()`: sama dengan di atas untuk transfer
  - `pullLatestKatalog()`: ambil dari Firestore berdasarkan `last_sync_timestamp`, upsert ke Room
- [ ] `SyncWorker` sudah ada — pastikan `hiltWorker` setup benar di `KonstruksiApp`
- [ ] Implementasi `SyncViewModel`:
  - Expose `WorkManager.getWorkInfosByTagLiveData()` sebagai StateFlow
  - Tombol "Sync Manual" → enqueue `SyncWorker.buildOneTimeRequest()`
- [ ] Tambah UI sync status di Dashboard (last sync time, jumlah PENDING)

### State Machine Delta Sync
```
Record Mutasi/Transfer
        ↓
   sync_status = PENDING
        ↓ (WorkManager: network tersedia)
   sync_status = IN_TRANSIT
        ↓                    ↓
   (Firestore OK)      (Firestore GAGAL)
        ↓                    ↓
   sync_status = SYNCED   PENDING (retry exponential)
```

### Kriteria Selesai
- Semua PENDING terupload saat koneksi tersedia → status berubah SYNCED ✓
- Gagal upload → tetap PENDING, retry otomatis ✓
- Data Firestore terbaru ter-pull ke Room ✓
- Delta sync: hanya data sejak `last_sync_timestamp` yang ditransfer ✓

---

## Pengujian

### Black-Box Testing — 22 Skenario

| ID | Fitur | Skenario | Expected |
|---|---|---|---|
| TC-01 | F1 | Login email+password valid | Masuk Dashboard |
| TC-02 | F1 | Login password salah | Pesan error |
| TC-03 | F1 | Login tanpa internet (sesi ada) | Masuk Dashboard (dari Room) |
| TC-04 | F1 | Logout | Kembali ke Login, Room cleared |
| TC-05 | F2 | Tambah barang baru | Muncul di daftar, SKU auto-generated |
| TC-06 | F2 | Edit stok barang | Stok terupdate di Room |
| TC-07 | F2 | Hapus barang | Hilang dari daftar |
| TC-08 | F2 | Dashboard tanpa internet | Data stok tampil dari Room |
| TC-09 | F3 | CHECK_IN barang | Stok bertambah, log tersimpan PENDING |
| TC-10 | F3 | CHECK_OUT valid (stok cukup) | Stok berkurang, log tersimpan PENDING |
| TC-11 | F3 | CHECK_OUT invalid (stok kurang) | Error "Stok tidak mencukupi" |
| TC-12 | F3 | Mutasi tanpa internet | Berhasil (tersimpan PENDING) |
| TC-13 | F4 | Buat transfer antar proyek | Status IN_TRANSIT |
| TC-14 | F4 | Konfirmasi penerimaan transfer | Status SELESAI |
| TC-15 | F4 | Batalkan transfer | Status DIBATALKAN |
| TC-16 | F5 | Lihat riwayat mutasi | Semua transaksi tampil |
| TC-17 | F5 | Filter by PENDING | Hanya PENDING yang tampil |
| TC-18 | F5 | Badge warna sesuai status | 🟡/🔵/🟢 tampil benar |
| TC-19 | F6 | Sync manual saat online | Semua PENDING → SYNCED |
| TC-20 | F6 | Sync saat offline | Tidak crash, tetap PENDING |
| TC-21 | F6 | Data Firestore ter-pull | Data terbaru muncul di Room |
| TC-22 | F6 | Periodic sync background | WorkManager jalan setiap 15 menit |

### Concurrency Testing — 5 Skenario (Target TSR = 100%)

| ID | Skenario | Method | Expected |
|---|---|---|---|
| CT-01 | Dua CHECK_OUT bersamaan pada SKU yang sama | Thread terpisah | Satu sukses, satu gagal (stok tidak minus) |
| CT-02 | Transfer + CHECK_OUT bersamaan | Concurrent coroutines | Room transaction mencegah race condition |
| CT-03 | SyncWorker berjalan saat user input mutasi | Background + foreground | Data konsisten, tidak double-write |
| CT-04 | Offline → Online: banyak PENDING menumpuk | Batch sync | Semua PENDING terproses, tidak ada yang hilang |
| CT-05 | LWW: dua device edit katalog yang sama offline | Timestamps | Device dengan `local_timestamp` lebih baru menang |

**TSR (Test Success Rate) = (jumlah skenario lulus / total skenario) × 100%**
Target: TSR = 100% untuk semua 5 skenario concurrency.

### SUS (System Usability Scale)
- 30 responden: mandor lapangan PT Sahabat Konstruksi Nusantara
- 10 pertanyaan standard SUS (skala Likert 1–5)
- Target: SUS score > 68 (di atas rata-rata industri)
- Formula: `SUS = (Σ skor) × 2.5`
- Interpretasi: > 68 = "Good", > 85 = "Excellent"

---

---

## Gap Proposal vs Implementasi (Per Juli 2026)

> Daftar ini merangkum semua perbedaan antara proposal penelitian (LaTeX chapter-3-christopher.tex)
> dan kode yang sudah diimplementasi. Gunakan sebagai checklist revisi proposal sebelum sidang.

### ❌ Yang perlu direvisi di PROPOSAL

| # | Item | Kondisi Saat Ini di Proposal | Yang Perlu Dilakukan |
|---|---|---|---|
| 1 | **Wireframe Transfer — sisi penerima** | Hanya ada `wireframe_transfer.png` (sisi pengirim). `TransferMasukScreen` dan `TransferDetailScreen` tidak ada wireframe-nya | Buat 2 wireframe baru + update teks sub-poin e) F4 untuk menjelaskan alur 2 sisi |
| 2 | **Teks Transfer sub-poin e)** | Menyebut `status_transfer = PENDING_TRANSFER` (enum salah) | Ganti jadi `IN_TRANSIT` sesuai `StatusTransfer` enum aktual |
| 3 | **Skema `UserSession` di LaTeX CLAUDE.md** | Masih versi lama — tidak punya `id_proyek` dan `nama_proyek` | Update LaTeX CLAUDE.md + pertimbangkan update ERD di proposal |
| 4 | **Skema `MasterKatalog` di LaTeX CLAUDE.md** | Tidak punya `id_proyek` | Sama seperti di atas |

### ✅ Yang sudah sesuai antara proposal dan kode

| Item | Status |
|---|---|
| Semua atribut `LogMutasi` (id_mandor, id_proyek, kondisi_barang, lokasi_penggunaan, penanggungjawab) | ✅ Sesuai |
| Aturan kondisi_barang: wajib untuk alat, null untuk bahan/scaffolding | ✅ Sesuai (UI + UseCase) |
| `KategoriBarang` constants (3 kategori tetap) | ✅ Ada di `Constants.kt` |
| `stok_minimum` di MasterKatalog | ✅ Sesuai |
| Room DB version 4 dengan 4 migrasi bertahap | ✅ Sesuai |
| `RecordMutasiUseCase` validasi kategori + stok atomik | ✅ Sesuai |

### 📋 Deviasi desain disengaja (tidak perlu direvisi, cukup disebutkan di Bab IV)

| Deviasi | Keterangan |
|---|---|
| Sync otomatis menggantikan tombol "Sync Manual" | Dipicu setelah setiap operasi tulis + periodik 15 menit |
| `ProfileScreen` (bukan di proposal) | Diakses via avatar inisial di header Dashboard — berisi identitas, proyek aktif, last sync, logout |
| Dashboard didesain ulang | "Mutasi Hari Ini", "Menunggu Sinkron", aksi cepat — bukan "total SKU/5 barang terakhir" |
| Hapus katalog UI belum dipasang | Logika ada di ViewModel/Repository, gestur swipe/long-press belum diimplementasi di UI |

---

## Tips Debugging Umum

**Room tidak update UI:**
→ Pastikan DAO return `Flow<>`, bukan `suspend fun List<>`
→ Pastikan collect dengan `collectAsStateWithLifecycle()` di Composable

**WorkManager tidak jalan:**
→ Cek `NetworkType.CONNECTED` constraint
→ Cek `@HiltWorker` + `@AssistedInject` benar
→ Cek `HiltWorkerFactory` di-set di `WorkManager.Configuration`

**Firestore upload gagal silently:**
→ Selalu gunakan `.await()` dengan `try-catch` pada Firestore calls
→ Cek Firebase rules di Console

**Stok minus (race condition):**
→ Gunakan `@Transaction` di Room DAO untuk validasi + update atomic
→ Atau gunakan `Mutex` di ViewModel level

---

## File yang Butuh Implementasi Penuh

File-file ini sudah ada sebagai stub dan perlu di-implement:

```
data/repository/AuthRepositoryImpl.kt        ← F1
data/repository/KatalogRepositoryImpl.kt     ← F2
data/repository/MutasiRepositoryImpl.kt      ← F3
data/repository/TransferRepositoryImpl.kt    ← F4
data/repository/SyncRepositoryImpl.kt        ← F6
data/remote/FirestoreDataSource.kt           ← F6

presentation/ui/auth/LoginScreen.kt          ← F1
presentation/ui/dashboard/DashboardScreen.kt ← F2
presentation/ui/katalog/KatalogScreen.kt     ← F2
presentation/ui/katalog/TambahKatalogScreen.kt ← F2
presentation/ui/mutasi/MutasiScreen.kt       ← F3
presentation/ui/transfer/TransferScreen.kt        ← F4 (sisi pengirim)
presentation/ui/transfer/TransferMasukScreen.kt   ← F4 (daftar masuk)
presentation/ui/transfer/TransferDetailScreen.kt  ← F4 (konfirmasi terima)
presentation/ui/riwayat/RiwayatScreen.kt     ← F5

presentation/viewmodel/TransferViewModel.kt  ← F4
presentation/viewmodel/RiwayatViewModel.kt   ← F5
presentation/viewmodel/SyncViewModel.kt      ← F6

presentation/navigation/AppNavigation.kt     ← semua fitur
```
