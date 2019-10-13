package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.repos.RepoWithProps

class RepoCreate(val props: RepoWithProps) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val id = dataRepository.createRepo(props)

        return UseCaseResult(id)
    }

    class AlreadyExists: Throwable()
}