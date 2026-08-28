package com.ta.konstruksi.testutil

import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.model.MasterKatalog
import com.ta.konstruksi.domain.model.Proyek
import com.ta.konstruksi.domain.model.TransferProyek
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.domain.repository.ProyekRepository
import com.ta.konstruksi.domain.repository.TransferRepository
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang
import com.ta.konstruksi.util.StatusTransfer
import com.ta.konstruksi.util.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Perkakas bersama untuk unit test JVM: aturan dispatcher Main, fake repository in-memory
 * (bukan Firestore/Room nyata), dan builder data uji. Dipakai lintas fitur agar test ringkas.
 */

/** Ganti Dispatchers.Main dengan dispatcher test agar viewModelScope bisa diuji di JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: kotlinx.coroutines.test.TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

// ---------- Builder data uji ----------

fun userSession(
    id: String = "M1",
    nama: String = "Budi",
    username: String = "budi",
    idProyek: String = "PRJ-001",
    namaProyek: String = "Proyek A"
) = UserSession(
    id_mandor = id, nama = nama, username = username, role = "mandor",
    auth_token = "tok", id_proyek = idProyek, nama_proyek = namaProyek
)

fun masterKatalog(
    sku: String = "SKU-1",
    kategori: String = KategoriBarang.BAHAN,
    nama: String = "Semen",
    satuan: String = "sak",
    stok: Int = 10,
    stokMinimum: Int = 0,
    idProyek: String = "PRJ-001"
) = MasterKatalog(
    sku_id = sku, kategori = kategori, nama_barang = nama, satuan = satuan,
    stok_lokal = stok, stok_minimum = stokMinimum, id_proyek = idProyek
)

fun logMutasi(
    id: String = "TX-1",
    sku: String = "SKU-1",
    jenis: JenisMutasi = JenisMutasi.CHECK_OUT,
    jumlah: Int = 2,
    kondisi: KondisiBarang? = null,
    ts: Long = 1_000L,
    sync: SyncStatus = SyncStatus.PENDING
) = LogMutasi(
    id_transaksi = id, sku_id = sku, id_mandor = "M1", id_proyek = "PRJ-001",
    jenis_mutasi = jenis, jumlah = jumlah, kondisi_barang = kondisi,
    lokasi_penggunaan = "Lantai 2", penanggungjawab = "Budi",
    local_timestamp = ts, sync_status = sync
)

fun transferProyek(
    id: String = "TR-1",
    sku: String = "SKU-1",
    nama: String = "Semen",
    kategori: String = KategoriBarang.BAHAN,
    satuan: String = "sak",
    asal: String = "Proyek A",
    tujuan: String = "Proyek B",
    jumlah: Int = 2,
    status: StatusTransfer = StatusTransfer.IN_TRANSIT,
    ts: Long = 2_000L,
    sync: SyncStatus = SyncStatus.PENDING
) = TransferProyek(
    id_transfer = id, sku_id = sku, nama_barang = nama, kategori = kategori, satuan = satuan,
    proyek_asal = asal, proyek_tujuan = tujuan, jumlah = jumlah,
    status_transfer = status, local_timestamp = ts, sync_status = sync
)

// ---------- Fake repositories (in-memory, reaktif via MutableStateFlow) ----------

class FakeKatalogRepository : KatalogRepository {
    private val items = LinkedHashMap<String, MasterKatalog>()
    private val flow = MutableStateFlow<List<MasterKatalog>>(emptyList())
    private fun publish() { flow.value = items.values.toList() }

    // sku yang dianggap punya riwayat mutasi (hapus dilarang).
    val skuBerriwayat = mutableSetOf<String>()

    fun put(item: MasterKatalog) { items[item.sku_id] = item; publish() }
    fun current(sku: String): MasterKatalog? = items[sku]

    override fun getAllKatalog(): Flow<List<MasterKatalog>> = flow
    override suspend fun getKatalogBySku(skuId: String): MasterKatalog? = items[skuId]
    override suspend fun findByProyekAndNama(idProyek: String, nama: String): MasterKatalog? =
        items.values.firstOrNull { it.id_proyek == idProyek && it.nama_barang == nama }
    override suspend fun upsertKatalog(item: MasterKatalog) = put(item)
    override suspend fun deleteKatalog(skuId: String) { items.remove(skuId); publish() }
    override suspend fun hasRiwayatMutasi(skuId: String): Boolean = skuBerriwayat.contains(skuId)
    override suspend fun updateStokLokal(skuId: String, delta: Int) {
        items[skuId]?.let { put(it.copy(stok_lokal = it.stok_lokal + delta)) }
    }
    override suspend fun decrementStokLokalJikaCukup(skuId: String, jumlah: Int): Boolean {
        val item = items[skuId] ?: return false
        if (item.stok_lokal - jumlah < 0) return false
        put(item.copy(stok_lokal = item.stok_lokal - jumlah))
        return true
    }
}

/** Fake mutasi. Bila [katalog] diberikan, meniru transaksi atomik (guard stok minus). */
class FakeMutasiRepository(private val katalog: FakeKatalogRepository? = null) : MutasiRepository {
    val recorded = mutableListOf<LogMutasi>()
    private val flow = MutableStateFlow<List<LogMutasi>>(emptyList())

