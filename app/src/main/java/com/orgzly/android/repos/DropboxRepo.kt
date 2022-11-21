package com.orgzly.android.repos

import android.content.Context
import android.net.Uri
import kotlin.Throws
import com.orgzly.android.util.UriUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class DropboxRepo(repoWithProps: RepoWithProps, context: Context?) : SyncRepo {
    private val repoUri: Uri
    private val client: DropboxClient
    override fun isConnectionRequired(): Boolean {
        return true
    }

    override fun isAutoSyncSupported(): Boolean {
        return false
    }

    override fun getUri(): Uri {
        return repoUri
    }

    @Throws(IOException::class)
    override fun getBooks(): List<VersionedRook> {
        return client.getBooks(repoUri)
    }

    @Throws(IOException::class)
    override fun retrieveBook(fileName: String, file: File): VersionedRook {
        return client.download(repoUri, fileName, file)
    }

    @Throws(IOException::class)
    override fun storeBook(file: File, fileName: String): VersionedRook {
        return client.upload(file, repoUri, fileName)
    }

    @Throws(IOException::class)
    override fun storeFile(file: File, pathInRepo: String, fileName: String): VersionedRook {
        if (file == null || !file.exists()) {
            throw FileNotFoundException("File $file does not exist")
        }

        val folderUri = Uri.withAppendedPath(uri, pathInRepo)
        return client.upload(file, folderUri, fileName)
    }

    @Throws(IOException::class)
    override fun renameBook(fromUri: Uri, name: String): VersionedRook {
        val toUri = UriUtils.getUriForNewName(fromUri, name)
        return client.move(repoUri, fromUri, toUri)
    }

    @Throws(IOException::class)
    override fun delete(uri: Uri) {
        client.delete(uri.path)
    }

    override fun toString(): String {
        return repoUri.toString()
    }

    companion object {
        const val SCHEME = "dropbox"
    }

    init {
        repoUri = Uri.parse(repoWithProps.repo.url)
        client = DropboxClient(context, repoWithProps.repo.id)
    }
}