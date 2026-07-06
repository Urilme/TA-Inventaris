package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.usecase.mutasi.RecordMutasiUseCase
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.Constants
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KondisiBarang
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
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<MutasiUiState>(MutasiUiState.Idle)
    val uiState: StateFlow<MutasiUiState> = _uiState

    // Sesi mandor aktif — dipakai untuk auto-fill id_mandor & pre-fill penanggung jawab
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session

    init {
        viewModelScope.launch { _session.value = authRepository.getLocalSession() }
    }

    fun recordMutasi(
        skuId: String,
        jenis: JenisMutasi,
        jumlah: Int,
        kondisi: KondisiBarang?,   // null untuk kategori bahan/scaffolding
        lokasiPenggunaan: String,
        penanggungjawab: String,
        catatan: String?
    ) {
        viewModelScope.launch {
            _uiState.value = MutasiUiState.Loading
            // id_mandor & id_proyek diambil otomatis dari sesi login aktif — tidak diinput user
            val idMandor = _session.value?.id_mandor.orEmpty()
            val idProyek = _session.value?.id_proyek?.takeIf { it.isNotBlank() } ?: Constants.DEFAULT_PROYEK
            recordMutasiUseCase(
                skuId = skuId,
                idMandor = idMandor,
                idProyek = idProyek,
                jenisMutasi = jenis,
                jumlah = jumlah,
                kondisiBarang = kondisi,
                lokasiPenggunaan = lokasiPenggunaan,
                penanggungjawab = penanggungjawab,
                catatan = catatan
            ).fold(
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
