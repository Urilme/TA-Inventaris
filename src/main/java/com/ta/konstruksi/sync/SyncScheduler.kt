package com.ta.konstruksi.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ta.konstruksi.sync.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menjadwalkan sinkronisasi otomatis setiap kali ada data baru ditulis.
 *
 * Pendekatan offline-first: kita TIDAK mengecek konektivitas manual. Pekerjaan
 * di-enqueue dengan constraint [NetworkType.CONNECTED] (lihat [SyncWorker]),
 * sehingga WorkManager menjalankannya segera bila ada internet, atau menahannya
 * sampai koneksi tersedia bila sedang luring. Tahan terhadap proses ditutup.
 *
 * Selain pemicu otomatis (tulis lokal, kembali ke latar depan, siklus periodik),
 * layar Katalog dan Dashboard juga dapat memicu [requestSync] lewat gestur
 * tarik-untuk-refresh; [observeSyncing] dipakai UI untuk menampilkan indikator
 * berputar selama pekerjaan itu masih ENQUEUED/RUNNING.
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

    fun observeSyncing(): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_SYNC_WORK)
            .map { infos -> infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING } }

    companion object {
        private const val UNIQUE_SYNC_WORK = "auto_sync_on_write"
    }
}
