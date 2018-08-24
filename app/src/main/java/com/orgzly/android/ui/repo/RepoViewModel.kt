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

class RepoViewModel(dataRepository: DataRepository, private val id: Long) : CommonViewModel() {
    val repo: LiveData<Repo> by lazy {
        dataRepository.getRepoLiveData(id)
    }

    val finishEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    val alreadyExistsEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    fun update(url: String) {
        run(RepoUpdate(id, url))
    }

    fun create(url: String) {
        run(RepoCreate(url))
    }

    private fun run(useCase: UseCase) {
        App.EXECUTORS.diskIO().execute {
            try {
                finishEvent.postValue(UseCaseRunner.run(useCase))

            } catch (ae: RepoCreate.AlreadyExists) {
                alreadyExistsEvent.postValue(true)

            } catch (t: Throwable) {
                errorEvent.postValue(t)
            }
        }
    }
}