package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.Proyek
import kotlinx.coroutines.flow.Flow

interface ProyekRepository {
    /** Daftar proyek dari cache lokal (di-refresh dari Firestore saat sync). */
    fun getAllProyek(): Flow<List<Proyek>>
}
