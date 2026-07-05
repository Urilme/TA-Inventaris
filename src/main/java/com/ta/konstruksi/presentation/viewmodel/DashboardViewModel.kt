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
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.SyncStatus
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
    val todayMutasiCount: Int = 0
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
        val lowStock = katalogList
            .sortedBy { it.stok_lokal }
            .take(3)
        // Daftar entitas yang masih menunggu sinkron (katalog + mutasi + transfer)
        val namaBySku = katalogList.associate { it.sku_id to it.nama_barang }
        val pendingItems = buildList {
            katalogList.filter { it.sync_status == SyncStatus.PENDING }.forEach {
                add(PendingSyncItem(it.nama_barang, "Perubahan katalog", it.local_timestamp))
            }
            mutasiList.filter { it.sync_status == SyncStatus.PENDING }.forEach {
                val nama = namaBySku[it.sku_id] ?: it.sku_id
                val detail = if (it.jenis_mutasi == JenisMutasi.CHECK_IN) "Masuk +${it.jumlah}" else "Keluar -${it.jumlah}"
                add(PendingSyncItem(nama, detail, it.local_timestamp))
            }
            transferList.filter { it.sync_status == SyncStatus.PENDING }.forEach {
                val nama = namaBySku[it.sku_id] ?: it.sku_id
                add(PendingSyncItem(nama, "Transfer ${it.jumlah} → ${it.proyek_tujuan}", it.local_timestamp))
            }
        }.sortedByDescending { it.timestamp }
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayCount = mutasiList.count { it.local_timestamp >= todayStart }
        DashboardUiState(session, lowStock, pendingItems.size, pendingItems, todayCount)
    }.onEach { _initialLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun logout() {
        viewModelScope.launch {
            try { authRepository.logout() } catch (_: Exception) {}
        }
    }
}
