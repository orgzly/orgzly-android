package com.orgzly.android.repos

import android.content.Context
import android.net.Uri

import com.orgzly.BuildConfig
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.DbRepoBookRepository

import javax.inject.Inject

class RepoFactory @Inject constructor(private val dbRepoBookRepository: DbRepoBookRepository) {

    fun getFromUri(context: Context, uri: Uri, repo: DataRepository): SyncRepo? {
        return getFromUri(context, uri.toString(), repo)
    }

    // TODO: Better throw exception, not return null?
    // TODO: Can we inject the DataRepository instead? Like the DbRepoBookRepository
    fun getFromUri(context: Context, uriString: String, repo: DataRepository): SyncRepo? {
        val uri = Uri.parse(uriString)

        if (uri != null && uri.scheme != null) { // Make sure uri is valid and has a scheme
            try {
                return when  {
                    uri.scheme == ContentRepo.SCHEME ->
                        ContentRepo(context, uri)

                    BuildConfig.IS_DROPBOX_ENABLED && uri.scheme == DropboxRepo.SCHEME && uri.authority == null ->
                        DropboxRepo(context, uri)

                    BuildConfig.IS_GIT_ENABLED && uri.scheme == GitRepo.SCHEME ->
                        GitRepo.buildFromUri(context, uri, repo)

                    uri.scheme == DirectoryRepo.SCHEME ->
                        DirectoryRepo(uriString, false)

                    uri.scheme == MockRepo.SCHEME ->
                        MockRepo(dbRepoBookRepository, uriString)

                    uri.scheme in WebdavRepo.SCHEMES ->
                        WebdavRepo.buildFromUri(context, uri, repo)

                    else -> null
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }
}
