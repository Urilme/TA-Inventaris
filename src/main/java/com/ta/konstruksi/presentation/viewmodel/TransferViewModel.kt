package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.Proyek
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.HasilBuatTransfer
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

    // Penjaga proses sinkron (diperiksa SEBELUM launch) — mencegah ketukan ganda lolos di jeda
    // sebelum Compose merender ulang tombol nonaktif. Pola sama dengan ProfileViewModel.logout().
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /**
     * Bangkitkan id_transfer SEKALI saat dialog konfirmasi dibuka. Dengan id yang stabil lintas
     * ketukan, [createTransfer] menjadi idempoten: ketukan kedua membawa id sama → ditolak di DAO
     * tanpa mengurangi stok dua kali. Lihat [HasilBuatTransfer].
     */
    fun generateTransferId(): String = UuidGenerator.generateId()

    fun createTransfer(item: MasterKatalog, proyekTujuan: String, jumlah: Int, proyekAsal: String, idTransfer: String) {
        if (_busy.value) return
        // Proyek tujuan tidak boleh sama dengan proyek asal (dijaga UI via dropdown, ditegakkan
        // juga di domain agar bebas dari jalur input tak terduga). Diperiksa sinkron di awal.
        if (proyekAsal.trim().equals(proyekTujuan.trim(), ignoreCase = true)) {
            _uiState.value = TransferUiState.Error("Proyek tujuan tidak boleh sama dengan proyek asal")
            return
        }
        _busy.value = true
        _uiState.value = TransferUiState.Loading
        viewModelScope.launch {
            try {
                // Transfer self-describing: bawa snapshot barang agar penerima bisa menampilkan &
                // memindahkannya ke katalog proyeknya tanpa perlu katalog proyek asal.
                val transfer = TransferProyek(
                    id_transfer = idTransfer,
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
                // Buat transfer + kurangi stok proyek asal secara ATOMIK & idempoten by-id.
                when (transferRepository.createTransfer(transfer)) {
                    HasilBuatTransfer.SUKSES -> {
                        _uiState.value = TransferUiState.Success
                        syncScheduler.requestSync()   // auto-sync bila online, ditunda bila offline
                    }
                    // Ketukan kedua (id sama): transfer sudah dibuat oleh ketukan pertama; anggap
                    // sukses tanpa mengurangi stok lagi agar UI menutup dialog dengan bersih.
                    HasilBuatTransfer.DUPLIKAT -> _uiState.value = TransferUiState.Success
                    HasilBuatTransfer.STOK_KURANG ->
                        _uiState.value = TransferUiState.Error("Stok tidak mencukupi untuk transfer sebanyak $jumlah")
                }
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal membuat transfer")
            } finally {
                _busy.value = false
            }
        }
    }

    fun confirmTransfer(transfer: TransferProyek) {
        if (_busy.value) return
        // Penjaga status di UI (cepat menolak snapshot yang jelas bukan IN_TRANSIT).
        if (transfer.status_transfer != StatusTransfer.IN_TRANSIT) {
            _uiState.value = TransferUiState.Error("Transfer sudah ${transfer.status_transfer.name.lowercase()}, tidak bisa dikonfirmasi")
            return
        }
        _busy.value = true
        _uiState.value = TransferUiState.Loading
        viewModelScope.launch {
            try {
                // Barang MASUK ke katalog proyek penerima. Guard status + tambah stok + set SELESAI
                // dijalankan ATOMIK di DAO agar konfirmasi ganda / ulang tak menambah stok dua kali.
                val proyekPenerima = _session.value?.id_proyek?.takeIf { it.isNotBlank() }
                    ?: authRepository.getLocalSession()?.id_proyek?.takeIf { it.isNotBlank() }
                    ?: Constants.DEFAULT_PROYEK

                val berhasil = transferRepository.confirmReceiveAtomic(transfer.id_transfer, proyekPenerima)
                if (!berhasil) {
                    _uiState.value = TransferUiState.Error("Transfer sudah dikonfirmasi atau dibatalkan")
                    return@launch
                }
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila offline
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal konfirmasi")
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Batalkan transfer yang masih IN_TRANSIT (mis. penerima menolak / salah kirim).
     * Efeknya kebalikan dari [createTransfer]: stok dikembalikan ke proyek asal dan status
     * menjadi DIBATALKAN. Hanya berlaku selama transfer belum SELESAI.
     */
    fun cancelTransfer(transfer: TransferProyek) {
        if (_busy.value) return
        if (transfer.status_transfer != StatusTransfer.IN_TRANSIT) {
            _uiState.value = TransferUiState.Error("Transfer sudah ${transfer.status_transfer.name.lowercase()}, tidak bisa dibatalkan")
            return
        }
        _busy.value = true
        _uiState.value = TransferUiState.Loading
        viewModelScope.launch {
            try {
                // Kembalikan stok ke proyek asal + set DIBATALKAN secara ATOMIK (guard status).
                val berhasil = transferRepository.cancelAtomic(transfer.id_transfer)
                if (!berhasil) {
                    _uiState.value = TransferUiState.Error("Transfer sudah berubah status, tidak bisa dibatalkan")
                    return@launch
                }
                _uiState.value = TransferUiState.Success
                syncScheduler.requestSync()   // auto-sync bila online, ditunda bila offline
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(e.message ?: "Gagal membatalkan transfer")
            } finally {
                _busy.value = false
            }
        }
    }

    fun reset() { _uiState.value = TransferUiState.Idle }

    /**
     * Picu sinkronisasi sekali-jalan (expedited). Dipanggil saat layar Transfer Masuk dibuka agar
     * transfer masuk tampil nyaris seketika tanpa menunggu siklus periodik 15 menit — melengkapi
     * pemicu foreground di MainActivity. Tetap dalam kerangka delta sync via WorkManager.
     */
    fun syncSekarang() { syncScheduler.requestSync() }
}
