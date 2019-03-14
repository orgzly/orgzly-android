package com.orgzly.android.usecase

import com.orgzly.android.NotesClipboard
import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.Place

class NotePaste(val bookId: Long, val noteId: Long, val place: Place) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val clipboard = NotesClipboard.load()

        val count = if (clipboard != null) {
            dataRepository.paste(clipboard, noteId, place)
        } else {
            0
        }

        return UseCaseResult(
                modifiesLocalData = count > 0,
                triggersSync = if (count > 0)
                    UseCase.SYNC_DATA_MODIFIED
                else
                    UseCase.SYNC_NOT_REQUIRED,
                userData = count)
    }
}