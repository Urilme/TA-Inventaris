# CLAUDE.md — Konteks Proyek Tugas Akhir

> File ini dibaca otomatis oleh Claude Code saat membuka project ini.
> Berisi seluruh konteks yang dibutuhkan untuk melanjutkan implementasi.

---

## Identitas Proyek

| Field | Detail |
|---|---|
| **Judul** | Pengembangan Aplikasi Mobile Monitoring Aset dan Inventaris Konstruksi Berbasis Sinkronisasi Asinkron pada PT Sahabat Konstruksi Nusantara |
| **Mahasiswa** | Christopher Benaya Tampubolon |
| **NIM** | 122140090 |
| **Program Studi** | Teknik Informatika |
| **Institusi** | Institut Teknologi Sumatera (ITERA) |
| **Pembimbing 1** | Ir. Aidil Saputra Kirsan, S.T., M.T., IPM., ASEAN Eng. |
| **Pembimbing 2** | Thomas Aditya Nugraha, S.T., M.T. |

---

## Tech Stack

| Layer | Teknologi |
|---|---|
| **Bahasa** | Kotlin |
| **UI** | Jetpack Compose (deklaratif, **TANPA XML**) |
| **Local DB** | Room Persistence Library (SQLite abstraction) |
| **Background Sync** | WorkManager |
| **Cloud Backend** | Cloud Firestore (NoSQL BaaS) |
| **Auth** | Firebase Authentication |
| **Async** | Kotlin Coroutines (`Dispatchers.IO`) |
| **DI** | Hilt (Dependency Injection) |
| **Navigation** | Jetpack Navigation Compose |
| **State Mgmt** | ViewModel + StateFlow/collectAsState |

---

## Arsitektur

### Pola Arsitektur: MVVM + Clean Architecture (Offline-First)

```
app/
└── src/main/java/com/ta/konstruksi/
    ├── data/
    │   ├── local/          ← Room DB (DAOs, Entities)
    │   ├── remote/         ← Firestore data sources
    │   └── repository/     ← Repository implementations
    ├── domain/
    │   ├── model/          ← Domain models
    │   ├── repository/     ← Repository interfaces
    │   └── usecase/        ← Business logic use cases
    ├── presentation/
    │   ├── ui/             ← Composable screens & components
    │   └── viewmodel/      ← ViewModels
    ├── sync/
    │   └── worker/         ← WorkManager Workers
    ├── di/                 ← Hilt modules
    └── util/               ← Utilities, constants
```

### Prinsip Desain Kritis

1. **Offline-First**: Semua operasi tulis ke Room dulu, Firestore adalah secondary. Device beroperasi penuh tanpa internet.
2. **Delta Sync**: Hanya transfer data yang berubah sejak `last_sync_timestamp`. BUKAN full-refresh.
3. **Store-and-Forward**: Mutasi disimpan lokal dengan `sync_status = PENDING`, dikirim ke cloud saat koneksi tersedia via WorkManager.
4. **Eventual Consistency**: Konflik diselesaikan dengan **Last-Write-Wins (LWW)** berdasarkan `local_timestamp`.
5. **Optimistic Concurrency**: UI update langsung (optimistic), rollback jika sync gagal.

---

## Database Schema (Room Entities)

### `UserSession`
```kotlin
@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val id_mandor: String,       // UUID
    val nama: String,
    val email: String,
    val role: String,                         // "mandor" | "admin"
    val auth_token: String,                   // Firebase ID token — di-reset tiap login online,
                                              // dipertahankan saat login offline; rekam jejak saja
    val last_sync_timestamp: Long = 0L,       // epoch millis, untuk delta sync
    val id_proyek: String = "",               // proyek yang ditugaskan (dari Firestore users/{uid})
    val nama_proyek: String = ""              // nama tampilan proyek — ditampilkan di Dashboard
)
```

