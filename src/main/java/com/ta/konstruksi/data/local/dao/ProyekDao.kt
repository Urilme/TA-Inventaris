package com.ta.konstruksi.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ta.konstruksi.data.local.entity.ProyekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProyekDao {
    @Query("SELECT * FROM proyek ORDER BY nama_proyek ASC")
    fun getAll(): Flow<List<ProyekEntity>>

    @Query("SELECT id_proyek FROM proyek")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProyekEntity>)

    @Query("DELETE FROM proyek")
    suspend fun deleteAll()

    @Query("DELETE FROM proyek WHERE id_proyek = :id")
    suspend fun deleteById(id: String)

    /**
     * Ganti isi tabel dengan snapshot terbaru dari server (registry read-only).
     *
     * Sejak v10, Master_Katalog dan Log_Mutasi punya foreign key ke tabel ini dengan
     * ON DELETE RESTRICT — proyek yang masih dipakai data lokal tidak boleh hilang begitu
     * saja. Karena itu proses ini memakai pola UPSERT (bukan hapus-semua-lalu-isi-ulang):
     * seluruh data terbaru ditulis lewat insertAll (REPLACE by id_proyek), lalu proyek yang
     * sudah tidak ada di snapshot server dicoba dihapus satu per satu. Kalau penghapusan
     * ditolak SQLite karena masih direferensikan katalog/mutasi lokal, itu dilewati saja —
     * bukan kegagalan, melainkan bukti FK RESTRICT bekerja sebagaimana mestinya. Proyek
     * tersebut akan otomatis bisa dibersihkan pada sinkronisasi berikutnya begitu referensi
     * lokalnya sudah tidak ada lagi.
     */
    @Transaction
    suspend fun replaceAll(items: List<ProyekEntity>) {
        insertAll(items)
        val incomingIds = items.map { it.id_proyek }.toSet()
        val staleIds = getAllIds().filterNot { it in incomingIds }
        for (id in staleIds) {
            try {
                deleteById(id)
            } catch (_: SQLiteConstraintException) {
                // Masih dipakai Master_Katalog/Log_Mutasi lokal — dibiarkan, bukan error fatal.
            }
        }
    }
}
