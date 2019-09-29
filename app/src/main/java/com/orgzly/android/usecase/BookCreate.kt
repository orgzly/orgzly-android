package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookCreate(val name: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.createBook(name)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}