package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookScrollToNote(val noteId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.openBookForNote(noteId, false)

        return UseCaseResult()
    }
}