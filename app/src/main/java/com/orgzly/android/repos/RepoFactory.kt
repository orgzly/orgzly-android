package com.orgzly.android.repos

import android.content.Context
import com.orgzly.BuildConfig
import com.orgzly.android.data.DbRepoBookRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepoFactory @Inject constructor(
        private val context: Context,
        private val dbRepoBookRepository: DbRepoBookRepository
) {
    fun getInstance(repoWithProps: RepoWithProps): SyncRepo {
        val type = repoWithProps.repo.type.id

        return when {
            type == RepoType.MOCK.id ->
                MockRepo(repoWithProps, dbRepoBookRepository)

            type == RepoType.DROPBOX.id && BuildConfig.IS_DROPBOX_ENABLED ->
                DropboxRepo(repoWithProps, context)

            type == RepoType.DIRECTORY.id ->
                DirectoryRepo(repoWithProps, false)

            type == RepoType.DOCUMENT.id ->
                ContentRepo(repoWithProps, context)

            type == RepoType.WEBDAV.id ->
                WebdavRepo.getInstance(repoWithProps)

            type == RepoType.GIT.id && BuildConfig.IS_GIT_ENABLED ->
                GitRepo.getInstance(repoWithProps)

            else ->
                throw IllegalArgumentException("Unknown type or disabled repo $repoWithProps")
        }
    }
}
