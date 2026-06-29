package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.entity.LogMutasiEntity
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity
import com.ta.konstruksi.data.local.entity.TransferProyekEntity
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.SyncStatus

// Konversi String <-> enum yang aman (fallback bila nilai tak dikenal)
internal fun parseSyncStatus(value: String): SyncStatus =
    runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.PENDING)

internal fun parseJenisMutasi(value: String): JenisMutasi =
    runCatching { JenisMutasi.valueOf(value) }.getOrDefault(JenisMutasi.CHECK_IN)

internal fun parseStatusTransfer(value: String): StatusTransfer =
    runCatching { StatusTransfer.valueOf(value) }.getOrDefault(StatusTransfer.IN_TRANSIT)

// --- MasterKatalog ---
internal fun MasterKatalogEntity.toDomain() = MasterKatalog(
    sku_id = sku_id,
    kategori = kategori,
    nama_barang = nama_barang,
    satuan = satuan,
    stok_lokal = stok_lokal,
    sync_status = parseSyncStatus(sync_status),
    local_timestamp = local_timestamp
)

internal fun MasterKatalog.toEntity() = MasterKatalogEntity(
    sku_id = sku_id,
    kategori = kategori,
    nama_barang = nama_barang,
    satuan = satuan,
    stok_lokal = stok_lokal,
    sync_status = sync_status.name,
    local_timestamp = local_timestamp
)

// --- LogMutasi ---
internal fun LogMutasiEntity.toDomain() = LogMutasi(
    id_transaksi = id_transaksi,
    sku_id = sku_id,
    jenis_mutasi = parseJenisMutasi(jenis_mutasi),
    jumlah = jumlah,
    catatan = catatan,
    local_timestamp = local_timestamp,
    sync_status = parseSyncStatus(sync_status)
)

internal fun LogMutasi.toEntity() = LogMutasiEntity(
    id_transaksi = id_transaksi,
    sku_id = sku_id,
    jenis_mutasi = jenis_mutasi.name,
    jumlah = jumlah,
    catatan = catatan,
    local_timestamp = local_timestamp,
    sync_status = sync_status.name
)

// --- TransferProyek ---
internal fun TransferProyekEntity.toDomain() = TransferProyek(
    id_transfer = id_transfer,
    sku_id = sku_id,
    proyek_asal = proyek_asal,
    proyek_tujuan = proyek_tujuan,
    jumlah = jumlah,
    status_transfer = parseStatusTransfer(status_transfer),
    local_timestamp = local_timestamp,
    sync_status = parseSyncStatus(sync_status)
)

internal fun TransferProyek.toEntity() = TransferProyekEntity(
    id_transfer = id_transfer,
    sku_id = sku_id,
    proyek_asal = proyek_asal,
    proyek_tujuan = proyek_tujuan,
    jumlah = jumlah,
    status_transfer = status_transfer.name,
    local_timestamp = local_timestamp,
    sync_status = sync_status.name
)
