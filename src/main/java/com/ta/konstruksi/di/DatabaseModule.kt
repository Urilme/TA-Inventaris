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

    // v1 → v2: tambah kolom baru pada log_mutasi.
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN id_mandor TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN kondisi_barang TEXT NOT NULL DEFAULT 'BAIK'")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN lokasi_penggunaan TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE log_mutasi ADD COLUMN penanggungjawab TEXT NOT NULL DEFAULT ''")
        }
    }

    // v2 → v3: tambah stok_minimum; jadikan kondisi_barang nullable (tabel direkreasi).
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE master_katalog ADD COLUMN stok_minimum INTEGER NOT NULL DEFAULT 0")

            // Rekreasi log_mutasi dengan kondisi_barang nullable
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

    // v3 → v4: tambah id_proyek/nama_proyek pada user_session & id_proyek pada master_katalog.
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_session ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE user_session ADD COLUMN nama_proyek TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE master_katalog ADD COLUMN id_proyek TEXT NOT NULL DEFAULT ''")

            // Backfill data lama ke proyek default.
            db.execSQL("UPDATE master_katalog SET id_proyek = 'PROYEK-UTAMA' WHERE id_proyek = ''")
            db.execSQL("UPDATE user_session SET id_proyek = 'PROYEK-UTAMA' WHERE id_proyek = ''")
        }
    }

    // v4 → v5: tambah tabel proyek (registry read-only).
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS proyek (" +
                    "id_proyek TEXT NOT NULL, " +
                    "nama_proyek TEXT NOT NULL, " +
                    "PRIMARY KEY(id_proyek))"
            )
        }
    }

    // v5 → v6: transfer_proyek simpan snapshot nama_barang/kategori/satuan.
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transfer_proyek ADD COLUMN nama_barang TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transfer_proyek ADD COLUMN kategori TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transfer_proyek ADD COLUMN satuan TEXT NOT NULL DEFAULT ''")
        }
    }

    // v6 → v7: ganti kolom email → username (tabel direkreasi, username disalin dari email lama).
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE user_session_new (
                    id_mandor TEXT NOT NULL,
                    nama TEXT NOT NULL,
                    username TEXT NOT NULL,
                    role TEXT NOT NULL,
                    auth_token TEXT NOT NULL,
                    last_sync_timestamp INTEGER NOT NULL,
                    id_proyek TEXT NOT NULL DEFAULT '',
                    nama_proyek TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY(id_mandor)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO user_session_new (
                    id_mandor, nama, username, role, auth_token, last_sync_timestamp, id_proyek, nama_proyek
                )
                SELECT id_mandor, nama,
                    CASE WHEN instr(email, '@') > 0 THEN substr(email, 1, instr(email, '@') - 1) ELSE email END,
                    role, auth_token, last_sync_timestamp, id_proyek, nama_proyek
                FROM user_session
                """.trimIndent()
            )
            db.execSQL("DROP TABLE user_session")
            db.execSQL("ALTER TABLE user_session_new RENAME TO user_session")
        }
    }

    // v7 → v8: kursor delta per koleksi di user_session (mutasi & transfer, seed 0 agar riwayat
    // lama ikut ditarik pada sync pertama) + hapus FK transfer_proyek → master_katalog supaya
    // transfer bisa ditarik di perangkat penerima yang tak punya katalog sku asal.
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_session ADD COLUMN last_sync_mutasi INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_session ADD COLUMN last_sync_transfer INTEGER NOT NULL DEFAULT 0")

            // Rekreasi transfer_proyek tanpa FOREIGN KEY (SQLite tak bisa drop FK via ALTER).
            db.execSQL(
                """
                CREATE TABLE transfer_proyek_new (
                    id_transfer TEXT NOT NULL,
                    sku_id TEXT NOT NULL,
                    nama_barang TEXT NOT NULL DEFAULT '',
                    kategori TEXT NOT NULL DEFAULT '',
                    satuan TEXT NOT NULL DEFAULT '',
                    proyek_asal TEXT NOT NULL,
                    proyek_tujuan TEXT NOT NULL,
                    jumlah INTEGER NOT NULL,
                    status_transfer TEXT NOT NULL,
                    local_timestamp INTEGER NOT NULL,
                    sync_status TEXT NOT NULL,
                    PRIMARY KEY(id_transfer)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transfer_proyek_new (
                    id_transfer, sku_id, nama_barang, kategori, satuan,
                    proyek_asal, proyek_tujuan, jumlah, status_transfer, local_timestamp, sync_status
                )
                SELECT id_transfer, sku_id, nama_barang, kategori, satuan,
                    proyek_asal, proyek_tujuan, jumlah, status_transfer, local_timestamp, sync_status
                FROM transfer_proyek
                """.trimIndent()
            )
            db.execSQL("DROP TABLE transfer_proyek")
            db.execSQL("ALTER TABLE transfer_proyek_new RENAME TO transfer_proyek")
        }
    }

    // v8 → v9: verifier kata sandi login luring (C1) — kolom pw_hash & pw_salt di user_session.
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_session ADD COLUMN pw_hash TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE user_session ADD COLUMN pw_salt TEXT NOT NULL DEFAULT ''")
        }
    }

    // v9 → v10: tambah FK id_proyek pada master_katalog & log_mutasi ke tabel proyek (RESTRICT),
    // supaya katalog/riwayat mutasi tidak bisa "yatim" dari proyek pemiliknya. Backfill dulu
    // proyek yang belum tercatat lokal (id_proyek lama/kosong) sebelum FK ditegakkan, supaya
    // instalasi lama yang datanya belum sinkron penuh tidak gagal migrasi.
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Backfill placeholder proyek untuk id_proyek yatim (dari data lama sebelum FK ada).
            db.execSQL(
                """
                INSERT OR IGNORE INTO proyek (id_proyek, nama_proyek)
                SELECT DISTINCT id_proyek, 'Proyek Tidak Dikenal (data lama)' FROM master_katalog
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO proyek (id_proyek, nama_proyek)
                SELECT DISTINCT id_proyek, 'Proyek Tidak Dikenal (data lama)' FROM log_mutasi
                """.trimIndent()
            )

            // Rekreasi master_katalog dengan FK id_proyek → proyek.
            db.execSQL(
                """
                CREATE TABLE master_katalog_new (
                    sku_id TEXT NOT NULL,
                    kategori TEXT NOT NULL,
                    nama_barang TEXT NOT NULL,
                    satuan TEXT NOT NULL,
                    stok_lokal INTEGER NOT NULL,
                    stok_minimum INTEGER NOT NULL DEFAULT 0,
                    id_proyek TEXT NOT NULL DEFAULT '',
                    sync_status TEXT NOT NULL,
                    local_timestamp INTEGER NOT NULL,
                    PRIMARY KEY(sku_id),
                    FOREIGN KEY(id_proyek) REFERENCES proyek(id_proyek) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO master_katalog_new (
                    sku_id, kategori, nama_barang, satuan, stok_lokal, stok_minimum,
                    id_proyek, sync_status, local_timestamp
                )
                SELECT sku_id, kategori, nama_barang, satuan, stok_lokal, stok_minimum,
                    id_proyek, sync_status, local_timestamp
                FROM master_katalog
                """.trimIndent()
            )
            db.execSQL("DROP TABLE master_katalog")
            db.execSQL("ALTER TABLE master_katalog_new RENAME TO master_katalog")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_master_katalog_id_proyek ON master_katalog(id_proyek)")

            // Rekreasi log_mutasi: pertahankan FK sku_id, tambah FK id_proyek → proyek.
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
                    FOREIGN KEY(sku_id) REFERENCES master_katalog(sku_id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(id_proyek) REFERENCES proyek(id_proyek) ON UPDATE NO ACTION ON DELETE RESTRICT
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
            db.execSQL("CREATE INDEX IF NOT EXISTS index_log_mutasi_id_proyek ON log_mutasi(id_proyek)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KonstruksiDatabase =
        Room.databaseBuilder(context, KonstruksiDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .build()

    @Provides fun provideUserSessionDao(db: KonstruksiDatabase): UserSessionDao = db.userSessionDao()
    @Provides fun provideMasterKatalogDao(db: KonstruksiDatabase): MasterKatalogDao = db.masterKatalogDao()
    @Provides fun provideLogMutasiDao(db: KonstruksiDatabase): LogMutasiDao = db.logMutasiDao()
    @Provides fun provideTransferProyekDao(db: KonstruksiDatabase): TransferProyekDao = db.transferProyekDao()
    @Provides fun provideProyekDao(db: KonstruksiDatabase): ProyekDao = db.proyekDao()
}
