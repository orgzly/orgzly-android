package com.orgzly.android.usecase

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils
import javax.inject.Inject

class UseCaseService : JobIntentService() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onCreate() {
        App.appComponent.inject(this)

        super.onCreate()
    }

    override fun onHandleWork(intent: Intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        when (intent.action) {
            AppIntent.ACTION_IMPORT_GETTING_STARTED_NOTEBOOK ->
                postGettingStartedImport {
                    UseCaseRunner.run(BookImportGettingStarted())
                }

            AppIntent.ACTION_REPARSE_NOTES ->
                notesUpdateBroadcast {
                    UseCaseRunner.run(NoteReparseStateAndTitles())
                }

            AppIntent.ACTION_SYNC_CREATED_AT_WITH_PROPERTY ->
                notesUpdateBroadcast {
                    UseCaseRunner.run(NoteSyncCreatedAtTimeWithProperty())
                }
        }
    }

    private fun notesUpdateBroadcast(action: () -> Unit) {
        LocalBroadcastManager.getInstance(this).let {
            it.sendBroadcast(Intent(AppIntent.ACTION_UPDATING_NOTES_STARTED))
            action()
            it.sendBroadcast(Intent(AppIntent.ACTION_UPDATING_NOTES_ENDED))
        }
    }

    private fun postGettingStartedImport(action: () -> Unit) {
        action()

        /* If notebook was already previously loaded, it's a user-requested reload.
         * Display a message in that case.
         */
        if (AppPreferences.isGettingStartedNotebookLoaded(this)) {
            val intent = Intent(AppIntent.ACTION_BOOK_IMPORTED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        } else {
            AppPreferences.isGettingStartedNotebookLoaded(this, true)
        }
    }

    companion object {
        val TAG: String = UseCaseService::class.java.name

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                    context,
                    UseCaseService::class.java,
                    App.ACTION_SERVICE_JOB_ID,
                    intent)
        }
    }
}
