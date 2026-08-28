package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.auth.LoginUseCase
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.userSession
import com.ta.konstruksi.util.usernameToEmail
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Uji validasi input login & pendelegasian ke repository. */
class LoginUseCaseTest {

    @Test
    fun `login gagal bila username kosong`() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(session = userSession()))
        val r = useCase("", "rahasia")
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("tidak boleh kosong")
    }

    @Test
    fun `login gagal bila kata sandi kosong`() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(session = userSession()))
        val r = useCase("budi", "")
        assertThat(r.isFailure).isTrue()
    }

    @Test
    fun `login luring dengan sesi tersimpan mengembalikan sesi`() = runTest {
        val session = userSession(id = "M1", idProyek = "PRJ-001")
        val useCase = LoginUseCase(FakeAuthRepository(loginResult = Result.success(session)))
        val r = useCase("budi", "rahasia")
        assertThat(r.isSuccess).isTrue()
        assertThat(r.getOrNull()!!.id_mandor).isEqualTo("M1")
        assertThat(r.getOrNull()!!.id_proyek).isEqualTo("PRJ-001")
    }

    @Test
    fun `login daring dengan kredensial valid mengembalikan sesi berhasil`() = runTest {
        val session = userSession(
            id = "M2", nama = "Siti", username = "siti", idProyek = "PRJ-002", namaProyek = "Proyek B"
        )
        val useCase = LoginUseCase(FakeAuthRepository(loginResult = Result.success(session)))
        val r = useCase("siti", "rahasia123")
        assertThat(r.isSuccess).isTrue()
        assertThat(r.getOrNull()!!.nama).isEqualTo("Siti")
        assertThat(r.getOrNull()!!.username).isEqualTo("siti")
        assertThat(r.getOrNull()!!.nama_proyek).isEqualTo("Proyek B")
    }

    @Test
    fun `login luring tanpa sesi tersimpan ditolak`() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(session = null, loginResult = null))
        val r = useCase("budi", "rahasia")
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()!!.message).contains("Tidak ada sesi tersimpan")
    }

    @Test
    fun `login diteruskan sebagai gagal bila repository gagal`() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(loginResult = Result.failure(RuntimeException("kredensial salah"))))
        val r = useCase("budi", "salah")
        assertThat(r.isFailure).isTrue()
    }

    // ---- Validasi Format Username ----
    // LoginUseCase HANYA mengecek kosong/tidak (lihat baris 11); tidak ada validator format
    // client-side. Username mengandung "@" atau spasi tetap lolos ke usernameToEmail() dan
    // baru ditolak oleh Firebase Auth SDK sendiri (format email tidak valid) di AuthRepositoryImpl.
    // Test ini membuktikan BATAS yang sesungguhnya menyebabkan penolakan: usernameToEmail()
    // menghasilkan alamat email yang cacat (mengandung "@" ganda atau spasi) untuk input semacam
    // itu, sehingga penolakan oleh Firebase adalah konsekuensi yang dapat dijelaskan, bukan
    // kebetulan.
    @Test
    fun `usernameToEmail menghasilkan alamat tidak valid untuk username mengandung at atau spasi`() {
        val hasilAt = usernameToEmail("budi@rahasia")
        val hasilSpasi = usernameToEmail("budi rahasia")

        // "@" pada username menghasilkan email dengan "@" ganda (format tidak valid).
        assertThat(hasilAt.count { it == '@' }).isEqualTo(2)
        // Spasi pada username terbawa ke email (format tidak valid, email tidak boleh memuat spasi).
        assertThat(hasilSpasi).contains(" ")
    }
}
