package com.orgzly.android.usecase

import android.content.Intent
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository

class BookImportGettingStarted : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.importGettingStartedBook()

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }

    override fun toIntent(): Intent {
        return Intent(AppIntent.ACTION_IMPORT_GETTING_STARTED_NOTEBOOK)
    }
}