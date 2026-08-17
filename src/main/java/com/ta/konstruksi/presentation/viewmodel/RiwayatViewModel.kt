package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class RiwayatItem {
    data class Mutasi(val data: LogMutasi, val namaBarang: String = "", val kategori: String = "") : RiwayatItem()
    data class Transfer(val data: TransferProyek, val namaBarang: String = "") : RiwayatItem()

    val timestamp: Long get() = when (this) {
        is Mutasi -> data.local_timestamp
        is Transfer -> data.local_timestamp
    }
    val syncStatus: SyncStatus get() = when (this) {
        is Mutasi -> data.sync_status
        is Transfer -> data.sync_status
    }
    val label: String get() = when (this) {
        is Mutasi -> namaBarang.ifBlank { "Barang tidak dikenal" }
        is Transfer -> namaBarang.ifBlank { "Barang tidak dikenal" }
    }
}

@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val mutasiRepository: MutasiRepository,
    private val transferRepository: TransferRepository,
    private val katalogRepository: KatalogRepository
) : ViewModel() {

    val filterState = MutableStateFlow<SyncStatus?>(null)

    // Menandai emisi pertama agar bisa membedakan "sedang memuat" vs "riwayat kosong"
    private val _initialLoaded = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _initialLoaded
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val riwayatList: StateFlow<List<RiwayatItem>> = combine(
        mutasiRepository.getAllMutasi(),
        transferRepository.getAllTransfer(),
        katalogRepository.getAllKatalog(),
        filterState
    ) { mutasiList, transferList, katalogList, filter ->
        // Peta SKU -> nama barang agar riwayat menampilkan nama, bukan UUID
        val namaBySku = katalogList.associate { it.sku_id to it.nama_barang }
        // Peta SKU -> kategori: dipakai mengisi badge kategori pada kartu mutasi bahan/scaffolding
        // (yang kondisi_barang-nya null) agar tidak menyisakan ruang kosong seperti kartu transfer.
        val kategoriBySku = katalogList.associate { it.sku_id to it.kategori }
        val items = mutableListOf<RiwayatItem>()
        mutasiList.forEach {
            items.add(RiwayatItem.Mutasi(it, namaBySku[it.sku_id].orEmpty(), kategoriBySku[it.sku_id].orEmpty()))
        }
        // Transfer bersifat self-describing: bila SKU asal tidak ada di katalog proyek ini
        // (mis. transfer antar-proyek yang diterima), pakai snapshot nama_barang dari TransferProyek
        // agar tidak tampil "Barang tidak dikenal". Konsisten dgn TransferMasuk/TransferDetail.
        transferList.forEach {
            val nama = namaBySku[it.sku_id].orEmpty().ifBlank { it.nama_barang }
            items.add(RiwayatItem.Transfer(it, nama))
        }
        val sorted = items.sortedByDescending { it.timestamp }
        if (filter != null) sorted.filter { it.syncStatus == filter } else sorted
    }.onEach { _initialLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: SyncStatus?) {
        filterState.value = status
    }
}