### `MasterKatalog`
```kotlin
@Entity(tableName = "master_katalog")
data class MasterKatalog(
    @PrimaryKey val sku_id: String,           // UUID-based SKU
    val kategori: String,                     // ENUM STRING: "alat" | "bahan" | "scaffolding"
    val nama_barang: String,
    val satuan: String,
    val stok_lokal: Int,
    val stok_minimum: Int = 0,                // threshold peringatan stok minimum di Dashboard
    val id_proyek: String = "",               // proyek pemilik barang — katalog difilter per proyek
    val sync_status: SyncStatus = SyncStatus.PENDING,
    val local_timestamp: Long = System.currentTimeMillis()
)
```

> **Aturan Kategori:** Kategori barang hanya tiga nilai tetap: `"alat"`, `"bahan"`, `"scaffolding"`.
> Gunakan konstanta di `Constants.kt` agar tidak ada typo:
> ```kotlin
> object KategoriBarang {
>     const val ALAT        = "alat"
>     const val BAHAN       = "bahan"
>     const val SCAFFOLDING = "scaffolding"
>
>     val ALL = listOf(ALAT, BAHAN, SCAFFOLDING)
>
>     /** Label tampilan untuk dropdown UI. */
>     fun label(value: String): String = when (value) {
>         ALAT        -> "Alat"
>         BAHAN       -> "Bahan"
>         SCAFFOLDING -> "Scaffolding"
>         else        -> value.replaceFirstChar { it.uppercase() }
>     }
> }
> ```

### `LogMutasi`
```kotlin
@Entity(tableName = "log_mutasi")
data class LogMutasi(
    @PrimaryKey val id_transaksi: String,     // UUID
    val sku_id: String,                       // FK ke MasterKatalog
    val id_mandor: String,                    // FK ke UserSession — siapa yang mencatat
    val id_proyek: String,                    // lokasi/proyek tempat mutasi terjadi
    val jenis_mutasi: JenisMutasi,            // CHECK_IN | CHECK_OUT
    val jumlah: Int,
    val kondisi_barang: KondisiBarang? = null, // BAIK | RUSAK_RINGAN | RUSAK_BERAT
                                               // NULLABLE — hanya diisi jika kategori = "alat"
                                               // bahan dan scaffolding → null
    val lokasi_penggunaan: String,            // area spesifik dalam proyek
    val penanggungjawab: String,              // nama mandor pemegang aset
    val catatan: String? = null,
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: SyncStatus = SyncStatus.PENDING
)
```

> **Aturan Kondisi Barang:**
> - `kategori = "alat"` → `kondisi_barang` **WAJIB** diisi (tidak boleh null)
> - `kategori = "bahan"` → `kondisi_barang` = `null` (tidak relevan untuk material habis pakai)
> - `kategori = "scaffolding"` → `kondisi_barang` = `null`
> - Validasi ini dilakukan di `RecordMutasiUseCase` sebelum INSERT ke Room

### `TransferProyek`
```kotlin
@Entity(tableName = "transfer_proyek")
data class TransferProyek(
    @PrimaryKey val id_transfer: String,      // UUID
    val sku_id: String,                       // FK ke MasterKatalog
    val proyek_asal: String,
    val proyek_tujuan: String,
    val jumlah: Int,
    val status_transfer: StatusTransfer,      // IN_TRANSIT | SELESAI | DIBATALKAN
    val local_timestamp: Long = System.currentTimeMillis(),
    val sync_status: SyncStatus = SyncStatus.PENDING
)
```

### Enum Types
```kotlin
enum class SyncStatus { PENDING, IN_TRANSIT, SYNCED }
enum class JenisMutasi { CHECK_IN, CHECK_OUT }
enum class StatusTransfer { IN_TRANSIT, SELESAI, DIBATALKAN }
enum class KondisiBarang { BAIK, RUSAK_RINGAN, RUSAK_BERAT }
```

---

