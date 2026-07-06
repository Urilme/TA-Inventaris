package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.SyncStatus
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
    transferRepository: TransferRepository
) : ViewModel() {

    private val sessionFlow = flow { emit(authRepository.getLocalSession()) }

    val uiState: StateFlow<ProfileUiState> = combine(
        sessionFlow,
        katalogRepository.getAllKatalog(),
        mutasiRepository.getAllMutasi(),
        transferRepository.getAllTransfer()
    ) { session, katalog, mutasi, transfer ->
        val pending = katalog.count { it.sync_status == SyncStatus.PENDING } +
            mutasi.count { it.sync_status == SyncStatus.PENDING } +
            transfer.count { it.sync_status == SyncStatus.PENDING }
        ProfileUiState(session = session, pendingCount = pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    /** Bersihkan sesi (Firebase signOut + hapus Room). Navigasi ke Login ditangani UI. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { authRepository.logout() } catch (_: Exception) {}
            onDone()
        }
    }
}
