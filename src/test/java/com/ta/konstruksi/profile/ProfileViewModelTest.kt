package com.ta.konstruksi.profile

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase
import com.ta.konstruksi.presentation.viewmodel.ProfileViewModel
import com.ta.konstruksi.testutil.FakeAuthRepository
import com.ta.konstruksi.testutil.FakeKatalogRepository
import com.ta.konstruksi.testutil.FakeMutasiRepository
import com.ta.konstruksi.testutil.FakeTransferRepository
import com.ta.konstruksi.testutil.MainDispatcherRule
import com.ta.konstruksi.testutil.logMutasi
import com.ta.konstruksi.testutil.userSession
import com.ta.konstruksi.util.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Uji perilaku logout ProfileViewModel terkait data belum tersinkron (PENDING). */
class ProfileViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `logout dengan data PENDING menyinkronkan dulu lalu keluar`() = runTest {
        val katalog = FakeKatalogRepository()
        val mutasi = FakeMutasiRepository().apply {
            emit(listOf(logMutasi(id = "TX-1", sku = "SKU-1", sync = SyncStatus.PENDING)))
        }
        val transfer = FakeTransferRepository()
        val auth = FakeAuthRepository(session = userSession())
        val triggerSync = mockk<TriggerSyncUseCase>()
        coEvery { triggerSync.invoke() } returns Result.success(Unit)
        val vm = ProfileViewModel(auth, katalog, mutasi, transfer, triggerSync)

        // Pastikan pendingCount sudah terhitung sebelum logout membacanya.
        val state = vm.uiState.first { it.pendingCount > 0 }
        assertThat(state.pendingCount).isEqualTo(1)

        vm.logout {}
        advanceUntilIdle()

        coVerify { triggerSync.invoke() }        // sinkron dipicu lebih dulu
        assertThat(auth.loggedOut).isTrue()      // baru keluar
    }

    @Test
    fun `logout tanpa data PENDING langsung keluar tanpa sinkron`() = runTest {
        val katalog = FakeKatalogRepository()
        val mutasi = FakeMutasiRepository()      // tidak ada mutasi
        val transfer = FakeTransferRepository()
        val auth = FakeAuthRepository(session = userSession())
        val triggerSync = mockk<TriggerSyncUseCase>(relaxed = true)
        val vm = ProfileViewModel(auth, katalog, mutasi, transfer, triggerSync)

        val state = vm.uiState.first()
        assertThat(state.pendingCount).isEqualTo(0)

        var loggedOutCalled = false
        vm.logout { loggedOutCalled = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { triggerSync.invoke() }   // tak ada yang perlu dikirim
        assertThat(auth.loggedOut).isTrue()
        assertThat(loggedOutCalled).isTrue()
    }
}
