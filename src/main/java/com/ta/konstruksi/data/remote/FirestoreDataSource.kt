package com.ta.konstruksi.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore data source — menangani seluruh operasi baca/tulis cloud.
 * Hanya dipakai saat sinkronisasi; TIDAK dipanggil pada operasi luring normal.
 */
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun uploadKatalog(entity: MasterKatalogEntity) {
        firestore.collection(Constants.COLLECTION_KATALOG)
            .document(entity.sku_id)
            .set(entity.toMap())
            .await()
    }

    suspend fun uploadMutasi(entity: LogMutasiEntity) {
        firestore.collection(Constants.COLLECTION_MUTASI)
            .document(entity.id_transaksi)
            .set(entity.toMap())
            .await()
    }

    suspend fun uploadTransfer(entity: TransferProyekEntity) {
        firestore.collection(Constants.COLLECTION_TRANSFER)
            .document(entity.id_transfer)
            .set(entity.toMap())
            .await()
    }

    /** Ambil katalog yang diperbarui di server sejak [sinceTimestamp]. */
    suspend fun pullKatalog(sinceTimestamp: Long): List<MasterKatalogEntity> {
        val snapshot = firestore.collection(Constants.COLLECTION_KATALOG)
            .whereGreaterThan("local_timestamp", sinceTimestamp)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toKatalogEntity() }
    }

    private fun MasterKatalogEntity.toMap(): Map<String, Any?> = mapOf(
        "sku_id" to sku_id,
        "kategori" to kategori,
        "nama_barang" to nama_barang,
        "satuan" to satuan,
        "stok_lokal" to stok_lokal,
        "local_timestamp" to local_timestamp,
        "sync_status" to "SYNCED"
    )

    private fun LogMutasiEntity.toMap(): Map<String, Any?> = mapOf(
        "id_transaksi" to id_transaksi,
        "sku_id" to sku_id,
        "id_mandor" to id_mandor,
        "id_proyek" to id_proyek,
        "jenis_mutasi" to jenis_mutasi,
        "jumlah" to jumlah,
        "kondisi_barang" to kondisi_barang,
        "lokasi_penggunaan" to lokasi_penggunaan,
        "penanggungjawab" to penanggungjawab,
        "catatan" to catatan,
        "local_timestamp" to local_timestamp,
        "sync_status" to "SYNCED"
    )

    private fun TransferProyekEntity.toMap(): Map<String, Any?> = mapOf(
        "id_transfer" to id_transfer,
        "sku_id" to sku_id,
        "proyek_asal" to proyek_asal,
        "proyek_tujuan" to proyek_tujuan,
        "jumlah" to jumlah,
        "status_transfer" to status_transfer,
        "local_timestamp" to local_timestamp,
        "sync_status" to "SYNCED"
    )

    private fun DocumentSnapshot.toKatalogEntity(): MasterKatalogEntity? {
        val sku = getString("sku_id") ?: return null
        return MasterKatalogEntity(
            sku_id = sku,
            kategori = getString("kategori") ?: "",
            nama_barang = getString("nama_barang") ?: "",
            satuan = getString("satuan") ?: "",
            stok_lokal = (getLong("stok_lokal") ?: 0L).toInt(),
            sync_status = "SYNCED",
            local_timestamp = getLong("local_timestamp") ?: System.currentTimeMillis()
        )
    }
}