## 6 Fitur Utama (Urutan Implementasi)

Urutan berdasarkan **Plan by Feature (FDD)** — implementasi dari pondasi ke atas:

### F1 — Autentikasi & Manajemen Sesi Offline ⭐ PRIORITAS 1
- Firebase Auth login (email/password)
- Simpan sesi ke Room (`UserSession`) untuk akses offline
- **Siklus `auth_token`**: diambil segar (`getIdToken(false)`) **hanya saat login online** dan
  **menimpa** baris sesi (di-reset tiap login ulang online). Saat **offline** (`loginLuring`),
  sesi lokal dipakai ulang apa adanya — `auth_token` lama **dipertahankan**, tidak diperbarui.
  Catatan: `auth_token` hanya disimpan sebagai rekam jejak; panggilan Firestore memakai sesi
  internal SDK `FirebaseAuth`, bukan field ini (jadi token stale tidak memengaruhi fungsi).
- Logout bersihkan sesi lokal
- **File utama**: `AuthViewModel`, `AuthRepository`, `LoginScreen`

### F2 — Katalog Lokal & Dashboard Offline ⭐ PRIORITAS 1
- CRUD katalog barang ke Room
- UUID-based SKU generation untuk barang tanpa serial fisik
- Dashboard tampilkan ringkasan stok dari Room (tanpa network call)
- **File utama**: `KatalogViewModel`, `KatalogRepository`, `DashboardScreen`, `KatalogScreen`

### F3 — Pencatatan Mutasi Inventaris ⭐ PRIORITAS 2
- Form check-in / check-out aset
- Validasi stok tidak minus (cek Room sebelum insert)
- Wajib isi: `kondisi_barang` (dropdown enum), `lokasi_penggunaan`, `penanggungjawab`
- `id_mandor` dan `id_proyek` di-auto-fill dari sesi login yang aktif
- Simpan ke `LogMutasi` dengan `sync_status = PENDING`
- Update `stok_lokal` di `MasterKatalog` secara optimistic
- **File utama**: `MutasiViewModel`, `MutasiRepository`, `MutasiScreen`

### F4 — Transfer Aset Antar Proyek ⭐ PRIORITAS 2
- Form transfer dengan `proyek_asal` dan `proyek_tujuan` — status awal `IN_TRANSIT`
- Sisi pengirim: `TransferScreen` — form buat transfer, stok proyek asal berkurang
- Sisi penerima: `TransferMasukScreen` — daftar transfer IN_TRANSIT yang masuk ke proyek aktif
- Konfirmasi penerimaan: `TransferDetailScreen` — detail + tombol "Konfirmasi Terima" → status jadi `SELESAI`, stok proyek tujuan bertambah
- **File utama**: `TransferViewModel`, `TransferRepository`, `TransferScreen`, `TransferMasukScreen`, `TransferDetailScreen`

### F5 — Riwayat Transaksi & Status Sinkronisasi ⭐ PRIORITAS 2
- List semua `LogMutasi` dan `TransferProyek` dari Room
- Badge visual per item: 🟡 PENDING | 🔵 IN_TRANSIT | 🟢 SYNCED
- Filter by status, tanggal, jenis mutasi
- **File utama**: `RiwayatViewModel`, `RiwayatScreen`

### F6 — Sinkronisasi Asinkron (Delta Sync) ⭐ PRIORITAS 3
- `SyncWorker` (WorkManager) dijalankan saat koneksi tersedia
- Query Room: `WHERE sync_status = 'PENDING'`
- Upload batch ke Firestore
- Jika sukses: update `sync_status = SYNCED`, update `last_sync_timestamp`
- Jika gagal: retry dengan exponential backoff (WorkManager built-in)
- **File utama**: `SyncWorker`, `SyncRepository`, `SyncStatusScreen`

---

## Sync State Machine