    // Mensimulasikan Room melempar exception I/O saat penyimpanan gagal.
    var throwOnRecord: Throwable? = null

    fun emit(list: List<LogMutasi>) { recorded.clear(); recorded.addAll(list); flow.value = recorded.toList() }

    override fun getAllMutasi(): Flow<List<LogMutasi>> = flow
    override suspend fun recordMutasiWithStock(mutasi: LogMutasi, delta: Int): Boolean {
        throwOnRecord?.let { throw it }
        if (katalog != null) {
            val item = katalog.getKatalogBySku(mutasi.sku_id) ?: return false
            if (delta < 0 && item.stok_lokal + delta < 0) return false
            katalog.updateStokLokal(mutasi.sku_id, delta)
        }
        recorded.add(mutasi); flow.value = recorded.toList(); return true
    }
    override suspend fun getPendingMutasi(): List<LogMutasi> = recorded.filter { it.sync_status == SyncStatus.PENDING }
    override suspend fun markSynced(idTransaksi: String) {}
}

class FakeTransferRepository : TransferRepository {
    val transfers = LinkedHashMap<String, TransferProyek>()
    private val flow = MutableStateFlow<List<TransferProyek>>(emptyList())
    private fun publish() { flow.value = transfers.values.toList() }

    fun seed(t: TransferProyek) { transfers[t.id_transfer] = t; publish() }

    override fun getAllTransfer(): Flow<List<TransferProyek>> = flow
    override suspend fun createTransfer(transfer: TransferProyek) { transfers[transfer.id_transfer] = transfer; publish() }
    override suspend fun updateStatusTransfer(idTransfer: String, status: StatusTransfer) {
        transfers[idTransfer]?.let { transfers[idTransfer] = it.copy(status_transfer = status); publish() }
    }
    override suspend fun getPendingTransfer(): List<TransferProyek> =
        transfers.values.filter { it.sync_status == SyncStatus.PENDING }
    override suspend fun markSynced(idTransfer: String) {}
}

class FakeProyekRepository(list: List<Proyek> = emptyList()) : ProyekRepository {
    private val flow = MutableStateFlow(list)
    override fun getAllProyek(): Flow<List<Proyek>> = flow
}

/** Fake auth. login mengembalikan [loginResult] bila diset, atau sesi tersimpan bila ada. */
class FakeAuthRepository(
    private var session: UserSession? = null,
    private val loginResult: Result<UserSession>? = null
) : AuthRepository {
    var loggedOut = false; private set

    override suspend fun login(username: String, password: String): Result<UserSession> =
        loginResult ?: (session?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Tidak ada sesi tersimpan")))
    override suspend fun logout() { loggedOut = true; session = null }
    override suspend fun getLocalSession(): UserSession? = session
    override suspend fun isSessionValid(): Boolean = session != null
}
