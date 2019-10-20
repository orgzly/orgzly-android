package com.orgzly.android.repos

import android.net.Uri
import com.orgzly.android.BookName
import com.orgzly.android.util.UriUtils
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.File
import java.io.FileOutputStream


class WebdavRepo(private val repoId: Long, private val uri: Uri, username: String, password: String) : SyncRepo {

    private val sardine = OkHttpSardine()

    init {
        sardine.setCredentials(username, password)
    }

    companion object {
        const val USERNAME_PREF_KEY = "username"
        const val PASSWORD_PREF_KEY = "password"

        fun getInstance(repoWithProps: RepoWithProps): WebdavRepo {
            val id = repoWithProps.repo.id

            val uri = Uri.parse(repoWithProps.repo.url)

            val username = checkNotNull(repoWithProps.props[USERNAME_PREF_KEY]) {
                "Username not found"
            }.toString()

            val password = checkNotNull(repoWithProps.props[PASSWORD_PREF_KEY]) {
                "Password not found"
            }.toString()

            return WebdavRepo(id, uri, username, password)
        }
    }

    override fun requiresConnection(): Boolean {
        return true
    }

    override fun getUri(): Uri {
        return uri
    }

    override fun getBooks(): MutableList<VersionedRook> {
        val url = uri.toUrl()

        if (!sardine.exists(url)) {
            sardine.createDirectory(url)
        }

        return sardine
                .list(url)
                .mapNotNull {
                    if (it.isDirectory || !BookName.isSupportedFormatFileName(it.name)) {
                        null
                    } else {
                        it.toVersionedRook()
                    }
                }
                .toMutableList()
    }

    override fun retrieveBook(fileName: String?, destination: File?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()

        sardine.get(fileUrl).use { inputStream ->
            FileOutputStream(destination).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun storeBook(file: File?, fileName: String?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()

        sardine.put(fileUrl, file, null)

        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun renameBook(from: Uri, name: String?): VersionedRook {
        val destUrl = UriUtils.getUriForNewName(from, name).toUrl()
        sardine.move(from.toUrl(), destUrl)
        return sardine.list(destUrl).first().toVersionedRook()
    }

    override fun delete(uri: Uri) {
        sardine.delete(uri.toUrl())
    }

    private fun DavResource.toVersionedRook(): VersionedRook {
        return VersionedRook(
                repoId,
                RepoType.WEBDAV,
                uri,
                Uri.withAppendedPath(uri, this.name),
                this.name + this.modified.time.toString(),
                this.modified.time
        )
    }

    private fun Uri.toUrl(): String {
        return this.toString().replace("^(?:web)?dav(s?://)".toRegex(), "http$1")
    }
}
