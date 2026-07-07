package com.ta.konstruksi.data.local.dao

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProyekEntity>)

    @Query("DELETE FROM proyek")
    suspend fun deleteAll()

    /** Ganti seluruh isi tabel dengan snapshot terbaru dari server (registry read-only). */
    @Transaction
    suspend fun replaceAll(items: List<ProyekEntity>) {
        deleteAll()
        insertAll(items)
    }
}
