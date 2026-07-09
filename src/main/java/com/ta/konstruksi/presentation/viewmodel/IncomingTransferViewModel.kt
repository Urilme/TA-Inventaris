package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.StatusTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sumber tunggal jumlah "transfer masuk" (aset IN_TRANSIT yang menuju proyek aktif mandor dan
 * masih menunggu konfirmasi penerimaan). Dipakai oleh badge navbar bawah agar konsisten di
 * semua layar. Predikat identik dengan [com.ta.konstruksi.presentation.ui.transfer.TransferMasukScreen].
 * Murni baca dari Room — mendukung mode luring.
 */
@HiltViewModel
class IncomingTransferViewModel @Inject constructor(
    transferRepository: TransferRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _proyekAktif = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            _proyekAktif.value = authRepository.getLocalSession()?.nama_proyek?.takeIf { it.isNotBlank() }
        }
    }

    val count: StateFlow<Int> = combine(
        transferRepository.getAllTransfer(),
        _proyekAktif
    ) { transfers, proyekAktif ->
        if (proyekAktif == null) 0
        else transfers.count {
            it.status_transfer == StatusTransfer.IN_TRANSIT &&
                it.proyek_tujuan.trim().equals(proyekAktif, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
