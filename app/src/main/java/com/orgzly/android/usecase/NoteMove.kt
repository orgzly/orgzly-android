package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteMove(val bookId: Long, val noteIds: Set<Long>, val offset: Int) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.moveNote(bookId, noteIds, offset)

        // FIXME: Auto-sync is handled on action bar destroy instead

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}