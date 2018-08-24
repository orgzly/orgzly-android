package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class RepoDelete(val repoId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.deleteRepo(repoId)

        return UseCaseResult()
    }
}