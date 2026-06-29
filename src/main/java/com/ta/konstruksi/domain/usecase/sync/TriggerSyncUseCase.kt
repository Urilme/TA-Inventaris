package com.ta.konstruksi.domain.usecase.sync

import com.ta.konstruksi.domain.repository.SyncRepository
import javax.inject.Inject

class TriggerSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        // Push katalog dulu (master data), baru mutasi & transfer, lalu pull katalog terbaru
        val katalogResult = syncRepository.syncPendingKatalog()
        val mutasiResult = syncRepository.syncPendingMutasi()
        val transferResult = syncRepository.syncPendingTransfer()
        val pullResult = syncRepository.pullLatestKatalog()

        return if (katalogResult.isSuccess && mutasiResult.isSuccess &&
            transferResult.isSuccess && pullResult.isSuccess
        ) {
            syncRepository.updateLastSyncTimestamp(System.currentTimeMillis())
            Result.success(Unit)
        } else {
            Result.failure(Exception("Sync sebagian gagal"))
        }
    }
}
