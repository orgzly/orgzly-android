package com.orgzly.android.ui.main

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.ComponentActivity
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils


abstract class ActivityForResult(val activity: ComponentActivity) {

    private val userData = mutableMapOf<String, Any>()

    fun startBookImportFileChooser() {
        startFileChooser(R.string.import_org_file, REQUEST_CODE_BOOK_IMPORT)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun startBookExportFileChooser(bookId: Long, defaultFileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_TITLE, defaultFileName)
        }

        userData.apply {
            clear()
            set(ARG_BOOK_ID, bookId)
        }

        activity.startActivityForResult(intent, REQUEST_CODE_BOOK_EXPORT)
    }

    fun startSavedSearchesImportFileChooser() {
        startFileChooser(R.string.import_, REQUEST_CODE_SAVED_SEARCHES_IMPORT)
    }


    private fun startFileChooser(@StringRes titleResId: Int, code: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val chooserIntent = Intent.createChooser(intent, activity.getString(titleResId))

        activity.startActivityForResult(chooserIntent, code)
    }

    fun onResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, resultCode, intent)

        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Activity result not OK")
            return
        }

        if (intent == null) {
            Log.w(TAG, "Activity result has no intent")
            return
        }

        when (requestCode) {
            REQUEST_CODE_BOOK_IMPORT -> {
                intent.data?.let { uri ->
                    onBookImport(uri)
                }
            }

            REQUEST_CODE_BOOK_EXPORT -> {
                intent.data?.let { uri ->
                    onBookExport(uri, userData[ARG_BOOK_ID] as Long)
                }
            }

            REQUEST_CODE_SAVED_SEARCHES_IMPORT -> {
                intent.data?.let { uri ->
                    onSearchQueriesImport(uri)
                }
            }

            else -> {
                Log.e(TAG, "Unknown request code $requestCode intent $intent")
            }
        }
    }

    abstract fun onBookImport(uri: Uri)
    abstract fun onBookExport(uri: Uri, bookId: Long)
    abstract fun onSearchQueriesImport(uri: Uri)

    companion object {
        private val TAG = ActivityForResult::class.java.name

        const val REQUEST_CODE_BOOK_IMPORT = 1
        const val REQUEST_CODE_BOOK_EXPORT = 2
        const val REQUEST_CODE_SAVED_SEARCHES_IMPORT = 3

        const val ARG_BOOK_ID = "bookId"
    }
}