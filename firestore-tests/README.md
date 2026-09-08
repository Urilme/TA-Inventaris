# Firestore Security Rules & Auth Emulator Tests (Black-Box No. 45)

Pengujian ini terpisah dari build Android/Gradle — berjalan di Node.js lewat
Firebase Local Emulator Suite. Membuktikan skenario **No. 45 (Penolakan Akun
Dinonaktifkan)**:

- `rules.test.js` — lapisan Firestore Security Rules (`request.auth != null`).
- `auth-disabled.test.js` — lapisan Firebase Auth (login ditolak setelah
  admin menonaktifkan akun via `disabled: true`).

## Prasyarat

1. **Node.js** (sudah kamu punya, karena `npm`/`node` dipakai proyek lain juga).
2. **Java 11+** — cukup, tidak perlu Java 21. `package.json` sudah mengunci
   `firebase-tools` ke versi `13.29.1` (bukan `npx firebase-tools` yang selalu
   menarik versi terbaru): versi 13.x masih mensyaratkan Java 11+, sedangkan
   `firebase-tools` 15.x ke atas mewajibkan Java 21+ dan bisa gagal dengan pesan
   semacam "no longer supports Java version before 21" begitu `npx` menariknya.
   Kalau masih ingin memakai versi terbaru: Android Studio sudah membawa JDK 21
   sendiri (JetBrains Runtime) di `C:\Program Files\Android\Android Studio\jbr` —
   arahkan `JAVA_HOME` ke sana untuk sesi terminal itu saja (lihat catatan di
   bawah), tanpa mengubah Java default sistem.
3. **Firebase CLI** — tidak perlu instal global; `npm install` menariknya sebagai
   `devDependency` lokal (lihat poin 2), dipanggil otomatis lewat `npm run test:emulators`.

## Cara menjalankan

```bash
cd firestore-tests
npm install
npm run test:emulators
```

Kalau tetap ingin memakai `firebase-tools` versi terbaru (Java 21+) alih-alih versi
terkunci di atas, di Windows PowerShell set dulu `JAVA_HOME` ke JBR Android Studio
untuk sesi ini:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

Perintah kedua otomatis: menyalakan emulator Firestore (port 8080) + Auth
(port 9099) sesuai `../firebase.json`, menjalankan `npm test` (Mocha) di
dalamnya, lalu mematikan emulator. Tidak menyentuh project Firebase
produksi sama sekali (`--project demo-inventaris-test` adalah project palsu
khusus emulator).

Kalau ingin melihat emulator UI sambil jalan (opsional, untuk cek manual):

```bash
npx firebase-tools emulators:start --project demo-inventaris-test --config ../firebase.json
# lalu di terminal lain:
npm test
```

Buka `http://localhost:4000` untuk Emulator UI.

## Deploy Firestore Rules & Composite Indexes

Sinkronisasi delta men-scoping data di sisi server: `pullKatalog`/`pullMutasi`
memakai `whereEqualTo("id_proyek", ...)` dan `pullTransfer` memakai dua kueri
`whereEqualTo("proyek_tujuan"/"proyek_asal", ...)`, semuanya dikombinasikan dengan
`whereGreaterThan("server_timestamp", ...)`. **Kombinasi equality + inequality pada
field berbeda WAJIB punya composite index** — tanpa index ter-deploy, kueri gagal di
runtime (`FAILED_PRECONDITION: The query requires an index`).

Index sudah didefinisikan di `../firestore.indexes.json` (4 index: katalog, mutasi,
transfer×2) dan dirujuk oleh `../firebase.json`. Deploy dari root repo:

```bash
# Rules + indexes sekaligus:
npx firebase-tools deploy --only firestore --project <PROJECT_ID_PRODUKSI>

# Atau terpisah:
npx firebase-tools deploy --only firestore:indexes --project <PROJECT_ID_PRODUKSI>
npx firebase-tools deploy --only firestore:rules   --project <PROJECT_ID_PRODUKSI>
```

Ganti `<PROJECT_ID_PRODUKSI>` dengan project Firebase asli (mis. `inventaris-9f544`).
Pembuatan index berjalan asinkron di server (bisa beberapa menit) — pantau statusnya
di Firebase Console → Firestore → Indexes hingga **Enabled** sebelum rilis app.

> Kompatibilitas dokumen lama: dokumen yang belum memiliki `id_proyek` tidak akan
> ikut tertarik oleh `whereEqualTo` (bukan error, hanya terlewat). Karena setiap
> unggahan dari app selalu menuliskan `id_proyek`, dokumen akan ter-scoping benar
> setelah ter-sinkron ulang; dokumen lama yang tak pernah di-update perlu di-backfill
> `id_proyek` satu kali (mis. lewat skrip admin) bila ingin tetap tertarik.

## Catatan penting

- **Status: sudah dijalankan & LULUS (6/6).** Diuji di emulator nyata (Firestore
  port 8080 + Auth port 9099) dengan JDK 21 (JBR Android Studio 21.0.6) dan
  Node.js 22 — kedua berkas test hijau: 2 test Auth (login sukses; ditolak
  `auth/user-disabled` setelah dinonaktifkan) dan 4 test Rules (tolak anonim,
  izinkan terautentikasi). Baris `PERMISSION_DENIED ... false for 'create'`
  yang muncul di tengah output adalah keluaran **yang diharapkan** dari kasus
  `assertFails` (bukti rules memang menolak), bukan kegagalan test.
- **Versi dependency:** `package.json` memakai `@firebase/rules-unit-testing@^4.0.1`
  (peer `firebase@^11`) agar selaras dengan `firebase@^11.x`, sehingga `npm install`
  berjalan bersih tanpa perlu `--legacy-peer-deps`.
- `firestore.rules` sebelumnya **terbuka total** (`allow read, write: if true`)
  untuk semua koleksi. Sudah diperketat menjadi `request.auth != null` —
  sudah diverifikasi (lihat riwayat chat) bahwa baik aplikasi mobile
  (TA-Inventaris) maupun website admin (TA-Web-Inventaris) selalu mengakses
  Firestore dalam konteks user yang sudah login, jadi perubahan ini aman.
- Batasan yang jujur perlu dicatat di laporan: ID token yang **sudah terbit
  sebelum** akun dinonaktifkan tetap valid sampai kedaluwarsa alami (bawaan
  Firebase). Proteksi ini efektif pada **percobaan login ulang**, bukan sesi
  yang sedang berjalan aktif.
