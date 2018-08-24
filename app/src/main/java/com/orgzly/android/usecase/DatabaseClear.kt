package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class DatabaseClear : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.clearDatabase()

        return UseCaseResult(
                modifiesLocalData = true
        )
    }
}