package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.ProyekDao
import com.ta.konstruksi.domain.model.Proyek
import com.ta.konstruksi.domain.repository.ProyekRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProyekRepositoryImpl @Inject constructor(
    private val proyekDao: ProyekDao
) : ProyekRepository {
    override fun getAllProyek(): Flow<List<Proyek>> =
        proyekDao.getAll().map { list -> list.map { it.toDomain() } }
}
