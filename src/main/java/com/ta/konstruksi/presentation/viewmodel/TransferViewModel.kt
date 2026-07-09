package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.Proyek
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.ProyekRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.Constants
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
    private val proyekRepository: ProyekRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    // Registry proyek untuk pilihan proyek tujuan (dari cache lokal, di-refresh saat sync)
    val proyekList: StateFlow<List<Proyek>> = proyekRepository
        .getAllProyek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sesi mandor aktif — dipakai untuk mengisi proyek_asal transfer dari proyek yang ditugaskan
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session

    init {
        viewModelScope.launch { _session.value = authRepository.getLocalSession() }
    }

    val katalogList: StateFlow<List<MasterKatalog>> = katalogRepository
        .getAllKatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transferList: StateFlow<List<TransferProyek>> = transferRepository
        .getAllTransfer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val uiState: StateFlow<TransferUiState> = _uiState

    fun createTransfer(item: MasterKatalog, proyekTujuan: String, jumlah: Int, proyekAsal: String) {
        viewModelScope.launch {
            _uiState.value = TransferUiState.Loading
            try {
                // Transfer self-describing: bawa snapshot barang agar penerima bisa menampilkan &
                // memindahkannya ke katalog proyeknya tanpa perlu katalog proyek asal.
                val transfer = TransferProyek(
                    id_transfer = UuidGenerator.generateId(),
                    sku_id = item.sku_id,
                    nama_barang = item.nama_barang,
                    kategori = item.kategori,
                    satuan = item.satuan,
                    proyek_asal = proyekAsal,
                    proyek_tujuan = proyekTujuan,
                    jumlah = jumlah,
                    status_transfer = StatusTransfer.IN_TRANSIT,
                    sync_status = SyncStatus.PENDING
                )
                transferRepository.createTransfer(transfer)
                katalogRepository.updateStokLokal(item.sku_id, -jumlah)   // stok proyek asal berkurang
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila offline
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal membuat transfer")
            }
        }
    }

    fun confirmTransfer(transfer: TransferProyek) {
        viewModelScope.launch {
            _uiState.value = TransferUiState.Loading
            try {
                // Barang benar-benar MASUK ke katalog proyek penerima (proyek aktif si mandor):
                // tambah stok ke item sejenis bila sudah ada, atau buat entri katalog baru.
                val proyekPenerima = _session.value?.id_proyek?.takeIf { it.isNotBlank() }
                    ?: authRepository.getLocalSession()?.id_proyek?.takeIf { it.isNotBlank() }
                    ?: Constants.DEFAULT_PROYEK

                if (transfer.nama_barang.isBlank()) {
                    // Transfer lama tanpa snapshot → perilaku lama (tambah stok pada sku asal)
                    katalogRepository.updateStokLokal(transfer.sku_id, transfer.jumlah)
                } else {
                    val existing = katalogRepository.findByProyekAndNama(proyekPenerima, transfer.nama_barang)
                    if (existing != null) {
                        katalogRepository.updateStokLokal(existing.sku_id, transfer.jumlah)
                    } else {
                        katalogRepository.upsertKatalog(
                            MasterKatalog(
                                sku_id = UuidGenerator.generateSkuId(),
                                kategori = transfer.kategori,
                                nama_barang = transfer.nama_barang,
                                satuan = transfer.satuan,
                                stok_lokal = transfer.jumlah,
                                stok_minimum = 0,
                                id_proyek = proyekPenerima
                            )
                        )
                    }
                }
                transferRepository.updateStatusTransfer(transfer.id_transfer, StatusTransfer.SELESAI)
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila offline
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal konfirmasi")
            }
        }
    }

    fun reset() { _uiState.value = TransferUiState.Idle }
}
