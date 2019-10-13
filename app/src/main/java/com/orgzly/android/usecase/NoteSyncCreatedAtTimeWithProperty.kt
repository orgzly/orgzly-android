package com.orgzly.android.usecase

import android.content.Intent
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository

class NoteSyncCreatedAtTimeWithProperty : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.syncCreatedAtTimeWithProperty()

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }

    override fun toIntent(): Intent {
        return Intent(AppIntent.ACTION_SYNC_CREATED_AT_WITH_PROPERTY)
    }
}