package com.orgzly.android.sync

import android.app.Application
import com.orgzly.BuildConfig
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

                Type.APP_SUSPENDED ->
                    if (AppPreferences.syncOnSuspend(context)) {
                        startSync()
                    }
            }
        }
    }

    private fun startSync() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        SyncRunner.startAuto()
    }

    enum class Type {
        NOTE_CREATED,
        DATA_MODIFIED,
        APP_RESUMED,
        APP_SUSPENDED,
    }

    companion object {
        private val TAG = AutoSync::class.java.name
    }
}
