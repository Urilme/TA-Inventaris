package com.ta.konstruksi.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.SyncRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.sync.TransferNotifier
import com.ta.konstruksi.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val triggerSyncUseCase: TriggerSyncUseCase,
    private val authRepository: AuthRepository,
    private val transferRepository: TransferRepository,
    private val transferNotifier: TransferNotifier,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            triggerSyncUseCase().fold(
                onSuccess = {
                    notifyIncomingTransfer()   // best-effort; tak memengaruhi hasil sync
                    // B1: bila antrean PENDING melebihi satu batch, lanjutkan siklus berikutnya
                    // (enqueue baru — mereset runAttemptCount, tak terpotong batas 3x retry).
                    if (syncRepository.hasPendingSync()) syncScheduler.requestSync()
                    Result.success()
                },
                onFailure = {
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Setelah pull selesai, cek transfer IN_TRANSIT baru menuju proyek aktif dan beri notifikasi
     * lokal (anti-duplikat di [TransferNotifier]). Best-effort: kegagalannya tidak menggagalkan sync.
     */
    private suspend fun notifyIncomingTransfer() {
        runCatching {
            val nama = authRepository.getLocalSession()?.nama_proyek
            if (!nama.isNullOrBlank()) {
                transferNotifier.notifyIncoming(transferRepository.getIncomingInTransit(nama))
            }
        }
    }

    companion object {
        fun buildPeriodicRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(Constants.SYNC_WORK_TAG)
                .build()
        }

        fun buildOneTimeRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(Constants.SYNC_WORK_TAG)
                .build()
        }
    }
}
