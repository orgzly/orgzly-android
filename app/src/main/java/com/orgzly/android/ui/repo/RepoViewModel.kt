package com.orgzly.android.ui.repo

import androidx.lifecycle.LiveData
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.RepoCreate
import com.orgzly.android.usecase.RepoUpdate
import com.orgzly.android.usecase.UseCase
import com.orgzly.android.usecase.UseCaseRunner

open class RepoViewModel(private val dataRepository: DataRepository, open var repoId: Long) : CommonViewModel() {
    val repo: LiveData<Repo> by lazy {
        dataRepository.getRepoLiveData(repoId)
    }

    val finishEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    val alreadyExistsEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    fun saveRepo(url: String) {
        if (repoId == 0L) {
            create(url)
        } else {
            update(url)
        }
    }

    fun update(url: String) {
        run(RepoUpdate(repoId, url))
    }

    fun create(url: String) {
        run(RepoCreate(url))
    }

    private fun run(useCase: UseCase) {
        App.EXECUTORS.diskIO().execute {
            try {
                val result = UseCaseRunner.run(useCase)

                // Update repo ID
                repoId = result.userData as Long

                finishEvent.postValue(result)

            } catch (ae: RepoCreate.AlreadyExists) {
                alreadyExistsEvent.postValue(true)

            } catch (t: Throwable) {
                errorEvent.postValue(t)
            }
        }
    }
}