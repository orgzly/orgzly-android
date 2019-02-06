package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class TimestampRefresh : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.updateTimestamps()

        return UseCaseResult(
                modifiesLocalData = true
        )
    }
}