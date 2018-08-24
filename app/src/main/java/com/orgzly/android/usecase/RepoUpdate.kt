package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class RepoUpdate(val repoId: Long, val url: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.updateRepo(repoId, url)

        return UseCaseResult()
    }
}