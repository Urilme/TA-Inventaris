package com.ta.konstruksi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.ta.konstruksi.sync.worker.SyncWorker
import com.ta.konstruksi.util.Constants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KonstruksiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodikSync()
    }

    private fun schedulePeriodikSync() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.SYNC_WORK_TAG,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.buildPeriodicRequest()
        )
    }
}
