package com.orgzly.android.sync

import android.app.Application
import android.content.Intent
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSync @Inject constructor(val context: Application, val dataRepository: DataRepository) {

    fun trigger(type: Type) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, type)

        if (AppPreferences.autoSync(context)) {
            when (type) {
                Type.NOTE_CREATED ->
                    if (AppPreferences.syncOnNoteCreate(context)) {
                        startSync()
                    }

                Type.DATA_MODIFIED ->
                    if (AppPreferences.syncOnNoteUpdate(context)) {
                        startSync()
                    }

                Type.APP_RESUMED ->
                    if (AppPreferences.syncOnResume(context)) {
                        startSync()
                    }
            }
        }
    }

    private fun startSync() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        App.EXECUTORS.diskIO().execute {
            // Skip sync if there are no repos
            if (dataRepository.getRepos().isNotEmpty()) {
                val intent = Intent(context, SyncService::class.java)
                        .setAction(AppIntent.ACTION_SYNC_START)
                        .putExtra(AppIntent.EXTRA_IS_AUTOMATIC, true)

                SyncService.start(context, intent)
            }
        }
    }

    enum class Type {
        NOTE_CREATED,
        DATA_MODIFIED,
        APP_RESUMED
    }

    companion object {
        private val TAG = AutoSync::class.java.name
    }

}