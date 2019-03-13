package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class RepoCreate(val url: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val id = dataRepository.createRepo(url)

        return UseCaseResult(userData = id)
    }

    class AlreadyExists: Throwable()
}