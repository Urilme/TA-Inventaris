package com.ta.konstruksi.presentation.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.UuidGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface KatalogUiState {
    object Idle : KatalogUiState
    object Loading : KatalogUiState
    object Success : KatalogUiState
    data class Error(val message: String) : KatalogUiState
}

/** Hasil eksplisit penghapusan barang — dipakai UI untuk navigasi/snackbar. */
sealed interface HapusKatalogState {
    object Idle : HapusKatalogState
    object Success : HapusKatalogState
    data class Error(val message: String) : HapusKatalogState
}

@HiltViewModel
class KatalogViewModel @Inject constructor(
    private val katalogRepository: KatalogRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    // Menandai emisi pertama dari Room agar bisa membedakan "sedang memuat" vs "kosong"
    private val _initialLoaded = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _initialLoaded
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val katalogList: StateFlow<List<MasterKatalog>> = katalogRepository
        .getAllKatalog()
        .onEach { _initialLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Status pekerjaan sync (ENQUEUED/RUNNING) — dipakai indikator gestur tarik-untuk-refresh. */
    val isRefreshing: StateFlow<Boolean> = syncScheduler.observeSyncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Dipicu gestur tarik-untuk-refresh; sinkronisasi tetap otomatis, ini murni mempercepat. */
    fun refresh() = syncScheduler.requestSync()

    private val _uiState = MutableStateFlow<KatalogUiState>(KatalogUiState.Idle)
    val uiState: StateFlow<KatalogUiState> = _uiState

    private val _hapusState = MutableStateFlow<HapusKatalogState>(HapusKatalogState.Idle)
    val hapusState: StateFlow<HapusKatalogState> = _hapusState

    fun tambahKatalog(
        kategori: String,
        namaBarang: String,
        satuan: String,
        stokAwal: Int,
        stokMinimum: Int = 0,
        skuId: String = UuidGenerator.generateSkuId()
    ) {
        viewModelScope.launch {
            _uiState.value = KatalogUiState.Loading
            try {
                katalogRepository.upsertKatalog(
                    MasterKatalog(
                        sku_id = skuId,
                        kategori = kategori,
                        nama_barang = namaBarang,
                        satuan = satuan,
                        stok_lokal = stokAwal,
                        stok_minimum = stokMinimum
                    )
                )
                _uiState.value = KatalogUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila luring
            } catch (e: Exception) {
                _uiState.value = KatalogUiState.Error(e.message ?: "Gagal menyimpan katalog")
            }
        }
    }

    /**
     * Hapus barang dari katalog. Barang yang sudah punya riwayat mutasi TIDAK bisa dihapus
     * (audit trail dijaga FK RESTRICT) — dicegah lewat pra-cek, dengan tangkapan constraint
     * sebagai jaring pengaman. Hasil dipublikasikan via [hapusState].
     */
    fun hapusKatalog(skuId: String) {
        viewModelScope.launch {
            try {
                if (katalogRepository.hasRiwayatMutasi(skuId)) {
                    _hapusState.value = HapusKatalogState.Error(
                        "Barang tidak bisa dihapus karena sudah memiliki riwayat mutasi."
                    )
                    return@launch
                }
                katalogRepository.deleteKatalog(skuId)
                _hapusState.value = HapusKatalogState.Success
            } catch (e: SQLiteConstraintException) {
                _hapusState.value = HapusKatalogState.Error(
                    "Barang tidak bisa dihapus karena sudah memiliki riwayat mutasi."
                )
            } catch (e: Exception) {
                _hapusState.value = HapusKatalogState.Error(e.message ?: "Gagal menghapus barang")
            }
        }
    }

    fun resetHapusState() { _hapusState.value = HapusKatalogState.Idle }

    fun resetUiState() { _uiState.value = KatalogUiState.Idle }
}
