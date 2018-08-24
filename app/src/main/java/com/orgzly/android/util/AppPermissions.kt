package com.orgzly.android.util


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import com.orgzly.R
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.util.ActivityUtils

object AppPermissions {
    @JvmStatic
    fun isGrantedOrRequest(activity: CommonActivity, requestCode: Usage): Boolean {
        val permission = permissionForRequest(requestCode)
        val rationale = rationaleForRequest(requestCode)

        if (!isGranted(activity, requestCode)) {
            /* Should we show an explanation? */
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                val view = activity.findViewById(R.id.main_content) as View

                activity.showSnackbar(Snackbar.make(view, rationale, Snackbar.LENGTH_LONG)
                        .setAction(R.string.settings) { ActivityUtils.openAppInfoSettings(activity) })

            } else {
                /* No explanation needed -- request the permission. */
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode.ordinal)
            }

            return false

        } else {
            return true
        }
    }

    @JvmStatic
    fun isGranted(context: Context, requestCode: Usage): Boolean {
        val permission = permissionForRequest(requestCode)
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Map request code to permission. */
    private fun permissionForRequest(requestCode: Usage): String {
        return when (requestCode) {
            Usage.LOCAL_REPO -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Usage.BOOK_EXPORT -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Usage.SYNC_START -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Usage.SAVED_SEARCHES_EXPORT_IMPORT -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Usage.EXTERNAL_FILES_ACCESS -> Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /** Map request code to explanation. */
    private fun rationaleForRequest(requestCode: Usage): Int {
        return when (requestCode) {
            Usage.LOCAL_REPO -> R.string.permissions_rationale_for_local_repo
            Usage.BOOK_EXPORT -> R.string.permissions_rationale_for_book_export
            Usage.SYNC_START -> R.string.permissions_rationale_for_sync_start
            Usage.SAVED_SEARCHES_EXPORT_IMPORT -> R.string.storage_permissions_missing
            Usage.EXTERNAL_FILES_ACCESS -> R.string.permissions_rationale_for_external_files_access
        }
    }

    enum class Usage {
        LOCAL_REPO,
        BOOK_EXPORT,
        SYNC_START,
        SAVED_SEARCHES_EXPORT_IMPORT,
        EXTERNAL_FILES_ACCESS
    }
}
