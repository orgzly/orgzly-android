package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class SavedSearchMoveDown(val id: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.moveSavedSearchDown(id)

        return UseCaseResult()
    }
}