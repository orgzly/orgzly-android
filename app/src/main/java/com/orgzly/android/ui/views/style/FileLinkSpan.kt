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

class FileLinkSpan(val path: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        // Run after onClick to prevent snackbar from closing immediately
        Handler().post {
            handleClick(widget)
        }
    }

    private fun handleClick(widget: View) {
        val context = widget.context

        val activity = App.getCurrentActivity()

        if (path.startsWith('/')) {
            openFileIfExists(context, activity, File(path))

        } else {
            isMaybeBook(path)?.let { name ->
                if (openBookIfExists(context, name)) {
                    return
                }
            }

            openFileIfExists(
                    context, activity, File(Environment.getExternalStorageDirectory(), path))
        }
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
                    Runnable { openFile(context, activity, file) })
        } else {
            activity?.let {
                it.showSnackbar(it.getString(R.string.file_does_not_exist, file.path))
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