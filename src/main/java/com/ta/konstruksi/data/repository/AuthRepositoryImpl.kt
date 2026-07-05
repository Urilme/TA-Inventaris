package com.ta.konstruksi.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.entity.UserSessionEntity
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementasi autentikasi OFFLINE-FIRST berbasis Firebase Authentication.
 *
 * - Daring: kredensial diverifikasi ke FirebaseAuth, lalu sesi disimpan ke Room ([UserSessionDao])
 *   agar aplikasi tetap bisa dipakai saat luring.
 * - Luring: bila sudah pernah login (sesi tersimpan untuk email yang sama), pengguna tetap
 *   dapat masuk memakai sesi lokal. Login PERTAMA tetap memerlukan internet.
 *
 * Akun mandor dibuat oleh admin melalui Firebase Console (tidak ada pendaftaran mandiri di app).
 */
class AuthRepositoryImpl @Inject constructor(
    private val userSessionDao: UserSessionDao,
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserSession> {
        val emailBersih = email.trim()
        if (emailBersih.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email dan kata sandi tidak boleh kosong"))
        }

        return try {
            val hasil = firebaseAuth.signInWithEmailAndPassword(emailBersih, password).await()
            val user = hasil.user
                ?: return Result.failure(IllegalStateException("Autentikasi gagal, coba lagi"))

            val token = runCatching { user.getIdToken(false).await().token }.getOrNull().orEmpty()
            val session = buatSesiDari(user, emailBersih, token)
            userSessionDao.upsert(session.toEntity())
            Result.success(session)
        } catch (e: FirebaseAuthInvalidUserException) {
            // Akun tidak terdaftar / dinonaktifkan
            Result.failure(Exception("Akun tidak ditemukan. Hubungi admin untuk pendaftaran."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email atau kata sandi salah"))
        } catch (e: FirebaseNetworkException) {
            // Luring: izinkan masuk hanya bila sesi tersimpan cocok dengan email yang dimasukkan
            loginLuring(emailBersih)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Login gagal"))
        }
    }

    /** Fallback luring: pakai sesi lokal bila email cocok dengan sesi terakhir yang tersimpan. */
    private suspend fun loginLuring(email: String): Result<UserSession> {
        val lokal = userSessionDao.getSession()
        return if (lokal != null && lokal.email.equals(email, ignoreCase = true)) {
            Result.success(lokal.toDomain())
        } else {
            Result.failure(Exception("Tidak ada koneksi. Login pertama memerlukan internet."))
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        userSessionDao.clearSession()
    }

    override suspend fun getLocalSession(): UserSession? =
        userSessionDao.getSession()?.toDomain()

    override suspend fun isSessionValid(): Boolean =
        userSessionDao.getSession() != null || firebaseAuth.currentUser != null

    /**
     * Bentuk [UserSession] dari [FirebaseUser]. Nama diambil dari displayName bila ada,
     * jika tidak diturunkan dari bagian lokal email (mis. "budi.santoso" -> "Budi Santoso").
     * `last_sync_timestamp` dipertahankan hanya bila sesi sebelumnya milik uid yang sama.
     */
    private suspend fun buatSesiDari(user: FirebaseUser, emailInput: String, token: String): UserSession {
        val email = user.email ?: emailInput
        val nama = user.displayName?.takeIf { it.isNotBlank() } ?: namaDariEmail(email)
        val existing = userSessionDao.getSession()
        val lastSync = if (existing?.id_mandor == user.uid) existing.last_sync_timestamp else 0L
        return UserSession(
            id_mandor = user.uid,
            nama = nama,
            email = email,
            role = "mandor",
            auth_token = token,
            last_sync_timestamp = lastSync
        )
    }

    private fun namaDariEmail(email: String): String =
        email.substringBefore("@")
            .replace(Regex("[._]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { "Mandor" }

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
