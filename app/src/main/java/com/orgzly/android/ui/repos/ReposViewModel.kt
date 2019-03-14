package com.orgzly.android.ui.repos

import androidx.lifecycle.LiveData
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.RepoDelete
import com.orgzly.android.usecase.UseCaseRunner

class ReposViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    val repos: LiveData<List<Repo>> by lazy {
        dataRepository.selectRepos()
    }

    val openRepoRequestEvent: SingleLiveEvent<Repo> = SingleLiveEvent()

    fun openRepo(id: Long) {
        App.EXECUTORS.diskIO().execute {
            openRepoRequestEvent.postValue(dataRepository.getRepo(id))
        }
    }

    fun deleteRepo(id: Long) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                UseCaseRunner.run(RepoDelete(id))
            }
        }
    }

//    fun create(url: String): Completable {
//        return Completable.fromAction {
//            data.createRepo(url)
//        }
//    }
//
//    fun update(id: Long, url: String): Completable {
//        return Completable.fromAction {
//            data.updateRepo(id, url)
//        }
//    }
//
//    fun delete(id: Long): Completable {
//        return Completable.fromAction {
//            data.deleteRepo(id)
//        }
//    }

}