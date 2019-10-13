package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteCreateFromNotification(val title: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.createNoteFromNotification(title)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_NOTE_CREATED
        )
    }
}