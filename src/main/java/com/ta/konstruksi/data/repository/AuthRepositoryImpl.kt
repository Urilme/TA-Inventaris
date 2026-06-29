package com.ta.konstruksi.data.repository

import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.entity.UserSessionEntity
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.util.UuidGenerator
import javax.inject.Inject

/**
 * Implementasi login LURING (offline-first): kredensial diverifikasi secara lokal,
 * lalu sesi dibuat dan disimpan ke Room melalui [UserSessionDao].
 *
 * Catatan: belum terhubung ke FirebaseAuth. Untuk saat ini setiap email/kata sandi
 * yang tidak kosong dianggap valid dan menghasilkan sesi mandor lokal. Ganti blok
 * verifikasi di [login] bila ingin memverifikasi ke server.
 */
class AuthRepositoryImpl @Inject constructor(
    private val userSessionDao: UserSessionDao
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserSession> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Email dan kata sandi tidak boleh kosong"))
            }

            // Nama mandor diturunkan dari bagian lokal email (mis. "budi.santoso" -> "Budi Santoso")
            val namaMandor = email.substringBefore("@")
                .replace(Regex("[._]"), " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { "Mandor" }

            // Pertahankan id & waktu sinkron bila sudah pernah login sebelumnya
            val existing = userSessionDao.getSession()
            val session = UserSession(
                id_mandor = existing?.id_mandor ?: UuidGenerator.generateId(),
                nama = namaMandor,
                email = email,
                role = "mandor",
                auth_token = UuidGenerator.generateId(),
                last_sync_timestamp = existing?.last_sync_timestamp ?: 0L
            )
            userSessionDao.upsert(session.toEntity())
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        userSessionDao.clearSession()
    }

    override suspend fun getLocalSession(): UserSession? =
        userSessionDao.getSession()?.toDomain()

    override suspend fun isSessionValid(): Boolean =
        userSessionDao.getSession() != null

    private fun UserSession.toEntity() = UserSessionEntity(
        id_mandor = id_mandor,
        nama = nama,
        email = email,
        role = role,
        auth_token = auth_token,
        last_sync_timestamp = last_sync_timestamp
    )

    private fun UserSessionEntity.toDomain() = UserSession(
        id_mandor = id_mandor,
        nama = nama,
        email = email,
        role = role,
        auth_token = auth_token,
        last_sync_timestamp = last_sync_timestamp
    )
}
