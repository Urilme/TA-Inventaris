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
import com.ta.konstruksi.util.usernameToEmail
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Autentikasi offline-first berbasis Firebase Authentication.
 * Daring: verifikasi ke FirebaseAuth lalu simpan sesi ke Room. Luring: pakai sesi lokal
 * bila username cocok (login pertama tetap butuh internet). Username diubah jadi email
 * sintetis "<username>@skn.app" sebelum dikirim ke Firebase.
 */
class AuthRepositoryImpl @Inject constructor(
    private val userSessionDao: UserSessionDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirestoreDataSource
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val usernameBersih = username.trim().lowercase()
        if (usernameBersih.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username dan kata sandi tidak boleh kosong"))
        }
        // Username tidak boleh mengandung '@' atau spasi.
        if (usernameBersih.contains('@') || usernameBersih.any { it.isWhitespace() }) {
            return Result.failure(IllegalArgumentException("Username tidak valid"))
        }

        val email = usernameToEmail(usernameBersih)
        return try {
            val hasil = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = hasil.user
                ?: return Result.failure(IllegalStateException("Autentikasi gagal, coba lagi"))

            val token = runCatching { user.getIdToken(false).await().token }.getOrNull().orEmpty()
            val session = buatSesiDari(user, usernameBersih, token)
            // Pertahankan kursor delta mutasi/transfer milik sesi lama (upsert REPLACE menimpa baris).
            val existing = userSessionDao.getSession()?.takeIf { it.id_mandor == user.uid }
            userSessionDao.upsert(
                session.toEntity().copy(
                    last_sync_mutasi = existing?.last_sync_mutasi ?: 0L,
                    last_sync_transfer = existing?.last_sync_transfer ?: 0L
                )
            )
            Result.success(session)
        } catch (e: FirebaseAuthInvalidUserException) {
            // Akun tidak terdaftar / dinonaktifkan.
            Result.failure(Exception("Akun tidak ditemukan. Hubungi admin untuk pendaftaran."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Username atau kata sandi salah"))
        } catch (e: FirebaseNetworkException) {
            // Luring: pakai sesi lokal bila cocok.
            loginLuring(usernameBersih)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Login gagal"))
        }
    }

    /** Fallback luring: pakai sesi lokal bila username cocok dengan sesi terakhir yang tersimpan. */
    private suspend fun loginLuring(username: String): Result<UserSession> {
        val lokal = userSessionDao.getSession()
        return if (lokal != null && lokal.username.equals(username, ignoreCase = true)) {
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
     * Bentuk [UserSession] dari [FirebaseUser]. Profil mandor diambil dari users/{uid};
     * bila belum ada dipakai nilai fallback agar login tetap berhasil.
     */
    private suspend fun buatSesiDari(user: FirebaseUser, usernameInput: String, token: String): UserSession {
        val username = user.email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: usernameInput
        val existing = userSessionDao.getSession()?.takeIf { it.id_mandor == user.uid }
        val lastSync = existing?.last_sync_timestamp ?: 0L

        // Profil dari server (opsional — tidak menggagalkan login bila gagal).
        val profil = runCatching { firestore.fetchUserProfile(user.uid) }.getOrNull()

        val nama = profil?.nama?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: namaDariUsername(username)
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
            username = username,
            role = role,
            auth_token = token,
            last_sync_timestamp = lastSync,
            id_proyek = idProyek,
            nama_proyek = namaProyek
        )
    }

    private fun namaDariUsername(username: String): String =
        username
            .replace(Regex("[._]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { "Mandor" }

    private fun UserSession.toEntity() = UserSessionEntity(
        id_mandor = id_mandor,
        nama = nama,
        username = username,
        role = role,
        auth_token = auth_token,
        last_sync_timestamp = last_sync_timestamp,
        id_proyek = id_proyek,
        nama_proyek = nama_proyek
    )

    private fun UserSessionEntity.toDomain() = UserSession(
        id_mandor = id_mandor,
        nama = nama,
        username = username,
        role = role,
        auth_token = auth_token,
        last_sync_timestamp = last_sync_timestamp,
        id_proyek = id_proyek,
        nama_proyek = nama_proyek
    )
}
