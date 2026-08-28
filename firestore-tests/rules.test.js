// Black-Box No. 45 (Penolakan Akun Dinonaktifkan) — lapisan Security Rules.
//
// Membuktikan bahwa firestore.rules (diperketat dari `if true` menjadi
// `request.auth != null`) benar-benar menolak permintaan tanpa autentikasi,
// dan mengizinkan permintaan yang sudah terautentikasi. Ini adalah bagian
// dari klaim skenario yang SEBELUMNYA tidak benar (rules lama terbuka total
// untuk semua koleksi tanpa syarat apa pun).
//
// Dijalankan via Firebase Local Emulator Suite (Firestore emulator), BUKAN
// project Firestore produksi. Jalankan dengan:
//   cd firestore-tests && npm install && npm run test:emulators

const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'demo-inventaris-test';

let testEnv;

before(async function () {
  this.timeout(20000);
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: 'localhost',
      port: 8080
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

describe('firestore.rules — request.auth != null', () => {
  it('menolak tulis ke /mutasi tanpa autentikasi', async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(db.collection('mutasi').doc('TX-1').set({ jumlah: 1 }));
  });

  it('menolak baca /katalog tanpa autentikasi', async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(db.collection('katalog').doc('SKU-1').get());
  });

  it('mengizinkan tulis ke /mutasi untuk user yang sudah terautentikasi', async () => {
    const db = testEnv.authenticatedContext('mandor-uid-1').firestore();
    await assertSucceeds(db.collection('mutasi').doc('TX-1').set({ jumlah: 1 }));
  });

  it('mengizinkan baca /katalog untuk user yang sudah terautentikasi', async () => {
    const db = testEnv.authenticatedContext('mandor-uid-1').firestore();
    await assertSucceeds(db.collection('katalog').doc('SKU-1').get());
  });
});
