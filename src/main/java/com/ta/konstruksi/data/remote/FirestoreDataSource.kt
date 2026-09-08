package com.ta.konstruksi.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.ProyekEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.util.Constants
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * Data source Firestore — operasi baca/tulis cloud, dipakai saat sinkronisasi.
 * Tiap dokumen menyertakan server_timestamp dan updated_by.
 */
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** Hasil pull: entitas terbaru + server_timestamp terbesar untuk kursor delta. */
    data class PullResult(
        val items: List<MasterKatalogEntity>,
        val latestServerMillis: Long
    )

    data class MutasiPullResult(
        val items: List<LogMutasiEntity>,
        val latestServerMillis: Long
    )

    data class TransferPullResult(
        val items: List<TransferProyekEntity>,
        val latestServerMillis: Long
    )

    /** Profil mandor dari dokumen users/{uid}. */
    data class UserProfile(
        val nama: String?,
        val role: String?,
        val idProyek: String?,
        val namaProyek: String?
    )

    /** Ambil profil mandor dari users/{uid}; null bila dokumen belum ada. */
    suspend fun fetchUserProfile(uid: String): UserProfile? {
        val doc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(
            nama = doc.getString("nama"),
            role = doc.getString("role"),
            idProyek = doc.getString("id_proyek"),
            namaProyek = doc.getString("nama_proyek")
        )
    }

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
     * Ambil katalog yang berubah setelah [sinceServerMillis] (delta sync), DISARING DI SERVER ke
     * [idProyek] via whereEqualTo — hemat kuota & biaya baca (tak mengunduh katalog proyek lain).
     * Bila [idProyek] kosong, tarik seluruh katalog. Kombinasi whereEqualTo + whereGreaterThan
     * memerlukan composite index (lihat firestore.indexes.json).
     */
    suspend fun pullKatalog(sinceServerMillis: Long, idProyek: String): PullResult {
        var query: Query = firestore.collection(Constants.COLLECTION_KATALOG)
        if (idProyek.isNotBlank()) query = query.whereEqualTo("id_proyek", idProyek)
        if (sinceServerMillis > 0L) {
            query = query.whereGreaterThan("server_timestamp", Timestamp(Date(sinceServerMillis)))
        }
        val snapshot = query.get().await()

        var latest = sinceServerMillis
        val items = mutableListOf<MasterKatalogEntity>()
        snapshot.documents.forEach { doc ->
            val serverMillis = doc.getTimestamp("server_timestamp")?.toDate()?.time ?: 0L
            if (serverMillis > latest) latest = serverMillis
            doc.toKatalogEntity()?.let { items.add(it) }
        }
        return PullResult(items, latest)
    }

    /**
     * Ambil mutasi yang berubah setelah [sinceServerMillis] (delta sync), DISARING DI SERVER ke
     * [idProyek] milik mandor via whereEqualTo. Bila kosong, tarik seluruh riwayat. Kombinasi
     * whereEqualTo + whereGreaterThan memerlukan composite index (firestore.indexes.json).
     */
    suspend fun pullMutasi(sinceServerMillis: Long, idProyek: String): MutasiPullResult {
        var query: Query = firestore.collection(Constants.COLLECTION_MUTASI)
        if (idProyek.isNotBlank()) query = query.whereEqualTo("id_proyek", idProyek)
        if (sinceServerMillis > 0L) {
            query = query.whereGreaterThan("server_timestamp", Timestamp(Date(sinceServerMillis)))
        }
        val snapshot = query.get().await()

        var latest = sinceServerMillis
        val items = mutableListOf<LogMutasiEntity>()
        snapshot.documents.forEach { doc ->
            val serverMillis = doc.getTimestamp("server_timestamp")?.toDate()?.time ?: 0L
            if (serverMillis > latest) latest = serverMillis
            doc.toMutasiEntity()?.let { items.add(it) }
        }
        return MutasiPullResult(items, latest)
    }

    /**
     * Ambil transfer yang relevan bagi [namaProyekAktif] — sebagai tujuan (agar penerima melihat
     * transfer masuk) ATAU asal (agar pengirim melihat perubahan status SELESAI/DIBATALKAN).
     * Firestore tak mendukung OR lintas field dalam satu kueri, jadi dipakai DUA kueri whereEqualTo
     * TERSARING DI SERVER (proyek_tujuan, lalu proyek_asal) yang digabung & didedup di klien. Dokumen
     * menyimpan NAMA proyek, jadi filter memakai nama. Tiap kueri (whereEqualTo + whereGreaterThan)
     * memerlukan composite index (firestore.indexes.json).
     */
    suspend fun pullTransfer(sinceServerMillis: Long, namaProyekAktif: String): TransferPullResult {
        var latest = sinceServerMillis
        val hasil = LinkedHashMap<String, TransferProyekEntity>()   // dedup by id_transfer

        fun serap(docs: List<DocumentSnapshot>) {
            docs.forEach { doc ->
                val serverMillis = doc.getTimestamp("server_timestamp")?.toDate()?.time ?: 0L
                if (serverMillis > latest) latest = serverMillis
                doc.toTransferEntity()?.let { hasil[it.id_transfer] = it }
            }
        }

        if (namaProyekAktif.isBlank()) {
            // Tanpa acuan proyek (mis. sesi belum siap) — tarik delta apa adanya.
            var q: Query = firestore.collection(Constants.COLLECTION_TRANSFER)
            if (sinceServerMillis > 0L) q = q.whereGreaterThan("server_timestamp", Timestamp(Date(sinceServerMillis)))
            serap(q.get().await().documents)
        } else {
            for (field in listOf("proyek_tujuan", "proyek_asal")) {
                var q: Query = firestore.collection(Constants.COLLECTION_TRANSFER)
                    .whereEqualTo(field, namaProyekAktif)
                if (sinceServerMillis > 0L) q = q.whereGreaterThan("server_timestamp", Timestamp(Date(sinceServerMillis)))
                serap(q.get().await().documents)
            }
        }
        return TransferPullResult(hasil.values.toList(), latest)
    }

    /** Ambil seluruh registry proyek (kecil, read-only) — ditarik penuh tiap sync. */
    suspend fun pullProyek(): List<ProyekEntity> {
        val snapshot = firestore.collection(Constants.COLLECTION_PROYEK).get().await()
        return snapshot.documents.mapNotNull { it.toProyekEntity() }
    }

    private fun DocumentSnapshot.toProyekEntity(): ProyekEntity? {
        val nama = getString("nama_proyek") ?: return null
        // Fallback ke id dokumen bila field id_proyek kosong.
        val idProyek = getString("id_proyek") ?: id
        return ProyekEntity(id_proyek = idProyek, nama_proyek = nama)
    }

    private fun MasterKatalogEntity.toMap(updatedBy: String): Map<String, Any?> = mapOf(
        "sku_id" to sku_id,
        "kategori" to kategori,
        "nama_barang" to nama_barang,
        "satuan" to satuan,
        "stok_lokal" to stok_lokal,
        "stok_minimum" to stok_minimum,
        "id_proyek" to id_proyek,
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
        "nama_barang" to nama_barang,
        "kategori" to kategori,
        "satuan" to satuan,
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
            id_proyek = getString("id_proyek") ?: "",
            sync_status = "SYNCED",
            local_timestamp = getLong("local_timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toMutasiEntity(): LogMutasiEntity? {
        val id = getString("id_transaksi") ?: return null
        val sku = getString("sku_id") ?: return null
        return LogMutasiEntity(
            id_transaksi = id,
            sku_id = sku,
            id_mandor = getString("id_mandor") ?: "",
            id_proyek = getString("id_proyek") ?: "",
            jenis_mutasi = getString("jenis_mutasi") ?: "",
            jumlah = (getLong("jumlah") ?: 0L).toInt(),
            kondisi_barang = getString("kondisi_barang"),
            lokasi_penggunaan = getString("lokasi_penggunaan") ?: "",
            penanggungjawab = getString("penanggungjawab") ?: "",
            catatan = getString("catatan"),
            local_timestamp = getLong("local_timestamp") ?: System.currentTimeMillis(),
            sync_status = "SYNCED"
        )
    }

    private fun DocumentSnapshot.toTransferEntity(): TransferProyekEntity? {
        val id = getString("id_transfer") ?: return null
        val sku = getString("sku_id") ?: return null
        return TransferProyekEntity(
            id_transfer = id,
            sku_id = sku,
            nama_barang = getString("nama_barang") ?: "",
            kategori = getString("kategori") ?: "",
            satuan = getString("satuan") ?: "",
            proyek_asal = getString("proyek_asal") ?: "",
            proyek_tujuan = getString("proyek_tujuan") ?: "",
            jumlah = (getLong("jumlah") ?: 0L).toInt(),
            status_transfer = getString("status_transfer") ?: "IN_TRANSIT",
            local_timestamp = getLong("local_timestamp") ?: System.currentTimeMillis(),
            sync_status = "SYNCED"
        )
    }
}
