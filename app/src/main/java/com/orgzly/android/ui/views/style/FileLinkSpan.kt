package com.orgzly.android.ui.views.style

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import android.support.v4.content.LocalBroadcastManager
import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.BookName
import com.orgzly.android.provider.clients.BooksClient
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.util.AppPermissions
import java.io.File
import java.lang.Exception

class FileLinkSpan(val path: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        // Run after onClick to prevent Snackbar from closing immediately
        Handler().post {
            handleClick(widget)
        }
    }

    private fun handleClick(widget: View) {
        val context = widget.context

        val activity = App.getCurrentActivity()

        val file = if (isAbsolute(path)) {
            File(path)

        } else {
            isMaybeBook(path)?.let { name ->
                if (openBookIfExists(context, name)) {
                    return
                }
            }

            File(Environment.getExternalStorageDirectory(), path)
        }

        openFileIfExists(context, activity, file)
    }

    private fun isAbsolute(path: String): Boolean {
        return path.startsWith('/')
    }

    private fun isMaybeBook(path: String): BookName? {
        val file = File(path)

        return if (!hasParent(file) && BookName.isSupportedFormatFileName(file.name)) {
            BookName.fromFileName(file.name)
        } else {
            null
        }
    }

    private fun hasParent(file: File): Boolean {
        return file.parentFile != null && file.parentFile.name != "."
    }

    private fun openBookIfExists(context: Context, bookName: BookName): Boolean {
        val book = BooksClient.get(context, bookName.name)

        if (book != null) {
            val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.id)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        return book != null
    }


    private fun openFileIfExists(context: Context, activity: CommonActivity?, file: File) {
        if (file.exists()) {
            activity?.runWithPermission(
                    AppPermissions.Usage.EXTERNAL_FILES_ACCESS,
                    Runnable {
                        try {
                            openFile(context, activity, file)
                        } catch (e: Exception) {
                            activity.showSnackbar(activity.getString(
                                    R.string.failed_to_open_linked_file_with_reason,
                                    e.localizedMessage))
                        }
                    })

        } else {
            activity?.let {
                it.showSnackbar(it.getString(R.string.file_does_not_exist, file.canonicalFile))
            }
        }
    }

    private fun openFile(context: Context, activity: CommonActivity?, file: File) {
        val contentUri = FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW, contentUri)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Added for support on API 16
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Try to start an activity for opening the file
        try {
            startActivity(context, intent, null)
        } catch (e: ActivityNotFoundException) {
            activity?.showSnackbar(R.string.external_file_no_app_found)
        }
    }
}