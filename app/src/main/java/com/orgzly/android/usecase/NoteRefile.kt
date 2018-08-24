package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteRefile(val bookId: Long, val noteIds: Set<Long>, val targetBookId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.refileNotes(bookId, noteIds, targetBookId)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED
        )
    }
}