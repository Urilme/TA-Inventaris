package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.auth.LoginUseCase
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.userSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * F1 — Autentikasi. Uji validasi input & pendelegasian ke repository.
 * Login daring/luring nyata (Firebase) diuji manual (Black-Box TC-01..03).
 */
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
    fun `login diteruskan sebagai gagal bila repository gagal`() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(loginResult = Result.failure(RuntimeException("kredensial salah"))))
        val r = useCase("budi", "salah")
        assertThat(r.isFailure).isTrue()
    }
}
