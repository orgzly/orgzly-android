package com.orgzly.android.usecase

import com.orgzly.android.db.NotesClipboard
import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.Place

class NotePaste(val bookId: Long, val noteId: Long, val place: Place) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val clipboard = NotesClipboard.load()

        val count = dataRepository.pasteNotes(clipboard, bookId, noteId, place)

        return UseCaseResult(
                modifiesLocalData = count > 0,
                triggersSync = if (count > 0) SYNC_DATA_MODIFIED else SYNC_NOT_REQUIRED,
                userData = count)
    }
}