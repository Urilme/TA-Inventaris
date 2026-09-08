// Firestore Security Rules — otorisasi per-peran & per-proyek.
//
// Membuktikan firestore.rules granular: mandor hanya boleh menulis data proyeknya,
// tak boleh menaikkan role/pindah proyek dirinya, tak boleh menulis registry proyek,
// hanya membaca dokumen users miliknya; sementara SELURUH operasi sah aplikasi (tulis
// katalog/mutasi/transfer di proyek sendiri, konfirmasi transfer oleh penerima, tulis
// registry oleh admin) TETAP berhasil.
//
// Dijalankan via Firebase Local Emulator Suite (BUKAN produksi):
//   cd firestore-tests && npm install && npm run test:emulators

const fs = require('fs');
const path = require('path');
const { setLogLevel } = require('firebase/firestore');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');

// Firestore SDK secara default mencetak setiap penolakan (assertFails) sebagai baris
// "GrpcConnection ... PERMISSION_DENIED" ke console — bukan kegagalan, tapi bikin output
// terminal sangat panjang dan tidak layak dijadikan screenshot dokumentasi. Dibungkam di sini.
setLogLevel('silent');

const PROJECT_ID = 'demo-inventaris-test';

const UID_A = 'uidA';         // mandor Proyek A (PRJ-A / "Proyek A")
const UID_B = 'uidB';         // mandor Proyek B (PRJ-B / "Proyek B")
const UID_ADMIN = 'uidAdmin'; // admin

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

// Seed data bypassing rules (setup fixtures).
async function seed(fn) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await fn(ctx.firestore());
  });
}

beforeEach(async () => {
  await testEnv.clearFirestore();
  await seed(async (db) => {
    await db.collection('users').doc(UID_A).set({ role: 'mandor', id_proyek: 'PRJ-A', nama_proyek: 'Proyek A' });
    await db.collection('users').doc(UID_B).set({ role: 'mandor', id_proyek: 'PRJ-B', nama_proyek: 'Proyek B' });
    await db.collection('users').doc(UID_ADMIN).set({ role: 'admin', id_proyek: '', nama_proyek: '' });
  });
});

const asA = () => testEnv.authenticatedContext(UID_A).firestore();
const asB = () => testEnv.authenticatedContext(UID_B).firestore();
const asAdmin = () => testEnv.authenticatedContext(UID_ADMIN).firestore();
const asAnon = () => testEnv.unauthenticatedContext().firestore();

describe('Baseline autentikasi', () => {
  it('menolak baca /katalog tanpa autentikasi', async () => {
    await assertFails(asAnon().collection('katalog').doc('SKU-1').get());
  });
  it('menolak tulis /mutasi tanpa autentikasi', async () => {
    await assertFails(asAnon().collection('mutasi').doc('TX-1').set({ jumlah: 1 }));
  });
});

describe('Katalog & mutasi terikat proyek', () => {
  it('mandor A BOLEH menulis katalog proyeknya sendiri', async () => {
    await assertSucceeds(asA().collection('katalog').doc('SKU-A').set({ id_proyek: 'PRJ-A', nama_barang: 'Semen' }));
  });
  it('mandor A DITOLAK menulis katalog proyek lain (PRJ-B)', async () => {
    await assertFails(asA().collection('katalog').doc('SKU-X').set({ id_proyek: 'PRJ-B', nama_barang: 'Semen' }));
  });
  it('mandor A DITOLAK menimpa katalog milik proyek lain', async () => {
    await seed(async (db) => { await db.collection('katalog').doc('SKU-B').set({ id_proyek: 'PRJ-B', nama_barang: 'Pasir' }); });
    // Bahkan mencoba mengklaimnya ke PRJ-A tetap ditolak (dokumen lama milik PRJ-B).
    await assertFails(asA().collection('katalog').doc('SKU-B').set({ id_proyek: 'PRJ-A', nama_barang: 'Pasir' }));
  });
  it('mandor A BOLEH menulis mutasi proyeknya', async () => {
    await assertSucceeds(asA().collection('mutasi').doc('TX-A').set({ id_proyek: 'PRJ-A', jumlah: 2 }));
  });
  it('mandor A DITOLAK menulis mutasi proyek lain', async () => {
    await assertFails(asA().collection('mutasi').doc('TX-X').set({ id_proyek: 'PRJ-B', jumlah: 2 }));
  });
});

