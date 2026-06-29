package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.usecase.mutasi.RecordMutasiUseCase
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.JenisMutasi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MutasiUiState {
    object Idle : MutasiUiState
    object Loading : MutasiUiState
    object Success : MutasiUiState
    data class Error(val message: String) : MutasiUiState
}

@HiltViewModel
class MutasiViewModel @Inject constructor(
    private val recordMutasiUseCase: RecordMutasiUseCase,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<MutasiUiState>(MutasiUiState.Idle)
    val uiState: StateFlow<MutasiUiState> = _uiState

    fun recordMutasi(skuId: String, jenis: JenisMutasi, jumlah: Int, catatan: String?) {
        viewModelScope.launch {
            _uiState.value = MutasiUiState.Loading
            recordMutasiUseCase(skuId, jenis, jumlah, catatan).fold(
                onSuccess = {
                    _uiState.value = MutasiUiState.Success
                    syncScheduler.requestSync()   // auto-sync bila online, ditunda bila luring
                },
                onFailure = { _uiState.value = MutasiUiState.Error(it.message ?: "Gagal") }
            )
        }
    }

    fun reset() { _uiState.value = MutasiUiState.Idle }
}
