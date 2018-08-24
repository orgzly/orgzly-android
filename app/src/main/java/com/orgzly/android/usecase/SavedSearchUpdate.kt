package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch

class SavedSearchUpdate(val savedSearch: SavedSearch) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.updateSavedSearch(savedSearch)

        return UseCaseResult(
                modifiesListWidget = true
        )
    }
}