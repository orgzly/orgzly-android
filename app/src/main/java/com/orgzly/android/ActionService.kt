package com.orgzly.android

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils


/**
 * TODO: Perform all actions in a single place
 */

class ActionService : JobIntentService() {
    private val shelf = Shelf(this)
    private val localBroadcastManager = LocalBroadcastManager.getInstance(this)

    override fun onHandleWork(intent: Intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        when (intent.action) {
            AppIntent.ACTION_IMPORT_GETTING_STARTED_NOTEBOOK ->
                importGettingStartedNotebook()

            AppIntent.ACTION_REPARSE_NOTES ->
                updatingNotesProgress {
                    shelf.reParseNotesStateAndTitles()
                }

            AppIntent.ACTION_SYNC_CREATED_AT_WITH_PROPERTY ->
                updatingNotesProgress {
                    shelf.syncCreatedAtTimeWithProperty()
                }

            AppIntent.ACTION_CLEAR_DATABASE ->
                clearDatabase()

            AppIntent.ACTION_OPEN_NOTE ->
                when {
                    intent.hasExtra(AppIntent.EXTRA_PROPERTY_NAME) && intent.hasExtra(AppIntent.EXTRA_PROPERTY_VALUE) ->
                        openFirstNoteWithProperty(
                                intent.getStringExtra(AppIntent.EXTRA_PROPERTY_NAME),
                                intent.getStringExtra(AppIntent.EXTRA_PROPERTY_VALUE))
                }

        }
    }

    private fun clearDatabase() {
        shelf.clearDatabase()

        val intent = Intent(AppIntent.ACTION_DB_CLEARED)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun updatingNotesProgress(f: () -> Unit) {
        localBroadcastManager.sendBroadcast(Intent(AppIntent.ACTION_UPDATING_NOTES_STARTED))
        f()
        localBroadcastManager.sendBroadcast(Intent(AppIntent.ACTION_UPDATING_NOTES_ENDED))
    }

    private fun importGettingStartedNotebook() {
        val book: Book = catch {
            shelf.loadBookFromResource(
                    GETTING_STARTED_NOTEBOOK_NAME,
                    BookName.Format.ORG,
                    resources,
                    GETTING_STARTED_NOTEBOOK_RESOURCE_ID)
        } ?: return

        /* Update status */
        shelf.setBookStatus(book, null, BookAction(
                BookAction.Type.INFO,
                resources.getString(R.string.loaded_from_resource, GETTING_STARTED_NOTEBOOK_NAME)))

        /* If notebook was already previously loaded, user probably requested reload.
         * Display a message in that case.
         */
        if (AppPreferences.isGettingStartedNotebookLoaded(this)) {
            val intent = Intent(AppIntent.ACTION_BOOK_IMPORTED)
            localBroadcastManager.sendBroadcast(intent)

        } else {
            AppPreferences.isGettingStartedNotebookLoaded(this, true)
        }
    }

    private fun openFirstNoteWithProperty(propName: String, propValue: String) {
        shelf.openFirstNoteWithProperty(propName, propValue)
    }

    private fun <T>catch (f: () -> T): T? {
        return try {
            f()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        val TAG: String = ActionService::class.java.name

        const val GETTING_STARTED_NOTEBOOK_NAME = "Getting Started with Orgzly"
        const val GETTING_STARTED_NOTEBOOK_RESOURCE_ID = R.raw.orgzly_getting_started

        fun enqueueWork(context: Context, action: String) {
            val intent = Intent(context, ActionService::class.java)

            intent.action = action

            enqueueWork(context, intent)
        }

        fun enqueueWork(context: Context, intent: Intent) {
            JobIntentService.enqueueWork(
                    context,
                    ActionService::class.java,
                    App.ACTION_SERVICE_JOB_ID,
                    intent)
        }

    }
}
