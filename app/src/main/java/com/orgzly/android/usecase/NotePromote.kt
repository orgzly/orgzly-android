package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NotePromote(val bookId: Long, val noteIds: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.promoteNotes(bookId, noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED
        )
    }
}