package com.orgzly.android.usecase

import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository

class TimestampUpdate : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.updateTimestamps()

        return UseCaseResult(
                modifiesLocalData = true
        )
    }

    override fun toAction(): String {
        return AppIntent.ACTION_UPDATE_TIMESTAMPS
    }

}