package com.orgzly.android.repos

import android.content.Context
import android.net.Uri
import com.orgzly.android.BookName
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.RepoPreferences
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class WebdavRepo(private val uri: Uri, username: String?, password: String?) : SyncRepo {

    private val sardine = OkHttpSardine()

    init {
        sardine.setCredentials(username, password)
    }

    companion object {
        const val SCHEME = "webdav"
        const val SSL_SCHEME = "webdavs"
        const val USERNAME_PREF_KEY = "repo_webdav_username"
        const val PASSWORD_PREF_KEY = "repo_webdav_password"

        @JvmStatic
        fun buildFromUri(context: Context, uri: Uri, repo: DataRepository): WebdavRepo {
            val prefs = RepoPreferences.fromUri(context, uri, repo).repoPreferences
            return WebdavRepo(
                    uri,
                    prefs.getString(USERNAME_PREF_KEY, null),
                    prefs.getString(PASSWORD_PREF_KEY, null)
            )
        }
    }

    override fun requiresConnection(): Boolean {
        return true
    }

    override fun getUri(): Uri {
        return uri
    }

    override fun getBooks(): MutableList<VersionedRook> {
        return sardine
                .list(uri.toUrl())
                .mapNotNull {
                    if (it.isDirectory && !BookName.isSupportedFormatFileName(it.name)) {
                        null
                    } else {
                        it.toVersionedRook()
                    }
                }
                .toMutableList()
    }

    override fun retrieveBook(fileName: String?, destination: File?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()
        val fileBytes = sardine.get(fileUrl).readBytes()
        val outputStream = FileOutputStream(destination)
        outputStream.write(fileBytes)
        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun storeBook(file: File?, fileName: String?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()
        val fileBytes = FileInputStream(file).readBytes()
        sardine.put(fileUrl, fileBytes)
        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun renameBook(from: Uri, name: String?): VersionedRook {
        val destUrl = Uri.withAppendedPath(uri, name).toUrl()
        sardine.move(from.toUrl(), destUrl)
        return sardine.list(destUrl).first().toVersionedRook()
    }

    override fun delete(uri: Uri) {
        sardine.delete(uri.toUrl())
    }

    private fun DavResource.toVersionedRook(): VersionedRook {
        return VersionedRook(
                uri,
                Uri.withAppendedPath(uri, this.name),
                this.name + this.modified.time.toString(),
                this.modified.time
        )
    }

    private fun Uri.toUrl(): String {
        return this.toString().let {
            if (it.startsWith("webdav")) {
                it.replaceFirst("webdav", "http")
            } else {
                it
            }
        }
    }
}