```
[Operasi Lokal] → PENDING
       ↓ (WorkManager triggered saat online)
    IN_TRANSIT  ← (sedang dikirim ke Firestore)
       ↓ (sukses)          ↓ (gagal)
    SYNCED              PENDING  ← (retry)
```

### WorkManager Trigger Conditions
1. Network tersedia (`NetworkType.CONNECTED`)
2. Manual trigger dari user (tombol "Sync Sekarang")
3. Periodic: setiap 15 menit (minimum WorkManager interval)

---

## Struktur Package Android

```
com.ta.konstruksi/
├── KonstruksiApp.kt              ← Application class (Hilt)
├── MainActivity.kt               ← Single Activity host
│
├── data/
│   ├── local/
│   │   ├── KonstruksiDatabase.kt ← Room Database
│   │   ├── dao/
│   │   │   ├── UserSessionDao.kt
│   │   │   ├── MasterKatalogDao.kt
│   │   │   ├── LogMutasiDao.kt
│   │   │   └── TransferProyekDao.kt
│   │   └── entity/
│   │       ├── UserSessionEntity.kt
│   │       ├── MasterKatalogEntity.kt
│   │       ├── LogMutasiEntity.kt
│   │       └── TransferProyekEntity.kt
│   ├── remote/
│   │   └── FirestoreDataSource.kt
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── KatalogRepositoryImpl.kt
│       ├── MutasiRepositoryImpl.kt
│       ├── TransferRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── UserSession.kt
│   │   ├── MasterKatalog.kt
│   │   ├── LogMutasi.kt
│   │   └── TransferProyek.kt
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── KatalogRepository.kt
│   │   ├── MutasiRepository.kt
│   │   ├── TransferRepository.kt
│   │   └── SyncRepository.kt
│   └── usecase/
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   └── LogoutUseCase.kt
│       ├── katalog/
│       │   ├── GetKatalogUseCase.kt
│       │   └── UpsertKatalogUseCase.kt
│       ├── mutasi/
│       │   └── RecordMutasiUseCase.kt
│       ├── transfer/
│       │   └── CreateTransferUseCase.kt
│       └── sync/
│           └── TriggerSyncUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   └── AppNavigation.kt      ← NavHost + routes
│   ├── ui/
│   │   ├── auth/
│   │   │   └── LoginScreen.kt
│   │   ├── dashboard/
│   │   │   └── DashboardScreen.kt
│   │   ├── katalog/
│   │   │   ├── KatalogScreen.kt
│   │   │   └── TambahKatalogScreen.kt
│   │   ├── mutasi/
│   │   │   └── MutasiScreen.kt
│   │   ├── transfer/
│   │   │   ├── TransferScreen.kt         ← form kirim aset (buat transfer baru)
│   │   │   ├── TransferMasukScreen.kt    ← daftar transfer IN_TRANSIT masuk ke proyek ini
│   │   │   └── TransferDetailScreen.kt   ← detail + konfirmasi penerimaan (→ SELESAI)
│   │   ├── profile/
│   │   │   └── ProfileScreen.kt          ← identitas mandor, proyek aktif, last sync, logout
│   │   ├── riwayat/
│   │   │   └── RiwayatScreen.kt
│   │   └── component/
│   │       ├── SyncStatusBadge.kt
│   │       └── StokCard.kt
│   └── viewmodel/
│       ├── AuthViewModel.kt
│       ├── KatalogViewModel.kt
│       ├── MutasiViewModel.kt
│       ├── TransferViewModel.kt
│       ├── RiwayatViewModel.kt
│       └── SyncViewModel.kt
│
├── sync/
│   └── worker/
│       └── SyncWorker.kt
│
├── di/
│   ├── DatabaseModule.kt
│   ├── FirestoreModule.kt
│   └── RepositoryModule.kt
│
└── util/
    ├── UuidGenerator.kt          ← UUID-based SKU generator
    ├── SyncStatus.kt             ← Enum definitions
    └── Constants.kt
```

