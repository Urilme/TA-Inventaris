package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.auth.LogoutUseCase
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.userSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Logout membersihkan sesi lokal. */
class LogoutUseCaseTest {

    @Test
    fun `logout membersihkan sesi lewat repository`() = runTest {
        val repo = FakeAuthRepository(session = userSession())
        assertThat(repo.getLocalSession()).isNotNull()

        LogoutUseCase(repo)()

        assertThat(repo.loggedOut).isTrue()
        assertThat(repo.getLocalSession()).isNull()
    }
}
