package com.orgzly.android.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.app.ComponentActivity
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils


abstract class ActivityForResult(val activity: ComponentActivity) {

    fun startBookImportFileChooser() {
        startFileChooser(R.string.import_org_file, BOOK_IMPORT_REQUEST_CODE)
    }

    fun startSavedSearchesImportFileChooser() {
        startFileChooser(R.string.import_, SAVED_SEARCHES_IMPORT_REQUEST_CODE)
    }


    private fun startFileChooser(@StringRes titleResId: Int, code: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val chooserIntent = Intent.createChooser(intent, activity.getString(titleResId))

        activity.startActivityForResult(chooserIntent, code)
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            BOOK_IMPORT_REQUEST_CODE -> {
                data.data?.let { uri ->
                    onBookImport(uri)
                }

            }

            SAVED_SEARCHES_IMPORT_REQUEST_CODE -> {
                data.data?.let { uri ->
                    onSearchQueriesImport(uri)
                }
            }
        }
    }

    abstract fun onBookImport(uri: Uri)
    abstract fun onSearchQueriesImport(uri: Uri)

    companion object {
        private val TAG = ActivityForResult::class.java.name

        const val BOOK_IMPORT_REQUEST_CODE = 1
        const val SAVED_SEARCHES_IMPORT_REQUEST_CODE = 2


    }
}