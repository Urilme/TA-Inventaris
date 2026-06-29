package com.ta.konstruksi.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ta.konstruksi.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SyncWorkState { IDLE, RUNNING, SUCCESS, FAILED }

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val syncState: StateFlow<SyncWorkState> = WorkManager.getInstance(context)
        .getWorkInfosByTagFlow(Constants.SYNC_WORK_TAG)
        .map { infos ->
            when {
                infos.any { it.state == WorkInfo.State.RUNNING } -> SyncWorkState.RUNNING
                infos.any { it.state == WorkInfo.State.SUCCEEDED } -> SyncWorkState.SUCCESS
                infos.any { it.state == WorkInfo.State.FAILED } -> SyncWorkState.FAILED
                else -> SyncWorkState.IDLE
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncWorkState.IDLE)

    fun triggerManualSync() {
        val request = com.ta.konstruksi.sync.worker.SyncWorker.buildOneTimeRequest()
        WorkManager.getInstance(context).enqueue(request)
    }
}
