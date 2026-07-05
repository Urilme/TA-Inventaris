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

## F1 — Autentikasi & Manajemen Sesi Offline

**Prioritas:** Wajib pertama — semua fitur lain bergantung pada sesi user.

### Checklist Implementasi
- [ ] Setup Firebase project → download `google-services.json` → letakkan di `app/`
- [ ] Implementasi `AuthRepositoryImpl`:
  - `login()`: panggil `FirebaseAuth.signInWithEmailAndPassword()`, simpan sesi ke `UserSessionDao`
  - `logout()`: panggil `FirebaseAuth.signOut()`, hapus sesi dari Room (`clearSession()`)
  - `getLocalSession()`: baca dari `UserSessionDao.getSession()`
  - `isSessionValid()`: cek `auth_token` tidak expired (Firebase token valid 1 jam, refresh otomatis)
- [ ] Implementasi `LoginScreen.kt`:
  - TextField email + TextField password (PasswordVisualTransformation)
  - Tombol "Masuk" → trigger `AuthViewModel.login()`
  - Loading indicator saat state = `AuthUiState.Loading`
  - Error snackbar saat state = `AuthUiState.Error`
  - Navigasi ke Dashboard saat state = `AuthUiState.Success`
- [ ] Cek sesi tersimpan saat app launch: jika ada sesi valid di Room → skip LoginScreen

### Kriteria Selesai
- Login dengan kredensial valid → masuk ke Dashboard ✓
- Login dengan kredensial salah → tampilkan pesan error ✓
- Tidak ada internet → sesi lokal dari Room masih bisa digunakan ✓
- Logout → sesi Room terhapus, kembali ke LoginScreen ✓

---

## F2 — Katalog Lokal & Dashboard Offline

**Prioritas:** Wajib sebelum F3 dan F4.

### Checklist Implementasi
- [ ] Implementasi `KatalogRepositoryImpl`:
  - `getAllKatalog()`: return `Flow` dari `MasterKatalogDao.getAllKatalog()`
  - `getKatalogBySku()`: query single item dari Room
  - `upsertKatalog()`: insert/update ke Room dengan `sync_status = PENDING`
  - `deleteKatalog()`: hapus dari Room, tandai untuk sync (atau soft-delete)
  - `updateStokLokal()`: panggil `MasterKatalogDao.updateStok(skuId, delta)`
- [ ] Implementasi `KatalogScreen.kt`:
  - `LazyColumn` daftar barang (collect dari `KatalogViewModel.katalogList`)
  - Setiap item tampilkan: nama barang, SKU, stok, satuan, badge sync status
  - FAB "+" → navigasi ke `TambahKatalogScreen`
  - Swipe-to-delete atau long press untuk hapus
- [ ] Implementasi `TambahKatalogScreen.kt`:
  - Form: kategori, nama barang, satuan, stok awal, **stok_minimum** (threshold peringatan)
  - Tombol simpan → `KatalogViewModel.tambahKatalog()` → SKU di-generate otomatis via `UuidGenerator.generateSkuId()`
- [ ] Implementasi `DashboardScreen.kt`:
  - Kartu ringkasan: total SKU, total stok, jumlah PENDING sync
  - Peringatan stok minimum: tampilkan barang yang `stok_lokal <= stok_minimum`
  - List 5 barang terakhir diupdate
  - Tombol "Sync Sekarang" → trigger `SyncViewModel`
  - Indikator status sync terakhir + waktu

### Kriteria Selesai
- Tambah barang baru → muncul di daftar dengan SKU-XXXXXX ✓
- Stok terbaru tampil di dashboard tanpa koneksi internet ✓
- Badge "PENDING" muncul pada barang baru yang belum disync ✓
- Peringatan muncul saat `stok_lokal <= stok_minimum` ✓

---

## F3 — Pencatatan Mutasi Inventaris

**Bergantung pada:** F2 (katalog harus ada)

### Checklist Implementasi
- [ ] Tambahkan enum `KondisiBarang` dan konstanta `KategoriBarang` di `util/SyncStatus.kt`:
  ```kotlin
  enum class KondisiBarang { BAIK, RUSAK_RINGAN, RUSAK_BERAT }

  object KategoriBarang {
      const val ALAT        = "alat"        // kondisi_barang WAJIB
      const val BAHAN       = "bahan"       // kondisi_barang = null
      const val SCAFFOLDING = "scaffolding" // kondisi_barang = null
  }
  ```