describe('Profil users', () => {
  it('mandor A BOLEH membaca dokumennya sendiri', async () => {
    await assertSucceeds(asA().collection('users').doc(UID_A).get());
  });
  it('mandor A DITOLAK membaca dokumen mandor lain', async () => {
    await assertFails(asA().collection('users').doc(UID_B).get());
  });
  it('mandor A DITOLAK menaikkan role dirinya menjadi admin', async () => {
    await assertFails(asA().collection('users').doc(UID_A).update({ role: 'admin' }));
  });
  it('mandor A DITOLAK memindahkan dirinya ke proyek lain', async () => {
    await assertFails(asA().collection('users').doc(UID_A).update({ id_proyek: 'PRJ-B' }));
  });
  it('mandor A BOLEH memperbarui field lain selama role & id_proyek tetap', async () => {
    await assertSucceeds(asA().collection('users').doc(UID_A).update({ nama: 'Budi Perbaikan' }));
  });
  it('admin BOLEH mengubah role mandor', async () => {
    await assertSucceeds(asAdmin().collection('users').doc(UID_A).update({ role: 'admin' }));
  });
});

describe('Registry proyek', () => {
  beforeEach(async () => {
    await seed(async (db) => { await db.collection('proyek').doc('PRJ-A').set({ id_proyek: 'PRJ-A', nama_proyek: 'Proyek A' }); });
  });
  it('mandor A BOLEH membaca registry proyek', async () => {
    await assertSucceeds(asA().collection('proyek').doc('PRJ-A').get());
  });
  it('mandor A DITOLAK menulis registry proyek', async () => {
    await assertFails(asA().collection('proyek').doc('PRJ-C').set({ nama_proyek: 'Proyek Palsu' }));
  });
  it('admin BOLEH menulis registry proyek', async () => {
    await assertSucceeds(asAdmin().collection('proyek').doc('PRJ-C').set({ id_proyek: 'PRJ-C', nama_proyek: 'Proyek C' }));
  });
});

describe('Transfer antar proyek', () => {
  it('mandor A (pengirim) BOLEH membuat transfer dari proyeknya', async () => {
    await assertSucceeds(asA().collection('transfer').doc('TR-1').set({
      proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 2, status_transfer: 'IN_TRANSIT'
    }));
  });
  it('mandor A DITOLAK membuat transfer atas nama proyek lain', async () => {
    await assertFails(asA().collection('transfer').doc('TR-2').set({
      proyek_asal: 'Proyek B', proyek_tujuan: 'Proyek A', jumlah: 2, status_transfer: 'IN_TRANSIT'
    }));
  });
  it('mandor B (penerima) BOLEH mengonfirmasi transfer yang menuju proyeknya', async () => {
    await seed(async (db) => {
      await db.collection('transfer').doc('TR-3').set({
        sku_id: 'SKU-1', proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 2, status_transfer: 'IN_TRANSIT'
      });
    });
    await assertSucceeds(asB().collection('transfer').doc('TR-3').update({ status_transfer: 'SELESAI' }));
  });
  it('mandor A DITOLAK mengubah transfer yang bukan asal/tujuan proyeknya', async () => {
    await seed(async (db) => {
      await db.collection('transfer').doc('TR-4').set({
        sku_id: 'SKU-1', proyek_asal: 'Proyek B', proyek_tujuan: 'Proyek C', jumlah: 2, status_transfer: 'IN_TRANSIT'
      });
    });
    await assertFails(asA().collection('transfer').doc('TR-4').update({ status_transfer: 'SELESAI' }));
  });
  // Hardening: field identitas transfer (jumlah, sku_id, proyek_asal/tujuan) terkunci saat update —
  // hanya status_transfer yang boleh bergerak. Mencegah penerima menggelembungkan jumlah saat konfirmasi.
  it('mandor B (penerima) DITOLAK mengubah jumlah saat konfirmasi', async () => {
    await seed(async (db) => {
      await db.collection('transfer').doc('TR-5').set({
        sku_id: 'SKU-1', proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 2, status_transfer: 'IN_TRANSIT'
      });
    });
    await assertFails(asB().collection('transfer').doc('TR-5').update({ status_transfer: 'SELESAI', jumlah: 999 }));
  });
  it('mandor B (penerima) BOLEH konfirmasi bila hanya status berubah (identitas tetap)', async () => {
    await seed(async (db) => {
      await db.collection('transfer').doc('TR-6').set({
        sku_id: 'SKU-1', proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 2, status_transfer: 'IN_TRANSIT'
      });
    });
    // Kirim ulang seluruh dokumen dengan nilai identitas sama persis (meniru .set() dari app).
    await assertSucceeds(asB().collection('transfer').doc('TR-6').set({
      sku_id: 'SKU-1', proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 2, status_transfer: 'SELESAI'
    }));
  });
});

