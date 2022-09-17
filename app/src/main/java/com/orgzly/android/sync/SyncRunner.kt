package com.orgzly.android.sync

import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.*
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.ui.repos.ReposActivity
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.util.LogUtils

object SyncRunner {
    const val AUTO_SYNC_DATA = "auto-sync"

    private val TAG: String = SyncRunner::class.java.name

    private const val UNIQUE_WORK_NAME = "sync"

    @JvmStatic
    fun startAuto() {
        startSync(true)
    }

    @JvmStatic
    @JvmOverloads
    fun startSync(autoSync: Boolean = false) {
        val workManager = WorkManager.getInstance(App.getAppContext())

        val syncWorker = OneTimeWorkRequestBuilder<SyncWorker>()
            // On Android >= 12 notification from overridden getForegroundInfo might not be shown
            // We are handing sync-in-progress notification manually
            // .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(AUTO_SYNC_DATA to autoSync))
            .build()

        workManager
            .beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, syncWorker)
            .enqueue()
    }

    @JvmStatic
    fun showSyncFailedSnackBar(activity: FragmentActivity, state: SyncState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity, state)

        val msg = state.getDescription(activity)

        activity.showSnackbar(msg, R.string.repositories) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setClass(activity, ReposActivity::class.java)
            }
            startActivity(activity, intent, null)
        }
    }

    @JvmStatic
    fun stopSync() {
        val workManager = WorkManager.getInstance(App.getAppContext())
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    @JvmStatic
    fun onStateChange(tag: String): LiveData<SyncState> {
        return Transformations.map(onAllWorkInfo()) { workInfoList ->
            syncStateFromWorkInfoList(workInfoList).also { state ->
                logStateChange(tag, workInfoList, state)
            }
        }

//        return MediatorLiveData<SyncState>().apply {
//            addSource(onInitWorkInfo()) {
//                value = state
//            }
//
//            addSource(onMainWorkInfo()) {
//                value = state
//            }
//        }
    }

    private fun logStateChange(tag: String, workInfoList: List<WorkInfo>?, state: SyncState?) {
        if (BuildConfig.LOG_DEBUG) {
            LogUtils.d(TAG, "-> ($tag) Workers changed state to $state <- $workInfoList")
        }
    }

    private fun onAllWorkInfo(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(App.getAppContext())
            .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME)
    }

    private fun syncStateFromWorkInfoList(workInfoList: List<WorkInfo>): SyncState? {
        if (workInfoList.isEmpty()) {
            return null
        }

        val oneAndOnlyWorker = workInfoList.first()

        oneAndOnlyWorker.run {
            if (state == WorkInfo.State.CANCELLED) {
                return SyncState.getInstance(SyncState.Type.CANCELED)

            } else if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                return SyncState.fromData(progress)
                    ?: SyncState.getInstance(SyncState.Type.STARTING)

            } else if (state.isFinished) {
                return SyncState.fromData(outputData)
            }
        }

        return null
    }
}