- [ ] Update `LogMutasiEntity` dan domain model `LogMutasi` dengan atribut baru:
  - `id_mandor: String` — auto-fill dari sesi login aktif
  - `id_proyek: String` — proyek tempat mutasi terjadi
  - `kondisi_barang: KondisiBarang? = null` — **nullable**, hanya wajib jika `kategori = "alat"`
  - `lokasi_penggunaan: String` — area spesifik dalam proyek
  - `penanggungjawab: String` — nama mandor pemegang aset
- [ ] Buat Room DB Migration `version 1 → 2` untuk menambahkan kolom baru (JANGAN fallbackToDestructiveMigration)
- [ ] Implementasi `MutasiRepositoryImpl`:
  - `recordMutasi()`: insert `LogMutasiEntity` ke Room dengan semua atribut baru
  - `getAllMutasi()`: return `Flow` dari DAO
  - `getPendingMutasi()`: query `sync_status = 'PENDING'`
  - `markSynced()`: update `sync_status = 'SYNCED'`
- [ ] Implementasi `MutasiScreen.kt`:
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
- [ ] `RecordMutasiUseCase` — tambahkan validasi kategori sebelum INSERT:
  ```kotlin
  if (katalog.kategori == KategoriBarang.ALAT && mutasi.kondisi_barang == null) {
      return Result.failure("Kondisi barang wajib diisi untuk kategori alat")
  }
  ```
- [ ] Pastikan `@Transaction` digunakan di DAO untuk update stok + insert mutasi agar atomic

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

### Checklist Implementasi
- [ ] Implementasi `TransferRepositoryImpl`:
  - `createTransfer()`: insert `TransferProyekEntity` dengan `status_transfer = IN_TRANSIT`
  - `updateStatusTransfer()`: update status ke SELESAI atau DIBATALKAN
  - `getPendingTransfer()` + `markSynced()`
- [ ] Implementasi `TransferScreen.kt`:
  - Dropdown pilih barang (SKU)
  - Input proyek asal (lokasi saat ini)
  - Input proyek tujuan
  - Input jumlah
  - Tombol "Kirim" → buat transfer IN_TRANSIT
  - List transfer aktif dengan status badge
  - Tombol "Konfirmasi Terima" pada item IN_TRANSIT → update ke SELESAI

### Kriteria Selesai
- Transfer dibuat → status IN_TRANSIT, stok di asal berkurang ✓
- Konfirmasi penerimaan → status SELESAI, stok di tujuan bertambah ✓
- Data tersimpan offline, PENDING sync ✓

---

## F5 — Riwayat Transaksi & Status Sinkronisasi

### Checklist Implementasi
- [ ] Implementasi `RiwayatScreen.kt`:
  - Tab atau filter: "Mutasi" | "Transfer" | "Semua"
  - `LazyColumn` semua transaksi dengan `SyncStatusBadge`
  - Filter berdasarkan: tanggal range, jenis (CHECK_IN/CHECK_OUT), status sync
  - Setiap item tampilkan: nama barang, jenis, jumlah, waktu, status
- [ ] Implement `RiwayatViewModel`:
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
- [ ] Implementasi `FirestoreDataSource`:
  - `uploadMutasi(list: List<LogMutasiEntity>)`: batch write ke Firestore collection `mutasi/`
  - `uploadTransfer(list: List<TransferProyekEntity>)`: batch write ke `transfer/`
  - `pullKatalog(since: Long)`: query Firestore `katalog/` dimana `server_timestamp > since`
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
presentation/ui/transfer/TransferScreen.kt   ← F4
presentation/ui/riwayat/RiwayatScreen.kt     ← F5

presentation/viewmodel/TransferViewModel.kt  ← F4
presentation/viewmodel/RiwayatViewModel.kt   ← F5
presentation/viewmodel/SyncViewModel.kt      ← F6

presentation/navigation/AppNavigation.kt     ← semua fitur
```