describe('Baca terikat proyek (C2)', () => {
  beforeEach(async () => {
    await seed(async (db) => {
      await db.collection('katalog').doc('KAT-A').set({ id_proyek: 'PRJ-A', nama_barang: 'Semen' });
      await db.collection('katalog').doc('KAT-B').set({ id_proyek: 'PRJ-B', nama_barang: 'Pasir' });
      await db.collection('mutasi').doc('MX-A').set({ id_proyek: 'PRJ-A', jumlah: 1 });
      await db.collection('mutasi').doc('MX-B').set({ id_proyek: 'PRJ-B', jumlah: 1 });
      await db.collection('transfer').doc('TR-AB').set({
        sku_id: 'S1', proyek_asal: 'Proyek A', proyek_tujuan: 'Proyek B', jumlah: 1, status_transfer: 'IN_TRANSIT'
      });
      await db.collection('transfer').doc('TR-CD').set({
        sku_id: 'S1', proyek_asal: 'Proyek C', proyek_tujuan: 'Proyek D', jumlah: 1, status_transfer: 'IN_TRANSIT'
      });
    });
  });

  // --- Katalog ---
  it('mandor A BOLEH membaca katalog proyeknya (get)', async () => {
    await assertSucceeds(asA().collection('katalog').doc('KAT-A').get());
  });
  it('mandor A DITOLAK membaca katalog proyek lain (get)', async () => {
    await assertFails(asA().collection('katalog').doc('KAT-B').get());
  });
  it('mandor A BOLEH query katalog yang difilter proyeknya', async () => {
    await assertSucceeds(asA().collection('katalog').where('id_proyek', '==', 'PRJ-A').get());
  });
  it('mandor A DITOLAK query katalog tanpa filter proyek', async () => {
    await assertFails(asA().collection('katalog').get());
  });
  it('admin BOLEH membaca katalog lintas proyek (monitoring website)', async () => {
    await assertSucceeds(asAdmin().collection('katalog').get());
  });

  // --- Mutasi ---
  it('mandor A DITOLAK membaca mutasi proyek lain (get)', async () => {
    await assertFails(asA().collection('mutasi').doc('MX-B').get());
  });
  it('mandor A BOLEH query mutasi yang difilter proyeknya', async () => {
    await assertSucceeds(asA().collection('mutasi').where('id_proyek', '==', 'PRJ-A').get());
  });
  it('admin BOLEH membaca mutasi lintas proyek', async () => {
    await assertSucceeds(asAdmin().collection('mutasi').get());
  });

  // --- Transfer (asal ATAU tujuan = proyek) ---
  it('mandor A BOLEH membaca transfer yang melibatkannya (get)', async () => {
    await assertSucceeds(asA().collection('transfer').doc('TR-AB').get());   // asal = Proyek A
  });
  it('mandor A DITOLAK membaca transfer proyek lain (get)', async () => {
    await assertFails(asA().collection('transfer').doc('TR-CD').get());      // C→D, tak melibatkan A
  });
  it('mandor B BOLEH query transfer masuk (proyek_tujuan = proyeknya)', async () => {
    await assertSucceeds(asB().collection('transfer').where('proyek_tujuan', '==', 'Proyek B').get());
  });
  it('mandor A BOLEH query transfer keluar (proyek_asal = proyeknya)', async () => {
    await assertSucceeds(asA().collection('transfer').where('proyek_asal', '==', 'Proyek A').get());
  });
  it('mandor A DITOLAK query transfer tanpa filter proyek', async () => {
    await assertFails(asA().collection('transfer').get());
  });
});
