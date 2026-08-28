// Black-Box No. 45 (Penolakan Akun Dinonaktifkan) — lapisan Firebase Auth.
//
// Membuktikan separuh pertama skenario: "Masuk ulang online pada akun yang
// telah dicabut admin -> Tolak otentikasi". Ini perilaku bawaan Firebase Auth
// (bukan logika aplikasi), tetapi tetap diverifikasi di sini secara konkret
// lewat Auth emulator: admin menonaktifkan akun (admin.auth().updateUser
// dengan disabled: true, meniru aksi "nonaktifkan mandor" di website admin),
// lalu percobaan login berikutnya dari sisi mobile harus gagal dengan kode
// auth/user-disabled.
//
// Batasan yang jujur dicatat (lihat juga komentar di firestore.rules): ID
// token yang SUDAH terbit SEBELUM akun dinonaktifkan tetap kriptografis
// valid sampai masa berlakunya habis secara alami -- ini bawaan Firebase,
// bukan sesuatu yang bisa "diperbaiki" dari sisi rules maupun aplikasi.
// Proteksi yang diuji di sini efektif pada percobaan LOGIN ULANG.
//
// Jalankan dengan: cd firestore-tests && npm install && npm run test:emulators

const assert = require('assert');

const PROJECT_ID = 'demo-inventaris-test';
const AUTH_EMULATOR_HOST = 'localhost:9099';

process.env.FIREBASE_AUTH_EMULATOR_HOST = AUTH_EMULATOR_HOST;

const { initializeApp: initializeAdminApp, deleteApp: deleteAdminApp } = require('firebase-admin/app');
const { getAuth: getAdminAuth } = require('firebase-admin/auth');
const { initializeApp, deleteApp } = require('firebase/app');
const {
  getAuth,
  connectAuthEmulator,
  signInWithEmailAndPassword
} = require('firebase/auth');

describe('Firebase Auth emulator — akun dinonaktifkan admin', () => {
  const email = 'mandor.dicabut@skn.app'; // pola sintetis identik dgn usernameToEmail() aplikasi
  const password = 'rahasia123';

  let adminApp;
  let clientApp;
  let clientAuth;
  let uid;

  before(async function () {
    this.timeout(20000);
    adminApp = initializeAdminApp(
      { projectId: PROJECT_ID },
      'admin-test-app-' + Date.now()
    );
    const user = await getAdminAuth(adminApp).createUser({ email, password });
    uid = user.uid;

    clientApp = initializeApp(
      { apiKey: 'fake-api-key-emulator', projectId: PROJECT_ID },
      'client-test-app-' + Date.now()
    );
    clientAuth = getAuth(clientApp);
    connectAuthEmulator(clientAuth, `http://${AUTH_EMULATOR_HOST}`, { disableWarnings: true });
  });

  after(async () => {
    await getAdminAuth(adminApp).deleteUser(uid).catch(() => {});
    await deleteAdminApp(adminApp).catch(() => {});
    await deleteApp(clientApp).catch(() => {});
  });

  it('login berhasil selama akun masih aktif', async () => {
    const cred = await signInWithEmailAndPassword(clientAuth, email, password);
    assert.strictEqual(cred.user.uid, uid);
  });

  it('login ditolak (auth/user-disabled) setelah admin menonaktifkan akun', async () => {
    await getAdminAuth(adminApp).updateUser(uid, { disabled: true });

    await assert.rejects(
      () => signInWithEmailAndPassword(clientAuth, email, password),
      (err) => {
        assert.strictEqual(err.code, 'auth/user-disabled');
        return true;
      }
    );
  });
});
