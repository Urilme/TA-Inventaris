package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.data.local.entity.UserSessionEntity
import com.ta.konstruksi.data.repository.AuthRepositoryImpl
import com.ta.konstruksi.util.PasswordHasher
import io.mockk.mockk
import org.junit.Test

/**
 * Uji keputusan login LURING (C1) via [AuthRepositoryImpl.verifikasiLuring] — dipisah dari
 * FirebaseAuth agar murni JVM. Membuktikan: kata sandi DIVERIFIKASI (bukan hanya username),
 * dan sesi lama tanpa verifier fallback ke cocok-username.
 */
class AuthLoginLuringTest {

    // Dependensi tak dipakai oleh verifikasiLuring → mock relaxed, tak pernah dipanggil.
    private fun repo() = AuthRepositoryImpl(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    private fun sessionEntity(username: String = "budi", pwHash: String = "", pwSalt: String = "") =
        UserSessionEntity(
            id_mandor = "M1", nama = "Budi", username = username, role = "mandor",
            auth_token = "tok", id_proyek = "PRJ-001", nama_proyek = "Proyek A",
            pw_hash = pwHash, pw_salt = pwSalt
        )

    @Test
    fun `luring dengan sandi benar berhasil`() {
        val v = PasswordHasher.hash("rahasia123")
        val result = repo().verifikasiLuring(sessionEntity(pwHash = v.hashB64, pwSalt = v.saltB64), "budi", "rahasia123")
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `luring dengan sandi salah ditolak meski username cocok`() {
        val v = PasswordHasher.hash("rahasia123")
        val result = repo().verifikasiLuring(sessionEntity(pwHash = v.hashB64, pwSalt = v.saltB64), "budi", "sandiSALAH")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `sesi lama tanpa verifier fallback ke cocok-username`() {
        val result = repo().verifikasiLuring(sessionEntity(pwHash = "", pwSalt = ""), "budi", "sandi apa pun")
        assertThat(result.isSuccess).isTrue()   // kompatibilitas: verifier terisi saat online berikutnya
    }

    @Test
    fun `luring gagal bila tidak ada sesi lokal`() {
        val result = repo().verifikasiLuring(null, "budi", "rahasia123")
        assertThat(result.isFailure).isTrue()
    }
}
