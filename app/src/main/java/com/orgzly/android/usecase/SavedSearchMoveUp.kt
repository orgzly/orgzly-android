package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class SavedSearchMoveUp(val id: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.moveSavedSearchUp(id)

        return UseCaseResult()
    }
}