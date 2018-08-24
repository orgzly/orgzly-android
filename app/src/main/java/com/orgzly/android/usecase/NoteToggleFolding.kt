package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteToggleFolding(val noteId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.toggleNoteFoldedState(noteId)

        return UseCaseResult()
    }
}