package com.orgzly.android.usecase

import com.orgzly.android.App
import com.orgzly.android.NotesClipboard
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences

class NoteClipboardCut(val bookId: Long, val ids: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val clipboard = NotesClipboard.getOrgInstance(dataRepository, ids)

        AppPreferences.notesOrgClipboard(App.getAppContext(), clipboard.toOrg())

        dataRepository.cutNotes(bookId, ids)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED,
                userData = clipboard
        )
    }
}