package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteCut(val bookId: Long, val ids: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val count = dataRepository.cutNotes(bookId, ids)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED,
                userData = count
        )
    }
}