package com.orgzly.android.ui.repo.webdav

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.repos.WebdavRepo
import com.orgzly.android.ui.repo.RepoViewModel
import com.thegrizzlylabs.sardineandroid.impl.SardineException

class WebdavRepoViewModel(
        dataRepository: DataRepository,
        override var repoId: Long
) : RepoViewModel(dataRepository, repoId) {

    var certificates: MutableLiveData<String?> = MutableLiveData(null)

    sealed class ConnectionResult {
        data class InProgress(val msg: Int): ConnectionResult()
        data class Success(val bookCount: Int): ConnectionResult()
        data class Error(val msg: Any): ConnectionResult()
    }

    val connectionTestStatus: MutableLiveData<ConnectionResult> = MutableLiveData()

    fun testConnection(uriString: String, username: String, password: String, certificates: String?) {
        App.EXECUTORS.networkIO().execute {
            try {
                connectionTestStatus.postValue(ConnectionResult.InProgress(R.string.connecting))

                val uri = Uri.parse(uriString)

                val bookCount = WebdavRepo(repoId, uri, username, password, certificates).run {
                    books.size
                }

                connectionTestStatus.postValue(ConnectionResult.Success(bookCount))

            } catch (e: Exception) {
                e.printStackTrace()

                val result = when (e) {
                    is SardineException -> {
                        when (e.statusCode) {
                            401 -> ConnectionResult.Error(R.string.webdav_test_error_auth)
                            else -> ConnectionResult.Error("${e.statusCode}: ${e.responsePhrase}")
                        }

                    }

                    else -> ConnectionResult.Error(e.message ?: R.string.webdav_test_error_unknown)
                }

                connectionTestStatus.postValue(result)
            }
        }
    }
}