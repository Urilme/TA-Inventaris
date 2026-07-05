package com.ta.konstruksi.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.util.Constants
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * Firestore data source — menangani seluruh operasi baca/tulis cloud.
 * Hanya dipakai saat sinkronisasi; TIDAK dipanggil pada operasi luring normal.
 *
 * Setiap dokumen menyertakan `server_timestamp` (FieldValue.serverTimestamp) dan `updated_by`
 * (id_mandor pengunggah) sesuai skema. `server_timestamp` juga menjadi kursor delta sync
 * (bebas dari perbedaan jam antar-perangkat), sedangkan `local_timestamp` dipakai untuk LWW.
 */
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** Hasil pull: entitas terbaru + server_timestamp terbesar (epoch millis) untuk kursor delta. */
    data class PullResult(
        val items: List<MasterKatalogEntity>,
        val latestServerMillis: Long
    )

    suspend fun uploadKatalog(entity: MasterKatalogEntity, updatedBy: String) {
        firestore.collection(Constants.COLLECTION_KATALOG)
            .document(entity.sku_id)
            .set(entity.toMap(updatedBy))
            .await()
    }

    suspend fun uploadMutasi(entity: LogMutasiEntity, updatedBy: String) {
        firestore.collection(Constants.COLLECTION_MUTASI)
            .document(entity.id_transaksi)
            .set(entity.toMap(updatedBy))
            .await()
    }

    suspend fun uploadTransfer(entity: TransferProyekEntity, updatedBy: String) {
        firestore.collection(Constants.COLLECTION_TRANSFER)
            .document(entity.id_transfer)
            .set(entity.toMap(updatedBy))
            .await()
    }

    /**
     * Ambil katalog yang diperbarui di server setelah [sinceServerMillis] (delta sync).
     * Bila 0, tarik seluruh katalog (sinkron awal).
     */
    suspend fun pullKatalog(sinceServerMillis: Long): PullResult {
        val collection = firestore.collection(Constants.COLLECTION_KATALOG)
        val query = if (sinceServerMillis > 0L) {
            collection.whereGreaterThan("server_timestamp", Timestamp(Date(sinceServerMillis)))
        } else {
            collection
        }
        val snapshot = query.get().await()

        var latest = sinceServerMillis
        val items = snapshot.documents.mapNotNull { doc ->
            val entity = doc.toKatalogEntity() ?: return@mapNotNull null
            val serverMillis = doc.getTimestamp("server_timestamp")?.toDate()?.time ?: 0L
            if (serverMillis > latest) latest = serverMillis
            entity
        }
        return PullResult(items, latest)
    }

    private fun MasterKatalogEntity.toMap(updatedBy: String): Map<String, Any?> = mapOf(
        "sku_id" to sku_id,
        "kategori" to kategori,
        "nama_barang" to nama_barang,
        "satuan" to satuan,
        "stok_lokal" to stok_lokal,
        "stok_minimum" to stok_minimum,
        "local_timestamp" to local_timestamp,
        "sync_status" to "SYNCED",
        "server_timestamp" to FieldValue.serverTimestamp(),
        "updated_by" to updatedBy
    )

    private fun LogMutasiEntity.toMap(updatedBy: String): Map<String, Any?> = mapOf(
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
        "sync_status" to "SYNCED",
        "server_timestamp" to FieldValue.serverTimestamp(),
        "updated_by" to updatedBy
    )

    private fun TransferProyekEntity.toMap(updatedBy: String): Map<String, Any?> = mapOf(
        "id_transfer" to id_transfer,
        "sku_id" to sku_id,
        "proyek_asal" to proyek_asal,
        "proyek_tujuan" to proyek_tujuan,
        "jumlah" to jumlah,
        "status_transfer" to status_transfer,
        "local_timestamp" to local_timestamp,
        "sync_status" to "SYNCED",
        "server_timestamp" to FieldValue.serverTimestamp(),
        "updated_by" to updatedBy
    )

    private fun DocumentSnapshot.toKatalogEntity(): MasterKatalogEntity? {
        val sku = getString("sku_id") ?: return null
        return MasterKatalogEntity(
            sku_id = sku,
            kategori = getString("kategori") ?: "",
            nama_barang = getString("nama_barang") ?: "",
            satuan = getString("satuan") ?: "",
            stok_lokal = (getLong("stok_lokal") ?: 0L).toInt(),
            stok_minimum = (getLong("stok_minimum") ?: 0L).toInt(),
            sync_status = "SYNCED",
            local_timestamp = getLong("local_timestamp") ?: System.currentTimeMillis()
        )
    }
}
