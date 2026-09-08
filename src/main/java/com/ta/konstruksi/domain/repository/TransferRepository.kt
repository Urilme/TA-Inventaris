package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.util.StatusTransfer
import kotlinx.coroutines.flow.Flow

/** Hasil pembuatan transfer atomik (idempoten by-id + guard stok). */
enum class HasilBuatTransfer { SUKSES, DUPLIKAT, STOK_KURANG }

interface TransferRepository {
    fun getAllTransfer(): Flow<List<TransferProyek>>

    /**
     * Riwayat transfer yang MELIBATKAN proyek aktif (sebagai asal ATAU tujuan), terbaru dulu,
     * dibatasi [limit] (paginasi Riwayat).
     */
    fun getRiwayatTransfer(limit: Int): Flow<List<TransferProyek>>

    /**
     * Buat transfer secara ATOMIK & idempoten: tolak bila id_transfer sudah ada (ketukan ganda),
     * kurangi stok proyek asal bila mencukupi, lalu simpan — semuanya dalam satu transaksi.
     * Karena id dibangkitkan sekali saat dialog konfirmasi dibuka, ketukan kedua membawa id yang
     * sama sehingga [HasilBuatTransfer.DUPLIKAT] dikembalikan tanpa mengurangi stok dua kali.
     */
    suspend fun createTransfer(transfer: TransferProyek): HasilBuatTransfer
    suspend fun updateStatusTransfer(idTransfer: String, status: StatusTransfer)

    /**
     * Konfirmasi penerimaan secara atomik (guard status + tambah stok + set SELESAI dalam satu
     * transaksi). @return false bila transfer sudah bukan IN_TRANSIT (ditolak).
     */
    suspend fun confirmReceiveAtomic(idTransfer: String, idProyekPenerima: String): Boolean

    /** Pembatalan atomik (guard status + kembalikan stok + set DIBATALKAN). @return false bila ditolak. */
    suspend fun cancelAtomic(idTransfer: String): Boolean

    suspend fun getPendingTransfer(): List<TransferProyek>
    suspend fun markSynced(idTransfer: String)

    /** Transfer IN_TRANSIT yang menuju [namaProyek] — dipakai untuk notifikasi "aset masuk". */
    suspend fun getIncomingInTransit(namaProyek: String): List<TransferProyek>
}
