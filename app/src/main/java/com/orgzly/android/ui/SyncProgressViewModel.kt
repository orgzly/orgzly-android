package com.orgzly.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.sync.SyncState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Used by all fragments to update sync progress.
 */
class SyncProgressViewModel : ViewModel() {
    private val _syncState = MutableSharedFlow<SyncState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val syncState: Flow<SyncState?> = _syncState.distinctUntilChanged()

    init {
        viewModelScope.launch {
            SyncRunner.onStateChange("sync-view-model").asFlow().collect {
                _syncState.tryEmit(it)
            }
        }
    }
}