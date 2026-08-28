package com.ta.konstruksi.domain.usecase.sync

import com.ta.konstruksi.domain.repository.SyncRepository
import javax.inject.Inject

class TriggerSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        // Fase PUSH dulu (master data → transaksi), lalu fase PULL. Katalog di-pull sebelum mutasi
        // agar barang induk tersedia untuk FK log_mutasi. Tiap koleksi punya kursor delta sendiri
        // (last_sync_timestamp/last_sync_mutasi/last_sync_transfer) sehingga tidak saling starve.
        val katalogResult = syncRepository.syncPendingKatalog()
        val mutasiResult = syncRepository.syncPendingMutasi()
        val transferResult = syncRepository.syncPendingTransfer()

        val pullKatalog = syncRepository.pullLatestKatalog()
        val pullMutasi = syncRepository.pullLatestMutasi()
        val pullTransfer = syncRepository.pullLatestTransfer()

        // Refresh registry proyek bersifat best-effort (data pelengkap read-only). Kegagalannya
        // (mis. offline / rules belum di-deploy) TIDAK menggagalkan sinkronisasi data inti.
        syncRepository.pullLatestProyek()

        return if (katalogResult.isSuccess && mutasiResult.isSuccess &&
            transferResult.isSuccess && pullKatalog.isSuccess &&
            pullMutasi.isSuccess && pullTransfer.isSuccess
        ) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Sync sebagian gagal"))
        }
    }
}
