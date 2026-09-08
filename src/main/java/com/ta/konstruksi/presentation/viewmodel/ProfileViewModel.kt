package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val session: UserSession? = null,
    val pendingCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    katalogRepository: KatalogRepository,
    mutasiRepository: MutasiRepository,
    transferRepository: TransferRepository,
    private val triggerSyncUseCase: TriggerSyncUseCase
) : ViewModel() {

    private val sessionFlow = flow { emit(authRepository.getLocalSession()) }

    val uiState: StateFlow<ProfileUiState> = combine(
        sessionFlow,
        katalogRepository.getAllKatalog(),
        mutasiRepository.getAllMutasi(),
        transferRepository.getAllTransfer()
    ) { session, katalog, mutasi, transfer ->
        // Hitung sebagai aksi pengguna (perubahan katalog akibat mutasi tidak dihitung ganda) —
        // definisi yang sama dengan Dashboard. Lihat buildPendingSyncItems().
        val pending = buildPendingSyncItems(katalog, mutasi, transfer).size
        ProfileUiState(session = session, pendingCount = pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    // Menandai proses logout berjalan (mencegah ketukan ganda; menampilkan spinner di dialog).
    private val _loggingOut = MutableStateFlow(false)
    val loggingOut: StateFlow<Boolean> = _loggingOut

    /**
     * Keluar akun. Bila masih ada data belum tersinkron, coba KIRIM dulu (best-effort) dan tunggu
     * hasilnya sebelum sesi dihapus — TriggerSyncUseCase gagal cepat saat offline sehingga tetap
     * lanjut logout. Setelah itu bersihkan sesi (Firebase signOut + hapus Room). Data PENDING yang
     * belum sempat terkirim tetap tersimpan di Room dan baru bisa dikirim saat mandor yang sama
     * masuk kembali di perangkat ini. Navigasi ke Login ditangani UI via [onDone].
     */
    fun logout(onDone: () -> Unit) {
        if (_loggingOut.value) return
        viewModelScope.launch {
            _loggingOut.value = true
            try {
                if (uiState.value.pendingCount > 0) {
                    runCatching { triggerSyncUseCase() }
                }
                runCatching { authRepository.logout() }
                onDone()
            } finally {
                _loggingOut.value = false
            }
        }
    }
}
