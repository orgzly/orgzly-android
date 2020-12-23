package com.orgzly.android.util

import android.content.Context
import com.orgzly.android.prefs.AppPreferences

object AttachmentUtils {
    /** Returns the attachment directory based on ID property. */
    fun getAttachDir(context: Context, idStr: String) : String {
        return if (idStr.length <= 2) {
            AppPreferences.attachDirDefaultPath(context) + "/" + idStr.substring(0, 2)
        } else {
            AppPreferences.attachDirDefaultPath(context) + "/" + idStr.substring(0, 2) + "/" + idStr.substring(2)
        }
    }
}