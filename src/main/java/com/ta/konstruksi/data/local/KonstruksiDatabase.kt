package com.ta.konstruksi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ta.konstruksi.data.local.dao.*
import com.ta.konstruksi.data.local.entity.*

@Database(
    entities = [
        UserSessionEntity::class,
        MasterKatalogEntity::class,
        LogMutasiEntity::class,
        TransferProyekEntity::class,
        ProyekEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class KonstruksiDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun masterKatalogDao(): MasterKatalogDao
    abstract fun logMutasiDao(): LogMutasiDao
    abstract fun transferProyekDao(): TransferProyekDao
    abstract fun proyekDao(): ProyekDao
}
