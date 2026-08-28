package com.ta.konstruksi.data.local.dao

import androidx.room.*
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LogMutasiDao {
    @Query("SELECT * FROM log_mutasi ORDER BY local_timestamp DESC")
    abstract fun getAllMutasi(): Flow<List<LogMutasiEntity>>

    @Query("SELECT * FROM log_mutasi WHERE id_transaksi = :id")
    abstract suspend fun getById(id: String): LogMutasiEntity?

    // Jumlah riwayat mutasi untuk suatu barang — dipakai memblokir hapus katalog (FK RESTRICT).
    @Query("SELECT COUNT(*) FROM log_mutasi WHERE sku_id = :skuId")
    abstract suspend fun countBySku(skuId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(mutasi: LogMutasiEntity)

    // Sertakan IN_TRANSIT agar item yang tersinkron terputus dipulihkan pada siklus berikutnya
    @Query("SELECT * FROM log_mutasi WHERE sync_status IN ('PENDING', 'IN_TRANSIT')")
    abstract suspend fun getPending(): List<LogMutasiEntity>

    @Query("UPDATE log_mutasi SET sync_status = :status WHERE id_transaksi = :id")
    abstract suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE log_mutasi SET sync_status = 'SYNCED' WHERE id_transaksi = :id")
    abstract suspend fun markSynced(id: String)

    // --- Operasi atomik stok + mutasi (lihat recordMutasiAtomic) ---
    @Query("SELECT stok_lokal FROM master_katalog WHERE sku_id = :skuId")
    abstract suspend fun getStok(skuId: String): Int?

    @Query("UPDATE master_katalog SET stok_lokal = stok_lokal + :delta, sync_status = 'PENDING' WHERE sku_id = :skuId")
    abstract suspend fun updateStok(skuId: String, delta: Int)

    /**
     * Catat mutasi DAN perbarui stok dalam SATU transaksi atomik.
     *
     * Mencegah race condition (stok minus) saat dua CHECK_OUT bersamaan pada SKU yang sama:
     * pembacaan stok + validasi + penulisan terjadi di dalam transaksi yang sama, sehingga
     * transaksi kedua melihat stok yang sudah dikurangi oleh transaksi pertama.
     *
     * @param delta positif untuk CHECK_IN, negatif untuk CHECK_OUT.
     * @return true bila tersimpan; false bila stok tidak mencukupi (transaksi dibatalkan).
     */
    @Transaction
    open suspend fun recordMutasiAtomic(mutasi: LogMutasiEntity, delta: Int): Boolean {
        if (delta < 0) {
            val current = getStok(mutasi.sku_id) ?: return false
            if (current + delta < 0) return false   // stok tidak mencukupi → batalkan
        }
        updateStok(mutasi.sku_id, delta)
        insert(mutasi)
        return true
    }
}
