package com.orgzly.android.usecase

import com.orgzly.android.db.NotesClipboard
import com.orgzly.android.data.DataRepository

class NoteCut(val bookId: Long, val ids: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val clipboard = NotesClipboard.create(dataRepository, ids).apply {
            save()
        }

        dataRepository.deleteNotes(bookId, ids)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED,
                userData = clipboard
        )
    }
}