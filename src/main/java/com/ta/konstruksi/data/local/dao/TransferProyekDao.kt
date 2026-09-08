package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.domain.repository.HasilBuatTransfer
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransferProyekDao {
    @Query("SELECT * FROM transfer_proyek ORDER BY local_timestamp DESC")
    abstract fun getAllTransfer(): Flow<List<TransferProyekEntity>>

    // Riwayat per proyek + berpaginasi (B2/B3): transfer yang MELIBATKAN proyek aktif sebagai
    // pengirim (asal) ATAU penerima (tujuan), terbaru dulu, dibatasi LIMIT. COLLATE NOCASE karena
    // proyek disimpan sebagai nama.
    @Query(
        "SELECT * FROM transfer_proyek WHERE proyek_asal = :nama COLLATE NOCASE " +
            "OR proyek_tujuan = :nama COLLATE NOCASE ORDER BY local_timestamp DESC LIMIT :limit"
    )
    abstract fun getRiwayatTransfer(nama: String, limit: Int): Flow<List<TransferProyekEntity>>

    @Query("SELECT * FROM transfer_proyek WHERE id_transfer = :id")
    abstract suspend fun getById(id: String): TransferProyekEntity?

    // Transfer IN_TRANSIT yang menuju proyek [nama] — untuk notifikasi "aset masuk".
    // COLLATE NOCASE karena proyek_tujuan diisi berdasarkan nama proyek (registry).
    @Query("SELECT * FROM transfer_proyek WHERE status_transfer = 'IN_TRANSIT' AND proyek_tujuan = :nama COLLATE NOCASE")
    abstract suspend fun getIncomingInTransit(nama: String): List<TransferProyekEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(transfer: TransferProyekEntity)

    // Bump local_timestamp agar perubahan status menang LWW saat ditarik perangkat lain.
    @Query("UPDATE transfer_proyek SET status_transfer = :status, sync_status = 'PENDING', local_timestamp = :timestamp WHERE id_transfer = :id")
    abstract suspend fun updateStatus(id: String, status: String, timestamp: Long)

    // Sertakan IN_TRANSIT agar item yang tersinkron terputus dipulihkan pada siklus berikutnya
    @Query("SELECT * FROM transfer_proyek WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    abstract suspend fun getPending(): List<TransferProyekEntity>

    // Batch unggah terbatas (B1).
    @Query("SELECT * FROM transfer_proyek WHERE sync_status IN ('PENDING', 'IN_TRANSIT') ORDER BY local_timestamp ASC LIMIT :limit")
    abstract suspend fun getPendingBatch(limit: Int): List<TransferProyekEntity>

    @Query("SELECT COUNT(*) FROM transfer_proyek WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    abstract suspend fun countPending(): Int

    @Query("UPDATE transfer_proyek SET sync_status = :status WHERE id_transfer = :id")
    abstract suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE transfer_proyek SET sync_status = 'SYNCED' WHERE id_transfer = :id")
    abstract suspend fun markSynced(id: String)

    // --- Operasi katalog penunjang konfirmasi/pembatalan atomik ---
    @Query("SELECT * FROM master_katalog WHERE id_proyek = :idProyek AND nama_barang = :nama LIMIT 1")
    abstract suspend fun cariKatalogProyekNama(idProyek: String, nama: String): MasterKatalogEntity?

    @Query("SELECT stok_lokal FROM master_katalog WHERE sku_id = :sku")
    abstract suspend fun stokKatalog(sku: String): Int?

    @Query("UPDATE master_katalog SET stok_lokal = stok_lokal + :delta, sync_status = 'PENDING' WHERE sku_id = :sku")
    abstract suspend fun tambahStokKatalog(sku: String, delta: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertKatalog(item: MasterKatalogEntity)

    /**
     * Buat transfer secara ATOMIK & idempoten. Guard baca-cek-tulis dalam satu transaksi:
     * (1) tolak bila id_transfer sudah ada — ketukan kedua (id dibangkitkan sekali saat dialog
     * dibuka) tidak menambah baris maupun mengurangi stok lagi; (2) pastikan stok proyek asal
     * cukup; (3) kurangi stok (PENDING) lalu simpan transfer. Ini pelengkap penjaga sinkron di
     * ViewModel — tetap aman walau guard UI terlewat (rekomposisi / mati proses).
     */
    @Transaction
    open suspend fun buatTransferAtomik(t: TransferProyekEntity): HasilBuatTransfer {
        if (getById(t.id_transfer) != null) return HasilBuatTransfer.DUPLIKAT
        val stok = stokKatalog(t.sku_id) ?: return HasilBuatTransfer.STOK_KURANG
        if (stok < t.jumlah) return HasilBuatTransfer.STOK_KURANG
        tambahStokKatalog(t.sku_id, -t.jumlah)   // kurangi stok proyek asal
        insert(t)
        return HasilBuatTransfer.SUKSES
    }

    /**
     * Konfirmasi penerimaan transfer secara ATOMIK: baca status, pastikan masih IN_TRANSIT,
     * tambah stok ke katalog proyek penerima (buat entri baru bila belum ada), lalu ubah status
     * jadi SELESAI — semuanya dalam satu transaksi. Guard baca-cek-tulis ini mencegah stok dobel
     * akibat double-submit maupun konfirmasi ulang transfer yang sama (mis. tertarik pull).
     *
     * @return true bila benar-benar dikonfirmasi; false bila transfer tak ada / bukan IN_TRANSIT.
     */
    @Transaction
    open suspend fun konfirmasiTerimaAtomik(
        idTransfer: String,
        idProyekPenerima: String,
        now: Long,
        skuBaru: String
    ): Boolean {
        val t = getById(idTransfer) ?: return false
        if (t.status_transfer != "IN_TRANSIT") return false   // sudah SELESAI/DIBATALKAN → tolak
        if (t.nama_barang.isBlank()) {
            // Transfer lama tanpa snapshot: tambah stok pada sku asal (perilaku lama).
            tambahStokKatalog(t.sku_id, t.jumlah)
        } else {
            val existing = cariKatalogProyekNama(idProyekPenerima, t.nama_barang)
            if (existing != null) {
                tambahStokKatalog(existing.sku_id, t.jumlah)
            } else {
                insertKatalog(
                    MasterKatalogEntity(
                        sku_id = skuBaru,
                        kategori = t.kategori,
                        nama_barang = t.nama_barang,
                        satuan = t.satuan,
                        stok_lokal = t.jumlah,
                        stok_minimum = 0,
                        id_proyek = idProyekPenerima,
                        sync_status = "PENDING",
                        local_timestamp = now
                    )
                )
            }
        }
        updateStatus(idTransfer, "SELESAI", now)
        return true
    }

    /**
     * Batalkan transfer secara ATOMIK: pastikan masih IN_TRANSIT, kembalikan stok ke proyek asal,
     * lalu ubah status jadi DIBATALKAN. @return false bila sudah bukan IN_TRANSIT.
     */
    @Transaction
    open suspend fun batalkanAtomik(idTransfer: String, now: Long): Boolean {
        val t = getById(idTransfer) ?: return false
        if (t.status_transfer != "IN_TRANSIT") return false
        tambahStokKatalog(t.sku_id, t.jumlah)
        updateStatus(idTransfer, "DIBATALKAN", now)
        return true
    }
}