---

## Konvensi Koding

### Kotlin Style
- Selalu gunakan `data class` untuk model
- Gunakan `sealed class` / `sealed interface` untuk UI state
- `Flow<T>` dari Room DAO, bukan `LiveData`
- `StateFlow` di ViewModel, bukan `MutableLiveData`
- Hindari nullable yang tidak perlu — gunakan default values

### Jetpack Compose
- **TIDAK ADA XML** — semua UI adalah `@Composable fun`
- `LaunchedEffect` untuk side-effects (navigasi, toast)
- `collectAsStateWithLifecycle()` untuk collect Flow di Composable
- Preview dengan `@Preview` annotation untuk setiap screen

### Room
- Semua DAO method adalah `suspend fun` atau return `Flow<>`
- Migration strategy: wajib buat `Migration` object, jangan `fallbackToDestructiveMigration` di production
- `@Transaction` untuk operasi multi-tabel (misal: mutasi → update stok)

### WorkManager
- `SyncWorker` extends `CoroutineWorker`
- Constraints: `NetworkType.CONNECTED`
- Retry policy: eksponensial backoff, max retry 3x
- Selalu return `Result.success()`, `Result.retry()`, atau `Result.failure()`

---

## Pengujian

### Black-Box Testing (22 Skenario)
Fokus pada:
- Login valid/invalid (F1)
- CRUD katalog (F2)
- Check-in/check-out dengan validasi stok (F3)
- Alur transfer IN_TRANSIT → SELESAI (F4)
- Filter riwayat transaksi (F5)
- Trigger sync manual + verifikasi status berubah ke SYNCED (F6)

### Concurrency Testing (5 Skenario, Target TSR = 100%)
Skenario utama:
1. Dua mutasi bersamaan pada SKU yang sama
2. Transfer + mutasi bersamaan
3. Sync terjadi saat user sedang input mutasi
4. Offline → Online dengan data PENDING menumpuk
5. Konflik LWW: dua device edit data sama offline

### SUS (System Usability Scale)
- 30 responden (mandor lapangan PT SKN)
- Target SUS score > 68 (above average)
- 10 pertanyaan standard SUS, skala Likert 1–5

---

## Firestore Collections

```
firestore/
├── users/{id_mandor}             ← UserSession document
├── katalog/{sku_id}              ← MasterKatalog document
├── mutasi/{id_transaksi}         ← LogMutasi document
├── transfer/{id_transfer}        ← TransferProyek document
└── proyek/{id_proyek}            ← registry proyek { id_proyek, nama_proyek } (read-only dari app)
```

> **Koleksi `proyek`** adalah registry proyek yang dikelola **website admin** (di luar app mobile).
> App hanya **membaca**-nya (di-cache ke Room tabel `proyek`, di-refresh tiap sync) untuk mengisi
> pilihan **proyek tujuan** di Transfer — agar nama proyek konsisten & bebas typo (dipilih, bukan diketik).

Setiap document di Firestore memiliki field tambahan:
- `server_timestamp`: `FieldValue.serverTimestamp()`
- `updated_by`: `id_mandor` yang terakhir mengubah

---

## Dependency Versions (build.gradle.kts)

```kotlin
// Versi sebagai referensi — sesuaikan dengan versi terbaru saat implementasi
val composeVersion = "1.6.x"
val roomVersion = "2.6.x"
val hiltVersion = "2.5x"
val workVersion = "2.9.x"
val firebaseBom = "33.x.x"
val kotlinVersion = "2.0.x"
val coroutinesVersion = "1.8.x"
```

---

## Quick Start untuk Claude Code

Saat memulai implementasi, ikuti urutan ini:

