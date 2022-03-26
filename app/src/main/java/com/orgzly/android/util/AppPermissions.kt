package com.orgzly.android.util


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.ui.util.ActivityUtils

object AppPermissions {
    private val TAG = AppPermissions::class.java.name

    @JvmStatic
    fun isGrantedOrRequest(activity: CommonActivity, requestCode: Usage): Boolean {
        val permission = permissionForRequest(requestCode)
        val rationale = rationaleForRequest(requestCode)

        val grantedOrRequested = if (!isGranted(activity, requestCode)) {
            /* Should we show an explanation? */
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                activity.showSnackbar(rationale, R.string.settings) {
                    ActivityUtils.openAppInfoSettings(activity)
                }

            } else {
                /* No explanation needed -- request the permission. */
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, permission, "Requesting...")
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode.ordinal)
            }

            false

        } else {
            true
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, permission, grantedOrRequested)

        return grantedOrRequested
    }

    @JvmStatic
    fun isGranted(context: Context, requestCode: Usage): Boolean {
        val permission = permissionForRequest(requestCode)

        // WRITE_EXTERNAL_STORAGE is unused in API 30 and later
        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, requestCode, permission, "API " + Build.VERSION.SDK_INT + ", returning true")
            return true
        }

        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, permission, isGranted)

        return isGranted
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
