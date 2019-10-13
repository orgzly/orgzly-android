package com.orgzly.android.ui.repo

import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.repos.RepoWithProps
import com.orgzly.android.repos.RepoType
import com.orgzly.android.repos.SyncRepo
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.RepoCreate
import com.orgzly.android.usecase.RepoUpdate
import com.orgzly.android.usecase.UseCase
import com.orgzly.android.usecase.UseCaseRunner

open class RepoViewModel(private val dataRepository: DataRepository, open var repoId: Long) : CommonViewModel() {

    val finishEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    val alreadyExistsEvent: SingleLiveEvent<Any> = SingleLiveEvent()

    fun loadRepoProperties(): RepoWithProps? {
        val repo = dataRepository.getRepo(repoId)

        return if (repo != null) {
            val props = dataRepository.getRepoPropsMap(repoId)

            RepoWithProps(repo, props)

        } else {
            null
        }
    }

    fun saveRepo(type: RepoType, url: String, props: Map<String, String> = emptyMap()) {
        val repo = Repo(repoId, type, url)

        val repoWithProps = RepoWithProps(repo, props)

        if (repoId == 0L) {
            create(repoWithProps)
        } else {
            update(repoWithProps)
        }
    }

    fun update(props: RepoWithProps) {
        run(RepoUpdate(props))
    }

    fun create(props: RepoWithProps) {
        run(RepoCreate(props))
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

    fun validate(repoType: RepoType, url: String): SyncRepo {
        return dataRepository.getRepoInstance(repoId, repoType, url)
    }
}