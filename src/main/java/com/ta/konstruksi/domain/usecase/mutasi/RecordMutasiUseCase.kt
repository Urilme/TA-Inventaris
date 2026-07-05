package com.ta.konstruksi.domain.usecase.mutasi

import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.util.JenisMutasi
import com.ta.konstruksi.util.KategoriBarang
import com.ta.konstruksi.util.KondisiBarang
import com.ta.konstruksi.util.UuidGenerator
import javax.inject.Inject

class RecordMutasiUseCase @Inject constructor(
    private val mutasiRepository: MutasiRepository,
    private val katalogRepository: KatalogRepository
) {
    suspend operator fun invoke(
        skuId: String,
        idMandor: String,
        idProyek: String,
        jenisMutasi: JenisMutasi,
        jumlah: Int,
        kondisiBarang: KondisiBarang?,   // null diperbolehkan; wajib hanya untuk kategori "alat"
        lokasiPenggunaan: String,
        penanggungjawab: String,
        catatan: String? = null
    ): Result<Unit> {
        // Field wajib (F3): lokasi_penggunaan, penanggungjawab
        if (lokasiPenggunaan.isBlank()) {
            return Result.failure(IllegalArgumentException("Lokasi penggunaan wajib diisi"))
        }
        if (penanggungjawab.isBlank()) {
            return Result.failure(IllegalArgumentException("Penanggung jawab wajib diisi"))
        }

        val item = katalogRepository.getKatalogBySku(skuId)
            ?: return Result.failure(IllegalArgumentException("Barang tidak ditemukan"))

        // Aturan kondisi barang (F3): hanya kategori "alat" yang mewajibkan & menyimpan kondisi.
        // bahan & scaffolding → kondisi selalu null (material habis pakai / bukan aset per-unit).
        val kondisiFinal: KondisiBarang? = if (item.kategori == KategoriBarang.ALAT) {
            kondisiBarang
                ?: return Result.failure(IllegalArgumentException("Kondisi barang wajib diisi untuk kategori alat"))
        } else {
            null
        }

        // Validasi stok tidak minus untuk CHECK_OUT (cek awal untuk pesan yang ramah)
        if (jenisMutasi == JenisMutasi.CHECK_OUT && item.stok_lokal < jumlah) {
            return Result.failure(IllegalStateException("Stok tidak mencukupi: ${item.stok_lokal} < $jumlah"))
        }

        val mutasi = LogMutasi(
            id_transaksi = UuidGenerator.generateId(),
            sku_id = skuId,
            id_mandor = idMandor,
            id_proyek = idProyek,
            jenis_mutasi = jenisMutasi,
            jumlah = jumlah,
            kondisi_barang = kondisiFinal,
            lokasi_penggunaan = lokasiPenggunaan.trim(),
            penanggungjawab = penanggungjawab.trim(),
            catatan = catatan
        )

        // Insert log + update stok dalam satu transaksi atomik.
        // Guard di dalam transaksi (recordMutasiAtomic) mencegah stok minus saat mutasi bersamaan.
        val delta = if (jenisMutasi == JenisMutasi.CHECK_IN) jumlah else -jumlah
        val tersimpan = mutasiRepository.recordMutasiWithStock(mutasi, delta)
        return if (tersimpan) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Stok tidak mencukupi (berubah oleh transaksi lain)"))
        }
    }
}
