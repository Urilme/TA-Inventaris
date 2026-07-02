package com.ta.konstruksi.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ta.konstruksi.data.local.KonstruksiDatabase
import com.ta.konstruksi.data.local.dao.*
import com.ta.konstruksi.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migrasi v1 → v2: menambahkan atribut baru pada tabel log_mutasi.
     * Nilai DEFAULT wajib sama persis dengan @ColumnInfo(defaultValue=...) di LogMutasiEntity
     * agar validasi skema Room lolos (JANGAN pakai fallbackToDestructiveMigration).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN id_mandor TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN kondisi_barang TEXT NOT NULL DEFAULT 'BAIK'")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN lokasi_penggunaan TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN penanggungjawab TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KonstruksiDatabase =
        Room.databaseBuilder(context, KonstruksiDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideUserSessionDao(db: KonstruksiDatabase): UserSessionDao = db.userSessionDao()
    @Provides fun provideMasterKatalogDao(db: KonstruksiDatabase): MasterKatalogDao = db.masterKatalogDao()
    @Provides fun provideLogMutasiDao(db: KonstruksiDatabase): LogMutasiDao = db.logMutasiDao()
    @Provides fun provideTransferProyekDao(db: KonstruksiDatabase): TransferProyekDao = db.transferProyekDao()
}
