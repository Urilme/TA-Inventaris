package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.SyncStatus
import com.ta.konstruksi.util.UuidGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TransferUiState {
    object Idle : TransferUiState
    object Loading : TransferUiState
    object Success : TransferUiState
    data class Error(val message: String) : TransferUiState
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val katalogRepository: KatalogRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    val katalogList: StateFlow<List<MasterKatalog>> = katalogRepository
        .getAllKatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transferList: StateFlow<List<TransferProyek>> = transferRepository
        .getAllTransfer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val uiState: StateFlow<TransferUiState> = _uiState

    fun createTransfer(skuId: String, proyekTujuan: String, jumlah: Int, proyekAsal: String) {
        viewModelScope.launch {
            _uiState.value = TransferUiState.Loading
            try {
                val transfer = TransferProyek(
                    id_transfer = UuidGenerator.generateId(),
                    sku_id = skuId,
                    proyek_asal = proyekAsal,
                    proyek_tujuan = proyekTujuan,
                    jumlah = jumlah,
                    status_transfer = StatusTransfer.IN_TRANSIT,
                    sync_status = SyncStatus.PENDING
                )
                transferRepository.createTransfer(transfer)
                katalogRepository.updateStokLokal(skuId, -jumlah)
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila luring
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal membuat transfer")
            }
        }
    }

    fun confirmTransfer(idTransfer: String, skuId: String, jumlah: Int) {
        viewModelScope.launch {
            _uiState.value = TransferUiState.Loading
            try {
                transferRepository.updateStatusTransfer(idTransfer, StatusTransfer.SELESAI)
                katalogRepository.updateStokLokal(skuId, jumlah)
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila luring
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal konfirmasi")
            }
        }
    }

    fun reset() { _uiState.value = TransferUiState.Idle }
}
