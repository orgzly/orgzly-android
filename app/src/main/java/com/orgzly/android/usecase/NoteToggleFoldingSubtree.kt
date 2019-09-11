package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteToggleFoldingSubtree(val noteId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.toggleNoteFoldedStateForSubtree(noteId)

        return UseCaseResult()
    }
}