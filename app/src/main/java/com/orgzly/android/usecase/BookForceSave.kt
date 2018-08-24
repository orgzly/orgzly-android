package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

/**
 * Saves book to its linked remote book, or to the one-and-only repository.
 */
class BookForceSave(val bookId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.forceSaveBook(bookId)

        return UseCaseResult()
    }
}