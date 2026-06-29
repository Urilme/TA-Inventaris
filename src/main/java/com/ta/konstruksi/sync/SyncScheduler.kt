package com.ta.konstruksi.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.ta.konstruksi.sync.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menjadwalkan sinkronisasi otomatis setiap kali ada data baru ditulis.
 *
 * Pendekatan offline-first: kita TIDAK mengecek konektivitas manual. Pekerjaan
 * di-enqueue dengan constraint [NetworkType.CONNECTED] (lihat [SyncWorker]),
 * sehingga WorkManager menjalankannya segera bila ada internet, atau menahannya
 * sampai koneksi tersedia bila sedang luring. Tahan terhadap proses ditutup.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun requestSync() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            SyncWorker.buildOneTimeRequest()
        )
    }

    companion object {
        private const val UNIQUE_SYNC_WORK = "auto_sync_on_write"
    }
}
