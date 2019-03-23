package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteDemote(val bookId: Long, val noteIds: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val count = dataRepository.demoteNotes(bookId, noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED,
                userData = count
        )
    }
}