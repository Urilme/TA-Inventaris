package com.ta.konstruksi.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.ta.konstruksi.data.local.dao.UserSessionDao
import com.ta.konstruksi.data.local.entity.UserSessionEntity
import com.ta.konstruksi.data.remote.FirestoreDataSource
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.util.Constants
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
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirestoreDataSource
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
     * Bentuk [UserSession] dari [FirebaseUser]. Profil mandor (nama, role, proyek yang ditugaskan)
     * diambil dari dokumen Firestore `users/{uid}` yang di-provision oleh admin/website.
     * Bila dokumen belum ada, dipakai nilai turunan/fallback agar login tetap berhasil.
     * `last_sync_timestamp` & proyek sebelumnya dipertahankan bila sesi lama milik uid yang sama.
     */
    private suspend fun buatSesiDari(user: FirebaseUser, emailInput: String, token: String): UserSession {
        val email = user.email ?: emailInput
        val existing = userSessionDao.getSession()?.takeIf { it.id_mandor == user.uid }
        val lastSync = existing?.last_sync_timestamp ?: 0L

        // Profil dari server (opsional — tidak boleh menggagalkan login bila luring/tak ada dokumen)
        val profil = runCatching { firestore.fetchUserProfile(user.uid) }.getOrNull()

        val nama = profil?.nama?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: namaDariEmail(email)
        val role = profil?.role?.takeIf { it.isNotBlank() } ?: existing?.role ?: "mandor"
        val idProyek = profil?.idProyek?.takeIf { it.isNotBlank() }
            ?: existing?.id_proyek?.takeIf { it.isNotBlank() }
            ?: Constants.DEFAULT_PROYEK
        val namaProyek = profil?.namaProyek?.takeIf { it.isNotBlank() }
            ?: existing?.nama_proyek?.takeIf { it.isNotBlank() }
            ?: Constants.DEFAULT_NAMA_PROYEK

        return UserSession(
            id_mandor = user.uid,
            nama = nama,
            email = email,
            role = role,
            auth_token = token,
            last_sync_timestamp = lastSync,
            id_proyek = idProyek,
            nama_proyek = namaProyek
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
        last_sync_timestamp = last_sync_timestamp,
        id_proyek = id_proyek,
        nama_proyek = nama_proyek
    )

    private fun UserSessionEntity.toDomain() = UserSession(
        id_mandor = id_mandor,
        nama = nama,
        email = email,
        role = role,
        auth_token = auth_token,
        last_sync_timestamp = last_sync_timestamp,
        id_proyek = id_proyek,
        nama_proyek = nama_proyek
    )
}
