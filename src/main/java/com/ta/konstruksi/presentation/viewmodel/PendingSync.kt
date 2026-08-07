package com.ta.konstruksi.presentation.viewmodel

import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.SyncStatus

/**
 * Susun daftar entitas yang masih menunggu sinkron sebagai *aksi pengguna*, bukan baris tabel mentah.
 *
 * Setiap mutasi (dan transfer) selalu ikut menandai baris katalog terkait menjadi PENDING karena
 * `stok_lokal` berubah. Bila keduanya dihitung terpisah, satu aksi tampak seperti dua item yang
 * menunggu — inilah yang membuat Dashboard ambigu saat offline. Karena itu baris katalog PENDING
 * hanya ditampilkan bila BUKAN sekadar efek samping dari mutasi/transfer PENDING pada SKU yang sama
 * (mis. penambahan barang baru atau penyuntingan katalog murni).
 *
 * Catatan: ini murni perbaikan tampilan/penghitungan. Mekanisme sinkron tetap mendorong baris
 * katalog PENDING ke Firestore seperti biasa (stok server wajib ikut ter-update).
 */
fun buildPendingSyncItems(
    katalogList: List<MasterKatalog>,
    mutasiList: List<LogMutasi>,
    transferList: List<TransferProyek>
): List<PendingSyncItem> {
    val namaBySku = katalogList.associate { it.sku_id to it.nama_barang }
    // SKU yang perubahan stoknya sudah terwakili oleh baris mutasi/transfer PENDING.
    val skuTerwakili = buildSet {
        mutasiList.filter { it.sync_status == SyncStatus.PENDING }.forEach { add(it.sku_id) }
        transferList.filter { it.sync_status == SyncStatus.PENDING }.forEach { add(it.sku_id) }
    }
    return buildList {
        katalogList
            .filter { it.sync_status == SyncStatus.PENDING && it.sku_id !in skuTerwakili }
            .forEach { add(PendingSyncItem(it.nama_barang, "Perubahan katalog", it.local_timestamp)) }
        mutasiList.filter { it.sync_status == SyncStatus.PENDING }.forEach {
            val nama = namaBySku[it.sku_id] ?: it.sku_id
            val detail = if (it.jenis_mutasi == JenisMutasi.CHECK_IN) "Masuk +${it.jumlah}" else "Keluar -${it.jumlah}"
            add(PendingSyncItem(nama, detail, it.local_timestamp))
        }
        transferList.filter { it.sync_status == SyncStatus.PENDING }.forEach {
            val nama = namaBySku[it.sku_id] ?: it.sku_id
            add(PendingSyncItem(nama, "Transfer ${it.jumlah} → ${it.proyek_tujuan}", it.local_timestamp))
        }
    }.sortedByDescending { it.timestamp }
}
