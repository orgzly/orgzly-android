package com.orgzly.android.ui.repos

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import com.orgzly.android.Shelf
import com.orgzly.android.repos.ContentRepo
import com.orgzly.android.ui.CommonActivity

@SuppressLint("Registered")
open class RepoActivity : CommonActivity() {
    companion object {
        val TAG: String = RepoActivity::class.java.name
    }

    @SuppressLint("StaticFieldLeak")
    fun updateRepoUrl(id: Long, url: String) {
        val shelf = Shelf(this)

        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                shelf.updateRepoUrl(id, url)
                return null
            }
        }.execute()
    }

    @SuppressLint("StaticFieldLeak")
    fun addRepoUrl(url: String) {
        val shelf = Shelf(this)

        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                shelf.addRepoUrl(url)
                return null
            }
        }.execute()
    }

    fun persistPermissions(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && ContentRepo.SCHEME == uri.scheme) {
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
    }

    open fun updateUri(uri: Uri) {
    }
}