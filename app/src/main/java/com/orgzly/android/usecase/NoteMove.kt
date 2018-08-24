package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteMove(val bookId: Long, val noteId: Long, val offset: Int) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.moveNote(bookId, noteId, offset)

        // FIXME: Auto-sync is handled on action bar destroy instead

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED
        )
    }
}