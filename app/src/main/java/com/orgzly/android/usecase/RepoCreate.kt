package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class RepoCreate(val url: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val repoId = dataRepository.createRepo(url)

        return UseCaseResult(repoId as Any)
    }

    class AlreadyExists: Throwable()
}