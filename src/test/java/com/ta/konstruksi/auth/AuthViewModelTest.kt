package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.usecase.auth.LoginUseCase
import com.ta.konstruksi.domain.usecase.auth.LogoutUseCase
import com.ta.konstruksi.presentation.viewmodel.AuthViewModel
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.userSession
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Uji reaksi AuthViewModel saat sesi lokal kosong: mengarahkan ke halaman masuk
 * (`getLocalSession() != null` menentukan status startup).
 */
class AuthViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun buatVm(session: UserSession?) = AuthViewModel(
        loginUseCase = LoginUseCase(FakeAuthRepository(session = session)),
        logoutUseCase = LogoutUseCase(FakeAuthRepository(session = session)),
        authRepository = FakeAuthRepository(session = session)
    )

    @Test
    fun `sesi lokal kosong saat startup mengarahkan ke layar masuk`() = runTest {
        val vm = buatVm(session = null)
        assertThat(vm.startupLoggedIn.value).isFalse()
    }

    @Test
    fun `sesi lokal tersimpan saat startup melewati layar masuk`() = runTest {
        val vm = buatVm(session = userSession())
        assertThat(vm.startupLoggedIn.value).isTrue()
    }
}
