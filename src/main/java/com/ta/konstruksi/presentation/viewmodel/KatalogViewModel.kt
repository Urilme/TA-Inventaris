package com.ta.konstruksi.presentation.viewmodel

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface KatalogUiState {
    object Idle : KatalogUiState
    object Loading : KatalogUiState
    object Success : KatalogUiState
    data class Error(val message: String) : KatalogUiState
}

@HiltViewModel
class KatalogViewModel @Inject constructor(
    private val katalogRepository: KatalogRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    val katalogList: StateFlow<List<MasterKatalog>> = katalogRepository
        .getAllKatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<KatalogUiState>(KatalogUiState.Idle)
    val uiState: StateFlow<KatalogUiState> = _uiState

    fun tambahKatalog(kategori: String, namaBarang: String, satuan: String, stokAwal: Int, skuId: String = UuidGenerator.generateSkuId()) {
        viewModelScope.launch {
            _uiState.value = KatalogUiState.Loading
            try {
                katalogRepository.upsertKatalog(
                    MasterKatalog(
                        sku_id = skuId,
                        kategori = kategori,
                        nama_barang = namaBarang,
                        satuan = satuan,
                        stok_lokal = stokAwal
                    )
                )
                _uiState.value = KatalogUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila luring
            } catch (e: Exception) {
                _uiState.value = KatalogUiState.Error(e.message ?: "Gagal menyimpan katalog")
            }
        }
    }

    fun hapusKatalog(skuId: String) {
        viewModelScope.launch { katalogRepository.deleteKatalog(skuId) }
    }

    fun resetUiState() { _uiState.value = KatalogUiState.Idle }
}
