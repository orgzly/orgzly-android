package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteDelete(val bookId: Long, val ids: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val count = dataRepository.deleteNotes(bookId, ids)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED,
                userData = count
        )
    }
}