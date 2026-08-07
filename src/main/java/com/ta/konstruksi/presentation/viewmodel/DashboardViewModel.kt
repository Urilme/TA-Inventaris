package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.sync.SyncScheduler
import com.ta.konstruksi.util.StatusTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingSyncItem(
    val label: String,      // nama barang
    val detail: String,     // mis. "Masuk +1", "Keluar -2", "Tambah katalog", "Transfer 5"
    val timestamp: Long
)

data class DashboardUiState(
    val session: UserSession? = null,
    val lowStockItems: List<MasterKatalog> = emptyList(),
    val pendingCount: Int = 0,
    val pendingItems: List<PendingSyncItem> = emptyList(),
    val todayMutasiCount: Int = 0,
    // Aset IN_TRANSIT menuju proyek aktif yang menunggu konfirmasi penerimaan
    val incomingTransferCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val katalogRepository: KatalogRepository,
    private val mutasiRepository: MutasiRepository,
    private val transferRepository: TransferRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    /** Dorong sinkronisasi (mis. saat Dashboard dibuka dalam keadaan online dengan backlog). */
    fun requestSync() = syncScheduler.requestSync()

    private val sessionFlow = flow { emit(authRepository.getLocalSession()) }

    // Menandai emisi pertama agar Dashboard bisa menampilkan indikator memuat awal
    private val _initialLoaded = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _initialLoaded
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val uiState: StateFlow<DashboardUiState> = combine(
        sessionFlow,
        katalogRepository.getAllKatalog(),
        mutasiRepository.getAllMutasi(),
        transferRepository.getAllTransfer()
    ) { session, katalogList, mutasiList, transferList ->
        // Peringatan stok (F2): barang yang punya ambang minimum (> 0) dan stoknya sudah menyentuh
        // ambang (KRITIS) atau masih dalam 1.5× ambang (RENDAH, peringatan dini). Konsisten dengan
        // aturan stokHealth() di Katalog. Urutkan berdasarkan rasio stok/ambang — yang paling genting
        // (rasio terkecil, mis. 0 = stok habis) di atas, adil untuk ambang yang berbeda-beda.
        val lowStock = katalogList
            .filter { it.stok_minimum > 0 && it.stok_lokal <= it.stok_minimum * 1.5 }
            .sortedBy { it.stok_lokal.toDouble() / it.stok_minimum }
            .take(3)
        // Daftar entitas yang masih menunggu sinkron, disusun sebagai aksi pengguna (bukan baris
        // tabel mentah) agar 1 mutasi tidak tampak jadi 2 item. Lihat buildPendingSyncItems().
        val pendingItems = buildPendingSyncItems(katalogList, mutasiList, transferList)
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayCount = mutasiList.count { it.local_timestamp >= todayStart }
        // Transfer masuk menunggu konfirmasi — predikat sama dengan TransferMasukScreen.
        val proyekAktif = session?.nama_proyek?.takeIf { it.isNotBlank() }
        val incomingCount = if (proyekAktif == null) 0 else transferList.count {
            it.status_transfer == StatusTransfer.IN_TRANSIT &&
                it.proyek_tujuan.trim().equals(proyekAktif, ignoreCase = true)
        }
        DashboardUiState(session, lowStock, pendingItems.size, pendingItems, todayCount, incomingCount)
    }.onEach { _initialLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun logout() {
        viewModelScope.launch {
            try { authRepository.logout() } catch (_: Exception) {}
        }
    }
}
