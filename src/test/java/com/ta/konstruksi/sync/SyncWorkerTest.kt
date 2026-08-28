package com.ta.konstruksi.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase
import com.ta.konstruksi.sync.worker.SyncWorker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Uji keputusan retry [SyncWorker.doWork] saat sinkronisasi gagal: mengembalikan Result.retry()
 * selama percobaan belum mencapai batas (3x), dan Result.failure() setelah batas terlampaui.
 *
 * Dijalankan sebagai unit test JVM lewat Robolectric (Context tersimulasi, SDK 34) memakai
 * androidx.work:work-testing — tanpa device/emulator fisik, dan tergabung dalam satu perintah
 * `gradlew testDebugUnitTest` bersama SyncRepositoryTest/LoginUseCaseTest lainnya.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SyncWorkerTest {

    private fun buatWorker(
        useCase: TriggerSyncUseCase,
        runAttemptCount: Int
    ): SyncWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ) = SyncWorker(appContext, workerParameters, useCase)
        }
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(factory)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    @Test
    fun `doWork mengembalikan retry saat sync gagal dan percobaan belum mencapai batas`() = runBlocking {
        val gagal = mockk<TriggerSyncUseCase>()
        coEvery { gagal.invoke() } returns Result.failure(RuntimeException("koneksi lambat/timeout"))
        val worker = buatWorker(gagal, runAttemptCount = 1) // percobaan ke-2, masih < 3

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    @Test
    fun `doWork mengembalikan failure setelah tiga kali percobaan gagal`() = runBlocking {
        val gagal = mockk<TriggerSyncUseCase>()
        coEvery { gagal.invoke() } returns Result.failure(RuntimeException("koneksi lambat/timeout"))
        val worker = buatWorker(gagal, runAttemptCount = 3) // sudah 3x gagal sebelumnya

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }

    @Test
    fun `doWork mengembalikan success saat sync berhasil`() = runBlocking {
        val sukses = mockk<TriggerSyncUseCase>()
        coEvery { sukses.invoke() } returns Result.success(Unit)
        val worker = buatWorker(sukses, runAttemptCount = 0)

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }
}
