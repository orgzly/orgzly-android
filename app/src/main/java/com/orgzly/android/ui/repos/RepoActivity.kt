package com.orgzly.android.ui.repos

import android.annotation.SuppressLint
import android.os.AsyncTask
import com.orgzly.android.Shelf
import com.orgzly.android.ui.CommonActivity

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
}