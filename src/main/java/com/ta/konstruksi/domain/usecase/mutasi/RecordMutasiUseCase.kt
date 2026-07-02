package com.ta.konstruksi.domain.usecase.mutasi

import com.ta.konstruksi.domain.model.LogMutasi
import com.ta.konstruksi.domain.repository.KatalogRepository
import com.ta.konstruksi.domain.repository.MutasiRepository
import com.ta.konstruksi.util.JenisMutasi
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
        kondisiBarang: KondisiBarang,
        lokasiPenggunaan: String,
        penanggungjawab: String,
        catatan: String? = null
    ): Result<Unit> {
        // Field wajib (F3): kondisi_barang, lokasi_penggunaan, penanggungjawab
        if (lokasiPenggunaan.isBlank()) {
            return Result.failure(IllegalArgumentException("Lokasi penggunaan wajib diisi"))
        }
        if (penanggungjawab.isBlank()) {
            return Result.failure(IllegalArgumentException("Penanggung jawab wajib diisi"))
        }

        // Validasi stok tidak minus untuk CHECK_OUT
        if (jenisMutasi == JenisMutasi.CHECK_OUT) {
            val item = katalogRepository.getKatalogBySku(skuId)
                ?: return Result.failure(IllegalArgumentException("Barang tidak ditemukan"))
            if (item.stok_lokal < jumlah) {
                return Result.failure(IllegalStateException("Stok tidak mencukupi: ${item.stok_lokal} < $jumlah"))
            }
        }

        val mutasi = LogMutasi(
            id_transaksi = UuidGenerator.generateId(),
            sku_id = skuId,
            id_mandor = idMandor,
            id_proyek = idProyek,
            jenis_mutasi = jenisMutasi,
            jumlah = jumlah,
            kondisi_barang = kondisiBarang,
            lokasi_penggunaan = lokasiPenggunaan.trim(),
            penanggungjawab = penanggungjawab.trim(),
            catatan = catatan
        )

        // Optimistic update stok lokal
        val delta = if (jenisMutasi == JenisMutasi.CHECK_IN) jumlah else -jumlah
        katalogRepository.updateStokLokal(skuId, delta)
        mutasiRepository.recordMutasi(mutasi)

        return Result.success(Unit)
    }
}
