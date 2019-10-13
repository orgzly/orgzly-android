package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

/**
 * Both from editing preface and saving the changes and from toggling checkboxes etc.
 */
class BookUpdatePreface(val bookId: Long, val preface: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setBookPreface(bookId, preface)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}