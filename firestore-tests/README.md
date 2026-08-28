# Firestore Security Rules & Auth Emulator Tests (Black-Box No. 45)

Pengujian ini terpisah dari build Android/Gradle — berjalan di Node.js lewat
Firebase Local Emulator Suite. Membuktikan skenario **No. 45 (Penolakan Akun
Dinonaktifkan)**:

- `rules.test.js` — lapisan Firestore Security Rules (`request.auth != null`).
- `auth-disabled.test.js` — lapisan Firebase Auth (login ditolak setelah
  admin menonaktifkan akun via `disabled: true`).

## Prasyarat

1. **Node.js** (sudah kamu punya, karena `npm`/`node` dipakai proyek lain juga).
2. **Java 21+** — Firestore/Auth emulator versi terbaru mensyaratkan JDK 21.
   Cek dulu: `java -version`. Kalau masih Java 11/17, tidak perlu instal apa pun
   kalau sudah ada Android Studio — pakai JDK 21 yang dibundel (JetBrains Runtime),
   biasanya di `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 21.x).
   Arahkan `JAVA_HOME` ke sana **hanya untuk sesi terminal ini** (lihat contoh di
   bawah), tanpa mengubah Java default sistem. Alternatif: instal JDK 21 dari
   [Adoptium](https://adoptium.net).
3. **Firebase CLI** — tidak perlu instal global, dipanggil lewat `npx` di script `npm run test:emulators`.

## Cara menjalankan

Windows (PowerShell) — set `JAVA_HOME` ke JBR JDK 21 dulu untuk sesi ini:

```powershell
cd firestore-tests
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
npm install
npm run test:emulators
```

macOS/Linux (kalau `java -version` sudah 21, cukup):

```bash
cd firestore-tests
npm install
npm run test:emulators
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
