package com.orgzly.android.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.orgzly.BuildConfig
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.sync.SyncState
import com.orgzly.android.util.LogUtils

class SyncViewModel : ViewModel() {
    val state: LiveData<SyncState> = SyncRunner.onStateChange("sync-view-model")

    fun isSyncRunning(): Boolean {
        val currentState = state.value

        val isRunning = currentState?.isRunning() ?: false

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "isRunning", isRunning, currentState)

        return isRunning
    }

    /* Prevent snackbar from being displayed when the last state is a failure
     * and user returns to the app. It's allowed once sync worker in seen running.
     */
    var allowSnackbarOnFailure = false

    companion object {
        private val TAG = SyncViewModel::class.java.name
    }
}