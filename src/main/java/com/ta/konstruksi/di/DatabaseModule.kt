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

    /**
     * Migrasi v2 → v3:
     *  - master_katalog: tambah kolom stok_minimum (ambang peringatan Dashboard).
     *  - log_mutasi: jadikan kondisi_barang NULLABLE (SQLite tak bisa melonggarkan NOT NULL
     *    secara langsung, jadi tabel direkreasi lalu datanya disalin).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1) Kolom stok_minimum pada master_katalog
            db.execSQL("ALTER TABLE master_katalog ADD COLUMN stok_minimum INTEGER NOT NULL DEFAULT 0")

            // 2) Rekreasi log_mutasi dengan kondisi_barang nullable
            db.execSQL(
                """
                CREATE TABLE log_mutasi_new (
                    id_transaksi TEXT NOT NULL,
                    sku_id TEXT NOT NULL,
                    id_mandor TEXT NOT NULL DEFAULT '',
                    id_proyek TEXT NOT NULL DEFAULT '',
                    jenis_mutasi TEXT NOT NULL,
                    jumlah INTEGER NOT NULL,
                    kondisi_barang TEXT,
                    lokasi_penggunaan TEXT NOT NULL DEFAULT '',
                    penanggungjawab TEXT NOT NULL DEFAULT '',
                    catatan TEXT,
                    local_timestamp INTEGER NOT NULL,
                    sync_status TEXT NOT NULL,
                    PRIMARY KEY(id_transaksi),
                    FOREIGN KEY(sku_id) REFERENCES master_katalog(sku_id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO log_mutasi_new (
                    id_transaksi, sku_id, id_mandor, id_proyek, jenis_mutasi, jumlah,
                    kondisi_barang, lokasi_penggunaan, penanggungjawab, catatan, local_timestamp, sync_status
                )
                SELECT id_transaksi, sku_id, id_mandor, id_proyek, jenis_mutasi, jumlah,
                    kondisi_barang, lokasi_penggunaan, penanggungjawab, catatan, local_timestamp, sync_status
                FROM log_mutasi
                """.trimIndent()
            )
            db.execSQL("DROP TABLE log_mutasi")
            db.execSQL("ALTER TABLE log_mutasi_new RENAME TO log_mutasi")
        }
    }

    /**
     * Migrasi v3 → v4: model kepemilikan proyek.
     *  - user_session: tambah id_proyek & nama_proyek (proyek yang ditugaskan ke mandor).
     *  - master_katalog: tambah id_proyek (katalog difilter per proyek).
     * Semua kolom baru NOT NULL DEFAULT '' agar cocok dgn @ColumnInfo(defaultValue = "").
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_session ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE user_session ADD COLUMN nama_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE master_katalog ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")

            // Backfill data lama ke proyek default ('PROYEK-UTAMA' = Constants.DEFAULT_PROYEK).
            // Sebelum fitur ini hanya ada satu proyek implisit, jadi seluruh katalog & sesi
            // yang sudah ada menjadi milik proyek default agar tidak "hilang" dari tampilan
            // (yang kini disaring per proyek). Nilai di-hardcode karena migrasi harus statis.
            db.execSQL("UPDATE master_katalog SET id_proyek = 'PROYEK-UTAMA' WHERE id_proyek = ''")
            db.execSQL("UPDATE user_session SET id_proyek = 'PROYEK-UTAMA' WHERE id_proyek = ''")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KonstruksiDatabase =
        Room.databaseBuilder(context, KonstruksiDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun provideUserSessionDao(db: KonstruksiDatabase): UserSessionDao = db.userSessionDao()
    @Provides fun provideMasterKatalogDao(db: KonstruksiDatabase): MasterKatalogDao = db.masterKatalogDao()
    @Provides fun provideLogMutasiDao(db: KonstruksiDatabase): LogMutasiDao = db.logMutasiDao()
    @Provides fun provideTransferProyekDao(db: KonstruksiDatabase): TransferProyekDao = db.transferProyekDao()
}
