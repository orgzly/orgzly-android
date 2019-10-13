package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.repos.RepoWithProps

class RepoUpdate(val repoWithProps: RepoWithProps) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val id = dataRepository.updateRepo(repoWithProps)

        return UseCaseResult(id)
    }
}