1. **Setup project baru** di Android Studio dengan template "Empty Activity (Compose)"
2. **Tambahkan dependencies** di `build.gradle.kts` (Room, Hilt, WorkManager, Firebase)
3. **Buat `google-services.json`** dari Firebase Console, letakkan di `app/`
4. **Implementasi F1** (Auth) — landasan untuk semua fitur lain
5. **Implementasi F2** (Katalog + Dashboard) — data dasar
6. **Implementasi F3** (Mutasi) — core business logic
7. **Implementasi F4** (Transfer) — fitur lanjutan
8. **Implementasi F5** (Riwayat) — read-only, relatif mudah
9. **Implementasi F6** (Sync Worker) — integrasi semua layer

Lihat `IMPLEMENTATION_GUIDE.md` untuk checklist detail per fitur.

---

## Gap Analysis: Proposal vs Implementasi

> Hasil analisis per Juli 2026 — gap yang ditemukan antara proposal penelitian (LaTeX) dan
> kode Android yang sudah diimplementasi. Perlu diselesaikan sebelum sidang.

### ✅ Sesuai (kode = proposal)
| Item | Keterangan |
|---|---|
| `LogMutasiEntity` | Semua atribut baru ada: `id_mandor`, `id_proyek`, `kondisi_barang` (nullable), `lokasi_penggunaan`, `penanggungjawab` |
| `KategoriBarang` | Ada di `Constants.kt` dengan `ALL` list dan `label()` helper |
| `KondisiBarang` enum | Ada di `SyncStatus.kt` |
| `RecordMutasiUseCase` | Validasi kategori sudah ada: kondisi wajib untuk alat, null untuk bahan/scaffolding |
| `MutasiScreen` | Dropdown kondisi hanya muncul saat `isAlat == true`; penanggungjawab auto-fill dari sesi |
| `UserSessionEntity` | Punya `id_proyek` dan `nama_proyek` (MIGRATION_3_4) |
| `MasterKatalogEntity` | Punya `stok_minimum` (MIGRATION_2_3) dan `id_proyek` (MIGRATION_3_4) |
| Room DB | Versi 4, semua migrasi terdokumentasi |

### ❌ Gap — Implementasi lebih lengkap dari proposal
| Gap | Detail | Tindakan di Proposal |
|---|---|---|
| **F4 punya 3 screen, proposal hanya 1 wireframe** | Android punya `TransferScreen` (kirim), `TransferMasukScreen` (daftar masuk), `TransferDetailScreen` (konfirmasi). Proposal hanya punya `wireframe_transfer.png` yang mencakup sisi pengirim saja | Tambah 2 wireframe baru dan update teks sub-poin e) untuk menjelaskan alur 2 sisi |
| **`id_proyek` & `nama_proyek` di `UserSession`** | Diimplementasi (MIGRATION_3_4) tapi LaTeX CLAUDE.md masih versi lama yang tidak punya field ini | Update skema `UserSession` di LaTeX CLAUDE.md dan proposal ERD |
| **`id_proyek` di `MasterKatalog`** | Diimplementasi (MIGRATION_3_4) tapi LaTeX CLAUDE.md tidak mencantumkannya | Update skema `MasterKatalog` di LaTeX CLAUDE.md dan proposal ERD |
| **`ProfileScreen`** | Diimplementasi sebagai deviasi desain (menggantikan tombol logout di header) tapi tidak ada di proposal | Boleh dicatat sebagai deviasi desain di Bab IV/V |

### ❌ Gap — Proposal menyebut tapi tidak diimplementasi
| Gap | Detail |
|---|---|
| **Enum `status_transfer` di proposal pakai `PENDING_TRANSFER`** | Teks proposal sub-poin e) menyebut `status_transfer = PENDING_TRANSFER` tapi enum aktual adalah `IN_TRANSIT`. Perlu koreksi di proposal. |

### 📋 Deviasi Desain yang Disengaja (bukan bug, bukan gap)
Lihat bagian "Deviasi desain" di `IMPLEMENTATION_GUIDE.md` untuk daftar lengkap.
