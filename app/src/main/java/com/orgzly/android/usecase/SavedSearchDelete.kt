package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class SavedSearchDelete(val ids: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.deleteSavedSearches(ids)

        return UseCaseResult(
                modifiesListWidget = true
        )
    }
}