package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class SavedSearchExport : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.exportSavedSearches()

        return UseCaseResult()
    }
}