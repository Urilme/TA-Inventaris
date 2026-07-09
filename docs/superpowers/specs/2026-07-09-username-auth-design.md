# Desain: Autentikasi Username (menggantikan Email)

**Tanggal:** 2026-07-09
**Fitur terkait:** F1 — Autentikasi & Manajemen Sesi Offline
**Status:** Disetujui, siap ke rencana implementasi

## Latar Belakang & Masalah

Login saat ini memakai Firebase Authentication provider **Email/Password**. Namun mandor
di PT SKN tidak punya alamat email yang terkait; akun mereka dibuat oleh **admin website**
(belum dibangun) tanpa email nyata. Menampilkan "Email" di layar login membingungkan dan
memaksa admin mengarang alamat email.

Firebase Auth **tidak menyediakan provider "username/password"**. Solusi standar dan paling
aman adalah **username-as-synthetic-email**: user mengetik username, app menambahkan domain
internal tetap (`@skn.app`) sebelum memanggil Firebase. Semua manfaat Firebase (hashing
password, manajemen token, sesi luring yang sudah diimplementasi) tetap dipertahankan.

**Alternatif yang ditolak:**
- *Custom Auth (custom token)* — butuh backend/Cloud Function, menyimpan hash password sendiri,
  kehilangan keamanan bawaan Firebase. Overkill untuk lingkup TA.
- *Cek username+password langsung di Firestore* — tidak aman.

## Konvensi

- **Domain sintetis tetap:** `skn.app`. Jelas bukan alamat email betulan; disembunyikan dari user.
- **Pemetaan:** `email = "${username.trim().lowercase()}@skn.app"`.
- **Normalisasi username:** selalu `trim` + `lowercase` sebelum dipakai — agar `Budi`, `budi `,
  dan `budi` merujuk ke satu akun yang sama.
- **Validasi (longgar):** wajib tidak kosong; tolak hanya bila mengandung `@` atau spasi
  (pesan: "Username tidak valid"). Selebihnya bebas — admin menentukan username.
- Domain **wajib sama persis** antara app dan admin website yang akan dibangun.

## Perubahan

### 1. Constants.kt — helper konversi
```kotlin
const val AUTH_DOMAIN = "skn.app"
fun usernameToEmail(username: String) = "${username.trim().lowercase()}@$AUTH_DOMAIN"
```

### 2. Model data + migrasi Room v6 → v7
Rename kolom **`email` → `username`**, isi **username murni** (`budi`, bukan `budi@skn.app`) di:
- `data/local/entity/UserSessionEntity.kt`
- `domain/model/UserSession.kt`
- `data/repository/Mappers.kt` (dan mapper di dalam `AuthRepositoryImpl` bila ada)

Migrasi memakai pola **recreate-table** (SQLite pada rentang Android target tidak menjamin
`ALTER TABLE DROP COLUMN`):
1. `CREATE TABLE user_session_new (...)` dengan kolom `username` menggantikan `email`.
2. Salin data: `username = substr(email, 1, instr(email,'@') - 1)` bila email berisi `@`,
   jika tidak, salin apa adanya — supaya **sesi luring yang sudah tersimpan tetap cocok**.
3. `DROP TABLE user_session; ALTER TABLE user_session_new RENAME TO user_session;`

Naikkan `version = 7` di `KonstruksiDatabase.kt`, daftarkan `MIGRATION_6_7` di
`di/DatabaseModule.kt`, tambah `schemas/com.ta.konstruksi.data.local.KonstruksiDatabase/7.json`
(diverifikasi cocok setelah build menghasilkan skema).

### 3. Repository — `AuthRepositoryImpl`
- `login(username, password)`:
  - Normalisasi username; validasi (non-blank, tanpa `@`/spasi).
  - `val email = usernameToEmail(username)` → `signInWithEmailAndPassword(email, password)`.
  - Simpan `session.username = username` (bukan email penuh).
- `buatSesiDari(...)`: turunkan username dari `user.email.substringBefore("@")`;
  `namaDariEmail` → `namaDariUsername` (logika pembentukan nama tampilan sama).
- `loginLuring(username)`: bandingkan `lokal.username` dengan input yang sudah dinormalisasi.
- Pesan error:
  - "Email dan kata sandi tidak boleh kosong" → "Username dan kata sandi tidak boleh kosong"
  - "Email atau kata sandi salah" → "Username atau kata sandi salah"

### 4. Domain / UseCase / ViewModel
Rename parameter `email` → `username` (murni penamaan + pesan) di:
- `domain/repository/AuthRepository.kt`
- `domain/usecase/auth/LoginUseCase.kt`
- `presentation/viewmodel/AuthViewModel.kt`

### 5. UI
- **LoginScreen**: label "Email" → "Username"; ikon `Icons.Default.Email` → `Icons.Default.Person`;
  placeholder `"email@domain.com"` → `"username"`; `KeyboardType.Email` → `KeyboardType.Text`;
  variabel state `email` → `username`.
- **ProfileScreen**: `InfoRow` ikon `Email` → `Person`; label "Email" → "Username";
  nilai `session?.username`.

## Langkah Manual (di luar kode)

- Buat akun uji **`budi@skn.app`** (password `rahasia123`) di **Firebase Console** — akun lama
  `budi@skn.com` tidak lagi dapat login lewat alur username. Perbarui memori test-account.
- Saat membangun admin website: buat akun Firebase dengan email `username@skn.app`
  (domain identik dengan konstanta app).

## Di Luar Lingkup (YAGNI)

Sinkronisasi, dokumen Firestore `users`, WorkManager, dan seluruh fitur lain **tidak berubah**.
Ini murni penggantian identitas login; alur data tidak tersentuh.

## Dampak Dokumentasi TA

Proposal/CLAUDE.md LaTeX menyebut auth "email/password" (F1). Perubahan ini menjadi satu poin
**deviasi desain** yang perlu dicatat beserta alasannya (mandor tanpa email; admin provisioning
berbasis username). Fitur reset-password-via-email memang tidak dipakai (tanpa self-registration),
jadi tidak terdampak.

## Kriteria Sukses

1. Login berhasil dengan mengetik `budi` + password (app menambahkan `@skn.app` internal).
2. Layar Login & Profil menampilkan "Username", tanpa kebocoran domain `@skn.app` ke user.
3. Migrasi v6 → v7 jalan tanpa kehilangan sesi; login luring tetap berfungsi untuk sesi lama.
4. Build `assembleDebug` lulus; skema Room 7.json konsisten.
