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
    data class Mutasi(val data: LogMutasi, val namaBarang: String = "") : RiwayatItem()
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
        is Mutasi -> namaBarang.ifBlank { data.sku_id }
        is Transfer -> namaBarang.ifBlank { data.sku_id }
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
        val items = mutableListOf<RiwayatItem>()
        mutasiList.forEach { items.add(RiwayatItem.Mutasi(it, namaBySku[it.sku_id] ?: it.sku_id)) }
        transferList.forEach { items.add(RiwayatItem.Transfer(it, namaBySku[it.sku_id] ?: it.sku_id)) }
        val sorted = items.sortedByDescending { it.timestamp }
        if (filter != null) sorted.filter { it.syncStatus == filter } else sorted
    }.onEach { _initialLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: SyncStatus?) {
        filterState.value = status
    }
}
